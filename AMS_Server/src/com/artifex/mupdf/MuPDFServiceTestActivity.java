package com.artifex.mupdf;

import java.io.File;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;

public class MuPDFServiceTestActivity extends Activity implements OnClickListener{

	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.mupdftest);

		findViewById(R.id.plus).setOnClickListener(this);
		findViewById(R.id.minus).setOnClickListener(this);

		File file = new File(Environment.getExternalStorageDirectory()
				.toString() + "/multiseat/test.pdf");
		Uri uri = Uri.parse(file.getPath());
		Intent i = new Intent(getApplicationContext(), MuPDFService.class);
		i.setData(uri);
		i.setAction(Intent.ACTION_VIEW);

		bindService(i, mConnection, Context.BIND_AUTO_CREATE);
	};


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.plus:
		{
			Message msg = Message.obtain(null, MuPDFService.NEXT);
			try {
				myService.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			break;
		}
		case R.id.minus:
		{
			Message msg = Message.obtain(null, MuPDFService.PREV);
			try {
				myService.send(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;
		}
		}
	}

	Messenger myService;
	boolean mBound;

	ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			myService = null;
			mBound = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			myService = new Messenger(service);
			mBound = true;
		}
	};

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}
}
