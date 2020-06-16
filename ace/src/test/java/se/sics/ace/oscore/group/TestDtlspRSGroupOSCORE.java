/*******************************************************************************
 * Copyright (c) 2019, RISE AB
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package se.sics.ace.oscore.group;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.CoapEndpoint.Builder;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.junit.Assert;

import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;

import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.CoseException;
import org.eclipse.californium.cose.KeyKeys;
import org.eclipse.californium.cose.MessageTag;
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.elements.util.StringUtil;
import org.eclipse.californium.oscore.OSCoreEndpointContextInfo;

import se.sics.ace.AceException;
import se.sics.ace.COSEparams;
import se.sics.ace.Constants;
import se.sics.ace.TestConfig;
import se.sics.ace.coap.rs.CoapDeliverer;
import se.sics.ace.cwt.CWT;
import se.sics.ace.cwt.CwtCryptoCtx;
import se.sics.ace.examples.KissTime;
import se.sics.ace.examples.LocalMessage;
import se.sics.ace.oscore.GroupInfo;
import se.sics.ace.oscore.GroupOSCORESecurityContextObjectParameters;
import se.sics.ace.oscore.OSCORESecurityContextObjectParameters;
import se.sics.ace.oscore.rs.AuthzInfoGroupOSCORE;
import se.sics.ace.oscore.rs.CoapAuthzInfoGroupOSCORE;
import se.sics.ace.oscore.rs.DtlspPskStoreGroupOSCORE;
import se.sics.ace.oscore.rs.GroupOSCOREJoinValidator;
import se.sics.ace.rs.AsRequestCreationHints;
import se.sics.ace.rs.TokenRepository;

/**
 * Server for testing the DTLSProfileDeliverer class. 
 * 
 * The Junit tests are in TestDtlspClientGroupOSCORE, 
 * which will automatically start this server.
 * 
 * @author Ludwig Seitz and Marco Tiloca
 *
 */
public class TestDtlspRSGroupOSCORE {
	
	//Name of the AS (this RS will accept token from this issuer)
	private static String AS_NAME = "TestAS";
		
	//Sets the secure and unsecure port to use
	private final static int SECURE_PORT = CoAP.DEFAULT_COAP_SECURE_PORT;
	private final static int PORT = CoAP.DEFAULT_COAP_PORT;

	private final static int groupIdPrefixSize = 4; // Up to 4 bytes, same for all the OSCORE Group of the Group Manager
	
	static Map<String, GroupInfo> activeGroups = new HashMap<>();
	
    /**
     * Definition of the Hello-World Resource
     */
    public static class HelloWorldResource extends CoapResource {
        
        /**
         * Constructor
         */
        public HelloWorldResource() {
            
            // set resource identifier
            super("helloWorld");
            
            // set display name
            getAttributes().setTitle("Hello-World Resource");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            
            // respond to the request
            exchange.respond("Hello World!");
        }
    }
    
    /**
     * Definition of the Temp Resource
     */
    public static class TempResource extends CoapResource {
        
        /**
         * Constructor
         */
        public TempResource() {
            
            // set resource identifier
            super("temp");
            
            // set display name
            getAttributes().setTitle("Temp Resource");
        }

        @Override
        public void handleGET(CoapExchange exchange) {
            
            // respond to the request
            exchange.respond("19.0 C");
        }
    }
    
    // M.T.
    /**
     * Definition of the root group-membership resource for Group OSCORE
     * 
     * Children of this resource are the group-membership resources
     */
    public static class GroupOSCORERootMembershipResource extends CoapResource {
        
        /**
         * Constructor
         * @param resId  the resource identifier
         */
        public GroupOSCORERootMembershipResource(String resId) {
            
            // set resource identifier
            super(resId);
            
            // set display name
            getAttributes().setTitle("Group OSCORE Entry Resource " + resId);
        }
        
    }
    
    // M.T.
    /**
     * Definition of the group-membership resource for Group OSCORE
     */
    public static class GroupOSCOREJoinResource extends CoapResource {
        
        /**
         * Constructor
         * @param resId  the resource identifier
         */
        public GroupOSCOREJoinResource(String resId) {
            
            // set resource identifier
            super(resId);
            
            // set display name
            getAttributes().setTitle("Group OSCORE Group-Membership Resource " + resId);
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
        	
        	Set<String> roles = new HashSet<>();
        	boolean providePublicKeys = false;
        	
        	String subject;
        	Request request = exchange.advanced().getCurrentRequest();
            if (request.getSourceContext() == null || request.getSourceContext().getPeerIdentity() == null) {
                //XXX: Kludge for OSCORE since cf-oscore doesn't set PeerIdentity
				if (exchange.advanced().getCryptographicContextID() != null) {
					byte[] clientSenderId = StringUtil.hex2ByteArray(
							request.getSourceContext().get(OSCoreEndpointContextInfo.OSCORE_RECIPIENT_ID));
					subject = new String(clientSenderId, Constants.charset);
                } else {
                	// At this point, this should not really happen, due to the earlier check at the Token Repository
                	exchange.respond(CoAP.ResponseCode.UNAUTHORIZED, "Unauthenticated client tried to get access");
	  				return;
                }
            } else  {
                subject = request.getSourceContext().getPeerIdentity().getName();
            }
            // TODO: REMOVE DEBUG PRINT
            // System.out.println("xxx @GM sid " + subject);
            // System.out.println("yyy @GM kid " + TokenRepository.getInstance().getKid(subject));
            
            String rsNonceString = TokenRepository.getInstance().getRsnonce(subject);
            
            // TODO: REMOVE DEBUG PRINT
            // System.out.println("xxx @GM rsnonce " + rsNonceString);
                        
            byte[] rsnonce = Base64.getDecoder().decode(rsNonceString);
            
        	byte[] requestPayload = exchange.getRequestPayload();
        	CBORObject joinRequest = CBORObject.DecodeFromBytes(requestPayload);
            
        	if (!joinRequest.getType().equals(CBORType.Map))
        		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "The payload of the join request must be a CBOR Map");
        		
        	// More steps follow:
        	//
        	// Retrieve 'scope' from the map; check the GroupID against the name of the resource, just for consistency.
        	//
        	// Retrieve the role(s) to possibly reduce the set of material to provide to the joining node.
        	//
        	// Any other check is performed through the method canAccess() of the TokenRepository, which is
        	// in turn invoked by the deliverRequest() method of CoapDeliverer, upon getting the join request.
        	// The actual checks of legitimate access are performed by scopeMatchResource() and scopeMatch()
        	// of the GroupOSCOREJoinValidator used as Scope/Audience Validator.
        	
        	// Retrieve scope
        	CBORObject scope = joinRequest.get(CBORObject.FromObject(Constants.SCOPE));
        	
        	if (scope == null) {
        		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Scope must be included for joining OSCORE groups");
        		return;
        	}

        	if (!scope.getType().equals(CBORType.ByteString)) {
        		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Scope must be wrapped in a binary string for joining OSCORE groups");
        		return;
            }
        	
        	byte[] rawScope = scope.GetByteString();
        	CBORObject cborScope = CBORObject.DecodeFromBytes(rawScope);
        	
        	if (!cborScope.getType().equals(CBORType.Array)) {
        		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Invalid scope format for joining OSCORE groups");
        		return;
            }
        	
        	if (cborScope.size() != 2) {
        		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Invalid scope format for joining OSCORE groups");
        		return;
            }
        	
        	// Retrieve the name of the OSCORE group
        	String groupName;
      	  	CBORObject scopeElement = cborScope.get(0);
      	  	if (scopeElement.getType().equals(CBORType.TextString)) {
      	  		groupName = scopeElement.AsString();

          	  	if (!groupName.equals(this.getName())) {
	  				exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "The group name in 'scope' is not pertinent for this group-membership resource");
	  				return;
	  			}      	  		
      	  	}
      	  	else {
      	  		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Invalid scope format for joining OSCORE groups");
        		return;
      	  	}
      	  	
      	  	// Retrieve the role or list of roles
      	  	scopeElement = cborScope.get(1);
      	  	if (scopeElement.getType().equals(CBORType.TextString)) {
      	  		// Only one role is specified
      	  		roles.add(scopeElement.AsString());
      	  	}
      	  	else if (scopeElement.getType().equals(CBORType.Array)) {
      	  		// Multiple roles are specified
      	  		if (scopeElement.size() < 2) {
      	  			exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "The CBOR Array of roles must include at least two roles");
            		return;
      	  		}
      	  		for (int i=0; i<scopeElement.size(); i++) {
      	  			if (scopeElement.get(i).getType().equals(CBORType.TextString))
      	  				roles.add(scopeElement.get(i).AsString());
      	  			else {
      	  				exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "The CBOR Array of roles must include at least two roles");
      	        		return;
      	  			}
      	  		}
      	  	}
      	  	else {
      	  		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Invalid format of roles");
        		return;
      	  	}
        	
        	// Retrieve 'get_pub_keys'
        	// If present, this parameter must be an empty CBOR array
        	CBORObject getPubKeys = joinRequest.get(CBORObject.FromObject(Constants.GET_PUB_KEYS));
        	if (getPubKeys != null) {
        		
        		if (!getPubKeys.getType().equals(CBORType.Array) && getPubKeys.size() != 0) {
            		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "get_pub_keys must be an empty array");
            		return;
        		}
        		
        		providePublicKeys = true;
        		
        	}
        	
        	// The first 'groupIdPrefixSize' pairs of characters are the Group ID Prefix.
        	// This string is surely hexadecimal, since it passed the early check against the URI path to the join resource.
        	//String prefixStr = scopeStr.substring(0, 2 * groupIdPrefixSize);
        	//byte[] prefixByteStr = Util.hexStringToByteArray(prefixStr);
        	
        	// Retrieve the entry for the target group, using the group name
        	GroupInfo myGroup = activeGroups.get(groupName);
        	
        	// Assign a new Sender ID to the joining node.
        	// For the sake of testing, a particular Sender ID is used as known to be available.
            byte[] senderId = new byte[] { (byte) 0x25 };
        	myGroup.allocateSenderId(senderId);        	
        	
        	// Retrieve 'client_cred'
        	CBORObject clientCred = joinRequest.get(CBORObject.FromObject(Constants.CLIENT_CRED));
        	
        	if (clientCred == null) {
        	
        		// TODO: check if the client if joining the group only with the "Monitor" role.
        		
        		// TODO: check if the Group Manager already owns this client's public key, otherwise reply with 4.00
        		
        	}
        	else {
        		
        		if (!clientCred.getType().equals(CBORType.ByteString)) {
            		myGroup.deallocateSenderId(senderId);
            		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "client_cred must be byte string");
            		return;
        		}

        		// This assumes that the public key is a COSE Key
        		CBORObject coseKey = CBORObject.DecodeFromBytes(clientCred.GetByteString());
        		
        		if (!coseKey.getType().equals(CBORType.Map)) {
            		myGroup.deallocateSenderId(senderId);
            		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "the public key must be a COSE key");
            		return;
        		}
        		
        		// Check that a OneKey object can be correctly built
        		OneKey publicKey;
        		try {
        			publicKey = new OneKey(coseKey);
				} catch (CoseException e) {
				    System.err.println(e.getMessage());
					myGroup.deallocateSenderId(senderId);
					exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "invalid public key format");
            		return;
				}
        		        		        		
        		// Sanity check on the type of public key
        		// TODO: The "Bad Request" response should actually tell the joining node the exact algorithm and parameters
        		
        		if (myGroup.getCsAlg().equals(AlgorithmID.ECDSA_256) ||
        		    myGroup.getCsAlg().equals(AlgorithmID.ECDSA_384) ||
        		    myGroup.getCsAlg().equals(AlgorithmID.ECDSA_512)) {
        			
        			if (!publicKey.get(KeyKeys.KeyType).equals(KeyKeys.KeyType_EC2) ||
        				!publicKey.get(KeyKeys.EC2_Curve).equals(myGroup.getCsKeyParams().get(CBORObject.FromObject(KeyKeys.EC2_Curve.AsCBOR())))) {
        				
                			myGroup.deallocateSenderId(senderId);
                			exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "invalid public key format");
                			return;
                		
        			}
        		}
        		
        		if (myGroup.getCsAlg().equals(AlgorithmID.EDDSA)) {
        			
        			if (!publicKey.get(KeyKeys.KeyType).equals(myGroup.getCsKeyParams().get(CBORObject.FromObject(KeyKeys.KeyType.AsCBOR()))) ||
        				!publicKey.get(KeyKeys.OKP_Curve).equals(myGroup.getCsParams().get(CBORObject.FromObject(KeyKeys.OKP_Curve.AsCBOR()))) ||
        				!publicKey.get(KeyKeys.OKP_Curve).equals(myGroup.getCsKeyParams().get(CBORObject.FromObject(KeyKeys.OKP_Curve.AsCBOR())))) {
        				
                			myGroup.deallocateSenderId(senderId);
                			exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "invalid public key format");
                			return;
                		
        			}
        				
        		}
        		
        		// Retrieve the proof-of-possession nonce and signature from the Client
        		CBORObject cnonce = joinRequest.get(CBORObject.FromObject(Constants.CNONCE));
            	
            	if (cnonce == null) {
            		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "A client nonce must be included for proof-of-possession for joining OSCORE groups");
            		return;
            	}

            	if (!cnonce.getType().equals(CBORType.ByteString)) {
            		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "The client nonce must be wrapped in a binary string for joining OSCORE groups");
            		return;
                }
            	
            	byte[] rawCnonce = cnonce.GetByteString();
        		
        		// Check the proof-of-possession signature over (rsnonce | cnonce), using the Client's public key
            	CBORObject clientSignature = joinRequest.get(CBORObject.FromObject(Constants.CLIENT_CRED_VERIFY));
            	
            	if (clientSignature == null) {
            		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "A client signature must be included for proof-of-possession for joining OSCORE groups");
            		return;
            	}

            	if (!cnonce.getType().equals(CBORType.ByteString)) {
            		exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "The client signature must be wrapped in a binary string for joining OSCORE groups");
            		return;
                }
            	
            	byte[] rawClientSignature = clientSignature.GetByteString();
        		
            	PublicKey pubKey = null;
                try {
					pubKey = publicKey.AsPublicKey();
				} catch (CoseException e) {
					System.out.println(e.getMessage());
					exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR, "Failed to use the Client's public key to verify the PoP signature");
            		return;
				}
                if (pubKey == null) {
                	exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR, "Failed to use the Client's public key to verify the PoP signature");
            		return;
                }
                
            	byte[] dataToSign = new byte [rsnonce.length + rawCnonce.length];
           	    System.arraycopy(rsnonce, 0, dataToSign, 0, rsnonce.length);
           	    System.arraycopy(rawCnonce, 0, dataToSign, rsnonce.length, rawCnonce.length);
           	    
           	    int countersignKeyCurve = 0;
           	    
           	    if (publicKey.get(KeyKeys.KeyType).equals(KeyKeys.KeyType_EC2))
					countersignKeyCurve = publicKey.get(KeyKeys.EC2_Curve).AsInt32();
           	    else if (publicKey.get(KeyKeys.KeyType).equals(KeyKeys.KeyType_OKP))
					countersignKeyCurve = publicKey.get(KeyKeys.OKP_Curve).AsInt32();
           	    
           	    // This should never happen, due to the previous sanity checks
           	    if (countersignKeyCurve == 0) {
           	    	exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR, "error when setting up the signature verification");
            		return;
           	    }
           	    
           	    if (!verifySignature(countersignKeyCurve, pubKey, dataToSign, rawClientSignature)) {
					exchange.respond(CoAP.ResponseCode.BAD_REQUEST, "Invalid Client's PoP signature");
            		return;
           	    }
            	
            	
            	// Set the 'kid' parameter of the COSE Key equal to the Sender ID of the joining node
        		publicKey.add(KeyKeys.KeyId, CBORObject.FromObject(senderId));
        		
        		// Store this client's public key
        		if (!myGroup.storePublicKey(GroupInfo.bytesToInt(senderId), publicKey.AsCBOR())) {
        			myGroup.deallocateSenderId(senderId);
					exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR, "error when storing the public key");
            		return;
        			
        		}
        		
        	}
        	
            // Respond to the Join Request
            
        	CBORObject joinResponse = CBORObject.NewMap();
        	
        	// Key Type Value assigned to the Group_OSCORE_Security_Context object.
        	// NOTE: '0' is a temporary value.
        	joinResponse.Add(Constants.GKTY, CBORObject.FromObject(0));
        	
        	// This map is filled as the Group_OSCORE_Security_Context object, as defined in draft-ace-key-groupcomm-oscore
        	CBORObject myMap = CBORObject.NewMap();
        	
        	// Fill the 'key' parameter
        	myMap.Add(OSCORESecurityContextObjectParameters.ms, myGroup.getMasterSecret());
        	myMap.Add(OSCORESecurityContextObjectParameters.clientId, senderId);
        	myMap.Add(OSCORESecurityContextObjectParameters.hkdf, myGroup.getHkdf().AsCBOR());
        	myMap.Add(OSCORESecurityContextObjectParameters.alg, myGroup.getAlg().AsCBOR());
        	myMap.Add(OSCORESecurityContextObjectParameters.salt, myGroup.getMasterSalt());
        	myMap.Add(OSCORESecurityContextObjectParameters.contextId, myGroup.getGroupId());
        	myMap.Add(GroupOSCORESecurityContextObjectParameters.cs_alg, myGroup.getCsAlg().AsCBOR());
        	if (myGroup.getCsParams().size() != 0)
        		myMap.Add(GroupOSCORESecurityContextObjectParameters.cs_params, myGroup.getCsParams());
        	if (myGroup.getCsKeyParams().size() != 0)
        		myMap.Add(GroupOSCORESecurityContextObjectParameters.cs_key_params, myGroup.getCsKeyParams());
        	myMap.Add(GroupOSCORESecurityContextObjectParameters.cs_key_enc, myGroup.getCsKeyEnc());
        	        	
        	joinResponse.Add(Constants.KEY, myMap);
        	
        	// If backward security has to be preserved:
        	//
        	// 1) The Epoch part of the Group ID should be incremented
        	// myGroup.incrementGroupIdEpoch();
        	//
        	// 2) The OSCORE group should be rekeyed

        	// The current version of the symmetric keying material
        	joinResponse.Add(Constants.NUM, CBORObject.FromObject(myGroup.getVersion()));
        	
        	// CBOR Value assigned to the "coap_group_oscore_app" profile.
        	// NOTE: '0' is a temporary value.
        	joinResponse.Add(Constants.ACE_GROUPCOMM_PROFILE, CBORObject.FromObject(0));
        	
        	// Expiration time in seconds, after which the OSCORE Security Context
        	// derived from the 'k' parameter is not valid anymore.
        	joinResponse.Add(Constants.EXP, CBORObject.FromObject(1000000));
        	
        	// NOTE: this is currently skipping the inclusion of the optional parameter 'group_policies'.
        	if (providePublicKeys) {
        		
        		CBORObject coseKeySet = CBORObject.NewArray();
        		
        		for (Integer i : myGroup.getUsedSenderIds()) {
        			
        			// Skip the entry of the just-added joining node 
        			if (i.equals(GroupInfo.bytesToInt(senderId)))
        				continue;
        			
        			CBORObject coseKeyPeer = myGroup.getPublicKey(i);
        			coseKeySet.Add(coseKeyPeer);
        			
        		}
        		
        		if (coseKeySet.size() > 0) {
        			
        			byte[] coseKeySetByte = coseKeySet.EncodeToBytes();
        			joinResponse.Add(Constants.PUB_KEYS, CBORObject.FromObject(coseKeySetByte));
        			
        		}
        		
        		// Debug:
        		// 1) Print 'kid' as equal to the Sender ID of the key owner
        		// 2) Print 'kty' of each public key
        		/*
        		for (int i = 0; i < coseKeySet.size(); i++) {
        			byte[] kid = coseKeySet.get(i).get(KeyKeys.KeyId.AsCBOR()).GetByteString();
        			for (int j = 0; j < kid.length; j++)
        				System.out.printf("0x%02X", kid[j]);
        			System.out.println("\n" + coseKeySet.get(i).get(KeyKeys.KeyType.AsCBOR()));
        		}
        		*/
        		
        	}
        	
        	byte[] responsePayload = joinResponse.EncodeToBytes();
        	exchange.respond(ResponseCode.CREATED, responsePayload, MediaTypeRegistry.APPLICATION_CBOR);
        	
        }
    }
    
    private static AuthzInfoGroupOSCORE ai = null; // M.T.
    
    private static CoapServer rs = null;
    
    private static CoapDeliverer dpd = null;
    
    private static String rpk = "piJYILr/9Frrqur4bAz152+6hfzIG6v/dHMG+SK7XaC2JcEvI1ghAKryvKM6og3sNzRQk/nNqzeAfZsIGAYisZbRsPCE3s5BAyYBAiFYIIrXSWPfcBGeHZvB0La2Z0/nCciMirhJb8fv8HcOCyJzIAE=";
    
    
    /**
     * The CoAPs server for testing, run this before running the Junit tests.
     *  
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // install needed cryptography providers
        org.eclipse.californium.oscore.InstallCryptoProviders.installProvider();

        final String rootGroupMembershipResource = "group-oscore";
        final String groupName = "feedca570000";
        
        //Set up DTLSProfileTokenRepository
        Set<Short> actions = new HashSet<>();
        actions.add(Constants.GET);
        Map<String, Set<Short>> myResource = new HashMap<>();
        myResource.put("helloWorld", actions);
        Map<String, Map<String, Set<Short>>> myScopes = new HashMap<>();
        myScopes.put("r_helloWorld", myResource);
        
        Set<Short> actions2 = new HashSet<>();
        actions2.add(Constants.GET);
        Map<String, Set<Short>> myResource2 = new HashMap<>();
        myResource2.put("temp", actions2);
        myScopes.put("r_temp", myResource2);
        
        // M.T.
        // Adding the join resource, as one scope for each different combinations of
        // roles admitted in the OSCORE Group, with Group name "feedca570000".
        Set<Short> actions3 = new HashSet<>();
        actions3.add(Constants.POST);
        Map<String, Set<Short>> myResource3 = new HashMap<>();
        myResource3.put(rootGroupMembershipResource + "/" + groupName, actions3);
        myScopes.put(rootGroupMembershipResource + "/" + groupName + "_requester", myResource3);
        myScopes.put(rootGroupMembershipResource + "/" + groupName + "_responder", myResource3);
        myScopes.put(rootGroupMembershipResource + "/" + groupName + "_monitor", myResource3);
        myScopes.put(rootGroupMembershipResource + "/" + groupName + "_requester_responder", myResource3);
        myScopes.put(rootGroupMembershipResource + "/" + groupName + "_requester_monitor", myResource3);
        
        // M.T.
        // Adding another join resource, as one scope for each different combinations of
        // roles admitted in the OSCORE Group, with Group name "fBBBca570000".
        // There will NOT be a token enabling the access to this resource.
        Set<Short> actions4 = new HashSet<>();
        actions4.add(Constants.POST);
        Map<String, Set<Short>> myResource4 = new HashMap<>();
        myResource4.put("fBBBca570000", actions4);
        myScopes.put("fBBBca570000_requester", myResource4);
        myScopes.put("fBBBca570000_responder", myResource4);
        myScopes.put("fBBBca570000_monitor", myResource4);
        myScopes.put("fBBBca570000_requester_responder", myResource4);
        myScopes.put("fBBBca570000_requester_monitor", myResource4);
        
        // M.T. (for rs4)
        // Adding the join resource, as one scope for each different combinations of
        // roles admitted in the OSCORE Group, with Group name "feedca570000".
        Set<Short> actions5 = new HashSet<>();
        actions5.add(Constants.POST);
        Map<String, Set<Short>> myResource5 = new HashMap<>();
        myResource5.put(rootGroupMembershipResource + "/" + groupName, actions5);
        myScopes.put(rootGroupMembershipResource + "/" + groupName + "_requester", myResource5);
        myScopes.put(rootGroupMembershipResource + "/" + groupName + "_responder", myResource5);
        myScopes.put(rootGroupMembershipResource + "/" + groupName + "_monitor", myResource5);
        myScopes.put(rootGroupMembershipResource + "/" + groupName + "_requester_responder", myResource5);
        myScopes.put(rootGroupMembershipResource + "/" + groupName + "_requester_monitor", myResource5);
        
        // M.T. (for rs4)
        // Adding another join resource, as one scope for each different combinations of
        // roles admitted in the OSCORE Group, with Group name "fBBBca570000".
        // There will NOT be a token enabling the access to this resource.
        Set<Short> actions6 = new HashSet<>();
        actions6.add(Constants.POST);
        Map<String, Set<Short>> myResource6 = new HashMap<>();
        myResource6.put("fBBBca570000", actions6);
        myScopes.put("fBBBca570000_requester", myResource6);
        myScopes.put("fBBBca570000_responder", myResource6);
        myScopes.put("fBBBca570000_monitor", myResource6);
        myScopes.put("fBBBca570000_requester_responder", myResource6);
        myScopes.put("fBBBca570000_requester_monitor", myResource6);
        
        // M.T.
        Set<String> auds = new HashSet<>();
        auds.add("rs1"); // Simple test audience
        auds.add("rs2"); // OSCORE Group Manager (This audience expects scopes as Byte Strings)
        auds.add("rs4"); // OSCORE Group Manager (This audience expects scopes as Byte Strings)
        GroupOSCOREJoinValidator valid = new GroupOSCOREJoinValidator(auds, myScopes, rootGroupMembershipResource);
        
        // M.T.
        // Include this audience in the list of audiences recognized as OSCORE Group Managers 
        Set<String> GMs = new HashSet<>();
        GMs.add("rs2");
        GMs.add("rs4");
        valid.setGMAudiences(GMs);
        
        // M.T.
        // Include this resource as a join resource for Group OSCORE.
        // The resource name is the name of the OSCORE group.
        valid.setJoinResources(Collections.singleton(rootGroupMembershipResource + "/" + groupName));
        
    	// Create the OSCORE group
        final byte[] masterSecret = { (byte) 0x01, (byte) 0x02, (byte) 0x03, (byte) 0x04,
                					  (byte) 0x05, (byte) 0x06, (byte) 0x07, (byte) 0x08,
                					  (byte) 0x09, (byte) 0x0A, (byte) 0x0B, (byte) 0x0C,
                					  (byte) 0x0D, (byte) 0x0E, (byte) 0x0F, (byte) 0x10 };

        final byte[] masterSalt =   { (byte) 0x9e, (byte) 0x7c, (byte) 0xa9, (byte) 0x22,
                					  (byte) 0x23, (byte) 0x78, (byte) 0x63, (byte) 0x40 };

        // Group OSCORE specific values for the AEAD algorithm and HKDF
        final AlgorithmID alg = AlgorithmID.AES_CCM_16_64_128;
        final AlgorithmID hkdf = AlgorithmID.HKDF_HMAC_SHA_256;

        // Group OSCORE specific values for the countersignature
        AlgorithmID csAlg = null;
        Map<CBORObject, CBORObject> csParamsMap = new HashMap<>();
        Map<CBORObject, CBORObject> csKeyParamsMap = new HashMap<>();
        
        // Uncomment to set ECDSA with curve P-256 for countersignatures
        // int countersignKeyCurve = KeyKeys.EC2_P256.AsInt32();
        
        // Uncomment to set EDDSA with curve Ed25519 for countersignatures
        int countersignKeyCurve = KeyKeys.OKP_Ed25519.AsInt32();
        
        // ECDSA_256
        if (countersignKeyCurve == KeyKeys.EC2_P256.AsInt32()) {
        	csAlg = AlgorithmID.ECDSA_256;
        	csKeyParamsMap.put(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_EC2);        
        	csKeyParamsMap.put(KeyKeys.EC2_Curve.AsCBOR(), KeyKeys.EC2_P256);
        }
        
        // EDDSA (Ed25519)
        if (countersignKeyCurve == KeyKeys.OKP_Ed25519.AsInt32()) {
        	csAlg = AlgorithmID.EDDSA;
        	csParamsMap.put(KeyKeys.OKP_Curve.AsCBOR(), KeyKeys.OKP_Ed25519);
        	csKeyParamsMap.put(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_OKP);
        	csKeyParamsMap.put(KeyKeys.OKP_Curve.AsCBOR(), KeyKeys.OKP_Ed25519);
        }

        final CBORObject csParams = CBORObject.FromObject(csParamsMap);
        final CBORObject csKeyParams = CBORObject.FromObject(csKeyParamsMap);
        final CBORObject csKeyEnc = CBORObject.FromObject(Constants.COSE_KEY);
        
        final int senderIdSize = 1; // Up to 4 bytes

        // Prefix (4 byte) and Epoch (2 bytes) --- All Group IDs have the same prefix size, but can have different Epoch sizes
        // The current Group ID is: 0xfeedca57f05c, with Prefix 0xfeedca57 and current Epoch 0xf05c 
    	final byte[] groupIdPrefix = new byte[] { (byte) 0xfe, (byte) 0xed, (byte) 0xca, (byte) 0x57 };
    	byte[] groupIdEpoch = new byte[] { (byte) 0xf0, (byte) 0x5c }; // Up to 4 bytes
    	
    	GroupInfo myGroup = new GroupInfo(groupName,
    								      masterSecret,
    			                          masterSalt,
    			                          groupIdPrefixSize,
    			                          groupIdPrefix,
    			                          groupIdEpoch.length,
    			                          GroupInfo.bytesToInt(groupIdEpoch),
    			                          senderIdSize,
    			                          alg,
    			                          hkdf,
    			                          csAlg,
    			                          csParams,
    			                          csKeyParams,
    			                          csKeyEnc);
        
    	byte[] mySid;
    	OneKey myKey;
    	
    	
    	/*
    	// Generate a pair of asymmetric keys and print them in base 64 (whole version, then public only)
        
        OneKey testKey = null;
 		
 		if (countersignKeyCurve == KeyKeys.EC2_P256.AsInt32())
 			testKey = OneKey.generateKey(AlgorithmID.ECDSA_256);
    	
    	if (countersignKeyCurve == KeyKeys.OKP_Ed25519.AsInt32())
    		testKey = OneKey.generateKey(AlgorithmID.EDDSA);
        
    	byte[] testKeyBytes = testKey.EncodeToBytes();
    	String testKeyBytesBase64 = Base64.getEncoder().encodeToString(testKeyBytes);
    	System.out.println(testKeyBytesBase64);
    	
    	OneKey testPublicKey = testKey.PublicKey();
    	byte[] testPublicKeyBytes = testPublicKey.EncodeToBytes();
    	String testPublicKeyBytesBase64 = Base64.getEncoder().encodeToString(testPublicKeyBytes);
    	System.out.println(testPublicKeyBytesBase64);
    	*/
    	
    	
    	// Add a group member with Sender ID 0x52
    	mySid = new byte[] { (byte) 0x52 };
    	myGroup.allocateSenderId(mySid);	
    	
    	String rpkStr1 = "";
    	
    	// Store the public key of the group member with Sender ID 0x52 (ECDSA_256)
    	if (countersignKeyCurve == KeyKeys.EC2_P256.AsInt32())
    		rpkStr1 = "pSJYIF0xJHwpWee30/YveWIqcIL/ATJfyVSeYbuHjCJk30xPAyYhWCA182VgkuEmmqruYmLNHA2dOO14gggDMFvI6kFwKlCzrwECIAE=";
    	
    	// Store the public key of the group member with Sender ID 0x52 (EDDSA - Ed25519)
    	if (countersignKeyCurve == KeyKeys.OKP_Ed25519.AsInt32())
    		rpkStr1 = "pAMnAQEgBiFYIHfsNYwdNE5B7g6HuDg9I6IJms05vfmJzkW1Loh0Yzib";
    	
    	myKey = new OneKey(CBORObject.DecodeFromBytes(Base64.getDecoder().decode(rpkStr1)));
    	
    	// Set the 'kid' parameter of the COSE Key equal to the Sender ID of the owner
    	myKey.add(KeyKeys.KeyId, CBORObject.FromObject(mySid));
    	myGroup.storePublicKey(GroupInfo.bytesToInt(mySid), myKey.AsCBOR());
    	
    	
    	// Add a group member with Sender ID 0x77
    	mySid = new byte[] { (byte) 0x77 };
    	myGroup.allocateSenderId(mySid);
    	
    	String rpkStr2 = "";
    	
    	// Store the public key of the group member with Sender ID 0x77 (ECDSA_256)
    	if (countersignKeyCurve == KeyKeys.EC2_P256.AsInt32())
    		rpkStr2 = "pSJYIHbIGgwahy8XMMEDF6tPNhYjj7I6CHGei5grLZMhou99AyYhWCCd+m1j/RUVdhRgt7AtVPjXNFgZ0uVXbBYNMUjMeIbV8QECIAE=";
    	
    	// Store the public key of the group member with Sender ID 0x77 (EDDSA - Ed25519)
    	if (countersignKeyCurve == KeyKeys.OKP_Ed25519.AsInt32())
    		rpkStr2 = "pAMnAQEgBiFYIBBbjGqMiAGb8MNUWSk0EwuqgAc5nMKsO+hFiEYT1bou";
    	
    	myKey = new OneKey(CBORObject.DecodeFromBytes(Base64.getDecoder().decode(rpkStr2)));
    	
    	// Set the 'kid' parameter of the COSE Key equal to the Sender ID of the owner
    	myKey.add(KeyKeys.KeyId, CBORObject.FromObject(mySid));
    	myGroup.storePublicKey(GroupInfo.bytesToInt(mySid), myKey.AsCBOR()); 	
    	
    	
    	// Add this OSCORE group to the set of active groups
    	// If the groupIdPrefix is 4 bytes in size, the map key can be a negative integer, but it is not a problem
    	activeGroups.put(groupName, myGroup);
    	
    	String tokenFile = TestConfig.testFilePath + "tokens.json";
    	//Delete lingering old token files
    	new File(tokenFile).delete();
              
        byte[] key128a 
            = {'c', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
      
        OneKey asymmetric = new OneKey(CBORObject.DecodeFromBytes(
                Base64.getDecoder().decode(rpk)));
        
        //Set up COSE parameters
        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, 
                AlgorithmID.AES_CCM_16_128_128, AlgorithmID.Direct);
        CwtCryptoCtx ctx 
            = CwtCryptoCtx.encrypt0(key128a, coseP.getAlg().AsCBOR());

        
        // Set up the inner Authz-Info library
        ai = new AuthzInfoGroupOSCORE(Collections.singletonList(AS_NAME), 
        	 new KissTime(), null, valid, ctx, tokenFile, valid, false);

        // Provide the authz-info endpoint with the set of active OSCORE groups
        ai.setActiveGroups(activeGroups);
      
        // M.T.
        // The related test in TestDtlspClientGroupOSCORE still works with this server even with a single
        // AuthzInfoGroupOSCORE 'ai', but only because 'ai' is constructed with a null Introspection Handler.
        // 
        // If provided, a proper Introspection Handler would require to take care of multiple audiences,
        // rather than of a single RS as IntrospectionHandler4Tests does. This is already admitted in the
        // Java interface IntrospectionHandler.
      
        //Add a test token to authz-info
        byte[] key128 = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.SCOPE, CBORObject.FromObject("r_temp"));
        params.put(Constants.AUD, CBORObject.FromObject("rs1"));
        params.put(Constants.CTI, CBORObject.FromObject(
                   "token1".getBytes(Constants.charset)));
        params.put(Constants.ISS, CBORObject.FromObject(AS_NAME));

        OneKey key = new OneKey();
        key.add(KeyKeys.KeyType, KeyKeys.KeyType_Octet);
      
        byte[] kid  = new byte[] {0x01, 0x02, 0x03};
        CBORObject kidC = CBORObject.FromObject(kid);
        key.add(KeyKeys.KeyId, kidC);
        key.add(KeyKeys.Octet_K, CBORObject.FromObject(key128));

        CBORObject cnf = CBORObject.NewMap();
        cnf.Add(Constants.COSE_KEY_CBOR, key.AsCBOR());
        params.put(Constants.CNF, cnf);
        CWT token = new CWT(params);
        ai.processMessage(new LocalMessage(0, null, null, token.encode(ctx)));
      
  	    AsRequestCreationHints asi 
  	    	= new AsRequestCreationHints("coaps://blah/authz-info/", null, false, false);
  	    Resource hello = new HelloWorldResource();
  	    Resource temp = new TempResource();
  	    Resource join = new GroupOSCOREJoinResource(groupName); // M.T.
  	    Resource authzInfo = new CoapAuthzInfoGroupOSCORE(ai);
  	    Resource groupOSCORERootMembership = new GroupOSCORERootMembershipResource(rootGroupMembershipResource); // M.T.
      
  	    rs = new CoapServer();
  	    rs.add(hello);
  	    rs.add(temp);
  	    rs.add(groupOSCORERootMembership); // M.T.
  	  	groupOSCORERootMembership.add(join);
  	    rs.add(authzInfo);
      
  	    dpd = new CoapDeliverer(rs.getRoot(), null, asi); 

      
  	    DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder()
              .setAddress(
                      new InetSocketAddress(SECURE_PORT));
  	    config.setSupportedCipherSuites(new CipherSuite[]{
               CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8,
               CipherSuite.TLS_PSK_WITH_AES_128_CCM_8});
  	    config.setRpkTrustAll();
  	    DtlspPskStoreGroupOSCORE psk = new DtlspPskStoreGroupOSCORE(ai);
  	    config.setPskStore(psk);
  	    config.setIdentity(asymmetric.AsPrivateKey(), asymmetric.AsPublicKey());
  	    config.setClientAuthenticationRequired(true);
  	    DTLSConnector connector = new DTLSConnector(config.build());
  	    CoapEndpoint cep = new Builder().setConnector(connector)
               .setNetworkConfig(NetworkConfig.getStandard()).build();
  	    rs.addEndpoint(cep);
  	    //Add a CoAP (no 's') endpoint for authz-info
  	    CoapEndpoint aiep = new Builder().setInetSocketAddress(
               new InetSocketAddress(PORT)).build();
  	    rs.addEndpoint(aiep);
  	    rs.setMessageDeliverer(dpd);
  	    rs.start();
  	    System.out.println("Resource Server (GM) starting on port " + aiep.getAddress().getPort() +
                " and " + cep.getAddress().getPort() + " (DTLS)");
    }

    /**
     * Stops the server
     * 
     * @throws IOException 
     * @throws AceException 
     */
    public static void stop() throws IOException, AceException {
        rs.stop();
        ai.close();
        new File(TestConfig.testFilePath + "tokens.json").delete();
    }

    
	/**
	 * Verify the correctness of a digital signature
	 * 
	 * @param countersignKeyCurve Elliptic curve used to process the signature, encoded as in RFC 8152
	 * @param pubKey Public key of the signer, used to verify the signature
	 * @param signedData Data over which the signature has been computed
	 * @param expectedSignatureSignature to verify
	 * @return True is the signature verifies correctly, false otherwise
	 */
    public static boolean verifySignature(int countersignKeyCurve, PublicKey pubKey, byte[] signedData, byte[] expectedSignature) {

    	Signature mySignature = null;
    	boolean success = false;
    	
        try {
      	   if (countersignKeyCurve == KeyKeys.EC2_P256.AsInt32())
    	   	   		mySignature = Signature.getInstance("SHA256withECDSA");
      	   else if (countersignKeyCurve == KeyKeys.OKP_Ed25519.AsInt32())
       			mySignature = Signature.getInstance("NonewithEdDSA", "EdDSA");
     	   else {
     		   // At the moment, only ECDSA (EC2_P256) and EDDSA (Ed25519) are supported
     		  Assert.fail("Unsupported signature algorithm");
     	   }
             
         }
         catch (NoSuchAlgorithmException e) {
             System.out.println(e.getMessage());
             Assert.fail("Unsupported signature algorithm");
         }
         catch (NoSuchProviderException e) {
             System.out.println(e.getMessage());
             Assert.fail("Unsopported security provider for signature computing");
         }
         
         try {
             if (mySignature != null)
                 mySignature.initVerify(pubKey);
             else
                 Assert.fail("Signature algorithm has not been initialized");
         }
         catch (InvalidKeyException e) {
             System.out.println(e.getMessage());
             Assert.fail("Invalid key excpetion - Invalid public key");
         }

         try {
        	 if (mySignature != null) {
	             mySignature.update(signedData);
	             success = mySignature.verify(expectedSignature);
        	 }
         } catch (SignatureException e) {
             System.out.println(e.getMessage());
             Assert.fail("Failed signature verification");
         }

         return success;

    }

}
