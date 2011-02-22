/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.alfresco.extension.environment.validation.validators.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.alfresco.extension.util.ComparablePair;


/**
 * This class defines the database validation rules specific to the Oracle relational database and JDBC driver.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class oracle_jdbc_OracleDriver
    extends AbstractDBSpecificValidator
{
    // Supported Oracle versions
    private final static ComparablePair[] SUPPORTED_JDBC_DRIVER_VERSION = { new ComparablePair(new Integer(11), new Integer(2)) };
    private final static String           ORACLE_10_2                   = "10.2";
    private final static String           ORACLE_10_2_0_4               = ORACLE_10_2 + ".0.4";
    private final static String           ORACLE_11_2                   = "11.2";
    private final static String           ORACLE_11_2_0_1_0             = ORACLE_11_2 + ".0.1.0";
    
    // "More information" URIs
    private final static String   ORACLE_URI_STR                        = "http://www.oracle.com/technology/software/products/database/";
    private final static String[] ORACLE_URI                            = { ORACLE_URI_STR };
    private final static String[] JDBC_URI                              = { "http://www.oracle.com/technetwork/database/features/jdbc/index-091264.html" };
    private final static String[] ORACLE_CONFIGURING_CHARACTER_SETS_URI = { "http://download.oracle.com/docs/cd/B19306_01/install.102/b14317/gblsupp.htm#BCEEHABC" };
    private final static String[] ALFRESCO_SPM_AND_ORACLE_URIS          = { AbstractValidator.ALFRESCO_SUMMARY_SPM_URI_STR, ORACLE_URI_STR };
    
    private final static Pattern ORACLE_VERSION_NUMBER_PATTERN = Pattern.compile("([0-9\\.]+)");
    

    /**
     * @see org.alfresco.extension.environment.validation.validators.database.DBSpecificValidator#validate(org.alfresco.extension.environment.validation.ValidatorCallback, java.sql.Connection)
     */
    public void validate(ValidatorCallback callback, Connection con)
    {
        validateJdbcDriverVersion(callback, con, SUPPORTED_JDBC_DRIVER_VERSION, JDBC_URI);
        validateDatabaseVersion(callback, con);
        validateEncoding(callback, con);
    }
    
    
    private void validateDatabaseVersion(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "Oracle Version");
        
        TestResult testResult = new TestResult();
        
        try
        {
            Map row = singletonQuery(con, "SELECT BANNER FROM V$VERSION WHERE BANNER LIKE 'CORE%'");
            
            if (row != null)
            {
                String banner = (String)row.get("BANNER");
                
                if (banner != null && banner.trim().length() > 0)
                {
                    Matcher matcher = ORACLE_VERSION_NUMBER_PATTERN.matcher(banner);
                    
                    if (matcher.find())
                    {
                        String version = matcher.group(1);
                        
                        if (version != null && version.trim().length() > 0)
                        {
                            progress(callback, version);
                            
                            if (version.startsWith(ORACLE_10_2))
                            {
                                if (version.startsWith(ORACLE_10_2_0_4))
                                {
                                    testResult.resultType = TestResult.PASS;
                                }
                                else
                                {
                                    testResult.resultType          = TestResult.WARN;
                                    testResult.errorMessage        = "Unsupported Oracle 10g version";
                                    testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                                    testResult.remedy              = "Install Oracle v" + ORACLE_10_2_0_4;
                                    testResult.urisMoreInformation = ALFRESCO_SPM_AND_ORACLE_URIS;
                                }
                            }
                            else if (version.startsWith(ORACLE_11_2))
                            {
                                if (version.startsWith(ORACLE_11_2_0_1_0))
                                {
                                    testResult.resultType = TestResult.PASS;
                                }
                                else
                                {
                                    testResult.resultType          = TestResult.WARN;
                                    testResult.errorMessage        = "Unsupported Oracle 11g version";
                                    testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                                    testResult.remedy              = "Install Oracle v" + ORACLE_11_2_0_1_0;
                                    testResult.urisMoreInformation = ALFRESCO_SPM_AND_ORACLE_URIS;
                                }
                            }
                            else
                            {
                                testResult.resultType          = TestResult.FAIL;
                                testResult.errorMessage        = "Unsupported Oracle version";
                                testResult.ramification        = "Alfresco will not function correctly on this version";
                                testResult.remedy              = "Install Oracle 10g (v" + ORACLE_10_2_0_4 + ") or 11g (v" + ORACLE_11_2_0_1_0 + ")";
                                testResult.urisMoreInformation = ALFRESCO_SPM_AND_ORACLE_URIS;
                            }
                        }
                        else
                        {
                            progress(callback, "unknown");
                            
                            testResult.resultType   = TestResult.FAIL;
                            testResult.errorMessage = "Unable to determine Oracle version";
                            testResult.ramification = "Alfresco may not function correctly";
                            testResult.remedy       = "Manually validate that Oracle 10g (v" + ORACLE_10_2_0_4 + ") or 11g (v" + ORACLE_11_2_0_1_0 + ") is installed";
                        }
                    }
                    else
                    {
                        progress(callback, "unknown");
                        
                        testResult.resultType   = TestResult.FAIL;
                        testResult.errorMessage = "Unable to determine Oracle version";
                        testResult.ramification = "Alfresco may not function correctly";
                        testResult.remedy       = "Manually validate that Oracle 10g (v" + ORACLE_10_2_0_4 + ") or 11g (v" + ORACLE_11_2_0_1_0 + ") is installed";
                    }
                }
                else
                {
                    progress(callback, "unknown");
                    
                    testResult.resultType   = TestResult.FAIL;
                    testResult.errorMessage = "Unable to determine Oracle version";
                    testResult.ramification = "Alfresco may not function correctly";
                    testResult.remedy       = "Manually validate that Oracle 10g (v" + ORACLE_10_2_0_4 + ") or 11g (v" + ORACLE_11_2_0_1_0 + ") is installed";
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = "Unable to determine Oracle version";
                testResult.ramification = "Alfresco may not function correctly";
                testResult.remedy       = "Manually validate that Oracle 10g (v" + ORACLE_10_2_0_4 + ") or 11g (v" + ORACLE_11_2_0_1_0 + ") is installed";
            }
        }
        catch (SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.FAIL;
            testResult.errorMessage = "Unable to determine Oracle version";
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually validate that Oracle 10g (v" + ORACLE_10_2_0_4 + ") or 11g (v" + ORACLE_11_2_0_1_0 + ") is installed";
            testResult.rootCause    = se;
        }
        
        endTest(callback, testResult);
    }
    
    
    private final void validateEncoding(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "Database Encoding");
        
        TestResult testResult = new TestResult();

        try
        {
            Map    row      = singletonQuery(con, "SELECT VALUE FROM NLS_DATABASE_PARAMETERS WHERE PARAMETER = 'NLS_CHARACTERSET'");
            String encoding = (String)row.get("VALUE");

            if (encoding != null && encoding.trim().length() > 0)
            {
                progress(callback, encoding);
                
                if (encoding.equalsIgnoreCase("AL32UTF8"))
                {
                    testResult.resultType = TestResult.PASS;
                }
                else if (encoding.equalsIgnoreCase("UTF8"))
                {
                    testResult.resultType          = TestResult.WARN;
                    testResult.errorMessage        = "It is recommended to use the newer 'AL32UTF8' (Unicode 4.0) encoding, rather than the older 'UTF8' (Unicode 3.0) encoding";
                    testResult.ramification        = "Data loss may occur if new (Unicode 4.0) characters are used in metadata";
                    testResult.remedy              = "Modify the Oracle character set configuration to 'AL32UTF8'";
                    testResult.urisMoreInformation = ORACLE_CONFIGURING_CHARACTER_SETS_URI;
                }
                else
                {
                    testResult.resultType          = TestResult.FAIL;
                    testResult.errorMessage        = "Database character encoding must be 'AL32UTF8' (recommended) or 'UTF8', but is neither";
                    testResult.ramification        = "Data loss will occur if extended (non-ASCII) characters are used in metadata";
                    testResult.remedy              = "Correct the Oracle character set configuration and rerun this test";
                    testResult.urisMoreInformation = ORACLE_CONFIGURING_CHARACTER_SETS_URI;
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "Unable to determine database character encoding";
                testResult.ramification = "Alfresco may not function correctly";
                testResult.remedy       = "Manually execute the SQL statement 'SELECT VALUE FROM NLS_DATABASE_PARAMETERS WHERE PARAMETER = 'NLS_CHARACTERSET';' " +
                                          "and ensure that the values is 'AL32UTF8'";
            }
        }
        catch (SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine database character encoding";
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually execute the SQL statement 'SELECT VALUE FROM NLS_DATABASE_PARAMETERS WHERE PARAMETER = 'NLS_CHARACTERSET';' " +
                                      "and ensure that the values is 'AL32UTF8'";
            testResult.rootCause    = se;
        }
        
        endTest(callback, testResult);
    }
        
}
