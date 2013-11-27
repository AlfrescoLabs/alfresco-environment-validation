package org.alfresco.extension.util;

import java.util.List;

import org.apache.commons.configuration.Configuration;

/**
 * Utility methods to manipulate configuration properties
 * @author philippe
 *
 */
public class PropertiesUtil
{
    /**
     * Utility method returning the "sparator" separated string of the options corresponding to the key
     * @param config apache common configuration
     * @param key configuration key
     * @param separator separator that will be used to build the key
     * @return
     */
    public static String concatenatePropsValues(Configuration config, String key, String separator)
    {
        List options = config.getList(key);
        StringBuffer sBuffer = new StringBuffer(256);
        for(int i = 0;i < options.size(); i++)
        {
            if(sBuffer.length() != 0)
                sBuffer.append(separator);
            sBuffer.append(options.get(i));
        }
        return sBuffer.toString();
    }
}
