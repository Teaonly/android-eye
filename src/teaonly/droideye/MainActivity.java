package teaonly.droideye;
import teaonly.droideye.*;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.System;
import java.lang.Thread;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.PictureCallback;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Paint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceView;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity 
    implements View.OnTouchListener, CameraView.CameraReadyCallback, OverlayView.UpdateDoneCallback{

    private CameraView cameraView_;
    private OverlayView overlayView_;
    private Button btnStart;
    private TextView tvMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //NativeAPI.LoadLibraries();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);    
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 

        setContentView(R.layout.main);

        btnStart = (Button)findViewById(R.id.btn_start);
        btnStart.setOnClickListener(startAction);
        tvMessage = (TextView)findViewById(R.id.tv_message);
        tvMessage.setText(getString(R.string.msg_help));
        
        initCamera();
    }
    
    @Override
    public void onCameraReady() {
        int wid = cameraView_.Width();
        int hei = cameraView_.Height();
    }
 
    @Override
    public void onUpdateDone() {
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }   

    @Override
    public void onStart(){
        super.onStart();
    }   

    @Override
    public void onResume(){
        super.onResume();
    }   
    
    @Override
    public void onPause(){  
        super.onPause();
        cameraView_.StopPreview(); 
        cameraView_.Release();
        finish();
    }  
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override 
    public boolean onTouch(View v, MotionEvent evt) {
        return false;
    }
   
    private void initCamera() {
        SurfaceView cameraSurface = (SurfaceView)findViewById(R.id.surface_camera);
        cameraView_ = new CameraView(cameraSurface);        
        cameraView_.setCameraReadyCallback(this);

        overlayView_ = (OverlayView)findViewById(R.id.surface_overlay);
        overlayView_.setOnTouchListener(this);
        overlayView_.setUpdateDoneCallback(this);
    }
    
    private OnClickListener startAction = new OnClickListener() {
        @Override
        public void onClick(View v) {
        
        }   
    };

}
