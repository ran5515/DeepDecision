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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image.Plane;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

  private String getPostDataString(HashMap<String, String> params) throws UnsupportedEncodingException {
    StringBuilder result = new StringBuilder();
    boolean first = true;
    Iterator var4 = params.entrySet().iterator();

    while(var4.hasNext()) {
      Map.Entry entry = (Map.Entry)var4.next();
      if(first) {
        first = false;
      } else {
        result.append("&");
      }

      result.append(URLEncoder.encode((String)entry.getKey(), "UTF-8"));
      result.append("=");
      result.append(URLEncoder.encode((String)entry.getValue(), "UTF-8"));
    }

    return result.toString();
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
            Thread.sleep(1000);

            long beginTime = System.currentTimeMillis();
            String host = "192.168.0.102";
            int timeOut = 3000;
            long BeforeTime = System.currentTimeMillis();
            boolean reachable =  InetAddress.getByName(host).isReachable(timeOut);
            long AfterTime = System.currentTimeMillis();
            long latencyTimeDifference = AfterTime - BeforeTime;
            Log.e(TAG, "reachable speed: " + reachable);
            Log.e(TAG, "latency speed: " + latencyTimeDifference);


            long beginNetTime = System.currentTimeMillis();
            Bitmap bp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bp.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] b = baos.toByteArray();
            String encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
            HashMap<String, String> hashMap = new HashMap<String, String>();
            String response = "";
            hashMap.put("image",encodedImage);
            BeforeTime = System.currentTimeMillis();
            String requestURL = "http://192.168.0.102/upload.php";
            try

            {
              URL url = new URL(requestURL);
              HttpURLConnection e = (HttpURLConnection) url.openConnection();
              e.setReadTimeout(15000);
              e.setConnectTimeout(15000);
              e.setRequestMethod("POST");
              e.setDoInput(true);
              e.setDoOutput(true);
              OutputStream os = e.getOutputStream();
              BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
              writer.write(getPostDataString(hashMap));
              writer.flush();
              writer.close();
              os.close();
              int responseCode = e.getResponseCode();
              String line;
              if (responseCode == 200) {
                for (BufferedReader br = new BufferedReader(new InputStreamReader(e.getInputStream())); (line = br.readLine()) != null; response = response + line) {
                  ;
                }
              } else {
                response = "";
                Log.d("PostResponseAsyncTask", responseCode + "");
              }
            } catch (
                    MalformedURLException var11
                    )

            {
              Log.d("PostResponseAsyncTask", "MalformedURLException Error: " + var11.toString());
            } catch (
                    ProtocolException var12
                    )

            {
              Log.d("PostResponseAsyncTask", "ProtocolException Error: " + var12.toString());
            } catch (
                    UnsupportedEncodingException var13
                    )

            {
              Log.d("PostResponseAsyncTask", "UnsupportedEncodingException Error: " + var13.toString());
            } catch (
                    IOException var14
                    )

            {
              Log.d("PostResponseAsyncTask", "IOException Error: " + var14.toString());
            }
            Log.e(TAG,"response speed: " + response);

            AfterTime = System.currentTimeMillis();
            long bwTimeDifference = AfterTime - BeforeTime;
            Log.e(TAG,"Bandwidth speed: " + bwTimeDifference);




            double net = 0;
            if(bwTimeDifference > latencyTimeDifference*1.5){
              bwTimeDifference -= latencyTimeDifference;
            }
            net = 3*4312*8.0/bwTimeDifference;
            Log.e(TAG,"net speed: "+net);
//            WifiManager wifiManager = (WifiManager)getApplication().getSystemService(Context.WIFI_SERVICE);
//            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//            if (wifiInfo != null) {
//              linkSpeed = wifiInfo.getLinkSpeed(); //measured using WifiInfo.LINK_SPEED_UNITS
//
//              net = Integer.parseInt(linkSpeed.toString())*1000;
//              Log.e(TAG, "speed: " + net);
//            }


            if(net <= 0) net = 0;
            //hard-coded data
            double costTarget = 0.01; //$/s. This constraint get very low before it matters.
            double costPerBit = 10.0/8.0/Math.pow(10,9); // $/b. Here use the value $10/GB
            double batteryTarget =10000;  //mW
            double networkBandwidth = net;   //kbps
            double networkLatency = latencyTimeDifference; //ms
            double alpha = 1;

            double framerateTarget = 0.1;
            double accuracyTarget = 1;

            //When performing interpolation/inverse, it will not work if searching for an x-value is larger than the domain of the function.
            //Therefore, set the smallest x-value to 0 and the largest x-value to the maximum possible.
            //Some of the functions are 1D (f:R->R), while others are 2D (f:R^2->R)
            //For the 2D functions, enter the y values as a matrix of size |x1| * |x2|

            //accuracy function. Data taken from Xiaodan's experiments 2.0 stefan.
            //x1=resolution (pixels^2), x2=latency, x3=kbits (remote only), y = accuracy
            //Remote: add an extra value for high bitrate (7000kbps/30fps=250), repeat last accuracy value
            MeasureFxn accuracyRemote = new MeasureFxn("25340,76800,101370","100,250,500,1000","0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20","46.1836698340698,45.69000622178971,45.06132256752855,44.20684085529408,43.739036440103575,43.236234098483564,42.60996789077836,42.11727383169272,41.80571387464917,41.247038854494065,41.06153729344343,40.81944489861306,40.08699950501435,39.91675545807455,39.82866919413174,39.83639774841614,39.39170052828359,39.03565918622135,39.24933669021487,38.903938150543496,38.84067113091935;50.359193587276316,49.43394968190677,48.28829027773091,47.30746180130756,46.568586036770206,46.122445419823194,45.473896595919356,44.5629279137482,44.1275266333198,43.6492568218472,43.16548986360417,43.01139255810844,42.603112528505264,42.183270330840045,41.65789109228156,41.689475861800425,41.50308196499006,41.17054390592858,40.978914985781536,40.96627609380498,40.58937175428525;52.981156572505526,52.0347125804407,50.79440391524013,49.70963677915194,48.78258625820247,48.22534514781452,47.502062791059224,46.734449839038746,46.029224919719816,45.51210207154525,45.10725680370122,44.834395439499666,44.19526625011476,43.6525021082122,43.88158028698261,43.60271691098184,43.05337381428601,42.91040834524106,42.82870637707099,42.47557505186133,42.48625943724101;53.95114596488747,52.518713735649946,51.483445512396074,50.46717716711805,49.32876862788683,48.661182430643734,47.90810285641374,46.88072126467568,46.21437519554098,45.54043863352338,45.643207807336985,45.06750339339369,44.55032298301995,44.12467268175183,44.188793032883076,43.716380567838776,43.460841785433026,43.37583309882593,42.9596579436877,42.87126134962581,42.78758603501027;;48.56544603454657,47.92480063897685,46.904766385662555,45.934819081284,45.062637073004,44.489657209681084,44.08701796790933,43.5975465314098,42.94746485341197,42.373798645902674,42.23067330342386,42.16025072802386,41.37116477022252,41.03828839693662,40.867896195148234,40.66482869502106,40.381152214966065,40.3175831038668,39.815817791122804,40.05516452164744,39.2787342091203;59.30150199215858,57.724891878175036,56.590605580029376,55.58929167766419,54.13933793706707,53.50372147722585,52.81771486045224,51.888863305546145,51.275065365751146,50.60626073994274,50.32907398205458,49.88294208051391,49.85160883199841,49.10487576442793,48.6536910271477,48.48337056081385,48.154393451270536,47.535803840855145,47.33660559761275,46.9576289491953,46.960644660088114;62.581576117467876,60.900913261194724,59.00430116249303,57.65759698630288,55.83700305542289,55.02684757324354,54.325071168467815,53.86411768187914,53.0772395891885,52.56728460268787,51.85051871773183,51.5685168999993,51.21441450709318,50.67204191786309,50.60471211991058,50.05708170754876,49.210320982243,48.9281471171104,48.22716060679573,48.41471089379495,48.557681754700596;63.8214688842132,60.688887763885724,59.130650772892665,57.11297955424196,55.87748299333517,54.81630965490654,53.9702314731729,53.402182889828985,52.729684484084395,52.214630651013245,51.83665704419105,50.967301051173244,50.9286954624122,49.77421828702645,49.835042571554844,49.133818479464445,48.793576546880985,48.13777387053211,47.930129618499336,47.68131430395731,47.55295649540118;;55.17708489244765,54.22011852618775,53.28343665745317,52.62641911070255,51.71449360302208,51.09071693386566,50.28034322729767,49.498812268934586,49.539306594672034,49.30463651378163,48.8143056671796,48.37482066030843,48.19204935426224,47.9081534740529,47.710664588774485,47.23346440253027,47.19871911542798,47.01104450665189,46.84563260351169,46.84872988843725,46.66870069124016;66.91407661776054,64.44973594845884,62.13021160995624,61.07150862885035,60.309291311435246,59.0762221330382,58.361714110640506,57.696685953039804,56.71050331509079,56.31701514952079,55.81967306687587,55.16693300954201,54.34191519480012,54.00533176289973,53.68953766782364,53.49987075053584,53.12273315176413,52.54377155634561,52.30012410034303,52.266545033144624,51.63868820307803;71.42750898579426,67.48121705016716,64.83792595282765,62.9946871557204,61.801786088589665,60.904489847699274,59.9813735627083,59.075276535537114,57.697064871929115,57.16718102261494,57.145908223535244,56.527135888090726,56.14383557355922,55.25663204766725,55.09458867422964,54.545260675358236,54.077800091511214,54.00625532499939,53.21068391558447,52.75524672355547,52.52820325292183;76.83298996472348,70.65506187348848,67.23104836042731,65.15183444649938,63.22204681753677,62.02627063613412,60.75607127211356,60.31767063515551,59.20967039334864,58.3006602963006,57.514361643617384,57.371922773762854,56.464850740532654,55.4507031825801,55.06421239282196,54.635741778084565,54.586928349061495,54.23684826411815,52.79508585630536,52.846461939524765,52.72143205705434");
            //Local: use highest resolution, highest bitrate values to estimate accuracy of raw
            MeasureFxn accuracyLocalTinyYolo = new MeasureFxn("25340,76800,101370","0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20","27.29767119247363,27.194062311888914,26.882888265724613,26.638327815257576,26.353831453026512,26.02401033868452,25.704370501861565,25.34879405645726,25.154700269055017,24.866685710114645,24.74160982587012,24.54946499203611,24.329605673317445,24.194258287509147,24.069958494946732,23.92404886967673,23.810163383658,23.728249597225435,23.587826239099083,23.462806660732618,23.35234401521366;29.826708887994428,29.57717771613746,29.483547187974505,29.28577648605695,28.885041171506714,28.252411898709646,28.03663464036388,27.485190869038338,27.27068174893651,27.097468238436047,26.780393313040154,26.64525909189175,26.431437980644002,26.203648933818343,26.04306497342908,25.719953126267782,25.600344327063638,25.46447594171152,25.5380579218406,25.31433901852912,25.415293768214898;30.754671538125113,30.70488139926198,30.321651213221248,29.937277804642626,29.67336165167596,29.284617498679836,28.815061183572556,28.566241903367516,28.2760146219677,27.971316946441963,27.74284809727724,27.553734964232476,27.34634592146513,27.125809701678875,26.983673072927914,26.85404485171454,26.7179921499534,26.56683563675183,26.416856745485973,26.242252661195007,26.17505962754119");
            MeasureFxn accuracyLocalBigYolo = new MeasureFxn("25340,76800,101370","0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20","53.95114596488747,52.518713735649946,51.483445512396074,50.46717716711805,49.32876862788683,48.661182430643734,47.90810285641374,46.88072126467568,46.21437519554098,45.54043863352338,45.643207807336985,45.06750339339369,44.55032298301995,44.12467268175183,44.188793032883076,43.716380567838776,43.460841785433026,43.37583309882593,42.9596579436877,42.87126134962581,42.78758603501027;63.8214688842132,60.688887763885724,59.130650772892665,57.11297955424196,55.87748299333517,54.81630965490654,53.9702314731729,53.402182889828985,52.729684484084395,52.214630651013245,51.83665704419105,50.967301051173244,50.9286954624122,49.77421828702645,49.835042571554844,49.133818479464445,48.793576546880985,48.13777387053211,47.930129618499336,47.68131430395731,47.55295649540118;76.83298996472348,70.65506187348848,67.23104836042731,65.15183444649938,63.22204681753677,62.02627063613412,60.75607127211356,60.31767063515551,59.20967039334864,58.3006602963006,57.514361643617384,57.371922773762854,56.464850740532654,55.4507031825801,55.06421239282196,54.635741778084565,54.586928349061495,54.23684826411815,52.79508585630536,52.846461939524765,52.72143205705434");

            //cases for debug
            //MeasureFxn accuracyLocalTinyYolo = new MeasureFxn("25340,76800,101370,307200","3,6,17,30","0,0,0,0;0,0,0,0;0,0,0,0;0,0,0,0");
            //MeasureFxn accuracyLocalBigYolo = new MeasureFxn("25340,76800,101370,307200","3,8,16,30","0,0,0,0;0,0,0,0;0,0,0,0;0,0,0,0");

            MeasureFxn[] accuracy = new MeasureFxn[numLocalModel + 1];
            accuracy[0] = accuracyRemote;
            accuracy[1] = accuracyLocalTinyYolo;
            accuracy[2] = accuracyLocalBigYolo;

            //latency function. Data taken from Xukan's experiments on S7.
            MeasureFxn latencyLocalTinyYolo = new MeasureFxn("25600,50176,82944,123904,173056,230400","218,377,609,839,921,1099");  //x=resolution, y=latency (ms)
            MeasureFxn latencyLocalBigYolo = new MeasureFxn("25600,50176,82944,123904,173056,230400","677,1073,1688,2466,3151,4039");
            MeasureFxn[] latency = new MeasureFxn[numLocalModel + 1];
            //there is no latency[0] for remote because we use an equation
            latency[1] = (latencyLocalTinyYolo);
            latency[2] = (latencyLocalBigYolo);
            double latency_remote = 30;	//remote latency doesn't depend on resolution

            //battery function. Data taken from Xukan's experiments on S7.
            //battery is a step function (doesn't smoothly decrease to 0)
            MeasureFxn batteryRemote = new MeasureFxn("100,500,1000","1000,2000,3000","2983,2983,2983;2984,2984,2984;2985,2985,2985");    //x1=bitrate, x2=bandwidth, y=battery (mW). Slightly modified from Xukan's data to be increasing with bitrate.
            MeasureFxn batteryLocalTinyYolo = new MeasureFxn("25600,50176,82944,123904,173056,230400","0.903392,1.625624,3.001152,3.366907,4.943007,7.061075");  //x1 = resolution, y=battery(J/frame)
            MeasureFxn batteryLocalBigYolo = new MeasureFxn("25600,50176,82944,123904,173056,230400","3.89275,7.464861,12.394984,15.664032,21.67888,27.8691"); // x1=resolution, y=battery (J/frame)
            MeasureFxn[] battery = new MeasureFxn[numLocalModel + 1];
            battery[0] = (batteryRemote);
            battery[1] = (batteryLocalTinyYolo);
            battery[2] = (batteryLocalBigYolo);
            Log.v(TAG,"hardcode functions");


            //setup offloading module
            Offload o = new Offload(costTarget, costPerBit, batteryTarget, framerateTarget, accuracyTarget, alpha);
            o.setFxns(accuracy, latency, battery, latency_remote);
            o.setNetworkInput(networkBandwidth, networkLatency);
            //Log.v(TAG,"setup offloading");

            //test 2D interpolation and inverse
            //String result = "result: " + String.valueOf(batteryRemote.inv(9, 2.5));
            //String result = "result: " + String.valueOf(batteryRemote.interp(3,4));

            //test 1D interpolation and inverse
            //String result = "result: " + String.valueOf(batteryLocalTinyYolo.interp(4));
            //String result = "result: " + String.valueOf(batteryLocalTinyYolo.inv(15));
            //Log.v(TAG,result);

            //TODO: implement MCDNN

            //o.runOptimization();
            o.runMCDNN();

            //print the results

            double bitrate = o.getBitrate();
            double resolution = o.getResolution();
            double framerate = o.getFramerate();
            double accuracyPerFrame = o.getAccuracyPerFrame();
            int decision = o.getDecision();


            if (net<500) {
              decision = 1;
              accuracyPerFrame = 42.6;
            }

            if (decision == 0) {    //only display bitrate if offloading
              Log.v(TAG, "bitrate: " + bitrate +",old: " + oldBitrate + ",first:" +first);
            }


            Log.v(TAG,"MCDNNbitrate: " + bitrate);
            Log.v(TAG,"MCDNNresolution: " + resolution);
            Log.v(TAG,"MCDNNframerate: " + framerate);
            Log.v(TAG,"MCDNNaccuracy per frame: " + accuracyPerFrame);
            Log.v(TAG,"MCDNNdecision: " + decision);
            long endTime = System.currentTimeMillis();
            Log.v(TAG,"total time: " + (endTime - beginTime));

            o.runOptimization();
            bitrate = o.getBitrate();
            resolution = o.getResolution();
            framerate = o.getFramerate();
            accuracyPerFrame = o.getAccuracyPerFrame();
            decision = o.getDecision();
            Log.v(TAG,"DEEPbitrate: " + bitrate);
            Log.v(TAG,"DEEPresolution: " + resolution);
            Log.v(TAG,"DEEPframerate: " + framerate);
            Log.v(TAG,"DEEPaccuracy per frame: " + accuracyPerFrame);
            Log.v(TAG,"DEEPdecision: " + decision);


            if(decision == 0) {
              if(first || bitrate != oldBitrate) {
                Intent intent = new Intent(CameraActivity.this, MainActivity.class).putExtra("Main", "" + bitrate + "," + resolution + "," + framerate);
                startActivity(intent);
              }
            }

            if(first){
              oldBitrate = 500;
              first = false;
            }
            //else{
//              //decision = 1;
//              Intent intent = new Intent(CameraActivity.this, DetectorActivity.class).putExtra("Detector", ""+resolution+"," + decision);
//              //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//              startActivity(intent);
//            }


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

  private void sweepNetworkBwLatency(Offload o){
    //Log.v(TAG,",bandwidth,latency,bitrate,resolution,framerate,accuracy,decision");
    for (double networkBandwidth =  100; networkBandwidth < 1001; networkBandwidth= networkBandwidth+100){
      for (double networkLatency = 0; networkLatency < 201; networkLatency = networkLatency + 10) {
        o.setNetworkInput(networkBandwidth, networkLatency);
        o.runOptimization();
        //Log.v(TAG, "\t" + networkBandwidth + "\t" + networkLatency + "\t" + o.getBitrate() + "\t" + o.getResolution() + "\t" + o.getFramerate() + "\t" + o.getAccuracyPerFrame() + "\t" + o.getDecision());
        //Log.v(TAG, "," + networkBandwidth + "," + networkLatency + "," + o.getBitrate() + "," + o.getResolution() + "," + o.getFramerate() + "," + o.getAccuracyPerFrame() + "," + o.getDecision());
      }
    }
  }

  private void sweepBatteryCost(Offload o){
    //Log.v(TAG,"costTarget,batteryarget,bitrate,resolution,framerate,accuracy_per_frame,decision");
    for (int i=1; i<10; i++){  //cost
      double costTarget = i*0.0001;
      for (int j=1; j<10; j++){  //battery
        double batteryTarget = j*100.0;
        o.setTarget(costTarget,batteryTarget);
        o.runOptimization();
        //Log.v(TAG,"\t" + costTarget + "\t" + batteryTarget + "\t" + o.getBitrate() + "\t" + o.getResolution() + "\t" + o.getFramerate() + "\t" + o.getAccuracyPerFrame() + "\t" + o.getDecision());
        Log.v(TAG,"," + costTarget + "," + batteryTarget + "," + o.getBitrate() + "," + o.getResolution() + "," + o.getFramerate() + "," + o.getAccuracyPerFrame() + "," + o.getDecision());
      }

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
