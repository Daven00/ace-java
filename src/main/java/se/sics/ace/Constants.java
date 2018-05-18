/*******************************************************************************
 * Copyright (c) 2017, RISE SICS AB
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
package se.sics.ace;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;

/**
 * Constants for use with the ACE framework.
 * 
 * @author Ludwig Seitz
 *
 */
public class Constants {
	
    /**
     * Charset for this library
     */
    public static Charset charset = Charset.forName("UTF-8");
    
	/** 
	 * General OAuth related abbreviations
	 */
    
    /**
     * The issuer of an access token
     */
	public static final short ISS = 1; // Major type 3 (text string)
	
	/**
	 * The subject of an access token
	 */
	public static final short SUB = 2; //3
	
	/**
	 * The audience of an access token
	 */
	public static final short AUD = 3; //3
	
	/**
	 * The expiration time of an access token
	 * (in Epoch time)
	 */
	public static final short EXP = 4; // MT 6 tag 1 (Epoch-based date/time)
	
	/**
	 * The "not before" time of an access token (in Epoch time)
	 */
	public static final short NBF = 5; // 6t1
	
	/**
	 * The time an access token was issues (in Epoch time)
	 */
	public static final short IAT = 6; // 6t1
	
	/**
	 * The access token identifier
	 */
	public static final short CTI = 7; // Major type 2 (byte string)
	
	/**
	 * The client identifier in a token request
	 */
	public static final short CLIENT_ID = 8; //3
	
	/**
	 * The client password in a token request for certain grant types
	 */
	public static final short CLIENT_SECRET = 9; //2
	
	/**
	 * The response type (see 
	 * https://www.iana.org/assignments/oauth-parameters/oauth-parameters.xhtml#endpoint)
	 */
	public static final short RESPONSE_TYPE = 10; //3
	
	/**
	 * The redirect URI
	 */
	public static final short REDIRECT_URI = 11; //3
	
	/**
	 * The scope of an access token
	 */
	public static final short SCOPE = 12; //3
	
	/**
	 * An opaque value used by the client to maintain
     *    state between the request and callback. 
	 */
	public static final short STATE = 13; //3
	
	/**
	 * The authorization code generated by the
     *   authorization server.
	 */
	public static final short CODE = 14; //2
	
	/**
	 * The error code
	 */
	public static final short ERROR = 15; //
	
	/**
	 * Human-readable ASCII text providing
     *    additional information on an error
	 */
	public static final short ERROR_DESCRIPTION = 16; //3
	
	/**
	 * A URI identifying a human-readable web page with
     * information about the error,
	 */
	public static final short ERROR_URI = 17; //3
	
	/**
	 * The grant type (e.g. "client_credentials")
	 */
	public static final short GRANT_TYPE = 18; // Major type 0 (uint)
	
	/**
	 * The access token
	 */
	public static final short ACCESS_TOKEN = 19; //
	
	/**
	 * The type of the access token, e.g. "pop" or "bearer"
	 */
	public static final short TOKEN_TYPE = 20; // 0
	
	/**
	 * The time when this token expires (in Epoch time)
	 */
	public static final short EXPIRES_IN = 21; // 0
	
	/**
	 * The username, for a username/password grant
	 */
	public static final short USERNAME = 22; //3
	
	/**
	 * The password, for a username/password grant
	 */
	public static final short PASSWORD = 23; //3
	
	/**
	 * The refresh token
	 */
	public static final short REFRESH_TOKEN = 24; //3
	
	/**
	 * The confirmation key for the proof-of-possession
	 */
	public static final short CNF = 25; // Major type 5 (map)
	
	/**
	 * The profile to be used between client and RS
	 */
	public static final short PROFILE = 26; //0
	
	/**
	 * The token in an introspection request
	 */
	public static final short TOKEN = 27; // 3
	
	/**
	 * A hint for the AS about the type of token in an introspection request
	 */
	public static final short TOKEN_TYPE_HINT = 28; //3 
	
	/**
	 * A boolean indicating if a token is active in an introspection response
	 */
	public static final short ACTIVE = 29; // boolean

	
	/**
	 * Information about the key that the RS uses to
     * authenticate towards the client.
	 */
	public static final short RS_CNF = 30; //5
	
	/**
	 * CWT claims
	 */
	public static final short[] CWT_CLAIMS 
		= {ISS, SUB, AUD, EXP, NBF, IAT, CTI, SCOPE};
	
	/**
	 * ACE-OAUTH-AUTHZ /token parameters
	 */
	public static final short[] TOKEN_PAR = {CLIENT_ID, CLIENT_SECRET, AUD, 
		RESPONSE_TYPE, REDIRECT_URI, SCOPE, STATE, CODE, ERROR, 
		ERROR_DESCRIPTION,ERROR_URI, GRANT_TYPE, ACCESS_TOKEN, TOKEN_TYPE, 
		EXPIRES_IN, USERNAME, PASSWORD, REFRESH_TOKEN, CNF, PROFILE};

   /**
    * ACE-OAUTH-AUTHZ /introspect parameters
    */
	public static final short[] INTROSPECT_PAR = {ACTIVE, USERNAME, CLIENT_ID,
	        SCOPE, ERROR, ERROR_DESCRIPTION, ERROR_URI, TOKEN_TYPE, EXP, IAT,
	        NBF, SUB, AUD, ISS, CTI, CNF, RS_CNF};
	
	/**
	 * Abbreviations for OAuth error codes
	 */
	
	/**
	 * The request is missing a required parameter, includes an
     * unsupported parameter value (other than grant type),
     * repeats a parameter, includes multiple credentials,
     * utilizes more than one mechanism for authenticating the
     * client, or is otherwise malformed.
	 */
	public static final short INVALID_REQUEST = 0;
	
	/**
	 * Client authentication failed
	 */
	public static final short INVALID_CLIENT = 1;
	
	/**
	 * The provided authorization grant or refresh token is
     * invalid, expired, revoked, does not match the redirection
     * URI used in the authorization request, or was issued to
     * another client.
     */	
	public static final short INVALID_GRANT = 2;
	
	/**
	 * The authenticated client is not authorized to use this
     * authorization grant type.
	 */
	public static final short UNAUTHORIZED_CLIENT = 3;
	
	/**
	 *  The authorization grant type is not supported by the
     *  authorization server.
	 */
	public static final short UNSUPPORTED_GRANT_TYPE = 4;
	
	/**
	 * The requested scope is invalid, unknown, malformed, or
     * exceeds the scope granted by the resource owner.
	 */
	public static final short INVALID_SCOPE = 5;
	
	/**
	 * The RS does not support the requestest pop key type
	 */
	public static final short UNSUPPORTED_POP_KEY = 6;
	
	/**
     * The string values for these abbreviations
     */
    public static final String[] ERROR_CODES 
        = {"invalid_request", "invalid_client", "invalid_grant", 
                "unauthorized_client", "unsupported_grant_type", 
                "invalid_scope", "unsupported_pop_key"};
    
	/**
	 * Abbreviations for OAuth grant types
	 */
	
	/**
	 * grant type password	
	 */
	public static final short GT_PASSWORD = 0;
	
	/**
	 * grant type authorization code
	 */
	public static final short GT_AUTHZ_CODE = 1;
	
	/**
	 * grant type client credentials
	 */
	public static final short GT_CLI_CRED = 2;
	
	/**
	 * grant type refresh token
	 */
	public static final short GT_REF_TOK = 3;
	
	/**
	 * RESTful action names
	 */
	public static final String[] RESTACTIONS 
	    = {"GET", "POST", "PUT", "DELETE"};

	
	/**
	 * Abbreviations for the cnf parameter/claim
	 */
	
	/**
	 * A cnf containing a COSE_Key
	 */
	public static final short COSE_KEY = 1;
	
    /**
     * ... same as above as CBORObject
     */
    public static final CBORObject COSE_KEY_CBOR 
        = CBORObject.FromObject(COSE_KEY);	
	
	/**
	 * A cnf containing a COSE_Encrypted wrapping a COSE_Key
	 */
	public static final short COSE_ENCRYPTED = 2;
	
	/**
     * ... same as above as CBORObject
     */
    public static final CBORObject COSE_ENCRYPTED_CBOR 
        = CBORObject.FromObject(COSE_ENCRYPTED);
	
	/**
	 * A cnf containing just a key identifier
	 */
	public static final short COSE_KID = 3;
	
    /**
     * ... same as above as CBORObject
     */
    public static final CBORObject COSE_KID_CBOR 
        = CBORObject.FromObject(COSE_KID);
    

    /**
     * Searches an array of strings for the index of the given string.
     * @param array  an array of Strings
     * @param val  a String value
     * @return  the index of val in array
     */
    public static short getIdx(String[] array, String val) {
        if (val == null || array == null) {
            return -1;
        }
        for (short i=0; i<array.length; i++) {
            if (val.equals(array[i])) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Takes a CBORObject that is a map and transforms it
     * into Map<Short, CBORObject>
     * @param cbor  the CBOR map
     * @return  the Map
     * @throws AceException if the cbor parameter is not a CBOR map or
     *  if a key is not a short
     */
    public static Map<Short, CBORObject> getParams(CBORObject cbor) 
            throws AceException {
        if (!cbor.getType().equals(CBORType.Map)) {
            throw new AceException("CBOR object is not a Map"); 
        }
        Map<Short, CBORObject> ret = new HashMap<>();
        for (CBORObject key : cbor.getKeys()) {
            if (!key.getType().equals(CBORType.Number)) {
                throw new AceException("CBOR key was not a Short: "
                        + key.toString());
            }
            ret.put(key.AsInt16(), cbor.get(key));
        }
        return ret;
    }
    
    /**
     * Takes a  Map<Short, CBORObject> and transforms it into a CBOR map.
     * 
     * @param map  the map
     * @return  the CBOR map
     */
    public static CBORObject getCBOR(Map<Short, CBORObject> map) {
        CBORObject cbor = CBORObject.NewMap();
        for (Map.Entry<Short, CBORObject> e : map.entrySet()) {
            cbor.Add(e.getKey(), e.getValue());
        }
        return cbor;
    }

    /**
     * Array of String values for the abbreviations (use for debugging)
     */
    public static final String[] ABBREV = {"", "iss", "sub", "aud", "exp", 
        "nbf", "iat", "cti", "client_id", "client_secret", "response_type",
        "redirect_uri", "scope", "state", "code", "error", "error_description", 
        "error_uri", "grant_type", "access_token", "token_type", "expires_in",
        "username", "password", "refresh_token", "cnf", "profile", "token",
        "token_type_hint", "active", "rs_cnf"};
    
    /**
     * The string values for the grant type abbreviations (use for debugging)
     */
    public static final String[] GRANT_TYPES = {"password", 
            "authorization_code", "client_credentials", "refresh_token"};


    /**
     * The abbreviation code for the DTLS profile
     */
    public static short COAP_DTLS = 1;
    
    /**
     * The abbreviation code for the OSCORE profile
     */
    public static short COAP_OSCORE = 2;
    
    /**
     * Return the abbreviated profile id for the full profile name.
     * 
     * @param profileStr  profile name
     * @return  the abbreviation
     */
    public static short getProfileAbbrev(String profileStr) {
        if (profileStr.equals("coap_dtls")) {
            return COAP_DTLS;
        } else if (profileStr.equals("coap_oscore")) {
            return COAP_OSCORE;
        } else {
            return -1;
        }
    }
    
    /**
     * Maps a parameter map to the unabbreviated version.
     * 
     * @param map
      * @return  the unabbreviated version of the map
      * @throws AceException  if map is not a CBOR map
     */
    public static Map<String, CBORObject> unabbreviate(CBORObject map) 
            throws AceException {
        if (!map.getType().equals(CBORType.Map)) {
            throw new AceException("Parameter is not a CBOR map");
        }
        Map<String, CBORObject> ret = new HashMap<>();
        for (CBORObject key : map.getKeys()) {
            String keyStr = null;
            CBORObject obj = map.get(key);
            if (key.isIntegral()) {
                short keyInt = key.AsInt16();
                if (keyInt > 0 && keyInt < Constants.ABBREV.length) {
                   keyStr = Constants.ABBREV[keyInt];
                    if (keyInt == GRANT_TYPE
                            && map.get(key).getType().equals(CBORType.Number)) {
                        obj = CBORObject.FromObject(GRANT_TYPES[obj.AsInt32()]);
                    } else if (keyInt == ERROR
                            && map.get(key).getType().equals(CBORType.Number)) {
                        obj = CBORObject.FromObject(ERROR_CODES[obj.AsInt32()]);
                    }                   
                } else {
                    throw new AceException("Malformed parameter map");
                }
            } else if (key.getType().equals(CBORType.TextString)) {
                keyStr = key.AsString();
            } else {
                throw new AceException("Malformed parameter map");
            }
            ret.put(keyStr, obj);
        }
       return ret;
    }
    
    /**
     * Representation of GET
     */
    public static short GET = 1;
    
    /**
     *  Representation of POST
     */
    public static short POST = 2;
    
    /**
     *  Representation of PUT
     */
    public static short PUT = 3;
    
    /**
     *  Representation of DELETE
     */
    public static short DELETE = 4;
    
    /**
     * Representation of FETCH
     */
    public static short FETCH = 5;
    
    /**
     * Representation of PATCH
     */
    public static short PATCH = 6;
    
    /**
     * Representation of iPATCH
     */
    public static short iPATCH = 7;
    
    
    
}