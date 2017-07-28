/*******************************************************************************
 * AAIoT update to ACE-Java
 *
 * Copyright 2017 Carnegie Mellon University. All Rights Reserved.
 *
 * NO WARRANTY. THIS CARNEGIE MELLON UNIVERSITY AND SOFTWARE ENGINEERING
 * INSTITUTE MATERIAL IS FURNISHED ON AN "AS-IS" BASIS. CARNEGIE MELLON
 * UNIVERSITY MAKES NO WARRANTIES OF ANY KIND, EITHER EXPRESSED OR IMPLIED, AS TO
 * ANY MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE OR
 * MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM USE OF THE MATERIAL.
 * CARNEGIE MELLON UNIVERSITY DOES NOT MAKE ANY WARRANTY OF ANY KIND WITH RESPECT
 * TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.
 *
 * Released under a BSD-style license, please see sei-license.txt or contact
 * permission@sei.cmu.edu for full terms.
 *
 * [DISTRIBUTION STATEMENT A] This material has been approved for public
 * release and unlimited distribution.
 * Please see Copyright notice for non-US Government use and distribution.
 * This Software includes and/or makes use of the following Third-Party Software
 * subject to its own license:
 *
 * 1. ACE-Java (https: * bitbucket.org/lseitz/ace-java) Copyright 2016 SICS
 * Swedish ICT AB.
 *
 * DM17-0098
 *******************************************************************************/
package se.sics.ace.as;

import COSE.CoseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import se.sics.ace.AceException;
import se.sics.ace.examples.PostgreSQLDBAdapter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Test the database connection classes using PostgreSQL.
 *
 * @author Sebastian Echeverria.
 */
public class TestPostgresqlDB extends TestDB {

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

        TestDB.setUp(new PostgreSQLDBAdapter());
    }

    /**
     * Deletes the test DB after the tests
     *
     * @throws SQLException
     * @throws AceException
     */
    @AfterClass
    public static void tearDown() throws SQLException, AceException {
        if(db != null) {
            db.close();
        }

        Properties connectionProps = new Properties();
        connectionProps.put("user", PostgreSQLDBAdapter.ROOT_USER);
        connectionProps.put("password", dbPwd);
        Connection rootConn = DriverManager.getConnection(
                PostgreSQLDBAdapter.DEFAULT_DB_URL + "/" + PostgreSQLDBAdapter.BASE_DB, connectionProps);

        String dropDB = "DROP DATABASE " + DBConnector.dbName + ";";
        String dropUser = "DROP ROLE " + TestDB.testUsername + ";";
        Statement stmt = rootConn.createStatement();
        stmt.execute(dropDB);
        stmt.execute(dropUser);
        stmt.close();
        rootConn.close();
    }
}
