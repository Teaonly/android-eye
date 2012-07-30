package teaonly.droideye;
import teaonly.droideye.*;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.System;
import java.lang.Thread;
import java.util.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.res.Resources;
import android.content.res.AssetManager;
import android.content.res.AssetFileDescriptor;
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
import android.text.format.Formatter;
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
    private static final String TAG = "TEAONLY";
    private enum AppState{
        IDLE, STREAMING 
    }

    AppState appState = AppState.IDLE;
    TeaServer webServer = null;
    private CameraView cameraView_;
    private OverlayView overlayView_;
    private Button btnExit;
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

        btnExit = (Button)findViewById(R.id.btn_exit);
        btnExit.setOnClickListener(exitAction);
        tvMessage = (TextView)findViewById(R.id.tv_message);
        
        initCamera();
    }
    
    @Override
    public void onCameraReady() {
        initWebServer();
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
        webServer.stop();
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
    
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && inetAddress.isSiteLocalAddress() ) {
                        String ipAddr = inetAddress.getHostAddress();
                        return ipAddr;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.d(TAG, ex.toString());
        }
        return null;
    }   

    private void initWebServer() {
        String ipAddr = getLocalIpAddress();
        if ( ipAddr != null ) {
            try{
                webServer = new TeaServer(8080, this); 
                webServer.registerCGI("/cgi/query", doQuery);
                webServer.registerCGI("/cgi/status", doStatus);
                webServer.registerCGI("/cgi/start", doPlay);
                webServer.registerCGI("/cgi/stop", doStop);
                webServer.registerCGI("/stream/capture", doCapture);
            }catch (IOException e){
                webServer = null;
            }
        }
        if ( webServer != null) {
            tvMessage.setText( getString(R.string.msg_access) + " http://" + ipAddr  + ":8080" );
        } else {
            tvMessage.setText( getString(R.string.msg_error) );
        }
    }

    private OnClickListener exitAction = new OnClickListener() {
        @Override
        public void onClick(View v) {
        
        }   
    };
   
    private PreviewCallback previewCb_ = new PreviewCallback() {
        public void onPreviewFrame(byte[] frame, Camera c) { 
            Log.d(TAG,">>>>>New Frame>>>>>"); 
        }    
    };
     
    private TeaServer.CommonGatewayInterface doQuery = new TeaServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            List<Camera.Size> supportSize =  cameraView_.getSupportedPreviewSize();                             
            String ret = "";
            for(int i = 0; i < supportSize.size() - 1; i++) {
                ret = ret + "" + supportSize.get(i).width + "x" + supportSize.get(i).height + "|";
            }
            int i = supportSize.size() - 1;
            ret = ret + "" + supportSize.get(i).width + "x" + supportSize.get(i).height ;
            return ret;
        }   
        
        @Override 
        public InputStream streaming(Properties parms) {
            return null;
        }    
    }; 

    private TeaServer.CommonGatewayInterface doStatus = new TeaServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            String ret;
            if ( appState == AppState.IDLE) {
                ret = "idle";
            } else {
                ret = "streaming";
            } 
            return ret;
        } 
        
        @Override 
        public InputStream streaming(Properties parms) {
            return null;
        }    
    }; 
 
    private TeaServer.CommonGatewayInterface doPlay = new TeaServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            if ( appState == AppState.IDLE) {
                int wid = Integer.parseInt(parms.getProperty("wid")); 
                int hei = Integer.parseInt(parms.getProperty("hei"));
                cameraView_.setupCamera(wid, hei); 
                cameraView_.SetPreview( previewCb_ );
                appState = AppState.STREAMING;
                return "OK";
            } else {
                return "BUSY";
            }
        }   
        
        @Override 
        public InputStream streaming(Properties parms) {
            return null;
        }    
    }; 
 
    private TeaServer.CommonGatewayInterface doStop = new TeaServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            return " ";
        }   
        
        @Override 
        public InputStream streaming(Properties parms) {
            return null;
        }
    }; 
 
    private TeaServer.CommonGatewayInterface doCapture = new TeaServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            return null;
        }   
        
        @Override 
        public InputStream streaming(Properties parms) {
            return null;
        }
    }; 
 
}
