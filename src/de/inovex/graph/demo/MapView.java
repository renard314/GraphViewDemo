package de.inovex.graph.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import de.inovex.graph.demo.contentprovider.RWELiveDataContentProvider.POWER_TYPE;

public class MapView extends View {
	private final static String sBorderPoints = "M 735.50,258.00M 739.00,273.50M 739.00,278.50M 735.00,313.50M 759.50,387.50M 730.00,390.50M 724.50,403.00M 678.50,426.00M 685.00,456.50M 724.00,490.00M 706.00,513.50M 709.50,531.00M 700.00,542.00M 681.00,537.00M 660.50,549.00M 632.00,553.50M 609.50,551.00M 591.50,548.50M 574.50,535.00M 558.00,535.00M 543.50,544.50M 568.50,474.50M 518.50,465.00M 515.00,440.50M 504.50,426.50M 504.00,404.00M 495.50,398.50M 504.00,368.50M 499.50,358.00M 527.00,336.00M 530.00,317.50M 518.50,300.00M 532.00,279.00M 537.50,268.50M 554.00,268.50M 559.50,271.50M 575.50,264.00M 582.00,250.50M 582.00,237.50M 575.50,217.00M 608.00,215.50M 618.00,233.50M 637.50,240.00M 652.50,243.00M 667.50,245.00M 680.50,230.50M 710.50,223.50M 715.00,239.50";
	@SuppressWarnings(value = { "unused" })
	private final static String DEBUG_TAG = MapView.class.getName();

	public static class Location extends Point implements Comparable<Location> {

		private static class PowerComparator implements Comparator<Location> {
			@Override
			public int compare(Location object1, Location object2) {
				return object1.power - object2.power;
			}
		}

		public static class DistanceComparator implements Comparator<Location> {
			private float mX;
			private float mY;

			public DistanceComparator(final float x, final float y) {
				mX = x;
				mY = y;
			}

			private double calcDistance(Location l) {
				final double dx = l.x - mX;
				final double dy = l.y - mY;
				return Math.sqrt(dx * dx + dy * dy);
			}

			@Override
			public int compare(Location object1, Location object2) {
				return Double.compare(calcDistance(object1), calcDistance(object2));
			}
		}

		private static PowerComparator mPowerComparator = new PowerComparator();
		int production;
		int power;
		POWER_TYPE powerType;
		ContentValues values;

		public static Comparator<Location> getPowerComparator() {
			return mPowerComparator;
		}

		public Location(int production, int power, int x, int y, POWER_TYPE type, ContentValues values) {
			this(production, power, x, y, type);
			this.values = values;
		}

		public Location(int production, int power, int x, int y, POWER_TYPE type) {
			super(x, y);
			this.production = production;
			this.power = power;
			this.powerType = type;
		}

		@Override
		public int compareTo(Location another) {
			return this.x - another.x;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (values == null) {
				values = new ContentValues();
			}
			sb.append("type = " + this.powerType.name() + ", x = " + this.x + ", y = " + this.y + ", power = " + this.power + ", production = " + this.production + ", Values = " + values.toString());
			return sb.toString();
		}
	}

	private List<Location> mLocations = new ArrayList<Location>();
	private Matrix mViewPortMatrix = new Matrix();
	private int minX = Integer.MAX_VALUE;
	private int maxX = Integer.MIN_VALUE;
	private int minY = Integer.MAX_VALUE;
	private int maxY = Integer.MIN_VALUE;
	private int minXBorder = Integer.MAX_VALUE;
	private int maxXBorder = Integer.MIN_VALUE;
	private int minYBorder = Integer.MAX_VALUE;
	private int maxYBorder = Integer.MIN_VALUE;
	private boolean mLocationAdded = false;
	private Paint mCirclePaint; // inner circle
	private Paint mInfoTextPaint; // inner circle
	private Paint mBorderPaint; // map border
	private Paint mCircleBorderPaint; // border of circle
	private Paint mInfoWindowPaint; // border of info window
	private Paint mInfoWindowFillPaint; // fill of info window

	private Paint mTouchedCircleBorderPaint; // border of current touched circle
	private Location mTouchedLocation = null;

	// private Paint mTextPaint;
	private final List<Point> mBorderPoints = new ArrayList<Point>();
	private Path mBorderPath;
	private RectF mBorderClip = new RectF();
	private int mRotation;

	public MapView(Context context) {
		super(context);
		init();
	}

	public MapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		mCirclePaint = new Paint() {
			{
				setStyle(Paint.Style.FILL);
				setAntiAlias(true);
				setColor(Color.WHITE);
			}
		};
		mCircleBorderPaint = new Paint() {
			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setStrokeWidth(2);
				setColor(Color.WHITE);
			}
		};
		mTouchedCircleBorderPaint = new Paint() {
			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setStrokeWidth(2);
				setColor(Color.WHITE);
			}
		};

		mBorderPaint = new Paint() {
			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setStrokeWidth(2);
				setColor(Color.WHITE);
				setPathEffect(new CornerPathEffect(6));
			}
		};

		mInfoWindowPaint = new Paint() {
			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setStrokeWidth(1.5f);
				setColor(Color.WHITE);
			}
		};

		mInfoWindowFillPaint = new Paint() {
			{
				setStyle(Paint.Style.FILL);
				setAntiAlias(true);
				setColor(0xaaffffff);				
			}
		};

		//final Typeface tf = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
		mInfoTextPaint = new Paint(){
			{
				setColor(Color.BLACK);
				setAntiAlias(true);
				setTypeface(Typeface.SANS_SERIF);
				setTypeface(Typeface.DEFAULT_BOLD);
				setTextAlign(Paint.Align.LEFT);
				setTextSize(15);
			}
		};

		parseBorderPoints();
		ValueAnimator anim = ValueAnimator.ofInt(0, 359);
		anim.setDuration(6000);
		anim.setRepeatCount(ValueAnimator.INFINITE);
		anim.addUpdateListener(new AnimatorUpdateListener() {

			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				mRotation = (Integer) animation.getAnimatedValue();
				invalidate();
			}
		});
		// anim.start();
	}

	private void parseBorderPoints() {
		int i = 0;
		String xString, yString;
		int x, y;
		while ((i = sBorderPoints.indexOf('M', i)) > -1) {
			xString = sBorderPoints.substring(i + 2, i + 5);
			yString = sBorderPoints.substring(i + 9, i + 12);
			x = Integer.parseInt(xString) >> 1;
			y = Integer.parseInt(yString) >> 1;
			maxXBorder = Math.max(x, maxXBorder);
			minXBorder = Math.min(x, minXBorder);
			maxYBorder = Math.max(y, maxYBorder);
			minYBorder = Math.min(y, minYBorder);
			mBorderPoints.add(new Point(x, y));
			i += 15;
		}
	}

	public void addLocations(List<Location> locations) {
		for (Location p : locations) {
			addLocation(p);
		}
		invalidate();
	}

	public void addLocation(Location p) {
		float[] mappedPoints = new float[2];
		mappedPoints[0] = p.x;
		mappedPoints[1] = p.y;
		mViewPortMatrix.mapPoints(mappedPoints);

		if (mBorderClip.contains(mappedPoints[0], mappedPoints[1])) {
			if (!mLocations.contains(p)) {
				mLocations.add(p);
				maxX = Math.max(p.x, maxX);
				minX = Math.min(p.x, minX);
				maxY = Math.max(p.y, maxY);
				minY = Math.min(p.y, minY);
				mLocationAdded = true;
			} else {
				int i = mLocations.indexOf(p);
				mLocations.get(i).production = p.production;
			}
			this.invalidate();
		}
	}

	public void addLocation(int production, int maxProduction, int x, int y, POWER_TYPE type, ContentValues values) {
		addLocation(new Location(production, maxProduction, x, y, type, values));
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (changed) {
			updateViewPortMatrix();
			updateBorderPath();
		}
	}

	/**
	 * computes offsets which would center the map in the view
	 * 
	 * @param offsets
	 *            will contain the new offsets
	 * @param scale
	 *            scaling factor which translates location and border points
	 *            into screen space
	 * @param totalMaxX
	 *            maximum X value of all points (border and location)
	 * @param totalMinX
	 *            minimum X value of all points
	 * @param totalMaxY
	 *            maximum Y value of all points
	 * @param totalMinY
	 *            minimum Y value of all points
	 */
	private void determineOffsetsToCenter(float[] offsets, float scale, final int totalMaxX, final int totalMinX, final int totalMaxY, final int totalMinY) {
		final float scaledW = getWidth() / scale;
		final float rightSpace = scaledW - (totalMaxX - totalMinX);
		final float scaledH = getHeight() / scale;
		final float bottomSpace = scaledH - (totalMaxY - totalMinY);

		offsets[0] = (int) (-totalMinX + (rightSpace / 2));
		offsets[1] = (int) (-totalMinY + (bottomSpace / 2));

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		final int action = event.getAction();
		final float[] mappedPoints = new float[2];
		final Matrix inverse = new Matrix();
		mViewPortMatrix.invert(inverse);

		switch (action) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:

			mappedPoints[0] = event.getX();
			mappedPoints[1] = event.getY();
			inverse.mapPoints(mappedPoints);
			Location l = Collections.min(mLocations, new Location.DistanceComparator(mappedPoints[0], mappedPoints[1]));
			final double dx = (l.x - mappedPoints[0]);
			final double dy = (l.y - mappedPoints[1]);

			final double dist = Math.sqrt(dx * dx + dy * dy);
			if (dist < 15) {
				mTouchedLocation = l;
			} else {
				mTouchedLocation = null;
			}
			postInvalidate();
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param totalMaxX
	 * @param totalMinX
	 * @param totalMaxY
	 * @param totalMinY
	 * @return
	 */
	private float determineBestScaleFactor(final int totalMaxX, final int totalMinX, final int totalMaxY, final int totalMinY) {
		final float h = getHeight();
		final float w = getWidth();
		final float diffX = totalMaxX - totalMinX;
		final float diffY = totalMaxY - totalMinY;
		final float ratioScreen = w / h;
		final float ratioPointsX = diffX / diffY;
		final float ratioPointsY = diffY / diffX;
		final float ratioDiffX = Math.abs(ratioScreen - ratioPointsX);
		final float ratioDiffY = Math.abs(ratioScreen - ratioPointsY);
		float scale = 0;
		if (ratioDiffX < ratioDiffY) {
			scale = w / diffX;
		} else {
			scale = h / diffY;
		}
		return scale;
	}

	private void updateViewPortMatrix() {
		float[] offsets = new float[2];
		final int totalMaxX = Math.max(maxX, maxXBorder);
		final int totalMinX = Math.min(minX, minXBorder);
		final int totalMaxY = Math.max(maxY, maxYBorder);
		final int totalMinY = Math.min(minY, minYBorder);

		final float scale = determineBestScaleFactor(totalMaxX, totalMinX, totalMaxY, totalMinY);
		determineOffsetsToCenter(offsets, scale, totalMaxX, totalMinX, totalMaxY, totalMinY);

		/* transforms data points into screen space */
		mViewPortMatrix.reset();
		// 1. put map into center of the view
		mViewPortMatrix.postTranslate(offsets[0], offsets[1]);
		// 2. scale points to screen space
		mViewPortMatrix.postScale(scale, scale);
	}

	private void updateBorderPath() {
		float[] mappedPoints = new float[2];
		mBorderPath = new Path();

		for (int i = 0; i < mBorderPoints.size(); i++) {
			mappedPoints[0] = mBorderPoints.get(i).x;
			mappedPoints[1] = mBorderPoints.get(i).y;
			mViewPortMatrix.mapPoints(mappedPoints);
			if (i > 0) {
				mBorderPath.lineTo(mappedPoints[0], mappedPoints[1]);
			} else {
				mBorderPath.moveTo(mappedPoints[0], mappedPoints[1]);
			}
		}
		mBorderPath.close();
		mBorderPath.computeBounds(mBorderClip, true);
	}

	private void drawInfoField(final float originX, final float originY, final float radius, final Canvas canvas,final Location l) {
		final float paddingTop = 5;
		final float paddingBottom = 5;
		final float paddingLeft = 10;
		final float paddingRight = 3;
		if (l.values != null && l.values.size() > 0) {
			Rect bounds = new Rect();
			int maxKeyLength = 0;
			int maxValueLength = 0;
			int maxTextHeight = 0;
			int lineCount = 0;
			for (String key : l.values.keySet()) {
				String value = l.values.getAsString(key);
				if (value != null) {
					mInfoTextPaint.getTextBounds(key, 0, key.length(), bounds);
					maxKeyLength = Math.max(bounds.width(), maxKeyLength);
					maxTextHeight = Math.max(bounds.height(), maxTextHeight);

					mInfoTextPaint.getTextBounds(value, 0, value.length(), bounds);
					maxValueLength = Math.max(bounds.width(), maxValueLength);
					maxTextHeight = Math.max(bounds.height(), maxTextHeight);
					lineCount++;
				}
			}
			final float extraSpace = mInfoTextPaint.measureText(" : ");
			final float height = maxTextHeight*lineCount + paddingBottom+paddingTop;
			final float width = maxKeyLength + maxValueLength + extraSpace + paddingLeft + paddingRight;
			final float viewHeight = getHeight()-2;
			final RectF infoRect = new RectF(2, viewHeight-height, width, viewHeight );
			
			canvas.drawLine(originX-radius, originY, infoRect.left, infoRect.top+6, mInfoWindowPaint);
			canvas.drawLine(originX+radius, originY, infoRect.right, infoRect.top+6, mInfoWindowPaint);
			canvas.drawRoundRect(infoRect,6,6, mInfoWindowFillPaint);
			canvas.drawRoundRect(infoRect,6,6, mInfoWindowPaint);
			
			float currentLine = viewHeight-height+maxTextHeight + (paddingTop);
			for (String key : l.values.keySet()){
				String value = l.values.getAsString(key);
				if (value!=null){
					canvas.drawText(key, paddingLeft,currentLine,  mInfoTextPaint);
					canvas.drawText(value, maxKeyLength + extraSpace + paddingLeft, currentLine,  mInfoTextPaint);
					currentLine+=maxTextHeight;
				}
			}

		}

	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mLocationAdded) {
			Collections.sort(mLocations);
			mLocationAdded = false;
		}
		final float[] mappedPoints = new float[2];

		canvas.rotate(mRotation, getWidth() / 2, getHeight() / 2);
		canvas.drawPath(mBorderPath, mBorderPaint);

		if (mLocations.size() > 0) {
			final int maxPower = Collections.max(mLocations, Location.getPowerComparator()).power;
			final int minPower = Collections.min(mLocations, Location.getPowerComparator()).power;
			final float linearScale = (20f / (maxPower - minPower));
			final double logRadiusScale = Math.log(20);

			for (Location p : mLocations) {
				if (p.power > 0) {
					mappedPoints[0] = p.x;
					mappedPoints[1] = p.y;
					mViewPortMatrix.mapPoints(mappedPoints);
					double r = p.power * linearScale;
					r = (20 * Math.log(r + 1) / logRadiusScale) + 3;
					final int productionRatio = 100 * p.production / p.power;
					final int color = MainActivity.getColorForType(p.powerType);

					if (productionRatio == 0) {
						mCirclePaint.setARGB(255, 200, 40, 60);
					} else {
						final int grey = Math.round(255 * (1f * p.production / p.power));
						mCirclePaint.setARGB(grey, Color.red(color), Color.green(color), Color.blue(color));
					}
					mCircleBorderPaint.setColor(color);
					canvas.drawCircle(mappedPoints[0], mappedPoints[1], (float) r, mCirclePaint);
					canvas.drawCircle(mappedPoints[0], mappedPoints[1], (float) r, mCircleBorderPaint);

					if (mTouchedLocation != null) {
						if (p.equals(mTouchedLocation)) {
							canvas.drawCircle(mappedPoints[0], mappedPoints[1], (float) (r + 1), mTouchedCircleBorderPaint);
							drawInfoField(mappedPoints[0], mappedPoints[1],(float) (r + 1), canvas, mTouchedLocation);
						}
					}
				}

			}
		}

	}

}
