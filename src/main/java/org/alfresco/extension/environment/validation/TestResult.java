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

package org.alfresco.extension.environment.validation;


/**
 * This class encapsulates the result of a single validation test.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class TestResult
{
    public final static int FAIL = 0;
    public final static int WARN = 1;
    public final static int INFO = 2;
    public final static int PASS = 3;
    
    public int       resultType;
    public String    errorMessage;
    public String    ramification;
    public String    remedy;
    public String[]  urisMoreInformation;
    public Throwable rootCause;
    
    
    public static String typeToString(final int resultType)
    {
        return(resultType == FAIL ? "FAIL!!" :
               (resultType == WARN ? "WARN!" :
                (resultType == INFO ? "INFO" :
                 "PASS")));
    }
    
    
    public String toString(int verboseMode)
    {
        String verboseOutput = (verboseMode > 0 ? (
                                                    (errorMessage        == null ? ""          : "\n    Reason              : " + errorMessage) +
                                                    (ramification        == null ? ""          : "\n    Ramification        : " + ramification) +
                                                    (remedy              == null ? ""          : "\n    Remedy              : " + remedy) +
                                                    (urisMoreInformation == null ? ""          : "\n    For more information: " + arrayToString(urisMoreInformation, "\n                        : ")) +
                                                    (verboseMode > 1 ? (rootCause == null ? "" : "\n    Exception           :" + exceptionToString(rootCause)) : "")
                                                  )
                                                : "");
                                            
        if (verboseMode > 0 && verboseOutput.length() > 0)
        {
            verboseOutput = verboseOutput + "\n";
        }
        
        return(typeToString(resultType) + verboseOutput);
    }
    
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return(toString(0));
    }
    
    
    private String arrayToString(final String[] array)
    {
        return(arrayToString(array, ", "));
    }
    

    //####TODO: handle null String values in the array properly
    private String arrayToString(final String[] array, final String delimiter)
    {
        StringBuffer result = new StringBuffer();
        
        if (array != null && array.length > 0)
        {
            for (int i = 0; i < array.length; i++)
            {
                result.append(array[i]);
                
                if (i < (array.length - 1))
                {
                    result.append(delimiter);
                }
            }
        }

        return(result.toString());
    }
    
    
    private String exceptionToString(final Throwable throwable)
    {
        StringBuffer result = new StringBuffer();

        if (throwable != null)
        {
            String    message = throwable.getMessage();
            Throwable cause   = throwable.getCause();

            if (cause != null)
            {
                result.append(exceptionToString(cause));
                result.append("\nWrapped by:");
            }

            if (message == null)
            {
                message = "";
            }

            result.append("\n");
            result.append(throwable.getClass().getName());
            result.append(": ");
            result.append(message);
            result.append("\n");
            result.append(renderStackTraceElements(throwable.getStackTrace()));
        }

        return(result.toString());
    }
    

    private String renderStackTraceElements(StackTraceElement[] elements)
    {
        StringBuffer result = new StringBuffer();

        if (elements != null)
        {
            for (int i = 0; i < elements.length; i++)
            {
                result.append("\tat " + elements[i].toString() + "\n");
            }
        }

        return(result.toString());
    }
    
}
