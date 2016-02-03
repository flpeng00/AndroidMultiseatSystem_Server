package org.secmem.amsserver;

import android.graphics.Bitmap;

public class FBController {
	static {
		System.loadLibrary("fbcon");
	}
	
	public static native int openFrameBuffer(String fbNo);
	public static native void setBitmap(Bitmap bitmap);
	public static native void closeFrameBuffer(String fbNo);	

}
