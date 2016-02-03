package org.secmem.amsserver.Processes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.secmem.amsserver.CommonData;
import org.secmem.amsserver.FBController;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.provider.MediaStore;
import android.util.Log;

public class ImageProcess {
	
	private String TAG = "ImageProcess";
	private final int SCREEN_WIDTH = 1280;
	private final int SCREEN_HEIGHT = 720;
	private Context context;
	
	public ImageProcess(Context c){
		context = c;
	}
	
	public boolean setImageToFrameBuffer(String filePath){
		Log.i(TAG, "sendBitmapToFb : " + filePath);
		File imgFile = new File(filePath);
		FileInputStream fileStream;
		try {
			fileStream = new FileInputStream(imgFile);
			Bitmap sourceBitmap = BitmapFactory.decodeStream(fileStream);
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

				FBController.setBitmap(canvasBitmap);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Log.i(TAG, "setBitmapToFb : Error");
			return false;
		}
		return true;
	}
	
	public ArrayList<CommonData> mGetBImageList(){
		
		CommonData  mTempCommonData;
		ArrayList<CommonData> mTempImageList = new ArrayList<CommonData>();
		Cursor mImageCursor;
		String[] proj = {
				MediaStore.Images.Media._ID,
				MediaStore.Images.Media.DATA,
				MediaStore.Images.Media.TITLE
		};

		mImageCursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);


		if(mImageCursor!=null && mImageCursor.moveToFirst())
		{
			do{
				mTempCommonData =   new CommonData();

				mTempCommonData.setId(mImageCursor.getLong(mImageCursor. getColumnIndex(MediaStore.Images.Media._ID)));
				mTempCommonData.setFilePath(mImageCursor.getString(mImageCursor. getColumnIndex(MediaStore.Images.Media.DATA)));
				mTempCommonData.setTitle(mImageCursor.getString(mImageCursor.getColumnIndex(MediaStore.Images.Media.TITLE)));
				
				Log.i(TAG,  "ID			: "+mTempCommonData.getId());
				Log.i(TAG,  "FILEPATH	: "+mTempCommonData.getFilePath());
				
				mTempImageList.add(mTempCommonData);

			}while(mImageCursor.moveToNext());
		}    
		return mTempImageList;
	}
}
