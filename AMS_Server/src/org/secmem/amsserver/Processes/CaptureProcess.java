package org.secmem.amsserver.Processes;

import java.io.DataOutputStream;
import java.io.IOException;

import org.secmem.amsserver.AMSService;

import android.util.Log;

public class CaptureProcess {
	
	private Process p;
	private DataOutputStream os = null;
	private static String TAG = "AMSService";
	
	public void play() throws IOException, InterruptedException{
		
		p = Runtime.getRuntime().exec("su");
		os = new DataOutputStream(p.getOutputStream());
		os.writeBytes("chmod 777 /dev/graphics/fb5\n");
		os.flush();
		os.writeBytes("screencap -p " + AMSService.AppfilePath + "/capture.png\n");
		os.flush();
		os.writeBytes("chmod 777 " + AMSService.AppfilePath + "/capture.png\n");
		os.flush();
		os.close();
		p.waitFor();
	}
		
		
	public void kill(){
		if(p!=null && os!=null){
			try {
				os.close();
				p.destroy();
				p = null;
				os = null;
				Log.i(TAG, "CaptureProcess : Killed");
			} catch (IOException e) {
				Log.i(TAG, "CaptureProcess : Killing Failed");
				e.printStackTrace();
			}
		}
	}

}
