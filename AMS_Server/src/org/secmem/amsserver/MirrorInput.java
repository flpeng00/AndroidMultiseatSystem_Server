package org.secmem.amsserver;

import java.io.DataOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class MirrorInput extends ImageView{

	private String TAG = "AMSService";
	private WindowManager windowManager;
	private WindowManager.LayoutParams serviceParam;
	private Process inputProcess;
	private int x, y;
	private Point screenSize;
	private float xRate, yRate;
	private float xFix, yFix;
	DataOutputStream inputOS;

	public MirrorInput(Context context, Point point) throws IOException{

		super(context);
		screenSize = point;
		Log.d(TAG, "ScreenSize : " + screenSize.x + ", "+ screenSize.y);
		xRate = (float)1/screenSize.x;
		yRate = (float)1/screenSize.y;
		Log.d(TAG, "" + xRate + " " + yRate + " " + 1 * xRate + " " + 1*yRate);
		setImageResource(R.drawable.cursor);
		setScaleType(ScaleType.FIT_XY);
		setVisibility(View.GONE);
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

		serviceParam = new WindowManager.LayoutParams(
				22, 22, 
				WindowManager.LayoutParams.TYPE_PHONE, 
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
				WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
				PixelFormat.TRANSLUCENT);
		serviceParam.gravity = Gravity.LEFT | Gravity.TOP;
		serviceParam.verticalMargin = 0.00f;
		serviceParam.horizontalMargin = 0.00f;
		x = 1;
		y = 1;
		windowManager.addView(this, serviceParam);
		inputProcess = Runtime.getRuntime().exec("su");
		inputOS = new DataOutputStream(inputProcess.getOutputStream());
	}

	public void move(int dx, int dy){
		x = x + dx;
		y = y + dy;
		if(x < 1)
			x = 1;
		else if(x > screenSize.x)
			x = screenSize.x;
		if(y < 1)
			y = 1;
		else if(y > screenSize.y)
			y = screenSize.y;
		serviceParam.horizontalMargin = xRate * x;
		serviceParam.verticalMargin = yRate * y;
		windowManager.updateViewLayout(this, serviceParam);
		Log.d(TAG, "MirrorCursor : Move " + x + ", " + y);
		Log.d(TAG, "MirrorCursor : Move " + xRate * x + ", " + yRate * y);
	}

	public void click(){

		try {
			xFix = x - 1;
			yFix = y - 1;
			Log.d(TAG, "MirrorCursor : Click " + xFix + ", " + yFix);

			inputOS.writeBytes("input tap " + xFix + " " + yFix + "\n");
			inputOS.flush();
			//inputOS.close();
			//inputProcess.waitFor();
			//inputProcess.destroy();
			Log.d(TAG, "MirrorCursor : Clicked");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void keyChar(String string){

		try {
			//Process inputProcess = Runtime.getRuntime().exec("su");
			//DataOutputStream inputOS = new DataOutputStream(inputProcess.getOutputStream());
			inputOS.writeBytes("input keyevent KEYCODE_" + string + "\n");
			inputOS.flush();
			//inputOS.close();
			//inputProcess.waitFor();
			//inputProcess.destroy();
			Log.d(TAG, "MirrorCursor : keyevent/" + string);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void home(){
		try {
			//Process inputProcess = Runtime.getRuntime().exec("su");
			//DataOutputStream inputOS = new DataOutputStream(inputProcess.getOutputStream());
			inputOS.writeBytes("input keyevent KEYCODE_HOME\n");
			inputOS.flush();
			//inputOS.close();
			//inputProcess.waitFor();
			//inputProcess.destroy();
			Log.d(TAG, "MirrorCursor : keyevent/home");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void keySpace(){
		try {
			//Process inputProcess = Runtime.getRuntime().exec("su");
			//DataOutputStream inputOS = new DataOutputStream(inputProcess.getOutputStream());
			inputOS.writeBytes("input keyevent KEYCODE_SPACE\n");
			inputOS.flush();
			//inputOS.close();
			//inputProcess.waitFor();
			//inputProcess.destroy();
			Log.d(TAG, "MirrorCursor : keyevent/space");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void keyBackSpace(){
		try {
			//Process inputProcess = Runtime.getRuntime().exec("su");
			//DataOutputStream inputOS = new DataOutputStream(inputProcess.getOutputStream());
			inputOS.writeBytes("input keyevent KEYCODE_DEL\n");
			inputOS.flush();
			//inputOS.close();
			//inputProcess.waitFor();
			//inputProcess.destroy();
			Log.d(TAG, "MirrorCursor : keyevent/del");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void keyEnter(){
		try {
			inputOS.writeBytes("input keyevent KEYCODE_ENTER\n");
			inputOS.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void back(){
		try {
			//Process inputProcess = Runtime.getRuntime().exec("su");
			//DataOutputStream inputOS = new DataOutputStream(inputProcess.getOutputStream());
			inputOS.writeBytes("input keyevent KEYCODE_BACK\n");
			inputOS.flush();
			//inputOS.close();
			//inputProcess.waitFor();
			//inputProcess.destroy();
			Log.d(TAG, "MirrorCursor : keyevent/back");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void kill() throws IOException{
		inputOS.close();
		inputProcess.destroy();
		Log.d(TAG, "MirrorCursor : Killed");
	}
}
