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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.ValidatorCallback;


/**
 * This class validates that the JVM is suitable for Alfresco.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class JVMValidator
    extends AbstractValidator
{
    private final static String VALIDATION_TOPIC = "JVM";
    
    private final static String SYSTEM_PROPERTY_JVM_VENDOR         = "java.vendor";
    private final static String SYSTEM_PROPERTY_JVM_HOME           = "java.home";
    private final static String SYSTEM_PROPERTY_JVM_SPEC_VERSION   = "java.specification.version";
    private final static String SYSTEM_PROPERTY_JVM_VERSION        = "java.version";
    private final static String SYSTEM_PROPERTY_JVM_ARCHITECTURE   = "sun.arch.data.model";   // See http://stackoverflow.com/questions/2062020/how-can-i-tell-if-im-running-in-64-bit-jvm-or-32-bit-jvm
    private final static String SYSTEM_PROPERTY_JVM_ARCHITECTURE_2 = "os.arch";
    
    private final static String JVM_VENDOR_SUN   = "Sun Microsystems Inc.";
    private final static String JVM_VENDOR_IBM   = "IBM Corporation";
    private final static String JVM_VENDOR_APPLE = "Apple Inc.";
    
    private final static String  JVM_VERSION_15                    = "1.5";
    private final static String  JVM_VERSION_16                    = "1.6";
    private final static int     JVM_VERSION_16_MINIMUM_PATCHLEVEL = 22;
    private final static Pattern JVM_PATCHLEVEL_REGEX              = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)_([0-9]+)");
    
    private final static String   JAVA_DOWNLOAD_URI_STR               = "http://www.oracle.com/technetwork/java/javase/downloads/index.html";
    private final static String[] JAVA_DOWNLOAD_URI                   = { JAVA_DOWNLOAD_URI_STR };
    private final static String[] ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS = { ALFRESCO_SUMMARY_SPM_URI_STR, ALFRESCO_DETAILED_SPM_URI_STR, JAVA_DOWNLOAD_URI_STR };
    
    private final static Map OS_ARCH_TO_ARCHITECTURE_MAP = Collections.unmodifiableMap(new HashMap() {{ // This list comes from http://lopica.sourceforge.net/os.html 
            put("x86",        "32");
            put("x86_64",     "64");
            put("i386",       "32");
            put("i686",       "32?");  // This can be either 32bit or 64bit - insufficient information to know for sure
            put("amd64",      "64");
            put("sparc",      "64");
            put("PowerPC",    "32?");  // This can be either 32bit or 64bit - insufficient information to know for sure
            put("Power",      "32?");  // This can be either 32bit or 64bit - insufficient information to know for sure
            put("Power_RS",   "32");
            put("ppc",        "32");
            put("ppc64",      "64");
            put("arm",        "32");
            put("armv41",     "32");
            put("PA-RISC",    "32?");  // This can be either 32bit or 64bit - insufficient information to know for sure
            put("PA_RISC",    "32?");  // This can be either 32bit or 64bit - insufficient information to know for sure
            put("PA_RISC2.0", "32?");  // This can be either 32bit or 64bit - insufficient information to know for sure
            put("IA64",       "64");
            put("IA64N",      "64");
            put("02.10.00",   "32");   // S/390
            put("mips",       "32?");  // This can be either 32bit or 64bit - insufficient information to know for sure
            put("alpha",      "64");
        }});    

    
    /**
     * @see org.alfresco.extension.environment.validation.Validator#validate(java.util.Map, org.alfresco.extension.environment.validation.ValidatorCallback)
     */
    public void validate(final Map parameters, final ValidatorCallback callback)
    {
        newTopic(callback, VALIDATION_TOPIC);
        
        validateVendor(callback);
        validateVersion(callback);
        validateArchitecture(callback);
        validateJavaHome(callback);
    }
    
    
    private void validateVendor(final ValidatorCallback callback)
    {
        TestResult testResult = new TestResult();
        
        startTest(callback, "Vendor");
        
        String jvmVendor = System.getProperty(SYSTEM_PROPERTY_JVM_VENDOR);
        
        if (jvmVendor != null)
        {
            progress(callback, jvmVendor);
            
            if (JVM_VENDOR_SUN.equals(jvmVendor))
            {
                testResult.resultType = TestResult.PASS;
            }
            else if (JVM_VENDOR_IBM.equals(jvmVendor))
            {
                testResult.resultType = TestResult.PASS; 
            }
            else if (JVM_VENDOR_APPLE.equals(jvmVendor))
            {
                testResult.resultType          = TestResult.WARN;
                testResult.errorMessage        = "The Apple JVM is not supported by Alfresco";
                testResult.ramification        = "Alfresco functions sufficiently well for development purposes but must not be used for production";
                testResult.urisMoreInformation = ALFRESCO_SPM_URIS;
            }
            else
            {
                testResult.resultType          = TestResult.FAIL;
                testResult.errorMessage        = "Unsupported JVM";
                testResult.ramification        = "Alfresco will not function properly";
                testResult.remedy              = "Install a supported 1.6 JVM";
                testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
            }
        }
        else
        {
            testResult.resultType          = TestResult.FAIL;
            testResult.errorMessage        = "Unable to determine JVM vendor";
            testResult.ramification        = "Alfresco will not function properly";
            testResult.remedy              = "Install a supported 1.6 JVM";
            testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
        }
        
        endTest(callback, testResult);
    }
    
    
    private void validateVersion(final ValidatorCallback callback)
    {
        TestResult testResult = new TestResult();
        
        startTest(callback, "Version");
        
        String jvmVersion = System.getProperty(SYSTEM_PROPERTY_JVM_VERSION);
        
        if (jvmVersion != null)
        {
            progress(callback, jvmVersion);

            if (jvmVersion.startsWith(JVM_VERSION_16))
            {
                String jvmVendor = System.getProperty(SYSTEM_PROPERTY_JVM_VENDOR);
                
                if (JVM_VENDOR_SUN.equals(jvmVendor) || JVM_VENDOR_APPLE.equals(jvmVendor))
                {
                    Matcher matcher = JVM_PATCHLEVEL_REGEX.matcher(jvmVersion);
                    
                    if (matcher.find())
                    {
                        String jvmPatchLevelStr = matcher.group(4);
                        int    jvmPatchLevel    = -1;
                        
                        try
                        {
                            jvmPatchLevel = Integer.parseInt(jvmPatchLevelStr);
                            
                            if (jvmPatchLevel >= JVM_VERSION_16_MINIMUM_PATCHLEVEL)
                            {
                                testResult.resultType = TestResult.PASS;
                            }
                            else
                            {
                                testResult.resultType          = TestResult.WARN;
                                testResult.errorMessage        = "Alfresco requires a 1.6 JVM, patchlevel " + JVM_VERSION_16_MINIMUM_PATCHLEVEL + " or higher";
                                testResult.ramification        = "Alfresco functions sufficiently well for development purposes but must not be used for production";
                                testResult.remedy              = "Install a supported 1.6 JVM, patchlevel " + JVM_VERSION_16_MINIMUM_PATCHLEVEL + " or higher";
                                testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
                            }
                            
                        }
                        catch (final NumberFormatException nfe)
                        {
                            testResult.resultType          = TestResult.WARN;
                            testResult.errorMessage        = "Unrecognised JVM patchlevel: " + jvmPatchLevel;
                            testResult.ramification        = "Please manually validate that a 1.6 JVM, patchlevel " + JVM_VERSION_16_MINIMUM_PATCHLEVEL + " or higher is installed";
                            testResult.remedy              = "Install a supported 1.6 JVM, patchlevel " + JVM_VERSION_16_MINIMUM_PATCHLEVEL + " or higher";
                            testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
                        }
                    }
                    else
                    {
                        testResult.resultType          = TestResult.WARN;
                        testResult.errorMessage        = "Unable to determine JVM patchlevel";
                        testResult.ramification        = "Please manually validate that a 1.6 JVM, patchlevel " + JVM_VERSION_16_MINIMUM_PATCHLEVEL + " or higher is installed";
                        testResult.remedy              = "Install a 1.6 JVM, patchlevel " + JVM_VERSION_16_MINIMUM_PATCHLEVEL + " or higher";
                        testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
                    }
                }
                else
                {
                    // It's not a Sun or Apple JVM, so we can't validate the patchlevel
                    testResult.resultType = TestResult.PASS;
                }
            }
            else if (JVM_VERSION_15.equals(jvmVersion))
            {
                testResult.resultType          = TestResult.WARN;
                testResult.errorMessage        = "Since v3.0, Alfresco no longer supports the 1.5 JVM";
                testResult.ramification        = "Alfresco functions sufficiently well for development purposes but must not be used for production";
                testResult.remedy              = "Install a supported 1.6 JVM";
                testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
            }
            else
            {
                testResult.resultType          = TestResult.FAIL;
                testResult.errorMessage        = "Unsupported JVM version";
                testResult.ramification        = "Alfresco probably won't start, and even if it does it will not function properly";
                testResult.remedy              = "Install a supported 1.6 JVM";
                testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
            }
        }
        else
        {
            progress(callback, "unknown");
            
            testResult.resultType          = TestResult.FAIL;
            testResult.errorMessage        = "Unable to determine JVM version";
            testResult.ramification        = "Alfresco probably won't start, and even if it does it will not function properly";
            testResult.remedy              = "Install the Sun 1.6 JVM";
            testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
        }
        
        endTest(callback, testResult);
    }
    
    
    private void validateArchitecture(final ValidatorCallback callback)
    {
        TestResult testResult = new TestResult();
        
        startTest(callback, "JVM Architecture");
        
        String jvmArchitecture = System.getProperty(SYSTEM_PROPERTY_JVM_ARCHITECTURE);
        
        if (jvmArchitecture == null)
        {
            // Try again, using the (less reliable) "os.arch" property
            String osArch = System.getProperty(SYSTEM_PROPERTY_JVM_ARCHITECTURE_2);
            
            if (osArch != null)
            {
                jvmArchitecture = (String)OS_ARCH_TO_ARCHITECTURE_MAP.get(osArch);
            }
        }
        
        if (jvmArchitecture != null)
        {
            if ("64".equals(jvmArchitecture))
            {
                progress(callback, jvmArchitecture + " bit");
                
                testResult.resultType = TestResult.PASS;
            }
            else if ("32".equals(jvmArchitecture))
            {
                progress(callback, jvmArchitecture + " bit");
                
                testResult.resultType          = TestResult.INFO;
                testResult.errorMessage        = "32 bit JVM detected";
                testResult.ramification        = "32 bit architectures have inherent scalability limitations.  Alfresco will function correctly but for high-scale instances, a 64 bit architecture is recommended";
                testResult.remedy              = "Consider installing a 64 bit JVM";
                testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
            }
            else
            {
                progress(callback, "unknown");
                
                if ("32?".equals(jvmArchitecture))
                {
                    testResult.resultType          = TestResult.INFO;
                    testResult.errorMessage        = "Probable 32 bit JVM detected";
                    testResult.ramification        = "32 bit architectures have inherent scalability limitations.  Alfresco will function correctly but for high-scale instances, a 64 bit architecture is recommended";
                    testResult.remedy              = "Manually validate the JVM architecture and consider installing a 64 bit JVM";
                    testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
                }
                else
                {
                    testResult.resultType          = TestResult.WARN;
                    testResult.errorMessage        = "Unable to determine JVM architecture";
                    testResult.ramification        = "Alfresco may not start, and if it does it may not function properly";
                    testResult.remedy              = "Manually validate that the JVM architecture is 32 bit or (preferably) 64 bit";
                    testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
                }
            }
        }
        else
        {
            progress(callback, "unknown");
            
            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Unable to determine JVM architecture";
            testResult.ramification        = "Alfresco may not start, and if it does it may not function properly";
            testResult.remedy              = "Manually validate that the JVM architecture is 32 bit or (preferably) 64 bit";
            testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
        }
        
        endTest(callback, testResult);
    }
    
    
    private void validateJavaHome(final ValidatorCallback callback)
    {
        TestResult testResult = new TestResult();
        
        startTest(callback, "Java Home");
        
        String javaHome = System.getProperty(SYSTEM_PROPERTY_JVM_HOME);
        
        if (javaHome != null)
        {
            progress(callback, javaHome);
            
            if (!stringContains(javaHome, " "))
            {
                testResult.resultType = TestResult.PASS;
            }
            else
            {
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "Java Home path contains spaces";
                testResult.ramification = "Alfresco may not function properly, as the JVM itself has bugs when the installation path contains spaces";
                testResult.remedy       = "Reinstall the JVM in a path that does not contain spaces";
            }
        }
        else
        {
            testResult.resultType          = TestResult.FAIL;
            testResult.errorMessage        = "Unable to determine Java Home";
            testResult.ramification        = "Alfresco probably won't start, and even if it does it will not function properly";
            testResult.remedy              = "Install the Sun 1.6 JVM";
            testResult.urisMoreInformation = JAVA_DOWNLOAD_URI;
        }
        
        endTest(callback, testResult);
    }
    
    
}
