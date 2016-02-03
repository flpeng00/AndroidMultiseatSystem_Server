package org.secmem.amsserver.Processes;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.secmem.amsserver.AMSService;
import org.secmem.amsserver.CommonData;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.util.Log;

public class VideoProcess {
	
	private Process p;
	private DataOutputStream os = null;
	private static String TAG = "AMSService";
	private static final int PORT = 14000;
	private Context context;
	
	public VideoProcess(Context c){
		context = c;
	}
	
	public void playIntro(final String AppfilePath){
		Thread mThread = new Thread(){
			@Override
			public void run() {

				try {
					Log.i("TAG", "Start Demo Movie");
					p = Runtime.getRuntime().exec("su");
					os = new DataOutputStream(p.getOutputStream());
					os.writeBytes("mplayer -vo fbdev2:/dev/graphics/fb5 -vf scale=640:360 " + AppfilePath + "/intro_movie.mp4\n");
					os.flush();
					os.close();
					p.waitFor();
					Log.i("TAG", "Intro : Playing");
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		mThread.start();
	}
	
	public void play(final String filePath, final String serverIP){

		kill();
		Thread mThread = new Thread(){
			@Override
			public void run() {
				try {
					Log.i(TAG, "VideoFilePath : " + filePath);
					p = Runtime.getRuntime().exec("su");
					Log.i(TAG, "Video : Play");
					os = new DataOutputStream(p.getOutputStream());
					os.writeBytes("ffmpeg -i \"" + filePath + 
							"\" -ac 2 -r 20 -f mpegts -vb 2M -s 480x270 -async 1 tcp://" + serverIP + ":" + PORT+"\n");
					p.waitFor();
					Log.i("TAG", "Video : End");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		mThread.start();
	}
	
	public ArrayList<CommonData> mGetBVideoList(){
		CommonData  mTempCommonData;
		ArrayList<CommonData> mTempVideoList = new ArrayList<CommonData>();
		Cursor mVideoCursor;
		String[] proj = {
				MediaStore.Video.Media._ID,
				MediaStore.Video.Media.DATA,
				MediaStore.Video.Media.TITLE			
		};

		mVideoCursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);


		if(mVideoCursor!=null && mVideoCursor.moveToFirst())
		{
			do{
				mTempCommonData =   new CommonData();

				mTempCommonData.setId(mVideoCursor.getLong(mVideoCursor. getColumnIndex(MediaStore.Video.Media._ID)));
				mTempCommonData.setFilePath(mVideoCursor.getString(mVideoCursor. getColumnIndex(MediaStore.Video.Media.DATA)));
				mTempCommonData.setTitle(mVideoCursor.getString(mVideoCursor.getColumnIndex(MediaStore.Video.Media.TITLE)));
			
				Log.i(TAG,  "ID			: "+mTempCommonData.getId());
				Log.i(TAG,  "FILEPATH	: "+mTempCommonData.getFilePath());
				
				mMakeVideoThumbnailImg(mTempCommonData.getId());
				mTempVideoList.add(mTempCommonData);

			}while(mVideoCursor.moveToNext());
		}    
		return mTempVideoList;
	}

	public void mMakeVideoThumbnailImg(long id){
		ContentResolver mCrThumb = context.getContentResolver();//test
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 1;
		Bitmap mVideoThumbnailBm = 
				MediaStore.Video.Thumbnails.getThumbnail(mCrThumb,  id,  MediaStore.Video.Thumbnails.MICRO_KIND,  options);
		if(mVideoThumbnailBm == null){
			mVideoThumbnailBm = Bitmap.createBitmap( 96, 96, Bitmap.Config.ARGB_8888  );
			
		}
		try {

			FileOutputStream out = new FileOutputStream(AMSService.AppfilePath+"/videothumbnail/"+id+".jpg");
			mVideoThumbnailBm.compress(Bitmap.CompressFormat.JPEG, 100, out);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public void kill(){
		if(p!=null && os!=null){
			try {
				os.close();
				p.destroy();
				p = null;
				os = null;
				Log.i(TAG, "VideoProcess : Killed");
			} catch (IOException e) {
				Log.i(TAG, "VideoProcess : Killing Failed");
				e.printStackTrace();
			}
		}
	}

}
