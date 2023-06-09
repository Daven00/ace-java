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
package se.sics.ace;

import java.util.Map;
import java.util.Set;

import org.eclipse.californium.core.coap.CoAP;

import com.upokecenter.cbor.CBORObject;

/**
 * A protocol message for either /token, /introspect, /authz-info, or /trl.
 * This abstract class is meant to be protocol independent, classes that implement
 * concrete instances could e.g. represent a CoAP message. 
 * Messages are expected to have a Map of parameters (which may be empty).
 * 
 * @author Ludwig Seitz and Marco Rasori
 *
 */
public interface Message {
	
    /**
     * Request has been fulfilled, resulting in the creation of a new resource.
     */
    public static int CREATED = CoAP.ResponseCode.CREATED.value;

	/**
	 * Request has been fulfilled, resulting in a resource transferred in the
	 * payload of the response
	 */
	public static int CONTENT = CoAP.ResponseCode.CONTENT.value;
    
	/**
	 * Generic failure reasons code (following REST/HTTP/COAP).
	 */
	public static int FAIL_BAD_REQUEST = CoAP.ResponseCode.BAD_REQUEST.value;
	
	/**
	 * Request was not authorized, the requester should try to authenticate
	 */
	public static int FAIL_UNAUTHORIZED = CoAP.ResponseCode.UNAUTHORIZED.value;
	
	/**
	 * Requester lacks permission to perform this request
	 */
	public static int FAIL_FORBIDDEN = CoAP.ResponseCode.FORBIDDEN.value;
	
	/**
	 * Requested resource was not found
	 */
	public static int FAIL_NOT_FOUND = CoAP.ResponseCode.NOT_FOUND.value;
	
	/**
	 * The requested operation on the resource is not allowed for this
	 * 	requester
	 */ 
	public static int FAIL_METHOD_NOT_ALLOWED 
	    = CoAP.ResponseCode.METHOD_NOT_ALLOWED.value;
	
	/**
	 * The responder cannot generate acceptable data format in the response
	 */
	public static int FAIL_NOT_ACCEPTABLE 
	    =  CoAP.ResponseCode.NOT_ACCEPTABLE.value;
	
	/**
	 * The request contained payload in a unsupported data format
	 */
	public static int FAIL_UNSUPPORTED_CONTENT_FORMAT 
	    =  CoAP.ResponseCode.UNSUPPORTED_CONTENT_FORMAT.value;
	
	/**
	 * The server had some internal problem
	 */
	public static int FAIL_INTERNAL_SERVER_ERROR 
	    = CoAP.ResponseCode.INTERNAL_SERVER_ERROR.value;
	
	/**
	 * The server doesn't implement some part required for this request
	 */
	public static int FAIL_NOT_IMPLEMENTED 
	    = CoAP.ResponseCode.NOT_IMPLEMENTED.value;

	/**
	 * @return  the success/failure code
	 */
	public int getMessageCode();
	
	
	/**
	 * @return  the raw bytes of the payload, null if the payload is empty.
	 */ 
	public byte[] getRawPayload();
	
	/**
	 * @return  The senders identity. This is assumed to have been authenticated by a lower
	 * 	level protocol. Null if we don't have an authenticated identity
	 */
	public String getSenderId();
	
	/**
	 * @return  a set of the parameter names (abbreviated), 
	 *     null if the message does not have a parameter map in the payload. 
	 */
	public Set<Short> getParameterNames();
	
	/**
     * Returns a parameter, or null if the parameter does not exist.
     * 
	 * @param name  the name abbreviation of the parameter
	 * @return  the parameter value or null if it doesn't exist or the 
	 *     message does not have a parameter map in the payload.
	 */
	public CBORObject getParameter(Short name);
	
	/**
	 * @return  the <code>Map</code> of parameters for this message or
	 *     null if the message does not have a parameter map in the    
	 *     payload.  This MUST provide the unabbreviated parameter names.
	 */
	public Map<Short, CBORObject> getParameters();
	
	/**
	 * Generate a reply message indicating success.
	 * 
	 * @param code  the success code
	 * @param payload  the payload of the reply, can be null.
	 * 
	 * @return  the reply message or null if the implementing class does not 
     *     support generating messages
	 */
	public abstract Message successReply(int code, CBORObject payload);
	
	/**
	 * Generate a reply message indicating failure.
	 * 
	 * @param failureReason  the failure reason code.
	 * @param payload  the payload of the reply, can be null.
	 * 
	 * @return  the reply message or null if the implementing class does not 
	 *     support generating messages
	 */
	public abstract Message failReply(int failureReason, CBORObject payload);

}
