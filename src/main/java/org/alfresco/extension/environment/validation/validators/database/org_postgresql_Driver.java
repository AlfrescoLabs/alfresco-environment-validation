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
import java.util.List;
import java.util.Map;

import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.alfresco.extension.util.ComparablePair;


/**
 * This class defines the database validation rules specific to the PostgreSQL relational database and JDBC driver.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class org_postgresql_Driver
    extends AbstractDBSpecificValidator
{
    // Supported PostgreSQL versions
    private final static ComparablePair[] SUPPORTED_JDBC_DRIVER_VERSION        = { new ComparablePair(new Integer(9), new Integer(0)) };
    private final static String           DEVELOPMENT_ONLY_POSTGRESQL_VERSIONS = "9.2";
    private final static String           SUPPORTED_POSTGRESQL_VERSION         = DEVELOPMENT_ONLY_POSTGRESQL_VERSIONS + ".4";
    
    // "More information" URIs
    private final static String   POSTGRESQL_URI_STR                        = "http://www.postgresql.org/download/";
    private final static String[] JDBC_URI                                  = { "http://jdbc.postgresql.org/download.html" };
    private final static String[] POSTGRESQL_CONFIGURING_CHARACTER_SETS_URI = { "http://www.postgresql.org/docs/" + DEVELOPMENT_ONLY_POSTGRESQL_VERSIONS + "/interactive/multibyte.html" };
    private final static String[] ALFRESCO_SPM_AND_POSTGRESQL_URIS          = { AbstractValidator.ALFRESCO_SUMMARY_SPM_URI_STR, POSTGRESQL_URI_STR }; 
    
                         

    /**
     * @see org.alfresco.extension.environment.validation.validators.database.DBSpecificValidator#validate(org.alfresco.extension.environment.validation.ValidatorCallback, java.sql.Connection)
     */
    public void validate(ValidatorCallback callback, Connection con)
    {
        setupConnection(con);
        
        validateJdbcDriverVersion(callback, con, SUPPORTED_JDBC_DRIVER_VERSION, JDBC_URI);
        validateDatabaseVersion(callback, con);
        validateEncoding(callback, con);
    }
    
    
    private void setupConnection(final Connection con)
    {
        try
        {
            sql(con, "SET NAMES 'UTF8'");  // Force our connection to be UTF8 in all cases
        }
        catch (SQLException se)
        {
            // Ignore - we don't really care if these statements fail
        }
    }
    
    private void validateDatabaseVersion(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "PostgreSQL Version");
        
        TestResult testResult = new TestResult();
        
        try
        {
            Map row = singletonQuery(con, "SELECT VERSION() AS VERSION");
            
            if (row != null)
            {
                String version = (String)row.get("VERSION");
                
                if (version != null && version.trim().length() > 0)
                {
                    version = parseVersion(version);
                    
                    progress(callback, version);
                    
                    if (version.equals(SUPPORTED_POSTGRESQL_VERSION))
                    {
                        testResult.resultType = TestResult.PASS;
                    }
                    else if (version.startsWith(DEVELOPMENT_ONLY_POSTGRESQL_VERSIONS))
                    {
                        testResult.resultType          = TestResult.WARN;
                        testResult.errorMessage        = "Unsupported PostgreSQL version";
                        testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                        testResult.remedy              = "Install PostgreSQL " + SUPPORTED_POSTGRESQL_VERSION;
                        testResult.urisMoreInformation = ALFRESCO_SPM_AND_POSTGRESQL_URIS;
                    }
                    else
                    {
                        testResult.resultType          = TestResult.FAIL;
                        testResult.errorMessage        = "Unsupported PostgreSQL version";
                        testResult.ramification        = "Alfresco will not function correctly on this version";
                        testResult.remedy              = "Install PostgreSQL " + SUPPORTED_POSTGRESQL_VERSION;
                        testResult.urisMoreInformation = ALFRESCO_SPM_AND_POSTGRESQL_URIS;
                    }
                }
                else
                {
                    progress(callback, "unknown");
                    
                    testResult.resultType   = TestResult.FAIL;
                    testResult.errorMessage = "Unable to determine PostgreSQL version";
                    testResult.ramification = "Alfresco may not function correctly";
                    testResult.remedy       = "Manually validate that PostgreSQL " + SUPPORTED_POSTGRESQL_VERSION + " is installed";
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = "Unable to determine PostgreSQL version";
                testResult.ramification = "Alfresco may not function correctly";
                testResult.remedy       = "Manually validate that PostgreSQL " + SUPPORTED_POSTGRESQL_VERSION + " is installed";
            }
        }
        catch (SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.FAIL;
            testResult.errorMessage = "Unable to determine PostgreSQL version";
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually validate that PostgreSQL " + SUPPORTED_POSTGRESQL_VERSION + " is installed";
            testResult.rootCause    = se;
        }
        
        endTest(callback, testResult);
    }
    
    
    /*
     * PostgreSQL version strings are of the form:
     * 
     *     PostgreSQL 8.4.3 on i386-apple-darwin, compiled by GCC i686-apple-darwin8-gcc-4.0.1 (GCC) 4.0.1 (Apple Computer, Inc. build 5370), 32-bit
     */
    private String parseVersion(final String version)
    {
        String result = version;
        
        if (version.startsWith("PostgreSQL "))
        {
            int          index    = "PostgreSQL ".length();
            char         nextChar = version.charAt(index);
            StringBuffer temp     = new StringBuffer(5);
            
            while (!Character.isWhitespace(nextChar))
            {
                temp.append(nextChar);
                
                index++;
                nextChar = version.charAt(index);
            }
                
            result = temp.toString();
        }
        
        return(result);
    }
    
    
    private final void validateEncoding(final ValidatorCallback callback, final Connection con)
    {
        validateEncoding(callback, con, "Client");
        validateEncoding(callback, con, "Server");
    }
    
    
    private final void validateEncoding(final ValidatorCallback callback, final Connection con, final String whichEncoding)
    {
        startTest(callback, whichEncoding + " Encoding");
        
        TestResult testResult = new TestResult();
        
        String encodingVariable = whichEncoding.toUpperCase() + "_ENCODING";

        try
        {
            Map    row      = singletonQuery(con, "SHOW " + encodingVariable);
            String encoding = (String)row.get(encodingVariable);

            if (encoding != null && encoding.trim().length() > 0)
            {
                progress(callback, encoding);
                
                if (encoding.toLowerCase().equals("utf8"))
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    testResult.resultType          = TestResult.FAIL;
                    testResult.errorMessage        = whichEncoding + " character encoding must be 'utf8' but is not";
                    testResult.ramification        = "Alfresco will not function correctly";
                    testResult.remedy              = "Correct the PostgreSQL character set configuration and rerun this test";
                    testResult.urisMoreInformation = POSTGRESQL_CONFIGURING_CHARACTER_SETS_URI;
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "Unable to determine " + whichEncoding + " character encoding";
                testResult.ramification = "Alfresco may not function correctly";
                testResult.remedy       = "Manually execute the SQL statement 'SHOW " + encodingVariable + "';' " +
                                          "and ensure that the values is 'utf8'";
            }
        }
        catch (SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine " + whichEncoding + " character encoding";
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually execute the SQL statement 'SHOW " + encodingVariable + ";' " +
                                      "and ensure that the values is 'utf8'";
            testResult.rootCause    = se;
        }
        
        endTest(callback, testResult);
    }
        

    
    
        
}
