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

package org.alfresco.extension.environment.validation.validators;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.ResourceLimit;
import org.hyperic.sigar.SigarException;

import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.alfresco.extension.util.Pair;


/**
 * This class validates that the OS and its configuration are suitable for Alfresco.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class OSValidator
    extends AbstractValidator
{
    private final static String VALIDATION_TOPIC = "Operating System";
    
    // Java system properties of interest
    private final static String SYSTEM_PROPERTY_OS_VERSION = "os.version";

    // OS versions
    private final static String OS_NAME_WINDOWS_SERVER_2003 = "Windows 2003";
    private final static String OS_NAME_WINDOWS_SERVER_2008 = "Windows Server 2008";
    private final static String OS_VERSION_SOLARIS_10       = "5.10";
    private final static String OS_VERSION_RHEL_5           = "5";
    private final static String OS_VERSION_SUSE_11          = "11";
    private final static String OS_VERSION_UBUNTU_10_04     = "10.04";
    
    // Linux distribution detection
    private final static String   FILE_NAME_RHEL_INFORMATION     = "/etc/redhat-release";
    private final static String   RHEL_DISTRIBUTION_NAME_PREFIX  = "Red Hat Enterprise Linux";
    private final static String   RHEL5_DISTRIBUTION_NAME_PREFIX = RHEL_DISTRIBUTION_NAME_PREFIX + " Server release 5";
    private final static String[] OS_COMMAND_LSB_DISTRO          = { "lsb_release", "-si" };
    private final static String[] OS_COMMAND_LSB_VERSION         = { "lsb_release", "-sr" };
    private final static String   DISTRO_NAME_RHEL               = "RHEL";
    private final static String   DISTRO_NAME_SUSE               = "SuSE";
    private final static String   DISTRO_NAME_UBUNTU             = "Ubuntu";
    private final static String   FILE_NAME_SUSE_INFORMATION     = "/etc/SuSE-release";
    private final static String   SUSE_DISTRIBUTION_NAME_PREFIX  = "SUSE Linux Enterprise Server";
    
    // File descriptors
    private final static int MINIMUM_FILE_DESCRIPTORS = 4096;
    
    
    private final static String[] RHEL_URI    = { "http://www.redhat.com/rhel/" };
    private final static String[] SUSE_URI    = { "http://www.novell.com/linux/" };
    private final static String[] UBUNTU_URI  = { "http://www.ubuntu.com/desktop/get-ubuntu/download" };
    private final static String[] SOLARIS_URI = { "http://www.oracle.com/technetwork/server-storage/solaris/downloads/index.html" };
    
    private final static Pattern  SUSE_VERSION_REGEX = Pattern.compile("\nVERSION = (\\d+)\\n");

    

    
    
    /**
     * @see org.alfresco.extension.environment.validation.Validator#validate(java.util.Map, org.alfresco.extension.environment.validation.ValidatorCallback)
     */
    public void validate(final Map parameters, final ValidatorCallback callback)
    {
        newTopic(callback, VALIDATION_TOPIC);
        
        validateOsAndVersion(callback);
        validateArchitecture(callback);
        validateFileDescriptorLimit(callback);
    }


    
    private void validateOsAndVersion(final ValidatorCallback callback)
    {
        startTest(callback, "OS");
        
        TestResult testResult = new TestResult();
        String osName = System.getProperty(SYSTEM_PROPERTY_OS_NAME);
        
        if (osName != null)
        {
            progress(callback, osName);
            
            if (OS_NAME_LINUX.equals(osName))
            {
                testResult.resultType = TestResult.PASS;
                endTest(callback, testResult);
                
                validateLinux(callback);
            }
            else if (OS_NAME_SOLARIS.equals(osName))
            {
                testResult.resultType = TestResult.PASS;
                endTest(callback, testResult);
                
                validateSolaris(callback);
            }
            else if (OS_NAME_MAC_OSX.equals(osName))
            {
                testResult.resultType          = TestResult.WARN;
                testResult.errorMessage        = OS_NAME_MAC_OSX + " is not supported by Alfresco";
                testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                testResult.remedy              = "Install a supported OS";
                testResult.urisMoreInformation = ALFRESCO_SPM_URIS;

                endTest(callback, testResult);
            }
            else if (osName.startsWith(OS_NAME_PREFIX_WINDOWS))
            {
                if (OS_NAME_WINDOWS_SERVER_2003.equals(osName) ||
                    OS_NAME_WINDOWS_SERVER_2008.equals(osName))
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    testResult.resultType          = TestResult.WARN;
                    testResult.errorMessage        = osName + " is not supported by Alfresco";
                    testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                    testResult.remedy              = "Install a supported OS";
                    testResult.urisMoreInformation = ALFRESCO_SPM_URIS;
                }

                endTest(callback, testResult);
            }
            else
            {
                testResult.resultType          = TestResult.FAIL;
                testResult.errorMessage        = osName + " is not supported by Alfresco";
                testResult.ramification        = "Alfresco probably won't start, and even if it does it will not function properly";
                testResult.remedy              = "Install a supported OS";
                testResult.urisMoreInformation = ALFRESCO_SPM_URIS;
                
                endTest(callback, testResult);
            }
        }
        else
        {
            testResult.resultType          = TestResult.FAIL;
            testResult.errorMessage        = "Unable to determine OS";
            testResult.ramification        = "Alfresco probably won't start, and even if it does it will not function properly";
            testResult.remedy              = "Install a supported OS";
            testResult.urisMoreInformation = ALFRESCO_SPM_URIS;
            
            endTest(callback, testResult);
        }
    }
    
    private void validateLinux(final ValidatorCallback callback)
    {
        startTest(callback, "Distribution");
        
        TestResult testResult        = new TestResult();        
        Pair       linuxDistribution = guessLinuxDistribution();
        
        if (linuxDistribution != null && linuxDistribution.getFirst() != null)
        {
            String distribution = (String)linuxDistribution.getFirst();
            String version      = (String)linuxDistribution.getSecond();
            
            progress(callback, distribution + (version == null ? "" : (" " + version)));
        
            if (DISTRO_NAME_RHEL.equals(distribution))
            {
                if (OS_VERSION_RHEL_5.equals(version))
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    testResult.resultType          = TestResult.WARN;
                    testResult.errorMessage        = "Unsupported RHEL version";
                    testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                    testResult.remedy              = "Install RHEL 5";
                    testResult.urisMoreInformation = RHEL_URI;
                }
            }
            else if (DISTRO_NAME_SUSE.equals(distribution))
            {
                if (OS_VERSION_SUSE_11.equals(version))
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    testResult.resultType          = TestResult.WARN;
                    testResult.errorMessage        = "Unsupported SuSE version";
                    testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                    testResult.remedy              = "Install SuSE " + OS_VERSION_SUSE_11;
                    testResult.urisMoreInformation = SUSE_URI;
                }
            }
            else if (DISTRO_NAME_UBUNTU.equals(distribution))
            {
                if (OS_VERSION_UBUNTU_10_04.equals(version))
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    testResult.resultType          = TestResult.WARN;
                    testResult.errorMessage        = "Unsupported Ubuntu version";
                    testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                    testResult.remedy              = "Install Ubuntu " + OS_VERSION_UBUNTU_10_04;
                    testResult.urisMoreInformation = UBUNTU_URI;
                }
            }
            else
            {
                testResult.resultType          = TestResult.WARN;
                testResult.errorMessage        = "Unsupported Linux distribution";
                testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
                testResult.remedy              = "Install a supported OS";
                testResult.urisMoreInformation = ALFRESCO_SPM_URIS;
            }
        }
        else
        {
            progress(callback, "unknown");
            
            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Unable to detect Linux distribution";
            testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
            testResult.remedy              = "Install a supported OS";
            testResult.urisMoreInformation = ALFRESCO_SPM_URIS;
        }
        
        endTest(callback, testResult);
    }
    
    private final void validateSolaris(final ValidatorCallback callback)
    {
        startTest(callback, "Version");
        
        TestResult testResult = new TestResult();        
        String     osVersion  = System.getProperty(SYSTEM_PROPERTY_OS_VERSION);
        
        progress(callback, osVersion);
        
        if (OS_VERSION_SOLARIS_10.equals(osVersion))
        {
            testResult.resultType = TestResult.PASS;
        }
        else
        {
            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Unsupported " + OS_NAME_SOLARIS + " version";
            testResult.ramification        = "Alfresco may function sufficiently well for development purposes but must not be used for production";
            testResult.remedy              = "Install " + OS_NAME_SOLARIS + " " + OS_VERSION_SOLARIS_10;
            testResult.urisMoreInformation = SOLARIS_URI;
        }
        
        endTest(callback, testResult);
    }
    
    private void validateArchitecture(final ValidatorCallback callback)
    {
        startTest(callback, "OS Architecture");
        
        TestResult      testResult = new TestResult();
        OperatingSystem os         = OperatingSystem.getInstance();
        
        String dataModel = os.getDataModel();
        
        if (dataModel != null)
        {
            if ("64".equals(dataModel))
            {
                progress(callback, dataModel + " bit");
                
                testResult.resultType = TestResult.PASS;
            }
            else if ("32".equals(dataModel))
            {
                progress(callback, dataModel + " bit");
                
                testResult.resultType   = TestResult.INFO;
                testResult.errorMessage = "32 bit operating system detected";
                testResult.ramification = "32 bit architectures have inherent scalability limitations.  Alfresco will function correctly but for high-scale instances, a 64 bit architecture is recommended";
                testResult.remedy       = "Consider installing a 64 bit operating system";
            }
            else if ("unknown".equalsIgnoreCase(dataModel))
            {
                progress(callback, dataModel + "unknown");
                
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "Unable to detect operating system architecture";
                testResult.ramification = "Alfresco may not start, and if it does it may not function properly";
                testResult.remedy       = "Please manually validate that the operating system is 32 bit or (preferably) 64 bit";
            }
            else
            {
                progress(callback, dataModel + "unknown");
                
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = dataModel + "bit operating system detected";
                testResult.ramification = dataModel + "bit architectures are not supported";
                testResult.remedy       = "Install a 32 bit or (preferably) a 64 bit operating system";
            }
        }
        else
        {
            progress(callback, dataModel + "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to detect operating system architecture";
            testResult.ramification = "Alfresco may not start, and if it does it may not function properly";
            testResult.remedy       = "Please manually validate that the operating system is 32 bit or (preferably) 64 bit";
        }

        endTest(callback, testResult);
    }
    
    private void validateFileDescriptorLimit(final ValidatorCallback callback)
    {
        if (!isWindows())
        {
            startTest(callback, "File Descriptors");
            
            TestResult    testResult    = new TestResult();
            ResourceLimit resourceLimit = null;

            try
            {
                resourceLimit = sigar.getResourceLimit();
            }
            catch (SigarException se)
            {
                // Ignore it and move on
            }
            
            if (resourceLimit != null)
            {
                long maxOpenFiles = resourceLimit.getOpenFilesCur();
                
                progress(callback, String.valueOf(maxOpenFiles));
                
                if (maxOpenFiles >= MINIMUM_FILE_DESCRIPTORS)
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    testResult.resultType   = TestResult.FAIL;
                    testResult.errorMessage = "Alfresco requires at least " + MINIMUM_FILE_DESCRIPTORS + " file descriptors";
                    testResult.ramification = "While Alfresco will start correctly, you will likely see errors during indexing of content into the search engine";
                    testResult.remedy       = "Increase the number of file descriptors available to Alfresco";
                }
            }
            else
            {
                progress(callback, "unknown");
                
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "Unable to determine limit on file descriptors";
                testResult.remedy       = "Please run 'ulimit -n' manually and ensure the result is at least " + MINIMUM_FILE_DESCRIPTORS;
            }
            
            endTest(callback, testResult);
        }
    }

    
    private Pair guessLinuxDistribution()
    {
        Pair result = null;
        
        // First up try to get RHEL information
        result = attemptToReadRHELInformation();

        // If that failed, try to get the SUSE information
        if (result == null)
        {
            result = attemptToReadSUSEInformation();
        }
        
        // If that failed, try to get LSB information (which isn't available on RHEL or SUSE by default, of course! :-\ )
        if (result == null)
        {
            result = attemptToReadLSBInformation();
        }
        
        return(result);
    }
    
    
    private Pair attemptToReadRHELInformation()
    {
        Pair result = null;

        try
        {
            String redhatRelease = readFile(FILE_NAME_RHEL_INFORMATION);
            
            if (redhatRelease != null)
            {
                if (redhatRelease.startsWith(RHEL_DISTRIBUTION_NAME_PREFIX))
                {
                    if (redhatRelease.startsWith(RHEL5_DISTRIBUTION_NAME_PREFIX))
                    {
                        result = new Pair(DISTRO_NAME_SUSE, OS_VERSION_RHEL_5);
                    }
                    else
                    {
                        result = new Pair(DISTRO_NAME_SUSE, null);
                    }
                }
                else
                {
                    result = new Pair(redhatRelease.trim(), null);
                }
            }
        }
        catch (IOException ioe)
        {
            // Ignore it and move on
        }
        
        return(result);
    }
    
    
    private Pair attemptToReadSUSEInformation()
    {
        Pair result = null;
        
        try
        {
            String suseRelease = readFile(FILE_NAME_SUSE_INFORMATION);
            
            if (suseRelease != null)
            {
                if (suseRelease.startsWith(SUSE_DISTRIBUTION_NAME_PREFIX))
                {
                    Matcher versionNumberMatcher = SUSE_VERSION_REGEX.matcher(suseRelease);
                    String  versionNumberStr     = null;
                    
                    if (versionNumberMatcher.find())
                    {
                        versionNumberStr = versionNumberMatcher.group(1);
                        
                        if (versionNumberStr != null)
                        {
                            try
                            {
                                // Note: we're only parsing the string to ensure it's an integer - we deliberately throw away the result
                                Integer.parseInt(versionNumberStr.trim());
                            }
                            catch (final NumberFormatException nfe)
                            {
                                versionNumberStr = null;
                            }
                        }
                    }
                    
                    result = new Pair(DISTRO_NAME_SUSE, versionNumberStr);
                }
                else
                {
                    result = new Pair(suseRelease.trim(), null);
                }
            }
        }
        catch (IOException ioe)
        {
            // Ignore it and move on
        }
        
        return(result);
    }
        
    
    private Pair attemptToReadLSBInformation()
    {
        Pair result = null;
        
        try
        {
            String lsbDistro  = executeCommandAndGrabStdout(OS_COMMAND_LSB_DISTRO);
            String lsbVersion = executeCommandAndGrabStdout(OS_COMMAND_LSB_VERSION);
            
            if (lsbDistro != null && lsbDistro.trim().length() > 0)
            {
                result = new Pair(lsbDistro.trim(), (lsbVersion == null || lsbVersion.trim().length() == 0) ? null : lsbVersion.trim());
            }
        }
        catch (IOException ioe)
        {
            // Ignore it and move on
        }
        catch (InterruptedException ie)
        {
            // Ignore it and move on
        }
        
        return(result);
    }

}
