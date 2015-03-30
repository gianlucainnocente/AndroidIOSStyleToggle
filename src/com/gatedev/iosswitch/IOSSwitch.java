package com.gatedev.iosswitch;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import com.gatedev.iosswitch.WobblyView.Pivot;

public class IOSSwitch extends FrameLayout {

	public static final String TAG = IOSSwitch.class.getSimpleName();

	private static final boolean DEFAULT_START_VALUE = false;

	private WobblyView mCircle;
	private View mSecondBackgroundView;
	private float mCircleRadius;

	private Drawable mSliderBackgroundDrawableTrue;
	private Drawable mSliderBackgroundDrawableFalse;
	private Drawable mSliderBackgroundDrawableFalseOver;
	private Drawable mCircleBackgroundDrawable;

	private boolean mValue;

	private enum InternalState {
		IDLE, CAUGHT_LEFT, CAUGHT_RIGHT, ANIMATE_EXPAND, ANIMATE_MOVE, ANIMATE_CONTRACT
	}

	private boolean mHasMoved;
	private boolean mHasTapped;
	private boolean mHasReleased;

	private InternalState mState = InternalState.IDLE;

	private OnValueChangedListener mListener;

	private ValueAnimator mScaleUpAnimation;
	private ValueAnimator mScaleDownAnimation;
	private float mFatScale = 1.2f;
	private float mWidthOverlapPercent = 0.3f;
	private float mWidthToTriggerMovePercent = 0.1f;

	private GestureDetector mGestureDetector;
	private SimpleOnGestureListener mGestureListener;
	private TimeInterpolator mExpandInterpolator = new LinearInterpolator();
	private TimeInterpolator mContractInterpolator = new DecelerateInterpolator(0.6f);
	private TimeInterpolator mMoveInterpolator = new OvershootInterpolator(0.6f);
	private TimeInterpolator mExpandBGInterpolator = new DecelerateInterpolator();
	private TimeInterpolator mContractBGInterpolator = new DecelerateInterpolator(1.1f);

	private long mExpandDuration;
	private long mExpandBGDuration;
	private long mContractDuration;
	private long mMoveDuration;

	public IOSSwitch(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context, attrs);
	}

	public IOSSwitch(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		init(context, attrs);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		if (mState == InternalState.IDLE) {

			mCircle.setScaleX(1f);
			mCircle.setTranslationX(getTranslationXForValue(mValue));
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		/*
		 * avoiding useless long slider (can anyway be obtained by using
		 * MeasureSpec.EXACTLY)
		 */

		if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {

			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		} else {

			int newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
					(int) (mCircleRadius * 4f - mCircleRadius * 2f
							* mWidthOverlapPercent), MeasureSpec.EXACTLY);

			if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
				super.onMeasure(newWidthMeasureSpec, heightMeasureSpec);
			} else if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.AT_MOST) {

				if (MeasureSpec.getSize(widthMeasureSpec) > MeasureSpec.getSize(newWidthMeasureSpec)) {
					super.onMeasure(newWidthMeasureSpec, heightMeasureSpec);
				} else {
					super.onMeasure(widthMeasureSpec, heightMeasureSpec);
				}
			}
		}

	}

	private float getTranslationXForValue(boolean value) {
		return value ? getWidth() - mCircle.getWidth() : 0;
	}

	public void animateTo(boolean value, boolean callListener) {
		mState = InternalState.ANIMATE_MOVE;

		if (value) {
			setBackground(mSliderBackgroundDrawableTrue);
		} else {
			setBackground(mSliderBackgroundDrawableFalse);
		}

		mCircle.animate().translationX(getTranslationXForValue(value))
		.setInterpolator(mMoveInterpolator).setDuration(mMoveDuration)
		.setListener(new UpdateStateMoveListener(value, callListener));
	}

	private class UpdateStateExpandListener extends AnimatorListenerAdapter {

		protected boolean newValue;
		protected boolean callListener;

		public UpdateStateExpandListener(boolean newValue, boolean callListener) {
			this.newValue = newValue;
			this.callListener = callListener;
		}

		@Override
		public void onAnimationCancel(Animator animation) {
			setNewValue();
			consumeRelease();

		}

		@Override
		public void onAnimationEnd(Animator animation) {
			setNewValue();
			consumeRelease();
		}

		private void setNewValue() {
			mState = newValue ? InternalState.CAUGHT_RIGHT
					: InternalState.CAUGHT_LEFT;
		}

		private void consumeRelease() {
			if (!mHasTapped && mHasReleased) {
				/*
				 * we arrive only moving while fat scaled here
				 */
				animateContract(mFatScale, callListener);
			}
		}
	}

	private class UpdateStateMoveListener extends UpdateStateExpandListener {

		public UpdateStateMoveListener(boolean newValue, boolean callListener) {
			super(newValue, callListener);
		}

		@Override
		public void onAnimationCancel(Animator animation) {
			super.onAnimationCancel(animation);

			consumeTap();
		}

		@Override
		public void onAnimationEnd(Animator animation) {
			super.onAnimationEnd(animation);

			consumeTap();
		}

		private void consumeTap() {
			if (mHasTapped) {
				if (!mScaleUpAnimation.isRunning()) {
					animateContract(mFatScale, callListener);
				} else {
					float currentScale = (Float) mScaleUpAnimation
							.getAnimatedValue();
					mScaleUpAnimation.cancel();
					animateContract(currentScale, callListener);
				}
			}
		}
	}

	private void animateExpand() {
		mState = InternalState.ANIMATE_EXPAND;

		if (!mValue) {
			mSecondBackgroundView.animate().scaleX(0f).scaleY(0f)
			.setInterpolator(mExpandBGInterpolator)
			.setDuration(mExpandDuration);
		}

		if (mValue) {
			mCircle.setCustomPivotX(Pivot.RIGHT);
		} else {
			mCircle.setCustomPivotX(Pivot.LEFT);
		}

		mScaleUpAnimation.setFloatValues(1f, mFatScale);

		mScaleUpAnimation.setInterpolator(mExpandInterpolator);
		mScaleUpAnimation.setDuration(mExpandDuration);
		mScaleUpAnimation.removeAllListeners();
		mScaleUpAnimation.removeAllUpdateListeners();

		mScaleUpAnimation.addUpdateListener(new ScaleCircleUpdateListener());

		mScaleUpAnimation.addListener(new UpdateStateExpandListener(mValue, true));
		mScaleUpAnimation.start();
	}

	private class ScaleCircleUpdateListener implements AnimatorUpdateListener {

		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			float value = (Float) animation.getAnimatedValue();
			mCircle.setCustomScaleX(value);
		}
	}

	private void animateContract(float startScale, boolean callListener) {
		if (mState == InternalState.CAUGHT_LEFT) {

			mSecondBackgroundView.animate().scaleX(1f).scaleY(1f)
			.setInterpolator(mContractBGInterpolator)
			.setDuration(mExpandBGDuration);
		}

		boolean newValue = mValue;

		Pivot pivot = Pivot.LEFT;
		if (mState == InternalState.CAUGHT_LEFT) {
			newValue = false;
			pivot = Pivot.LEFT;
		}

		if (mState == InternalState.CAUGHT_RIGHT) {
			newValue = true;
			pivot = Pivot.RIGHT;
		}

		mCircle.setCustomPivotX(pivot);

		mState = InternalState.ANIMATE_CONTRACT;

		mScaleDownAnimation.setFloatValues(startScale, 1f);

		mScaleDownAnimation.setInterpolator(mContractInterpolator);
		mScaleDownAnimation.setDuration(mContractDuration);

		mScaleDownAnimation.removeAllListeners();
		mScaleDownAnimation.removeAllUpdateListeners();
		mScaleDownAnimation.addUpdateListener(new ScaleCircleUpdateListener());

		mScaleDownAnimation.addListener(new UpdateValueListener(newValue, callListener));
		mScaleDownAnimation.start();
	}

	private class UpdateValueListener extends AnimatorListenerAdapter {

		private boolean newValue;
		private boolean callListener;

		public UpdateValueListener(boolean newValue, boolean callListener) {
			this.newValue = newValue;
			this.callListener = callListener;
		}

		@Override
		public void onAnimationCancel(Animator animation) {
			setNewValue();
			setClickable(true);
		}

		@Override
		public void onAnimationEnd(Animator animation) {
			setNewValue();
			setClickable(true);
		}

		private void setNewValue() {
			mValue = newValue;
			mState = InternalState.IDLE;

			if (mListener != null && callListener) {
				mListener.onValueChanged(newValue);
			}

			if (newValue) {
				setBackground(mSliderBackgroundDrawableTrue);
			} else {
				setBackground(mSliderBackgroundDrawableFalse);
			}
		}
	}

	private void init(Context context, AttributeSet attrs) {

		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
				R.styleable.IOSSwitch, 0, 0);

		boolean startValue;

		try {

			mSliderBackgroundDrawableTrue = a
					.getDrawable(R.styleable.IOSSwitch_background_true);
			mSliderBackgroundDrawableFalse = a
					.getDrawable(R.styleable.IOSSwitch_background_false);
			mSliderBackgroundDrawableFalseOver = a
					.getDrawable(R.styleable.IOSSwitch_background_false_over);

			mCircleBackgroundDrawable = a
					.getDrawable(R.styleable.IOSSwitch_circle_image);

			startValue = a.getBoolean(R.styleable.IOSSwitch_start_value,
					DEFAULT_START_VALUE);

		} finally {
			a.recycle();
		}

		mGestureListener = new MyGestureListener();
		mGestureDetector = new GestureDetector(context, mGestureListener);

		mScaleUpAnimation = ValueAnimator.ofFloat();
		mScaleDownAnimation = ValueAnimator.ofFloat();

		mCircleRadius = mCircleBackgroundDrawable.getIntrinsicHeight() / 2f;

		mExpandDuration = Math.round((float) getContext().getResources()
				.getInteger(android.R.integer.config_shortAnimTime) * 0.8f);

		mContractDuration = Math.round((float) mExpandDuration * 0.8f);
		mMoveDuration = Math.round((float) mExpandDuration * 1.1f);
		mExpandBGDuration = Math.round((float) mContractDuration * 2f);

		mSecondBackgroundView = new View(context);
		mSecondBackgroundView.setBackgroundDrawable(mSliderBackgroundDrawableFalseOver);

		addView(mSecondBackgroundView, new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, Math.round(mCircleRadius * 2f)));

		mCircle = new WobblyView(context);
		mCircle.setWobblyBackground(mCircleBackgroundDrawable);
		addView(mCircle);

		setClickable(true);

		setValue(startValue);
	}

	public void setValue(boolean newValue) {
		if (mState == InternalState.IDLE) {
			mValue = newValue;
			if (newValue) {
				if (getHeight() != 0) {
					mCircle.setTranslationX(getHeight() - mCircle.getHeight());
				}

				mCircle.setCustomPivotX(Pivot.RIGHT);
				mSecondBackgroundView.setScaleX(0f);
				mSecondBackgroundView.setScaleY(0f);
				setBackground(mSliderBackgroundDrawableTrue);
			} else {
				mCircle.setTranslationX(0);
				mCircle.setCustomPivotX(Pivot.LEFT);
				mSecondBackgroundView.setScaleX(1f);
				mSecondBackgroundView.setScaleY(1f);
				setBackground(mSliderBackgroundDrawableFalse);
			}
		}
	}

	private class MyGestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			mHasTapped = true;

			return true;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(!isEnabled()) {
			return true;
		}

		mGestureDetector.onTouchEvent(event);

		switch (event.getAction()) {

		case MotionEvent.ACTION_DOWN:

			if (isClickable() && isOntoSlider(event)) {

				resetActions();
				animateExpand();
				/*
				 * disallow parent event management (for example to avoid a
				 * ViewPager swiping while moving the switch)
				 */
				requestDisallowInterceptTouchEvent(true);
				return true;

			} else {

				return false;
			}

		case MotionEvent.ACTION_MOVE:

			handleCatch(event);
			return true;

		case MotionEvent.ACTION_UP:
			/*
			 * here we arrive only when it had been caught
			 */
			setClickable(false);
			handleRelease(event);

			return true;

		default:

			return false;
		}
	}

	private void handleRelease(MotionEvent event) {
		mHasReleased = true;

		if (!mHasMoved) {
			mHasTapped = true;

			animateTo(!mValue, true);
		} else {

			if (mState == InternalState.CAUGHT_LEFT
					|| mState == InternalState.CAUGHT_RIGHT) {
				animateContract(mFatScale, true);
			}
		}
	}

	private void resetActions() {
		/*
		 * clear status from previous touch interactions
		 */
		mHasMoved = false;
		mHasTapped = false;
		mHasReleased = false;
	}

	private void handleCatch(MotionEvent event) {
		if (mState == InternalState.CAUGHT_LEFT
				|| mState == InternalState.CAUGHT_RIGHT) {

			float sensibleWidth = mCircleRadius * 2f
					* mWidthToTriggerMovePercent;
			float minX = sensibleWidth;
			float maxX = getWidth() - sensibleWidth;

			if (event.getX() > maxX) {

				mHasMoved = true;

				if (mState == InternalState.CAUGHT_LEFT) {
					animateTo(true, true);
				}
			}

			if (event.getX() < minX) {

				mHasMoved = true;

				if (mState == InternalState.CAUGHT_RIGHT) {
					animateTo(false, true);
				}
			}
		}
	}

	private boolean isOntoSlider(MotionEvent event) {

		float y = getHeight() / 2f;
		float xL = getTranslationXForValue(false) + mCircleRadius;
		float xR = getTranslationXForValue(true) + mCircleRadius;

		boolean onTheMiddle = event.getX() > xL && event.getX() < xR
				&& event.getY() > 0 && event.getY() < getHeight();

				if (onTheMiddle) {
					return true;
				}

				boolean onTheLeft = Utils.distance(xL, y, event.getX(), event.getY()) < mCircleRadius;

				if (onTheLeft) {
					return true;
				}

				boolean onTheRight = Utils.distance(xR, y, event.getX(), event.getY()) < mCircleRadius;

				if (onTheRight) {
					return true;
				}

				return false;
	}

	public boolean getValue() {
		return mValue;
	}

	public interface OnValueChangedListener {
		public void onValueChanged(boolean newValue);
	}

	public void setOnValueChangedListener(OnValueChangedListener newListener) {
		mListener = newListener;
	}

	public void removeOnValueChangedListener() {
		mListener = null;
	}

}
