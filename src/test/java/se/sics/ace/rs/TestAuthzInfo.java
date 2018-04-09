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
package se.sics.ace.rs;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import COSE.HeaderKeys;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.upokecenter.cbor.CBORObject;

import COSE.AlgorithmID;
import COSE.CoseException;
import COSE.KeyKeys;
import COSE.MessageTag;
import COSE.OneKey;
import se.sics.ace.AceException;
import se.sics.ace.COSEparams;
import se.sics.ace.Constants;
import se.sics.ace.DBHelper;
import se.sics.ace.Message;
import se.sics.ace.ReferenceToken;
import se.sics.ace.TestConfig;
import se.sics.ace.as.Introspect;
import se.sics.ace.cwt.CWT;
import se.sics.ace.cwt.CwtCryptoCtx;
import se.sics.ace.examples.KissPDP;
import se.sics.ace.examples.KissTime;
import se.sics.ace.examples.KissValidator;
import se.sics.ace.examples.LocalMessage;
import se.sics.ace.examples.SQLConnector;

/**
 * 
 * @author Ludwig Seitz
 */
public class TestAuthzInfo {
    
    static OneKey publicKey;
    static byte[] key128 = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    static byte[] key128a = {'c', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    static SQLConnector db = null;

    private static AuthzInfo ai = null;
    private static Introspect i; 
    private static TokenRepository tr = null;
    private static KissPDP pdp = null;
    
    /**
     * Set up tests.
     * @throws SQLException 
     * @throws AceException 
     * @throws IOException 
     * @throws CoseException 
     */
    @BeforeClass
    public static void setUp() 
            throws SQLException, AceException, IOException, CoseException {

        DBHelper.setUpDB();
        db = DBHelper.getSQLConnector();

        OneKey key = OneKey.generateKey(AlgorithmID.ECDSA_256);
        publicKey = key.PublicKey();
        publicKey.add(KeyKeys.KeyId, CBORObject.FromObject(new byte[]{0x31, 0x42}));
        
        OneKey sharedKey = new OneKey();
        sharedKey.add(KeyKeys.KeyType, KeyKeys.KeyType_Octet);
        sharedKey.add(KeyKeys.KeyId, CBORObject.FromObject(new byte[]{0x74, 0x11}));
        sharedKey.add(KeyKeys.Octet_K, CBORObject.FromObject(key128));

        Set<String> profiles = new HashSet<>();
        profiles.add("coap_dtls");
        Set<String> keyTypes = new HashSet<>();
        keyTypes.add("PSK");
        db.addClient("client1", profiles, null, null, keyTypes, null, 
                publicKey);
        db.addClient("client2", profiles, null, null, keyTypes, sharedKey,
                publicKey);

        Set<String> actions = new HashSet<>();
        actions.add("GET");
        Map<String, Set<String>> myResource = new HashMap<>();
        myResource.put("temp", actions);
        Map<String, Map<String, Set<String>>> myScopes = new HashMap<>();
        myScopes.put("r_temp", myResource);
        
        Map<String, Set<String>> myResource2 = new HashMap<>();
        myResource2.put("co2", actions);
        myScopes.put("r_co2", myResource2);
        
        KissValidator valid = new KissValidator(Collections.singleton("rs1"),
                myScopes);
        createTR(valid);
        tr = TokenRepository.getInstance();
        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, 
                AlgorithmID.AES_CCM_16_128_128, AlgorithmID.Direct);
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128, 
                coseP.getAlg().AsCBOR());

        pdp = new KissPDP(db);
        pdp.addIntrospectAccess("ni:///sha-256;xzLa24yOBeCkos3VFzD2gd83Urohr9TsXqY9nhdDN0w");
        pdp.addIntrospectAccess("rs1");
        i = new Introspect(pdp, db, new KissTime(), key);
        ai = new AuthzInfo(tr, Collections.singletonList("TestAS"), 
                new KissTime(), 
                new IntrospectionHandler4Tests(i, "rs1", "TestAS"),
                valid, ctx);
    }

    private static  Map<HeaderKeys, CBORObject> getHeadersWithKID(String audience)
    {
        // Add the audience as the KID in the header, so it can be referenced by introspection requests.
        CBORObject requestedAud = CBORObject.NewArray();
        requestedAud.Add(audience);
        Map<HeaderKeys, CBORObject> uHeaders = new HashMap<>();
        uHeaders.put(HeaderKeys.KID, requestedAud);
        return uHeaders;
    }
    
    /**
     * Create the Token repository if not already created,
     * if already create ignore.
     * 
     * @param valid 
     * @throws IOException 
     * 
     */
    private static void createTR(KissValidator valid) throws IOException {
        try {
            TokenRepository.create(valid, TestConfig.testFilePath 
                    + "tokens.json", null);
        } catch (AceException e) {
            System.err.println(e.getMessage());
            try {
                TokenRepository tr = TokenRepository.getInstance();
                tr.close();
                new File(TestConfig.testFilePath + "tokens.json").delete();
                TokenRepository.create(valid, TestConfig.testFilePath 
                        + "tokens.json", null);
            } catch (AceException e2) {
               throw new RuntimeException(e2);
            }
           
            
        }
    }
    
    /**
     * Deletes the test DB after the tests
     * 
     * @throws SQLException 
     * @throws AceException 
     */
    @AfterClass
    public static void tearDown() throws SQLException, AceException, Exception {
        DBHelper.tearDownDB();
        pdp.close();
        i.close();
        tr.close();
        new File(TestConfig.testFilePath + "tokens.json").delete();
    }
    
    /**
     * Test inactive reference token submission to AuthzInfo
     * 
     * @throws IllegalStateException 
     * @throws InvalidCipherTextException 
     * @throws CoseException 
     * @throws AceException 
     */
    @Test
    public void testRefInactive() throws IllegalStateException, 
            InvalidCipherTextException, CoseException, AceException {
        ReferenceToken token = new ReferenceToken(20);
        LocalMessage request = new LocalMessage(0, "client1", "rs1",
               token.encode());
                
        LocalMessage response = (LocalMessage)ai.processMessage(request);
        assert(response.getMessageCode() == Message.FAIL_UNAUTHORIZED);
        CBORObject map = CBORObject.NewMap();
        map.Add(Constants.ERROR, Constants.UNAUTHORIZED_CLIENT);
        map.Add(Constants.ERROR_DESCRIPTION, "Token is not active");
        Assert.assertArrayEquals(map.EncodeToBytes(), response.getRawPayload());
    }
    
    /**
     * Test CWT with a scope claim that is overwritten by introspection
     * 
     * @throws AceException 
     * @throws CoseException 
     * @throws InvalidCipherTextException 
     * @throws IllegalStateException 
     * @throws IntrospectionException 
     */
    @Test
    public void testCwtIntrospect() throws AceException, IllegalStateException,
            InvalidCipherTextException, CoseException, IntrospectionException {
        Map<Short, CBORObject> params = new HashMap<>();
        params.put(Constants.SCOPE, CBORObject.FromObject("r_co2"));
        params.put(Constants.CTI, CBORObject.FromObject(new byte[]{0x01}));
        CBORObject cnf = CBORObject.NewMap();
        cnf.Add(Constants.COSE_KEY_CBOR, publicKey.AsCBOR());
        params.put(Constants.CNF, cnf);
        
        String ctiStr = Base64.getEncoder().encodeToString(new byte[]{0x01});
        //Make introspection succeed
        db.addToken(ctiStr, params);
        db.addCti2Client(ctiStr, "client1");
        
        //this overwrites the scope
        params.put(Constants.SCOPE, CBORObject.FromObject("r_temp"));
        params.put(Constants.AUD, CBORObject.FromObject("rs1"));
        params.put(Constants.ISS, CBORObject.FromObject("TestAS"));
        OneKey key = new OneKey();
        key.add(KeyKeys.KeyType, KeyKeys.KeyType_Octet);
        CBORObject kid = CBORObject.FromObject(new byte[]{0x00, 0x01});
        key.add(KeyKeys.KeyId, kid);
        key.add(KeyKeys.Octet_K, CBORObject.FromObject(key128));
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.COSE_KEY_CBOR, key.AsCBOR());
        params.put(Constants.CNF, cbor);
        CWT token = new CWT(params);
        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, 
                AlgorithmID.AES_CCM_16_128_128, AlgorithmID.Direct);
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128, 
                coseP.getAlg().AsCBOR());
        LocalMessage request = new LocalMessage(0, "client1", "rs1", 
               token.encode(ctx));
        LocalMessage response = (LocalMessage)ai.processMessage(request);
        assert(response.getMessageCode() == Message.CREATED);
        CBORObject resP = CBORObject.DecodeFromBytes(response.getRawPayload());
        CBORObject cti = resP.get(CBORObject.FromObject(Constants.CTI));
        Assert.assertArrayEquals(cti.GetByteString(), new byte[]{0x01});
        String kidStr = Base64.getEncoder().encodeToString(
                new byte[]{0x31, 0x42});
        assert(1 == tr.canAccess(
                kidStr, null, "co2", "GET", new KissTime(), null));

    }
    
    /**
     * Test CWT with invalid MAC submission to AuthzInfo
     * 
     * @throws IllegalStateException 
     * @throws InvalidCipherTextException 
     * @throws CoseException 
     * @throws AceException  
     */
    @Test
    public void testInvalidCWT() throws IllegalStateException, 
            InvalidCipherTextException, CoseException, AceException {
        Map<Short, CBORObject> claims = new HashMap<>();
        claims.put(Constants.ISS, CBORObject.FromObject("coap://as.example.com"));
        claims.put(Constants.AUD, CBORObject.FromObject("coap://light.example.com"));
        claims.put(Constants.EXP, CBORObject.FromObject(1444064944));
        claims.put(Constants.NBF, CBORObject.FromObject(1443944944));
        claims.put(Constants.IAT, CBORObject.FromObject(1443944944));
        byte[] cti = {0x02};
        claims.put(Constants.CTI, CBORObject.FromObject(cti));
        claims.put(Constants.CNF, publicKey.AsCBOR());
        claims.put(Constants.SCOPE, CBORObject.FromObject(
                "r+/s/light rwx+/a/led w+/dtls"));
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128a, AlgorithmID.AES_CCM_16_64_128.AsCBOR());
        CWT cwt = new CWT(claims);

        LocalMessage request = new LocalMessage(0, "client1", "rs1", 
                cwt.encode(ctx));
                
        LocalMessage response = (LocalMessage)ai.processMessage(request);
        assert(response.getMessageCode() == Message.FAIL_BAD_REQUEST);
        CBORObject map = CBORObject.NewMap();
        map.Add(Constants.ERROR, Constants.UNAUTHORIZED_CLIENT);
        map.Add(Constants.ERROR_DESCRIPTION, "Token is invalid");
        Assert.assertArrayEquals(map.EncodeToBytes(), response.getRawPayload());
    }
    
    /**
     * Test an invalid token format submission to AuthzInfo
     * 
     * @throws IllegalStateException 
     * @throws InvalidCipherTextException 
     * @throws CoseException 
     * @throws AceException  
     */
    @Test
    public void testInvalidTokenFormat() throws IllegalStateException, 
            InvalidCipherTextException, CoseException, AceException {
        CBORObject token = CBORObject.False;
        LocalMessage request = new LocalMessage(0, "client1", "rs1", 
               token);
                
        LocalMessage response = (LocalMessage)ai.processMessage(request);
        assert(response.getMessageCode() == Message.FAIL_BAD_REQUEST);
        CBORObject map = CBORObject.NewMap();
        map.Add(Constants.ERROR, Constants.INVALID_REQUEST);
        map.Add(Constants.ERROR_DESCRIPTION, "Unknown token format");
        Assert.assertArrayEquals(map.EncodeToBytes(), response.getRawPayload());
    }
    
    /**
     * Test expired CWT submission to AuthzInfo
     * 
     * @throws IllegalStateException 
     * @throws InvalidCipherTextException 
     * @throws CoseException 
     * @throws AceException  
     */
    @Test
    public void testExpiredCWT() throws IllegalStateException, 
            InvalidCipherTextException, CoseException, AceException {
        Map<Short, CBORObject> claims = new HashMap<>();
        byte[] cti = {0x03};
        claims.put(Constants.CTI, CBORObject.FromObject(cti));
        String ctiStr = Base64.getEncoder().encodeToString(cti);
        
        //Make introspection succeed
        db.addToken(ctiStr, claims);
        db.addCti2Client(ctiStr, "client1");
        
        claims.put(Constants.CNF, publicKey.AsCBOR());
        claims.put(Constants.SCOPE, CBORObject.FromObject(
                "r+/s/light rwx+/a/led w+/dtls")); 
        claims.put(Constants.ISS, CBORObject.FromObject("coap://as.example.com"));
        claims.put(Constants.AUD, CBORObject.FromObject("coap://light.example.com"));
        claims.put(Constants.NBF, CBORObject.FromObject(1443944944));
        claims.put(Constants.IAT, CBORObject.FromObject(1443944944));        
        claims.put(Constants.EXP, CBORObject.FromObject(10000));
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128, AlgorithmID.AES_CCM_16_64_128.AsCBOR());
        CWT cwt = new CWT(claims);
        LocalMessage request = new LocalMessage(0, "clientA", "rs1", 
               cwt.encode(ctx));
                
        LocalMessage response = (LocalMessage)ai.processMessage(request);
        assert(response.getMessageCode() == Message.FAIL_UNAUTHORIZED);
        CBORObject map = CBORObject.NewMap();
        map.Add(Constants.ERROR, Constants.UNAUTHORIZED_CLIENT);
        map.Add(Constants.ERROR_DESCRIPTION, "Token is expired");
        Assert.assertArrayEquals(map.EncodeToBytes(), response.getRawPayload());
        db.deleteToken(ctiStr);
    }
    
    /**
     * Test CWT without issuer submission to AuthzInfo
     * 
     * @throws IllegalStateException 
     * @throws InvalidCipherTextException 
     * @throws CoseException 
     * @throws AceException 
     */
    @Test
    public void testNoIssuer() throws IllegalStateException, 
            InvalidCipherTextException, CoseException, AceException {
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.SCOPE, CBORObject.FromObject("r_temp"));
        params.put(Constants.AUD, CBORObject.FromObject("rs1"));
        params.put(Constants.CTI, CBORObject.FromObject(new byte[]{0x04}));
        String ctiStr = Base64.getEncoder().encodeToString(new byte[]{0x04});
        
        //Make introspection succeed
        db.addToken(Base64.getEncoder().encodeToString(
                new byte[]{0x04}), params);
        db.addCti2Client(ctiStr, "client1");        
        
        CWT token = new CWT(params);
        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, 
                AlgorithmID.AES_CCM_16_128_128, AlgorithmID.Direct);
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128, 
                coseP.getAlg().AsCBOR());
        LocalMessage request = new LocalMessage(0, "client1", "rs1", 
                token.encode(ctx));
                
        LocalMessage response = (LocalMessage)ai.processMessage(request);
        CBORObject map = CBORObject.NewMap();
        map.Add(Constants.ERROR, Constants.INVALID_REQUEST);
        map.Add(Constants.ERROR_DESCRIPTION, "Token has no issuer");
        assert(response.getMessageCode() == Message.FAIL_BAD_REQUEST);
        Assert.assertArrayEquals(map.EncodeToBytes(), response.getRawPayload());
    }
    
    /**
     * Test CWT with unrecognized issuer submission to AuthzInfo
     * 
     * @throws IllegalStateException 
     * @throws InvalidCipherTextException 
     * @throws CoseException 
     * @throws AceException 
     */
    @Test
    public void testIssuerNotRecognized() throws IllegalStateException, 
            InvalidCipherTextException, CoseException, AceException {
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.CTI, CBORObject.FromObject(new byte[]{0x05}));
        CBORObject cnf = CBORObject.NewMap();
        cnf.Add(Constants.COSE_KEY_CBOR, publicKey.AsCBOR());
        params.put(Constants.CNF, cnf);
        String ctiStr = Base64.getEncoder().encodeToString(new byte[]{0x05});
        
        //Make introspection succeed
        db.addToken(Base64.getEncoder().encodeToString(
                new byte[]{0x05}), params);
        db.addCti2Client(ctiStr, "client1");
        
        params.put(Constants.SCOPE, CBORObject.FromObject("r_temp"));
        params.put(Constants.AUD, CBORObject.FromObject("rs1"));
        params.put(Constants.ISS, CBORObject.FromObject("FalseAS"));
        CWT token = new CWT(params);
        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, 
                AlgorithmID.AES_CCM_16_128_128, AlgorithmID.Direct);
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128, 
                coseP.getAlg().AsCBOR());
        LocalMessage request = new LocalMessage(0, "client1", "rs1", 
                token.encode(ctx));
                
        LocalMessage response = (LocalMessage)ai.processMessage(request);  
        CBORObject map = CBORObject.NewMap();
        map.Add(Constants.ERROR, Constants.INVALID_REQUEST);
        map.Add(Constants.ERROR_DESCRIPTION, "Token issuer unknown");
        assert(response.getMessageCode() == Message.FAIL_UNAUTHORIZED);
        Assert.assertArrayEquals(map.EncodeToBytes(), response.getRawPayload());
    }
    
    /**
     * Test CWT without audience submission to AuthzInfo
     * 
     * @throws IllegalStateException 
     * @throws InvalidCipherTextException 
     * @throws CoseException 
     * @throws AceException 
     */
    @Test
    public void testNoAudience() throws IllegalStateException, 
            InvalidCipherTextException, CoseException, AceException {
        Map<Short, CBORObject> params = new HashMap<>();
        params.put(Constants.CTI, CBORObject.FromObject(new byte[]{0x06}));
        String ctiStr = Base64.getEncoder().encodeToString(new byte[]{0x06});

        //Make introspection succeed
        db.addToken(Base64.getEncoder().encodeToString(
                new byte[]{0x06}), params);
        db.addCti2Client(ctiStr, "client1");
        
        params.put(Constants.SCOPE, CBORObject.FromObject("r_temp"));
        params.put(Constants.ISS, CBORObject.FromObject("TestAS"));
        CWT token = new CWT(params);
        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, 
                AlgorithmID.AES_CCM_16_128_128, AlgorithmID.Direct);
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128, 
                coseP.getAlg().AsCBOR());
        LocalMessage request = new LocalMessage(0, "clientA", "rs1", 
                token.encode(ctx));
                
        LocalMessage response = (LocalMessage)ai.processMessage(request);
        CBORObject map = CBORObject.NewMap();
        map.Add(Constants.ERROR, Constants.INVALID_REQUEST);
        map.Add(Constants.ERROR_DESCRIPTION, "Token has no audience");
        assert(response.getMessageCode() == Message.FAIL_BAD_REQUEST);
        Assert.assertArrayEquals(map.EncodeToBytes(), response.getRawPayload());
    }
    
    /**
     * Test CWT with audience that does not match RS submission to AuthzInfo
     * 
     * @throws IllegalStateException 
     * @throws InvalidCipherTextException 
     * @throws CoseException 
     * @throws AceException 
     */
    @Test
    public void testNoAudienceMatch() throws IllegalStateException, 
            InvalidCipherTextException, CoseException, AceException {
        Map<Short, CBORObject> params = new HashMap<>();
        params.put(Constants.CTI, CBORObject.FromObject(new byte[]{0x07}));
        String ctiStr = Base64.getEncoder().encodeToString(new byte[]{0x07});

        //Make introspection succeed
        db.addToken(Base64.getEncoder().encodeToString(
                new byte[]{0x07}), params);
        db.addCti2Client(ctiStr, "client1");
        
        params.put(Constants.SCOPE, CBORObject.FromObject("r_temp"));
        params.put(Constants.AUD, CBORObject.FromObject("blah"));
        params.put(Constants.ISS, CBORObject.FromObject("TestAS"));
        CWT token = new CWT(params);
        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, 
                AlgorithmID.AES_CCM_16_128_128, AlgorithmID.Direct);
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128, 
                coseP.getAlg().AsCBOR());
        LocalMessage request = new LocalMessage(0, "clientA", "rs1", 
                token.encode(ctx));
                
        LocalMessage response = (LocalMessage)ai.processMessage(request);  
        CBORObject map = CBORObject.NewMap();
        map.Add(Constants.ERROR, Constants.UNAUTHORIZED_CLIENT);
        map.Add(Constants.ERROR_DESCRIPTION, "Audience does not apply");
        assert(response.getMessageCode() == Message.FAIL_FORBIDDEN);
        Assert.assertArrayEquals(map.EncodeToBytes(), response.getRawPayload());   
    }  
    
    /**
     * Test CWT without scope submission to AuthzInfo
     * 
     * @throws IllegalStateException 
     * @throws InvalidCipherTextException 
     * @throws CoseException 
     * @throws AceException  
     */
    @Test
    public void testNoScope() throws IllegalStateException, 
            InvalidCipherTextException, CoseException, AceException {
        Map<Short, CBORObject> params = new HashMap<>();
        params.put(Constants.CTI, CBORObject.FromObject(new byte[]{0x08}));
        String ctiStr = Base64.getEncoder().encodeToString(new byte[]{0x08});

        //Make introspection succeed
        db.addToken(Base64.getEncoder().encodeToString(
                new byte[]{0x08}), params);
        db.addCti2Client(ctiStr, "client1");

        params.put(Constants.AUD, CBORObject.FromObject("rs1"));
        params.put(Constants.ISS, CBORObject.FromObject("TestAS"));
        CWT token = new CWT(params);
        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, 
                AlgorithmID.AES_CCM_16_128_128, AlgorithmID.Direct);
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128, 
                coseP.getAlg().AsCBOR());
        LocalMessage request = new LocalMessage(0, "clientA", "rs1", 
               token.encode(ctx));
        LocalMessage response = (LocalMessage)ai.processMessage(request);
        CBORObject map = CBORObject.NewMap();
        map.Add(Constants.ERROR, Constants.INVALID_SCOPE);
        map.Add(Constants.ERROR_DESCRIPTION, "Token has no scope");
        assert(response.getMessageCode() == Message.FAIL_BAD_REQUEST);
        Assert.assertArrayEquals(map.EncodeToBytes(), response.getRawPayload());
    }
    
    /**
     * Test successful submission to AuthzInfo
     * 
     * @throws IllegalStateException 
     * @throws InvalidCipherTextException 
     * @throws CoseException 
     * @throws AceException  
     */
    @Test
    public void testSuccess() throws IllegalStateException, 
            InvalidCipherTextException, CoseException, AceException {
        Map<Short, CBORObject> params = new HashMap<>();
        params.put(Constants.CTI, CBORObject.FromObject(new byte[]{0x09}));
        params.put(Constants.SCOPE, CBORObject.FromObject("r_temp"));
        params.put(Constants.AUD, CBORObject.FromObject("rs1"));
        params.put(Constants.ISS, CBORObject.FromObject("TestAS"));
        OneKey key = new OneKey();
        key.add(KeyKeys.KeyType, KeyKeys.KeyType_Octet);
        String kidStr = "ourKey";
        CBORObject kid = CBORObject.FromObject(
                kidStr.getBytes(Constants.charset));
        key.add(KeyKeys.KeyId, kid);
        key.add(KeyKeys.Octet_K, CBORObject.FromObject(key128));
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.COSE_KEY_CBOR, key.AsCBOR());
        params.put(Constants.CNF, cbor);
        String ctiStr = Base64.getEncoder().encodeToString(new byte[]{0x09});

        //Make introspection succeed
        db.addToken(Base64.getEncoder().encodeToString(
                new byte[]{0x09}), params);
        db.addCti2Client(ctiStr, "client1");  

        
        CWT token = new CWT(params);
        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, 
                AlgorithmID.AES_CCM_16_128_128, AlgorithmID.Direct);
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128, 
                coseP.getAlg().AsCBOR());
        LocalMessage request = new LocalMessage(0, "clientA", "rs1", 
               token.encode(ctx));
                
        LocalMessage response = (LocalMessage)ai.processMessage(request);
        System.out.println(response.toString());
        assert(response.getMessageCode() == Message.CREATED);
        CBORObject resP = CBORObject.DecodeFromBytes(response.getRawPayload());
        CBORObject cti = resP.get(CBORObject.FromObject(Constants.CTI));
        Assert.assertArrayEquals(cti.GetByteString(), 
                new byte[]{0x09});
    }    
    
    /**
     * Test successful submission to AuthzInfo with an array of audiences
     * 
     * @throws IllegalStateException 
     * @throws InvalidCipherTextException 
     * @throws CoseException 
     * @throws AceException  
     */
    @Test
    public void testAudArray() throws IllegalStateException, 
            InvalidCipherTextException, CoseException, AceException {
        Map<Short, CBORObject> params = new HashMap<>();
        params.put(Constants.CTI, CBORObject.FromObject(new byte[]{0x11}));
        params.put(Constants.SCOPE, CBORObject.FromObject("r_temp"));
        CBORObject aud = CBORObject.NewArray();
        aud.Add(CBORObject.FromObject("rs1"));
        aud.Add(CBORObject.FromObject("foo"));
        aud.Add(CBORObject.FromObject("bar"));
        params.put(Constants.AUD, aud);
        params.put(Constants.ISS, CBORObject.FromObject("TestAS"));
        OneKey key = new OneKey();
        key.add(KeyKeys.KeyType, KeyKeys.KeyType_Octet);
        String kidStr = "ourKey";
        CBORObject kid = CBORObject.FromObject(
                kidStr.getBytes(Constants.charset));
        key.add(KeyKeys.KeyId, kid);
        key.add(KeyKeys.Octet_K, CBORObject.FromObject(key128));
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.COSE_KEY_CBOR, key.AsCBOR());
        params.put(Constants.CNF, cbor);
        String ctiStr = Base64.getEncoder().encodeToString(new byte[]{0x11});

        //Make introspection succeed
        db.addToken(Base64.getEncoder().encodeToString(
                new byte[]{0x11}), params);
        db.addCti2Client(ctiStr, "client1");  

        
        CWT token = new CWT(params);
        COSEparams coseP = new COSEparams(MessageTag.Encrypt0, 
                AlgorithmID.AES_CCM_16_128_128, AlgorithmID.Direct);
        CwtCryptoCtx ctx = CwtCryptoCtx.encrypt0(key128, 
                coseP.getAlg().AsCBOR());
        LocalMessage request = new LocalMessage(0, "clientA", "rs1", 
               token.encode(ctx));
                
        LocalMessage response = (LocalMessage)ai.processMessage(request);
        System.out.println(response.toString());
        assert(response.getMessageCode() == Message.CREATED);
        CBORObject resP = CBORObject.DecodeFromBytes(response.getRawPayload());
        CBORObject cti = resP.get(CBORObject.FromObject(Constants.CTI));
        Assert.assertArrayEquals(cti.GetByteString(), 
                new byte[]{0x11});
    }    
}
