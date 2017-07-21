package org.tensorflow.demo;

import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by Xukan on 3/25/17.
 */

public class AppendLog {
    public static void Log(String text)
    {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        Calendar c = Calendar.getInstance(tz);
        String time = String.format("%02d", c.get(Calendar.MONTH) + 1) + "/"
                + String.format("%02d",c.get(Calendar.DAY_OF_MONTH))+ "/ "
                + String.format("%02d",c.get(Calendar.HOUR_OF_DAY)) + ":"
                + String.format("%02d", c.get(Calendar.MINUTE)) + ":"
                + String.format("%02d", c.get(Calendar.SECOND));
        File logFile = new File("sdcard/ObgDetectlog.file");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                Log.v("ProcessingTimeMs","filefailed");
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(time + "   ");
            buf.append(SystemClock.uptimeMillis() + "   ");
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

