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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.ValidatorCallback;
import org.alfresco.extension.util.Pair;


/**
 * This class validates that the various 3rd party applications Alfresco uses are installed and versions that are suitable for use by Alfresco.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class ThirdPartyApplicationValidator
    extends AbstractValidator
{
    private final static String VALIDATION_TOPIC = "3rd Party Apps";
    
    private final static String[]   OS_COMMAND_OPEN_OFFICE_UNIX     = { "soffice", "-headless", "-help" };   // Warning: doesn't terminate on Linux with OO 3.1
    private final static String[]   OS_COMMAND_OPEN_OFFICE_WINDOWS  = { "soffice", "-help" , "-headless"};
    private final static Pattern    OPEN_OFFICE_VERSION_REGEX       = Pattern.compile("OpenOffice\\.org ([0-9]+\\.[0-9]+)");
    private final static String     MINIMUM_OPEN_OFFICE_VERSION_STR = "3.2";    
    private final static BigDecimal MINIMUM_OPEN_OFFICE_VERSION     = new BigDecimal(MINIMUM_OPEN_OFFICE_VERSION_STR);
    
    private final static String   OPEN_OFFICE_DOWNLOAD_URI                      = "http://download.openoffice.org/";
    private final static String[] OPEN_OFFICE_DOWNLOAD_URIS                     = { OPEN_OFFICE_DOWNLOAD_URI };
    private final static String[] OPEN_OFFICE_DOWNLOAD_URI_AND_ALFRESCO_OO_DOCS = { OPEN_OFFICE_DOWNLOAD_URI, "http://wiki.alfresco.com/wiki/Setting_up_OpenOffice_for_Alfresco" };
    
    
    private final static String[]   OS_COMMAND_IMAGE_MAGICK_UNIX     = { "convert", "-version" };
    private final static String[]   OS_COMMAND_IMAGE_MAGICK_WINDOWS  = { "convert", "-version" };
    private final static Pattern    IMAGE_MAGICK_VERSION_REGEX       = Pattern.compile("Version: ImageMagick (([0-9]+\\.[0-9]+)\\.[0-9]+(-[0-9]*)?)");
    private final static String     MINIMUM_IMAGE_MAGICK_VERSION_STR = "6.8";   
    private final static BigDecimal MINIMUM_IMAGE_MAGICK_VERSION     = new BigDecimal(MINIMUM_IMAGE_MAGICK_VERSION_STR);
    
    private final static String   IMAGE_MAGICK_DOWNLOAD_URI                      = "http://www.imagemagick.org/";
    private final static String[] IMAGE_MAGICK_DOWNLOAD_URIS                     = { IMAGE_MAGICK_DOWNLOAD_URI };
    private final static String[] IMAGE_MAGICK_DOWNLOAD_URI_AND_ALFRESCO_IM_DOCS = { IMAGE_MAGICK_DOWNLOAD_URI, "http://wiki.alfresco.com/wiki/ImageMagick_Configuration" };  // Note: this wiki page is badly out of date...
    
    private final static String[]   OS_COMMAND_PDF2SWF                              = { "pdf2swf", "-V" };
    private final static Pattern    PDF2SWF_VERSION_REGEX                           = Pattern.compile("pdf2swf - part of swftools (([0-9]\\.[0-9])?.*)");
    private final static String     MINIMUM_PDF2SWF_VERSION_STR                     = "0.9";
    private final static BigDecimal MINIMUM_PDF2SWF_VERSION                         = new BigDecimal(MINIMUM_PDF2SWF_VERSION_STR);
    private final static String     PDF2SWF_DOWNLOAD_URI                            = "http://www.swftools.org/";
    private final static String[]   PDF2SWF_DOWNLOAD_URIS                           = { PDF2SWF_DOWNLOAD_URI };
    private final static String[]   PDF2SWF_DOWNLOAD_URI_AND_ALFRESCO_SWFTOOLS_DOCS = { "http://www.swftools.org/", "http://wiki.alfresco.com/wiki/Installing_Alfresco_components#Installing_SWFTools" };
    
    
    /**
     * @see org.alfresco.extension.environment.validation.Validator#validate(java.util.Map, org.alfresco.extension.environment.validation.ValidatorCallback)
     */
    public void validate(final Map parameters, final ValidatorCallback callback)
    {
        newTopic(callback, VALIDATION_TOPIC);
        
        validateOpenOffice(callback);
        validateImageMagick(callback);
        validateSwfTools(callback);
    }
    

    private void validateOpenOffice(final ValidatorCallback callback)
    {
        String openOfficeHelpOutput = validateCanForkOpenOffice(callback);
        
        if (openOfficeHelpOutput != null)
        {
            validateOpenOfficeVersion(callback, openOfficeHelpOutput);
        }
    }
    
    
    private String validateCanForkOpenOffice(final ValidatorCallback callback)
    {
        startTest(callback, "Can fork OpenOffice");
        
        String     result            = null;
        TestResult testResult        = new TestResult();
        String[]   openOfficeCommand = isWindows() ? OS_COMMAND_OPEN_OFFICE_WINDOWS : OS_COMMAND_OPEN_OFFICE_UNIX;
        
        try
        {
        	if (isWindows())
            {
            	result = executeCommandAndGrabStdout(openOfficeCommand);
            }
            else
            {
            	result = executeCommandAndGrabStderr(openOfficeCommand);
            }

            // If the command was successfully executed but returned nothing, fake some output just to force the version validation to run
            if (result == null)
            {
                result = "";
            }
            
            progress(callback, "yes");
            
            testResult.resultType = TestResult.PASS;
        }
        catch (Exception e)
        {
            progress(callback, "no");
            
            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Unable to fork OpenOffice executable";
            testResult.ramification        = "Various document format transformations, as well as full text indexing of various document formats will be unavailable";
            testResult.remedy              = "Install OpenOffice v" + MINIMUM_OPEN_OFFICE_VERSION_STR + " or greater and either ensure it is in the PATH or configure Alfresco to point to the fully qualified path of the OpenOffice executable ('soffice')";
            testResult.urisMoreInformation = OPEN_OFFICE_DOWNLOAD_URI_AND_ALFRESCO_OO_DOCS;
            testResult.rootCause           = e;
        }
        
        endTest(callback, testResult);
        
        return(result);
    }
    
    
    private void validateOpenOfficeVersion(final ValidatorCallback callback, final String openOfficeHelpOutput)
    {
        startTest(callback, "OpenOffice Version");
        
        TestResult testResult = new TestResult();
        
        // NOTE: It appears that OpenOffice uses some weird scheme to write its help screen - a scheme that doesn't result in the output going to stdout.
        //       As a result we are unable to read that output and parse it - instead all we get are a series of blank lines.  In this case we issue a
        //       warning to manually check the version number, which is probably the best of a bad lot of options.
        // UPDATE : On Linux it goes to stderr, but on Windows it disappears.... 
        Pair       parsedVersion = parseVersionString(OPEN_OFFICE_VERSION_REGEX, openOfficeHelpOutput, 1, 1);
        String     versionStr    = (String)parsedVersion.getFirst();
        BigDecimal version       = (BigDecimal)parsedVersion.getSecond();
        
        if (versionStr == null)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine OpenOffice version";
            testResult.remedy       = "Manually validate that OpenOffice v" + MINIMUM_OPEN_OFFICE_VERSION_STR + " or greater is installed by running the 'soffice -headless -help' command";
        }
        else
        {
            progress(callback, versionStr);
            
            if (version != null && MINIMUM_OPEN_OFFICE_VERSION.compareTo(version) <= 0)
            {
                testResult.resultType = TestResult.PASS;
            }
            else
            {
                testResult.resultType          = TestResult.FAIL;
                testResult.errorMessage        = "Alfresco requires OpenOffice v" + MINIMUM_OPEN_OFFICE_VERSION_STR + " or greater";
                testResult.ramification        = "While Alfresco will start correctly, various document transformations and some full text indexing will not function correctly";
                testResult.remedy              = "Install OpenOffice v" + MINIMUM_OPEN_OFFICE_VERSION_STR + " or greater";
                testResult.urisMoreInformation = OPEN_OFFICE_DOWNLOAD_URIS;
            }
        }
        
        endTest(callback, testResult);
    }

    
    private void validateImageMagick(final ValidatorCallback callback)
    {
        String imageMagickHelpOutput = validateCanForkImageMagick(callback);
        
        if (imageMagickHelpOutput != null)
        {
            validateImageMagickVersion(callback, imageMagickHelpOutput);
        }
    }
    
    
    private String validateCanForkImageMagick(final ValidatorCallback callback)
    {
        startTest(callback, "Can fork ImageMagick");
     
        String     result             = null;
        TestResult testResult         = new TestResult();
        String[]   imageMagickCommand = isWindows() ? OS_COMMAND_IMAGE_MAGICK_WINDOWS : OS_COMMAND_IMAGE_MAGICK_UNIX;
        
        
        try
        {
            result = executeCommandAndGrabStdout(imageMagickCommand);

            // If the command was successfully executed but returned nothing, fake some output just to force the version validation to run
            if (result == null)
            {
                result = "";
            }
            
            progress(callback, "yes");
            
            testResult.resultType = TestResult.PASS;
        }
        catch (final Exception e)
        {
            progress(callback, "no");
            
            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Unable to fork ImageMagick executable";
            testResult.ramification        = "Various image format transformations will be unavailable";
            testResult.remedy              = "Install ImageMagick v" + MINIMUM_IMAGE_MAGICK_VERSION_STR + " or greater and either ensure it is in the PATH or configure Alfresco to point to the fully qualified path of the ImageMagick executable ('convert' on Unix, 'imconvert.exe' on Windows)";
            testResult.urisMoreInformation = IMAGE_MAGICK_DOWNLOAD_URI_AND_ALFRESCO_IM_DOCS;
            testResult.rootCause           = e;
        }
        
        endTest(callback, testResult);
        
        return(result);
    }
    
    
    private void validateImageMagickVersion(final ValidatorCallback callback, final String imageMagickHelpOutput)
    {
        startTest(callback, "ImageMagick Version");
        
        TestResult testResult = new TestResult();

        Pair       parsedVersion = parseVersionString(IMAGE_MAGICK_VERSION_REGEX, imageMagickHelpOutput, 1, 2);
        String     versionStr    = (String)parsedVersion.getFirst();
        BigDecimal version       = (BigDecimal)parsedVersion.getSecond();
        
        if (versionStr == null)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine ImageMagick version";
            testResult.remedy       = "Manually validate that ImageMagick v" + MINIMUM_IMAGE_MAGICK_VERSION_STR + " or greater is installed by running either the 'convert -version' (Unix) or 'imconvert -version' (Windows) commands";
        }
        else
        {
            progress(callback, versionStr);
            
            if (version != null && MINIMUM_IMAGE_MAGICK_VERSION.compareTo(version) <= 0)
            {
                testResult.resultType = TestResult.PASS;
            }
            else
            {
                testResult.resultType          = TestResult.WARN;  // Only a warning since Alfresco seems to work ok on most versions of ImageMagick
                testResult.errorMessage        = "Alfresco requires ImageMagick v" + MINIMUM_IMAGE_MAGICK_VERSION_STR + " or greater";
                testResult.ramification        = "While Alfresco will start correctly, various image transformations may not function correctly";
                testResult.remedy              = "Install ImageMagick v" + MINIMUM_IMAGE_MAGICK_VERSION_STR + " or greater";
                testResult.urisMoreInformation = IMAGE_MAGICK_DOWNLOAD_URIS;
            }
        }
        
        endTest(callback, testResult);
    }
    
    
    private void validateSwfTools(final ValidatorCallback callback)
    {
        String pdf2SwfOutput = validateCanForkSwfTools(callback);
        
        if (pdf2SwfOutput != null)
        {
            validateSwfToolsVersion(callback, pdf2SwfOutput);
        }
    }
    
    
    private String validateCanForkSwfTools(final ValidatorCallback callback)
    {
        startTest(callback, "Can fork pdf2swf");
        
        TestResult testResult = new TestResult();
        String     result     = null;
        
        try
        {
            result = executeCommandAndGrabStdout(OS_COMMAND_PDF2SWF);
            
            progress(callback, "yes");
            testResult.resultType = TestResult.PASS;
        }
        catch (final Exception e)
        {
            progress(callback, "no");
            
            testResult.resultType          = TestResult.WARN;
            testResult.errorMessage        = "Unable to fork pdf2swf executable";
            testResult.ramification        = "Various document format transformations will be unavailable";
            testResult.remedy              = "Install SWFTools and either ensure it is in the PATH or configure Alfresco to point to the fully qualified path of the 'pdf2swf' executable";
            testResult.urisMoreInformation = PDF2SWF_DOWNLOAD_URI_AND_ALFRESCO_SWFTOOLS_DOCS;
            testResult.rootCause           = e;
        }
        
        endTest(callback, testResult);
        
        return(result);
    }
    
    
    private void validateSwfToolsVersion(final ValidatorCallback callback, final String pdf2SwfOutput)
    {
        startTest(callback, "pdf2swf Version");
        
        TestResult testResult = new TestResult();
        
        Pair       parsedVersion = parseVersionString(PDF2SWF_VERSION_REGEX, pdf2SwfOutput, 1, 2);
        String     versionStr    = (String)parsedVersion.getFirst();
        BigDecimal version       = (BigDecimal)parsedVersion.getSecond();
        
        if (versionStr == null)
        {
            progress(callback, "unknown");
            
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine pdf2swf version";
            testResult.remedy       = "Manually validate that pdf2swf v" + MINIMUM_PDF2SWF_VERSION_STR + " or greater is installed by running the 'pdf2swf -V' command";
        }
        else
        {
            progress(callback, versionStr);
            
            if (version != null && MINIMUM_PDF2SWF_VERSION.compareTo(version) <= 0)
            {
                testResult.resultType = TestResult.PASS;
            }
            else
            {
                testResult.resultType          = TestResult.FAIL;
                testResult.errorMessage        = "Alfresco requires pdf2swf v" + MINIMUM_PDF2SWF_VERSION_STR + " or greater";
                testResult.ramification        = "While Alfresco will start correctly, Share document previews and various transformations will not function correctly";
                testResult.remedy              = "Install pdf2swf v" + MINIMUM_PDF2SWF_VERSION_STR + " or greater";
                testResult.urisMoreInformation = PDF2SWF_DOWNLOAD_URIS;
            }
        }
        
        endTest(callback, testResult);
    }

    
    private Pair parseVersionString(final Pattern regex,
                                    final String  programOutput,
                                    final int     versionStrGroupNo,
                                    final int     versionDecGroupNo)
    {
        Pair       result     = null;
        String     versionStr = null;
        BigDecimal version    = null;
        
        Matcher matcher = regex.matcher(programOutput);
        
        if (matcher.find())
        {
            versionStr = matcher.group(versionStrGroupNo);
            
            String versionDecStr = matcher.group(versionDecGroupNo);
            
            if (versionDecStr != null)
            {
                try
                {
                    version = new BigDecimal(matcher.group(versionDecGroupNo));
                }
                catch (final NumberFormatException nfe)
                {
                    version = null;
                }
            }
        }
        
        result = new Pair(versionStr, version);
        
        return(result);
    }
    
}
