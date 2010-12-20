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

package org.alfresco.extension.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * This class provides an easy and safe way to execute an external process (shell script, CLI program, etc.),
 * capturing not only the exit code but also any output that process writes to stdout or stderr.
 *
 * @author Peter Monks (peter.monks@alfresco.com)
 * @version $Id$
 */
public class ProcessInvoker
{
    private final long MAXIMUM_WAIT_TIME = 10000;  // 10 seconds
    private final long POLL_INTERVAL     = 250;    // 1/4 second
    
    
    
    /**
     * Executes (forks) an external process.  Note that this won't invoke built-in commands (such as "dir" on Windows or "ulimit" on Linux).
     * 
     * @param commandAndParameters The command to execute (at index 0) along with the list of parameters <i>(may not be null or empty)</i>.
     * @return The status code, stdout and stderr returned by the process.  Note that extremely large outputs should be avoided, since they can overflow the heap.
     */
    public Triple execute(final String[] commandAndParameters)
        throws IOException,
               InterruptedException
    {
        return(execute(commandAndParameters, MAXIMUM_WAIT_TIME));
    }
    
    
    /**
     * Executes (forks) an external process.  Note that this won't invoke built-in commands (such as "dir" on Windows or "ulimit" on Linux).
     * 
     * @param commandAndParameters The command to execute (at index 0) along with the list of parameters <i>(may not be null or empty)</i>.
     * @param waitTime             The maximum time to wait (in ms) for the command to complete <i>(must be > 0)</i>.
     * @return The status code, stdout and stderr returned by the process.  Note that extremely large outputs should be avoided, since they can overflow the heap.
     */
    public Triple execute(final String[] commandAndParameters, final long waitTime)
        throws IOException,
               InterruptedException
    {
        Triple  result  = null;
        final Process process = Runtime.getRuntime().exec(commandAndParameters);
        
        StreamCatcher stdoutCatcher = new StreamCatcher(process.getInputStream());
        StreamCatcher stderrCatcher = new StreamCatcher(process.getErrorStream());
        
        stdoutCatcher.setDaemon(true);
        stdoutCatcher.setName("stdoutCaptureThread");
            
        stderrCatcher.setDaemon(true);
        stderrCatcher.setName("stderrCaptureThread");
            
        stdoutCatcher.start();
        stderrCatcher.start();
        
        // Give the process waitTime milliseconds to do its thing
        int     exitCode  = -1;
        long    waitUntil = System.currentTimeMillis() + (waitTime < 0 ? 0 : waitTime);
        boolean done      = false;
        
        while (!done && System.currentTimeMillis() < waitUntil)
        {
            try
            {
                exitCode = process.exitValue();
                done     = true; // Note: we don't reach here unless the process terminated normally
            }
            catch (final IllegalThreadStateException itse)
            {
                Thread.sleep(POLL_INTERVAL);  // Sleep before polling the process again
            }
        }
        
        if (!done)
        {
            // hmmm......the process was still running so attempt to whack it
            process.destroy();  // Note: with OpenOffice running in headless mode on Linux, this doesn't appear to actually kill the process
        }
        
        stdoutCatcher.interrupt();
        stderrCatcher.interrupt();
        
        result = new Triple(new Integer(exitCode), stdoutCatcher.getOutput(), stderrCatcher.getOutput());
        
        return(result);
    }
    
    
    /**
     * Private class that captures an InputStream on a background thread.  
     */
    private class StreamCatcher
        extends Thread
    {
        private final InputStream  inputStream;
        private final StringBuffer output;
        
        
        StreamCatcher(final InputStream inputStream)
        {
            this.inputStream = inputStream;
            this.output      = new StringBuffer();
        }
        
        
        public void run()
        {
            try
            {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String         line   = null;
                
                synchronized(output)
                {
                    while ((line = reader.readLine()) != null && !interrupted())
                    {
                        output.append(line);
                        output.append("\n");
                    }
                }
            }
            catch (final IOException ioe)
            {
                // Swallow the exception and move on
            }
        }
        
        public String getOutput()
        {
            synchronized(output)
            {
                return(output.toString().trim());
            }
        }
    }
        

}
