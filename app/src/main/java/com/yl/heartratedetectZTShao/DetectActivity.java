package com.yl.heartratedetectZTShao;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;


import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Vector;

/**
 * Created by 邵大帅 on 2018/4/8.
 */

public class DetectActivity extends AppCompatActivity implements
        CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private CameraBridgeViewBase cameraView;
    private CascadeClassifier classifier;
    private Mat mGray;
    private Mat mRgba;
    private UIHandler progressbar;
    private clockHandler clock;
    private ProgressBar process;
    Button timeCount;

    private int mAbsoluteFaceSize = 0;
    static private boolean isFrontCamera = true;

    public int T = 10; //倒计时时长

    public  int facenum=0;
    public  static Vector<Mat> outFaces=new Vector();
    public  static Vector<Mat> outFaces2=new Vector<>();
    private  Vector <Long> time=new Vector<>();

    static {
        System.loadLibrary("opencv_java3");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initWindowSettings();
        setContentView(R.layout.activity_detect);
        cameraView = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);//默认前置
        initClassifier();
        cameraView.enableView();


        process=(ProgressBar)findViewById(R.id.process);
        ImageView black=(ImageView)findViewById(R.id.black);

        black.setVisibility(View.VISIBLE);
        Button switchCamera = (Button) findViewById(R.id.switch_camera);
        switchCamera.setOnClickListener(this);

        timeCount = (Button) findViewById(R.id.time_count);
//        int color=Color.parseColor("#555555");
        timeCount.setBackgroundColor(getResources().getColor(R.color.colorBackground));
        timeCount.setOnClickListener(this);
        progressbar=new UIHandler();
        clock=new clockHandler();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        outState.putString("URL","ddd");
        super.onSaveInstanceState(outState);
    }


    private class UIHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            process.setVisibility(View.VISIBLE);
        }
    }


class MyCountDownTimer implements Runnable {
    @Override
    public void run() {
        while (T > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            T--;
            Message msg = new Message();
            DetectActivity.this.clock.sendMessage(msg);
        }
        int[] timeseries;
        timeseries = time2Timeseries(time);
        Intent intent = new Intent();
        intent.setClass(DetectActivity.this, HeartRateCalculation.class);
        intent.putExtra("facenum", facenum);
        intent.putExtra("timeseries", timeseries);
        DetectActivity.this.startActivity(intent);
    }
}
    public void implicitIntent(String URL){
        Uri uri = Uri.parse(URL);
        Intent implicitGo = new Intent(Intent.ACTION_VIEW,uri);
        if(implicitGo.resolveActivity(getPackageManager())!=null) startActivity(implicitGo);
    }

    class DownloadTask extends AsyncTask<Integer, Integer, String>{

        //后面尖括号内分别是参数（例子里是线程休息时间），进度(publishProgress用到)，返回值 类型
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProgressDialog startToShow=new ProgressDialog(DetectActivity.this);
            startToShow.show(DetectActivity.this,"开始加载","已经加载了:");

        }

        @Override
        protected String doInBackground(Integer... params) {
            //第二个执行方法,onPreExecute()执行完后执行
            for(int i=0;i<=100;i++){
                publishProgress(i);
                try {
                    Thread.sleep(params[0]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                publishProgress(i);
            }
            return "执行完毕";
        }
        @Override
        protected void onProgressUpdate(Integer... progress) {
            timeCount.setText(progress[0]+"%");
            super.onProgressUpdate(progress);
        }
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(DetectActivity.this, "你打不开我哈哈", Toast.LENGTH_SHORT).show();
            super.onPostExecute(result);
        }
    }
private class clockHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            timeCount.setText("还剩"+T+"秒");
            this.getLooper().getQueue();
        }
    }

public int[] time2Timeseries(Vector<Long> time){
    int []timeseries=new int[facenum];
    for(int i=0;i<facenum;i++){
        timeseries[i]=(int)(time.get(i)-time.get(0));
    }
    return timeseries;
}

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switch_camera:
                cameraView.disableView();
                if (isFrontCamera) {
                    cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                    isFrontCamera = false;
                } else {
                    cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                    isFrontCamera = true;
                }
                cameraView.enableView();
                break;

                case R.id.time_count:
                    cameraView.setCvCameraViewListener(this); // 设置相机监
                    Message msg= new Message();
                    DetectActivity.this.progressbar.sendMessage(msg);
                    new Thread(new MyCountDownTimer()).start();
                    new DownloadTask().execute();
                    break;
            }
        }

        public void useSharedPeference(){
            SharedPreferences sharedPreferences = getSharedPreferences("UserInfo", Context.MODE_PRIVATE); //name 是保存的xml的文件名
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("name", new String("1231"));
            editor.putString("password", new String("123"));
            editor.commit();
        }

        public String[] accessSharedPeference(){
            SharedPreferences sharedPreferences = getSharedPreferences("UserInfo", Context.MODE_PRIVATE);
            String[] nameAndPassword = new String[2];
            nameAndPassword[0] = sharedPreferences.getString("name","");
            nameAndPassword[1] = sharedPreferences.getString("passwork","");
            return nameAndPassword;
        }


    // 初始化窗口设置, 包括全屏、横屏、常亮
    private void initWindowSettings() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);//portrait
    }

    // 初始化人脸级联分类器，必须先初始化
    private void initClassifier() {
        try {
            InputStream is = getResources()
                    .openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            classifier = new CascadeClassifier(cascadeFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    @Override
    // 这里执行人脸检测的逻辑, 根据OpenCV提供的例子实现(face-detection)
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        // 翻转矩阵以适配前后置摄像头
        if (isFrontCamera) {
            Core.flip(mRgba, mRgba, 1);
            Core.flip(mGray, mGray, 1);
        }
        float mRelativeFaceSize = 0.2f;
        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();//rows:行数，cols:列数。
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }
        MatOfRect faces = new MatOfRect();

        if (classifier != null)
            classifier.detectMultiScale(mGray, faces, 1.1, 4, 2,
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());//检测

        Rect[] facesArray = faces.toArray();


        Scalar faceRectColor = new Scalar(50, 50, 50, 200);//框颜色

        for (Rect faceRect : facesArray)
        {
            Imgproc.rectangle(mRgba, faceRect.tl(), faceRect.br(), faceRectColor, 10);//全脸
            Rect cheek=new Rect(faceRect.leftcheekstart(), faceRect.leftcheekend());
            outFaces.add(new Mat(mRgba,cheek));
            Rect rightcheek = new Rect(faceRect.rightcheekstart(), faceRect.rightcheekend());
            outFaces2.add(new Mat(mRgba,rightcheek));
            time.add(System.currentTimeMillis());
            facenum++;

        }
        return mRgba;

        }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraView != null) {
            cameraView.disableView();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.disableView();
    }



}
