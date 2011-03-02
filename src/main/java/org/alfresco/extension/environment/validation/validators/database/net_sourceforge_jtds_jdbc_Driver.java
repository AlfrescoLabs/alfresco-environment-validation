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

import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.alfresco.extension.util.ComparablePair;


/**
 * This class defines the database validation rules specific to the MS SQL Server relational database, specific to the jTDS JDBC driver.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class net_sourceforge_jtds_jdbc_Driver
    extends AbstractDBSpecificValidator
{
    // SQL Server product versions
    private final static ComparablePair[] SUPPORTED_JDBC_DRIVER_VERSION = { new ComparablePair(new Integer(1), new Integer(2)) };
    private final static String           MS_SQL_SERVER_2008_R2         = "10.50.";

    
    // "More information" URIs
    private final static String   MS_SQL_SERVER_URI_STR                        = "http://technet.microsoft.com/en-us/sqlserver/";
    private final static String[] MS_SQL_SERVER_URI                            = { MS_SQL_SERVER_URI_STR };
    private final static String[] JDBC_URI                                     = { "http://sourceforge.net/projects/jtds/files/" };
    private final static String[] MS_SQL_SERVER_CONFIGURING_CHARACTER_SETS_URI = { "http://msdn.microsoft.com/en-us/library/ms144260(v=SQL.100).aspx" };
    private final static String[] SNAPSHOT_ISOLATION_URIS                      = { "http://msdn.microsoft.com/en-us/library/ms175095.aspx", "http://wiki.alfresco.com/wiki/Database_Configuration#Microsoft_SQL_Server_example" };
    private final static String[] ALFRESCO_SPM_AND_MS_SQL_SERVER_URIS          = { AbstractValidator.ALFRESCO_SUMMARY_SPM_URI_STR, MS_SQL_SERVER_URI_STR }; 

    
    /* (non-Javadoc)
     * @see org.alfresco.extension.environment.validation.validators.database.DBSpecificValidator#validate(org.alfresco.extension.environment.validation.ValidatorCallback, java.sql.Connection)
     */
    public void validate(ValidatorCallback callback, Connection con)
    {
        validateJdbcDriverVersion(callback, con, SUPPORTED_JDBC_DRIVER_VERSION, JDBC_URI);
        validateDatabaseVersion(callback, con);
        validateDatabaseEdition(callback, con);
        validateSnapshotIsolation(callback, con);
    }
    
    
    protected void validateDatabaseVersion(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "MS SQL Server Version");
        
        TestResult testResult = new TestResult();
        
        try
        {
            Map row = singletonQuery(con, "SELECT SERVERPROPERTY('ProductVersion') AS VERSION");
            
            if (row != null)
            {
                String version = (String)row.get("VERSION");
                
                if (version != null && version.trim().length() > 0)
                {
                    if (version.startsWith(MS_SQL_SERVER_2008_R2))
                    {
                        progress(callback, "2008R2");
                        testResult.resultType = TestResult.PASS;
                    }
                    else
                    {
                        progress(callback, version);
                        
                        testResult.resultType          = TestResult.FAIL;
                        testResult.errorMessage        = "Unsupported MS SQL Server version";
                        testResult.ramification        = "Alfresco will not function correctly on this version";
                        testResult.remedy              = "Install MS SQL Server 2008 R2";
                        testResult.urisMoreInformation = ALFRESCO_SPM_AND_MS_SQL_SERVER_URIS;
                    }
                }
                else
                {
                    progress(callback, "unknown");
                    
                    testResult.resultType   = TestResult.FAIL;
                    testResult.errorMessage = "Unable to determine MS SQL Server version";
                    testResult.ramification = "Alfresco may not function correctly";
                    testResult.remedy       = "Install MS SQL Server 2008 R2";
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = "Unable to determine MS SQL Server version";
                testResult.ramification = "Alfresco may not function correctly";
                testResult.remedy       = "Install MS SQL Server 2008 R2";
            }
        }
        catch (final SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.FAIL;
            testResult.errorMessage = "Unable to determine MS SQL Server version";
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually validate that MS SQL Server 2008 R2 is installed";
            testResult.rootCause    = se;
        }
        
        endTest(callback, testResult);
    }
    
    
    protected void validateDatabaseEdition(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "MS SQL Server Edition");
        
        TestResult testResult = new TestResult();
        
        try
        {
            Map row = singletonQuery(con, "SELECT SERVERPROPERTY('Edition') AS EDITION");
            
            if (row != null)
            {
                String edition = (String)row.get("EDITION");
                
                if (edition != null && edition.trim().length() > 0)
                {
                    progress(callback, edition);
                    
                    if (edition.startsWith("Enterprise")  ||
                        edition.startsWith("Standard")    ||
                        edition.startsWith("Data Center") ||
                        edition.startsWith("Small Business Server"))
                    {
                        testResult.resultType = TestResult.PASS;
                    }
                    else
                    {
                        testResult.resultType          = TestResult.WARN;
                        testResult.errorMessage        = "Unsupported MS SQL Server edition";
                        testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                        testResult.remedy              = "Install MS SQL Server 2008 R2, Standard, Small Business, Enterprise or Data Center Edition";
                        testResult.urisMoreInformation = ALFRESCO_SPM_AND_MS_SQL_SERVER_URIS;
                    }
                }
                else
                {
                    progress(callback, "unknown");
                    
                    testResult.resultType   = TestResult.FAIL;
                    testResult.errorMessage = "Unable to determine MS SQL Server edition";
                    testResult.ramification = "Alfresco may not function correctly";
                    testResult.remedy       = "Manually validate that the MS SQL Server is Standard, Small Business, Enterprise or Data Center Edition";
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = "Unable to determine MS SQL Server edition";
                testResult.ramification = "Alfresco may not function correctly";
                testResult.remedy       = "Manually validate that the MS SQL Server is Standard, Small Business, Enterprise or Data Center Edition";
            }
        }
        catch (final SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.FAIL;
            testResult.errorMessage = "Unable to determine MS SQL Server edition";
            testResult.ramification = "Alfresco may not function correctly";
            testResult.remedy       = "Manually validate that the MS SQL Server is Standard, Small Business, Enterprise or Data Center Edition";
            testResult.rootCause    = se;
        }
        
        endTest(callback, testResult);
    }
    
    
    private void validateSnapshotIsolation(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "Snapshot Isolation");
        
        TestResult testResult = new TestResult();
        
        try
        {
            String databaseName = con.getCatalog();
            
            if (databaseName != null && databaseName.trim().length() > 0)
            {
                Map row = singletonQuery(con, "SELECT snapshot_isolation_state_desc AS SNAPSHOT_ISOLATION FROM sys.databases WHERE name = '" + databaseName + "'");
                
                if (row != null)
                {
                    String snapshotIsolation = (String)row.get("SNAPSHOT_ISOLATION");
                    
                    if (snapshotIsolation != null && snapshotIsolation.trim().length() > 0)
                    {
                        progress(callback, snapshotIsolation.toLowerCase());
                        
                        if (snapshotIsolation.equalsIgnoreCase("on"))
                        {
                            testResult.resultType = TestResult.PASS;
                        }
                        else
                        {
                            testResult.resultType          = TestResult.FAIL;
                            testResult.errorMessage        = "Unsupported MS SQL Server configuration - snapshot isolation must be enabled for the " + databaseName + " database";
                            testResult.ramification        = "Alfresco will not function correctly on this database";
                            testResult.remedy              = "Execute 'ALTER DATABASE " + databaseName + " SET ALLOW_SNAPSHOT_ISOLATION ON;' to enable snapshot isolation";
                            testResult.urisMoreInformation = SNAPSHOT_ISOLATION_URIS;
                        }
                    }
                    else
                    {
                        progress(callback, "unknown");
                        
                        testResult.resultType   = TestResult.FAIL;
                        testResult.errorMessage = "Unable to determine snapshot isolation for database " + databaseName;
                        testResult.ramification = "Alfresco may not function correctly";
                        testResult.remedy       = "Manually validate that snapshot isolation is enabled by running the following query and ensuring the resulting value is 'ON':  SELECT snapshot_isolation_state_desc AS SNAPSHOT_ISOLATION FROM sys.databases WHERE name = '" + databaseName + "'";
                        testResult.urisMoreInformation = SNAPSHOT_ISOLATION_URIS;
                    }
                }
                else
                {
                    progress(callback, "unknown");
                    
                    testResult.resultType   = TestResult.FAIL;
                    testResult.errorMessage = "Unable to determine snapshot isolation for database " + databaseName;
                    testResult.ramification = "Alfresco may not function correctly";
                    testResult.remedy       = "Manually validate that snapshot isolation is enabled by running the following query and ensuring the resulting value is 'ON':  SELECT snapshot_isolation_state_desc AS SNAPSHOT_ISOLATION FROM sys.databases WHERE name = '" + databaseName + "'";
                    testResult.urisMoreInformation = SNAPSHOT_ISOLATION_URIS;
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = "Unable to determine snapshot isolation (could not determine database name)";
                testResult.ramification = "Alfresco may not function correctly";
                testResult.remedy       = "Manually validate that snapshot isolation is enabled by running the following query and ensuring the resulting value is 'ON':  SELECT snapshot_isolation_state_desc AS SNAPSHOT_ISOLATION FROM sys.databases WHERE name = '" + databaseName + "'";
                testResult.urisMoreInformation = SNAPSHOT_ISOLATION_URIS;
            }
        }
        catch (final SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType          = TestResult.FAIL;
            testResult.errorMessage        = "Unable to determine snapshot isolation";
            testResult.ramification        = "Alfresco may not function correctly";
            testResult.remedy              = "Manually validate that snapshot isolation is enabled in the Alfresco database";
            testResult.urisMoreInformation = SNAPSHOT_ISOLATION_URIS;
            testResult.rootCause           = se;
        }
        
        endTest(callback, testResult);
    }


}
