/*******************************************************************************
 * Copyright (c) 2018 RISE SICS and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Rikard Höglund (RISE SICS)
 *    
 ******************************************************************************/
package org.eclipse.californium.oscore;

import javax.xml.bind.DatatypeConverter;

import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.OneKey;
import org.eclipse.californium.oscore.GroupOSCoreCtx.RecipientCtx;

/**
 * 
 * Support functions for debugging and interop client/server.
 * FIXME: Use org.eclipse.californium.core.Utils.toHexText() instead?
 *
 */
public class Utility {
	
	/**
	 * Set debug levels.
	 * Normal DEBUG will print key information.
	 * DETAILED_DEBUG will also print information per messages sent like AAD.
	 */
	private final static boolean DEBUG = true;
	public final static boolean DETAILED_DEBUG = true;
	
	/*
	 * Method for printing a byte array as hexadecimal
	 * 
	 * 
	 */
	public static String arrayToString(byte[] array) {
		StringBuilder s = new StringBuilder();
		
		if(array == null) {
			s.append("null");
			return s.toString();
		}

		s.append(DatatypeConverter.printHexBinary(array));
		s.append(" (" + array.length + " bytes)");
		
		return s.toString();
	}
	
	/*
	 * Method for printing the currently used key information.
	 * 
	 * 
	 */
	public static void printContextInfo(GroupOSCoreCtx ctx) {
		if(!DEBUG) {
			return;
		}
		
		
		byte[] master_secret, master_salt, common_iv, id_context;
		byte[] sender_id, sender_key;
		int sender_seq_number;
		//byte[] recipient_id, recipient_key;	
		OneKey sender_private_key;
		Integer par_countersign;
		AlgorithmID alg_countersign;
		int countersign_length;
		
		//Common context
		master_secret = ctx.getMasterSecret();
		master_salt = ctx.getSalt();
		common_iv = ctx.getCommonIV();
		id_context = ctx.getIdContext();
		
		//Sender context
		sender_id = ctx.getSenderId();
		sender_key = ctx.getSenderKey();
		sender_seq_number = ctx.getSenderSeq();
		
		//New from Group OSCORE
		sender_private_key = ctx.getSenderPrivateKey();
		par_countersign = ctx.getParCountersign();
		alg_countersign = ctx.getAlgCountersign();
		
		//Extra for convenience
		countersign_length = ctx.getCountersignLength();
		
		System.out.println("Common Context:");
		System.out.print("\tMaster Secret: ");
		System.out.println(arrayToString(master_secret));
		System.out.print("\tMaster Salt: ");
		System.out.println(arrayToString(master_salt));
		System.out.print("\tCommon IV: ");
		System.out.println(arrayToString(common_iv));
		System.out.print("\tID Context: ");
		System.out.println(arrayToString(id_context));
		
		System.out.println("Sender Context:");
		System.out.print("\tSender ID: ");
		System.out.println(arrayToString(sender_id));
		System.out.print("\tSender Key: ");
		System.out.println(arrayToString(sender_key));
		System.out.print("\tSender Seq Number: ");
		System.out.println(sender_seq_number);
		
		System.out.println("Further:");
		System.out.print("\tSender Private Key: ");
		if(sender_private_key != null) {
			byte[] keyObjectBytes = sender_private_key.AsCBOR().EncodeToBytes();
			String sender_private_key_base64 = DatatypeConverter.printBase64Binary(keyObjectBytes);
			System.out.println(sender_private_key_base64);
		} else {
			System.out.println("Null");
		}
		System.out.print("\talg_countersign: ");
		System.out.println(alg_countersign);
		System.out.print("\tpar_countersign: ");
		System.out.println(par_countersign);
		System.out.print("\tCountersignature length: ");
		System.out.println(countersign_length);
		
//		System.out.println("Recipient Context: ");
//		System.out.print("\tRecipient ID: ");
//		System.out.print("\tRecipient Key: ");
		
		printAllRecipientContextInfo(ctx);
	}
	
	/**
	 * Print information about all recipient context in a Group Context 
	 * @param ctx
	 */
	public static void printAllRecipientContextInfo(GroupOSCoreCtx ctx) {
		if(!DEBUG) {
			return;
		}
		
		for(RecipientCtx rcpCtx : ctx.getRecipientContexts()) {
			printRecipientContextInfo(rcpCtx);
		}
		
	}
	
	/**
	 * Print information about specific recipient context in a Group OSCORE Context 
	 * @param ctx
	 */
	public static void printRecipientContextInfo(RecipientCtx rcpCtx) {
		if(!DEBUG) {
			return;
		}
		byte[] recipient_id, recipient_key;
		OneKey recipient_public_key;
		//int recipient_seq_number;

		recipient_id = rcpCtx.recipient_id;
		recipient_key = rcpCtx.recipient_key;
		recipient_public_key = rcpCtx.recipient_public_key;
		//recipient_seq_number = rcpCtx.recipient_seq;
		
		System.out.println("Recipient Context:");
		System.out.print("\tRecipient ID: ");
		System.out.println(arrayToString(recipient_id));
		System.out.print("\tRecipient Key: ");
		System.out.println(arrayToString(recipient_key));
		//System.out.print("\tRecipient Seq Number: ");
		//System.out.println(recipient_seq_number);
		
		System.out.print("\tRecipient Public Key: ");
		if(recipient_public_key != null) {
			byte[] keyObjectBytes = recipient_public_key.AsCBOR().EncodeToBytes();
			String recipient_public_key_base64 = DatatypeConverter.printBase64Binary(keyObjectBytes);
			System.out.println(recipient_public_key_base64);
		} else {
			System.out.println("Null");
		}
	}
	
	public static void printContextInfo(HashMapCtxDB db, String baseUri) {
		OSCoreCtx aCtx = null;
		
		try {
			aCtx = db.getContext(baseUri);
		} catch (OSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(aCtx instanceof GroupOSCoreCtx) {
			printContextInfo((GroupOSCoreCtx)aCtx);
		} else {
			System.err.println("Can only print for Group OSCORE Contexts!");
		}
	
	}
}