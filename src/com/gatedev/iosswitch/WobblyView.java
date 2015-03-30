package com.gatedev.iosswitch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class WobblyView extends View {

	public enum Pivot {LEFT, RIGHT}
	private Pivot mPivot = Pivot.LEFT;
	private Drawable mDrawable;
	private float mMaxScaleX;
	private float mCurrentScaleX;
	private int mHeight;
	private int mWidth;
	
	public void setWobblyBackground(Drawable background) {
		mDrawable = background;
		
		invalidate();
	}
	
	public WobblyView(Context context) {
		super(context);
		
		init(context, null);
	}
	
	public WobblyView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		init(context, attrs);
	}
	
	public WobblyView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		mMaxScaleX = 1.2f;
		mCurrentScaleX = 1f;
	}
	
	public void setCustomScaleX(float scaleX) {
		mCurrentScaleX = scaleX;
		
		invalidate();
	}
	
	public void setCustomPivotX(Pivot pivot) {
		mPivot = pivot;
		
		invalidate();
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		/*
		 * AT_MOST not supported, i suppose there's always enough space
		 */
		if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
			mHeight = MeasureSpec.getSize(heightMeasureSpec);
		} else {
			mHeight = mDrawable.getIntrinsicHeight();
		}

		mWidth = Math.round(mHeight * mMaxScaleX);

		setMeasuredDimension(mWidth, mHeight);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (mPivot == Pivot.RIGHT) {
			canvas.translate(Math.round(getHeight() * (mMaxScaleX - mCurrentScaleX)), 0);
		}

		mDrawable.setBounds(0, 0, Math.round(getHeight() * mCurrentScaleX), getHeight());
		mDrawable.draw(canvas);
		canvas.restore();
	}

}
