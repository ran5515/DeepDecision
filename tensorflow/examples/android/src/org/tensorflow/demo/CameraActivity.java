/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.demo;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.Image.Plane;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;
import java.nio.ByteBuffer;
import org.tensorflow.demo.env.Logger;
import org.tensorflow.demo.R;
import org.tensorflow.demo.video.MainActivity;

public abstract class CameraActivity extends Activity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();
  private static final String TAG = "CameraActivity";
  private static final int PERMISSIONS_REQUEST = 1;
  private final int numLocalModel = 2;
  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

  private boolean debug = false;

  private Handler handler;
  private HandlerThread handlerThread;


  private int mBatteryLevel;
  private IntentFilter mBatteryLevelFilter;

  BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
      //Toast.makeText(context, "Current Battery Level: " + mBatteryLevel, Toast.LENGTH_LONG).show();
      AppendLog.Log("Battery Level: " + mBatteryLevel);
    }
  };

  private void registerMyReceiver() {
    mBatteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
    registerReceiver(mBatteryReceiver, mBatteryLevelFilter);
  }


  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.activity_camera);

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }

    //registerMyReceiver();

    Handler mHandler = new Handler();

    new Thread(new Runnable() {
      @Override
      public void run() {
        // TODO Auto-generated method stub
        Integer linkSpeed;
        boolean first = true;
        double oldBitrate = 0.0;
        double oldRes = 0.0;
        double oldFrame = 0.0;
        double oldDecision = 0.0;

        while (true) {
          try {
            Thread.sleep(10000);


            int net = 0;
            WifiManager wifiManager = (WifiManager)getApplication().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
              linkSpeed = wifiInfo.getLinkSpeed(); //measured using WifiInfo.LINK_SPEED_UNITS

              net = Integer.parseInt(linkSpeed.toString())*1000;
              Log.e(TAG, "speed: " + net);
            }


            if(net <= 0) net = 0;
            //hard-coded data
            double costTarget = 0.01; //$/s. This constraint get very low before it matters.
            double costPerBit = 10.0/8.0/Math.pow(10,9); // $/b. Here use the value $10/GB
            double batteryTarget = 5000;  //mW
            double networkBandwidth = net;   //kbps
            double networkLatency = 10; //ms

            //When performing interpolation/inverse, it will not work if searching for an x-value is larger than the domain of the function.
            //Therefore, set the smallest x-value to 0 and the largest x-value to the maximum possible.
            //Some of the functions are 1D (f:R->R), while others are 2D (f:R^2->R)
            //For the 2D functions, enter the y values as a matrix of size |x1| * |x2|

            //accuracy function. Data taken from Xiaodan's experiments.
            //x1=resolution (pixels^2), x2=kbits/frame
            MeasureFxn accuracyRemote = new MeasureFxn("25340,76800,101370,307200","3,8,16,30","20.45,19.72,19.60,19.57;40.85,45.57,44.7,45.2;59.91,64.91,68.78,72.44;42.76,44.82,43.64,43.00");
            MeasureFxn accuracyLocalTinyYolo = new MeasureFxn("25340,76800,101370,307200","3,6,17,30","29.80,30.27,31.34,31.06;48.27,50.83,51.92,52.88;62.87,72.48,76.65,80.48;53.74,58.52,58.95,57.76");
//        MeasureFxn accuracyLocalTinyYolo = new MeasureFxn("25340,76800,101370,307200","3,6,17,30","0,0,0,0;0,0,0,0;0,0,0,0;0,0,0,0");
            MeasureFxn accuracyLocalBigYolo = new MeasureFxn("25340,76800,101370,307200","3,8,16,30","20.45,19.72,19.60,19.57;40.85,45.57,44.7,45.2;59.91,64.91,68.78,72.44;42.76,44.82,43.64,43.00");
//        MeasureFxn accuracyLocalBigYolo = new MeasureFxn("25340,76800,101370,307200","3,8,16,30","0,0,0,0;0,0,0,0;0,0,0,0;0,0,0,0");
            MeasureFxn[] accuracy = new MeasureFxn[numLocalModel + 1];
            accuracy[0] = accuracyRemote;
            accuracy[1] = accuracyLocalTinyYolo;
            accuracy[2] = accuracyLocalBigYolo;

            //latency function. Data taken from Xukan's experiments.
            MeasureFxn latencyLocalTinyYolo = new MeasureFxn("25600,50176,82944,123904,173056,230400","144,266,371,561,942,1007");  //x=resolution, y=latency (ms)
            MeasureFxn latencyLocalBigYolo = new MeasureFxn("25600,50176,82944,123904,173056,230400","661,1200,1800,2700,3600,4312");
            MeasureFxn[] latency = new MeasureFxn[numLocalModel + 1];
            //there is no latency[0] for remote because we use an equation
            latency[1] = (latencyLocalTinyYolo);
            latency[2] = (latencyLocalBigYolo);

            //battery function. Data taken from ??
            MeasureFxn batteryRemote = new MeasureFxn("100,500,1000","1000,2000,3000","2983,2983,2983;2984,2984,2984;2985,2985,2985");    //x1=bitrate, x2=bandwidth
            MeasureFxn batteryLocalTinyYolo = new MeasureFxn("25340,76800,101370,307200","4144,4312,4928,4013");  //x1 = resolution, y=battery(mJ)
            MeasureFxn batteryLocalBigYolo = new MeasureFxn("25340,76800,101370,307200","5750,6957,7343,6352");
            MeasureFxn[] battery = new MeasureFxn[numLocalModel + 1];
            battery[0] = (batteryRemote);
            battery[1] = (batteryLocalTinyYolo);
            battery[2] = (batteryLocalBigYolo);
            Log.v(TAG,"hardcode functions");


            //setup offloading module
            Offload o = new Offload(costTarget, costPerBit, batteryTarget);
            o.setFxns(accuracy, latency, battery);
            o.setNetworkInput(networkBandwidth, networkLatency);
            Log.v(TAG,"setup offloading");

            //test 2D interpolation and inverse
            //String result = "result: " + String.valueOf(batteryRemote.inv(9, 2.5));
            //String result = "result: " + String.valueOf(batteryRemote.interp(3,4));

            //test 1D interpolation and inverse
            //String result = "result: " + String.valueOf(batteryLocalTinyYolo.interp(4));
            //String result = "result: " + String.valueOf(batteryLocalTinyYolo.inv(15));
            //Log.v(TAG,result);

            //TODO: implement MCDNN
            //TODO: test tight constraints
            //TODO: Some test examples by hand

            //run the optimization
            o.runOptimization();

            //extract the results
            double bitrate = o.getBitrate();
            double resolution = o.getResolution();
            double framerate = o.getFramerate();
            int decision = o.getDecision();

            Log.v(TAG,"bitrate: " + bitrate);
            Log.v(TAG,"resolution: " + resolution);
            Log.v(TAG,"framerate: " + framerate);
            Log.v(TAG,"decision: " + decision);

            if(first){
              first = false;
            }else{
              if(oldBitrate == bitrate && oldDecision == decision &&
                      oldFrame == framerate && oldRes == resolution){
                continue;
              }
            }
            oldBitrate = bitrate;
            oldRes = resolution;
            oldFrame = framerate;
            oldDecision = decision;


            if(decision == 0) {
              Intent intent = new Intent(CameraActivity.this, MainActivity.class).putExtra("Main", ""+bitrate+","+resolution+"," + framerate);
              startActivity(intent);
            }else{
              //decision = 1;
              Intent intent = new Intent(CameraActivity.this, DetectorActivity.class).putExtra("Detector", ""+resolution+"," + decision);
              //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(intent);
            }


          } catch (Exception e) {
            // TODO: handle exception
          }
        }
      }
    }).start();
  }

  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  public synchronized void stopCam(){
    Log.e("CACACA","stopCam1");
    handlerThread.quitSafely();
    Log.e("CACACA","stopCam2");

    Intent intent = new Intent(CameraActivity.this, MainActivity.class);
    startActivity(intent);


//    try {
//      Intent intent = new Intent(CameraActivity.this, MainActivity.class);
//      startActivity(intent);
//      Log.e("CACACA","stopCam3");
//      handlerThread.join();
//      Log.e("CACACA","stopCam4");
//      handlerThread = null;
//      handler = null;
//
//      Log.e("CACACA","stopCam5");
//    } catch (final InterruptedException e) {
//      LOGGER.e(e, "Exception!");
//    }
  }

  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);

    if (!isFinishing()) {
      LOGGER.d("Requesting finish");
      finish();
    }

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    LOGGER.d("onStop " + this);
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      final int requestCode, final String[] permissions, final int[] grantResults) {
    switch (requestCode) {
      case PERMISSIONS_REQUEST: {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
          setFragment();
        } else {
          requestPermission();
        }
      }
    }
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) || shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
        Toast.makeText(CameraActivity.this, "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
    }
  }

  protected void setFragment() {
    final Fragment fragment =
        CameraConnectionFragment.newInstance(
            new CameraConnectionFragment.ConnectionCallback() {
              @Override
              public void onPreviewSizeChosen(final Size size, final int rotation) {
                CameraActivity.this.onPreviewSizeChosen(size, rotation);
              }
            },
            this,
            getLayoutId(),
            getDesiredPreviewFrameSize());

    getFragmentManager()
        .beginTransaction()
        .replace(R.id.container, fragment)
        .commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return debug;
  }

  public void requestRender() {
    final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
    if (overlay != null) {
      overlay.postInvalidate();
    }
  }

  public void addCallback(final OverlayView.DrawCallback callback) {
    final OverlayView overlay = (OverlayView) findViewById(R.id.debug_overlay);
    if (overlay != null) {
      overlay.addCallback(callback);
    }
  }

  public void onSetDebug(final boolean debug) {}

  @Override
  public boolean onKeyDown(final int keyCode, final KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      debug = !debug;
      requestRender();
      onSetDebug(debug);
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);
  protected abstract int getLayoutId();
  protected abstract Size getDesiredPreviewFrameSize();
}
