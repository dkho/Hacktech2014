package com.ripecho.hacktech2014;

import java.util.Stack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class PixelGridView extends View implements View.OnClickListener{
	private DrawActivity parent;
	private Paint linePaint;
	private Paint toolPaint;
	private Paint bgPaint;
	private float szX;
	private float szY;
	private int numPxX;
	private int numPxY;
	private float width = 1;
	private float height = 1;
	private ScaleGestureDetector sgd;
	
	
	private static final float SOME_CONSTANT_TBD = .5f;
	private static final int INVALID_POINTER_ID = -1;
	private int mActivePointerId = INVALID_POINTER_ID;
	private float mPosX;
	private float mPosY;
	private float mLastTouchX;
	private float mLastTouchY;
	private float mScaleFactor = 1.f;
	private Bitmap gridMap; 

	private Matrix inverse;
	
	public PixelGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		parent = (DrawActivity) context;
		linePaint = new Paint();
		toolPaint = new Paint();
		bgPaint = new Paint();
		
		linePaint.setColor(Color.BLACK);
		linePaint.setStrokeWidth(0);
		toolPaint.setColor(parent.getColor());
		bgPaint.setColor(Color.LTGRAY);
		bgPaint.setStyle(Paint.Style.FILL);
		setBackgroundColor(Color.DKGRAY);
		sgd = new ScaleGestureDetector(context, new ScaleListener());
		
		this.setOnClickListener(this);
		gridMap = parent.getBitmap();
	}

	public boolean onTouchEvent(MotionEvent ev) {
		
	    // Let the ScaleGestureDetector inspect all events.
	    sgd.onTouchEvent(ev);
	    
	    final int action = ev.getAction();
	    switch (action & MotionEvent.ACTION_MASK) {
	    case MotionEvent.ACTION_DOWN: {
	        final float x = ev.getX();
	        final float y = ev.getY();
	        
	        mLastTouchX = x;
	        mLastTouchY = y;
	        mActivePointerId = ev.getPointerId(0);
	        break;
	    }
	        
	    case MotionEvent.ACTION_MOVE: {
	        final int pointerIndex = ev.findPointerIndex(mActivePointerId);
	        final float x = ev.getX(pointerIndex);
	        final float y = ev.getY(pointerIndex);

	        // Only move if the ScaleGestureDetector isn't processing a gesture.
	        if (!sgd.isInProgress() && ev.getPointerCount() > 1) {
	            final float dx = x - mLastTouchX;
	            final float dy = y - mLastTouchY;

	            mPosX += dx;
	            mPosY += dy;

	            invalidate();
	        }

	        mLastTouchX = x;
	        mLastTouchY = y;

	        break;
	    }
	        
	    case MotionEvent.ACTION_UP: {
	    	if (ev.findPointerIndex(mActivePointerId) != -1)
	    	{
		    	int pointerIndex = ev.findPointerIndex(mActivePointerId);
		    	Log.d("DEBUG", "x: " + convertX(ev.getX(pointerIndex)));
	            Log.d("DEBUG", "y: " + convertY(ev.getY(pointerIndex)));
	            updateBitmap(convertX(ev.getX(pointerIndex)), convertY(ev.getY(pointerIndex)), parent.getBitmap(), parent.getColor());
	    	}
	        mActivePointerId = INVALID_POINTER_ID;
	        break;
	    }
	        
	    case MotionEvent.ACTION_CANCEL: {
	        mActivePointerId = INVALID_POINTER_ID;
	        break;
	    }
	    
	    case MotionEvent.ACTION_POINTER_UP: {
	        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) 
	                >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	        final int pointerId = ev.getPointerId(pointerIndex);
	        if (pointerId == mActivePointerId) {
	            // This was our active pointer going up. Choose a new
	            // active pointer and adjust accordingly.
	            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
	            mLastTouchX = ev.getX(newPointerIndex);
	            mLastTouchY = ev.getY(newPointerIndex);
	            mActivePointerId = ev.getPointerId(newPointerIndex);
	        }
	        break;
	    }
	    }
	    
	    return true;
	}
	
	public void setParent(DrawActivity p){
		parent = p;
	}
	
	protected void onSizeChanged(int w, int h, int oldw, int oldh){
		width = (float)w;
		height = (float)h;
		//setDrawingArea(64,64);
		setDrawingArea(parent.getBitmap().getWidth(), parent.getBitmap().getHeight());
	}
	
	private void setDrawingArea(int x, int y){
		numPxX = x;
		numPxY = y;
		szX = width/x;
		if(szX*y > height) szY = szX = height/y;
		else szY = szX;
	}
	
	private int convertX(float x){
		/*float temp = x/width;
		temp = temp*numPxX;
		return (int)temp;*/
		return (int)((x - mPosX / mScaleFactor) / mScaleFactor / szX);
		//return (int)x;
	}
	
	private int convertY(float y){
		/*float temp = y/height;
		temp = temp*numPxY;
		return (int)temp;*/
		return (int)((y - mPosY / mScaleFactor) / mScaleFactor / szY);
	}
	
	private void convertCoord(float x, float y)
	{
		float[] input = new float[9];
		input[0] = x;
		input[4] = y;
		input[8] = 1;
		
	}
	
	private void updateBitmap(float x, float y, Bitmap bmp, int col){
		if ((int)x >= 0 && (int)x < parent.getBitmap().getWidth() && (int)y >= 0 && (int)y < parent.getBitmap().getHeight())
		{
			if (parent.getTool() == Tool.PENCIL)
				bmp.setPixel((int)x, (int)y, col);
			else if (parent.getTool() == Tool.ERASER)
				bmp.setPixel((int)x, (int)y, Color.TRANSPARENT);
			else if (parent.getTool() == Tool.EYE_DROPPER)
			{
				parent.setColor(bmp.getPixel((int)x, (int)y));
				parent.setTool(Tool.PENCIL);
			}
			else
			{
				int baseCol = bmp.getPixel((int)x, (int)y);
				Stack<Coord> s = new Stack<Coord>();
				s.push(new Coord((int)x, (int)y));
				while (!s.empty())
				{
					Coord p = s.pop();
					if (p.x < 0 || p.x >= bmp.getWidth() || p.y < 0 || p.y >= bmp.getHeight())
						continue;
					if (bmp.getPixel(p.x, p.y) == baseCol)
					{
						bmp.setPixel(p.x, p.y, parent.getColor());
						s.push(new Coord(p.x + 1, p.y));
						s.push(new Coord(p.x - 1, p.y));
						s.push(new Coord(p.x, p.y + 1));
						s.push(new Coord(p.x, p.y - 1));
					}
				}
			}
		}
	}
	
	private class Coord
	{
		public int x, y;
		public Coord(int x, int y)
		{
			this.x = x;
			this.y = y;
		}
	}
	
	public void onClick(View v){
		updateBitmap(v.getX(),v.getY(),gridMap,parent.getColor());
	}
	
	protected void onDraw(Canvas canvas){
		super.onDraw(canvas);
		
		canvas.save();
		canvas.scale(mScaleFactor, mScaleFactor, width/2, height/2);
		//canvas.scale(mScaleFactor, mScaleFactor);
		canvas.translate(mPosX/(mScaleFactor), mPosY/(mScaleFactor));
		canvas.drawRect(0, 0, szX*numPxX, szY*numPxY, bgPaint);
		for (int i = 0; i < parent.getBitmap().getWidth(); i++)
		{
			for (int j = 0; j < parent.getBitmap().getHeight(); j++)
			{
				toolPaint.setColor(parent.getBitmap().getPixel(i, j));
				canvas.drawRect(i * szX, j * szY, (i + 1) * szX, (j + 1) * szY, toolPaint);
			}
		}
		
		for(float i = 0; i <= szX*numPxX; i += szX){
			canvas.drawLine(i, 0, i, szY*numPxY, linePaint);
			
		}
		for(float i = 0; i <= szY*numPxY; i += szY){
			canvas.drawLine(0, i, szX*numPxX, i, linePaint);
		}
		//canvas.getMatrix().invert(inverse);
		canvas.restore();
		
		invalidate();
	}
	
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
	    @Override
	    public boolean onScale(ScaleGestureDetector detector) {
	    	mScaleFactor *= detector.getScaleFactor();
	        
	        // Don't let the object get too small or too large.
	        mScaleFactor = Math.max((8/numPxX), Math.min(mScaleFactor, numPxX/6f));
	        if(mScaleFactor>SOME_CONSTANT_TBD)
	        	setDrawingArea(parent.getBitmap().getWidth(), parent.getBitmap().getHeight());
	        	
	        invalidate();
	        return true;
	    }
	}
	
	public void updateSize()
	{
		setDrawingArea(parent.getBitmap().getWidth(), parent.getBitmap().getHeight());
	}
}


