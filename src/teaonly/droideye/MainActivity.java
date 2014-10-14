package teaonly.droideye;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.ByteBuffer;
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

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.framing.FrameBuilder;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;


public class MainActivity extends Activity
        implements CameraView.CameraReadyCallback {
    public static String TAG="TEAONLY";
    private final int ServerPort = 8080;
    private final int StreamingPort = 8088;
    private final int PictureWidth = 480;
    private final int PictureHeight = 360;
    private final int MediaBlockNumber = 16;
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
    BlockingQueue mediaQueue = new ArrayBlockingQueue(MediaBlockNumber);
    BlockingQueue freeQueue = new ArrayBlockingQueue(MediaBlockNumber);
    MediaBlock currentBlock;

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

    private void resetMediaBuffer() {
        synchronized(MainActivity.this) {
            mediaQueue.clear();
            freeQueue.clear();
            for (int i = 1; i < MediaBlockNumber; i++) {
                mediaBlocks[i].reset();
                freeQueue.offer( mediaBlocks[i]);
            }
            mediaBlocks[0].reset();
            currentBlock = mediaBlocks[0];
        }
    }

    private void doStreaming () {
        MediaBlock targetBlock = null;
        synchronized(MainActivity.this) {
            targetBlock = (MediaBlock) mediaQueue.poll();
        }

        if ( targetBlock != null) {
            streamingServer.sendMedia( targetBlock.data(), targetBlock.length());
            synchronized(MainActivity.this) {
                targetBlock.reset();
                freeQueue.offer(targetBlock);
            }
        }

        streamingHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doStreaming();
            }
        }, StreamingInterval);

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

    private class VideoEncodingTask implements Runnable {
        private byte[] resultNal = new byte[1024*1024];
        private byte[] videoHeader = new byte[8];

        public VideoEncodingTask() {
            videoHeader[0] = (byte)0x19;
            videoHeader[1] = (byte)0x82;
            videoHeader[2] = (byte)0x08;
            videoHeader[3] = (byte)0x25;
        }

        public void run() {
            int flag = 0;
            if ( currentBlock.videoCount == 0) {
                flag = 1;
            }

            int ret = nativeDoVideoEncode(yuvFrame, resultNal, flag);
            if ( ret <= 0) {
                return;
            }

            videoHeader[4] = (byte)(ret & 0xFF);
            videoHeader[5] = (byte)((ret>>8) & 0xFF);
            videoHeader[6] = (byte)((ret>>16) & 0xFF);
            videoHeader[7] = (byte)((ret>>24) & 0xFF);

            synchronized(MainActivity.this) {
                if ( currentBlock.length() + ret + 8 <= MediaBlockSize ) {
                    currentBlock.write( videoHeader, 8 );
                    currentBlock.writeVideo( resultNal, ret);
                } else {
                    // FIXME : drop this packet

                    if ( freeQueue.size() == 0) {
                        currentBlock.reset();
                    } else {
                        mediaQueue.offer(currentBlock);
                        currentBlock = (MediaBlock) freeQueue.poll();
                        currentBlock.reset();
                    }

                    inProcessing = false;
                    return;
                }

                if ( currentBlock.videoCount >= EstimatedFrameNumber) {
                    if ( freeQueue.size() == 0) {
                        currentBlock.reset();
                    } else {
                        mediaQueue.offer(currentBlock);
                        currentBlock = (MediaBlock) freeQueue.poll();
                        currentBlock.reset();
                    }

                }

                inProcessing = false;
            }


        }
    };

    private class AudioEncoder extends Thread {
        private byte[] audioPCM = new byte[1024*32];
        private byte[] audioPacket = new byte[1024*1024];
        private byte[] audioHeader = new byte[8];

        int packageSize = 3200;

        public AudioEncoder () {
            audioHeader[0] = (byte)0x19;
            audioHeader[1] = (byte)0x82;
            audioHeader[2] = (byte)0x08;
            audioHeader[3] = (byte)0x25;
        }

        @Override
        public void run() {
            while(true) {
                int ret = audioCapture.read(audioPCM, 0, packageSize);
                if ( ret == AudioRecord.ERROR_INVALID_OPERATION ||
                     ret == AudioRecord.ERROR_BAD_VALUE) {
                    break;
                }

                ret = nativeDoAudioEncode(audioPCM, ret, audioPacket);
                if(ret <= 0) {
                    break;
                }

                audioHeader[4] = (byte)(ret & 0xFF);
                audioHeader[5] = (byte)((ret>>8) & 0xFF);
                audioHeader[6] = (byte)((ret>>16) & 0xFF);
                audioHeader[7] = (byte)((ret>>24) & 0xFF);

                synchronized (MainActivity.this) {
                    currentBlock.write( audioHeader, 8);
                    currentBlock.write( audioPacket, ret);
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
                mediaSocket = conn;
                inStreaming = true;
            }
      	}

        @Override
	      public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
		         if ( conn == mediaSocket) {
                inStreaming = false;
                mediaSocket = null;
                resetMediaBuffer();
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
