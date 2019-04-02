package com.jmj.robotgrabador;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v7.app.AppCompatActivity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.otaliastudios.cameraview.Audio;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Frame;
import com.otaliastudios.cameraview.FrameProcessor;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;
import com.otaliastudios.cameraview.SessionType;
import com.otaliastudios.cameraview.Size;
import com.otaliastudios.cameraview.VideoQuality;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;

public class MainActivity extends AppCompatActivity implements ArduinoListener {

    private final float FACTOR_LJOY = 200;
    private final float FACTOR_RJOY = 100;
    private final String OUT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DR2019/";

    private TextView tvMsg;
    private TextView tvDebug;
    private Arduino arduino;
    private long startMillis;
    private boolean recording = false;
    private Button bStart;
    private FileWriter fwAnnot;
    private CameraView camera;

    private class MyCameraListener extends CameraListener {
        @Override
        public void onVideoTaken(File video) {
            // The File is the same you passed before.
            // Now it holds a MP4 video.
        }
    }

    private class MyFrameProcessor implements FrameProcessor {
        @Override
        @WorkerThread
        public void process(@NonNull Frame frame) {
            byte[] data = frame.getData();
            int rotation = frame.getRotation();
            long time = frame.getTime();
            Size size = frame.getSize();
            int format = frame.getFormat();
            // process ...
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        // Create out dir
        File outDir = new File(OUT_DIR);
        if (! outDir.exists()) {
            outDir.mkdir();
        }

        // Widgets
        tvMsg = findViewById(R.id.tvMsg);
        tvDebug = findViewById(R.id.tvDebug);
        bStart = findViewById(R.id.bStart);

        // Camera
        camera = findViewById(R.id.camera);
        camera.setLifecycleOwner(this);
        camera.addCameraListener(new MyCameraListener());
        camera.addFrameProcessor(new MyFrameProcessor());
        camera.setSessionType(SessionType.VIDEO);
        camera.setAudio(Audio.OFF);
        camera.setVideoQuality(VideoQuality.MAX_480P);
        camera.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER); // Tap to focus!

        // Button callbacks
        bStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecording();
            }
        });

        // Arduino
        arduino = new Arduino(this, 115200);
        arduino.addVendorId(0x1A86);    // robotdyn mega 2560
    }


    private void toggleRecording() {
        if (recording) {
            recording = false;
            getWindow().getDecorView().setBackgroundColor(Color.WHITE);
            camera.stopCapturingVideo();
            bStart.setText("START");
            try {
                fwAnnot.flush();
                fwAnnot.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
            String prefix = simpleDateFormat.format(new Date());

            File fVideo = new File(OUT_DIR + prefix + ".mp4");
            File fAnnot = new File(OUT_DIR + prefix + ".csv");
            try {
                fwAnnot = new FileWriter(fAnnot , true);
                fwAnnot.write("Timestamp,Vsum,Vdif\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            camera.startCapturingVideo(fVideo);
            bStart.setText("STOP");
            startMillis = System.currentTimeMillis();
            getWindow().getDecorView().setBackgroundColor(Color.RED);
            recording = true;
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_BUTTON_START:
                toggleRecording();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }


    @Override
    public boolean onGenericMotionEvent(MotionEvent event)
    {
        int source = event.getSource();

        if((source & InputDevice.SOURCE_JOYSTICK)== InputDevice.SOURCE_JOYSTICK && event.getAction() == MotionEvent.ACTION_MOVE)
        {
            int i = event.getActionIndex();
            float xl = event.getAxisValue(MotionEvent.AXIS_X);      // left thumb x
            float yl = event.getAxisValue(MotionEvent.AXIS_Y);      // left thumb y
            float xr = event.getAxisValue(MotionEvent.AXIS_Z);      // right thumb x
            float yr = event.getAxisValue(MotionEvent.AXIS_RZ);     // right thumb y

            // Other axes: LTRIGGER, RTRIGGER = GAS, LTRIGGER = BRAKE
            // Buttons: UP, DOWN, LEFT, RIGHT, BUTTON_{A, B, X, Y, L1, R1, L2, R2, THUMBL, THUMBR, START, SELECT}

            int vsum = (int)(-yl * FACTOR_LJOY);  // sum
            int vdif = (int)(xl * FACTOR_LJOY);   // difference
            //int vi = (int)(-yl * FACTOR_LJOY - yr * FACTOR_RJOY - xl * FACTOR_LJOY - xr * FACTOR_RJOY);
            //int vd = (int)(-yl * FACTOR_LJOY - yr * FACTOR_RJOY + xl * FACTOR_LJOY + xr * FACTOR_RJOY);

            long currentMillis = System.currentTimeMillis();

            if (recording) {
                String csvLine = String.valueOf(currentMillis - startMillis) + ","
                        + String.valueOf(vsum) + ","
                        + String.valueOf(vdif) + "\n";
                try {
                    fwAnnot.write(csvLine);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            String cmd = "v" + String.valueOf(vsum) + " " + String.valueOf(vdif) + "\n";
            display("v" + String.valueOf(vsum) + " " + String.valueOf(vdif));
            arduino.send(cmd.getBytes());
            return true;
        }

        return super.onGenericMotionEvent(event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        arduino.setArduinoListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        arduino.unsetArduinoListener();
        arduino.close();
    }

    @Override
    public void onArduinoAttached(UsbDevice device) {
        display("Arduino attached");
        arduino.open(device);
    }

    @Override
    public void onArduinoDetached() {
        display("Arduino detached");

    }

    @Override
    public void onArduinoMessage(byte[] bytes) {
        display(new String(bytes));
    }

    @Override
    public void onArduinoOpened() {

    }

    @Override
    public void onUsbPermissionDenied() {
        // Permission denied: display popup, then...
        arduino.reopen();
    }

    public void display(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvDebug.append(message);
            }
        });
    }
}

