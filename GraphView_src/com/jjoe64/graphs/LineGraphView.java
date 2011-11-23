package com.jjoe64.graphs;

import java.util.Date;
import java.util.List;
import java.util.WeakHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;

/**
 * Line Graph View. This draws a line chart.
 * 
 * @author jjoe64 - jonas gehring - http://www.jjoe64.com
 * 
 *         Copyright (C) 2011 Jonas Gehring Licensed under the GNU Lesser
 *         General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
public class LineGraphView extends GraphView {

	private static final String DEBUG_TAG = LineGraphView.class.getPackage().toString();
	private static final float CIRCLE_RADIUS = 3f;
	
	/** used to draw a circle at each point */
	private Paint mCirclePaint;
	private Paint mCircleOuterPaint;

	/** used to fill the area below the graph line */
	private Paint mFillPaint;

	/**
	 * the graph line is drawn using two lines. one outer line which is thicker
	 * and darker than the inner lighter line
	 */
	private Paint mInnerPaint;
	private Paint mOuterPaint;

	/** transforms data points into screen points **/
	private final Matrix mViewPortMatrix = new Matrix();

	/** if true the area below the graph line will be filled */
	private boolean drawBackground = false;

	/** if true graph line will be smoothed by a quadratic fit function */
	private boolean mSmoothLine = false;

	/** helpers to avoit 'new' during draw calls */
	private final float[] mPoints = new float[2];
	private Path mPath = new Path();
	private Path mCirclesPath = new Path();
	private Path mClosedPath = new Path();
	private CornerPathEffect mPathEffect = new CornerPathEffect(5);

	/** used for formatting the labels */
	private java.text.DateFormat mDateFormat = null;
	private Date mDate = new Date();
	private String mYUnit=null;
	
	
	private float[] mSrcPoints; 
	private float[] mDestPoints; 

	WeakHashMap<GraphViewSeries, Path> mCachedPath = new WeakHashMap<GraphViewSeries, Path>();

	private void init() {

		mFillPaint = new Paint() {
			{
				setStyle(Paint.Style.FILL);
				setARGB(255, 20, 40, 60);
			}
		};

		mInnerPaint = new Paint() {
			{
				setStyle(Paint.Style.STROKE);
				setStrokeCap(Paint.Cap.ROUND);
				setStrokeWidth(1.5f);
				setAntiAlias(true);
			}
		};

		mOuterPaint = new Paint() {
			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setStrokeWidth(3.0f);
				setStrokeCap(Cap.ROUND);
			}
		};

		mCirclePaint = new Paint() {
			{
				setStyle(Paint.Style.FILL);
				setAntiAlias(true);
			}
		};
		
		mCircleOuterPaint = new Paint(){{
			setStyle(Paint.Style.STROKE);
			setAntiAlias(true);
			setStrokeWidth(CIRCLE_RADIUS+.5f);
			setStrokeCap(Cap.ROUND);
			}
		};
	}

	public LineGraphView(Context context) {
		super(context);
		init();
	}

	public LineGraphView(Context context, AttributeSet set) {
		super(context, set);
		init();
	}
	
	public void setYUnit(final String unit){
		this.mYUnit = unit;
	}

	

	/**
	 * returns the color value which is used to draw the outer path the graph
	 * line
	 * 
	 * @param innerColor
	 * @return color which is half as dark as innerColor
	 */
	private int calculateOuterColor(int innerColor) {
		int a = Color.alpha(innerColor);
		int r = Color.red(innerColor);
		int g = Color.green(innerColor);
		int b = Color.blue(innerColor);
		return Color.argb(a, r >> 1, g >> 1, b >> 1);
	}

	/**
	 * returns the color value which is used to fill the area below graph line
	 * 
	 * @param innerColor
	 * @return transparent version of innerColor
	 */
	private int calculateFillColor(int innerColor) {
		int a = Color.alpha(innerColor);
		int r = Color.red(innerColor);
		int g = Color.green(innerColor);
		int b = Color.blue(innerColor);
		return Color.argb(a >> 1, r, g, b);
	}

	Bitmap b = null;

	@Override
	public void drawSeries(Canvas canvas, int color, List<GraphViewData> values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart) {
		long startTime = System.currentTimeMillis();
		
		if (mSrcPoints==null){
			mSrcPoints = new float[values.size()*2];
			mDestPoints= new float[values.size()*2];
		} else if (mSrcPoints.length < values.size()*2) {
			mSrcPoints = new float[values.size()*2];
			mDestPoints= new float[values.size()*2];
		} 
				
		mInnerPaint.setColor(color);
		mOuterPaint.setColor(calculateOuterColor(color));
		mFillPaint.setColor(calculateFillColor(color));
		mCirclePaint.setColor(color);
		mCircleOuterPaint.setColor(calculateOuterColor(color));
		
		/*transform data points into screen space*/
		mViewPortMatrix.reset();
		//1. scale
		//mViewPortMatrix.postTranslate((float)-minX, (float)-minY);
		mViewPortMatrix.postScale((float)(graphwidth / diffX),(float)(graphheight / diffY));
		//2. flip vertically
		mViewPortMatrix.postScale(1,-1);
		mViewPortMatrix.postTranslate(0,graphheight);
		//3. adjust for borders
		mViewPortMatrix.postTranslate(horstart, border);
		double yVal,xVal;
		final boolean isManualY = isManualYAxisBounds();
		final double manualMaxY = getManualMaxYValue();
		final double manualMinY = getManualMinYValue();
		int pointIndex = 0;
		
		for (GraphViewData data : values) {
			xVal = data.valueX;
			yVal = data.valueY;
			
			/*clamp to bounds*/
			if (isManualY) { 
				if (yVal>manualMaxY){
					yVal = manualMinY;
				} else if (yVal<manualMinY){
					yVal = manualMinY;
				}				
			}
			final double floatX = xVal-minX;
			final double floatY = yVal-minY;
			if (floatX>Float.MAX_VALUE){
				Log.w(DEBUG_TAG,"X value to large to convert to float");
			}
			
			mSrcPoints[pointIndex++] = (float)(floatX);
			mSrcPoints[pointIndex++] = (float)(floatY);
		}
		mViewPortMatrix.mapPoints(mDestPoints, 0, mSrcPoints, 0, values.size());
		

		int count = values.size()*2;
		if (count % 4 >0){
			count -= 2;
		}
		canvas.drawLines(mDestPoints,0,count, mInnerPaint);
		count = (values.size()<<1)-2;
		if (count % 4 >0){
			count -= 2;
		}		
		canvas.drawLines(mDestPoints,2,count, mInnerPaint);
	}
	
	private Date mLastFormattedDate;

	@Override
	protected String formatLabel(double value, boolean isValueX) {
		if (isValueX) {
			if (mDateFormat == null) {				
				mDateFormat = DateFormat.getTimeFormat(getContext());
			}
			mDate.setTime((long) value);
			return mDateFormat.format(mDate);
		} else {
			String result = super.formatLabel(value, isValueX);
			if (mYUnit!=null){
				result = result + " " + mYUnit;
			}
			return result;
		}
	}
	
	/**
	 * 
	 * @return if true, graph line will be smoothed using a quadratic fit
	 *         function
	 */
	public boolean getSmoothing() {
		return mSmoothLine;
	}

	/**
	 * 
	 * @param value
	 *            true to smooth the graph line with a quadratic fit function
	 */
	public void setSmoothing(boolean value) {
		this.mSmoothLine = value;
	}

	public boolean getDrawBackground() {
		return drawBackground;
	}

	/**
	 * @param drawBackground
	 *            true to fill the area below the graph line
	 */
	public void setDrawBackground(boolean drawBackground) {
		this.drawBackground = drawBackground;
	}
}
