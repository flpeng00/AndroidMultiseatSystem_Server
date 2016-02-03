package org.secmem.amsserver.Processes;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.secmem.amsserver.AMSService;
import org.secmem.amsserver.CommonData;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;

public class AudioProcess {
	
	private Process p;
	private DataOutputStream os = null;
	Context context;
	private static String TAG = "AMSService";
	private static final int PORT = 16000;
	
	public AudioProcess(Context c){
		context = c;
	}
	
	public void kill(){
		if(p!=null && os!=null){
			try {
				os.close();
				p.destroy();
				p = null;
				os = null;
				Log.i(TAG, "AudioProcess : Killed");
			} catch (IOException e) {
				Log.i(TAG, "AudioProcess : Killing Failed");
				e.printStackTrace();
			}
		}
	}
	
	public void play(final String filePath, final String serverIP, final DataOutputStream systemOS){
		
		kill();
		Thread mThread = new Thread(){
			@Override
			public void run() {
				try {
					Log.i(TAG, "AudioFilePath : " + filePath);
					
					p = Runtime.getRuntime().exec("su");
					Log.i(TAG, "Audio : Play");
					os = new DataOutputStream(p.getOutputStream());
					os.writeBytes("ffmpeg -i \""+filePath+"\" -ac 2 -r 20 -vn -f mpegts tcp://"
							+ serverIP + ":" + PORT + "\n");
					os.flush();
					os.close();
					p.waitFor();
					Log.i(TAG, "Audio : End");
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
	
	public ArrayList<CommonData> mGetBAudioList(){
		CommonData  mTempCommonData;
		ArrayList<CommonData> mTempAudioList = new ArrayList<CommonData>();
		Cursor mAudioCursor;
		String[] proj = {
				MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.DATA,
				MediaStore.Audio.Media.TITLE
		};

		mAudioCursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null);


		if(mAudioCursor!=null && mAudioCursor.moveToFirst())
		{
			do{
				mTempCommonData =   new CommonData();

				mTempCommonData.setId(mAudioCursor.getLong(mAudioCursor. getColumnIndex(MediaStore.Audio.Media._ID)));
				mTempCommonData.setFilePath(mAudioCursor.getString(mAudioCursor. getColumnIndex(MediaStore.Audio.Media.DATA)));
				mTempCommonData.setTitle(mAudioCursor.getString(mAudioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
				
				Log.i(TAG,  "ID			: "+mTempCommonData.getId());
				Log.i(TAG,  "FILEPATH	: "+mTempCommonData.getFilePath());
				
				mTempAudioList.add(mTempCommonData);

			}while(mAudioCursor.moveToNext());
		}    
		return mTempAudioList;
	}

}
