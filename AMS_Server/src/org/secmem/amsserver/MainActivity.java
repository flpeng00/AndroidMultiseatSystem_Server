package org.secmem.amsserver;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

@SuppressLint("NewApi")
public class MainActivity extends Activity {

	private static final String TAG = "MainActivity";
	private final int CONNECTION_DIALOG = 0;
	private final int DISCONNECTION_DIALOG = 1;

	private Messenger serviceMessenger = null;
	private ImageButton imgbtnConnect;
	private boolean isServiceBounded = false;
	private boolean isConnected = false;
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor sharedEdit;
	private ProgressDialog pDialog = null;

	private ServiceConnection conn = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceMessenger = new Messenger(service);
			isServiceBounded = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceMessenger = null;
			isServiceBounded = false;
		}
	};

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		Log.d(TAG, "onBackPressed");
		unbindService(conn);
		super.onBackPressed();
	}

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		bindService(new Intent(this, AMSService.class), conn, Context.BIND_AUTO_CREATE);
		imgbtnConnect = (ImageButton)findViewById(R.id.imgbtn_connect);
		imgbtnConnect.setBackgroundResource(R.drawable.selector_disconnected);
		sharedPreferences = getSharedPreferences("pref", MODE_PRIVATE);
		sharedEdit = sharedPreferences.edit();
		pDialog = new ProgressDialog(MainActivity.this);
		pDialog.setTitle("AMS Server");
		pDialog.setMessage("Connecting...");
		pDialog.setIndeterminate(true);
		pDialog.setCancelable(false);
		Log.d(TAG, "Created");
	}

	@Override
	protected void onStart() {
		super.onStart();
		imgbtnConnect.setOnClickListener(onClickListener);
	}

	OnClickListener onClickListener = new OnClickListener(){
		@SuppressWarnings("deprecation")
		@Override
		public void onClick(View v) {
			switch(v.getId()){
			case R.id.imgbtn_connect:
				if(isConnected) {
					try {
						Log.d(TAG, "Disconnect MainSocket");
						serviceMessenger.send(Message.obtain(null, AMSService.MSG_DISCONNECT_SERVICE));
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				} else{
					showDialog(CONNECTION_DIALOG);
				}
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		IntentFilter filter = new IntentFilter();
		filter.addAction(AMSServerIntent.ACTION_CONNECTED);
		filter.addAction(AMSServerIntent.ACTION_CONNECTION_FAILED);
		filter.addAction(AMSServerIntent.ACTION_DEVICE_OPEN_FAILED);
		filter.addAction(AMSServerIntent.ACTION_DISCONNECTED);
		filter.addAction(AMSServerIntent.ACTION_INTERRUPTED);
		filter.addAction(AMSServerIntent.ACTION_SHOW_CONNECT_FRAGMENT);
		filter.addAction(AMSServerIntent.ACTION_SHOW_CONNECTED_FRAGMENT);
		filter.addAction(AMSServerIntent.ACTION_SHOW_DRIVER_INSTALLATION_FRAGMENT);
		filter.addAction(AMSServerIntent.ACTION_DISCONNECTION_FAILED);
		filter.addAction(AMSServerIntent.ACTION_CONNECT_AUTHENTICATED);
		filter.addAction(AMSServerIntent.ACTION_DISCONNECTING);
		filter.addAction(AMSServerIntent.ACTION_CONNECTING);
		registerReceiver(serviceConnReceiver, filter);
		super.onResume();
	}

	@Override
	protected void onPause(){
		super.onPause();
		unregisterReceiver(serviceConnReceiver);
	}

	@SuppressWarnings("deprecation")
	public String getPath(Uri uri) {
		String[] projection = {MediaStore.Video.Media.DATA};
		Cursor cursor = managedQuery(uri, projection, null, null, null);
		startManagingCursor(cursor);
		int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
		cursor.moveToFirst();
		return cursor.getString(columnIndex);
	}

	protected Dialog onCreateDialog(int id){
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		switch(id){	
		case CONNECTION_DIALOG:
			final EditText editIP = new EditText(this);
			editIP.setText(sharedPreferences.getString("IP_ADDRESS", ""));
			builder.setTitle("Input client IP");
			builder.setView(editIP);
			builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(Pattern.matches("^(([2][5][0-5]|[2][0-4][0-9]|[0-1][0-9][0-9]|[0-9][0-9]|[0-9])\\.){3}([2][5][0-5]|[2][0-4][0-9]|[0-1][0-9][0-9]|[0-9][0-9]|[0-9])$", 
							editIP.getText())){
						sharedEdit.putString("IP_ADDRESS", editIP.getText().toString());
						sharedEdit.commit();
						try {
							serviceMessenger.send(Message.obtain(null, AMSService.MSG_CONNECT_CTRL));
						} catch (RemoteException e) {
							Toast.makeText(getApplicationContext(), "Error : Cannot send msg to service", Toast.LENGTH_LONG).show();
							e.printStackTrace();
						}
					}else{
						Toast.makeText(getApplicationContext(), "Invalid IPv4 Format", Toast.LENGTH_SHORT).show();
					}
				}
			});
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {				
				}
			});
			break;
		case DISCONNECTION_DIALOG:
			break;
		default:
			break;

		}
		return builder.create();
	}

	private Thread pDThread = new Thread(){

		@Override
		public void destroy() {
			// TODO Auto-generated method stub
			super.destroy();
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			super.run();
		}

	};

	private BroadcastReceiver serviceConnReceiver = new BroadcastReceiver(){

		@SuppressLint("NewApi")
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if(AMSServerIntent.ACTION_CONNECTING.equals(action)){
				Log.d(TAG, "BR : Connecting");
				pDialog.setMessage("Connecting...");
				pDialog.show();
			} else if(AMSServerIntent.ACTION_CONNECTED.equals(action)){
				Log.d(TAG, "BR : Connection Success");
				imgbtnConnect.setBackgroundResource(R.drawable.selector_connected);
				isConnected = true;
				pDialog.dismiss();
				Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
				try {
					serviceMessenger.send(Message.obtain(null, AMSService.MSG_ON_CONNECTION));
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if(AMSServerIntent.ACTION_DISCONNECTING.equals(action)){
				Log.d(TAG, "BR : Disconnecting");
				pDialog.setMessage("Disconnecting...");
				pDialog.show();
			} else if(AMSServerIntent.ACTION_DISCONNECTED.equals(action)){
				imgbtnConnect.setBackgroundResource(R.drawable.selector_disconnected);
				isConnected = false;
				pDialog.dismiss();
				Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_LONG);
				android.os.Process.killProcess(android.os.Process.myPid());
				Log.d(TAG, "BR : Disconnected");
			} else if(AMSServerIntent.ACTION_CONNECTION_FAILED.equals(action)){
				Log.d(TAG, "BR : Connecting Failed");
				Toast.makeText(getApplicationContext(), "Connection Closed", Toast.LENGTH_SHORT).show();
				pDialog.dismiss();
				isConnected = false;
				try {
					serviceMessenger.send(Message.obtain(null, AMSService.MSG_DISCONNECT_SERVICE));
				} catch (RemoteException e) {
					Toast.makeText(getApplicationContext(), "Error : Cannot send msg to service", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			} else{
				Log.d(TAG, "BR : This intent does not work");
			}
		}

	};

}
