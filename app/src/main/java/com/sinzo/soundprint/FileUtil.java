package com.sinzo.soundprint;

import android.os.Environment;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

/**
 * Created by Farell on 2016/8/20.
 */

public class FileUtil {
    public static final String LOCAL = "Test";
    public static final String LOCAL_PATH = Environment.getExternalStorageDirectory().getPath() + File.separator;
    /**
     * 录音文件目录
     */
    public static final String REC_PATH = LOCAL_PATH + LOCAL + File.separator;
    private static final String TAG = "AudioRecord";
    static final int SAMPLE_RATE_IN_HZ = 8000;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord mAudioRecord;// 进行类的定义
    boolean isGetVoiceRun;
    Object mLock;
    public int TimeperS = 10;
    private int minDb = 30,maxDb = 30;
    public int DbOffset = 0;//修正偏移量


    /**
     * 自动在SD卡创建相关的目录
     */
    static {
        File dirRootFile = new File(LOCAL_PATH);
        if (!dirRootFile.exists()) {
            dirRootFile.mkdirs();
        }
        File recFile = new File(REC_PATH);
        if (!recFile.exists()) {
            recFile.mkdirs();
        }
    }

    /**
     * 判断是否存在存储空间   *
     *
     * @return
     */
    public FileUtil()
    {
        mLock = new Object();
    }
    public void setTimeperS(int time)
    {
         TimeperS = time;
    }
     private int curDb= 0;//当前的噪音值
    public  int getcurDb()
    {
        return  curDb;
    }
     public TextView textView;
    public TextView txMAXDb;
    public TextView txMINDb;
    public void setTextview(TextView tx)
    {
       textView  = tx;
    }
    public void setTextviewMAXDb(TextView tx)
    {
        txMAXDb  = tx;
    }
    public void setTextviewMINDb(TextView tx)
    {
        txMINDb  = tx;
    }
//------------------------多线程部分，负责控制控件显示噪音值-----------------//
    final Handler handler= new Handler();//创造句柄
            final Runnable runnable = new Runnable() {
        public void run() {
            textView.setText(Integer.toString(curDb));
            txMINDb.setText(Integer.toString(minDb));
            txMAXDb.setText(Integer.toString(maxDb));
            }
        };
    final Thread refreshT = new Thread(){
        //public boolean isrun=true;
                @Override
        public void run() {
                while(true) {
                    handler.post(runnable); //加入到消息队列
                    try {
                        sleep(100); //更新时间
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                }
        };
//------------------------------------------------------------------------//


    public void refreshText()
    {
        
      //  
    }
    public void getNoiseLevel()
    {

        if (isGetVoiceRun) {
            Log.e(TAG, "正在记录中");
            return;
        }
        refreshT.start();//更新记录
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        if (mAudioRecord == null) {
            Log.e("sound", "mAudioRecord初始化失败");
        }
        isGetVoiceRun = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                while (isGetVoiceRun) {
                    //r是实际读取的数据长度，一般而言r会小于buffersize
                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    // 将 buffer 内容取出，进行平方和运算
                    for (int i = 0; i < buffer.length; i++) {
                        v += buffer[i] * buffer[i];
                    }
                    // 平方和除以数据总长度，得到音量大小。
                    double mean = v / (double) r;
                    double volume = 10 * Math.log10(mean);
                    Log.d(TAG, "分贝值:" + volume);
                    if(volume > 10)
                    curDb = (int)volume + DbOffset;//转换为float防止输出过多
                    if(curDb > maxDb )     maxDb = curDb;
                    else if(curDb <minDb)  minDb = curDb;
                    // 大概一秒十次
                    synchronized (mLock) {
                        try {
                            mLock.wait(1000 / TimeperS);//等待Timpers毫秒
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }).start();
    }
    public static boolean isExitSDCard() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    private static boolean hasFile(String fileName) {
        File f = createFile(fileName);
        return null != f && f.exists();
    }

    public static File createFile(String fileName) {

        File myCaptureFile = new File(REC_PATH + fileName);
        if (myCaptureFile.exists()) {
            myCaptureFile.delete();
        }
        try {
            myCaptureFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myCaptureFile;
    }


}