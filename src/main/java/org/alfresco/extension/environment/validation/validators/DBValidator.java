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
package org.alfresco.extension.environment.validation.validators;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.alfresco.extension.environment.validation.validators.database.DBSpecificValidator;

/**
 * This class validates that the relational database is supported and configured correctly for use by Alfresco.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class DBValidator
    extends AbstractValidator
{
    private final static String VALIDATION_TOPIC = "Database";
    
    // Parameters to this validator
    public final static String PARAMETER_DATABASE_TYPE     = VALIDATION_TOPIC + ".database.type";
    public final static String PARAMETER_DATABASE_HOSTNAME = VALIDATION_TOPIC + ".database.hostname";
    public final static String PARAMETER_DATABASE_PORT     = VALIDATION_TOPIC + ".database.port";
    public final static String PARAMETER_DATABASE_NAME     = VALIDATION_TOPIC + ".database.name";
    public final static String PARAMETER_DATABASE_LOGIN    = VALIDATION_TOPIC + ".database.login";
    public final static String PARAMETER_DATABASE_PASSWORD = VALIDATION_TOPIC + ".database.password";
    
    // Map of database types to JDBC drivers
    private static final Map DATABASE_TYPE_TO_JDBC_DRIVER_MAP = new HashMap()
    {{
        put("mysql",       "org.gjt.mm.mysql.Driver");
        put("postgresql",  "org.postgresql.Driver");
        put("oracle",      "oracle.jdbc.OracleDriver");
        //TODO: Uncomment and modify once mssqlserver and db2 are supported for 4.2
        //put("mssqlserver", "net.sourceforge.jtds.jdbc.Driver");
        //put("db2",         "com.ibm.db2.jcc.DB2Driver");
    }};
    
    private static final Map DATABASE_TYPE_TO_JDBC_URL_MAP = new HashMap()
    {{
        put("mysql",       "jdbc:mysql://<host>:<port>/<database>");
        put("postgresql",  "jdbc:postgresql://<host>:<port>/<database>");
        put("oracle",      "jdbc:oracle:thin:@<host>:<port>:<database>");
        //TODO: Uncomment and modify once mssqlserver and db2 are supported for 4.2
        //put("mssqlserver", "jdbc:jtds:sqlserver://<host>:<port>/<database>");
        //put("db2",         "jdbc:db2://<host>:<port>/<database>");
    }};
    
    private static final Map DATABASE_TYPE_TO_DEFAULT_PORT_MAP = new HashMap()
    {{
        put("mysql",       "3306");
        put("postgresql",  "5432");
        put("oracle",      "1521");
        //TODO: Uncomment and modify once mssqlserver and db2 are supported for 4.2        
        //put("mssqlserver", "1433");
        //put("db2",         "50000");
    }};

    

    /* (non-Javadoc)
     * @see org.alfresco.extension.environment.validation.Validator#validate(java.util.Map, org.alfresco.extension.environment.validation.ValidatorCallback)
     */
    public void validate(final Map parameters, final ValidatorCallback callback)
    {
        newTopic(callback, VALIDATION_TOPIC);
        
        String databaseType = null;
        String jdbcDriver   = null;
        String jdbcUrl      = null;
        String jdbcLogin    = null;
        String jdbcPassword = null;
        
        if (parameters != null)
        {
            databaseType = ((String)parameters.get(PARAMETER_DATABASE_TYPE));
            if (databaseType == null) databaseType = "";   // To avoid NPEs.  Shouldn't ever happen, but never say never...
            
            jdbcDriver   = (String)DATABASE_TYPE_TO_JDBC_DRIVER_MAP.get(databaseType.toLowerCase());
            jdbcUrl      = constructJdbcUrl(databaseType, parameters);
            jdbcLogin    = (String)parameters.get(PARAMETER_DATABASE_LOGIN);
            jdbcPassword = (String)parameters.get(PARAMETER_DATABASE_PASSWORD);
        }
        
        if (validateJdbcParameters(callback, databaseType, jdbcDriver, jdbcUrl, jdbcLogin, jdbcPassword))
        {
            if (validateCanLoadJdbcDriver(callback, jdbcDriver))
            {
                validateDatabaseConnectivityAndConfiguration(callback, jdbcDriver, jdbcUrl, jdbcLogin, jdbcPassword);
            }
        }
    }
    
    
    private String constructJdbcUrl(final String databaseType, final Map parameters)
    {
        String result          = null;
        String jdbcUrlTemplate = (String)DATABASE_TYPE_TO_JDBC_URL_MAP.get(databaseType.toLowerCase());
        
        if (jdbcUrlTemplate != null)
        {
            // hostname
            result = jdbcUrlTemplate.replaceAll("\\<host\\>", (String)parameters.get(PARAMETER_DATABASE_HOSTNAME));
            
            // port
            String port = (String)parameters.get(PARAMETER_DATABASE_PORT);
            
            if (port == null)
            {
                port = (String)DATABASE_TYPE_TO_DEFAULT_PORT_MAP.get(databaseType.toLowerCase());
            }
            
            result = result.replaceAll("\\<port\\>", port);

            // database
            String database = (String)parameters.get(PARAMETER_DATABASE_NAME);
            
            if (database == null)
            {
                database = "alfresco";
            }
            
            result = result.replaceAll("\\<database\\>", database);
        }
        
        return(result);
    }
    
    
    private boolean validateJdbcParameters(final ValidatorCallback callback,
                                           final String            databaseType,
                                           final String            jdbcDriver,
                                           final String            jdbcUrl,
                                           final String            jdbcLogin,
                                           final String            jdbcPassword)
    {
        startTest(callback, "Database Type");
        progress(callback, databaseType);
        
        
        TestResult testResult = new TestResult();
        
        boolean result = DATABASE_TYPE_TO_JDBC_DRIVER_MAP.get(databaseType.toLowerCase()) != null;
        
        if (result)
        {
            progress(callback, "...recognised");
          	testResult.resultType = TestResult.PASS;
        }
        else
        {
            progress(callback, "...unrecognised");
            
            testResult.resultType   = TestResult.FAIL;
            testResult.errorMessage = "Unrecognised database type '" + databaseType + "'";
            testResult.ramification = "The database configuration cannot be validated";
            testResult.remedy       = "Rerun the validaton tool, providing one of the supported database types: mysql, postgresql, oracle"; //, mssqlserver, db2"; //Unsupported for 4.0.0
        }
        
        endTest(callback, testResult);
        
        return(result);
    }
    
    
    private boolean validateCanLoadJdbcDriver(final ValidatorCallback callback, final String jdbcDriver)
    {
        startTest(callback, "JDBC Driver Loaded");
        
        TestResult testResult = new TestResult();
        
        try
        {
            Class.forName(jdbcDriver);
            
            progress(callback, "yes");
            
            testResult.resultType = TestResult.PASS;
        }
        catch (ClassNotFoundException cnfe)
        {
            progress(callback, "no");
            
            testResult.resultType   = TestResult.FAIL;
            testResult.errorMessage = "JDBC driver " + jdbcDriver + " could not be loaded";
            testResult.ramification = "The validation tool is unable to validate the database configuration";
            testResult.remedy       = "This indicates a bug in the validation tool - please a raise a support ticket on the Alfresco Network site";
            testResult.rootCause    = cnfe;
        }
        
        endTest(callback, testResult);
        
        return(testResult.resultType == TestResult.PASS);
    }

    
    private void validateDatabaseConnectivityAndConfiguration(final ValidatorCallback callback, final String jdbcDriver, final String jdbcUrl, final String jdbcLogin, final String jdbcPassword)
    {
        startTest(callback, "Database Connectivity");
        
        TestResult testResult = new TestResult();
        
        Connection con = null;
        
        try
        {
            con = getConnection(jdbcUrl, jdbcLogin, jdbcPassword);
            
            progress(callback, "connected");
            testResult.resultType = TestResult.PASS;
            endTest(callback, testResult);
            
            validateScrollableResultSet(callback, con);
            validateDatabaseSpecificConfiguration(callback, jdbcDriver, con);
        }
        catch (SQLException se)
        {
            progress(callback, "unable to connect");
                     
            testResult.resultType   = TestResult.FAIL;
            testResult.errorMessage = se.getMessage();
            testResult.ramification = "The validation tool is unable to connect to the database using JDBC URL " + jdbcUrl;
            testResult.remedy       = "Please double check the JDBC information and connectivity between this machine and the database server and try running the validation tool again";
            testResult.rootCause    = se;
            
            endTest(callback, testResult);
        }
        finally
        {
            if (con != null)
            {
                try
                {
                    if (con != null && !con.isClosed())
                    {
                        con.close();
                    }
                }
                catch (SQLException se)
                {
                    // Swallow the exception and move on - we don't care if closing the database connection fails
                }
            }
        }
    }
    
    
    private void validateScrollableResultSet(final ValidatorCallback callback, final Connection con)
    {
        startTest(callback, "Scrollable Result Sets");
        
        TestResult testResult = new TestResult();
        
        try
        {
            boolean scrollableAndInsensitiveResultSets = con.getMetaData().supportsResultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE);
            progress(callback, String.valueOf(scrollableAndInsensitiveResultSets));
            
            if (scrollableAndInsensitiveResultSets)
            {
                testResult.resultType = TestResult.PASS;
            }
            else
            {
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = "Result Sets are not scrollable and insensitive to changes in the underlying data";
                testResult.ramification = "Alfresco will not function correctly";
                testResult.remedy       = "Correct the configuration of your database server so that it supports scrollable resultsets that are insensitive to changes in the underlying data";
            }
        }
        catch (final SQLException se)
        {
            progress(callback, "unknown");

            testResult.resultType   = TestResult.FAIL;
            testResult.errorMessage = "Unable to determine result set scrollability and insensitivity: " + se.getMessage();
            testResult.ramification = "The validation tool is unable to validate the database configuration";
            testResult.remedy       = "Manually validate that your database server supports scrollable resultsets that are insensitive to changes in the underlying data";
            testResult.rootCause    = se;
        }

        endTest(callback, testResult);
    }
    
    
    private void validateDatabaseSpecificConfiguration(final ValidatorCallback callback, final String jdbcDriver, final Connection con)
    {
        String validatorClassname = "org.alfresco.extension.environment.validation.validators.database." + jdbcDriver.replace('.', '_');  // Note: this creates valid, albeit ugly, classnames
        
        try
        {
            Class               dbValidatorClass = Class.forName(validatorClassname);
            DBSpecificValidator dbValidator      = (DBSpecificValidator)dbValidatorClass.newInstance();
            
            dbValidator.validate(callback, con);
        }
        catch (final Exception e)
        {
            TestResult testResult = new TestResult();
            startTest(callback, "Database Configuration");
            
            progress(callback, "couldn't load configuration validator for " + jdbcDriver);

            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Database validation class " + validatorClassname + " could not be loaded: " + e.getMessage();
            testResult.ramification        = "The validation tool is unable to validate the database configuration";
            testResult.remedy              = "This is a bug in the validation tool - please raise a bug report with Alfresco support and include the complete output from this tool";
            testResult.urisMoreInformation = ALFRESCO_NETWORK_URI;
            testResult.rootCause           = e;
            
            endTest(callback, testResult);
        }
    }

    
    private Connection getConnection(final String jdbcUrl, final String login, final String password)
        throws SQLException
    {
        Connection result = null;
        
        if (login != null && login.trim().length() > 0)
        {
            result = DriverManager.getConnection(jdbcUrl, login, password);
        }
        else
        {
            result = DriverManager.getConnection(jdbcUrl);
        }
        
        return(result);
    }


}
