
package com.cs.hrstest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DrawHelper extends View {
	private int SCREEN_PIX_MAX_X = 0;
	private int SCREEN_PIX_MIN_X = 0;
	private int SCREEN_PIX_MAX_Y = 0;
	private int SCREEN_PIX_MIN_Y = 0;
	
	private Paint paint = new Paint();
	private Bitmap bitmapCache = null;
	private int screenPreX = 0;
	private int screenPreY = 0;
	private final int COLOR_OUTLINE = Color.argb(0x60, 0x85, 0x85, 0x85);
	private final int COLOR_WAVEFORM = Color.argb(0xff, 0xff, 0x66, 0);
	private final int COLOR_BACKGROUND = Color.argb(0xff, 0xff, 0xff, 0xff);
	private int DRAW_OFF_SET_Y = 400;
	private int DRAW_OFF_SET_X = 10;

	private final int[] WaveformColor = {Color.BLUE, Color.GREEN, Color.RED, Color.argb(0xff, 0xff, 0x66, 0)};

	public DrawHelper(Context context) {
		super(context);
	}
	
	void resetDraw()
	{
		Canvas c = new Canvas(bitmapCache);
		c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		drawOutline(SCREEN_PIX_MIN_X, SCREEN_PIX_MIN_Y, SCREEN_PIX_MAX_X, SCREEN_PIX_MAX_Y);
		screenPreX = DRAW_OFF_SET_X;
		screenPreY = DRAW_OFF_SET_Y;
		paint.setColor(COLOR_WAVEFORM);
		paint.setStrokeWidth(2);
//		drawPoint(screenPreX, screenPreY);
		invalidate();
	}

	
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawBitmap(bitmapCache, 0, 0, paint);
	}

	
	// here will be called as soon as the view's outline is ready
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		SCREEN_PIX_MIN_X = 0;
		SCREEN_PIX_MIN_Y = 0;
		SCREEN_PIX_MAX_X = w;
		SCREEN_PIX_MAX_Y = h;
		paint.setAntiAlias(true);
		bitmapCache = Bitmap.createBitmap(SCREEN_PIX_MAX_X-SCREEN_PIX_MIN_X,
				SCREEN_PIX_MAX_Y-SCREEN_PIX_MIN_Y, Bitmap.Config.ARGB_8888);
		resetDraw();
	}


	void drawWaveform(List<Integer> drawHR)
	{
		List<Integer> useHR = null;
		if (drawHR.size() >= 240)
		{
			useHR = drawHR.subList(drawHR.size()-240, drawHR.size());
		}
		if (drawHR.size() < 240)
		{
			List<Integer> ls=new ArrayList<Integer>();
			for (int i=0; i<240-drawHR.size(); i++)
			{
				ls.add(0); // default value if list have less than 240 records
			}
			for (Integer val : drawHR)
			{
				ls.add(val);
			}
			useHR = ls;
		}
		Canvas c = new Canvas(bitmapCache);

		screenPreX = DRAW_OFF_SET_X;
		screenPreY = DRAW_OFF_SET_Y - useHR.get(0)*2;
		c.drawPoint(screenPreX, screenPreY, paint);
		for (int i=1; i<useHR.size(); i++)
		{
			int nowX = screenPreX+2;
			int nowY = DRAW_OFF_SET_Y-useHR.get(i)*2;
			if ((nowY<DRAW_OFF_SET_Y) && screenPreY<DRAW_OFF_SET_Y){
				c.drawLine(screenPreX, screenPreY, nowX, nowY, paint);
			}
			screenPreX = nowX;
			screenPreY = nowY;
		}
	}



//	void drawHRWaveform(List<Integer> hr)
	void drawHRWaveform(ArrayList<DeviceList.DeviceInfoHolder> devList)
	{
		Canvas c = new Canvas(bitmapCache);
		c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
		drawOutline(SCREEN_PIX_MIN_X, SCREEN_PIX_MIN_Y, SCREEN_PIX_MAX_X, SCREEN_PIX_MAX_Y);
		paint.setColor(COLOR_WAVEFORM);

		for (int i=0; i<devList.size(); i++)
		{
			DeviceList.DeviceInfoHolder item = devList.get(i);
			if (item.hrValList.size() > 0) {
				paint.setColor(WaveformColor[i]);
				c.drawText(item.name, i*100+10, 380, paint);
				drawWaveform(item.hrValList);
			}
		}

		c.drawText("0 dpm", 0, 390, paint);
		c.drawText("200 dpm", 0, 10, paint);
		invalidate();
	}


	// 1) (0,0) is top left corner.
	// 2) (maxX,0) is top right corner
	// 3) (0,maxY) is bottom left corner
	// 4) (maxX,maxY) is bottom right corner
	private void drawOutline(int topLeftX, int topLeftY, int bottomRightX, int bottomRightY) {
		final int lineWidth = 2;
		topLeftY += lineWidth;
		bottomRightX -= lineWidth;
		bottomRightY -= lineWidth;
		final int bottomLeftX = topLeftX;
		final int bottomLeftY = bottomRightY;
		final int topRightX = bottomRightX;
		final int topRightY = topLeftY;

		float orgStrokeWidth = paint.getStrokeWidth();
		paint.setStrokeWidth(lineWidth);

		Canvas c = new Canvas(bitmapCache);
//		c.drawColor(COLOR_BACKGROUND);
		
		paint.setColor(COLOR_OUTLINE);
		c.drawLine(topLeftX, topLeftY, topRightX, topRightY, paint);
		c.drawLine(topRightX, topRightY, bottomRightX, bottomRightY, paint);
		c.drawLine(bottomRightX, bottomRightY, bottomLeftX, bottomLeftY, paint);
		c.drawLine(bottomLeftX, bottomLeftY, topLeftX, topLeftY, paint);
		paint.setStrokeWidth(orgStrokeWidth);
		invalidate();
	}
	

	public DrawHelper(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DrawHelper(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	void myLog(String str)
	{
		Log.i("KTL", "DRAW::" + str);
	}
}
