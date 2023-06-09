package se.sics.ace.as;

import COSE.CoseException;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import com.upokecenter.cbor.CBORType;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.ace.AceException;
import se.sics.ace.Constants;
import se.sics.ace.DBHelper;
import se.sics.ace.Message;
import se.sics.ace.examples.KissPDP;
import se.sics.ace.examples.LocalMessage;
import se.sics.ace.examples.SQLConnector;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Test the trl endpoint class.
 *
 * @author Marco Rasori
 */
public class TestTrlEndpoint {

    private static SQLConnector db = null;
    private static Trl r = null;

    /**
     * Set up tests.
     *
     * @throws AceException
     * @throws SQLException
     * @throws IOException
     * @throws CoseException
     */
    @BeforeClass
    public static void setUp() throws AceException, SQLException, IOException, CoseException {

        DBHelper.setUpDB(null);
        db = DBHelper.getSQLConnector();
        Map<String, String> peerIdentitiesToNames = new HashMap<>();

        peerIdentitiesToNames.put("Id1", "clientA");
        peerIdentitiesToNames.put("Id2", "clientB");
        peerIdentitiesToNames.put("Id3", "clientC");
        peerIdentitiesToNames.put("Id4", "rs1");
        peerIdentitiesToNames.put("Id5", "rs2");
        db.addClient("clientA",new HashSet<String>(){{add("coap_dtls");}},
                null,null,new HashSet<String>(){{add("PSK");}},new OneKey(),null);
        db.addClient("clientB",new HashSet<String>(){{add("coap_dtls");}},
                null,null,new HashSet<String>(){{add("PSK");}},new OneKey(),null);
        db.addClient("clientC",new HashSet<String>(){{add("coap_dtls");}},
                null,null,new HashSet<String>(){{add("PSK");}},new OneKey(),null);

        db.addCti2Peers("cti1", "clientB", new HashSet<String>() {{add("rs1");}});
        db.addRevokedToken("cti1");
        db.addCti2TokenHash("cti1", "tokenHash1");

        db.addCti2Peers("cti2", "clientA", new HashSet<String>() {{add("rs1");}});
        db.addRevokedToken("cti2");
        db.addCti2TokenHash("cti2", "tokenHash2");

        r = new Trl(db, peerIdentitiesToNames, 10, null);
    }

    /**
     * Deletes the test DB after the tests
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        //pdp.close();

        DBHelper.tearDownDB();
    }

    /**
     * Test the trl endpoint, test should fail with Unauthorized client:
     * Id6 is not allowed to observe
     *
     * @throws Exception
     */
    @Test
    public void testUnauthorizedClient() throws Exception {
        Message msg = new LocalMessage(-1, "Id6", null, (CBORObject) null);

        r.setQueryParameters(new HashMap<>());
        r.setHasObserve(false);

        Message response = r.processMessage(msg);

        assert (response.getMessageCode() == Message.FAIL_UNAUTHORIZED);

        CBORObject cbor = CBORObject.NewMap();
        cbor.Add(Constants.ERROR, Constants.UNAUTHORIZED_CLIENT);

        Assert.assertArrayEquals(cbor.EncodeToBytes(), response.getRawPayload());

    }


    /**
     * Test the trl endpoint, test should succeed and return a CBOR map
     * containing FULL_SET and CURSOR.
     * FULL_SET is a CBOR array that should be empty as Id3 has no pertaining tokens in the trl
     *
     * @throws Exception
     */
    @Test
    public void testSucceedNoHashes() throws Exception {
        Message msg = new LocalMessage(-1, "Id3", null, (CBORObject) null);

        r.setQueryParameters(new HashMap<>());
        r.setHasObserve(false);

        Message response = r.processMessage(msg);

        assert (response.getMessageCode() == Message.CONTENT);

        CBORObject fullSet = CBORObject.DecodeFromBytes(response.getRawPayload()).get(Constants.FULL_SET);
        assert(fullSet.size() == 0);
        assert(CBORObject.DecodeFromBytes(response.getRawPayload()).ContainsKey(Constants.CURSOR));



    }

    /**
     * Test the trl endpoint, test should succeed and return a CBOR map
     * containing FULL_SET and CURSOR.
     * Check that FULL_SET is a CBOR array, its size is 1, and contains what expected.
     * (CURSOR is null since no revocation was "really" made by the RevocationHandler)
     *
     * @throws Exception
     */
    @Test
    public void testSucceed() throws Exception {
        Message msg = new LocalMessage(-1, "Id2", null, (CBORObject) null);

        r.setQueryParameters(new HashMap<>());
        r.setHasObserve(false);

        Message response = r.processMessage(msg);

        assert (response.getMessageCode() == Message.CONTENT);

        CBORObject hashes = CBORObject.NewArray();
        String tokenHash = "tokenHash1";
        hashes.Add(CBORObject.FromObject(tokenHash.getBytes(Constants.charset)));

        CBORObject fullSet = CBORObject.DecodeFromBytes(response.getRawPayload()).get(Constants.FULL_SET);
        assert(fullSet.getType() == CBORType.Array);
        assert(fullSet.size() == 1);
        assert(hashes.CompareToIgnoreTags(fullSet) == 0);
        assert(CBORObject.DecodeFromBytes(response.getRawPayload()).ContainsKey(Constants.CURSOR));
    }


    /**
     * Test the parsing of query parameters at the trl endpoint
     * In all these cases, the test should return a 'invalid parameter value' error.
     *
     * @throws Exception
     */
    @Test
    public void testInvalidParameterValue() throws Exception {
        Message msg = new LocalMessage(-1, "Id2", null, (CBORObject) null);

        // 'diff' lower than 0
        Map<String,Integer> qParams = new HashMap<>();
        qParams.put("diff", -3);
        r.setQueryParameters(qParams);
        r.setHasObserve(false);

        Message response = r.processMessage(msg);

        CBORObject payload = CBORObject.DecodeFromBytes(response.getRawPayload());
        Map<Short, CBORObject> map = Constants.getParams(payload);
        assert(map.containsKey(Constants.TRL_ERROR));
        Assert.assertEquals(map.get(Constants.TRL_ERROR).AsNumber().ToInt16Checked(), Constants.INVALID_PARAMETER_VALUE);


        // 'pmax' equal to 0 with observe option set
        msg = new LocalMessage(-1, "Id2", null, (CBORObject) null);

        qParams = new HashMap<>();
        qParams.put("pmax", 0);
        r.setQueryParameters(qParams);
        r.setHasObserve(true);

        response = r.processMessage(msg);

        payload = CBORObject.DecodeFromBytes(response.getRawPayload());
        map = Constants.getParams(payload);
        assert(map.containsKey(Constants.TRL_ERROR));
        Assert.assertEquals(map.get(Constants.TRL_ERROR).AsNumber().ToInt16Checked(), Constants.INVALID_PARAMETER_VALUE);


        // 'pmax' lower than 0 with observe option set
        msg = new LocalMessage(-1, "Id2", null, (CBORObject) null);

        qParams = new HashMap<>();
        qParams.put("pmax", 0);
        r.setQueryParameters(qParams);
        r.setHasObserve(true);

        response = r.processMessage(msg);

        payload = CBORObject.DecodeFromBytes(response.getRawPayload());
        map = Constants.getParams(payload);
        assert(map.containsKey(Constants.TRL_ERROR));
        Assert.assertEquals(map.get(Constants.TRL_ERROR).AsNumber().ToInt16Checked(), Constants.INVALID_PARAMETER_VALUE);


        // 'cursor' lower than 0
        msg = new LocalMessage(-1, "Id2", null, (CBORObject) null);

        qParams = new HashMap<>();
        qParams.put("diff", 1);
        qParams.put("cursor", -1);
        r.setQueryParameters(qParams);
        r.setHasObserve(false);

        response = r.processMessage(msg);

        payload = CBORObject.DecodeFromBytes(response.getRawPayload());
        map = Constants.getParams(payload);
        assert(map.containsKey(Constants.TRL_ERROR));
        Assert.assertEquals(map.get(Constants.TRL_ERROR).AsNumber().ToInt16Checked(), Constants.INVALID_PARAMETER_VALUE);
    }

    /**
     * Test the parsing of query parameters at the trl endpoint
     * In all these cases, the test should return a 'invalid set of parameters' error.
     *
     * @throws Exception
     */
    @Test
    public void testInvalidSetOfParameters() throws Exception {

        // 'cursor' present and 'diff' not present
        Message msg = new LocalMessage(-1, "Id2", null, (CBORObject) null);

        Map<String, Integer> qParams = new HashMap<>();
        qParams.put("cursor", 1);
        r.setQueryParameters(qParams);
        r.setHasObserve(false);

        Message response = r.processMessage(msg);

        CBORObject payload = CBORObject.DecodeFromBytes(response.getRawPayload());
        Map<Short, CBORObject> map = Constants.getParams(payload);
        assert (map.containsKey(Constants.TRL_ERROR));
        Assert.assertEquals(map.get(Constants.TRL_ERROR).AsNumber().ToInt16Checked(), Constants.INVALID_SET_OF_PARAMETERS);
    }

    /**
     * Test the parsing of query parameters at the trl endpoint
     * In all these cases, the test should return a 'invalid set of parameters' error.
     *
     * @throws Exception
     */
    @Test
    public void testIgnorePmaxIfObserveNotSet() throws Exception {

        // valid 'pmax' with observe option not set
        Message msg = new LocalMessage(-1, "Id2", null, (CBORObject) null);

        Map<String, Integer> qParams = new HashMap<>();
        qParams.put("pmax", 1);
        r.setQueryParameters(qParams);
        r.setHasObserve(false);

        Message response = r.processMessage(msg);

        // if the trl responds with a CBOR map, check that the map does not contain an error.
        // The trl can use a CBOR map for a successful message when the "third mode" --the
        // one that uses 'cursor'-- is used.
        CBORObject payload = CBORObject.DecodeFromBytes(response.getRawPayload());
        if (payload.getType() == CBORType.Map) {
            Map<Short, CBORObject> map = Constants.getParams(payload);
            assert (!map.containsKey(Constants.TRL_ERROR));
        }
        // otherwise, the trl responds with a CBOR array
        else
            assert(payload.getType() == CBORType.Array);

    }
}
