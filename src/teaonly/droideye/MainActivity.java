package teaonly.droideye;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;
import java.math.BigInteger;
import java.util.Collections;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.http.conn.util.InetAddressUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.PictureCallback;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.media.AudioRecord;
import android.os.Bundle;
import android.os.Looper;
import android.os.Handler;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.framing.FrameBuilder;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

//import com.google.android.gms.ads.*;

public class MainActivity extends Activity
        implements CameraView.CameraReadyCallback {
    public static String TAG="TEAONLY";
    private final int ServerPort = 8080;
    private final int StreamingPort = 8088;
    private final int PictureWidth = 480;
    private final int PictureHeight = 360;
    private final int MediaBlockNumber = 3;
    private final int MediaBlockSize = 1024*512;
    private final int EstimatedFrameNumber = 30;
    private final int StreamingInterval = 100;

    private StreamingServer streamingServer = null;
    private TeaServer webServer = null;
    private OverlayView overlayView = null;
    private CameraView cameraView = null;
    private AudioRecord audioCapture = null;

    ExecutorService executor = Executors.newFixedThreadPool(3);
    VideoEncodingTask videoTask = new  VideoEncodingTask();
    private ReentrantLock previewLock = new ReentrantLock();
    boolean inProcessing = false;

    byte[] yuvFrame = new byte[1920*1280*2];

    MediaBlock[] mediaBlocks = new MediaBlock[MediaBlockNumber];
    int mediaWriteIndex = 0;
    int mediaReadIndex = 0;

    Handler streamingHandler;


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
        
        /* removed AD
        AdView adView = new AdView(this);
        adView.setAdUnitId("ca-app-pub-7979468066645196/6325058260");
        adView.setAdSize(AdSize.BANNER);
        LinearLayout layout = (LinearLayout)findViewById(R.id.layout_ad);
        layout.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        */

        // init audio and camera
        for(int i = 0; i < MediaBlockNumber; i++) {
            mediaBlocks[i] = new MediaBlock(MediaBlockSize);
        }
        resetMediaBuffer();

        try {
            streamingServer = new StreamingServer(StreamingPort);
            streamingServer.start();
        } catch (UnknownHostException e) {
            return;
        }

        if ( initWebServer() ) {
            initAudio();
            initCamera();
        } else {
            return;
        }

        streamingHandler = new Handler();
        streamingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doStreaming();
            }
        }, StreamingInterval);

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        if ( webServer != null)
            webServer.stop();
        if ( audioCapture != null)
            audioCapture.release();

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
        cameraView.setupCamera(PictureWidth, PictureHeight, 4, 25.0, previewCb);

        nativeInitMediaEncoder(cameraView.Width(), cameraView.Height());

        if ( audioCapture != null) {
            audioCapture.startRecording();
            AudioEncoder audioEncoder = new AudioEncoder();
            audioEncoder.start();
        }

        cameraView.StartPreview();
    }

    //
    //  Internal help functions
    //
    private boolean initWebServer() {

        String ipAddr = wifiIpAddress(this);
        if ( ipAddr != null ) {
            try{
                webServer = new TeaServer(8080, this);
                webServer.registerCGI("/cgi/query", doQuery);
            }catch (IOException e){
                webServer = null;
            }
        }

        TextView tv = (TextView)findViewById(R.id.tv_message);
        if ( webServer != null) {
            tv.setText( getString(R.string.msg_access_local) + " http://" + ipAddr  + ":8080" );
            return true;
        } else {
            if ( ipAddr == null) {
                tv.setText( getString(R.string.msg_wifi_error) );
            } else {
                tv.setText( getString(R.string.msg_port_error) );
            }
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
            try {
              audioCapture = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    8000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    targetSize);
            } catch (IllegalArgumentException	 e) {
              audioCapture = null;
            }
        }
    }

    private void resetMediaBuffer() {
        synchronized(MainActivity.this) {
            for (int i = 1; i < MediaBlockNumber; i++) {
                mediaBlocks[i].reset();
            }
            mediaWriteIndex = 0;
            mediaReadIndex = 0;
        }
    }

    private void doStreaming () {
        synchronized(MainActivity.this) {
               
            MediaBlock targetBlock = mediaBlocks[mediaReadIndex];
            if ( targetBlock.flag == 1) {
                streamingServer.sendMedia( targetBlock.data(), targetBlock.length());
                targetBlock.reset();

                mediaReadIndex ++;
                if ( mediaReadIndex >= MediaBlockNumber) {
                    mediaReadIndex = 0;
                } 
            }
        }

        streamingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doStreaming();
            }
        }, StreamingInterval);

    }

    protected String wifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFIIP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
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
        if ( inProcessing == true) {
            return;
        }
        inProcessing = true;

        int picWidth = cameraView.Width();
        int picHeight = cameraView.Height();
        int size = picWidth*picHeight + picWidth*picHeight/2;
        System.arraycopy(frame, 0, yuvFrame, 0, size);

        executor.execute(videoTask);
    };


    private TeaServer.CommonGatewayInterface doQuery = new TeaServer.CommonGatewayInterface () {
        @Override
        public String run(Properties parms) {
            String ret = "";
            if ( streamingServer.inStreaming == true ) {
                ret = "{\"state\": \"busy\"}";
            } else {
                ret = "{\"state\": \"ok\",";
                ret = ret + "\"width\": \"" + cameraView.Width() + "\",";
                ret = ret + "\"height\": \"" + cameraView.Height() + "\"}";
            }
            return ret;
        }

        @Override
        public InputStream streaming(Properties parms) {
            return null;
        }
    };

    private class VideoEncodingTask implements Runnable {
        private byte[] resultNal = new byte[1024*1024];
        private byte[] videoHeader = new byte[8];

        public VideoEncodingTask() {
            videoHeader[0] = (byte)0x19;
            videoHeader[1] = (byte)0x79;
        }

        public void run() {
            MediaBlock currentBlock = mediaBlocks[ mediaWriteIndex ];
            if ( currentBlock.flag == 1) {
                inProcessing = false;
                return;
            }

            int intraFlag = 0;
            if ( currentBlock.videoCount == 0) {
                intraFlag = 1;
            }
            int millis = (int)(System.currentTimeMillis() % 65535);
            int ret = nativeDoVideoEncode(yuvFrame, resultNal, intraFlag);
            if ( ret <= 0) {
                return;
            }
            
            // timestamp
            videoHeader[2] = (byte)(millis & 0xFF);
            videoHeader[3] = (byte)((millis>>8) & 0xFF);
            // length
            videoHeader[4] = (byte)(ret & 0xFF);
            videoHeader[5] = (byte)((ret>>8) & 0xFF);
            videoHeader[6] = (byte)((ret>>16) & 0xFF);
            videoHeader[7] = (byte)((ret>>24) & 0xFF);

            synchronized(MainActivity.this) {
                if ( currentBlock.flag == 0) { 
                    boolean changeBlock = false;

                    if ( currentBlock.length() + ret + 8 <= MediaBlockSize ) {
                        currentBlock.write( videoHeader, 8 );
                        currentBlock.writeVideo( resultNal, ret);
                    } else {
                        changeBlock = true;
                    }
                    
                    if ( changeBlock == false ) {
                        if ( currentBlock.videoCount >= EstimatedFrameNumber) {
                            changeBlock = true;
                        }
                    }

                    if ( changeBlock == true) {
                        currentBlock.flag = 1;
                        
                        mediaWriteIndex ++;
                        if ( mediaWriteIndex >= MediaBlockNumber) {
                            mediaWriteIndex = 0;
                        } 
                    }
                }

            }

            inProcessing = false;
        }
    };

    private class AudioEncoder extends Thread {
        private byte[] audioPCM = new byte[1024*32];
        private byte[] audioPacket = new byte[1024*1024];
        private byte[] audioHeader = new byte[8];

        int packageSize = 16000;

        public AudioEncoder () {
            audioHeader[0] = (byte)0x19;
            audioHeader[1] = (byte)0x82;
        }

        @Override
        public void run() {
            while(true) {
                int millis = (int)(System.currentTimeMillis() % 65535);
                
                int ret = audioCapture.read(audioPCM, 0, packageSize);
                if ( ret == AudioRecord.ERROR_INVALID_OPERATION ||
                     ret == AudioRecord.ERROR_BAD_VALUE) {
                    break;
                }

                ret = nativeDoAudioEncode(audioPCM, ret, audioPacket);
                if(ret <= 0) {
                    break;
                }
                
                // timestamp 
                audioHeader[2] = (byte)(millis & 0xFF);
                audioHeader[3] = (byte)((millis>>8) & 0xFF);
                // length
                audioHeader[4] = (byte)(ret & 0xFF);
                audioHeader[5] = (byte)((ret>>8) & 0xFF);
                audioHeader[6] = (byte)((ret>>16) & 0xFF);
                audioHeader[7] = (byte)((ret>>24) & 0xFF);

                synchronized (MainActivity.this) {
                    MediaBlock currentBlock = mediaBlocks[ mediaWriteIndex];
                    if ( currentBlock.flag == 0) { 
                        currentBlock.write( audioHeader, 8);
                        ret = currentBlock.write( audioPacket, ret);
                        if ( ret == 0) {
                            Log.d(TAG, ">>>>>>> lost audio in Java>>>");
                        }
                    }
                }
            }
        }
    }


    private class StreamingServer extends WebSocketServer {
        private WebSocket mediaSocket = null;
        public boolean inStreaming = false;
        ByteBuffer buf = ByteBuffer.allocate(MediaBlockSize);

        public StreamingServer( int port) throws UnknownHostException {
		        super( new InetSocketAddress( port ) );
	      }

        public boolean sendMedia(byte[] data, int length) {
            boolean ret = false;

            if ( inStreaming == true) {
                buf.clear();
                buf.put(data, 0, length);
                buf.flip();
            }

            if ( inStreaming == true) {
                mediaSocket.send( buf );
                ret = true;
            }

            return ret;
        }

        @Override
      	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
            if ( inStreaming == true) {
                conn.close();
            } else {
                resetMediaBuffer();
                mediaSocket = conn;
                inStreaming = true;
            }
      	}

        @Override
	    public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
            if ( conn == mediaSocket) {
                inStreaming = false;
                mediaSocket = null;
            }
        }

        @Override
      	public void onError( WebSocket conn, Exception ex ) {
             if ( conn == mediaSocket) {
                inStreaming = false;
                mediaSocket = null;
             }
      	}

        @Override
      	public void onMessage( WebSocket conn, ByteBuffer blob ) {

      	}

        @Override
        public void onMessage( WebSocket conn, String message ) {

        }

    }

    private native void nativeInitMediaEncoder(int width, int height);
    private native void nativeReleaseMediaEncoder(int width, int height);
    private native int nativeDoVideoEncode(byte[] in, byte[] out, int flag);
    private native int nativeDoAudioEncode(byte[] in, int length, byte[] out);

    static {
        System.loadLibrary("MediaEncoder");
    }

}
