package org.secmem.amsserver;

public class CommonData {
	private long mId;
	private String mFilePath;
	public String mTitle;
//	public Bitmap mThumbnail;
//	public long mVideoSize;
//	public long mVideoDuration;
//	public String mVideoResolution;

	public CommonData(){
		mId 		= 0;
		mFilePath 	= null;
		mTitle 	= null;
		//mVideoResolution	= null;
		//mVideoSize 		= 0;
		//mVideoDuration 	= 0;
		
	}
	
	public void setId(long id){
		mId = id;
	}
	
	public void setTitle(String title){
		mTitle = title;
	}
	
	public void setFilePath(String filePath){
		mFilePath = filePath;
	}
	
	public long getId(){
		return mId;
	}
	
	public String getFilePath(){
		return mFilePath;
	}
	
	public String getTitle(){
		return mTitle;
	}
	
}