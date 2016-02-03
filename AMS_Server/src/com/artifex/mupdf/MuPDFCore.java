package com.artifex.mupdf;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.secmem.amsserver.FBController;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Environment;
import android.util.Log;

public class MuPDFCore
{
	/* load our native library */
	static {
		System.loadLibrary("mupdf");
	}
	private FBController fbController;
	/* Readable members */
	private final int SCREEN_WIDTH = 1280;  
	private final int SCREEN_HEIGHT = 720;
	private int pageNum  = -1;
	private int numPages = -1;
	public  float pageWidth;
	public  float pageHeight;
	public String TAG="MuPDFCore";
	/* The native functions */
	private static native int openFile(String filename);
	private static native int countPagesInternal();
	private static native void gotoPageInternal(int localActionPageNum);
	private static native float getPageWidth();
	private static native float getPageHeight();
	public static native void drawPage(Bitmap bitmap,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH);
	public static native RectF[] searchPage(String text);
	public static native int getPageLink(int page, float x, float y);
	public static native LinkInfo [] getPageLinksInternal(int page);
	public static native boolean hasOutlineInternal();
	public static native boolean needsPasswordInternal();
	public static native boolean authenticatePasswordInternal(String password);
	public static native void destroying();

	public MuPDFCore(String filename) throws Exception
	{
		//fbController = new FBController();
		if (openFile(filename) <= 0)
		{
			throw new Exception("Failed to open "+filename);
		}
	}

	public  int countPages()
	{
		if (numPages < 0)
			numPages = countPagesSynchronized();

		Log.d(TAG, "" + numPages);
		return numPages;
	}

	private synchronized int countPagesSynchronized() {
		return countPagesInternal();
	}

	/* Shim function */
	public void gotoPage(int page)
	{
		Log.d(TAG, "" + page);
		if (page > numPages-1)
			page = numPages-1;
		else if (page < 0)
			page = 0;
		if (this.pageNum == page)
			return;
		gotoPageInternal(page);
		this.pageNum = page;
		this.pageWidth = getPageWidth();
		this.pageHeight = getPageHeight();
	}

	public synchronized PointF getPageSize(int page) {
		gotoPage(page);
		return new PointF(pageWidth, pageHeight);
	}

	public synchronized void onDestroy() {
		destroying();
	}


	int count = 0;
	public boolean setImageToFrameBuffer(Bitmap bitmap){
		
		//fileStream = new FileInputStream(imgFile);
		Bitmap sourceBitmap = bitmap;
		Bitmap canvasBitmap = Bitmap.createBitmap(SCREEN_WIDTH, SCREEN_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(canvasBitmap);
		int sourceWidth = sourceBitmap.getWidth();
		int sourceHeight = sourceBitmap.getHeight();
		float resizeRate = 1;
		
		if (sourceWidth > SCREEN_WIDTH | sourceHeight > SCREEN_HEIGHT){
			if((sourceWidth/sourceHeight) > (SCREEN_WIDTH/SCREEN_HEIGHT)){
				resizeRate = SCREEN_WIDTH / (float)sourceWidth;
				Log.i(TAG, "Resize Rate : " + resizeRate);
				Log.i(TAG, "Resize : SCREEN_WIDTH, " + sourceHeight * resizeRate);
				sourceBitmap = Bitmap.createScaledBitmap(sourceBitmap, SCREEN_WIDTH, (int) (sourceHeight * resizeRate), true);
			} else{
				resizeRate = SCREEN_HEIGHT / (float)sourceHeight;
				Log.i(TAG, "Resize Rate : " + resizeRate);
				Log.i(TAG, "Resize : " + sourceWidth * resizeRate + ", SCREEN_HEIGHT");
				sourceBitmap = Bitmap.createScaledBitmap(sourceBitmap, (int)(resizeRate * sourceWidth), SCREEN_HEIGHT, true);
			}
		}
		canvas.drawBitmap(sourceBitmap, (SCREEN_WIDTH - sourceBitmap.getWidth())/2, (SCREEN_HEIGHT-sourceBitmap.getHeight())/2, null);
		
		//if(fbController.openFrameBuffer("5") == -1)
		//	return false;

		//else{
			Log.d(TAG,"setbitmap");
			FBController.setBitmap(canvasBitmap);
		//}
		return true;
	}
	
	public synchronized void drawPage(int page, Bitmap bitmap,
			int pageW, int pageH,
			int patchX, int patchY,
			int patchW, int patchH) {
		gotoPage(page);

		drawPage(bitmap, pageW, pageH, patchX, patchY, patchW, patchH);
		//Thread.sleep(10000);
		//Log.d("bitmapdata",""+bitmap.getHeight()+" "+bitmap.getWidth()+" "+bitmap.getByteCount()+" ");
		//Log.d("dd","dd");
		setImageToFrameBuffer(bitmap);
		
		//Log.d("d1d","dd1");
		/*try {
			count += 1;
			String path = Environment.getExternalStorageDirectory()
					.toString();
			OutputStream fOut = null;
			Log.i("TEST", "Path is " + path
					+ "/multiseat_v/"
					+ "screentest" + count + ".jpg");
			File file = new File(path
					+ "/multiseat_v/", "screentest"
							+ count + ".jpg");
			fOut = new FileOutputStream(file);

			bitmap.compress(
					Bitmap.CompressFormat.JPEG, 85,
					fOut);
			fOut.flush();
			fOut.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}

	public synchronized int hitLinkPage(int page, float x, float y) {
		return getPageLink(page, x, y);
	}

	public synchronized LinkInfo [] getPageLinks(int page) {
		return getPageLinksInternal(page);
	}

	public synchronized RectF [] searchPage(int page, String text) {
		gotoPage(page);
		return searchPage(text);
	}

	public synchronized boolean hasOutline() {
		return hasOutlineInternal();
	}

	public synchronized boolean needsPassword() {
		return needsPasswordInternal();
	}

	public synchronized boolean authenticatePassword(String password) {
		return authenticatePasswordInternal(password);
	}
}
