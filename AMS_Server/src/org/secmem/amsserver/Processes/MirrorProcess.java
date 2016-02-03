package org.secmem.amsserver.Processes;

import java.io.DataOutputStream;
import java.io.IOException;

import android.util.Log;

public class MirrorProcess {
	
	private Process p;
	private DataOutputStream os = null;
	private static String TAG = "MirrorProcess";
	private static final int PORT = 13000;
	
	public void kill(){
		if(p!=null && os!=null){
			try {
				os.close();
				p.destroy();
				p = null;
				os = null;
				Log.i(TAG, "MirrorProcess : Killed");
			} catch (IOException e) {
				Log.i(TAG, "MirrorProcess : Killing Failed");
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
					os.writeBytes("ffmpeg -f fbdev -an -r 40 -i /dev/graphics/fb0 -f mpegts -vb 2M -s 640x360 udp://" 
							+ serverIP + ":" + PORT+"\n");
					os.flush();
					os.close();
					p.waitFor();
					Log.i(TAG, "Mirroring Start");				
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		mThread.start();
	}
}
