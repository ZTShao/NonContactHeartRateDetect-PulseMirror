package com.yl.heartratedetectZTShao;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import org.opencv.core.Size;

import static com.yl.heartratedetectZTShao.DetectActivity.outFaces;
import static com.yl.heartratedetectZTShao.DetectActivity.outFaces2;

import static java.lang.Math.floor;
import static org.opencv.imgproc.Imgproc.resize;


/**
 * Created by 邵大帅 on 2018/4/9.
 */

public class HeartRateCalculation extends AppCompatActivity implements View.OnClickListener {

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heartrate);
        TextView heartrate=(TextView)findViewById(R.id.heartRate);
        TextView Rate=(TextView)findViewById(R.id.rate);
        Rate.setText("您的心率为：");
        Rate.setTextColor(Color.parseColor("#668B8B"));
        Rate.setTextSize(30);

        Intent deliver = this.getIntent();
        int facenum=deliver.getIntExtra("facenum",0);
        int [] timeseries=deliver.getIntArrayExtra("timeseries");

        double imageLength=timeseries[facenum-1];
        imageLength=imageLength/facenum;
        int rate=(int)(1000/imageLength);


        Size normalizedSize=new Size(outFaces.get(facenum-1).width(),outFaces.get(facenum-1).height());
        for(int i=0;i<outFaces2.size();i++){
            resize(outFaces.get(i),outFaces.get(i),normalizedSize);
            resize(outFaces2.get(i),outFaces2.get(i),normalizedSize);
        }

        VideoProcessor heartRate=new VideoProcessor(rate);
        double pulseFreqLeft=heartRate.colorMagnify(outFaces);
        double pulseFreqRight=heartRate.colorMagnify(outFaces2);

        double HR= 60 * pulseFreqLeft;
        double HR2=60 * pulseFreqRight;
        double averateHR=(HR+HR2)/2;
        int TrueHR=findRate(averateHR);

        heartrate.setText(String.valueOf(TrueHR));
        heartrate.setTextColor(Color.parseColor("#FFFFFF"));
        heartrate.setTextSize(100);
        Button back=(Button)findViewById(R.id.back);
        back.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        outFaces.clear();
        outFaces2.clear();
        Intent intent = new Intent();
        intent.setClass(HeartRateCalculation.this,DetectActivity.class);
        HeartRateCalculation.this.startActivity(intent);
    }

    public int findRate(double HR){
    double floorHR=floor(HR);
    double suffixHR=HR-floorHR;
    int trueHR;
    if(suffixHR<0.5){
        trueHR=(int)floorHR;
    }
    else trueHR=(int)floorHR+1;
    return trueHR;


    }
}
