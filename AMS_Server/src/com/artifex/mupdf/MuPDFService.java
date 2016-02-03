package com.artifex.mupdf;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public class MuPDFService extends Service{

	public final static String TAG = "MuPDFService";
	

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		Log.d(TAG, "onCreate()");
	}

	public final static int MSG_MOVE_NEXT = 0;
	public final static int MSG_MOVE_PREV = 1;

	Bitmap mEntireBm = null;

	int mPageNumber = 0;

	float WIDTH = 1280;
	float HEIGHT = 720;

	final Messenger myMessenger = new Messenger(new MyMessageHandler());

	@SuppressLint("HandlerLeak")
	public class MyMessageHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub

			switch (msg.what) {
			case MSG_MOVE_NEXT:
				Log.d(TAG, "NEXT Button Click");
				//mDocView.setDisplayedViewIndex(++currentView);
				mPageNumber++;

				if(core != null)
				{
					if(mPageNumber > core.countPages()){
						mPageNumber = core.countPages();
					}

					drawPDFPage(mPageNumber);
				}
				//updatePageNumView(currentView);
				break;

			case MSG_MOVE_PREV:
				Log.d(TAG, "PREV Button Click");
				//mDocView.setDisplayedViewIndex(--currentView);
				mPageNumber--;

				if(core != null)
				{
					if(mPageNumber < 0){
						mPageNumber = 0;
					}

					drawPDFPage(mPageNumber);
				}
				//updatePageNumView(currentView);
				break;
			}
			super.handleMessage(msg);
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {

		Log.d(TAG,"onBind");
		Log.d(TAG, "MuPDF Start");

		mAlertBuilder = new AlertDialog.Builder(this);

		if (core == null) {
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				Uri uri = intent.getData();
				Log.d(TAG, "Filename" + uri.toString());
				
				if (uri.toString().startsWith("content://media/external/file")) {
					Cursor cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
					if (cursor.moveToFirst()) {
						uri = Uri.parse(cursor.getString(0));
					}
				}
				
				Log.d(TAG, Uri.decode(uri.getEncodedPath()));
				core = openFile(Uri.decode(uri.getEncodedPath()));
			}
			if (core != null && core.needsPassword()) {
				requestPassword();

				Log.d(TAG,"RequestPassword");
				return null;
			}
		}
		if (core == null)
		{
			AlertDialog alert = mAlertBuilder.create();
			alert.setTitle("Open Failed");
			alert.setButton(AlertDialog.BUTTON_POSITIVE, "Dismiss",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					stopSelf();
				}
			});

			Log.d(TAG,"Dismiss");
			return null;
		}

		createUI();

		mPageNumber = 0;
		drawPDFPage(mPageNumber);

		return myMessenger.getBinder();
	}

	/* The core rendering instance */
	private MuPDFCore    core;
	private String       mFileName;
	private EditText     mPasswordView;
	private AlertDialog.Builder mAlertBuilder;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG,"onStart");
		return super.onStartCommand(intent, flags, startId);
	}


	public void drawPDFPage(int page){
		PointF size = core.getPageSize(page);

		float mSourceScale = Math.min(WIDTH/size.x, HEIGHT/size.y);
		Point newSize = new Point((int)(size.x*mSourceScale), (int)(size.y*mSourceScale));
		Point mSize = newSize;
		Log.d("SIZE", ""+mSize.x+" "+mSize.y);
		if (mEntireBm == null || mEntireBm.getWidth() != newSize.x
				|| mEntireBm.getHeight() != newSize.y) {
			mEntireBm = Bitmap.createBitmap(mSize.x, mSize.y, Bitmap.Config.ARGB_8888);
		}

		core.drawPage(page, mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y);
	}


	private MuPDFCore openFile(String path)
	{
		int lastSlashPos = path.lastIndexOf('/');
		mFileName = new String(lastSlashPos == -1
				? path
						: path.substring(lastSlashPos+1));
		System.out.println("Trying to open "+path);
		try
		{
			core = new MuPDFCore(path);
			// New file: drop the old outline data
		}
		catch (Exception e)
		{
			System.out.println(e);
			return null;
		}
		return core;
	}


	public void requestPassword() {
		mPasswordView = new EditText(this);
		mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

		AlertDialog alert = mAlertBuilder.create();
		alert.setTitle("Enter Password");
		alert.setView(mPasswordView);
		alert.setButton(AlertDialog.BUTTON_POSITIVE, "Ok",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (core.authenticatePassword(mPasswordView.getText().toString())) {
					createUI();
				} else {
					requestPassword();
				}
			}
		});
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				stopSelf();
			}
		});

		alert.show();
	}

	public void createUI() {
		if (core == null)
			return;
		mPageNumber = 0;
	}

	public Object onRetainNonConfigurationInstance()
	{
		MuPDFCore mycore = core;
		core = null;
		return mycore;
	}

	public void onDestroy()
	{
		if (core != null)
			core.onDestroy();
		core = null;
		super.onDestroy();
	}
}
