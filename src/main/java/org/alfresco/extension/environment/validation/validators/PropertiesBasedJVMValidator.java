package org.alfresco.extension.environment.validation.validators;

import java.util.Map;

import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.Validator;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
/**
 * Specific configuration validator for jvm.
 * This validator is based on a configuration file
 * @author philippe
 *
 */
public class PropertiesBasedJVMValidator extends AbstractValidator
{
    private final static String SYSTEM_PROPERTY_JVM_VENDOR         = "java.vendor";
    private final static String   JAVA_DOWNLOAD_URI_STR               = "http://www.oracle.com/technetwork/java/javase/downloads/index.html";
    private final static String[] ALFRESCO_SPM_AND_JAVA_DOWNLOAD_URIS = { ALFRESCO_SUMMARY_SPM_URI_STR, ALFRESCO_DETAILED_SPM_URI_STR, JAVA_DOWNLOAD_URI_STR };
    private final static String SYSTEM_PROPERTY_JVM_VERSION        = "java.version";
    static private final String PROPERTIES_FILE_NAMES = "jvm-validator.properties"; 
    static Configuration specificConfig = null;
    
    
    public PropertiesBasedJVMValidator()
    {
        if (specificConfig != null)
            return;
        //load config
        try
        {
            specificConfig = new PropertiesConfiguration("org/alfresco/extension/environment/validation/validators/jvm-validator.properties");
        }
        catch (ConfigurationException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void validate(Map parameters, ValidatorCallback callback)
    {
        TestResult testResult = new TestResult();
        
        startTest(callback, "Version");   
        String jvmVersion = System.getProperty(SYSTEM_PROPERTY_JVM_VERSION);
        
        if (jvmVersion != null)
        {
            progress(callback, jvmVersion);
            testResult.resultType = this.checkJVMVersion((String)parameters.get("alfresco.version"),jvmVersion);
            String jvmVendor = System.getProperty(SYSTEM_PROPERTY_JVM_VENDOR);
            
            if (jvmVendor != null)
            {
                progress(callback, jvmVendor);
            }
            else
            {
                
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
    
    protected int checkJVMVersion(String alfrescoVersion, String jvmVersion)
    {
        String pureAlfVersion = alfrescoVersion.replaceAll("[\\s.]", "");
        // key example 400.vendor.jvmv-version
        String specificConfigKey = pureAlfVersion + "." + "vendor.jvmv-version";
        
        String imposedJVMVersion = specificConfig.getString(specificConfigKey);
        
        if(jvmVersion.startsWith(imposedJVMVersion))
            return TestResult.PASS;
        
        return TestResult.FAIL;
    }

}
