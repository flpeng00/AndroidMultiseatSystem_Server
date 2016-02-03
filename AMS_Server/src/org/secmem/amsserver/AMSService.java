package org.secmem.amsserver;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.secmem.amsserver.Processes.AudioProcess;
import org.secmem.amsserver.Processes.CaptureProcess;
import org.secmem.amsserver.Processes.DocumentProcess;
import org.secmem.amsserver.Processes.ImageProcess;
import org.secmem.amsserver.Processes.InitProcess;
import org.secmem.amsserver.Processes.MirrorProcess;
import org.secmem.amsserver.Processes.VDProcess;
import org.secmem.amsserver.Processes.VideoProcess;

import ssm.sw.natives.InputHandler;
import ssm.sw.natives.NativeKeyCode;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.StrictMode;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;

import com.artifex.mupdf.MuPDFService;
import com.google.gson.Gson;

@SuppressLint("NewApi")
public class AMSService extends Service{

	private static final String TAG = "AMSService";

	//For Interface
	private final Messenger messenger = new Messenger(new AMSHandler());
	private SharedPreferences pref;
	public static String AppfilePath = null;
	private Gson mGson;
	private NotificationManager nManager;
	private Context context;
	private Intent intent;
	private MirrorInput mirrorInput;
	private final int CURSOR_MOVEMENT = 20;
	private final int NOTI_ID = 0;

	//For PDF Service
	private boolean isPDFServiceBounded = false;
	private Messenger pdfServiceMessenger;

	//For Connection
	private Socket ctrlSocket = null;
	private Socket dataSocket = null;
	private DataInputStream dataInputStream;
	private DataOutputStream dataOutputStream;
	private DataInputStream ctrlInputStream;
	private DataOutputStream ctrlOutputStream;
	private String ipAddress = null;
	private boolean isAuthenticated = false;
	private boolean isCtrlClosed = true;
	private JSONObject jsonPacket;

	//Connection PORTS
	private static final int CTRL_PORT = 11000;
	private static final int DATA_PORT = 12000;

	//Process
	private InitProcess initProcess = null;
	private MirrorProcess mirrorProcess= null;
	private VideoProcess videoProcess = null;
	private VDProcess vdProcess = null;
	private AudioProcess audioProcess = null;
	private CaptureProcess captureProcess = null;
	private ImageProcess imageProcess = null;
	private DocumentProcess documentProcess = null;

	//Message Connection
	public static final int MSG_SERVICE_FAILED = 0;
	public static final int MSG_START_SERVICE = 1;
	public static final int MSG_CONNECT_CTRL = 2;
	public static final int MSG_CONNECT_DATA = 3;
	public static final int MSG_ON_CONNECTION = 4;
	public static final int MSG_DISCONNECT_SERVICE = 5;
	public static final int MSG_PROCESS_IMAGEFILE = 6;
	public static final int MSG_PROCESS_AUDIOFILE = 7;
	public static final int MSG_PROCESS_VIDEOFILE = 8;

	//Message for UI
	public static final int MSG_CURSOR_VISIBLE = 0;
	public static final int MSG_CURSOR_GONE = 1;
	public static final int MSG_CURSOR_LEFT = 2;
	public static final int MSG_CURSOR_RIGHT = 3;
	public static final int MSG_CURSOR_UP = 4;
	public static final int MSG_CURSOR_DOWN = 5;


	private InputHandler mInputHandler;

	@SuppressLint("NewApi")
	@Override
	public void onCreate() {

		if(android.os.Build.VERSION.SDK_INT>9){
			Log.d(TAG, "StrictMode : Set");
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();	
			StrictMode.setThreadPolicy(policy);
		}
		jsonPacket = new JSONObject();
		context = getApplicationContext();
		AppfilePath = context.getFilesDir().getPath();
		copyAmsBase();
		Point point = new Point();
		WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
		Display display = windowManager.getDefaultDisplay();
		display.getSize(point);
		initProcess = new InitProcess();
		mirrorProcess = new MirrorProcess();
		vdProcess = new VDProcess();
		videoProcess = new VideoProcess(context);
		audioProcess = new AudioProcess(context);
		captureProcess = new CaptureProcess();
		imageProcess = new ImageProcess(context);
		documentProcess = new DocumentProcess(context);

		try {
			mirrorInput = new MirrorInput(context, point);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		mInputHandler = new InputHandler(this);
		mInputHandler.open();

		initProcess.play();
		nManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		intent = new Intent(this, MuPDFService.class);
		Log.d(TAG, "Service : Created");
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestory");
		if(isDataConnected() || isCtrlConnected()){
			disconnectData();
			disconnectCtrl();
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		pref = getSharedPreferences("pref", MODE_PRIVATE);
		pref.edit();
		intent = new Intent(this, MuPDFService.class);
		return messenger.getBinder();
	};

	private void changeNoti(String string){
		nManager.notify(NOTI_ID, new Notification.Builder(context)
		.setContentTitle("AMSService")
		.setSmallIcon(R.drawable.notification)
		.setContentText(string).build());
	}

	private boolean isshift = false;

	private synchronized void onDataConnected(){

		Thread mThread = new Thread(){
			@Override
			public void run() {

				String type = null;
				String contents = null;
				String filePath = null;
				long fileid = -1;

				int i;
				ArrayList<CommonData> videoList = null;
				ArrayList<CommonData> audioList = null;
				ArrayList<CommonData> imageList = null;
				ArrayList<CommonData> documentList = null;
				try {
					while(true){
						jsonPacket = new JSONObject(dataInputStream.readUTF());
						type = jsonPacket.optString("type");
						contents = jsonPacket.optString("contents");
						fileid = jsonPacket.optLong("id");
						Log.d(TAG, "Packet : " + type + "/" + contents);
						if(type.equals("mirror")){
							if(contents.equals("start")){
								changeNoti("Mirroring Processing");
								mirrorProcess.play(ipAddress);
								uiHandler.sendMessage(Message.obtain(null, MSG_CURSOR_VISIBLE));
							}
							else {
								mirrorInput.kill();
								mirrorProcess.kill();
								changeNoti("Connected");
								uiHandler.sendMessage(Message.obtain(null, MSG_CURSOR_GONE));
							}
						}else if(type.equals("virtual")){
							if(contents.equals("start")){
								vdProcess.play(ipAddress);
								changeNoti("VirtualDisplay Processing");
							} else{
								vdProcess.kill();
								changeNoti("Connected");
							}
						}else if(type.equals("video")){
							if(contents.equals("ready")){
								mGson = new Gson();
								videoList = videoProcess.mGetBVideoList();
								String videoData = mGson.toJson(videoList);
								Log.d(TAG, "Send : " + videoData.length());
								dataOutputStream.writeUTF(videoData);
							} else if(contents.equals("thumbnail")){
								try {
									transferThumbnail(dataOutputStream, "/videothumbnail/");
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							} else if(contents.equals("start")){
								for(i=0;i<videoList.size();i++){
									if(videoList.get(i).getId() == fileid){
										filePath = videoList.get(i).getFilePath();
										changeNoti("Video : " + videoList.get(i).getTitle());
										videoProcess.play(filePath, ipAddress);
										break;
									}
								}
							} else{
								changeNoti("Connected");
								videoProcess.kill();
							}
						} else if(type.equals("audio")){						
							if(contents.equals("ready")){
								mGson = new Gson();
								audioList = audioProcess.mGetBAudioList();
								String audioData = mGson.toJson(audioList);
								Log.d(TAG, "Send : " + audioData.length());
								dataOutputStream.writeUTF(audioData);										
							}
							else if(contents.equals("start")){
								for(i=0;i<audioList.size();i++){
									if(audioList.get(i).getId() == fileid){
										Log.d(TAG, "Play : fileid" + ipAddress);
										audioProcess.play(audioList.get(i).getFilePath(), ipAddress, dataOutputStream);
										changeNoti("Audio : " + audioList.get(i).getTitle());
										break;
									}
								}
							} 
							else {
								audioProcess.kill();
								changeNoti("Connected");
							}
						} else if(type.equals("image")){
							if(contents.equals("ready")){
								mGson = new Gson();
								imageList = imageProcess.mGetBImageList();
								String imageData = mGson.toJson(imageList);
								Log.d(TAG, "Send : " + imageData.length());
								dataOutputStream.writeUTF(imageData);
							} else if(contents.equals("start")) {
								for(i=0;i<imageList.size();i++){
									if(imageList.get(i).getId() == fileid){
										Log.d(TAG, "Play : fileid" + ipAddress);
										imageProcess.setImageToFrameBuffer(imageList.get(i).getFilePath());
										break;
									}
								}
								changeNoti("Image Processing");
							} else{
								changeNoti("Connected");
							}
						} else if(type.equals("capture")){
							if(contents.equals("start")){
								try {
									captureProcess.play();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								imageProcess.setImageToFrameBuffer(AppfilePath+"/capture.png");
							} 
						} else if(type.equals("document")){
							if(contents.equals("ready")){
								mGson = new Gson();
								documentList = documentProcess.mGetBPdfList();
								String documentData = mGson.toJson(documentList);
								Log.d(TAG,"Send : "+ documentData.length());
								dataOutputStream.writeUTF(documentData);
							} else if(contents.equals("start")) {
								for(i=0;i<documentList.size();i++){
									if(documentList.get(i).getId() == fileid){
										Log.d(TAG, "Play : fileid" + ipAddress);
										Uri uri = Uri.parse(documentList.get(i).getFilePath());
										changeNoti("Document : " + documentList.get(i).getFilePath());
										intent.setData(uri);
										intent.setAction(Intent.ACTION_VIEW);
										Log.d(TAG, "pdfConn Bind");
										bindService(intent, pdfConn, Context.BIND_AUTO_CREATE);
										break;
									}
								} 
							} else if(contents.equals("next")){
								pdfServiceMessenger.send(Message.obtain(null, MuPDFService.MSG_MOVE_NEXT));
							} else if(contents.equals("prev")){
								pdfServiceMessenger.send(Message.obtain(null, MuPDFService.MSG_MOVE_PREV));
							} else if(contents.equals("stop")){
								if (isPDFServiceBounded) {
									unbindService(pdfConn);
									isPDFServiceBounded = false;
								}
							} else{
								changeNoti("Connected");
							}
						} else if(type.equals("key")){
							if(contents.equals("left"))
								uiHandler.sendMessage(Message.obtain(null, MSG_CURSOR_LEFT));
							else if(contents.equals("right"))
								uiHandler.sendMessage(Message.obtain(null, MSG_CURSOR_RIGHT));
							else if(contents.equals("up"))
								uiHandler.sendMessage(Message.obtain(null, MSG_CURSOR_UP));
							else if(contents.equals("down"))
								uiHandler.sendMessage(Message.obtain(null, MSG_CURSOR_DOWN));
							else if(contents.equals("shift"))
							{
								isshift = !isshift;
								if(isshift){
									mInputHandler.keyDown(NativeKeyCode.KEY_LEFTSHIFT);
								}
								else{
									mInputHandler.keyUp(NativeKeyCode.KEY_LEFTSHIFT);
								}
							}
							else if(contents.equals("pagedown"))
							{
								mInputHandler.keyStroke(NativeKeyCode.KEY_DOWN);
							}
							else if(contents.equals("pageup"))
							{
								mInputHandler.keyStroke(NativeKeyCode.KEY_UP);
							}
							else if(contents.equals("home"))
							{
								mirrorInput.home();
							}
							else if(contents.equals("back"))
							{
								mInputHandler.keyStroke(NativeKeyCode.KEY_BACK);
								//mirrorInput.back();
							}
							else if(contents.equals("click"))
								mirrorInput.click();
							else if(contents.equals("space"))
							{
								mInputHandler.keyStroke(NativeKeyCode.KEY_SPACE);
								//mirrorInput.keySpace();
							}
							else if(contents.equals("backspace"))
							{
								mInputHandler.keyStroke(NativeKeyCode.KEY_BACKSPACE);
								//mirrorInput.keyBackSpace();
							}
							else if(contents.equals("enter"))
							{
								mInputHandler.keyStroke(NativeKeyCode.KEY_ENTER);
								//mirrorInput.keyEnter();
							}
							else
							{
								if(contents.charAt(0) >= 'A' && contents.charAt(0) <= 'Z')
								{
									switch (contents.charAt(0)) {
									case 'A':
										mInputHandler.keyStroke(NativeKeyCode.KEY_A);
										break;
									case 'B':
										mInputHandler.keyStroke(NativeKeyCode.KEY_B);
										break;
									case 'C':
										mInputHandler.keyStroke(NativeKeyCode.KEY_C);
										break;
									case 'D':
										mInputHandler.keyStroke(NativeKeyCode.KEY_D);
										break;
									case 'E':
										mInputHandler.keyStroke(NativeKeyCode.KEY_E);
										break;
									case 'F':
										mInputHandler.keyStroke(NativeKeyCode.KEY_F);
										break;
									case 'G':
										mInputHandler.keyStroke(NativeKeyCode.KEY_G);
										break;
									case 'H':
										mInputHandler.keyStroke(NativeKeyCode.KEY_H);
										break;
									case 'I':
										mInputHandler.keyStroke(NativeKeyCode.KEY_I);
										break;
									case 'J':
										mInputHandler.keyStroke(NativeKeyCode.KEY_J);
										break;
									case 'K':
										mInputHandler.keyStroke(NativeKeyCode.KEY_K);
										break;
									case 'L':
										mInputHandler.keyStroke(NativeKeyCode.KEY_L);
										break;
									case 'M':
										mInputHandler.keyStroke(NativeKeyCode.KEY_M);
										break;
									case 'N':
										mInputHandler.keyStroke(NativeKeyCode.KEY_N);
										break;
									case 'O':
										mInputHandler.keyStroke(NativeKeyCode.KEY_O);
										break;
									case 'P':
										mInputHandler.keyStroke(NativeKeyCode.KEY_P);
										break;
									case 'Q':
										mInputHandler.keyStroke(NativeKeyCode.KEY_Q);
										break;
									case 'R':
										mInputHandler.keyStroke(NativeKeyCode.KEY_R);
										break;
									case 'S':
										mInputHandler.keyStroke(NativeKeyCode.KEY_S);
										break;
									case 'T':
										mInputHandler.keyStroke(NativeKeyCode.KEY_T);
										break;
									case 'U':
										mInputHandler.keyStroke(NativeKeyCode.KEY_U);
										break;
									case 'V':
										mInputHandler.keyStroke(NativeKeyCode.KEY_V);
										break;
									case 'W':
										mInputHandler.keyStroke(NativeKeyCode.KEY_W);
										break;
									case 'X':
										mInputHandler.keyStroke(NativeKeyCode.KEY_X);
										break;
									case 'Y':
										mInputHandler.keyStroke(NativeKeyCode.KEY_Y);
										break;
									case 'Z':
										mInputHandler.keyStroke(NativeKeyCode.KEY_Z);
										break;
									}
								}
								else if(contents.charAt(0) >= '0' && contents.charAt(0) <= '9')
								{
									mInputHandler.keyStroke(KeyEvent.keyCodeFromString(contents) + 1);
								}
								//mirrorInput.keyChar(contents);
							}
						}
					}
				} catch (IOException e) {
					Log.d(TAG, "onDataConnection : BR/CONNECTION_FAILED");
					if(isDataConnected() || isCtrlConnected()){
						sendBroadcast(new Intent(AMSServerIntent.ACTION_CONNECTION_FAILED));
					}
					Log.d(TAG, "onDataConnection : Failed");
					e.printStackTrace();
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (RemoteException e) {
					Log.d(TAG, "onDataConnection : Message Send Error");
					e.printStackTrace();
				}
			}
		};
		mThread.start();
	}

	private synchronized void connectCtrl(){

		new AsyncTask<Void, Void, Void>(){
			@Override
			protected Void doInBackground(Void... arg0) {
				try {
					ipAddress = pref.getString("IP_ADDRESS", null);
					Log.d(TAG, "ipAddress : " + ipAddress);
					InetSocketAddress socketAddr = new InetSocketAddress(ipAddress, CTRL_PORT);
					ctrlSocket = new Socket();
					ctrlSocket.connect(socketAddr, 5000);
					ctrlInputStream = new DataInputStream(ctrlSocket.getInputStream());
					ctrlOutputStream = new DataOutputStream(ctrlSocket.getOutputStream());
					isCtrlClosed = false;
					Log.d(TAG, "Ctrl Connection : Created");
					String cmd = null;
					while(true){
						if(isCtrlClosed)
							break;
						cmd = ctrlInputStream.readUTF();
						Log.d(TAG, cmd);
						if(cmd.equals("disconnect")){
							sendBroadcast(new Intent(AMSServerIntent.ACTION_DISCONNECTING));
							disconnectData();
							break;
						} else if(cmd.equals("success")){
							Log.d(TAG, "Ctrl Connection : Success");
							isAuthenticated = true;
							try {
								Log.d(TAG, "Data Connection : Start");
								InetSocketAddress socketAddr1 = new InetSocketAddress(ipAddress, DATA_PORT);
								dataSocket = new Socket();
								dataSocket.connect(socketAddr1, 5000);
								dataInputStream = new DataInputStream(dataSocket.getInputStream());
								dataOutputStream = new DataOutputStream(dataSocket.getOutputStream());					
								videoProcess.playIntro(AppfilePath);
								Log.d(TAG, "Data Connection : BR/CONNECTED");
								sendBroadcast(new Intent(AMSServerIntent.ACTION_CONNECTED));
								changeNoti("Connected");
								Log.d(TAG, "Data Connection : Success");
							} catch (IOException e) {
								Log.d(TAG, "Data Connection : Failed");
								e.printStackTrace();
								break;
							}									
						}
					}
					Log.d(TAG, "Ctrl Connection : Loop Esc");
					sendBroadcast(new Intent(AMSServerIntent.ACTION_DISCONNECTING));
					if(isDataConnected() || isCtrlConnected()){
						disconnectData();
						disconnectCtrl();
					}
					Log.d(TAG, "Ctrl Connection : BR/DISCONNECTED");
					sendBroadcast(new Intent(AMSServerIntent.ACTION_DISCONNECTED));
				} catch (IOException e) {
					Log.d(TAG, "Ctrl Connection : Failed");
					if(isDataConnected() || isCtrlConnected()){
						Log.d(TAG, "Ctrl Connection : BR/CONNECTION_FAILED");
						sendBroadcast(new Intent(AMSServerIntent.ACTION_CONNECTION_FAILED));
					}
					e.printStackTrace();
				}
				Log.d(TAG, "Ctrl Connection : Terminated");
				return null;
			}
		}.execute();
	}

	private synchronized void disconnectData(){

		new AsyncTask<Void, Void, Void>(){

			@Override
			protected Void doInBackground(Void... params) {
				try {
					videoProcess.kill();
					mirrorProcess.kill();
					vdProcess.kill();
					audioProcess.kill();
					captureProcess.kill();
					initProcess.kill();
					imageProcess = null;
					if (isPDFServiceBounded) {
						unbindService(pdfConn);
						isPDFServiceBounded = false;
					}
					if(dataSocket!=null){
						dataSocket.close();
						dataSocket = null;
						Log.d(TAG, "DataSocket : Nullified");
					}
					Log.d(TAG, "Data Disconnected");
				} catch (IOException e) {
					Log.d(TAG, "Data Disconnection : Failed");
					e.printStackTrace();
				}
				return null;
			}
		}.execute();
	}

	private synchronized void disconnectCtrl(){

		new AsyncTask<Void, Void, Void>(){

			@Override
			protected Void doInBackground(Void... params) {
				if(isCtrlConnected()){
					if(ctrlSocket!=null){
						try{
							isCtrlClosed = true;
							ctrlOutputStream.writeUTF("disconnect");
							//ctrlInputStream.close();
							//ctrlOutputStream.close();
							ctrlSocket.close();
							ctrlSocket = null;
							nManager.cancel(NOTI_ID);
							Log.d(TAG, "Ctrl Disconnected");
						}catch(IOException e){
							Log.d(TAG, "Ctrl Disconnection : Failed");
							isCtrlClosed = false;
							e.printStackTrace();
						}
					}
				}
				Log.d(TAG, "Ctrl Disconnection : Terminated");
				return null;
			}
		}.execute();
	}

	private boolean isDataConnected() {
		return (dataSocket!=null && dataSocket.isConnected()) ? true : false;
	}

	private boolean isCtrlConnected() {
		return (ctrlSocket!=null && ctrlSocket.isConnected()) ? true : false;
	}

	private void sendStatusPacket(String type, String contents){
		try {
			jsonPacket.put("type", type);
			jsonPacket.put("contents", contents);
			Log.d(TAG, jsonPacket.toString());
			dataOutputStream.write(jsonPacket.toString().getBytes());
		} catch (JSONException e) {
			Log.d(TAG, "sendPacket() : Error");
			e.printStackTrace();
		} catch (IOException e) {
			Log.d(TAG, "sendPacket() : Error");
			e.printStackTrace();
		}
	}

	@SuppressLint("HandlerLeak")
	private class AMSHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {

			switch(msg.what) {
			case MSG_CONNECT_CTRL:
				if(isAuthenticated){
				} else
					connectCtrl();
				break;
			case MSG_CONNECT_DATA:
				if(!isDataConnected()){
				} else
					Log.d(TAG, "Already Connected");
				break;
			case MSG_ON_CONNECTION:
				if(isDataConnected() && isCtrlConnected()){
					onDataConnected();
				}
				break;
			case MSG_DISCONNECT_SERVICE:
				sendBroadcast(new Intent(AMSServerIntent.ACTION_DISCONNECTING));
				disconnectData();
				disconnectCtrl();
				sendBroadcast(new Intent(AMSServerIntent.ACTION_DISCONNECTED));
			case MSG_SERVICE_FAILED:
				break;
			case MSG_PROCESS_IMAGEFILE:
				if(isDataConnected()){
					String Path = pref.getString("FILEPATH", null);
					Log.d(TAG, "FilePath : " + Path);
					sendStatusPacket("photo", "start");
					imageProcess.setImageToFrameBuffer(Path);
				}
				break;
			case MSG_PROCESS_AUDIOFILE:
				if(true){
					String Path = pref.getString("FILEPATH", null);
					Log.d(TAG, "FilePath : " + Path);
					sendStatusPacket("audio", "start");
					audioProcess.play(Path, ipAddress, dataOutputStream);
				}
			case MSG_PROCESS_VIDEOFILE:
				if(true){	//isConnected
					String Path = pref.getString("FILEPATH", null);
					Log.d(TAG, "FilePath : " + Path);
					sendStatusPacket("video", "start");
					videoProcess.play(Path, ipAddress);
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}	
	}

	ServiceConnection pdfConn = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			pdfServiceMessenger = new Messenger(service);
			isPDFServiceBounded = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			pdfServiceMessenger = null;
			isPDFServiceBounded = false;
		}
	};

	@SuppressWarnings("resource")
	public void transferThumbnail(DataOutputStream dataOutputStream, String path) throws IOException, InterruptedException{
		Log.e(TAG,"in thumbnail");

		File thumbnaillist= new File(AppfilePath+path);

		File[] files=thumbnaillist.listFiles();
		Log.d(TAG, ""+files.length);
		for( int count = 0 ; count < files.length ; count ++ ){
			System.out.println(files[count].getName());
		}

		dataOutputStream.writeInt(files.length);

		for (int count = 0 ; count < files.length ; count ++ ){
			dataOutputStream.writeUTF(files[count].getName());
		}
		for ( int count = 0 ;  count < files.length ; count ++ ){
			int filesize = (int) files[count].length();
			dataOutputStream.writeInt(filesize);
		}
		for ( int count = 0 ;  count < files.length ; count ++ ){
			int filesize = (int) files[count].length();
			byte [] buffer = new byte [filesize];
			Log.d(TAG, "filsize : "+filesize);
			FileInputStream fis = new FileInputStream(files[count]);
			BufferedInputStream bis = new BufferedInputStream(fis);
			bis.read(buffer, 0, filesize);
			dataOutputStream.write(buffer,0,filesize);
			dataOutputStream.flush();
		} 
		Log.e(TAG,"in thumbnail : Complete!");
	}

	private void copyAmsBase()
	{
		AssetManager assetManager = getApplicationContext().getAssets();
		InputStream in = null;
		OutputStream out = null;
		try {
			in = assetManager.open("ffmpeg");
			Log.d(TAG, AppfilePath+"/ffmpeg");
			out = new FileOutputStream(AppfilePath+"/ffmpeg");
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;

			in = assetManager.open("mplayer");
			out = new FileOutputStream(AppfilePath+"/mplayer");
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;

			in = assetManager.open("intro_movie.mp4");
			out = new FileOutputStream(AppfilePath+"/intro_movie.mp4");
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;

			in = assetManager.open("musicbase.jpg");
			out = new FileOutputStream(AppfilePath+"/musicbase.jpg");
			copyFile(in, out);
			in.close();
			in = null;
			out.flush();
			out.close();
			out = null;
		} catch (IOException e) {
			Log.e("tag", "Failed to copy asset file", e);
		}
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
	}

	public Handler uiHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case MSG_CURSOR_VISIBLE:
				mirrorInput.setVisibility(View.VISIBLE);
				break;
			case MSG_CURSOR_GONE:
				mirrorInput.setVisibility(View.GONE);
				break;
			case MSG_CURSOR_LEFT:
				mirrorInput.move(CURSOR_MOVEMENT * (-1), 0);
				break;
			case MSG_CURSOR_RIGHT:
				mirrorInput.move(CURSOR_MOVEMENT, 0);
				break;
			case MSG_CURSOR_UP:
				mirrorInput.move(0, CURSOR_MOVEMENT * (-1));
				break;
			case MSG_CURSOR_DOWN:
				mirrorInput.move(0, CURSOR_MOVEMENT);
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}

	};
}


