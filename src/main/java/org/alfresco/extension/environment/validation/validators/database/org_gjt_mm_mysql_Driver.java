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

import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.alfresco.extension.util.ComparablePair;


/**
 * This class defines the database validation rules specific to the MySQL relational database and JDBC driver. 
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class org_gjt_mm_mysql_Driver
    extends AbstractDBSpecificValidator
{
    // Supported MySQL version
    private final static ComparablePair[] SUPPORTED_JDBC_DRIVER_VERSION      = { new ComparablePair(new Integer(5), new Integer(1)) };
    private final static String           SUPPORTED_MYSQL_VERSION            = "5.1";
    private final static String           SUPPORTED_MYSQL_VERSION_SIGNATURE  = SUPPORTED_MYSQL_VERSION + ".";
    private final static int              MINIMUM_SUPPORTED_MYSQL_PATCHLEVEL = 51;
    private final static String           FULL_VERSION_STRING                = SUPPORTED_MYSQL_VERSION_SIGNATURE + MINIMUM_SUPPORTED_MYSQL_PATCHLEVEL;
    
    // "More information" URIs
    private final static String[] MYSQL_URI                                         = { "http://dev.mysql.com/downloads/mysql/" };
    private final static String[] JDBC_URI                                          = { "http://dev.mysql.com/downloads/connector/j/" };
    private final static String[] MYSQL_CONFIGURING_STORAGE_ENGINE_URI              = { "http://dev.mysql.com/doc/refman/" + SUPPORTED_MYSQL_VERSION + "/en/storage-engine-setting.html" };
    private final static String[] MYSQL_CONFIGURING_CHARACTER_SETS_URI              = { "http://dev.mysql.com/doc/refman/" + SUPPORTED_MYSQL_VERSION + "/en/charset-applications.html" };
    private final static String[] MYSQL_CONFIGURING_IDENTIFIER_CASE_SENSITIVITY_URI = { "http://dev.mysql.com/doc/refman/" + SUPPORTED_MYSQL_VERSION + "/en/identifier-case-sensitivity.html" };
    private final static String[] MYSQL_AUTO_INCREMENT_LOCK_MODES_URI               = { "http://dev.mysql.com/doc/refman/" + SUPPORTED_MYSQL_VERSION + "/en/innodb-auto-increment-handling.html" };
    private final static String[] MYSQL_WAIT_TIMEOUT_URI                            = { "http://dev.mysql.com/doc/refman/" + SUPPORTED_MYSQL_VERSION + "/en/server-system-variables.html#sysvar_wait_timeout" };
    private final static String[] MYSQL_LOCKS_UNSAFE_URI                            = { "http://dev.mysql.com/doc/refman/" + SUPPORTED_MYSQL_VERSION + "/en/innodb-parameters.html#sysvar_innodb_locks_unsafe_for_binlog" };

    /**
     * @see org.alfresco.extension.environment.validation.validators.database.DBSpecificValidator#validate(org.alfresco.extension.environment.validation.ValidatorCallback, java.sql.Connection)
     */
    public void validate(final ValidatorCallback callback, final Connection con)
    {
        setupConnection(con);
        
        validateJdbcDriverVersion(callback, con, SUPPORTED_JDBC_DRIVER_VERSION, JDBC_URI);
        validateDatabaseVersion(callback, con);
        validateEngine(callback, con);
        validateIdentifierCaseSensitivityLevel(callback, con);
        validateInnoDbAutoIncrementLockMode(callback, con);
        validateWaitTimeout(callback, con);
        validateEncoding(callback, con);
        validateInnoDbLocksUnsafeForBinlogMode(callback, con);
    }
    
    
    private void setupConnection(final Connection con)
    {
        try
        {
            sql(con, "SET NAMES 'utf8'");  // Force our connection to be UTF8 in all cases
        }
        catch (final SQLException se)
        {
            // Ignore - we don't really care if these statements fail
        }
    }
    
    private void validateDatabaseVersion(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "MySQL Version");
        
        TestResult testResult = new TestResult();
        
        try
        {
            Map row = singletonQuery(con, "SELECT VERSION() AS VERSION");
            
            if (row != null)
            {
                String version = (String)row.get("VERSION");
                
                if (version != null && version.trim().length() > 0)
                {
                    progress(callback, version);
                    
                    if (version.startsWith(SUPPORTED_MYSQL_VERSION_SIGNATURE))
                    {
                        String[] versionComponents = version.split("\\.");
                        
                        if (versionComponents.length >= 3 && versionComponents[2].trim().length() > 0)
                        {
                            try
                            {
                                int patchLevel = Integer.parseInt(versionComponents[2].trim());
                                
                                if (patchLevel >= MINIMUM_SUPPORTED_MYSQL_PATCHLEVEL)
                                {
                                    testResult.resultType = TestResult.PASS;
                                }
                                else
                                {
                                    testResult.resultType          = TestResult.WARN;
                                    testResult.errorMessage        = "Unsupported MySQL " + SUPPORTED_MYSQL_VERSION + " patchlevel (" + patchLevel + ")";
                                    testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                                    testResult.remedy              = "Install MySQL " + SUPPORTED_MYSQL_VERSION + " with at least patchlevel " + MINIMUM_SUPPORTED_MYSQL_PATCHLEVEL;
                                    testResult.urisMoreInformation = MYSQL_URI;
                                }
                            }
                            catch (final NumberFormatException nfe)
                            {
                                testResult.resultType          = TestResult.WARN;
                                testResult.errorMessage        = "Unable to determine MySQL " + SUPPORTED_MYSQL_VERSION + " patchlevel";
                                testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                                testResult.remedy              = "Manually validate that MySQL " + SUPPORTED_MYSQL_VERSION + " with at least patchlevel " + MINIMUM_SUPPORTED_MYSQL_PATCHLEVEL + " is installed";
                                testResult.urisMoreInformation = MYSQL_URI;
                                testResult.rootCause           = nfe;
                            }
                        }
                        else
                        {
                            testResult.resultType          = TestResult.WARN;
                            testResult.errorMessage        = "Unable to determine MySQL " + SUPPORTED_MYSQL_VERSION + " patchlevel";
                            testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                            testResult.remedy              = "Manually validate that MySQL " + SUPPORTED_MYSQL_VERSION + " with at least patchlevel " + MINIMUM_SUPPORTED_MYSQL_PATCHLEVEL + " is installed";
                            testResult.urisMoreInformation = MYSQL_URI;
                        }
                    }
                    else
                    {
                        testResult.resultType          = TestResult.FAIL;
                        testResult.errorMessage        = "Unsupported MySQL version";
                        testResult.ramification        = "Alfresco will not function correctly on this version";
                        testResult.remedy              = "Install MySQL " + SUPPORTED_MYSQL_VERSION + " with at least patchlevel " + MINIMUM_SUPPORTED_MYSQL_PATCHLEVEL;
                        testResult.urisMoreInformation = MYSQL_URI;
                    }
                }
                else
                {
                    progress(callback, "unknown");
                    
                    testResult.resultType   = TestResult.FAIL;
                    testResult.errorMessage = "Unable to determine MySQL version";
                    testResult.ramification = "Alfresco may not function correctly";
                    testResult.remedy       = "Manually validate that MySQL " + SUPPORTED_MYSQL_VERSION + " with at least patchlevel " + MINIMUM_SUPPORTED_MYSQL_PATCHLEVEL + " is installed";
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = "Unable to determine MySQL version";
                testResult.ramification = "Alfresco may not function correctly";
                testResult.remedy       = "Manually validate that MySQL " + SUPPORTED_MYSQL_VERSION + " with at least patchlevel " + MINIMUM_SUPPORTED_MYSQL_PATCHLEVEL + " is installed";
            }
        }
        catch (final SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.FAIL;
            testResult.errorMessage = "Unable to determine MySQL version";
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually validate that MySQL " + SUPPORTED_MYSQL_VERSION + " with at least patchlevel " + MINIMUM_SUPPORTED_MYSQL_PATCHLEVEL + " is installed";
            testResult.rootCause    = se;
        }
        
        endTest(callback, testResult);
    }
    
    
    private void validateEngine(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "Default Storage Engine");
        
        TestResult testResult = new TestResult();
        
        try
        {
            List    engines      = query(con, "SHOW ENGINES");
            boolean foundDefault = false;
            
            for (int i = 0; i < engines.size(); i++)
            {
                Map    row     = (Map)engines.get(i);
                String support = (String)row.get("SUPPORT");
                
                if ("DEFAULT".equals(support))
                {
                    String engine = (String)row.get("ENGINE");
                    progress(callback, engine);
                    foundDefault = true;
                
                    if ("InnoDB".equals(engine))
                    {
                        testResult.resultType = TestResult.PASS;
                    }
                    else
                    {
                        testResult.resultType          = TestResult.WARN;
                        testResult.errorMessage        = "InnoDB should be the default storage engine, but is not";
                        testResult.ramification        = "None. Since v3.3, Alfresco will force the use of InnoDB for the Alfresco tables regardless of the default engine";
                        testResult.remedy              = "Reconfigure MySQL to use the InnoDB storage engine as the default";
                        testResult.urisMoreInformation = MYSQL_CONFIGURING_STORAGE_ENGINE_URI;
                    }
                    
                    break;
                }
            }
            
            if (!foundDefault)
            {
                progress(callback, "unknown");
                
                testResult.resultType          = TestResult.WARN;
                testResult.errorMessage        = "Unable to determine the default storage engine";
                testResult.ramification        = "None. Since v3.3, Alfresco will force the use of InnoDB for the Alfresco tables regardless of the default engine";
                testResult.remedy              = "Reconfigure MySQL to use the InnoDB storage engine as the default";
                testResult.urisMoreInformation = MYSQL_CONFIGURING_STORAGE_ENGINE_URI;
            }
        }
        catch (final SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Unable to determine the default storage engine";
            testResult.ramification        = "None. Since v3.3, Alfresco will force the use of InnoDB for the Alfresco tables regardless of the default engine";
            testResult.remedy              = "Reconfigure MySQL to use the InnoDB storage engine as the default";
            testResult.urisMoreInformation = MYSQL_CONFIGURING_STORAGE_ENGINE_URI;
            testResult.rootCause           = se;
        }
        
        endTest(callback, testResult);
    }
    
    
    private final void validateIdentifierCaseSensitivityLevel(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "Case Sensitivity Level");
        
        TestResult testResult = new TestResult();
        
        try
        {
            Map row = singletonQuery(con, "SHOW VARIABLES WHERE VARIABLE_NAME = 'lower_case_table_names'");
            
            if (row != null)
            {
                String identifierCaseSensitivityLevel = (String)row.get("VARIABLE_VALUE");
                
                if (identifierCaseSensitivityLevel != null && identifierCaseSensitivityLevel.trim().length() > 0)
                {
                    progress(callback, identifierCaseSensitivityLevel);
                    
                    if ("1".equals(identifierCaseSensitivityLevel))
                    {
                        testResult.resultType = TestResult.PASS;
                    }
                    else
                    {
                        testResult.resultType          = TestResult.WARN;
                        testResult.errorMessage        = "OS-specific identifier case sensitivity level configured";
                        testResult.ramification        = "Backups of the Alfresco database will be OS specific";
                        testResult.remedy              = "Reconfigure MySQL to use case-insensitive identifiers; specifically, set lower_case_table_names=1 in the MySQL configuration";
                        testResult.urisMoreInformation = MYSQL_CONFIGURING_IDENTIFIER_CASE_SENSITIVITY_URI;
                    }
                }
                else
                {
                    progress(callback, "unknown");
                    
                    testResult.resultType          = TestResult.WARN;
                    testResult.errorMessage        = "Unable to determine identifier case sensitivity level";
                    testResult.ramification        = "Backups of the Alfresco database may be OS specific";
                    testResult.remedy              = "Manually validate that MySQL is configured to use case-insensitive identifiers; specifically, ensure that lower_case_table_names=1 in the MySQL configuration";
                    testResult.urisMoreInformation = MYSQL_CONFIGURING_IDENTIFIER_CASE_SENSITIVITY_URI;
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType          = TestResult.WARN;
                testResult.errorMessage        = "Unable to determine identifier case sensitivity level";
                testResult.ramification        = "Backups of the Alfresco database may be OS specific";
                testResult.remedy              = "Manually validate that MySQL is configured to use case-insensitive identifiers; specifically, ensure that lower_case_table_names=1 in the MySQL configuration";
                testResult.urisMoreInformation = MYSQL_CONFIGURING_IDENTIFIER_CASE_SENSITIVITY_URI;
            }
        }
        catch (final SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Unable to determine identifier case sensitivity level: " + se.getMessage();
            testResult.ramification        = "Backups of the Alfresco database may be OS specific";
            testResult.remedy              = "Manually validate that MySQL is configured to use case-insensitive identifiers; specifically, ensure that lower_case_table_names=1 in the MySQL configuration";
            testResult.urisMoreInformation = MYSQL_CONFIGURING_IDENTIFIER_CASE_SENSITIVITY_URI;
            testResult.rootCause           = se;
        }
        
        endTest(callback, testResult);
    }

    
    private final void validateInnoDbAutoIncrementLockMode(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "Auto-inc Lock Mode");
        
        TestResult testResult = new TestResult();
        
        try
        {
            Map row = singletonQuery(con, "SHOW VARIABLES WHERE VARIABLE_NAME = 'innodb_autoinc_lock_mode'");
            
            if (row != null)
            {
                String identifierCaseSensitivityLevel = (String)row.get("VARIABLE_VALUE");
                
                if (identifierCaseSensitivityLevel != null && identifierCaseSensitivityLevel.trim().length() > 0)
                {
                    progress(callback, identifierCaseSensitivityLevel);
                    
                    if ("2".equals(identifierCaseSensitivityLevel))
                    {
                        testResult.resultType = TestResult.PASS;
                    }
                    else
                    {
                        testResult.resultType          = TestResult.WARN;
                        testResult.errorMessage        = "Non-optimal InnoDB auto-increment lock mode configured";
                        testResult.ramification        = "Alfresco may perform poorly under heavy write load due to excessive blocking in MySQL";
                        testResult.remedy              = "Reconfigure MySQL with InnoDB auto-increment lock mode 2; specifically, set innodb_autoinc_lock_mode=2 in the MySQL configuration";
                        testResult.urisMoreInformation = MYSQL_AUTO_INCREMENT_LOCK_MODES_URI;
                    }
                }
                else
                {
                    progress(callback, "unknown");
                    
                    testResult.resultType          = TestResult.WARN;
                    testResult.errorMessage        = "Unable to determine InnoDB auto-increment lock mode";
                    testResult.ramification        = "Alfresco may perform poorly under heavy write load due to excessive blocking in MySQL";
                    testResult.remedy              = "Manually validate that MySQL is configured with InnoDB auto-increment lock mode 2; specifically, set innodb_autoinc_lock_mode=2 in the MySQL configuration";
                    testResult.urisMoreInformation = MYSQL_AUTO_INCREMENT_LOCK_MODES_URI;
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType          = TestResult.WARN;
                testResult.errorMessage        = "Unable to determine InnoDB auto-increment lock mode";
                testResult.ramification        = "Alfresco may perform poorly under heavy write load due to excessive blocking in MySQL";
                testResult.remedy              = "Manually validate that MySQL is configured with InnoDB auto-increment lock mode 2; specifically, set innodb_autoinc_lock_mode=2 in the MySQL configuration";
                testResult.urisMoreInformation = MYSQL_AUTO_INCREMENT_LOCK_MODES_URI;
            }
        }
        catch (final SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Unable to determine identifier case sensitivity level: " + se.getMessage();
            testResult.ramification        = "Backups of the Alfresco database may be OS specific";
            testResult.remedy              = "Manually validate that MySQL is configured to use case-insensitive identifiers; specifically, ensure that lower_case_table_names=1 in the MySQL configuration";
            testResult.urisMoreInformation = MYSQL_CONFIGURING_IDENTIFIER_CASE_SENSITIVITY_URI;
            testResult.rootCause           = se;
        }
        
        endTest(callback, testResult);
    }

    
    private final void validateWaitTimeout(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "Wait Timeout");
        
        TestResult testResult = new TestResult();
        
        try
        {
            Map row = singletonQuery(con, "SHOW VARIABLES WHERE VARIABLE_NAME = 'wait_timeout'");
            
            if (row != null)
            {
                String waitTimeoutStr = (String)row.get("VARIABLE_VALUE");
                
                if (waitTimeoutStr != null && waitTimeoutStr.trim().length() > 0)
                {
                    progress(callback, waitTimeoutStr);
                    
                    try
                    {
                        long waitTimeout = Long.valueOf(waitTimeoutStr).longValue();
                        
                        if (waitTimeout >= 28800)   // 28800ms (8 hours) is the default for wait_timeout, and is an appropriate value for Alfresco
                        {
                            testResult.resultType = TestResult.PASS;
                        }
                        else
                        {
                            testResult.resultType          = TestResult.WARN;
                            testResult.errorMessage        = "Non-optimal wait timeout configured";
                            testResult.ramification        = "Alfresco may lose connections to MySQL without further configuration of the database connection pool";
                            testResult.remedy              = "Reconfigure MySQL back to the default wait timeout; specifically, set wait_timeout=28800 in the MySQL configuration, or remove this setting altogether";
                            testResult.urisMoreInformation = MYSQL_WAIT_TIMEOUT_URI;
                        }
                    }
                    catch (final NumberFormatException nfe)
                    {
                        testResult.resultType          = TestResult.WARN;
                        testResult.errorMessage        = "Unable to determine wait timeout";
                        testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                        testResult.remedy              = "Manually validate that the wait timeout is at least 28800";
                        testResult.urisMoreInformation = MYSQL_WAIT_TIMEOUT_URI;
                        testResult.rootCause           = nfe;
                    }
                }
                else
                {
                    progress(callback, "unknown");
                    
                    testResult.resultType          = TestResult.WARN;
                    testResult.errorMessage        = "Unable to determine wait timeout";
                    testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                    testResult.remedy              = "Manually validate that the wait timeout is at least 28800";
                    testResult.urisMoreInformation = MYSQL_WAIT_TIMEOUT_URI;
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType          = TestResult.WARN;
                testResult.errorMessage        = "Unable to determine wait timeout";
                testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                testResult.remedy              = "Manually validate that the wait timeout is at least 28800";
                testResult.urisMoreInformation = MYSQL_WAIT_TIMEOUT_URI;
            }
        }
        catch (final SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Unable to determine wait timeout";
            testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
            testResult.remedy              = "Manually validate that the wait timeout is at least 28800";
            testResult.urisMoreInformation = MYSQL_WAIT_TIMEOUT_URI;
            testResult.rootCause           = se;
        }
        
        endTest(callback, testResult);
    }

    
    private final void validateEncoding(final ValidatorCallback callback, final Connection con)
    {
        try
        {
            List encodings = query(con, "SHOW VARIABLES LIKE 'character\\_set\\_%'");
            
            for (int i = 0; i < encodings.size(); i++)
            {
                Map    row              = (Map)encodings.get(i);
                String characterSetting = (String)row.get("VARIABLE_NAME");
                String encoding         = (String)row.get("VARIABLE_VALUE");  // MySQL 5.1
                
                if (encoding == null || encoding.trim().length() == 0)
                {
                    encoding = (String)row.get("VALUE");  // MySQL 5.0
                }

                validateEncodingSetting(callback, characterSetting, encoding);
            }
        }
        catch (final SQLException se)
        {
            startTest(callback, "Character Encoding");
            
            TestResult testResult = new TestResult();
            
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine character encoding";
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually execute the SQL statement 'SHOW VARIABLES LIKE 'character\\_set\\_%';' " +
                                      "and ensure that all values are 'utf8', with the exception of 'character_set_filesystem' " +
                                      "which must have 'binary' encoding";
            testResult.rootCause    = se;
            
            endTest(callback, testResult);
        }
    }

    private final void validateEncodingSetting(final ValidatorCallback callback, final String characterSetting, final String encoding)
    {
        String settingName = getSettingName(characterSetting);
        startTest(callback, settingName + " Encoding");
        
        TestResult testResult = new TestResult();
        
        if (encoding != null && encoding.trim().length() > 0)
        {
            progress(callback, encoding);
            
            if ("character_set_filesystem".equals(characterSetting))
            {
                if ("binary".equals(encoding))
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    testResult.resultType          = TestResult.FAIL;
                    testResult.errorMessage        = settingName + " character encoding must be 'binary' but is not";
                    testResult.ramification        = "Alfresco will not function correctly";
                    testResult.remedy              = "Correct the MySQL character set configuration and rerun this test";
                    testResult.urisMoreInformation = MYSQL_CONFIGURING_CHARACTER_SETS_URI;
                }
            }
            else
            {
                if ("utf8".equals(encoding))
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    testResult.resultType          = TestResult.FAIL;
                    testResult.errorMessage        = settingName + " character encoding must be 'utf8' but is not";
                    testResult.ramification        = "Alfresco will not function correctly";
                    testResult.remedy              = "Correct the MySQL character set configuration and rerun this test";
                    testResult.urisMoreInformation = MYSQL_CONFIGURING_CHARACTER_SETS_URI;
                }
            }
        }
        else
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine character encoding for " + settingName;
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually execute the SQL statement 'SHOW VARIABLES LIKE 'character\\_set\\_%';' " +
                                      "and ensure that all values are 'utf8', with the exception of 'character_set_filesystem' " +
                                      "which must have 'binary' encoding";
        }
        
        endTest(callback, testResult);
    }
    private final String getSettingName(final String characterSetting)
    {
        String result = characterSetting;
        
        if (characterSetting.startsWith("character_set_"))
        {
            String temp = characterSetting.substring("character_set_".length());
            result = String.valueOf(temp.charAt(0)).toUpperCase() + temp.substring(1);
        }
        
        return(result);
    }
    private final void     validateInnoDbLocksUnsafeForBinlogMode(final ValidatorCallback callback, final Connection con)
    {
        try
        {  
            List encodings = query(con, "SHOW VARIABLES LIKE 'innodb_locks_unsafe_for_binlog'");
            
            for (int i = 0; i < encodings.size(); i++)
            {
                Map    row              = (Map)encodings.get(i);
                String setting         = (String)row.get("VARIABLE_VALUE");  // MySQL 5.1
                
                if (setting == null || setting.trim().length() == 0)
                {
                    setting = (String)row.get("VALUE");  // MySQL 5.0
                }

                validateBinlogSetting(callback, setting);
            }
        }
        catch (final SQLException se)
        {
            startTest(callback, "Unsafe for Binlog");
            
            TestResult testResult = new TestResult();
            
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine innodb_locks_unsafe_for_binlog setting";
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually execute the SQL statement 'SHOW VARIABLES LIKE 'innodb_locks_unsafe_for_binlog';' " +
                                      "and ensure that the values is 'ON'";
            testResult.urisMoreInformation = MYSQL_LOCKS_UNSAFE_URI;
            testResult.rootCause    = se;
            
            endTest(callback, testResult);
        }
    }
    private final void validateBinlogSetting(final ValidatorCallback callback, final String setting)
    {
        startTest(callback, "Unsafe for binlog");
        
        TestResult testResult = new TestResult();
        
        if (setting != null && setting.trim().length() > 0)
        {
            progress(callback, setting);
            
            if ("ON".equalsIgnoreCase(setting))
            {
            	testResult.resultType = TestResult.PASS;
            }
            else
            {
                testResult.resultType          = TestResult.FAIL;
                testResult.errorMessage        = "innodb_locks_unsafe_for_binlog should be set to ON";
                testResult.ramification        = "Alfresco will not function correctly";
                testResult.remedy              = "Correct the value of innodb_locks_unsafe_for_binlog and rerun this test";
                testResult.urisMoreInformation = MYSQL_LOCKS_UNSAFE_URI;
            }
        }
        endTest(callback, testResult);
    }

   
    

}