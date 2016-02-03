package org.secmem.amsserver.Processes;

import java.io.DataOutputStream;
import java.io.IOException;

import org.secmem.amsserver.AMSService;
import org.secmem.amsserver.FBController;

import android.util.Log;

public class InitProcess {
	
	private Process p = null;
	private DataOutputStream os = null;
	private static String TAG = "AMSServer";
	
	public void kill(){
		if(p!=null && os!=null){
			try {
				os.close();
				p.destroy();
				p = null;
				os = null;
				Log.i(TAG, "InitProcess : Killed");
			} catch (IOException e) {
				Log.i(TAG, "InitProcess : Killing Failed");
				e.printStackTrace();
			}
		}
	}
	
	public void play(){
		
		try {
			Log.e(TAG, "Complete");
			p = Runtime.getRuntime().exec("su");
			os = new DataOutputStream(p.getOutputStream());
			os.writeBytes("/system/bin/mount -o remount rw, /data\n");
			os.flush();	
			os.writeBytes("/system/bin/mount -o remount rw, /system\n");
			os.flush();			
			os.writeBytes("chmod 777 /dev/graphics/fb5\n");
			os.flush();
			os.writeBytes("chmod 777 /dev/graphics/fb0\n");
			os.flush();
			os.writeBytes("rm -R "+AMSService.AppfilePath+"/videothumbnail\n");
			os.flush();
			os.writeBytes("mkdir "+AMSService.AppfilePath+"/videothumbnail\n");
			os.flush();
			os.writeBytes("chmod -R 777 "+AMSService.AppfilePath+"/videothumbnail\n");
			os.flush();
			os.writeBytes("cp "+AMSService.AppfilePath+"/ffmpeg /system/bin/ffmpeg\n");
			os.flush();
			os.writeBytes("cp "+AMSService.AppfilePath+"/mplayer /system/bin/mplayer\n");
			os.flush();
			os.writeBytes("chmod 777 /system/bin/ffmpeg\n");
			os.flush();
			os.writeBytes("chmod 777 /system/bin/mplayer\n");
			os.flush();
			os.close();
			p.waitFor();
			FBController.openFrameBuffer("5");
			Log.e(TAG, "Complete");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
