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

import java.util.Map;

import org.hyperic.sigar.Mem;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.CpuInfo;

import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.ValidatorCallback;


/**
 * This class validates that the OS and its configuration are suitable for Alfresco.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class ServerHardwareValidator
    extends AbstractValidator
{
    private final static String VALIDATION_TOPIC = "Server Hardware";
    
    private final static int RECOMMENDED_CPU_SPEED_MHZ = 2500;
    private final static int MINIMUM_CPU_SPEED_MHZ     = 1200;
    private final static int MINIMUM_CORE_COUNT        = 2;
    private final static int RECOMMENDED_RAM_MB        = 2048;
    private final static int MIMIMUM_RAM_MB            = 512;
    
    
    /**
     * @see org.alfresco.extension.environment.validation.Validator#validate(java.util.Map, org.alfresco.extension.environment.validation.ValidatorCallback)
     */
    public void validate(final Map parameters, final ValidatorCallback callback)
    {
        newTopic(callback, VALIDATION_TOPIC);
        
        validateCpuSpeed(callback);
        validateCpuCount(callback);
        validateRam(callback);
    }


    
    private void validateCpuSpeed(final ValidatorCallback callback)
    {
        startTest(callback, "CPU Clock Speed");
        
        TestResult testResult = new TestResult();
        CpuInfo[]  infoOnCpus = null;
        
        try
        {
            infoOnCpus = sigar.getCpuInfoList();
        }
        catch (final SigarException se)
        {
            // Ignore it and move on
        }
            
        int cpuClockSpeed = Integer.MAX_VALUE;
        
        // Loop through and find the *slowest* clock speed of any of the cores - that's the one we'll use for validation
        if (infoOnCpus != null)
        {
            for (int i = 0; i < infoOnCpus.length; i++)
            {
                if (cpuClockSpeed > infoOnCpus[i].getMhz())
                {
                    cpuClockSpeed = infoOnCpus[i].getMhz();
                }
            }
        }
        
        if (cpuClockSpeed != Integer.MAX_VALUE)
        {
            progress(callback, cpuClockSpeed + "Mhz");
            
            if (cpuClockSpeed >= RECOMMENDED_CPU_SPEED_MHZ)
            {
                testResult.resultType = TestResult.PASS;
            }
            else if (cpuClockSpeed >= MINIMUM_CPU_SPEED_MHZ)
            {
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "CPU clock speed of " + cpuClockSpeed + "Mhz is slower than that recommended for test or production use (" + RECOMMENDED_CPU_SPEED_MHZ + "Mhz)";
                testResult.ramification = "Alfresco will perform well for development purposes but this server should not be used for other purposes";
                testResult.remedy       = "Upgrade the server to one with more modern CPUs (CPU clock speed of at least " + RECOMMENDED_CPU_SPEED_MHZ + "Mhz)";
            }
            else
            {
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = "CPU clock speed of " + cpuClockSpeed + "Mhz is slower than the minimum required by Alfresco (" + MINIMUM_CPU_SPEED_MHZ + "Mhz)";
                testResult.ramification = "Alfresco will not perform well";
                testResult.remedy       = "Upgrade the server to one with more modern CPUs (CPU clock speed of at least " + MINIMUM_CPU_SPEED_MHZ + "Mhz, and preferably " + RECOMMENDED_CPU_SPEED_MHZ + "Mhz or more)";
            }
        }
        else
        {
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine CPU clock speed";
            testResult.ramification = "This machine may not be fast enough to run Alfresco for test or production purposes";
            testResult.remedy       = "Manually validate that the CPU clock speed of this server is at least " + MINIMUM_CPU_SPEED_MHZ + "Mhz";
        }
        
        endTest(callback, testResult);
    }
    
    
    private void validateCpuCount(final ValidatorCallback callback)
    {
        startTest(callback, "CPU Count");
        
        TestResult testResult = new TestResult();
        CpuInfo[]  infoOnCpus = null;
        
        try
        {
            infoOnCpus = sigar.getCpuInfoList();
        }
        catch (final SigarException se)
        {
            // Ignore it and move on
        }
            
        int cpuSocketCount = -1;
        int cpuCoreCount   = -1;
        
        if (infoOnCpus != null && infoOnCpus.length > 0)
        {
            cpuSocketCount = infoOnCpus[0].getTotalSockets();
            cpuCoreCount   = infoOnCpus[0].getTotalCores();
        }
        
        if (cpuSocketCount != -1 && cpuCoreCount != -1)
        {
            // We don't really care about sockets, but let's print it out just for laughs
            progress(callback, cpuSocketCount + " " + (cpuSocketCount == 1 ? "socket" : "sockets") + ",");
        }
        
        if (cpuCoreCount != -1)
        {
            progress(callback, cpuCoreCount + " " + (cpuCoreCount == 1 ? "core" : "cores"));
            
            if (cpuCoreCount >= MINIMUM_CORE_COUNT)
            {
                testResult.resultType = TestResult.PASS;
            }
            else
            {
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = "This machine does not have enough CPUs (cores) to run Alfresco for non-development purposes";
                testResult.ramification = "Alfresco will function well for development purposes but this server should not be used for any other purpose";
                testResult.remedy       = "Upgrade the server to have at least " + MINIMUM_CORE_COUNT + " CPUs (cores)";
            }
        }
        else
        {
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine CPU (core) count";
            testResult.ramification = "This machine may not have enough CPUs (cores) to run Alfresco for non-development purposes";
            testResult.remedy       = "Manually validate that this machine has at least " + MINIMUM_CORE_COUNT + " CPUs (cores)";
        }
        
        endTest(callback, testResult);
    }
    

    private void validateRam(final ValidatorCallback callback)
    {
        startTest(callback, "Installed RAM");
        
        TestResult testResult = new TestResult();
        Mem        memoryInfo = null;
        
        try
        {
            memoryInfo = sigar.getMem();
        }
        catch (final SigarException se)
        {
            // Ignore it and move on
        }
            
        long installedRam = -1;
        
        // Loop through and find the *slowest* clock speed of any of the cores - that's the one we'll use for validation
        if (memoryInfo != null)
        {
            installedRam = memoryInfo.getRam();
        }
        
        if (installedRam != -1)
        {
            progress(callback, installedRam + "MB");
            
            if (installedRam >= RECOMMENDED_RAM_MB)
            {
                testResult.resultType = TestResult.PASS;
            }
            else if (installedRam >= MIMIMUM_RAM_MB)
            {
                testResult.resultType   = TestResult.WARN;
                testResult.errorMessage = installedRam + "MB of RAM is not enough to run Alfresco for test or production purposes";
                testResult.ramification = "Alfresco will function well for development purposes but this server should not be used for other purposes";
                testResult.remedy       = "Upgrade the server to have at least " + RECOMMENDED_RAM_MB + "MB of RAM";
            }
            else
            {
                testResult.resultType   = TestResult.FAIL;
                testResult.errorMessage = installedRam + "MB of RAM is not enough to run Alfresco for test or production purposes";
                testResult.ramification = "Alfresco may not function correctly and if it does, it will not perform well";
                testResult.remedy       = "Upgrade the server to have at least " + MIMIMUM_RAM_MB + "MB of RAM (preferably at least " + RECOMMENDED_RAM_MB + "MB)";
            }
        }
        else
        {
            testResult.resultType   = TestResult.WARN;
            testResult.errorMessage = "Unable to determine size of installed RAM";
            testResult.ramification = "This machine may not have sufficient RAM to run Alfresco correctly";
            testResult.remedy       = "Manually validate that this server has at least " + MIMIMUM_RAM_MB + "MB (preferably at least " + RECOMMENDED_RAM_MB + "MB) of RAM installed";
        }
        
        endTest(callback, testResult);
    }
    
    
}
