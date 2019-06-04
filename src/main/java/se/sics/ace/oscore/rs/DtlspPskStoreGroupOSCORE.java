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
package se.sics.ace.oscore.rs;

import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.logging.Logger;

import org.eclipse.californium.scandium.dtls.PskPublicInformation;
import org.eclipse.californium.scandium.dtls.pskstore.PskStore;
import org.eclipse.californium.scandium.util.ServerNames;

import com.upokecenter.cbor.CBORException;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;

import COSE.KeyKeys;
import COSE.OneKey;

import se.sics.ace.AceException;
import se.sics.ace.Constants;
import se.sics.ace.Message;
import se.sics.ace.coap.rs.dtlsProfile.DtlspPskStore;
import se.sics.ace.examples.LocalMessage;
import se.sics.ace.rs.TokenRepository;

/**
 * A store for Pre-Shared-Keys (PSK) at the RS.
 * 
 * Implements the retrieval of the access token as defined in section 4.1. of 
 * draft-gerdes-ace-dtls-authorize.
 * 
 * @author Ludwig Seitz and Marco Tiloca
 *
 */
public class DtlspPskStoreGroupOSCORE implements PskStore {
    
    /**
     * The logger
     */
    private static final Logger LOGGER 
        = Logger.getLogger(DtlspPskStore.class.getName());
    
    
    /**
     * This component needs to access the authz-info endpoint.
     */
    private AuthzInfoGroupOSCORE authzInfo;
        
    
    /**
     * Constructor.
     * 
     * @param authzInfo  the authz-info used by this RS
     */
    public DtlspPskStoreGroupOSCORE(AuthzInfoGroupOSCORE authzInfo) {
        this.authzInfo = authzInfo;
    }


    @Override
    public byte[] getKey(PskPublicInformation identity) {
        return getKey(identity.getPublicInfoAsString());
    }
    
    /**
     * Avoid having to refactor all my code since the CF people decided they needed to change public APIs
     * 
     * @param identity  the String identity of the key
     * @return  the bytes of the key
     */
    private byte[] getKey(String identity) {
        if (TokenRepository.getInstance() == null) {
            LOGGER.severe("TokenRepository not initialized");
            return null;
        }
        //First try if we have that key
        OneKey key = null;
        try {
            key = TokenRepository.getInstance().getKey(identity);
            if (key != null) {
                return key.get(KeyKeys.Octet_K).GetByteString();
            }
        } catch (AceException e) {
            LOGGER.severe("Error: " + e.getMessage());
            return null;
        }          

        //We don't have that key, try if the identity is an access token
        CBORObject payload = null;
        try {
            payload = CBORObject.DecodeFromBytes(
                    Base64.getDecoder().decode(identity));
        } catch (NullPointerException | CBORException e) {
            LOGGER.severe("Error decoding the psk_identity: " 
                    + e.getMessage());
            return null;
        }
        
        //We may have an access token, continue processing it        
        LocalMessage message = new LocalMessage(0, identity, null, payload);
        LocalMessage res
            = (LocalMessage)this.authzInfo.processMessage(message);
        if (res.getMessageCode() == Message.CREATED) {
            CBORObject resPayl = CBORObject.DecodeFromBytes(res.getRawPayload());
            if (!resPayl.getType().equals(CBORType.Map)) {
                LOGGER.severe("Authz-Info returned non-CBOR-Map payload");
                return null;
            }
            //Note that this is either the token's cti or the internal
            //id that the AuthzInfo endpoint assigned to it 
            CBORObject cti = resPayl.get(CBORObject.FromObject(Constants.CTI));
            String ctiStr = Base64.getEncoder().encodeToString(
                    cti.GetByteString());
            try {
                 key = TokenRepository.getInstance().getPoP(ctiStr);
                 return key.get(KeyKeys.Octet_K).GetByteString();
            } catch (AceException e) {
                LOGGER.severe("Error: " + e.getMessage());
                return null;
            }
        }
        LOGGER.severe("Error: Token in psk_identity not valid");  
        return null;
    }

    @Override
    public PskPublicInformation getIdentity(InetSocketAddress inetAddress) {
        // Not needed here, this PskStore is for servers only
        return null;
    }

    @Override
    public byte[] getKey(ServerNames serverNames, PskPublicInformation identity) {
        //XXX: No support for ServerNames extension yet
        return getKey(identity);
    }


    @Override
    public PskPublicInformation getIdentity(InetSocketAddress peerAddress,
            ServerNames virtualHost) {
        // XXX: No support for ServerNames extension yet
        return null;
    }
}
