/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.alfresco.extension.environment.validation.ValidatorCallbackHelper;
import org.alfresco.extension.util.ComparablePair;
import org.alfresco.extension.util.Pair;


/**
 * This class is a useful base class for database-specific validators.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
abstract public class AbstractDBSpecificValidator
    extends ValidatorCallbackHelper
    implements DBSpecificValidator
{
    /**
     * Simple method for executing any arbitrary statement and ignoring the result (including any success / failure conditions).
     * 
     * @param con       The database connection to use <i>(must not be null)</i>.
     * @param sqlSelect The SQL statement to execute <i>(must not be null, empty or blank)</i>.
     * @throws SQLException
     */
    protected void sql(final Connection con, final String sql)
        throws SQLException
    {
        con.prepareStatement(sql).execute();
    }
    
    
    protected Map singletonQuery(final Connection con, final String sqlSelect)
        throws SQLException
    {
        Map  result    = null;
        List resultSet = query(con, sqlSelect);
        
        if (resultSet.size() > 0)
        {
            result = (Map)resultSet.get(0);
        }
        
        return(result);
    }
    
    
    /**
     * Executes a SQL SELECT statement and returns the result as a List of Maps.
     * 
     * @param con       The database connection to use <i>(must not be null)</i>.
     * @param sqlSelect The SQL SELECT statement to execute <i>(must not be null, empty or blank)</i>.
     * @return
     * @throws SQLException
     */
    protected List query(final Connection con, final String sqlSelect)
        throws SQLException
    {
        List      result = new ArrayList();
        ResultSet rs     = null;
        
        try
        {
            rs = con.prepareStatement(sqlSelect).executeQuery();

            List columnNames = getColumnNames(rs.getMetaData());

            // Copy the entire result set to the list
            while (rs.next())
            {
                Map row = new HashMap();

                // Copy the current row to the map
                for (int i = 0; i < columnNames.size(); i++)
                {
                    String columnName  = (String)columnNames.get(i);
                    Object columnValue = rs.getObject(i + 1);  // Note: columns in JDBC are 1-based

                    row.put(columnName, columnValue);
                }
                
                result.add(row);
            }
        }
        finally
        {
            if (rs != null)
            {
                try
                {
                    rs.close();
                }
                catch (SQLException se)
                {
                    // Ignore this exception - we don't really care if closing the result set fails
                }
            }
        }
        
        return(result);
    }
    
    
    protected List getColumnNames(final ResultSetMetaData metadata)
        throws SQLException
    {
        List result = new ArrayList();
    
        // Note: columns in JDBC are 1-based
        for (int i = 1; i <= metadata.getColumnCount(); i++)
        {
            String columnName      = metadata.getColumnName(i);
            String finalColumnName = null;

            // We have an unnamed column so synthesise a name
            if (columnName == null || columnName.trim().length() == 0)
            {
                finalColumnName = "COLUMN_" + i;
            }
            else
            {
                finalColumnName = columnName.trim().toUpperCase(Locale.ENGLISH);
            }
    
            // We have a duplicate column
            if (result.contains(finalColumnName))
            {
                throw new IllegalStateException("Two columns have the same name (" + finalColumnName + ")");
            }
    
            result.add(finalColumnName);
        }
        
        return(result);
    }
    
    
    protected void validateJdbcDriverVersion(final ValidatorCallback callback,
                                             final Connection        con,
                                             final ComparablePair[]  supportedVersions,
                                             final String[]          urisForMoreInformation)
    {
        startTest(callback, "JDBC Driver Version");
        
        TestResult testResult = new TestResult();

        try
        {
            ComparablePair jdbcVersion = getJDBCDriverVersion(con);
            
            if (jdbcVersion != null)
            {
                progress(callback, jdbcVersion.getFirst() + "." + jdbcVersion.getSecond());
                
                Arrays.sort(supportedVersions);
                
                if (Arrays.binarySearch(supportedVersions, jdbcVersion) >= 0)
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    testResult.resultType          = TestResult.FAIL;
                    testResult.errorMessage        = "Unsupported JDBC driver version (" + jdbcVersion.getFirst() + "." + jdbcVersion.getSecond() + ")";
                    testResult.ramification        = "Alfresco will not function correctly with this version of the JDBC driver";
                    testResult.remedy              = "Install a JDBC driver with one of the following versions: " + buildSupportedJDBCVersionString(supportedVersions);
                    testResult.urisMoreInformation = urisForMoreInformation;
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType          = TestResult.FAIL;
                testResult.errorMessage        = "Unable to determine JDBC driver version";
                testResult.ramification        = "Alfresco may not function correctly with this version of the JDBC driver";
                testResult.remedy              = "Manually validate that the JDBC driver is one of the following versions: " + buildSupportedJDBCVersionString(supportedVersions);
                testResult.urisMoreInformation = AbstractValidator.ALFRESCO_SPM_URIS;
            }
        }
        catch (final SQLException se)
        {
            progress(callback, "unknown");
            
            testResult.resultType          = TestResult.FAIL;
            testResult.errorMessage        = "Unable to determine JDBC driver version";
            testResult.ramification        = "Alfresco may not function correctly with this version of the JDBC driver";
            testResult.remedy              = "Manually validate that the JDBC driver is one of the following versions: " + buildSupportedJDBCVersionString(supportedVersions);
            testResult.urisMoreInformation = AbstractValidator.ALFRESCO_SPM_URIS;
            testResult.rootCause           = se;
        }
        
        endTest(callback, testResult);
    }
    
    
    private String buildSupportedJDBCVersionString(final ComparablePair[] supportedVersions)
    {
        StringBuffer result = new StringBuffer();
        
        for (int i = 0; i < supportedVersions.length; i++)
        {
            if (supportedVersions[i] != null)
            {
                result.append(String.valueOf(supportedVersions[i].getFirst()));
                result.append('.');
                result.append(String.valueOf(supportedVersions[i].getSecond()));
                
                if (i < supportedVersions.length - 1)
                {
                    result.append(", ");
                }
            }
        }
        
        return(result.toString());
    }
    
    
    protected ComparablePair getJDBCDriverVersion(final Connection con)
        throws SQLException
    {
        ComparablePair result = null;
        
        if (con != null)
        {
            DatabaseMetaData dbMetadata = con.getMetaData();
            
            if (dbMetadata != null)
            {
                int majorVersion = dbMetadata.getDriverMajorVersion();
                int minorVersion = dbMetadata.getDriverMinorVersion();
                
                result = new ComparablePair(new Integer(majorVersion), new Integer(minorVersion));
            }
        }
        
        return(result);
    }
    
}
