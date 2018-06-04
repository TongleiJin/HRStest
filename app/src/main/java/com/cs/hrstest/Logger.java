package com.cs.hrstest;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jtl.jintonglei on 2016/6/28 028.
 */
public class Logger {
    private static boolean startFlag = false;
    private static String storePath = null;
    private static String fileName = null;
    private static RandomAccessFile logFile = null;

    public static void start(String logHeader)
    {
        if (!startFlag)
        {
            logHeader += "\n";
            fileName = "hr" + getTimeStamp() + ".txt";
            File path = new File(Environment.getExternalStorageDirectory(), "HRMeasure/");
            if (!path.exists()) {
                path.mkdirs();
            }
            storePath = path.getAbsolutePath() + "/" + fileName;
            startFlag = true;
            //myLog("PATH::" + storePath);
            logFile = openLogFile();
            writeLine(logHeader);
        }
    }

    private static void writeLine(String l)
    {
        try {
            logFile.write(l.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void writeRecord(String line)
    {
        line += "\n";
        writeLine(line);
//        myLog(line);
    }


    private static String getLogPath()
    {
        return storePath;
//        final String LOG_FILE_NAME = "hr_measure/" + fileName;
//        return new File(Environment.getExternalStorageDirectory(), LOG_FILE_NAME).getAbsolutePath();
    }


    private static RandomAccessFile openLogFile()
    {
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(getLogPath(), "rw");
        } catch (FileNotFoundException e) {
            myLog("<> cannot open file: " + getLogPath());
            return null;
        }
        return randomAccessFile;
    }
    
    private void closeLogFile()
    {
        try {
            logFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static String getTimeStamp()
    {
        String stamp = new SimpleDateFormat("yyyyMMdd-hhmmss").format(new Date());
        return stamp;
    }

    private static void myLog(String str)
    {
        Log.i("KTL", "LOG::" + str);
    }

}
