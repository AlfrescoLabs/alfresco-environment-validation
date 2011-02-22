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

package org.alfresco.extension.environment.validation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.hyperic.sigar.Sigar;

import org.alfresco.extension.util.ProcessInvoker;
import org.alfresco.extension.util.Quad;
import org.alfresco.extension.util.Triple;


/**
 * This class provides some handy utility functions for Validators.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public abstract class AbstractValidator
    extends    ValidatorCallbackHelper
    implements Validator
{
    // Handy "more information" URIs
    public final static String   ALFRESCO_SUMMARY_SPM_URI_STR  = "http://www.alfresco.com/services/support/stacks/";
    public final static String   ALFRESCO_DETAILED_SPM_URI_STR = "https://network.alfresco.com/?f=default&o=workspace://SpacesStore/4defa351-68cb-4491-9f23-46fb861ddd05";
    public final static String[] ALFRESCO_SPM_URIS             = { ALFRESCO_SUMMARY_SPM_URI_STR, ALFRESCO_DETAILED_SPM_URI_STR };
    public final static String[] ALFRESCO_NETWORK_URI          = { "http://network.alfresco.com/" };
    
    // OS Names
    protected final static String SYSTEM_PROPERTY_OS_NAME = "os.name";
    protected final static String OS_NAME_LINUX           = "Linux";
    protected final static String OS_NAME_SOLARIS         = "SunOS";
    protected final static String OS_NAME_PREFIX_WINDOWS  = "Windows";
    protected final static String OS_NAME_MAC_OSX         = "Mac OS X";

    // OS commands
    private final static String[] OS_COMMAND_PING_WINDOWS = { "ping", "-n", "numPings", "hostname" };
    private final static String[] OS_COMMAND_PING_LINUX   = { "ping", "-q", "-c", "numPings", "hostname" };
    private final static String[] OS_COMMAND_PING_MAC_OSX = OS_COMMAND_PING_LINUX;
    private final static String[] OS_COMMAND_PING_SOLARIS = { "ping", "-ns", "hostname", "56", "numPings" };  // Trust Solaris to be different...


    protected final static Sigar          sigar = new Sigar();
    private   final static ProcessInvoker pi    = new ProcessInvoker();
    
    
    protected String readFile(final String fileName)
        throws IOException
    {
        StringBuffer result = null;
        
        if (fileName != null && fileName.trim().length() > 0)
        {
            Reader fileReader = null;
            char[] buf        = new char[4096];
            int    numRead    = 0;
    
            result = new StringBuffer();
            
            try
            {
                fileReader = new BufferedReader(new FileReader(fileName));
    
                while ((numRead = fileReader.read(buf)) != -1)
                {
                    result.append(String.valueOf(buf, 0, numRead));
                }
            }
            finally
            {
                if (fileReader != null)
                {
                    fileReader.close();
                    fileReader = null;
                }
            }
        }
        
        return(result == null ? null : result.toString());
    }
    
    
    protected Triple executeCommand(final String[] commandAndParameters)
        throws IOException,
               InterruptedException
    {
        return(pi.execute(commandAndParameters));
    }
    
    
    protected Triple executeCommand(final String[] commandAndParameters, long waitTime)
        throws IOException,
               InterruptedException
    {
        return(pi.execute(commandAndParameters, waitTime));
    }


    protected String executeCommandAndGrabStdout(final String[] commandAndParameters)
        throws IOException,
               InterruptedException
    {
        return((String)executeCommand(commandAndParameters).getSecond());
    }
    
    
    protected String executeCommandAndGrabStdout(final String[] commandAndParameters, long waitTime)
        throws IOException,
               InterruptedException
    {
        return((String)executeCommand(commandAndParameters, waitTime).getSecond());
    }


    protected String executeCommandAndGrabStderr(final String[] commandAndParameters)
        throws IOException,
               InterruptedException
    {
        return((String)executeCommand(commandAndParameters).getThird());
    }

    protected String executeCommandAndGrabStderr(final String[] commandAndParameters, long waitTime)
        throws IOException,
               InterruptedException
    {
        return((String)executeCommand(commandAndParameters, waitTime).getThird());
    }
    
    
    protected boolean hostNameResolves(final String hostname)
    {
        boolean result = false;
        
        try
        {
            InetAddress hostAddress  = InetAddress.getByName(hostname); 
                
            if (hostAddress != null)
            {
                result = true;
            }
        }
        catch (UnknownHostException uhe)
        {
            result = false;
        }
        catch (SecurityException se)
        {
            result = false;
        }
        
        return(result);
    }


    /**
     * 
     * @param hostname
     * @return Triple, containing:
     *            BigDecimal: packet loss as a % (eg. 90.0 = 90% packet loss)
     *            BigDecimal: average response time in ms
     *            BigDecimal: std dev response time in ms (or null if not available)
     *         <i>(will be null if running on an unsupported OS)</i>   
     *          
     */
    protected Triple ping(final int numPings, final String hostname)
    {
        Triple result = null;
        
        try
        {
            result = (isWindows()    ? pingWindows(numPings, hostname) :
                      (isLinux()     ? pingLinux(  numPings, hostname) :
                       (isMacOSX()   ? pingMacOSX( numPings, hostname) :
                        (isSolaris() ? pingSolaris(numPings, hostname) : null))));
        }
        catch (InterruptedException ie)
        {
            result = null;
        }
        catch (IOException ioe)
        {
            result = null;
        }
        
        return(result);
    }
    
    
    private Triple pingWindows(final int numPings, final String hostname)
        throws InterruptedException,
               IOException
    {
        String[] pingCommand = OS_COMMAND_PING_WINDOWS;
        String   output      = null;
        
        pingCommand[2] = String.valueOf(numPings);
        pingCommand[3] = hostname;
        output         = executeCommandAndGrabStdout(pingCommand, numPings * 1000 + 2000);   // Wait at least two extra second for the ping command to complete
        
        BigDecimal packetLoss         = parsePacketLoss(output, "% loss)");
        BigDecimal responseTimeAvg    = parseWindowsAvgResponseTime(output);
        BigDecimal responseTimeStdDev = null;   // Not provided by Windows ping
        
        return(new Triple(packetLoss, responseTimeAvg, responseTimeStdDev));
    }
    
    
    private BigDecimal parseWindowsAvgResponseTime(final String pingOutput)
    {
        BigDecimal result = null;
        int        index  = pingOutput.indexOf("Average = ");
        
        if (index > 0)
        {
            index += "Average = ".length();
        
            StringBuffer temp        = new StringBuffer(4);
            char         currentChar = pingOutput.charAt(index);
            
            while (index > 0 &&
                   isDecimalChar(currentChar))
            {
                temp.append(currentChar);
    
                index++;
                currentChar = pingOutput.charAt(index);
            }
            
            result = stringToBigDecimal(temp.toString());
        }
        
        return(result);
    }
    
    
    private Triple pingLinux(final int numPings, final String hostname)
        throws InterruptedException,
               IOException
    {
        return(pingLinuxOrMacOSX(numPings, hostname, "min/avg/max/mdev = "));
    }
    
    
    private Triple pingMacOSX(final int numPings, final String hostname)
        throws InterruptedException,
               IOException
    {
        return(pingLinuxOrMacOSX(numPings, hostname, "min/avg/max/stddev = "));
    }
    
    
    private Triple pingLinuxOrMacOSX(final int numPings, final String hostname, final String minAvgMaxStdDevMarker)
        throws InterruptedException,
               IOException
    {
        String[] pingCommand = OS_COMMAND_PING_LINUX;
        String   output      = null;
        
        pingCommand[3] = String.valueOf(numPings);
        pingCommand[4] = hostname;
        output         = executeCommandAndGrabStdout(pingCommand, numPings * 1000 + 2000);   // Wait at least two extra second for the ping command to complete
        
        BigDecimal packetLoss      = parsePacketLoss(output, "% packet loss");
        Quad       minAvgMaxStdDev = parseResponseTimeMinAvgMaxStdDev(output, minAvgMaxStdDevMarker);
        
        BigDecimal responseTimeAvg    = minAvgMaxStdDev == null ? null : (BigDecimal)minAvgMaxStdDev.getSecond();
        BigDecimal responseTimeStdDev = minAvgMaxStdDev == null ? null : (BigDecimal)minAvgMaxStdDev.getFourth();
        
        return(new Triple(packetLoss, responseTimeAvg, responseTimeStdDev));
    }
    
    
    private Triple pingSolaris(final int numPings, final String hostname)
        throws InterruptedException,
               IOException
    {
        String[] pingCommand = OS_COMMAND_PING_SOLARIS;
        String   output      = null;

        pingCommand[2] = hostname;
        pingCommand[4] = String.valueOf(numPings);
        output         = executeCommandAndGrabStdout(pingCommand, numPings * 1000 + 2000);   // Wait at least two extra seconds for the ping command to complete

        BigDecimal packetLoss         = parsePacketLoss(output, "% packet loss");
        Quad       minAvgMax          = parseResponseTimeMinAvgMaxStdDev(output, "min/avg/max = ");

        BigDecimal responseTimeAvg    = minAvgMax == null ? null : (BigDecimal)minAvgMax.getSecond();
        BigDecimal responseTimeStdDev = null;   // Not provided by Solaris ping

        return(new Triple(packetLoss, responseTimeAvg, responseTimeStdDev));
    }
    
    
    private BigDecimal parsePacketLoss(final String pingOutput, final String parseFrom)
    {
        BigDecimal result = null;
        int        index  = pingOutput.indexOf(parseFrom);

        if (index > 0)
        {
            index--;
            
            StringBuffer temp        = new StringBuffer(3);
            char         currentChar = pingOutput.charAt(index);
            
            while (index >= 0 &&
                   isIntegralChar(currentChar))
            {
                temp.append(currentChar);
    
                index--;
                currentChar = pingOutput.charAt(index);
            }
            
            temp.reverse();

            result = stringToBigDecimal(temp.toString());
        }
        
        return(result);
    }
    
    
    private Quad parseResponseTimeMinAvgMaxStdDev(final String output, final String parseFrom)
    {
        Quad result = null;
        
        if (output != null && parseFrom != null)
        {
            int index = output.indexOf(parseFrom);
            
            if (index >= 0)
            {
                String          snippet   = output.substring(index + parseFrom.length());
                StringTokenizer tokenizer = new StringTokenizer(snippet);
                
                if (tokenizer.hasMoreTokens())
                {
                    String minMaxAvgStdDev = tokenizer.nextToken();
                    
                    result = parseResponseTimeMinAvgMaxStdDev(minMaxAvgStdDev);
                }
            }
        }
        
        return(result);
    }
    
    
    private Quad parseResponseTimeMinAvgMaxStdDev(final String responseTimeStatsStr)
    {
        Quad result = null;
        
        if (responseTimeStatsStr != null)
        {
            String[] responseTimeStats = tokenizeString(responseTimeStatsStr, "/");

            if (responseTimeStats != null)
            {
                BigDecimal min    = null;
                BigDecimal avg    = null;
                BigDecimal max    = null;
                BigDecimal stdDev = null;
                
                if (responseTimeStats.length > 0)
                {
                    min = stringToBigDecimal(responseTimeStats[0]);
                  
                    if (responseTimeStats.length > 1)
                    {
                        avg = stringToBigDecimal(responseTimeStats[1]);
                        
                        if (responseTimeStats.length > 2)
                        {
                            max = stringToBigDecimal(responseTimeStats[2]);
                              
                            if (responseTimeStats.length > 3)
                            {
                                stdDev = stringToBigDecimal(responseTimeStats[3]);
                            }
                        }
                    }
                }
                
                result = new Quad(min, avg, max, stdDev);
            }
        }
        
        return(result);
    }
    
    
    protected String[] tokenizeString(final String theString, final String delimiter)
    {
        String[] result = null;
        
        if (theString != null && delimiter != null)
        {
            List            temp      = new ArrayList();
            StringTokenizer tokenizer = new StringTokenizer(theString, delimiter);
            
            while (tokenizer.hasMoreTokens())
            {
                String nextToken = tokenizer.nextToken();
                temp.add(nextToken);
            }
            
            result = (String[])temp.toArray(new String[0]);
        }
        
        return(result);
    }
    
    
    protected boolean stringContains(final String lookIn, final String lookFor)
    {
        boolean result = false;
        
        if (lookIn != null && lookFor != null)
        {
            result = lookIn.lastIndexOf(lookFor) != -1;
        }
        
        return(result);
    }
    
    
    protected boolean isIntegralChar(char character)
    {
        return(character == '-' ||
               Character.isDigit(character));
    }
    
    
    // Note: not quite accurate, since it allows double negative (-) and decimal point (.) symbols in a single token
    protected boolean isDecimalChar(char character)
    {
        return(character == '.' ||
               isIntegralChar(character));
    }
    
    
    protected Integer stringToInteger(final String theString)
    {
        Integer result = null;
        
        if (theString != null)
        {
            try
            {
                result = Integer.valueOf(theString);
            }
            catch (NumberFormatException nfe)
            {
                result = null;
            }
        }
        
        return(result);
    }
    
    
    protected BigDecimal stringToBigDecimal(final String theString)
    {
        BigDecimal result = null;
        
        if (theString != null)
        {
            try
            {
                result = new BigDecimal(theString);
            }
            catch (NumberFormatException nfe)
            {
                result = null;
            }
        }
        
        return(result);
    }

    
    
    
    protected boolean isMacOSX()
    {
        return(System.getProperty(SYSTEM_PROPERTY_OS_NAME).equals(OS_NAME_MAC_OSX));
    }
    
    protected boolean isLinux()
    {
        return(System.getProperty(SYSTEM_PROPERTY_OS_NAME).equals(OS_NAME_LINUX));
    }
    
    protected boolean isSolaris()
    {
        return(System.getProperty(SYSTEM_PROPERTY_OS_NAME).equals(OS_NAME_SOLARIS));
    }
    
    protected boolean isWindows()
    {
        return(System.getProperty(SYSTEM_PROPERTY_OS_NAME).startsWith(OS_NAME_PREFIX_WINDOWS));
    }
    
    
    // NOTE: Copied from JLAN source code
    protected boolean isWindows64()
    {

        // Check for Windows
        
        String prop = System.getProperty("os.name");
        if (prop == null || prop.startsWith("Windows") == false)
            return false;
        
        // Check the OS architecture
        
        prop = System.getProperty("os.arch");
        if (prop != null && prop.equalsIgnoreCase("amd64"))
            return true;
        
        // Check the VM name
        
        prop = System.getProperty("java.vm.name");
        if (prop != null && prop.indexOf("64-Bit") != -1)
            return true;
        
        // Check the data model
        
        prop = System.getProperty("sun.arch.data.model");
        if (prop != null && prop.equals("64"))
            return true;
        
        // Not 64 bit Windows
        
        return false;
    }
    

}
