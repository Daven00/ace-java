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
package se.sics.ace.as;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.upokecenter.cbor.CBORObject;

import COSE.AlgorithmID;
import COSE.Attribute;
import COSE.CoseException;
import COSE.Encrypt0Message;
import COSE.HeaderKeys;
import COSE.KeyKeys;
import COSE.MessageTag;
import COSE.OneKey;

import se.sics.ace.AceException;
import se.sics.ace.COSEparams;
import se.sics.ace.Constants;
import se.sics.ace.Message;
import se.sics.ace.TestConfig;
import se.sics.ace.cwt.CWT;
import se.sics.ace.cwt.CwtCryptoCtx;
import se.sics.ace.examples.KissPDP;
import se.sics.ace.examples.KissTime;
import se.sics.ace.examples.LocalMessage;
import se.sics.ace.examples.SQLConnector;

/**
 * Test the token endpoint class.
 * 
 * @author Ludwig Seitz
 *
 */
public class TestToken {
    
    private static OneKey publicKey;
    private static OneKey privateKey; 
    private static byte[] key128 = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    private static SQLConnector db = null;
    private static String dbPwd = null;
    private static Token t = null;
    private static String cti1;
    private static String cti2;
    
    /**
     * Set up tests.
     * @throws AceException 
     * @throws SQLException 
     * @throws IOException 
     * @throws CoseException 
     */
    @BeforeClass
    public static void setUp() throws AceException, SQLException, IOException, CoseException {
        BufferedReader br = new BufferedReader(new FileReader("db.pwd"));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            dbPwd = sb.toString().replace(System.getProperty("line.separator"), "");         
        } finally {
            br.close();
        }
        //Just to be sure no old test pollutes the DB
        SQLConnector.wipeDatabase(dbPwd);
        
        SQLConnector.createUser(dbPwd, "aceuser", "password", 
                "jdbc:mysql://localhost:3306");
        SQLConnector.createDB(dbPwd, "aceuser", "password", null,
                "jdbc:mysql://localhost:3306");


        privateKey = OneKey.generateKey(AlgorithmID.ECDSA_256);
        publicKey = privateKey.PublicKey(); 
        publicKey.add(KeyKeys.KeyId, CBORObject.FromObject(
                "myKey".getBytes(Constants.charset)));
        
        db = SQLConnector.getInstance(null, null, null);

        CBORObject keyData = CBORObject.NewMap();
        keyData.Add(KeyKeys.KeyType.AsCBOR(), KeyKeys.KeyType_Octet);
        keyData.Add(KeyKeys.Octet_K.AsCBOR(), 
                CBORObject.FromObject(key128));
        OneKey skey = new OneKey(keyData);
        
        //Setup RS entries
        Set<String> profiles = new HashSet<>();
        profiles.add("coap_dtls");
        profiles.add("coap_oscoap");
        
        Set<String> scopes = new HashSet<>();
        scopes.add("temp");
        scopes.add("co2");
        
        Set<String> auds = new HashSet<>();
        auds.add("sensors");
        auds.add("actuators");
        auds.add("failCWTpar");
        
        Set<String> keyTypes = new HashSet<>();
        keyTypes.add("PSK");
        keyTypes.add("RPK");
        
        Set<Short> tokenTypes = new HashSet<>();
        tokenTypes.add(AccessTokenFactory.CWT_TYPE);
        tokenTypes.add(AccessTokenFactory.REF_TYPE);
        
        Set<COSEparams> cose = new HashSet<>();
        COSEparams coseP = new COSEparams(MessageTag.Sign1, 
                AlgorithmID.ECDSA_256, AlgorithmID.Direct);
        cose.add(coseP);
        
        long expiration = 1000000L;
       
        db.addRS("rs1", profiles, scopes, auds, keyTypes, tokenTypes, cose, 
                expiration, skey, publicKey);
        
        profiles.remove("coap_oscoap");
        scopes.clear();
        auds.clear();
        auds.add("sensors");
        auds.add("failTokenType");
        keyTypes.remove("PSK");
        tokenTypes.remove(AccessTokenFactory.REF_TYPE);
        expiration = 300000L;
        db.addRS("rs2", profiles, scopes, auds, keyTypes, tokenTypes, cose,
                expiration, skey, null);
        
        profiles.clear();
        profiles.add("coap_oscoap");
        scopes.add("co2");
        auds.clear();
        auds.add("actuators");
        auds.add("failTokenType");
        auds.add("failProfile");
        keyTypes.clear();
        keyTypes.add("PSK");
        tokenTypes.clear();
        tokenTypes.add(AccessTokenFactory.REF_TYPE);
        cose.clear();
        coseP = new COSEparams(MessageTag.MAC0, 
                AlgorithmID.HMAC_SHA_256, AlgorithmID.Direct);
        cose.add(coseP);
        expiration = 30000L;
        db.addRS("rs3", profiles, scopes, auds, keyTypes, tokenTypes, cose,
                expiration, null, publicKey);
        
        profiles.clear();
        profiles.add("coap_dtls");
        auds.clear();
        auds.add("failProfile");
        keyTypes.clear();
        keyTypes.add("PSK");
        tokenTypes.clear();
        tokenTypes.add(AccessTokenFactory.REF_TYPE);
        cose.clear();
        coseP = new COSEparams(MessageTag.MAC0, 
                AlgorithmID.HMAC_SHA_256, AlgorithmID.Direct);
        cose.add(coseP);
        expiration = 30000L;
        db.addRS("rs4", profiles, scopes, auds, keyTypes, tokenTypes, cose,
                expiration, null, publicKey);
        
        profiles.clear();
        profiles.add("coap_dtls");
        scopes.add("co2");
        auds.clear();
        auds.add("failTokenNotImplemented");
        keyTypes.clear();
        keyTypes.add("PSK");
        tokenTypes.clear();
        tokenTypes.add(AccessTokenFactory.TEST_TYPE);
        cose.clear();
        coseP = new COSEparams(MessageTag.MAC0, 
                AlgorithmID.HMAC_SHA_256, AlgorithmID.Direct);
        cose.add(coseP);
        expiration = 30000L;
        db.addRS("rs5", profiles, scopes, auds, keyTypes, tokenTypes, cose,
                expiration, null, publicKey);
        
        profiles.clear();
        profiles.add("coap_oscoap");
        scopes.add("co2");
        auds.clear();
        keyTypes.clear();
        keyTypes.add("TST");
        tokenTypes.clear();
        tokenTypes.add(AccessTokenFactory.REF_TYPE);
        cose.clear();
        coseP = new COSEparams(MessageTag.MAC0, 
                AlgorithmID.HMAC_SHA_256, AlgorithmID.Direct);
        cose.add(coseP);
        expiration = 30000L;
        db.addRS("rs6", profiles, scopes, auds, keyTypes, tokenTypes, cose,
                expiration, null, publicKey);
        
        
        profiles.clear();
        profiles.add("coap_oscoap");
        scopes.add("co2");
        auds.clear();
        auds.add("failCWTpar");
        keyTypes.clear();
        keyTypes.add("PSK");
        tokenTypes.clear();
        tokenTypes.add(AccessTokenFactory.CWT_TYPE);
        cose.clear();
        coseP = new COSEparams(MessageTag.MAC0, 
                AlgorithmID.HMAC_SHA_256, AlgorithmID.Direct);
        cose.add(coseP);
        expiration = 30000L;
        db.addRS("rs7", profiles, scopes, auds, keyTypes, tokenTypes, cose,
                expiration, null, publicKey);
        
        //Setup client entries
        profiles.clear();
        profiles.add("coap_dtls");
        keyTypes.clear();
        keyTypes.add("RPK");
        db.addClient(
                "clientA", profiles, null, null, 
                keyTypes, null, publicKey, false);
  
        profiles.clear();
        profiles.add("coap_oscoap");
        keyTypes.clear();
        keyTypes.add("PSK");        
        db.addClient("clientB", profiles, "co2", "rs1", 
                keyTypes, skey, null, false);
        
        profiles.clear();
        profiles.add("coap_oscoap");
        keyTypes.clear();
        keyTypes.add("TST");        
        db.addClient("clientC", profiles, "co2", "sensors", 
                keyTypes, skey, null, false);
        
        profiles.clear();
        profiles.add("coap_dtls");
        keyTypes.clear();
        keyTypes.add("RPK");
        keyTypes.add("PSK");
        db.addClient("clientD", profiles, null, null, 
                keyTypes, skey, null, false);
        
        profiles.clear();
        profiles.add("coap_dtls");
        profiles.add("coap_oscoap");
        keyTypes.clear();
        keyTypes.add("RPK");
        keyTypes.add("PSK");
        db.addClient("clientE", profiles, null, null, 
                keyTypes, skey, publicKey, false);
        
        //Setup token entries
        byte[] cti = new byte[] {0x00};
        cti1 = Base64.getEncoder().encodeToString(cti);
        Map<Short, CBORObject> claims = new HashMap<>();
        claims.put(Constants.SCOPE, CBORObject.FromObject("co2"));
        claims.put(Constants.AUD,  CBORObject.FromObject("sensors"));
        claims.put(Constants.EXP, CBORObject.FromObject(1000000L));   
        claims.put(Constants.AUD,  CBORObject.FromObject("actuators"));
        claims.put(Constants.CTI, CBORObject.FromObject(cti));
        db.addToken(cti1, claims);
        
        cti = new byte[]{0x01};
        cti2 = Base64.getEncoder().encodeToString(cti);
        claims.clear();
        claims.put(Constants.SCOPE, CBORObject.FromObject("temp"));
        claims.put(Constants.AUD,  CBORObject.FromObject("actuators"));
        claims.put(Constants.EXP, CBORObject.FromObject(2000000L));
        claims.put(Constants.CTI, CBORObject.FromObject(cti));
        db.addToken(cti2, claims);
        t = new Token("AS", 
                KissPDP.getInstance(TestConfig.testFilePath + "acl.json", db),
                db, new KissTime(), privateKey); 
    }
    
    /**
     * Deletes the test DB after the tests
     * 
     * @throws AceException 
     * @throws SQLException 
     */
    @AfterClass
    public static void tearDown() throws AceException, SQLException {
        Properties connectionProps = new Properties();
        connectionProps.put("user", "root");
        connectionProps.put("password", dbPwd);
        Connection rootConn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306", connectionProps);
              
        String dropDB = "DROP DATABASE IF EXISTS " + DBConnector.dbName + ";";
        String dropUser = "DROP USER 'aceuser'@'localhost';";
        Statement stmt = rootConn.createStatement();
        stmt.execute(dropDB);
        stmt.execute(dropUser);
        stmt.close();
        rootConn.close();
        db.close();
    }
    
    
    /**
     * Test the token endpoint. Request should fail since it is unauthorized.
     * 
     * @throws Exception
     */
    @Test
    public void testFailUnauthorized() throws Exception {
        Map<Short, CBORObject> params = new HashMap<>();
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        LocalMessage msg = new LocalMessage(-1, "client_1", "TestAS", params); 
        Message response = t.processMessage(msg);
        assert(response.getMessageCode() == Message.FAIL_BAD_REQUEST);
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.ERROR, Constants.UNAUTHORIZED_CLIENT);
        Assert.assertArrayEquals(response.getRawPayload(), cbor.EncodeToBytes());
    }
    

    /**
     * Test the token endpoint. Request should fail since the scope is missing.
     * 
     * 
     * @throws Exception
     */
    @Test
    public void testFailMissingScope() throws Exception {
        Map<Short, CBORObject> params = new HashMap<>();
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        LocalMessage msg = new LocalMessage(-1, "clientA", "TestAS", params);
        Message response = t.processMessage(msg);
        assert(response.getMessageCode() == Message.FAIL_BAD_REQUEST);
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.ERROR, Constants.INVALID_REQUEST);
        cbor.Add(Constants.ERROR_DESCRIPTION, "No scope found for message");
        Assert.assertArrayEquals(response.getRawPayload(), 
                cbor.EncodeToBytes());
    }
    
    /**
     * Test the token endpoint. 
     * Request should fail since the audience is missing.
     * 
     * 
     * @throws Exception
     */
    @Test
    public void testFailMissingAudience() throws Exception {
        Map<Short, CBORObject> params = new HashMap<>();
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.SCOPE, CBORObject.FromObject("blah"));
        LocalMessage msg = new LocalMessage(-1, "clientA","TestAS", params);
        Message response = t.processMessage(msg);
        assert(response.getMessageCode() == Message.FAIL_BAD_REQUEST);
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.ERROR, Constants.INVALID_REQUEST);
        cbor.Add(Constants.ERROR_DESCRIPTION, "No audience found for message");
        Assert.assertArrayEquals(response.getRawPayload(), 
        cbor.EncodeToBytes());
    }
    
    /**
     * Test the token endpoint. 
     * Request should fail since the scope is not allowed.
     * 
     * 
     * @throws Exception
     */
    @Test
    public void testFailForbidden() throws Exception {  
        Map<Short, CBORObject> params = new HashMap<>();
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.SCOPE, CBORObject.FromObject("blah"));
        params.put(Constants.AUD, CBORObject.FromObject("blubb"));
        Message msg = new LocalMessage(-1, "clientA", "TestAS", params);
        Message response = t.processMessage(msg);
        assert(response.getMessageCode() == Message.FAIL_BAD_REQUEST);
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.ERROR, Constants.INVALID_SCOPE);
        Assert.assertArrayEquals(response.getRawPayload(), 
                cbor.EncodeToBytes());
    }
    
    /**
     * Test the token endpoint. 
     * Request should fail since the audience does not support
     * a common token type.
     * 
     * 
     * @throws Exception
     */
    @Test
    public void testFailIncompatibleTokenType() throws Exception { 
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.AUD, CBORObject.FromObject("failTokenType"));
        params.put(Constants.SCOPE, CBORObject.FromObject("failTokenType"));
        Message msg = new LocalMessage(-1, "clientB", "TestAS", params);
        Message response = t.processMessage(msg);
        assert(response.getMessageCode()
                == Message.FAIL_INTERNAL_SERVER_ERROR);
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.ERROR, "Audience incompatible on token type");
        Assert.assertArrayEquals(response.getRawPayload(), 
        cbor.EncodeToBytes());
    } 
    
    /**
     * Test the token endpoint. 
     * Request should fail since the audience does not support
     * a common profile.
     * 
     * 
     * @throws Exception
     */
    @Test
    public void testFailIncompatibleProfile() throws Exception {
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.AUD, CBORObject.FromObject("failProfile"));
        params.put(Constants.SCOPE, CBORObject.FromObject("failProfile"));
        Message msg = new LocalMessage(-1, "clientB", "TestAS", params);
        Message response = t.processMessage(msg);
        assert(response.getMessageCode()
                == Message.FAIL_INTERNAL_SERVER_ERROR);
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.ERROR, "No compatible profile found");
        Assert.assertArrayEquals(response.getRawPayload(), 
        cbor.EncodeToBytes());
    }
    
    /**
     * Test the token endpoint. 
     * Request should fail since the audience supports
     * an unknown token type.
     *  
     * @throws Exception
     */
    @Test
    public void testFailUnsupportedTokenType() throws Exception { 
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.AUD, CBORObject.FromObject("rs5"));
        params.put(Constants.SCOPE, CBORObject.FromObject("failTokenNotImplemented"));
        Message msg = new LocalMessage(-1, "clientA", "TestAS", params);
        Message response = t.processMessage(msg);      
        assert(response.getMessageCode()
                == Message.FAIL_NOT_IMPLEMENTED);
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.ERROR, "Unsupported token type");
        Assert.assertArrayEquals(response.getRawPayload(), 
        cbor.EncodeToBytes());
    }
    
    /**
     * Test the token endpoint. 
     * Request should fail since the client failed to provide an
     * RPK. 
     * 
     * @throws Exception
     */
    @Test
    public void testFailRpkNotProvided() throws Exception { 
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.AUD, CBORObject.FromObject("rs2"));
        params.put(Constants.SCOPE, CBORObject.FromObject("r_light"));
        Message msg = new LocalMessage(-1, "clientD", "TestAS", params);
        Message response = t.processMessage(msg);
        assert(response.getMessageCode() == Message.FAIL_BAD_REQUEST);
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.ERROR, Constants.INVALID_REQUEST);
        cbor.Add(Constants.ERROR_DESCRIPTION, "Client failed to provide RPK");
        Assert.assertArrayEquals(response.getRawPayload(), 
        cbor.EncodeToBytes());
    }
    
    /**
     * Test the token endpoint. 
     * Request should fail since the client provided an
     * unknown key type. 
     * 
     * @throws Exception
     */
    @Test
    public void testFailUnknownKeyType() throws Exception {
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.AUD, CBORObject.FromObject("rs6"));
        params.put(Constants.SCOPE, CBORObject.FromObject("r_valve"));
        Message msg = new LocalMessage(-1, "clientC", "TestAS", params);
        Message response = t.processMessage(msg);
        assert(response.getMessageCode() == Message.FAIL_BAD_REQUEST);
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.ERROR, Constants.UNSUPPORTED_POP_KEY);
        Assert.assertArrayEquals(response.getRawPayload(), 
        cbor.EncodeToBytes());
    }
    
    /**
     * Test the token endpoint. 
     * Request should fail since the audience does not
     * have a common CwtCryptoCtx
     * 
     * @throws Exception
     */
    @Test
    public void testFailIncompatibleCwt() throws Exception { 
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.AUD, CBORObject.FromObject("failCWTpar"));
        params.put(Constants.SCOPE, CBORObject.FromObject("co2"));
        Message msg = new LocalMessage(-1, "clientB", "TestAS", params);
        Message response = t.processMessage(msg);
        assert(response.getMessageCode() 
                == Message.FAIL_INTERNAL_SERVER_ERROR);
        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.ERROR, "No common security context found for audience");
        Assert.assertArrayEquals(response.getRawPayload(), 
        cbor.EncodeToBytes());
    }
    
    /**
     * Test the token endpoint. 
     * Request should succeed with default scope.
     * 
     * @throws Exception
     */
    @Test
    public void testSucceedDefaultScope() throws Exception { 
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.AUD, CBORObject.FromObject("rs1"));
        Message msg = new LocalMessage(-1, "clientB", "TestAS", params);
        Message response = t.processMessage(msg);
        CBORObject rparams = CBORObject.DecodeFromBytes(
                response.getRawPayload());
        params = Constants.getParams(rparams);
        assert(response.getMessageCode() 
                == Message.CREATED);
        CBORObject token = params.get(Constants.ACCESS_TOKEN);
        CWT cwt = CWT.processCOSE(token.EncodeToBytes(), CwtCryptoCtx.sign1Verify(
                publicKey, AlgorithmID.ECDSA_256.AsCBOR()));
        assert(cwt.getClaim(Constants.AUD).AsString().equals("rs1"));
    }
    
    /**
     * Test the token endpoint. 
     * Request should succeed with default audience.
     * 
     * @throws Exception
     */
    @Test
    public void testSucceedDefaultAud() throws Exception { 
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.SCOPE, CBORObject.FromObject("co2"));
        Message msg = new LocalMessage(-1, "clientB", "TestAS", params);
        Message response = t.processMessage(msg);
        CBORObject rparams = CBORObject.DecodeFromBytes(
                response.getRawPayload());
        params = Constants.getParams(rparams);
        assert(response.getMessageCode() 
                == Message.CREATED);
        CBORObject token = params.get(Constants.ACCESS_TOKEN);
        CWT cwt = CWT.processCOSE(token.EncodeToBytes(), CwtCryptoCtx.sign1Verify(
                publicKey, AlgorithmID.ECDSA_256.AsCBOR()));
        assert(cwt.getClaim(Constants.SCOPE).AsString().equals("co2"));
    }
    
    /**
     * Test the token endpoint, creating a REF token with multiple scopes, one
     * of which is not allowed. Request should succeed, but not give access to
     * scope "foobar"
     * 
     * @throws Exception
     */
    @Test
    public void testSucceed() throws Exception { 
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.SCOPE, 
                CBORObject.FromObject("rw_valve r_pressure foobar"));
        params.put(Constants.AUD, CBORObject.FromObject("rs3"));
        Message msg = new LocalMessage(-1, "clientB", "TestAS", params);
        Message response = t.processMessage(msg);
        CBORObject rparams = CBORObject.DecodeFromBytes(
                response.getRawPayload());
        params = Constants.getParams(rparams);
        assert(response.getMessageCode() == Message.CREATED);
        CBORObject token = params.get(Constants.ACCESS_TOKEN);
        String cti = Base64.getEncoder().encodeToString(token.GetByteString());
        Map<Short, CBORObject> claims = db.getClaims(cti);
        assert(claims.get(Constants.SCOPE).AsString().contains("rw_valve"));
        assert(claims.get(Constants.SCOPE).AsString().contains("r_pressure"));
        assert(!claims.get(Constants.SCOPE).AsString().contains("foobar"));
    }
    
    
    /**
     * Test with COSE_Encrypt in cnf parameter
     *
     * @throws AceException 
     * @throws CoseException 
     * @throws InvalidCipherTextException 
     * @throws IllegalStateException 
     */
    @Test
    public void testSucceedCE() throws AceException, CoseException, 
            IllegalStateException, InvalidCipherTextException {
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.SCOPE, 
                CBORObject.FromObject("rw_valve"));
        params.put(Constants.AUD, CBORObject.FromObject("rs3"));
        CBORObject rpk = CBORObject.NewMap();
        Encrypt0Message enc = new Encrypt0Message();
        enc.addAttribute(HeaderKeys.Algorithm, 
                AlgorithmID.AES_CCM_16_128_128.AsCBOR(), 
                Attribute.PROTECTED);
        enc.SetContent(publicKey.EncodeToBytes());
        enc.encrypt(key128);
        rpk.Add(Constants.COSE_ENCRYPTED_CBOR, enc.EncodeToCBORObject());
        params.put(Constants.CNF, rpk);
        Message msg = new LocalMessage(-1, "clientB", "TestAS", params);
        Message response = t.processMessage(msg);
        CBORObject rparams = CBORObject.DecodeFromBytes(
                response.getRawPayload());
        params = Constants.getParams(rparams);
        assert(response.getMessageCode() == Message.CREATED);
        CBORObject token = params.get(Constants.ACCESS_TOKEN);
        String cti = Base64.getEncoder().encodeToString(token.GetByteString());
        Map<Short, CBORObject> claims = db.getClaims(cti);
        assert(claims.get(Constants.SCOPE).AsString().contains("rw_valve"));
    }
    
    /**
     * Test with kid only in cnf parameter
     *
     * @throws AceException  
     */        
    @Test
    public void testSucceedCnfKid() throws AceException {
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        params.put(Constants.SCOPE, 
                CBORObject.FromObject("r_pressure"));
        params.put(Constants.AUD, CBORObject.FromObject("rs3"));
        CBORObject cnf = CBORObject.NewMap();
        cnf.Add(Constants.COSE_KID_CBOR, publicKey.get(KeyKeys.KeyId));
        params.put(Constants.CNF, cnf);
        Message msg = new LocalMessage(-1, "clientE", "TestAS", params);
        Message response = t.processMessage(msg);
        CBORObject rparams = CBORObject.DecodeFromBytes(
                response.getRawPayload());
        params = Constants.getParams(rparams);
        assert(response.getMessageCode() == Message.CREATED);
        CBORObject token = params.get(Constants.ACCESS_TOKEN);
        String ctiStr = Base64.getEncoder().encodeToString(
                token.GetByteString());
        Map<Short, CBORObject> claims = db.getClaims(ctiStr);
        assert(claims.get(Constants.SCOPE).AsString().contains("r_pressure"));
        CBORObject cnf2 = claims.get(Constants.CNF);
        assert(cnf.equals(cnf2));
    }
    
    
    /**
     * Test the token endpoint. Test purging expired tokens.
     * 
     * @throws Exception
     */
    @Test
    public void testPurge() throws Exception {
        Map<Short, CBORObject> claims = db.getClaims(cti1);
        assert(!claims.isEmpty());
        db.purgeExpiredTokens(1000001L);
        claims = db.getClaims(cti1);
        assert(claims.isEmpty());
    }
    
    /**
     * Test the token endpoint. Test removing a specific token.
     * 
     * @throws Exception
     */
    @Test
    public void testRemoveToken() throws Exception { 
        Map<Short, CBORObject> claims = db.getClaims(cti2);
        assert(!claims.isEmpty());
        db.deleteToken(cti2);
        claims = db.getClaims(cti2);
        assert(claims.isEmpty());
    }
    
    /**
     * Test the token endpoint by requesting multiple tokens and
     * checking that the cti counter is correctly adjusted.
     * This uses default audience and scope.
     * 
     * @throws Exception
     */
    @Test
    public void testMultiRequest() throws Exception {
        Map<Short, CBORObject> params = new HashMap<>(); 
        params.put(Constants.GRANT_TYPE, Token.clientCredentials);
        Message msg = new LocalMessage(-1, "clientB", "TestAS", params);
        t.processMessage(msg);
        Long ctiCtrStart = db.getCtiCounter();
        for (int i=0; i<10; i++) {
            t.processMessage(msg);
        }
        Long ctiCtrEnd = db.getCtiCounter();
        assert(ctiCtrEnd == ctiCtrStart+10);
        
    }

}
