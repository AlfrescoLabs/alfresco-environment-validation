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

import java.math.BigDecimal;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Map;

import org.alfresco.extension.environment.validation.*;
import org.alfresco.extension.util.Pair;
import org.alfresco.extension.util.Triple;


/**
 * This class validates that the network configuration is suitable for Alfresco.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class NetworkValidator
    extends AbstractValidator
{
    private final static String VALIDATION_TOPIC = "Network";
    
    // Parameters to this validator
    // None 
    
    // Generic (non-CIFS) port number validation rules - please feel free to add others as necessary
    private final PortValidationRule[] TCP_PORTS = {
                                                       new PortValidationRule(8005,  "Tomcat",     TestResult.FAIL, "Alfresco will not start", "Check for running processes that are using this port (eg. existing Tomcat instance)"),
                                                       new PortValidationRule(8080,  "HTTP",       TestResult.FAIL, "Alfresco will not start", "Check for running processes that are using this port (eg. existing Tomcat instances)"),
                                                       new PortValidationRule(50500, "RMI",        TestResult.FAIL, "Alfresco will not start", "Check for running processes that are using this port (eg. existing Alfresco instances)"),
                                                       new PortValidationRule(21,    "FTP",        TestResult.WARN, "FTP protocol will be unavailable", "Check for running processes that are using this port (eg. existing FTP servers), on non-Windows OSes consider reconfiguring Alfresco to use a non-privileged port then use a port forwarding tool (such as iptables) to forward this port, alternate port 2121 will be tested next"),
                                                       //alternate TCP
                                                       new PortValidationRule(2121,  "FTP",        TestResult.WARN, "FTP protocol will be unavailable", "An alternate port 2121 was checked without any success!"),
                                                       new PortValidationRule(139,   "NetBT",      TestResult.WARN, "CIFS protocol will be unavailable to pre-Windows 2000 clients", "Check for running processes that are using that port (eg. existing CIFS servers), on non-Windows OSes consider reconfiguring Alfresco to use a non-privileged port then use a port forwarding tool (such as iptables) to forward this port"),
                                                       new PortValidationRule(1139,  "NetBT",     TestResult.WARN, "CIFS protocol will be unavailable to pre-Windows 2000 clients", "Check for running processes that are using that port 1139 as an alternative to 139 without success, on non-Windows OSes consider reconfiguring Alfresco to use a non-privileged port then use a port forwarding tool (such as iptables) to forward this port"),
                                                       new PortValidationRule(445,   "SMB",        TestResult.WARN, "CIFS protocol will be unavailable", "Check for running processes that are using this port (eg. existing CIFS servers), on non-Windows OSes consider reconfiguring Alfresco to use a non-privileged port then use a port forwarding tool (such as iptables) to forward this port"),
                                                       new PortValidationRule(4445,  "SMB",        TestResult.WARN, "CIFS protocol will be unavailable", "An alternate poert 4445 tested without any success"),
                                                       new PortValidationRule(8100,  "OpenOffice", TestResult.WARN, "OpenOffice daemon will be unable to start", "Check for running processes that are using this port (eg. zombie OO processes)"),
                                                       new PortValidationRule(7070,  "Sharepoint", TestResult.WARN, "Sharepoint protocol will be unavailable", "Check for running processes that are using this port"),
                                                       new PortValidationRule(25,    "SMTP",       TestResult.INFO, "SMTP protocol will be unavailable (note: not enabled by default)", "Check for running processes that are using this port (eg. existing SMTP servers), on non-Windows OSes consider reconfiguring Alfresco to use a non-privileged port then use a port forwarding tool (such as iptables) to forward this port"),
                                                       new PortValidationRule(2525,  "SMTP",       TestResult.INFO, "SMTP protocol will be unavailable (note: not enabled by default)", "Check for running processes that are using this port (eg. existing SMTP servers), An alternate port 2525 was checked without any success!"),
                                                       new PortValidationRule(143,   "IMAP",       TestResult.INFO, "IMAP protocol will be unavailable (note: not enabled by default)", "Check for running processes that are using this port (eg. existing IMAP servers), on non-Windows OSes consider reconfiguring Alfresco to use a non-privileged port then use a port forwarding tool (such as iptables) to forward this port")
                                                   };
    
    private final PortValidationRule[] UDP_PORTS = {
                                                       new PortValidationRule(137,   "NetBT",      TestResult.WARN, "CIFS protocol will be unavailable to pre-Windows 2000 clients", "Check for running processes that are using this port (eg. existing CIFS servers), on non-Windows OSes consider reconfiguring Alfresco to use a non-privileged port then use a port forwarding tool (such as iptables) to forward this port"),
                                                       new PortValidationRule(138,   "NetBT",      TestResult.WARN, "CIFS protocol will be unavailable to pre-Windows 2000 clients", "Check for running processes that are using this port (eg. existing CIFS servers), on non-Windows OSes consider reconfiguring Alfresco to use a non-privileged port then use a port forwarding tool (such as iptables) to forward this port")
                                                   };

    // Ping statistics
    private  final static String     LOCALHOST_IP_ADDRESS_PREFIX    = "127.";
    private  final static int        NUMBER_OF_PINGS                = 10;
    private  final static BigDecimal MAXIMUM_PACKET_LOSS            = new BigDecimal(0.0);
    private  final static BigDecimal MAXIMUM_AVG_RESPONSE_TIME_MS   = new BigDecimal(10.0);
    private  final static BigDecimal MAXIMUM_STDEV_RESPONSE_TIME_MS = MAXIMUM_AVG_RESPONSE_TIME_MS.divide((new BigDecimal(10.0)), BigDecimal.ROUND_UP);    // 1/10 of the average
    
    
    
    /**
     * @see org.alfresco.extension.environment.validation.Validator#validate(java.util.Map, org.alfresco.extension.environment.validation.ValidatorCallback)
     */
    public void validate(final Map parameters, final ValidatorCallback callback)
    {
        newTopic(callback, VALIDATION_TOPIC);

        String databaseHostname = parameters == null ? null : (String)parameters.get(DBValidator.PARAMETER_DATABASE_HOSTNAME);
        
        validateHostname(callback);
        validateTcpPorts(callback);
        validateUdpPorts(callback);
        
        if (isWindows())
        {
            validateCifsWindows(callback);
        }
        
        if (validateDatabaseParameters(callback, databaseHostname))
        {
            validateDatabaseNetworkLatency(callback, databaseHostname); 
        }
    }
    
    
    private void validateTcpPorts(final ValidatorCallback callback)
    {
        if (TCP_PORTS != null)
        {
            for (int i = 0; i < TCP_PORTS.length; i++)
            {
                validateTcpPort(callback, TCP_PORTS[i].portNumber, TCP_PORTS[i].portDescription, TCP_PORTS[i].unableToBindResultType, TCP_PORTS[i].ramification, TCP_PORTS[i].remedy);
            }
        }
    }

    
    private void validateUdpPorts(final ValidatorCallback callback)
    {
        if (UDP_PORTS != null)
        {
            for (int i = 0; i < UDP_PORTS.length; i++)
            {
                validateUdpPort(callback, UDP_PORTS[i].portNumber, UDP_PORTS[i].portDescription, UDP_PORTS[i].unableToBindResultType, UDP_PORTS[i].ramification, UDP_PORTS[i].remedy);
            }
        }
    }
    
    
    private void validateCifsWindows(final ValidatorCallback callback)
    {
        if (isWindows())
        {
            TestResult testResult = new TestResult();
            String     dllName    = "Win32NetBIOS";
            
            if (isWindows64())
            {
                dllName = "Win32NetBIOSx64";
            }
    
            startTest(callback, dllName + ".dll");
    
            // Attempt to Load the Win32 NetBIOS interface library
            try
            {
                System.loadLibrary(dllName);
                
                progress(callback, "available");
                testResult.resultType = TestResult.PASS;
            }
            catch (Throwable t)
            {
                progress(callback, "unavailable");
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "Unable to load " + dllName + ".dll";
                testResult.ramification = "CIFS protocol will be unavailable";
                testResult.remedy       = "Ensure " + dllName + ".dll is in the PATH";
                testResult.rootCause    = t;
            }
            
            endTest(callback, testResult);
        }
    }
    
    
    private void validateTcpPort(final ValidatorCallback callback,
                                 final int               portNumber,
                                 final String            portDescription,
                                 final int               failureResultType,
                                 final String            ramification,
                                 final String            remedy)
    {
        TestResult testResult = new TestResult();
        
        startTest(callback, "TCP " + portNumber + (portDescription == null ? "" : (" (" + portDescription + ")")));
        
        Pair    result  = checkTcpPort(portNumber);
        boolean success = ((Boolean)result.getFirst()).booleanValue();
        
        if (success)
        {
            progress(callback, "available");
            testResult.resultType = TestResult.PASS;
        }
        else
        {
            progress(callback, "unavailable");
            Exception rootCause = (Exception)result.getSecond();
            
            testResult.resultType   = failureResultType;
            testResult.errorMessage = rootCause == null ? null : rootCause.getMessage();
            testResult.ramification = ramification;
            testResult.remedy       = remedy;
            testResult.rootCause    = rootCause;
        }
        
        endTest(callback, testResult);
    }
    
    
    private void validateUdpPort(final ValidatorCallback callback,
                                 final int               portNumber,
                                 final String            portDescription,
                                 final int               failureResultType,
                                 final String            ramification,
                                 final String            remedy)
    {
        TestResult testResult = new TestResult();
        
        startTest(callback, "UDP " + portNumber + (portDescription == null ? "" : (" (" + portDescription + ")")));
        
        Pair    result  = checkUdpPort(portNumber);
        boolean success = ((Boolean)result.getFirst()).booleanValue();
        
        if (success)
        {
            progress(callback, "available");
            testResult.resultType = TestResult.PASS;
        }
        else
        {
            progress(callback, "unavailable");
            Exception rootCause = (Exception)result.getSecond();
            
            testResult.resultType   = failureResultType;
            testResult.errorMessage = rootCause == null ? null : rootCause.getMessage();
            testResult.ramification = ramification;
            testResult.remedy       = remedy;
            testResult.rootCause    = rootCause;
        }
        
        endTest(callback, testResult);
    }

    
    private boolean validateDatabaseParameters(final ValidatorCallback callback, final String databaseHostname)
    {
        startTest(callback, "Database Hostname");
        
        TestResult testResult = new TestResult();
        
        boolean result = (databaseHostname != null);
        
        if (result)
        {
            if (hostNameResolves(databaseHostname))
            {
                progress(callback, "resolved");
                testResult.resultType = TestResult.PASS;
            }
            else
            {
                progress(callback, "unresolved");
                
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "Host name " + databaseHostname + " could not be resolved to an IP address";
                testResult.ramification = "The network configuration cannot be validated";
                testResult.remedy       = "Review the DNS configuration on your network to ensure the hostname " + databaseHostname + " can be resolved";
            }
        }
        else
        {
            progress(callback, "not provided");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "The database server hostname was not provided to the validation tool";
            testResult.ramification = "The network configuration cannot be validated";
            testResult.remedy       = "Rerun the validaton tool, providing the database server hostname";
        }
        
        endTest(callback, testResult);
        
        return(result);
    }
    
    
    private void validateDatabaseNetworkLatency(final ValidatorCallback callback, final String databaseHostname)
    {
        Triple pingStatistics = validatePacketLoss(callback, databaseHostname);
        
        if (pingStatistics != null)
        {
            validateAvgResponseTime(callback,    (BigDecimal)pingStatistics.getSecond());
            validateResponseTimeStdDev(callback, (BigDecimal)pingStatistics.getThird());
        }
    }
    
    
    private Triple validatePacketLoss(final ValidatorCallback callback, final String databaseHostname)
    {
        startTest(callback, "Packet Loss");
        
        TestResult testResult = new TestResult();
        Triple     result     = null;
        
        progress(callback, "(please wait)");
            
        result = ping(NUMBER_OF_PINGS, databaseHostname);
            
        if (result != null)
        {
            BigDecimal packetLoss = (BigDecimal)result.getFirst();
            
            if (packetLoss != null)
            {
                progress(callback, String.valueOf(packetLoss) + "%");
                
                if (MAXIMUM_PACKET_LOSS.compareTo(packetLoss) >= 0)
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    testResult.resultType   = TestResult.FAIL;
                    testResult.errorMessage = "Excessive packet loss between Alfresco server and database server";
                    testResult.ramification = "Alfresco's performance will be significantly degraded";
                    testResult.remedy       = "Review the network connection to ensure packet loss is <= " + MAXIMUM_PACKET_LOSS + "%";
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "Unable to determine packet loss between Alfresco server and database server";
                testResult.ramification = "Alfresco's performance may be significantly degraded";
                testResult.remedy       = "Manually determine packet loss (eg. using the 'ping' command) and ensure it is <= " + MAXIMUM_PACKET_LOSS + "%";
            }
        }
        else
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine network characteristics between Alfresco server and database server";
            testResult.ramification = "Alfresco's performance may be significantly degraded";
            testResult.remedy       = "Manually determine network characteristics (eg. using the 'ping' command), and ensure packet loss is <= " + MAXIMUM_PACKET_LOSS + "%, " +
                                      "average response time is <= " + MAXIMUM_AVG_RESPONSE_TIME_MS + "ms, " +
                                      "response time standard deviation is <= " + MAXIMUM_STDEV_RESPONSE_TIME_MS + "ms";
        }
        
        endTest(callback, testResult);
        
        return(result);
    }
    
    private void validateAvgResponseTime(final ValidatorCallback callback, final BigDecimal avgResponseTimeInMs)
    {
        startTest(callback, "Average Response Time");
        
        TestResult testResult = new TestResult();
            
        if (avgResponseTimeInMs != null)
        {
            progress(callback, String.valueOf(avgResponseTimeInMs) + "ms");
            
            if (MAXIMUM_AVG_RESPONSE_TIME_MS.compareTo(avgResponseTimeInMs) >= 0)
            {
                testResult.resultType = TestResult.PASS;
            }
            else
            {
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = "Average response time between Alfresco server and database server exceeds " + MAXIMUM_AVG_RESPONSE_TIME_MS + "ms";
                testResult.ramification = "Alfresco's performance will be significantly degraded";
                testResult.remedy       = "Review the network connection to ensure average response time is <= " + MAXIMUM_AVG_RESPONSE_TIME_MS + "ms";
            }
        }
        else
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine average response time between Alfresco server and database server";
            testResult.ramification = "Alfresco's performance may be significantly degraded";
            testResult.remedy       = "Manually determine average response time (eg. using the 'ping' command) and ensure it is <= " + MAXIMUM_AVG_RESPONSE_TIME_MS + "ms";
        }
        
        endTest(callback, testResult);
    }
    
    private void validateResponseTimeStdDev(final ValidatorCallback callback, final BigDecimal responseTimeStdDev)
    {
        startTest(callback, "Response Time Std Dev");
        
        TestResult testResult = new TestResult();
            
        if (responseTimeStdDev != null)
        {
            progress(callback, String.valueOf(responseTimeStdDev) + "ms");
            
            if (MAXIMUM_STDEV_RESPONSE_TIME_MS.compareTo(responseTimeStdDev) >= 0)
            {
                testResult.resultType = TestResult.PASS;
            }
            else
            {
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = "Response time standard deviation between Alfresco server and database server exceeds " + MAXIMUM_STDEV_RESPONSE_TIME_MS + "ms";
                testResult.ramification = "Alfresco's performance will be significantly degraded";
                testResult.remedy       = "Review the network connection to ensure response time standard deviation is <= " + MAXIMUM_STDEV_RESPONSE_TIME_MS + "ms";
            }
        }
        else
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine response time standard deviation between Alfresco server and database server";
            testResult.ramification = "Alfresco's performance may be significantly degraded";
            testResult.remedy       = "Manually determine response time standard deviation (eg. using the 'ping' command) and ensure it is <= " + MAXIMUM_STDEV_RESPONSE_TIME_MS + "ms";
        }
        
        endTest(callback, testResult);
    }
    
    /*
     * Note: a resolveable hostname is only required for clustering
     */
    private void validateHostname(final ValidatorCallback callback)
    {
        InetAddress localHost = validateLocalHostname(callback);
        
        if (localHost != null)
        {
            String localIpAddress = validateIpAddress(callback, localHost);
            
            if (localIpAddress != null)
            {
                validateDNSHostame(callback, localHost, localIpAddress);
            }
        }
    }
    
    private InetAddress validateLocalHostname(final ValidatorCallback callback)
    {
        startTest(callback, "Local Hostname");

        TestResult  testResult = new TestResult();
        InetAddress result     = null; 
        
        try
        {
            result = InetAddress.getLocalHost();

            if (result != null)
            {
                String hostname = result.getHostName();
                
                progress(callback, hostname);
                
                testResult.resultType = TestResult.PASS;
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "Unable to determine local hostname";
                testResult.ramification = "Alfresco cannot be clustered";
                testResult.remedy       = "Fix the server's network configuration";
            }
        }
        catch (UnknownHostException uhe)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine local hostname";
            testResult.ramification = "Alfresco cannot be clustered";
            testResult.remedy       = "Fix the server's network configuration";
            testResult.rootCause    = uhe;
        }
        
        endTest(callback, testResult);
        
        return(result);
    }
    
    private String validateIpAddress(final ValidatorCallback callback, final InetAddress localHost)
    {
        startTest(callback, "IP Address");

        TestResult testResult = new TestResult();
        String     result     = localHost.getHostAddress();
        
        if (result != null)
        {
            progress(callback, result);
            
            if (result.startsWith(LOCALHOST_IP_ADDRESS_PREFIX))
            {
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "IP address for hostname " + localHost.getHostName() + " is in the localhost address range (127.*.*.*)";
                testResult.ramification = "Alfresco cannot be clustered";
                testResult.remedy       = "Fix the server's network configuration and/or the DNS configuration";
            }
            else
            {
                testResult.resultType = TestResult.PASS;
            }
        }
        else
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine IP address for " + localHost.getHostName();
            testResult.ramification = "Alfresco cannot be clustered";
            testResult.remedy       = "Fix the server's network configuration";
        }
        
        endTest(callback, testResult);
        
        return(result);
    }
    
    private void validateDNSHostame(final ValidatorCallback callback, final InetAddress localHost, final String ipAddress)
    {
        startTest(callback, "DNS Hostname");

        TestResult testResult    = new TestResult();
        
        try
        {
            String     localHostname = localHost.getHostName();
            String     dnsHostname   = InetAddress.getByName(ipAddress).getHostName();   // getCanonicalHostname would be better, but it only appeared in JDK 1.4
            
            if (dnsHostname != null)
            {
                progress(callback, dnsHostname);
                
                if (localHostname.equals(dnsHostname))
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    // Note: based on some clustering tests by Rich McKnight, this isn't strictly necessary for a cluster to function
                    //####TODO: REVISIT!
                    testResult.resultType   = TestResult.WARN;
                    testResult.errorMessage = "Local (" + localHostname + ") and DNS (" + dnsHostname + ") hostnames don't match";
                    testResult.ramification = "Alfresco cannot be clustered";
                    testResult.remedy       = "Fix the server's network configuration and/or the DNS configuration";
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "Unable to resolve hostname for IP address " + ipAddress;
                testResult.ramification = "Alfresco cannot be clustered";
                testResult.remedy       = "Fix the DNS configuration";
            }
        }
        catch (UnknownHostException uhe)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to resolve hostname for IP address " + ipAddress;
            testResult.ramification = "Alfresco cannot be clustered";
            testResult.remedy       = "Fix the DNS configuration";
            testResult.rootCause    = uhe;
        }
        
        
        endTest(callback, testResult);
    }
    
    
    private Pair checkTcpPort(int portNumber)
    {
        Pair         result = null;
        ServerSocket socket = null;
        
        try
        {
            socket = new ServerSocket(portNumber);
            result = new Pair(Boolean.TRUE, null);
        }
        catch (Exception e)
        {
            result = new Pair(Boolean.FALSE, e);
        }
        finally
        {
            if (socket != null)
            {
                try
                {
                    socket.close();
                    socket = null;
                }
                catch (Exception e2)
                {
                    // Silently swallow the exception - we don't really care if closing the socket fails
                }
            }
        }
        
        return(result);
    }


    private Pair checkUdpPort(int portNumber)
    {
        Pair           result = null;
        DatagramSocket socket = null;
        
        try
        {
            socket = new DatagramSocket(portNumber);
            result = new Pair(Boolean.TRUE, null);
        }
        catch (Exception e)
        {
            result = new Pair(Boolean.FALSE, e);
        }
        finally
        {
            if (socket != null)
            {
                try
                {
                    socket.close();
                    socket = null;
                }
                catch (Exception e2)
                {
                    // Silently swallow the exception - we don't really care if closing the socket fails
                }
            }
        }
        
        return(result);
    }
    

    // Ugh Java really is teh suck
    private class PortValidationRule
    {
        private final int    portNumber;
        private final String portDescription;
        private final int    unableToBindResultType;
        private final String ramification;
        private final String remedy;
        
        public PortValidationRule(final int    portNumber,
                                  final String portDescription,
                                  final int    unableToBindResultType,
                                  final String ramification,
                                  final String remedy)
        {
            this.portNumber             = portNumber;
            this.portDescription        = portDescription;
            this.unableToBindResultType = unableToBindResultType;
            this.ramification           = ramification;
            this.remedy                 = remedy;
        }
    }
    
    
}
