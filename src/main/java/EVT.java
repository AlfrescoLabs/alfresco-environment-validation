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



import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.alfresco.extension.environment.validation.validators.AllValidators;
import org.alfresco.extension.environment.validation.validators.DBValidator;


/**
 * This class is a command line entry point to the Environment Validation Tool.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class EVT
{
    // Map of "shortcut" parameters to "long form" parameters
    private static final Map PARAMETER_MAP = new HashMap()
    {{
        put("-t", DBValidator.PARAMETER_DATABASE_TYPE);
        put("-h", DBValidator.PARAMETER_DATABASE_HOSTNAME);
        put("-r", DBValidator.PARAMETER_DATABASE_PORT);
        put("-d", DBValidator.PARAMETER_DATABASE_NAME);
        put("-l", DBValidator.PARAMETER_DATABASE_LOGIN);
        put("-p", DBValidator.PARAMETER_DATABASE_PASSWORD);
    }};
    
    
    private static int verboseMode = 0;    // 0 = not verbose, 1 = verbose, 2 = super verbose
    private static int       DEFAULT_REMAINING_LENGTH = 48;
    
    
    /**
     * @param args
     */
    public static void main(final String[] args)
    {
        // System.out.println("java.library.path = " + System.getProperty("java.library.path"));
        final Map parameters = parseParameters(args);

        if (parameters.containsKey("-v"))                                  verboseMode = 1;
        if (parameters.containsKey("-V") || parameters.containsKey("-vv")) verboseMode = 2;
        
        System.out.println("\nAlfresco Environment Validation Tool (for Alfresco Enterprise 4.0.2)");
        System.out.println("--------------------------------------------------------------------");

        if (parameters.isEmpty()                                             ||
            parameters.containsKey("-?")                                     ||
            parameters.containsKey("--help")                                 ||
            parameters.containsKey("/?")                                     ||
            !parameters.containsKey(DBValidator.PARAMETER_DATABASE_TYPE)     ||
            !parameters.containsKey(DBValidator.PARAMETER_DATABASE_HOSTNAME) ||
            !parameters.containsKey(DBValidator.PARAMETER_DATABASE_LOGIN))
        {
            System.out.println("");
            System.out.println("usage: evt[.sh|.cmd] [-?|--help] [-v] [-V|-vv]");
            System.out.println("            -t databaseType -h databaseHost [-r databasePort]");
            System.out.println("            [-d databaseName] -l databaseLogin [-p databasePassword]");
            System.out.println("");
            System.out.println("where:      -?|--help        - display this help");
            System.out.println("            -v               - produce verbose output");
            System.out.println("            -V|-vv           - produce super-verbose output (stack traces)");
            System.out.println("            databaseType     - the type of database.  May be one of:");
            System.out.println("                               mysql, postgresql, oracle, mssqlserver, db2");
            System.out.println("            databaseHost     - the hostname of the database server");
            System.out.println("            databasePort     - the port the database is listening on (optional -");
            System.out.println("                               defaults to default for the database type)");
            System.out.println("            databaseName     - the name of the Alfresco database (optional -");
            System.out.println("                               defaults to 'alfresco')");
            System.out.println("            databaseLogin    - the login Alfresco will use to connect to the");
            System.out.println("                               database");
            System.out.println("            databasePassword - the password for that user (optional)");
            System.out.println("");
            System.out.println("The tool must be run as the OS user that Alfresco will run as.  In particular");
            System.out.println("it will report erroneous results if run as \"root\" (or equivalent on other");
            System.out.println("OSes) if Alfresco is not intended to be run as that user.");
            System.out.println("");
        }
        else
        {
            final StdoutValidatorCallback callback   = new StdoutValidatorCallback();
            
            (new AllValidators()).validate(parameters, callback);
            
            System.out.println("\n\n                         **** FINAL GRADE: " + TestResult.typeToString(callback.worstResult) + " ****\n");
        }
    }
    
    
    private static Map parseParameters(final String[] args)
    {
        Map result = new HashMap();
        
        for (int i = 0; i < args.length; i++)
        {
            if (PARAMETER_MAP.containsKey(args[i]))
            {
                if ((i + 1) < args.length)
                {
                    String key   = (String)PARAMETER_MAP.get(args[i]);
                    String value = args[i+1];
                    
                    result.put(key, value);
                    i++;  // Skip one
                }
            }
            else
            {
                // Unmapped parameter, just put it in as is, without a value
                result.put(args[i], null);
            }
        }
        
        return(Collections.unmodifiableMap(result));
    }
    
    
    private static class StdoutValidatorCallback
        implements ValidatorCallback
    {
        public  int worstResult = TestResult.PASS;
        
        private int remainingLength;
            
        public void newTopic(final String topicName)
        {
            System.out.print("\n");
            
            if (topicName != null)
            {
                System.out.println("Validating " + topicName);
            }
        }
        
        public void startTest(final String testName)
        {
            int length = (testName == null ? 0 : testName.length());
            
            System.out.print("  ");
            
            if (testName != null)
            {
                System.out.print(testName);
            }
            
            for (int i = 0; i < (22 - length); i++)
            {
                System.out.print(' ');
            }
            
            System.out.print(": ");
            
            remainingLength = DEFAULT_REMAINING_LENGTH;
        }
        
        public void progress(final String progressMessage)
        {
            if (progressMessage != null)
            {
                if (progressMessage.length() > DEFAULT_REMAINING_LENGTH)
                {
                    System.out.println(progressMessage);
                    System.out.print("                          ");
                    remainingLength = DEFAULT_REMAINING_LENGTH;
                }
                else
                {
                    System.out.print(progressMessage + ' ');
                    remainingLength -= progressMessage.length() + 1;
                }
            }
        }
        
        public void endTest(final TestResult testResult)
        {
            for (int i = 0; i < remainingLength; i++)
            {
                System.out.print('.');
            }
            
            System.out.println(testResult.toString(verboseMode));
            
            worstResult = worstResult > testResult.resultType ? testResult.resultType : worstResult; 
        }

    }
        

}
