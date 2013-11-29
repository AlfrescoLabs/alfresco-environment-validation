package org.alfresco.extension.environment.validation.validators;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.Validator;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * Specific configuration validator for jvm. This validator is based on a configuration file
 * 
 * @author Peter Monks (pmonks@alfresco.com)
 * @author philippe (philippe.dubois@alfresco.com)
 */
public class PropertiesBasedJVMValidator extends AbstractValidator
{
    private final static String VALIDATION_TOPIC = "JVM";
    private final static String SYSTEM_PROPERTY_JVM_VENDOR = "java.vendor";
    private final static String JAVA_DOWNLOAD_URI_STR = "http://www.oracle.com/technetwork/java/javase/downloads/index.html";
    private final static String[] ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS =
    { ALFRESCO_SUMMARY_SPM_URI_STR, ALFRESCO_DETAILED_SPM_URI_STR, JAVA_DOWNLOAD_URI_STR };
    private final static String SYSTEM_PROPERTY_JVM_VERSION = "java.version";
    static private final String PROPERTIES_FILE_NAMES = "jvm-validator.properties";
    private final static Pattern JVM_PATCHLEVEL_REGEX              = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)_([0-9]+)");
    static Configuration specificConfig = null;

    public PropertiesBasedJVMValidator()
    {
        if (specificConfig != null)
            return;
        // load config
        try
        {
            specificConfig = new PropertiesConfiguration(
                    "org/alfresco/extension/environment/validation/validators/jvm-validator.properties");
        }
        catch (ConfigurationException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * @see org.alfresco.extension.environment.validation.Validator#validate(java.util.Map,
     *      org.alfresco.extension.environment.validation.ValidatorCallback)
     */
    public void validate(final Map parameters, final ValidatorCallback callback)
    {
        newTopic(callback, VALIDATION_TOPIC);

        validateVendor(parameters, callback);
        validateVersion(parameters,callback);
        // validateArchitecture(callback);
        // validateJavaHome(callback);
    }

    public void validateVendor(Map parameters, ValidatorCallback callback)
    {
        TestResult testResult = new TestResult();

        startTest(callback, "Version");
        String jvmVersion = System.getProperty(SYSTEM_PROPERTY_JVM_VERSION);

        if (jvmVersion != null)
        {
            progress(callback, jvmVersion);
            testResult.resultType = this.checkJVMVersion((String) parameters.get("alfresco.version"), jvmVersion);
            if (testResult.resultType == testResult.PASS)
            {
                String jvmVendor = System.getProperty(SYSTEM_PROPERTY_JVM_VENDOR);

                if (jvmVendor != null)
                {
                    progress(callback, jvmVendor);
                    int res = checkVendorSupported((String) parameters.get("alfresco.version"), jvmVendor);
                    if (res == testResult.FAIL)
                    {
                        testResult.resultType = TestResult.FAIL;
                        testResult.errorMessage = "Unsupported vendor " + jvmVendor;
                        testResult.ramification = "Alfresco probably won't start, and even if it does it will not function properly";
                        testResult.remedy = "Install the jvm specified in Alfresco Supported Stack matrix";
                        testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
                    }

                    if (res == testResult.WARN)
                    {
                        testResult.resultType = TestResult.WARN;
                        testResult.errorMessage = "The " + jvmVendor + " vendor is not supported by Alfresco";
                        testResult.ramification = "Alfresco functions sufficiently well for development purposes but must not be used for production";
                        testResult.urisMoreInformation = ALFRESCO_SPM_URIS;
                    }
                }
                else
                {
                    testResult.resultType = TestResult.FAIL;
                    testResult.errorMessage = "Unable to determine JVM vendor";
                    testResult.ramification = "Alfresco will not function properly";
                    testResult.remedy = "Install a supported 1.6 JVM";
                    testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
                }
            }
            else
            {
                testResult.resultType = TestResult.FAIL;
                testResult.errorMessage = "Unexpected jvm version " + jvmVersion + " for Alfresco version " + (String) parameters.get("alfresco.version");
                testResult.ramification = "Alfresco will not function properly";
                testResult.remedy = "Install a supported 1.6 JVM";
                testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
            }
        }
        else
        {
            progress(callback, "unknown");

            testResult.resultType = TestResult.FAIL;
            testResult.errorMessage = "Unable to determine JVM version";
            testResult.ramification = "Alfresco probably won't start, and even if it does it will not function properly";
            testResult.remedy = "Install the Sun 1.6 JVM";
            testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
        }

        endTest(callback, testResult);
    }

    protected int checkJVMVersion(String alfrescoVersion, String jvmVersion)
    {
        String pureAlfVersion = alfrescoVersion.replaceAll("[\\s.]", "");
        // key example 400.vendor.jvmv-version
        String specificConfigKey = pureAlfVersion + "." + "vendor.jvmv-version";

        String imposedJVMVersion = specificConfig.getString(specificConfigKey);

        if (imposedJVMVersion == null)
        {
            System.out.println(specificConfigKey
                    + "not found in checkJVMVersion. Checker probably not configured for that version "
                    + alfrescoVersion + "!");
            return TestResult.FAIL;
        }

        if (jvmVersion.startsWith(imposedJVMVersion))
        {
            return TestResult.PASS;
        }

        return TestResult.FAIL;
    }

    protected int checkVendorSupported(String alfrescoVersion, String vendor)
    {
        // get the list of supported vendor for alfrescoVersion
        String pureAlfVersion = alfrescoVersion.replaceAll("[\\s.]", "");
        // key example 400.vendor.supported
        String specificConfigKey = pureAlfVersion + "." + "vendor.supported";

        List supportedVendorList = specificConfig.getList(specificConfigKey);
        // check that vendor is in supportedVendorLis
        for (int i = 0; i < supportedVendorList.size(); i++)
        {
            String supVendor = (String) supportedVendorList.get(i);
            if (supVendor.equals(vendor))
                return TestResult.PASS;
        }

        // maybe this is a warn
        specificConfigKey = pureAlfVersion + "." + "vendor.warn";
        supportedVendorList = specificConfig.getList(specificConfigKey);
        // check that vendor is in supportedVendorLis
        for (int i = 0; i < supportedVendorList.size(); i++)
        {
            String supVendor = (String) supportedVendorList.get(i);
            if (supVendor.equals(vendor))
                return TestResult.WARN;
        }

        return TestResult.FAIL;
    }

    public void validateVersion(Map parameters, ValidatorCallback callback)
    {
        TestResult testResult = new TestResult();
        testResult.resultType = testResult.PASS;
        startTest(callback, "Patch level");
        
        String jvmVersion = System.getProperty(SYSTEM_PROPERTY_JVM_VERSION);
        Matcher matcher = JVM_PATCHLEVEL_REGEX.matcher(jvmVersion);
        String checkedAlfrescoVersion = (String) parameters.get("alfresco.version");
        String pureAlfVersion = checkedAlfrescoVersion.replaceAll("[\\s.]", "");
        
        int minimumPatchLevel = specificConfig.getInt(pureAlfVersion + ".vendor.supported.patch-level");
        
        String requiredJVMVersion =  specificConfig.getString(pureAlfVersion + ".vendor.jvmv-version");
        
        if (matcher.find())
        {
            String jvmPatchLevelStr = matcher.group(4);
            progress(callback, jvmPatchLevelStr);
            int    jvmPatchLevel    = -1;
            
            try
            {
                jvmPatchLevel = Integer.parseInt(jvmPatchLevelStr);
                               
                if (jvmPatchLevel >= minimumPatchLevel)
                {
                    testResult.resultType = TestResult.PASS;
                }
                else
                {
                    testResult.resultType          = TestResult.WARN;
                    testResult.errorMessage        = "Alfresco requires a " + requiredJVMVersion + "JVM, patchlevel " + minimumPatchLevel + " or higher";
                    testResult.ramification        = "Alfresco functions sufficiently well for development purposes but must not be used for production";
                    testResult.remedy              = "Install a supported " + requiredJVMVersion + " JVM, patchlevel " + minimumPatchLevel + " or higher";
                    testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
                }
                
            }
            catch (final NumberFormatException nfe)
            {
                testResult.resultType          = TestResult.WARN;
                testResult.errorMessage        = "Unrecognised JVM patchlevel: " + jvmPatchLevel;
                testResult.ramification        = "Please manually validate that a " + requiredJVMVersion + "JVM, patchlevel " + minimumPatchLevel + " or higher is installed";
                testResult.remedy              = "Install a supported " + requiredJVMVersion + " JVM, patchlevel " + jvmPatchLevelStr + " or higher";
                testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
            }
        }
        else
        {
            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Unable to determine JVM patchlevel";
            testResult.ramification        = "Please manually validate that a " + "1.6" + " JVM, patchlevel ";
            testResult.remedy              = "Install a " +  requiredJVMVersion  + "patchlevel " + minimumPatchLevel + " or higher";
            testResult.urisMoreInformation = ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS;
        }
        endTest(callback, testResult);
    }
    

}
