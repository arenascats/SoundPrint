package com.sinzo.soundprint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.IOException;
import    java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {


    private MediaRecorder mRecorder = new MediaRecorder();
    int recFlag = 0;//录音标志
    int startFlag = 0;//测量开始标志
    int Maxpoint = 100;//每页的最大显示点
    int SampleTime = 20;
    boolean Portrait = true;//竖屏

    String filename ;
    static final int SAMPLE_RATE_IN_HZ = 8000;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord mAudioRecord;
    boolean isGetVoiceRun;
    private LineChart mLineChart;
    Object mLock;
    public void AudioRecordDemo()
    {
        //mLock = new Objects();
    }

    protected void onCreate(Bundle savedInstanceState)//创建显示界面时候
    {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏状态栏
         getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                  WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        final Button DbTimeSwitch = (Button)findViewById(R.id.btTimeSwitch);
        final Button DbMeasure = (Button)findViewById(R.id.btStDb);
        final Button ScreenSwitch = (Button)findViewById(R.id.btTest);
        final  Button DbReboot = (Button)findViewById((R.id.btReboot));
        final FileUtil ft= new FileUtil();

        mLineChart = (LineChart)findViewById(R.id.chart);
        LineData mLineData =getLineData(36,100);
        showChart(mLineChart, mLineData, Color.rgb(255,255,255));//传入表格，设置表格的背景颜色

        String phoneName = Build.BRAND;
        if( phoneName.contains("sum") || phoneName.contains("SUM"))
        {
            ft.DbOffset = -20;
        }


        //-----------------------------------------------------------------------------------按钮的监听处理
        ScreenSwitch.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {

                                                ScreenSwitch();//屏幕切换
                                            }
                                        });

                DbReboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                restartApplication();
            }
        });




        final Handler handler= new Handler();//创造句柄
        final Runnable runnable = new Runnable() {
            public void run() {

             addEntryme(mLineChart,ft.getcurDb());
            }
        };
        final Thread refreshT = new Thread(){
            //public boolean isrun=true;
            @Override
            public void run() {

                  while(true) {
                      if (startFlag == 1) handler.post(runnable); //加入到消息队列
                      try {
                          sleep(1000 / ft.TimeperS); //更新时间
                      } catch (InterruptedException e) {
                          return;
                      }
                  }

            }
        };
        ft.setTextview((TextView)findViewById(R.id.tvDbNumber));//用于实时显示噪音值的textview控件
        ft.setTextviewMAXDb((TextView)findViewById(R.id.tvMaxDb));//用于实时显示最大噪音值的textview控件
        ft.setTextviewMINDb((TextView)findViewById(R.id.tvMinDb));//用于实时显示最小噪音值的textview控件


//---------------------------------------------------采样率切换按钮事件
        DbTimeSwitch.setOnClickListener(new View.OnClickListener(){
            @Override
            public  void onClick(View v)   {
                //When I touch the button
                TextView Infomation = (TextView)findViewById(R.id.lbInfor);
                if(SampleTime != 40)
                {
                    SampleTime += 5;//每5进阶
                    Infomation.setText("当前显示为每页"+Maxpoint+"点,"+"采样速率为"+SampleTime+"次每秒");
                }
                else if(SampleTime ==40)
                {
                    SampleTime = 20;
                    Infomation.setText("当前显示为每页"+Maxpoint+"点,"+"采样速率为"+SampleTime+"次每秒");
                }


            }
        });

        //-----------------------------------------------Db测量线程启动和暂停按钮事件
      DbMeasure.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v){
              TextView Infomation = (TextView)findViewById(R.id.lbInfor);
                   if(startFlag ==0)
                    {
                    ft.getNoiseLevel();
                    DbMeasure.setText("正在测量环境噪音");
                        ft.setTimeperS(SampleTime);////---------------------------设置采样速度
                    Infomation.setText("当前显示为每页"+Maxpoint+"点,"+"采样速率为"+ft.TimeperS+"次每秒");
                    refreshT.start();//图表绘制线程
                    startFlag = 1;
                }
            else  if(startFlag ==1)
              {
                  DbMeasure.setText("噪音测量已暂停");
                  startFlag = 2;
              }
              else if(startFlag == 2)
                   {
                       DbMeasure.setText("正在测量环境噪音");
                       Infomation.setText("当前显示为每页"+Maxpoint+"点,"+"采样速率为"+ft.TimeperS+"次每秒");
                       startFlag = 1;
                   }
          }

        });
    }

    //函数：record
    //返回值：无
    //功能：记录音频并保存
    private void record()
    {

        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        filename = newFileName();
        mRecorder.setOutputFile("/storage/emulated/0/Download/"+filename);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRecorder.start();
    }

    //函数：recordstop
    //返回值：无
    //功能：停止录音并保存文件
    private  void recordstop()
    {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

    }

    //函数名：newFileName
    //返回值：字符串
    //功能：返回以日期组合而成的字符串
    private String newFileName()//以日期为名字
    {
        String result = "";
        SimpleDateFormat formatter=new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        result= formatter.format(curDate);
        return result;
    }

    //函数名：showChart
    //输入值：LineChart，LineData，color
    //返回值：无
    //功能：依据设定初始化表格
    public void showChart(LineChart lineChart, LineData lineData, int color) {
        lineChart.setDrawBorders(false);  //是否在折线图上添加边框

        // no description text
        lineChart.setDescription("");// 数据描述
        // 如果没有数据的时候，会显示这个，类似listview的emtpyview
        lineChart.setNoDataTextDescription("You need to provide data for the chart.");

        // enable / disable grid background
        lineChart.setDrawGridBackground(false); // 是否显示表格颜色
        lineChart.setGridBackgroundColor(Color.rgb(0,0,0)); // 表格的的颜色，在这里是是给颜色设置一个透明度

        // enable touch gestures
        lineChart.setTouchEnabled(true); // 设置是否可以触摸

        // enable scaling and dragging
        lineChart.setDragEnabled(true);// 是否可以拖拽
        lineChart.setScaleEnabled(true);// 是否可以缩放

        // if disabled, scaling can be done on x- and y-axis separately
        lineChart.setPinchZoom(false);//

        lineChart.setBackgroundColor(color);// 设置背景

        // add data
        lineChart.setData(lineData); // 设置数据

        // get the legend (only possible after setting data)
        Legend mLegend = lineChart.getLegend(); // 设置比例图标示，就是那个一组y的value的

        // modify the legend ...
        // mLegend.setPosition(LegendPosition.LEFT_OF_CHART);
        mLegend.setForm(Legend.LegendForm.CIRCLE);// 样式
        mLegend.setFormSize(6f);// 字体
        mLegend.setTextColor(Color.WHITE);// 颜色
//      mLegend.setTypeface(mTf);// 字体

        lineChart.animateX(1000); // 立即执行的动画,x轴
    }


    public void ScreenSwitch()
    {
        if(Portrait == true)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//横屏设置
        else if(Portrait == false)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //竖屏设置
    }
    public LineData getLineData(int count, float range) {
        ArrayList<Entry> entries = new ArrayList<>();
//        entries.add(new Entry(0, 15));
//        entries.add(new Entry(1, 20));
//        entries.add(new Entry(2, 25));
//        entries.add(new Entry(3, 30));
//        entries.add(new Entry(4, 35));
//        entries.add(new Entry(5, 40));

        LineDataSet dataSet = new LineDataSet(entries, "Db");
        LineData lineData = new LineData(dataSet);

        return lineData;
    }
    int cout = 6;
    private  void addEntryme(LineChart lineChart)
    {
        float f = (float) ((Math.random()) * 10);
        Entry entry = new Entry(cout,f);
        cout++;
        LineData data = lineChart.getData();
        LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
        data.addEntry(entry,0);
        lineChart.notifyDataSetChanged();
        lineChart.setVisibleXRangeMaximum(100);//最大显示数量
        lineChart.moveViewToX(data.getEntryCount() - 5);
    }
    int discout = 0;//用以移动图表
    private  void addEntryme(LineChart lineChart,float y)//添加点到图表上
    {
        int temp = 0;

        Entry entry = new Entry((cout),y);
        cout++;
        LineData data = lineChart.getData();
        LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
        data.addEntry(entry,0);
        lineChart.notifyDataSetChanged();
        lineChart.setVisibleXRangeMaximum(Maxpoint);
        discout++;

            lineChart.moveViewToX(data.getEntryCount() -5);
            discout =0 ;
    }
    private void setMaxpoint(int num)
    {
        Maxpoint  = num;
    }

    private void restartApplication() {//程序重新启动
        final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

}
