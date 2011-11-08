package de.inovex.graph.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

public class MapView extends View {
	private final static String sBorderPoints="M 735.50,258.00M 739.00,273.50M 739.00,278.50M 735.00,313.50M 759.50,387.50M 730.00,390.50M 724.50,403.00M 678.50,426.00M 685.00,456.50M 724.00,490.00M 706.00,513.50M 709.50,531.00M 700.00,542.00M 681.00,537.00M 660.50,549.00M 632.00,553.50M 609.50,551.00M 591.50,548.50M 574.50,535.00M 558.00,535.00M 543.50,544.50M 568.50,474.50M 518.50,465.00M 515.00,440.50M 504.50,426.50M 504.00,404.00M 495.50,398.50M 504.00,368.50M 499.50,358.00M 527.00,336.00M 530.00,317.50M 518.50,300.00M 532.00,279.00M 537.50,268.50M 554.00,268.50M 559.50,271.50M 575.50,264.00M 582.00,250.50M 582.00,237.50M 575.50,217.00M 608.00,215.50M 618.00,233.50M 637.50,240.00M 652.50,243.00M 667.50,245.00M 680.50,230.50M 710.50,223.50M 715.00,239.50";
	
	
	private static class PointComparator implements Comparator<Point>{

		@Override
		public int compare(Point object1, Point object2) {
			return object1.x-object2.x;
		}
		
	}
	
	private List<Point> mPoints = new ArrayList<Point>();
	private Matrix mViewPortMatrix = new Matrix();
	private PointComparator mPointComparator = new PointComparator();
	private int minX = Integer.MAX_VALUE;
	private int maxX = Integer.MIN_VALUE;
	private int minY = Integer.MAX_VALUE;
	private int maxY = Integer.MIN_VALUE;
	private int minXBorder = Integer.MAX_VALUE;
	private int maxXBorder = Integer.MIN_VALUE;
	private int minYBorder = Integer.MAX_VALUE;
	private int maxYBorder = Integer.MIN_VALUE;
	
	private Path mPath = null;
	private boolean mPointsNeedSorting = false;
	private Paint mCirclePaint;
	private Paint mBorderPaint;
	private final List<Point> mBorderPoints = new ArrayList<Point>(); 
	private Path mBorderPath;
	
	public MapView(Context context) {
		super(context);
		init();
	}

	public MapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void parseBorderPoints(){
		int i = 0;
		String xString,yString;
		int x,y;
		while((i=sBorderPoints.indexOf('M',i))>-1){
			xString = sBorderPoints.substring(i+2, i+5);
			yString = sBorderPoints.substring(i+9,i+12);
			x = Integer.parseInt(xString)>>1;
			y = Integer.parseInt(yString)>>1;
			maxXBorder = Math.max(x, maxXBorder);
			minXBorder = Math.min(x, minXBorder);
			maxYBorder = Math.max(y, maxYBorder);
			minYBorder = Math.min(y, minYBorder);
			mBorderPoints.add(new Point(x,y));			
			i+=15;
		}
	}
	
	private void init(){
		mCirclePaint = new Paint(){
			{
				setStyle(Paint.Style.FILL);
				setAntiAlias(true);	
				setColor(Color.WHITE);
			}
		};
		mBorderPaint = new Paint(){
			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setStrokeWidth(2);
				setColor(Color.WHITE);
			}
		};
		parseBorderPoints();
	}
	
	public void addPoints(List<Point> points){
		for (Point p:points){
			addPoint(p);
		}		
		invalidate();
	}
	public void addPoint(Point p){
		mPointsNeedSorting = true;
		mPoints.add(p);
		maxX = Math.max(p.x, maxX);
		minX = Math.min(p.x, minX);
		maxY = Math.max(p.y, maxY);
		minY = Math.min(p.y, minY);
	}
	
	public void addPoint(int x, int y){
		addPoint(new Point(x, y));
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (mPath == null || changed){
			updatePath();
		}
	}
	
	private void determineOffsetsToCenter(float[] offsets, float scale, final int totalMaxX, final int totalMinX, final int totalMaxY, final int totalMinY){
		final float scaledW = getWidth()/scale;
		final float rightSpace = scaledW - (totalMaxX-totalMinX);
		final float scaledH = getHeight() / scale;
		final float bottomSpace = scaledH - (totalMaxY-totalMinY);
		
		offsets[0] = (int) (-totalMinX + (rightSpace/2));
		offsets[1] = (int) (-totalMinY + (bottomSpace/2));
		 
		
	}
	private float determineBestScaleFactor(final int totalMaxX, final int totalMinX, final int totalMaxY, final int totalMinY){
		final float h = getHeight();
		final float w = getWidth();				
		final float diffX = totalMaxX-totalMinX;
		final float diffY = totalMaxY - totalMinY;
		final float ratioScreen = w/h;
		final float ratioPointsX = diffX/diffY;
		final float ratioPointsY = diffY/diffX;
		final float ratioDiffX = Math.abs(ratioScreen - ratioPointsX);
		final float ratioDiffY = Math.abs(ratioScreen - ratioPointsY);
		float scale = 0;
		if (ratioDiffX< ratioDiffY){
			scale = w/diffX;
		} else {
			scale = h/diffY;
		}
		return scale;
	}
	
	private void updatePath(){
		if (mPointsNeedSorting){
			Collections.sort(mPoints, mPointComparator);
			mPointsNeedSorting = false;
		}
		float[] mappedPoints = new float[2];

		final int totalMaxX = Math.max(maxX, maxXBorder);
		final int totalMinX = Math.min(minX, minXBorder);
		final int totalMaxY = Math.max(maxY, maxYBorder);
		final int totalMinY = Math.min(minY, minYBorder);

		final float scale = determineBestScaleFactor(totalMaxX, totalMinX, totalMaxY, totalMinY);
		determineOffsetsToCenter(mappedPoints, scale, totalMaxX, totalMinX, totalMaxY, totalMinY);
		
		
		/*transforms data points into screen space*/
		mViewPortMatrix.reset();
		//1. put map into center of the view
		mViewPortMatrix.postTranslate(mappedPoints[0] ,mappedPoints[1]);
		//2. scale points to screen space
		mViewPortMatrix.postScale(scale,scale);
		
		mPath = new Path();
		mBorderPath = new Path();
		
		for(int i = 0; i < mBorderPoints.size(); i++){			
			mappedPoints[0] = mBorderPoints.get(i).x;
			mappedPoints[1] = mBorderPoints.get(i).y;
			mViewPortMatrix.mapPoints(mappedPoints);
			if (i>0){
				mBorderPath.lineTo(mappedPoints[0], mappedPoints[1]);
			} else {
				mBorderPath.moveTo(mappedPoints[0], mappedPoints[1]);
			}
		}
		mBorderPath.close();

		for (Point p :mPoints){
			mappedPoints[0] = p.x;
			mappedPoints[1] = p.y;
			mViewPortMatrix.mapPoints(mappedPoints);
			mPath.addCircle(mappedPoints[0], mappedPoints[1], 3, Path.Direction.CW);			
		}

	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		if (mPointsNeedSorting){
			updatePath();
		}
		canvas.drawPath(mPath,mCirclePaint);
		canvas.drawPath(mBorderPath, mBorderPaint);
	}


}
