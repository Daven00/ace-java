package org.eclipse.californium.oscore;

import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.KeyKeys;
import com.upokecenter.cbor.CBORObject;

public class Contexts {
	
	//General parameters
	
	final static AlgorithmID alg = AlgorithmID.AES_CCM_16_64_128;
	final static AlgorithmID kdf = AlgorithmID.HKDF_HMAC_SHA_256;
	final static int ED25519 = KeyKeys.OKP_Ed25519.AsInt32(); //Integer value 6
	//static final int TEMP = KeyKeys.EC2_P256.AsInt32();
	final static int replay_size = 32;
	
	//Common Context
	public static class Common {
	
		final static byte[] master_secret = { 0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, (byte) 0x80, (byte) 0x90, (byte) 0xa0, (byte) 0xb0, (byte) 0xc0, (byte) 0xd0, (byte) 0xe0, (byte) 0xf0, 0x01 };
		final static byte[] master_salt = { (byte) 0xe9, (byte) 0xc7, (byte) 0x9a, 0x22, 0x32, (byte) 0x87, 0x36, 0x04 };
		final static byte[] id_context = new byte[] { 0x73, (byte) 0xbc, 0x3f, 0x12, 0x00, 0x71, 0x2a, 0x3d};
		
		final static AlgorithmID alg_countersign = AlgorithmID.EDDSA;
		final static Integer par_countersign = ED25519; //Ed25519
		
	}
	
	//Entity #1
	public static class Entity_1 {
	
		final static byte[] sid = new byte[] { (byte) 0xA2 }; //0xa2
		final static String sid_private_key_string = "pQMnAQEgBiFYIAaekSuDljrMWUG2NUaGfewQbluQUfLuFPO8XMlhrNQ6I1ggZHFNQaJAth2NgjUCcXqwiMn0r2/JhEVT5K1MQsxzUjk=";
		//static OneKey sid_private_key;
	
		final static byte[] data = new byte[] { (byte) 0xA4, (byte) 0x01, (byte) 0x01, (byte) 0x20, (byte) 0x06, (byte) 0x21, (byte) 0x58, (byte) 0x20, (byte) 0x4C, (byte) 0x5E, (byte) 0x5A, (byte) 0x89, (byte) 0x8A, (byte) 0xFC, (byte) 0x77, (byte) 0xD9, (byte) 0xC9, (byte) 0x07, (byte) 0x73, (byte) 0xD9, (byte) 0xB4, (byte) 0xF5, (byte) 0xE7, (byte) 0xB3, (byte) 0x78, (byte) 0x60, (byte) 0x57, (byte) 0x53, (byte) 0xF9, (byte) 0xBA, (byte) 0x9E, (byte) 0x8A, (byte) 0x62, (byte) 0x48, (byte) 0x8C, (byte) 0x64, (byte) 0xE1, (byte) 0xA5, (byte) 0x24, (byte) 0xB0, (byte) 0x23, (byte) 0x58, (byte) 0x20, (byte) 0xC9, (byte) 0xAF, (byte) 0xCF, (byte) 0x66, (byte) 0x10, (byte) 0xBA, (byte) 0xB6, (byte) 0x9A, (byte) 0x7E, (byte) 0x72, (byte) 0xB7, (byte) 0x8B, (byte) 0x6D, (byte) 0x36, (byte) 0x4B, (byte) 0xE8, (byte) 0x6F, (byte) 0x12, (byte) 0xCF, (byte) 0x29, (byte) 0x35, (byte) 0x23, (byte) 0xDA, (byte) 0x51, (byte) 0x43, (byte) 0x3B, (byte) 0x09, (byte) 0xA7, (byte) 0x99, (byte) 0xFF, (byte) 0x0F, (byte) 0x62 };
		public static CBORObject signing_key_cbor = CBORObject.DecodeFromBytes(data);
	}
	
	//Entity #2
	public static class Entity_2 {
		
		final static byte[] rid1 = new byte[] { 0x52 };
		final static String rid1_public_key_string = "pAMnAQEgBiFYIHfsNYwdNE5B7g6HuDg9I6IJms05vfmJzkW1Loh0Yzib";
		//static OneKey rid1_public_key;
	
		final static byte[] data = new byte[] { (byte) 0xA5, (byte) 0x01, (byte) 0x02, (byte) 0x20, (byte) 0x01, (byte) 0x21, (byte) 0x58, (byte) 0x20, (byte) 0x5B, (byte) 0xC9, (byte) 0xE4, (byte) 0x04, (byte) 0x87, (byte) 0x13, (byte) 0x0A, (byte) 0x03, (byte) 0x0D, (byte) 0x37, (byte) 0xF8, (byte) 0x16, (byte) 0x2A, (byte) 0x17, (byte) 0xEF, (byte) 0x14, (byte) 0xCC, (byte) 0x9E, (byte) 0x96, (byte) 0x01, (byte) 0x9A, (byte) 0x30, (byte) 0x7D, (byte) 0xBA, (byte) 0xDC, (byte) 0x90, (byte) 0x69, (byte) 0x1C, (byte) 0x56, (byte) 0x3D, (byte) 0x76, (byte) 0x6B, (byte) 0x22, (byte) 0x58, (byte) 0x20, (byte) 0x1D, (byte) 0x6E, (byte) 0xB7, (byte) 0x5E, (byte) 0x55, (byte) 0x85, (byte) 0xC1, (byte) 0xB1, (byte) 0x90, (byte) 0x51, (byte) 0xA8, (byte) 0x4D, (byte) 0xCC, (byte) 0x76, (byte) 0x08, (byte) 0xB6, (byte) 0x04, (byte) 0x09, (byte) 0x5B, (byte) 0xE8, (byte) 0x57, (byte) 0xBA, (byte) 0x37, (byte) 0x72, (byte) 0x7D, (byte) 0x65, (byte) 0x34, (byte) 0x3F, (byte) 0xEF, (byte) 0x61, (byte) 0x6D, (byte) 0xC3, (byte) 0x23, (byte) 0x58, (byte) 0x20, (byte) 0xBB, (byte) 0x39, (byte) 0x27, (byte) 0x6D, (byte) 0x3A, (byte) 0x04, (byte) 0xE1, (byte) 0x4E, (byte) 0x44, (byte) 0x21, (byte) 0xA5, (byte) 0x66, (byte) 0x89, (byte) 0xF7, (byte) 0xCA, (byte) 0xFE, (byte) 0xC1, (byte) 0xD0, (byte) 0x8D, (byte) 0xF3, (byte) 0x02, (byte) 0x9C, (byte) 0xB7, (byte) 0xCE, (byte) 0xD9, (byte) 0x68, (byte) 0x28, (byte) 0x3A, (byte) 0x08, (byte) 0x4B, (byte) 0x7E, (byte) 0x38 };
		public static CBORObject signing_key_cbor = CBORObject.DecodeFromBytes(data);
	}
	
	//Entity #3
	public static class Entity_3 {
		
		final static byte[] rid2 = new byte[] { 0x77 };
		final static String rid2_public_key_string = "pAMnAQEgBiFYIBBbjGqMiAGb8MNUWSk0EwuqgAc5nMKsO+hFiEYT1bou";
		//static OneKey rid2_public_key;
		
		final static byte[] data = new byte[] { (byte) 0xA5, (byte) 0x01, (byte) 0x02, (byte) 0x20, (byte) 0x01, (byte) 0x21, (byte) 0x58, (byte) 0x20, (byte) 0x57, (byte) 0xCF, (byte) 0x4C, (byte) 0x3D, (byte) 0xBF, (byte) 0x16, (byte) 0x21, (byte) 0x6B, (byte) 0x10, (byte) 0x09, (byte) 0xD3, (byte) 0x0F, (byte) 0x3C, (byte) 0x7C, (byte) 0x40, (byte) 0x8A, (byte) 0x71, (byte) 0x44, (byte) 0xE6, (byte) 0x3F, (byte) 0xEC, (byte) 0x18, (byte) 0xC5, (byte) 0x61, (byte) 0x97, (byte) 0x0F, (byte) 0x2E, (byte) 0xDC, (byte) 0x6E, (byte) 0xEA, (byte) 0x99, (byte) 0x3A, (byte) 0x22, (byte) 0x58, (byte) 0x20, (byte) 0xB2, (byte) 0x0E, (byte) 0xF6, (byte) 0xB0, (byte) 0x51, (byte) 0x8D, (byte) 0x25, (byte) 0xCB, (byte) 0xEB, (byte) 0x2E, (byte) 0xF5, (byte) 0xDB, (byte) 0x8E, (byte) 0x12, (byte) 0xDE, (byte) 0x05, (byte) 0x6B, (byte) 0x40, (byte) 0x75, (byte) 0xB3, (byte) 0xF4, (byte) 0x98, (byte) 0x67, (byte) 0x81, (byte) 0x38, (byte) 0x5B, (byte) 0x90, (byte) 0xA6, (byte) 0x25, (byte) 0xB0, (byte) 0x4A, (byte) 0xC7, (byte) 0x23, (byte) 0x58, (byte) 0x20, (byte) 0xC9, (byte) 0x6D, (byte) 0x7F, (byte) 0x08, (byte) 0xEF, (byte) 0x1F, (byte) 0xE1, (byte) 0x3B, (byte) 0xC3, (byte) 0x11, (byte) 0xCA, (byte) 0xB7, (byte) 0xFC, (byte) 0x5C, (byte) 0x5C, (byte) 0xBA, (byte) 0x36, (byte) 0x93, (byte) 0x00, (byte) 0x42, (byte) 0x93, (byte) 0xC6, (byte) 0x38, (byte) 0xF2, (byte) 0x50, (byte) 0xEB, (byte) 0x6E, (byte) 0xA1, (byte) 0x22, (byte) 0xE7, (byte) 0xC8, (byte) 0x79 };
		public static CBORObject signing_key_cbor = CBORObject.DecodeFromBytes(data);
	}
	
}

/*
 * 
 *
 * Common Context:
Master Secret: 0x0102030405060708090a0b0c0d0e0f10 (16 bytes)
Master Salt: 0x9e7ca92223786340 (8 bytes)
Common IV: 0x0x2ca58fb85ff1b81c0b7181b85e (13 bytes)
ID Context: 0x37cbf3210017a2d3 (8 bytes)
Par Countersign: 0x26
Par Countersign Key: 0x822601
Entity #1
Sender ID: 0xa1 (0 byte)
Sender Key: 0xaf2a1300a5e95788b356336eeecd2b92 (16 bytes)
Sender Seq Number: 00
Sender IV: 0x2ca58fb85ff1b81c0b7181b85e (using Partial IV: 00)
Signing Key: {1: 2, -1: 1, -2: hâ€™E2A7DC0C5D23831A4F52FBFF759EF01A6B3A7D58694774D6E8505B31A351D6C4â€™, -3: hâ€™F8CA44FEDC6C322D0946FC69AE7482CD066AD11F34AA5F5C63F4EADB320FD941â€™, -4: hâ€™469C76F26B8D9F286449F42566AB8B8BA1B3A8DC6E711A1E2A6B548DBE2A1578â€™}
 * 
 * 
 * */
 