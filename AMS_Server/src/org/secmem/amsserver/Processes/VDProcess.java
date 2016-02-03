package org.secmem.amsserver.Processes;

import java.io.DataOutputStream;
import java.io.IOException;

import android.util.Log;

public class VDProcess {
	
	private Process p;
	private DataOutputStream os = null;
	private static String TAG = "AMSService";
	private static final int PORT = 15000;
	
	public void kill(){
		if(p!=null && os!=null){
			try {
				os.close();
				p.destroy();
				p = null;
				os = null;
				Log.i(TAG, "VDProcess : Killed");
			} catch (IOException e) {
				Log.i(TAG, "VDProcess : Killing Failed");
				e.printStackTrace();
			}
		}
	}
	
	public void play( final String serverIP ){
		kill();
		Thread mThread = new Thread(){
			@Override
			public void run() {
				try {
					p = Runtime.getRuntime().exec("su");
					os = new DataOutputStream(p.getOutputStream());
					os.writeBytes("ffmpeg -f fbdev -an -r 40 -i /dev/graphics/fb5 -f mpegts -vb 2M -s 1280x720 udp://" + 
							serverIP + ":" + PORT+"\n");
					os.flush();
					os.close();
					p.waitFor();
					Log.i(TAG, "Virtual Display Start");			
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		mThread.start();
	}
}
