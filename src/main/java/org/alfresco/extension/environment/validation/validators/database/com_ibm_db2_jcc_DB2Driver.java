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

import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.alfresco.extension.util.ComparablePair;


/**
 * This class defines the database validation rules specific to the DB2 relational database and JDBC driver. 
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class com_ibm_db2_jcc_DB2Driver
    extends AbstractDBSpecificValidator
{
    // Supported DB2 version
    // See http://www-01.ibm.com/support/docview.wss?rs=71&uid=swg21363866 for a list of JDBC driver versions
    private final static ComparablePair[] SUPPORTED_JDBC_DRIVER_VERSION   = { new ComparablePair(new Integer(3), new Integer(57)),   // DB2 9.7 GA
                                                                              new ComparablePair(new Integer(3), new Integer(58)),   // DB2 9.7 FP1
                                                                              new ComparablePair(new Integer(3), new Integer(59)),   // DB2 9.7 FP2
                                                                              new ComparablePair(new Integer(3), new Integer(60)),   // DB2 9.7 FP3??
                                                                              new ComparablePair(new Integer(3), new Integer(61)) }; // DB2 9.7 FP4??
    private final static String           SUPPORTED_DB2_VERSION           = "9.7";
    private final static String           SUPPORTED_DB2_VERSION_SIGNATURE = SUPPORTED_DB2_VERSION + ".";
    
    // "More information" URIs
    private final static String[] DB2_URI                            = { "http://www-01.ibm.com/software/data/db2/linux-unix-windows/download.html" };
    private final static String[] JDBC_URI                           = DB2_URI;
    private final static String[] DB2_CONFIGURING_CHARACTER_SETS_URI = { /* ####TODO!!!! */ };

    
    /**
     * @see org.alfresco.extension.environment.validation.validators.database.DBSpecificValidator#validate(org.alfresco.extension.environment.validation.ValidatorCallback, java.sql.Connection)
     */
    public void validate(final ValidatorCallback callback, final Connection con)
    {
        validateJdbcDriverVersion(callback, con, SUPPORTED_JDBC_DRIVER_VERSION, JDBC_URI);
        validateDatabaseVersion(callback, con);
        validateEncoding(callback, con);
    }
    
    private void validateDatabaseVersion(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "DB2 Version");
        
        TestResult testResult = new TestResult();
        
        try
        {
            Map row = singletonQuery(con, "SELECT SERVICE_LEVEL FROM TABLE (sysproc.env_get_inst_info()) AS A");
            
            if (row != null)
            {
                String version = (String)row.get("SERVICE_LEVEL");
                
                if (version != null && version.trim().length() > 0)
                {
                    if (version.startsWith("DB2 v"))
                    {
                        version = version.substring("DB2 v".length());
                    }
                    
                    if (version.startsWith("DB2 "))
                    {
                        version = version.substring("DB2 ".length());
                    }
                    
                    progress(callback, version);
                    
                    if (version.startsWith(SUPPORTED_DB2_VERSION_SIGNATURE))
                    {
                        testResult.resultType = TestResult.PASS;
                    }
                    else
                    {
                        testResult.resultType          = TestResult.FAIL;
                        testResult.errorMessage        = "Unsupported DB2 version";
                        testResult.ramification        = "Alfresco will not function correctly on this version";
                        testResult.remedy              = "Install DB2 " + SUPPORTED_DB2_VERSION;
                        testResult.urisMoreInformation = DB2_URI;
                    }
                }
                else
                {
                    progress(callback, "unknown");
                    
                    testResult.resultType   = TestResult.FAIL;
                    testResult.errorMessage = "Unable to determine DB2 version";
                    testResult.ramification = "Alfresco may not function correctly";
                    testResult.remedy       = "Manually validate that DB2 " + SUPPORTED_DB2_VERSION + " is installed";
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = "Unable to determine DB2 version";
                testResult.ramification = "Alfresco may not function correctly";
                testResult.remedy       = "Manually validate that DB2 " + SUPPORTED_DB2_VERSION + " is installed";
            }
        }
        catch (SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.FAIL;
            testResult.errorMessage = "Unable to determine DB2 version";
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually validate that DB2 " + SUPPORTED_DB2_VERSION + " is installed";
            testResult.rootCause    = se;
        }
        
        endTest(callback, testResult);
    }
    
    
    private final void validateEncoding(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "Server Encoding");
        
        TestResult testResult = new TestResult();
        
        try
        {
            Map row = singletonQuery(con, "SELECT VALUE AS ENCODING FROM SYSIBMADM.DBCFG WHERE NAME = 'codeset'");
            
            if (row != null)
            {
                String encoding = (String)row.get("ENCODING");
                
                if (encoding != null && encoding.trim().length() > 0)
                {
                    progress(callback, encoding);
                    
                    if ("UTF-8".equals(encoding))
                    {
                        testResult.resultType = TestResult.PASS;
                    }
                    else
                    {
                        testResult.resultType          = TestResult.FAIL;
                        testResult.errorMessage        = "Database character encoding must be 'UTF-8' but is not";
                        testResult.ramification        = "Alfresco will not function correctly";
                        testResult.remedy              = "Correct the DB2 character set configuration and rerun this test";
                        testResult.urisMoreInformation = DB2_CONFIGURING_CHARACTER_SETS_URI;
                    }
                }
                else
                {
                    progress(callback, "unknown");
                    
                    testResult.resultType   = TestResult.WARN;
                    testResult.errorMessage = "Unable to determine database character encoding";
                    testResult.ramification = "Alfresco may not function correctly";
                    testResult.remedy       = "Manually validate that the database character encoding is 'UTF-8'";
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "Unable to determine database character encoding";
                testResult.ramification = "Alfresco may not function correctly";
                testResult.remedy       = "Manually validate that the database character encoding is 'UTF-8'";
            }
        }
        catch (SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine database character encoding";
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually validate that the database character encoding is 'UTF-8'";
            testResult.rootCause    = se;
        }
        
        endTest(callback, testResult);
    }
    
}
