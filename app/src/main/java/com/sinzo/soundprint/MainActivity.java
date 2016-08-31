package com.sinzo.soundprint;
import android.app.Activity;
import android.os.Bundle;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
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
import java.util.Objects;

public class MainActivity extends AppCompatActivity {


    private MediaRecorder mRecorder = new MediaRecorder();
    int recFlag = 0;//录音标志
    int startFlag = 0;//测量开始标志
    int Maxpoint = 100;//每页的最大显示点
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
        setContentView(R.layout.activity_main);
        final Button Record = (Button)findViewById(R.id.btRecord);
        final Button DbMeasure = (Button)findViewById(R.id.btStDb);
        final Button CaTest = (Button)findViewById(R.id.btTest);
        final FileUtil ft= new FileUtil();

        mLineChart = (LineChart)findViewById(R.id.chart);
        LineData mLineData =getLineData(36,100);
        showChart(mLineChart, mLineData, Color.rgb(114, 188, 223));

        //---------------------------------按钮的监听处理
        CaTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                addEntryme(mLineChart);
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
                while(startFlag == 1) {
                    handler.post(runnable); //加入到消息队列
                    try {
                        sleep(100); //更新时间
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
        ft.setTextview((TextView)findViewById(R.id.tvDbNumber));//用于实时显示噪音值的textview控件

        Record.setOnClickListener(new View.OnClickListener(){
            @Override
            public  void onClick(View v)   {
                //When I touch the button
                TextView Infomation = (TextView)findViewById(R.id.lbInfor);
                if(recFlag == 0)
                {
                Infomation.setText("开始录音");
                    record();
                    Record.setText("停止录音");
                    recFlag = 1;
                }
                else if(recFlag == 1)
                {
                    recordstop();
                    Infomation.setText("保存的文件名为： "+filename);
                    Record.setText("开始录音");
                    recFlag = 0;

                }


            }
        });
      DbMeasure.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v){
              TextView Infomation = (TextView)findViewById(R.id.lbInfor);
                   if(startFlag ==0)
                    {
                    ft.getNoiseLevel();
                    DbMeasure.setText("正在测量环境噪音");
                    Infomation.setText("当前显示为每页"+Maxpoint+"点,"+"采样速率为"+1000/ft.TimeperS+"次每秒");
                    refreshT.start();//图表绘制线程
                    startFlag = 1;
                }
            else  if(startFlag ==1)
              {
                  DbMeasure.setText("噪音测量已暂停");
                  startFlag = 0;
              }
          }

        });
    }

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
    private  void recordstop()
    {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

    }
    private String newFileName()//以日期为名字
    {
        String result = "";
        SimpleDateFormat formatter=new SimpleDateFormat("yyyyMMddHHmmss");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        result= formatter.format(curDate);
        return result;
    }
    public void showChart(LineChart lineChart, LineData lineData, int color) {
        lineChart.setDrawBorders(false);  //是否在折线图上添加边框

        // no description text
        lineChart.setDescription("");// 数据描述
        // 如果没有数据的时候，会显示这个，类似listview的emtpyview
        lineChart.setNoDataTextDescription("You need to provide data for the chart.");

        // enable / disable grid background
        lineChart.setDrawGridBackground(false); // 是否显示表格颜色
        lineChart.setGridBackgroundColor(0x70FFFFFF); // 表格的的颜色，在这里是是给颜色设置一个透明度

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

    public LineData getLineData(int count, float range) {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 1));
        entries.add(new Entry(1, 2));
        entries.add(new Entry(2, 3));
        entries.add(new Entry(3, 4));
        entries.add(new Entry(4, 5));
        entries.add(new Entry(5, 6));

        LineDataSet dataSet = new LineDataSet(entries, "# of Calls");
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

    private  void addEntryme(LineChart lineChart,float y)
    {
        Entry entry = new Entry(cout,y);
        cout++;
        LineData data = lineChart.getData();
        LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
        data.addEntry(entry,0);
        lineChart.notifyDataSetChanged();
        lineChart.setVisibleXRangeMaximum(Maxpoint);
        lineChart.moveViewToX(data.getEntryCount() - Maxpoint);
    }
    private void setMaxpoint(int num)
    {
        Maxpoint  = num;
    }
}
