package org.alfresco.extension.environment.validation.validators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Random;

import org.alfresco.extension.environment.validation.AbstractValidator;
import org.alfresco.extension.environment.validation.TestResult;
import org.alfresco.extension.environment.validation.ValidatorCallback;

/**
 * The aim of this validator is to evaluate the speed of disk where indexes will be stored
 * 
 * @author Philippe
 */
public class IndexDiskSpeedValidator extends AbstractValidator
{
    private final static String VALIDATION_TOPIC = "speed of disk containing index";

    // Parameters to this validator
    public final static String PARAMETER_DISK_LOCATION = VALIDATION_TOPIC + ".index.location";

    /**
     * @see org.alfresco.extension.environment.validation.Validator#validate(java.util.Map,
     *      org.alfresco.extension.environment.validation.ValidatorCallback)
     */
    public void validate(final Map parameters, final ValidatorCallback callback)
    {
        newTopic(callback, VALIDATION_TOPIC);

        validateSeekReadWriteSpeed(parameters, callback);

        // validateOsAndVersion(callback);
        // validateArchitecture(callback);
        // validateFileDescriptorLimit(callback);
    }

    private void validateSeekReadWriteSpeed(final Map parameters, final ValidatorCallback callback)
    {
        startTest(callback, "VALIDATE INDEX DISK SPEED");
        long speed= -1;
        String indexLocation = (String)parameters.get(PARAMETER_DISK_LOCATION);
        try
        {

            String content = "A";
            //File file = new File("/media/Elements/test/test.txt");
            //File file = new File("/media/WD500/test/test.txt");
            //File file = new File("/home/philippe/evt/test.txt");
            File file = new File(indexLocation + "/test.txt");

            // if file doesn't exists, then create it
            if (!file.exists())
            {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            // write test file of 1GB
            for (int pos = 0; pos < 1024 * 1024 * 1024; pos++)
            {
                bw.write(content);
                if (pos % (1024 * 1024 * 100) == 0)
                    progress(callback, ".");
            }

            bw.close();
            long startTime = System.currentTimeMillis();
            //meter speed to perform 1024*1024 read write speed
            Random randomGenerator = new Random();
            RandomAccessFile fileRan = new RandomAccessFile(file.getAbsolutePath(), "rw");
            for (int i = 0; i < 1024*256; i++)
            {
                long posorig = randomGenerator.nextInt(1024 * 1024 * 1024);
                fileRan.seek(posorig);
                //read
                int aByte = fileRan.read();
                long posdest = randomGenerator.nextInt(1024 * 1024 * 1024);
                fileRan.seek(posdest); 
                aByte = 'b';
                fileRan.write(aByte);
            }

            fileRan.close();
            long endTime = System.currentTimeMillis();
            
            speed = (endTime - startTime);
            // delete testing file
            file.delete();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        TestResult testResult = new TestResult();
        if( (speed > 0) && (speed < 10000))
        {
            testResult.resultType = testResult.PASS;
            testResult.errorMessage = "Seek time is good! Value:" + speed;
        }
        
        if( (speed > 10000) && (speed < 21000))
        {
            testResult.resultType = testResult.INFO;
            testResult.errorMessage = "Seek time is normal! Value:" + speed;
        }
        
        if( (speed > 21000) && (speed < 50000))
        {
            testResult.resultType = testResult.WARN;
            testResult.errorMessage = "Seek time is abnormaly slow! Value:" + speed;
        }
        
        if( (speed > 50000))
        {
            testResult.resultType = testResult.FAIL;
            testResult.errorMessage = "Seek time is too slow! Value:" + speed;
        }
        


        endTest(callback, testResult);

    }
}
