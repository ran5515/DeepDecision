package org.tensorflow.demo.video;

import org.tensorflow.demo.AppendLog;
import org.tensorflow.demo.DetectorActivity;
import org.tensorflow.demo.R;
import org.tensorflow.demo.video.Session;
import org.tensorflow.demo.video.SessionBuilder;
import org.tensorflow.demo.video.audio.AudioQuality;
import org.tensorflow.demo.video.gl.SurfaceView;
import org.tensorflow.demo.video.video.VideoQuality;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * A straightforward example of how to stream AMR and H.263 to some public IP using libstreaming.
 * Note that this example may not be using the latest version of libstreaming !
 */
public class MainActivity extends Activity implements OnClickListener, Session.Callback, SurfaceHolder.Callback {
	private Socket socket;
	private static final int SERVERPORT = 8889;
	private final static String TAG = "MainActivity";

	private Button mButton1, mButton2,mButton3,mButton4;
	private SurfaceView mSurfaceView;
	private EditText mEditText,portext;
	private Session mSession;


	private int mBatteryLevel;
	private IntentFilter mBatteryLevelFilter;

	BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
			//Toast.makeText(context, "Current Battery Level: " + mBatteryLevel, Toast.LENGTH_LONG).show();
			AppendLog.Log("video Battery: " + mBatteryLevel);
		}
	};

	private void registerMyReceiver() {
		mBatteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(mBatteryReceiver, mBatteryLevelFilter);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		mButton1 = (Button) findViewById(R.id.button1);
		mButton2 = (Button) findViewById(R.id.button2);
		mButton3 = (Button) findViewById(R.id.button3);
		mButton4 = (Button) findViewById(R.id.button4);
		mSurfaceView = (SurfaceView) findViewById(R.id.surface);
		mEditText = (EditText) findViewById(R.id.editText1);
		portext = (EditText) findViewById(R.id.editText2);

		mSession = SessionBuilder.getInstance()
		.setCallback(this)
		.setSurfaceView(mSurfaceView)
		.setPreviewOrientation(90)//90
		.setContext(getApplicationContext())
		.setAudioEncoder(SessionBuilder.AUDIO_NONE)
		.setAudioQuality(new AudioQuality(16000, 32000))
		.setVideoEncoder(SessionBuilder.VIDEO_H264)
		.setVideoQuality(new VideoQuality(352,288,30,1000000))//320 240
		.build();

		mButton1.setOnClickListener(this);
		mButton2.setOnClickListener(this);
		mButton3.setOnClickListener(this);
		mButton4.setOnClickListener(this);

		mSurfaceView.getHolder().addCallback(this);

		//registerMyReceiver();
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mSession.isStreaming()) {
			mButton1.setText(R.string.stop);
		} else {
			mButton1.setText(R.string.start);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mSession.release();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.button1) {
			// Starts/stops streaming
			mSession.setDestination(mEditText.getText().toString());
			if (!mSession.isStreaming()) {
				mSession.configure();
			} else {
				mSession.stop();
			}
			mButton1.setEnabled(false);
		} else if (v.getId() == R.id.button2){
			// Switch between the two cameras
			mSession.switchCamera();
		}else if (v.getId() == R.id.button3){
			new Thread(new ClientThread()).start();
		}else if (v.getId() == R.id.button4){
			Intent it = new Intent(MainActivity.this, DetectorActivity.class);
			startActivity(it);
		}
	}

	@Override
	public void onBitrateUpdate(long bitrate) {
		Log.d(TAG,"Bitrate: "+bitrate);
	}

	@Override
	public void onSessionError(int message, int streamType, Exception e) {
		mButton1.setEnabled(true);
		if (e != null) {
			logError(e.getMessage());
		}
	}

	@Override
	
	public void onPreviewStarted() {
		Log.d(TAG,"Preview started.");
	}

	@Override
	public void onSessionConfigured() {
		Log.d(TAG,"Preview configured.");
		// Once the stream is configured, you can get a SDP formated session description
		// that you can send to the receiver of the stream.
		// For example, to receive the stream in VLC, store the session description in a .sdp file
		// and open it with VLC while streming.
		Log.d(TAG, mSession.getSessionDescription());
		mSession.start();
	}

	@Override
	public void onSessionStarted() {
		Log.d(TAG,"Session started.");
		mButton1.setEnabled(true);
		mButton1.setText(R.string.stop);
	}

	@Override
	public void onSessionStopped() {
		Log.d(TAG,"Session stopped.");
		mButton1.setEnabled(true);
		mButton1.setText(R.string.start);
	}	
	
	/** Displays a popup to report the eror to the user */
	private void logError(final String msg) {
		final String error = (msg == null) ? "Error unknown" : msg; 
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setMessage(error).setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {}
		});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mSession.startPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mSession.stop();
	}

	class ClientThread implements Runnable {

		@Override
		public void run() {

			try {
				InetAddress serverAddr = InetAddress.getByName(mEditText.getText().toString());
				int p = Integer.parseInt(portext.getText().toString());
				socket = new Socket(serverAddr, p);
				BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String s;
				//Log.v("Results","in Thread1");
				//s = reader.readLine();
				while((s = reader.readLine()) != null){
					System.out.println("Results :" + s);
				}
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

	}
}
