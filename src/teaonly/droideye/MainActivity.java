package teaonly.droideye;

import teaonly.droideye.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.http.conn.util.InetAddressUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.PictureCallback;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.Paint;
import android.graphics.YuvImage;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.util.*;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity
        implements CameraView.CameraReadyCallback {
    public static String TAG="TEAONLY";
    private final int ServerPort = 8080;

    private TeaServer webServer = null;
    private OverlayView overlayView = null;
    private CameraView cameraView = null;
    private AudioRecord audioCapture = null;

    ExecutorService executor = Executors.newFixedThreadPool(3);
    private ReentrantLock previewLock = new ReentrantLock();
    boolean inProcessing = false;

    byte[] yuvFrame = new byte[1920*1280*2];

    //
    //  Activiity's event handler
    //
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // application setting
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // load and setup GUI
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // init audio and camera
        if ( initWebServer() ) {
            initAudio();
            initCamera();
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        if ( cameraView != null) {
            previewLock.lock();
            cameraView.StopPreview();
            cameraView.Release();
            previewLock.unlock();
            cameraView = null;
        }

        finish();
        //System.exit(0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    //
    //  Interface implementation
    //
    public void onCameraReady() {
        cameraView.StopPreview();
        cameraView.setupCamera(480, 360, 4, 25.0, previewCb);

        nativeInitMediaEncoder(cameraView.Width(), cameraView.Height());

        audioCapture.startRecording();
        AudioEncoder audioEncoder = new AudioEncoder();
        audioEncoder.start();

        cameraView.StartPreview();
    }

    //
    //  Internal help functions
    //
    private boolean initWebServer() {
        String ipAddr = "127.0.0.1";
        if ( ipAddr != null ) {
            try{
                webServer = new TeaServer(8080, this);
            }catch (IOException e){
                webServer = null;
            }
        }


        TextView tv = (TextView)findViewById(R.id.tv_message);
        if ( webServer != null) {
            tv.setText( getString(R.string.msg_access_local) + " http://" + ipAddr  + ":8080" );
            return true;
        } else {
            tv.setText( getString(R.string.msg_error) );
            return false;
        }
    }
    private void initCamera() {
        SurfaceView cameraSurface = (SurfaceView)findViewById(R.id.surface_camera);
        cameraView = new CameraView(cameraSurface);
        cameraView.setCameraReadyCallback(this);

        overlayView = (OverlayView)findViewById(R.id.surface_overlay);
        //overlayView_.setOnTouchListener(this);
        //overlayView_.setUpdateDoneCallback(this);
    }

    private void initAudio() {
        int minBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int targetSize = 16000 * 2;      // 1 seconds buffer size
        if (targetSize < minBufferSize) {
            targetSize = minBufferSize;
        }
        if (audioCapture == null) {
            audioCapture = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    targetSize);
        }


    }


    //
    //  Internal help class and object definment
    //
    private PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] frame, Camera c) {
            previewLock.lock();
            doVideoEncode(frame);
            c.addCallbackBuffer(frame);
            previewLock.unlock();
        }
    };

    private void doVideoEncode(byte[] frame) {
        synchronized(this) {
            if ( inProcessing == true) {
                return;
            }
            inProcessing = true;
        }

        int picWidth = cameraView.Width();
        int picHeight = cameraView.Height();
        int size = picWidth*picHeight + picWidth*picHeight/2;
        System.arraycopy(frame, 0, yuvFrame, 0, size);

        VideoEncodingTask videoTask = new  VideoEncodingTask();
        executor.execute(videoTask);
    };

    private class VideoEncodingTask implements Runnable {
        private byte[] resultNal = new byte[1024*1024];

        public void run() {
            int ret = nativeDoVideoEncode(yuvFrame, resultNal, 1);

            synchronized(MainActivity.this) {
                inProcessing = false;
            }
        }
    };

    private class AudioEncoder extends Thread {
        byte[] audioPackage = new byte[1024*32];
        int packageSize = 8000*2;   // 0.5 seconds

        @Override
        public void run() {
            while(true) {
                int ret = audioCapture.read(audioPackage, 0, packageSize);
                if ( ret == AudioRecord.ERROR_INVALID_OPERATION ||
                     ret == AudioRecord.ERROR_BAD_VALUE) {
                    break;
                }
                //nativeAgent.updatePCM(audioPackage, ret);
            }
        }
    }

    private native void nativeInitMediaEncoder(int width, int height);
    private native void nativeReleaseMediaEncoder(int width, int height);
    private native int nativeDoVideoEncode(byte[] in, byte[] out, int flag);

    static {
        System.loadLibrary("MediaEncoder");
    }

}
