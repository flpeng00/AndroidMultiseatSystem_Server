package org.secmem.amsserver.Processes;

import java.util.ArrayList;

import org.secmem.amsserver.CommonData;
import org.secmem.amsserver.FBController;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class DocumentProcess {
	private FBController fbController;
	private String TAG = "DocumentProcess";
	private Context context;

	public DocumentProcess(Context c) {
		fbController=new FBController();
		context = c;
	}
	public ArrayList<CommonData> mGetBPdfList() {
		ContentResolver cr = context.getContentResolver();
		Uri uri = MediaStore.Files.getContentUri("external");

		String[] projection = null;

		String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
				+ MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;
		String[] selectionArgs = null; 

		String sortOrder = null; 

		String selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE
				+ "=?";
		String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
				"pdf");
		String[] selectionArgsPdf = new String[] { mimeType };
		Cursor allPdfFiles = cr.query(uri, projection, selectionMimeType,
				selectionArgsPdf, sortOrder);

		CommonData mTempCommonData;
		ArrayList<CommonData> mTempPdfList = new ArrayList<CommonData>();

		if (allPdfFiles != null && allPdfFiles.moveToFirst()) {
			do {
				mTempCommonData = new CommonData();

				mTempCommonData.setId(allPdfFiles.getLong(allPdfFiles
						.getColumnIndex((MediaStore.Files.FileColumns._ID))));
				mTempCommonData.setFilePath(allPdfFiles.getString(allPdfFiles
						.getColumnIndex((MediaStore.Files.FileColumns.DATA))));
				mTempCommonData.setTitle(allPdfFiles.getString(allPdfFiles
						.getColumnIndex((MediaStore.Files.FileColumns.TITLE))));

				Log.i(TAG, "TITLE : " + mTempCommonData.getTitle());
				Log.i(TAG, "ID : " + mTempCommonData.getId());
				Log.i(TAG, "FILEPATH	: " + mTempCommonData.getFilePath());

				mTempPdfList.add(mTempCommonData);

			} while (allPdfFiles.moveToNext());
		}
		return mTempPdfList;
	}
}
