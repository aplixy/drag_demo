package com.example.dragdemo;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;


public class DragLinerLayout extends LinearLayout {
	private static final String TAG = "DragLinerLayout";

	private static final boolean DEBUG = true;

	private static final boolean USE_CACHE = false;

	private static final int MAX_SETTLE_DURATION = 600; // ms

	static class ItemInfo {
		Object object;

		int position;

		boolean scrolling;
	}

	private static final Interpolator sInterpolator = new Interpolator() {
		public float getInterpolation(float t) {
			t -= 1.0f;
			return t * t * t + 1.0f;
		}
	};

	private final ArrayList<ItemInfo> mItems = new ArrayList<ItemInfo>();

	//private Scroller mScroller;

	private boolean mScrollingCacheEnabled;

	private boolean mScrolling;

	private boolean mIsBeingDragged;

	//private boolean mIsUnableToDrag;
	
	//private boolean mIsInterceptDown;

	private int mTouchSlop;

	/**
	 * Position of the last motion event.
	 */
	private float mLastMotionX;

	private float mLastMotionY;

	/**
	 * ID of the active pointer. This is used to retain consistency during
	 * drags/flings if multiple pointers are used.
	 */
	private int mActivePointerId = INVALID_POINTER;

	/**
	 * Sentinel value for no current active pointer. Used by
	 * {@link #mActivePointerId}.
	 */
	private static final int INVALID_POINTER = -1;

	/**
	 * Determines speed during touch scrolling
	 */
	private VelocityTracker mVelocityTracker;

	private int mMaximumVelocity;

	private OnSlideListener mOnSlideListener;

	/**
	 * Indicates that the pager is in an idle, settled state. The current page
	 * is fully in view and no animation is in progress.
	 */
	public static final int SCROLL_STATE_IDLE = 0;

	/**
	 * Indicates that the pager is currently being dragged by the user.
	 */
	public static final int SCROLL_STATE_DRAGGING = 1;

	/**
	 * Indicates that the pager is in the process of settling to a final
	 * position.
	 */
	public static final int SCROLL_STATE_SETTLING = 2;

	//private int mScrollState = SCROLL_STATE_SETTLING;
	
	
	
	private static final int AUTO_SCROLL_STEP = 20;;
	private static final int AUTO_SCROLL_TIME_GAP = 10;
	
	private LinearLayout mHeaderViewGroup;
	private FrameLayout mContentViewGroup;
	
	private int mHeaderHeight;
	private int mMinAutoScrollHeight;
	
	private Handler mHandler = new Handler();
	
	private AutoScrollRunnable mAutoScrollRunnable = new AutoScrollRunnable();
	
	//private boolean mIsChildMove;
	
	private boolean mIsChangedToMe;
	private boolean mIsChangedToChild;
	
	private boolean mIsTouchDown;
	
	/**
	 * Callback interface for responding to changing state of the selected page.
	 */
	public interface OnSlideListener {

		public void onSlideStart(DragLinerLayout layout);

		public void onAttachTop(DragLinerLayout layout);
		
		public void onAttachBottom(DragLinerLayout layout);
	}

	public DragLinerLayout(Context context) {
		super(context);
		init();
	}

	public DragLinerLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	void init() {
		setWillNotDraw(false);
		setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		setFocusable(true);
		final Context context = getContext();
		//mScroller = new Scroller(context, sInterpolator);
		final ViewConfiguration configuration = ViewConfiguration.get(context);
		//mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
		
		setOrientation(LinearLayout.VERTICAL);
		
		addLinearView(context);
	}
	
	private void addLinearView(Context context) {
		mHeaderViewGroup = new LinearLayout(context);
		mHeaderViewGroup.setOrientation(LinearLayout.VERTICAL);
		LayoutParams lpHeader = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		addView(mHeaderViewGroup, lpHeader);
		
		
		mContentViewGroup = new FrameLayout(context);
		ViewGroup.LayoutParams lpContent = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		addView(mContentViewGroup, lpContent);
		
		
		mHeaderViewGroup.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				mHeaderHeight = mHeaderViewGroup.getHeight();
				
				mMinAutoScrollHeight = (int)(mHeaderHeight / 3.0f);
			}
		});
	}
	
	public void addHeaderView(View headerView) {
		mHeaderViewGroup.addView(headerView);
		
		mHeaderHeight = mHeaderViewGroup.getHeight();
		
		mMinAutoScrollHeight = (int)(mHeaderHeight / 3.0f);
	}
	
	public void addContentView(View contentView) {
		mContentViewGroup.addView(contentView);
	}

	/**
	 * Set a listener that will be invoked whenever the page changes or is
	 * incrementally scrolled. See {@link OnSlideListener}.
	 * 
	 * @param listener
	 *            Listener to set
	 */
	public void setOnSlideListener(OnSlideListener listener) {
		mOnSlideListener = listener;
	}

	// We want the duration of the page snap animation to be influenced by the
	// distance that
	// the screen has to travel, however, we don't want this duration to be
	// effected in a
	// purely linear fashion. Instead, we use this method to moderate the effect
	// that the distance
	// of travel has on the overall snap duration.
	float distanceInfluenceForSnapDuration(float f) {
		f -= 0.5f; // center the values about 0.
		f *= 0.3f * Math.PI / 2.0f;
		return (float) Math.sin(f);
	}

	
	private void completeScroll() {
		boolean needPopulate = mScrolling;
		if (needPopulate) {
			// Done with scroll, no longer want to cache view drawing.
			setScrollingCacheEnabled(false);
		}
		mScrolling = false;
		for (int i = 0; i < mItems.size(); i++) {
			ItemInfo ii = mItems.get(i);
			if (ii.scrolling) {
				needPopulate = true;
				ii.scrolling = false;
			}
		}
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		
		
		
		int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
		Log.v(TAG, "****dispatchTouchEvent--->" + action);
		
		//if (action != MotionEvent.ACTION_DOWN) {
		//	if (mIsChangedToChild) {
		//		mContentViewGroup.dispatchTouchEvent(ev);
		//		if (action != MotionEvent.ACTION_UP) return false;
		//	}
		//}
		
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG, "****dispatchTouchEvent--->ACTION_DOWN");
			
			mHandler.removeCallbacks(mAutoScrollRunnable);
			
			mLastMotionX = ev.getX();
			mLastMotionY = ev.getY();
			mActivePointerId = MotionEventCompat.getPointerId(ev, 0);

			break;
			
		case MotionEvent.ACTION_MOVE:
			
			final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
			final float x = MotionEventCompat.getX(ev, pointerIndex);
			final float dx = x - mLastMotionX;
			final float xDiff = Math.abs(dx);
			final float y = MotionEventCompat.getY(ev, pointerIndex);
			final float dy = y - mLastMotionY;
			final float yDiff = Math.abs(dy);
			
			//Log.v(TAG, "yDiff--->" + yDiff + ", xDiff--->" + xDiff);
			
			boolean willIntercept = false;
			
			if (yDiff > mTouchSlop && yDiff > xDiff) {
				//Log.d(TAG, "纵向滑, dy--->" + dy);
				
				LayoutParams lp = (LayoutParams) mHeaderViewGroup.getLayoutParams();
				if (dy < 0) {// 向上推
					if (-lp.topMargin < mHeaderHeight) {
						//Log.i(TAG, "向上推，并且没推到头");
						willIntercept = true;
					} else {
						Log.i(TAG, "向上推，推到最上边了");
						willIntercept = false;
						if (!mIsChangedToChild) {
							changeTouchEventToChild(ev);
							mIsChangedToMe = false;
							return false;
						}
					}
					
				} else if (dy > 0) {// 向下拉
					if (-lp.topMargin <= mHeaderHeight && -lp.topMargin > 0) {
						//Log.d(TAG, "向下拉，并且没拉到头");
						if (canScroll(this, false, (int) dy, (int) x, (int) y)) {
							Log.i(TAG, "向下拉，子View可以滑动");
							willIntercept = false;
						} else {
							//Log.w(TAG, "向下拉，子View不能动");
							if (!mIsChangedToMe) {
								changeTouchEventToMe(ev);
							}
							willIntercept = true;
						}
					} else if (lp.topMargin == 0) {
						Log.d(TAG, "向下拉，全部拉下来了");
						willIntercept = false;
					} else {
						willIntercept = true;
					}
				}
				
				if (!willIntercept) {
					mLastMotionX = x;
					mLastMotionY = y;
				}
			}
			
			if (willIntercept) {
				Log.e(TAG, "-^-dispatch move置为true");
				mIsBeingDragged = true;
				mLastMotionX = x;
				setScrollingCacheEnabled(true);
			} else if (!mIsChangedToMe){
				Log.w(TAG, "-^-dispatch move置为false");
				mIsBeingDragged = false;
			}
			
			if (mIsChangedToChild) {
				mContentViewGroup.dispatchTouchEvent(ev);
				return false;
			}
			
			break;
			
		case MotionEvent.ACTION_CANCEL:
			break;
			
		case MotionEventCompat.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			break;

		default:
			Log.w(TAG, "****dispatchTouchEvent--->default");
			
			if (mIsChangedToChild) {
				mContentViewGroup.dispatchTouchEvent(ev);
			}
			
			Log.w(TAG, "-^-dispatch default置为false");
			mIsBeingDragged = false;
			mActivePointerId = INVALID_POINTER;
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			
			completeScroll();
			
			mIsChangedToMe = false;
			mIsChangedToChild = false;
			
			//return true;
			break;
		}
		
		
		if (!mIsBeingDragged) {
			// Track the velocity as long as we aren't dragging.
			// Once we start a real drag we will track in onTouchEvent.
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
			}
			mVelocityTracker.addMovement(ev);
		}
		
		return super.dispatchTouchEvent(ev);
	}
	
	private void changeTouchEventToMe(MotionEvent ev) {
		Log.d(TAG, "changeTouchEventToMe");
		
		MotionEvent event1 = MotionEvent.obtain(ev);
		event1.setAction(MotionEvent.ACTION_CANCEL);
		mContentViewGroup.dispatchTouchEvent(event1);
		event1.recycle();

		
		MotionEvent event = MotionEvent.obtain(ev);
		event.setLocation(ev.getX(), ev.getY());
		event.setAction(MotionEvent.ACTION_DOWN);
		this.dispatchTouchEvent(event);
		event.recycle();
		
		mIsChangedToChild = false;
		mIsChangedToMe = true;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		

		if (getChildCount() < 1) {
			return false;
		}
		/*
		 * This method JUST determines whether we want to intercept the motion.
		 * If we return true, onMotionEvent will be called and we do the actual
		 * scrolling there.
		 */

		final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;
		
		Log.i(TAG, "###onInterceptTouchEvent--->" + action);
		Log.v(TAG, "###onInterceptTouchEvent--->mIsBeingDragged--->" + mIsBeingDragged);

		// Always take care of the touch gesture being complete.
		if (/*action == MotionEvent.ACTION_CANCEL || */action == MotionEvent.ACTION_UP) {
			// Release the drag.
			
			Log.d(TAG, "###onInterceptTouchEvent--->Release the drag");
			
			Log.w(TAG, "-^-onIntercept UP置为false");
			
			mIsBeingDragged = false;
			mActivePointerId = INVALID_POINTER;
			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
			return false;
		}

		// Nothing more to do here if we have decided whether or not we
		// are dragging.
		if (action != MotionEvent.ACTION_DOWN) {
			if (mIsBeingDragged) {
				return true;
			}
		}

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			break;
		case MotionEvent.ACTION_MOVE:
			break;
		case MotionEventCompat.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			break;
		}

		if (!mIsBeingDragged) {
			// Track the velocity as long as we aren't dragging.
			// Once we start a real drag we will track in onTouchEvent.
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
			}
			mVelocityTracker.addMovement(ev);
		}

		/*
		 * The only time we want to intercept motion events is if we are in the
		 * drag mode.
		 */
		
		return mIsBeingDragged;
		//return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		
		final int action = ev.getAction();
		
		Log.w(TAG, "+++onTouchEvent--->" + action);

		if (getChildCount() < 1) {
			return false;
		}

		if (action == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
			// Don't handle edge touches immediately -- they may actually belong
			// to one of our
			// descendants.
			return false;
		}
		
		if (action == MotionEvent.ACTION_CANCEL) {
			if (mIsBeingDragged) {
				//mActivePointerId = INVALID_POINTER;
			}
			return false;
		}
		
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		
		boolean needsInvalidate = false;

		switch (action & MotionEventCompat.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: {
			/*
			 * If being flinged and user touches, stop the fling. isFinished
			 * will be false if being flinged.
			 */
			completeScroll();

			// Remember where the motion event started
			mLastMotionY = ev.getY();
			mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
			break;
		}
		case MotionEvent.ACTION_MOVE:
			
			if (!mIsBeingDragged) {
				final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
				final float x = MotionEventCompat.getX(ev, pointerIndex);
				final float xDiff = Math.abs(x - mLastMotionX);
				final float y = MotionEventCompat.getY(ev, pointerIndex);
				final float yDiff = Math.abs(y - mLastMotionY);
				
				if (yDiff > mTouchSlop && yDiff > xDiff) {
					
					Log.e(TAG, "-^-onTouch MOVE置为true");
					mIsBeingDragged = true;
					mLastMotionY = y;
					
					Log.e(TAG, "on touch move--->");
					setScrollingCacheEnabled(true);
				}
			}
			
			//Log.d(TAG, "+++onTouchEvent---MOVE---mIsBeingDragged--->" + mIsBeingDragged);
			
			if (mIsBeingDragged) {
				// Scroll to follow the motion event
				final int activePointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
				final float y = MotionEventCompat.getY(ev, activePointerIndex);
				final float deltaY = y - mLastMotionY;
				mLastMotionY = y;
				
				if (deltaY != 0.0f) {
					LayoutParams lp = (LayoutParams) mHeaderViewGroup.getLayoutParams();
					
					// =====================向上推================
					if (deltaY < 0) {
						mIsTouchDown = false;
						
						// 还没完全推上去
						if (-lp.topMargin < mHeaderHeight) {
							lp.topMargin += deltaY;
							if (-lp.topMargin >= mHeaderHeight) lp.topMargin = -mHeaderHeight;
						}
						
						// 推到最上边了
						if (lp.topMargin == -mHeaderHeight) {
							completeScroll();
							mHeaderViewGroup.setLayoutParams(lp);
							return false;
						}
					} 
					// =====================向下拉=================
					else {
						mIsTouchDown = true;
						
						// 还没完全拉下来
						if (lp.topMargin < 0) {
							lp.topMargin += deltaY;
							if (lp.topMargin >= 0) lp.topMargin = 0;
						}
						
						// 完全拉下来了
						if (lp.topMargin == 0) {
							completeScroll();
							mHeaderViewGroup.setLayoutParams(lp);
							return false;
						}
					}
					
					mHeaderViewGroup.setLayoutParams(lp);
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mIsBeingDragged) {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
				mActivePointerId = INVALID_POINTER;
			}
			endDrag(ev);
			break;
		case MotionEvent.ACTION_OUTSIDE:
			endDrag(ev);
			break;
		case MotionEventCompat.ACTION_POINTER_DOWN: {
			final int index = MotionEventCompat.getActionIndex(ev);
			final float y = MotionEventCompat.getY(ev, index);
			mLastMotionY = y;
			mActivePointerId = MotionEventCompat.getPointerId(ev, index);
			break;
		}
		case MotionEventCompat.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			mLastMotionY = MotionEventCompat.getY(ev, MotionEventCompat.findPointerIndex(ev, mActivePointerId));
			break;
		default:
			endDrag(ev);
			break;
		}
		
		if (needsInvalidate) {
			invalidate();
		}
		
		return true;
	}
	
	private void changeTouchEventToChild(MotionEvent ev) {
		Log.d(TAG, "changeTouchEventToChild");
		ev.setAction(MotionEvent.ACTION_CANCEL);
		super.dispatchTouchEvent(ev);

		MotionEvent event = MotionEvent.obtain(ev);
		event.setLocation(ev.getX(), ev.getY());
		event.setAction(MotionEvent.ACTION_DOWN);
		mContentViewGroup.dispatchTouchEvent(event);
		event.recycle();
		
		mIsChangedToChild = true;
		mIsChangedToMe = false;
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = MotionEventCompat.getActionIndex(ev);
		final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
		if (pointerId == mActivePointerId) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastMotionY = MotionEventCompat.getY(ev, newPointerIndex);
			mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
			if (mVelocityTracker != null) {
				mVelocityTracker.clear();
			}
		}
	}

	private void endDrag(MotionEvent ev) {
		
		
		
		Log.w(TAG, "-^-end drag置为false");
		mIsBeingDragged = false;
		
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
		
		
		
		//float deltaY = ev.getY() - mLastMotionY;
		
		//Log.d(TAG, ">>>>endDrag, deltaY--->" + deltaY);
		
		//LayoutParams lp = (LayoutParams) mHeaderViewGroup.getLayoutParams();
		
		//if (deltaY > 0) {// 往下拉
		if (mIsTouchDown) {// 往下拉
//			if (-lp.topMargin < 2 * mMinAutoScrollHeight) {
//				mAutoScrollRunnable.setIsUp(false);
//			} else {
//				mAutoScrollRunnable.setIsUp(true);
//			}
			
			mAutoScrollRunnable.setIsUp(false);
		} else {// 往上推
//			if (-lp.topMargin > mMinAutoScrollHeight) {
//				mAutoScrollRunnable.setIsUp(true);
//			} else {
//				mAutoScrollRunnable.setIsUp(false);
//			}
			
			mAutoScrollRunnable.setIsUp(true);
		}
		
		mHandler.post(mAutoScrollRunnable);
		
	}
	
	public void scrollUp(){
		LayoutParams lp = (LayoutParams) mHeaderViewGroup.getLayoutParams();
		if (lp.topMargin == -mHeaderHeight) return;
		
		mAutoScrollRunnable.setIsUp(true);
		mHandler.post(mAutoScrollRunnable);
	}
	
	public void scrollDown(){
		LayoutParams lp = (LayoutParams) mHeaderViewGroup.getLayoutParams();
		if (lp.topMargin == 0) return;
		
		mAutoScrollRunnable.setIsUp(false);
		mHandler.post(mAutoScrollRunnable);
	}
	

	private void setScrollingCacheEnabled(boolean enabled) {
		if (mScrollingCacheEnabled != enabled) {
			mScrollingCacheEnabled = enabled;
			if (USE_CACHE) {
				final int size = getChildCount();
				for (int i = 0; i < size; ++i) {
					final View child = getChildAt(i);
					if (child.getVisibility() != GONE) {
						child.setDrawingCacheEnabled(enabled);
					}
				}
			}
		}
	}

	/**
	 * Tests scrollability within child views of v given a delta of dx.
	 * 
	 * @param v
	 *            View to test for horizontal scrollability
	 * @param checkV
	 *            Whether the view v passed should itself be checked for
	 *            scrollability (true), or just its children (false).
	 * @param dy
	 *            Delta scrolled in pixels
	 * @param x
	 *            X coordinate of the active touch point
	 * @param y
	 *            Y coordinate of the active touch point
	 * @return true if child views of v can be scrolled by delta of dx.
	 */
	protected boolean canScroll(View v, boolean checkV, int dy, int x, int y) {
		if (v instanceof ViewGroup) {
			final ViewGroup group = (ViewGroup) v;
			final int scrollX = v.getScrollX();
			final int scrollY = v.getScrollY();
			final int count = group.getChildCount();
			// Count backwards - let topmost views consume scroll distance
			// first.
			for (int i = count - 1; i >= 0; i--) {
				// TODO: Add versioned support here for transformed views.
				// This will not work for transformed views in Honeycomb+
				final View child = group.getChildAt(i);
				if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
						&& canScroll(child, true, dy, x + scrollX - child.getLeft(), y + scrollY - child.getTop())) {
					return true;
				}
			}
		}

		//if (v instanceof TurnOffTimerView) {
		//	return checkV && true;
		//}

		//return checkV && ViewCompat.canScrollHorizontally(v, -dx);
		return checkV && ViewCompat.canScrollVertically(v, -dy);
		
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		//if (!mIsBeingDragged)
			//smoothScrollTo(0, 0);
		completeScroll();
		
	}
	
	// ========================================================
	private class AutoScrollRunnable implements Runnable {
		private boolean mIsUp;
		
		public void setIsUp(boolean isUp) {
			mIsUp = isUp;
		}
		
		@Override
		public void run() {
			LayoutParams lp = (LayoutParams) mHeaderViewGroup.getLayoutParams();
			
			if (lp.topMargin == 0 && !mIsUp || lp.topMargin == -mHeaderHeight && mIsUp) {
				if (mOnSlideListener != null) {
					if (lp.topMargin == 0) {
						mOnSlideListener.onAttachBottom(DragLinerLayout.this);
					} else {
						mOnSlideListener.onAttachTop(DragLinerLayout.this);
					}
				}
				
				mHandler.removeCallbacks(this);
				return;
			} else {
				if (mIsUp) {
					lp.topMargin -= AUTO_SCROLL_STEP;
					if (-lp.topMargin >= mHeaderHeight) {
						lp.topMargin = -mHeaderHeight;
						if (mOnSlideListener != null) {
							mOnSlideListener.onAttachTop(DragLinerLayout.this);
						}
						mHandler.removeCallbacks(this);
					} else {
						mHandler.postDelayed(this, AUTO_SCROLL_TIME_GAP);
					}
				} else {
					lp.topMargin += AUTO_SCROLL_STEP;
					if (lp.topMargin >= 0) {
						lp.topMargin = 0;
						
						if (mOnSlideListener != null) {
							mOnSlideListener.onAttachBottom(DragLinerLayout.this);
						}
						
						mHandler.removeCallbacks(this);
					} else {
						mHandler.postDelayed(this, AUTO_SCROLL_TIME_GAP);
					}
				}
				
				mHeaderViewGroup.setLayoutParams(lp);
			}
		}
	};

}
