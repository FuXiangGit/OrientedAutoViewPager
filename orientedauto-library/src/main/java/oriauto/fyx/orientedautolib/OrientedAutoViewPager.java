/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package oriauto.fyx.orientedautolib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.CallSuper;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.view.accessibility.AccessibilityRecordCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.AbsSavedState;
import android.view.FocusFinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;
import android.widget.Scroller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Layout manager that allows the user to flip left and right
 * through pages of data.  You supply an implementation of a
 * {@link PagerAdapter} to generate the pages that the view shows.
 * <p>
 * <p>ViewPager is most often used in conjunction with {@link android.app.Fragment},
 * which is a convenient way to supply and manage the lifecycle of each page.
 * There are standard adapters implemented for using fragments with the ViewPager,
 * which cover the most common use cases.  These are
 * {@link android.support.v4.app.FragmentPagerAdapter} and
 * {@link android.support.v4.app.FragmentStatePagerAdapter}; each of these
 * classes have simple code showing how to build a full user interface
 * with them.
 * <p>
 * <p>Views which are annotated with the {@link DecorView} annotation are treated as
 * part of the view pagers 'decor'. Each decor view's position can be controlled via
 * its {@code android:layout_gravity} attribute. For example:
 * <p>
 * <pre>
 * &lt;android.support.v4.view.ViewPager
 *     android:layout_width=&quot;match_parent&quot;
 *     android:layout_height=&quot;match_parent&quot;&gt;
 *
 *     &lt;android.support.v4.view.PagerTitleStrip
 *         android:layout_width=&quot;match_parent&quot;
 *         android:layout_height=&quot;wrap_content&quot;
 *         android:layout_gravity=&quot;top&quot; /&gt;
 *
 * &lt;/android.support.v4.view.ViewPager&gt;
 * </pre>
 * <p>
 * <p>For more information about how to use ViewPager, read <a
 * href="{@docRoot}training/implementing-navigation/lateral.html">Creating Swipe Views with
 * Tabs</a>.</p>
 * <p>
 * <p>You can find examples of using ViewPager in the API 4+ Support Demos and API 13+ Support Demos
 * sample code.
 */

/**
 * Created by FuYuxiang
 * Based on castorflex's VerticalViewPager (https://github.com/castorflex/VerticalViewPager)
 * Based on (https://github.com/NateRobinson/CardStackViewpager)
 * <p>
 * 03.05.15
 */
public class OrientedAutoViewPager extends ViewGroup {

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
     * Indicates that the pager is in the process of settling to a final position.
     */
    public static final int SCROLL_STATE_SETTLING = 2;
    static final int[] LAYOUT_ATTRS = new int[]{
            android.R.attr.layout_gravity
    };
    private static final String TAG = "FyxViewPager";
    private static final boolean DEBUG = false;
    private static final boolean USE_CACHE = false;
    private static final int DEFAULT_OFFSCREEN_PAGES = 1;
    private static final int MAX_SETTLE_DURATION = 600; // ms
    private static final int MIN_DISTANCE_FOR_FLING = 25; // dips
    private static final int DEFAULT_GUTTER_SIZE = 16; // dips
    private static final int MIN_FLING_VELOCITY = 400; // dips
    private static final Comparator<ItemInfo> COMPARATOR = new Comparator<ItemInfo>() {
        @Override
        public int compare(ItemInfo lhs, ItemInfo rhs) {
            return lhs.position - rhs.position;
        }
    };
    private static final Interpolator sInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };
    /**
     * Sentinel value for no current active pointer.
     * Used by {@link #mActivePointerId}.
     */
    private static final int INVALID_POINTER = -1;
    // If the pager is at least this close to its final position, complete the scroll
    // on touch down and let the user interact with the content inside instead of
    // "catching" the flinging pager.
    private static final int CLOSE_ENOUGH = 2; // dp
    private static final int DRAW_ORDER_DEFAULT = 0;
    private static final int DRAW_ORDER_FORWARD = 1;
    private static final int DRAW_ORDER_REVERSE = 2;
    private static final ViewPositionComparator sPositionComparator = new ViewPositionComparator();
    private final ArrayList<ItemInfo> mItems = new ArrayList<ItemInfo>();
    private final ItemInfo mTempItem = new ItemInfo();
    private final Rect mTempRect = new Rect();
    PagerAdapter mAdapter;
    int mCurItem;   // Index of currently displayed page.
    Handler mHandler = new Handler();
    boolean isAutoLoop = false;
    private Orientation mOrientation = Orientation.HORIZONTAL;
    /**
     * Used to track what the expected number of items in the adapter should be.
     * If the app changes this when we don't expect it, we'll throw a big obnoxious exception.
     */
    private int mExpectedAdapterCount;
    private int mRestoredCurItem = -1;
    private Parcelable mRestoredAdapterState = null;
    private ClassLoader mRestoredClassLoader = null;
    private Scroller mScroller;
    private boolean mIsScrollStarted;
    private PagerObserver mObserver;
    private int mPageMargin;
    private Drawable mMarginDrawable;
    private int mTopLeftPageBounds;
    private int mBottomRightPageBounds;
    // Offsets of the first and last items, if known.
    // Set during population, used to determine if we are at the beginning
    // or end of the pager data set during touch scrolling.
    private float mFirstOffset = -Float.MAX_VALUE;
    private float mLastOffset = Float.MAX_VALUE;
    private int mChildWidthMeasureSpec;
    private int mChildHeightMeasureSpec;
    private boolean mInLayout;
    private boolean mScrollingCacheEnabled;
    private boolean mPopulatePending;
    private int mOffscreenPageLimit = DEFAULT_OFFSCREEN_PAGES;
    private boolean mIsBeingDragged;
    private boolean mIsUnableToDrag;
    private int mDefaultGutterSize;
    private int mGutterSize;
    private int mTouchSlop;
    /**
     * Position of the last motion event.
     */
    private float mLastMotionX;
    private float mLastMotionY;
    //fyx add
    private float mInitialMotionX;
    private float mInitialMotionY;
    /**
     * ID of the active pointer. This is used to retain consistency during
     * drags/flings if multiple pointers are used.
     */
    private int mActivePointerId = INVALID_POINTER;
    /**
     * Determines speed during touch scrolling
     */
    private VelocityTracker mVelocityTracker;
    private int mMinimumVelocity;
    private int mMaximumVelocity;
    private int mFlingDistance;
    private int mCloseEnough;
    private boolean mFakeDragging;
    private long mFakeDragBeginTime;
    private EdgeEffect mTopLeftEdge;
    private EdgeEffect mRightBottomEdge;
    private boolean mFirstLayout = true;
    private boolean mNeedCalculatePageOffsets = false;
    private boolean mCalledSuper;
    private int mDecorChildCount;
    private List<ViewPager.OnPageChangeListener> mOnPageChangeListeners;
    private ViewPager.OnPageChangeListener mOnPageChangeListener;
    private ViewPager.OnPageChangeListener mInternalPageChangeListener;
    private List<OnAdapterChangeListener> mAdapterChangeListeners;
    private ViewPager.PageTransformer mPageTransformer;
    private int mPageTransformerLayerType;
    private int mDrawingOrder;
    private ArrayList<View> mDrawingOrderedChildren;
    private int mScrollState = SCROLL_STATE_IDLE;
    private final Runnable mEndScrollRunnable = new Runnable() {
        @Override
        public void run() {
            setScrollState(SCROLL_STATE_IDLE);
            populate();
        }
    };
    private boolean isLoop = false;
    private long delayMillis = 2000L;
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (getChildCount() > 1 && getCurrentItem() < getChildCount()) {
                mHandler.postDelayed(this, delayMillis);
                setCurrentItem(getCurrentItem() + 1, true);
            }
        }
    };

    public OrientedAutoViewPager(Context context) {
        super(context);
        initViewPager();
    }

    public OrientedAutoViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViewPager();
    }

    private static boolean isDecorView(@NonNull View view) {
        Class<?> clazz = view.getClass();
        return clazz.getAnnotation(DecorView.class) != null;
    }

    /**
     * 添加是否自动滚动控制条件
     *
     * @param isAutoLoop
     */
    public void setAutoLoop(boolean isAutoLoop) {
        this.isAutoLoop = isAutoLoop;
        if (isAutoLoop) {
            startLoop();
        } else {
            stopLoop();
        }
    }

    /**
     * 启动自滚动
     */
    public void startLoop() {
        if (!isLoop && isAutoLoop) {
            mHandler.postDelayed(mRunnable, delayMillis);// 每两秒执行一次runnable.
            isLoop = true;
        }
    }

    /**
     * 停止自滚动
     */
    public void stopLoop() {
        if (isLoop && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        isLoop = false;
    }

    /**
     * 默认滑动间隔时间,默认2秒
     *
     * @param delayMillis
     */
    public void setAutoScroDelayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    void initViewPager() {
        setWillNotDraw(false);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
        setFocusable(true);
        final Context context = getContext();
        mScroller = new Scroller(context, sInterpolator);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        final float density = context.getResources().getDisplayMetrics().density;

        mTouchSlop = configuration.getScaledPagingTouchSlop();
        mMinimumVelocity = (int) (MIN_FLING_VELOCITY * density);
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mTopLeftEdge = new EdgeEffect(context);
        mRightBottomEdge = new EdgeEffect(context);

        mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
        mCloseEnough = (int) (CLOSE_ENOUGH * density);
        mDefaultGutterSize = (int) (DEFAULT_GUTTER_SIZE * density);

        ViewCompat.setAccessibilityDelegate(this, new MyAccessibilityDelegate());

        if (ViewCompat.getImportantForAccessibility(this)
                == ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            ViewCompat.setImportantForAccessibility(this,
                    ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }

        ViewCompat.setOnApplyWindowInsetsListener(this,
                new android.support.v4.view.OnApplyWindowInsetsListener() {
                    private final Rect mTempRect = new Rect();

                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(final View v,
                                                                  final WindowInsetsCompat originalInsets) {
                        // First let the ViewPager itself try and consume them...
                        final WindowInsetsCompat applied =
                                ViewCompat.onApplyWindowInsets(v, originalInsets);
                        if (applied.isConsumed()) {
                            // If the ViewPager consumed all insets, return now
                            return applied;
                        }

                        // Now we'll manually dispatch the insets to our children. Since ViewPager
                        // children are always full-height, we do not want to use the standard
                        // ViewGroup dispatchApplyWindowInsets since if child 0 consumes them,
                        // the rest of the children will not receive any insets. To workaround this
                        // we manually dispatch the applied insets, not allowing children to
                        // consume them from each other. We do however keep track of any insets
                        // which are consumed, returning the union of our children's consumption
                        final Rect res = mTempRect;
                        res.left = applied.getSystemWindowInsetLeft();
                        res.top = applied.getSystemWindowInsetTop();
                        res.right = applied.getSystemWindowInsetRight();
                        res.bottom = applied.getSystemWindowInsetBottom();

                        for (int i = 0, count = getChildCount(); i < count; i++) {
                            final WindowInsetsCompat childInsets = ViewCompat
                                    .dispatchApplyWindowInsets(getChildAt(i), applied);
                            // Now keep track of any consumed by tracking each dimension's min
                            // value
                            res.left = Math.min(childInsets.getSystemWindowInsetLeft(),
                                    res.left);
                            res.top = Math.min(childInsets.getSystemWindowInsetTop(),
                                    res.top);
                            res.right = Math.min(childInsets.getSystemWindowInsetRight(),
                                    res.right);
                            res.bottom = Math.min(childInsets.getSystemWindowInsetBottom(),
                                    res.bottom);
                        }

                        // Now return a new WindowInsets, using the consumed window insets
                        return applied.replaceSystemWindowInsets(
                                res.left, res.top, res.right, res.bottom);
                    }
                });
    }

    public void setOrientation(Orientation orientation) {
        mOrientation = orientation;
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(mEndScrollRunnable);
        // To be on the safe side, abort the scroller
        if ((mScroller != null) && !mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        super.onDetachedFromWindow();
    }

    void setScrollState(int newState) {
        if (mScrollState == newState) {
            return;
        }

        mScrollState = newState;
        if (mPageTransformer != null) {
            // PageTransformers can do complex things that benefit from hardware layers.
            enableLayers(newState != SCROLL_STATE_IDLE);
        }
        dispatchOnScrollStateChanged(newState);
    }

    private void removeNonDecorViews() {
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (!lp.isDecor) {
                removeViewAt(i);
                i--;
            }
        }
    }

    /**
     * Retrieve the current adapter supplying pages.
     *
     * @return The currently registered PagerAdapter
     */
    public PagerAdapter getAdapter() {
        return mAdapter;
    }

    /**
     * Set a PagerAdapter that will supply views for this pager as needed.
     *
     * @param adapter Adapter to use
     */
    public void setAdapter(PagerAdapter adapter) {
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
            mAdapter.startUpdate(this);
            for (int i = 0; i < mItems.size(); i++) {
                final ItemInfo ii = mItems.get(i);
                mAdapter.destroyItem(this, ii.position, ii.object);
            }
            mAdapter.finishUpdate(this);
            mItems.clear();
            removeNonDecorViews();
            mCurItem = 0;
            scrollTo(0, 0);
        }

        final PagerAdapter oldAdapter = mAdapter;
        mAdapter = adapter;
        mExpectedAdapterCount = 0;

        if (mAdapter != null) {
            if (mObserver == null) {
                mObserver = new PagerObserver();
            }
            mAdapter.registerDataSetObserver(mObserver);
            mPopulatePending = false;
            final boolean wasFirstLayout = mFirstLayout;
            mFirstLayout = true;
            mExpectedAdapterCount = mAdapter.getCount();
            if (mRestoredCurItem >= 0) {
                mAdapter.restoreState(mRestoredAdapterState, mRestoredClassLoader);
                setCurrentItemInternal(mRestoredCurItem, false, true);
                mRestoredCurItem = -1;
                mRestoredAdapterState = null;
                mRestoredClassLoader = null;
            } else if (!wasFirstLayout) {
                populate();
            } else {
                requestLayout();
            }
        }

        // Dispatch the change to any listeners
        if (mAdapterChangeListeners != null && !mAdapterChangeListeners.isEmpty()) {
            for (int i = 0, count = mAdapterChangeListeners.size(); i < count; i++) {
                mAdapterChangeListeners.get(i).onAdapterChanged(this, oldAdapter, adapter);
            }
        }
    }

    /**
     * Add a listener that will be invoked whenever the adapter for this ViewPager changes.
     *
     * @param listener listener to add
     */
    public void addOnAdapterChangeListener(@NonNull OnAdapterChangeListener listener) {
        if (mAdapterChangeListeners == null) {
            mAdapterChangeListeners = new ArrayList<>();
        }
        mAdapterChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added via
     * {@link #addOnAdapterChangeListener(OnAdapterChangeListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnAdapterChangeListener(@NonNull OnAdapterChangeListener listener) {
        if (mAdapterChangeListeners != null) {
            mAdapterChangeListeners.remove(listener);
        }
    }

    private int getClientSize() {
        return (mOrientation == Orientation.VERTICAL) ?
                getMeasuredHeight() - getPaddingTop() - getPaddingBottom() :
                getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * Set the currently selected page.
     *
     * @param item         Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    public void setCurrentItem(int item, boolean smoothScroll) {
        mPopulatePending = false;
        setCurrentItemInternal(item, smoothScroll, false);
    }

    public int getCurrentItem() {
        return mCurItem;
    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout with its current adapter there will be a smooth animated transition between
     * the current item and the specified item.
     *
     * @param item Item index to select
     */
    public void setCurrentItem(int item) {
        mPopulatePending = false;
        setCurrentItemInternal(item, !mFirstLayout, false);
    }

    void setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
        setCurrentItemInternal(item, smoothScroll, always, 0);
    }

    void setCurrentItemInternal(int item, boolean smoothScroll, boolean always, int velocity) {
        if (mAdapter == null || mAdapter.getCount() <= 0) {
            setScrollingCacheEnabled(false);
            return;
        }
        if (!always && mCurItem == item && mItems.size() != 0) {
            setScrollingCacheEnabled(false);
            return;
        }

        if (item < 0) {
            item = 0;
        } else if (item >= mAdapter.getCount()) {
            item = mAdapter.getCount() - 1;
        }
        final int pageLimit = mOffscreenPageLimit;
        if (item > (mCurItem + pageLimit) || item < (mCurItem - pageLimit)) {
            // We are doing a jump by more than one page.  To avoid
            // glitches, we want to keep all current pages in the view
            // until the scroll ends.
            for (int i = 0; i < mItems.size(); i++) {
                mItems.get(i).scrolling = true;
            }
        }
        final boolean dispatchSelected = mCurItem != item;

        if (mFirstLayout) {
            // We don't have any idea how big we are yet and shouldn't have any pages either.
            // Just set things up and let the pending layout handle things.
            mCurItem = item;
            if (dispatchSelected) {
                dispatchOnPageSelected(item);
            }
            requestLayout();
        } else {
            populate(item);
            scrollToItem(item, smoothScroll, velocity, dispatchSelected);
        }
    }

    private void scrollToItem(int item, boolean smoothScroll, int velocity,
                              boolean dispatchSelected) {
        final ItemInfo curInfo = infoForPosition(item);
        int dest = 0;
        if (curInfo != null) {
            final int size = getClientSize();
            dest = (int) (size * Math.max(mFirstOffset,
                    Math.min(curInfo.offset, mLastOffset)));
        }
        if (smoothScroll) {
            if (mOrientation == Orientation.VERTICAL) {
                smoothScrollTo(0, dest, velocity);
            } else {
                smoothScrollTo(dest, 0, velocity);
            }
            if (dispatchSelected) {
                dispatchOnPageSelected(item);
            }
        } else {
            if (dispatchSelected) {
                dispatchOnPageSelected(item);
            }
            completeScroll(false);
            if (mOrientation == Orientation.VERTICAL) {
                scrollTo(0, dest);
            } else {
                scrollTo(dest, 0);
            }
            pageScrolled(dest);
        }
    }

    /**
     * Set a listener that will be invoked whenever the page changes or is incrementally
     * scrolled. See {@link ViewPager.OnPageChangeListener}.
     *
     * @param listener Listener to set
     * @deprecated Use {@link #addOnPageChangeListener(ViewPager.OnPageChangeListener)}
     * and {@link #removeOnPageChangeListener(ViewPager.OnPageChangeListener)} instead.
     */
    @Deprecated
    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mOnPageChangeListener = listener;
    }

    /**
     * Add a listener that will be invoked whenever the page changes or is incrementally
     * scrolled. See {@link ViewPager.OnPageChangeListener}.
     * <p>
     * <p>Components that add a listener should take care to remove it when finished.
     * Other components that take ownership of a view may call {@link #clearOnPageChangeListeners()}
     * to remove all attached listeners.</p>
     *
     * @param listener listener to add
     */
    public void addOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        if (mOnPageChangeListeners == null) {
            mOnPageChangeListeners = new ArrayList<>();
        }
        mOnPageChangeListeners.add(listener);
    }

    /**
     * Remove a listener that was previously added via
     * {@link #addOnPageChangeListener(ViewPager.OnPageChangeListener)}.
     *
     * @param listener listener to remove
     */
    public void removeOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        if (mOnPageChangeListeners != null) {
            mOnPageChangeListeners.remove(listener);
        }
    }

    /**
     * Remove all listeners that are notified of any changes in scroll state or position.
     */
    public void clearOnPageChangeListeners() {
        if (mOnPageChangeListeners != null) {
            mOnPageChangeListeners.clear();
        }
    }

    /**
     * Sets a {@link ViewPager.PageTransformer} that will be called for each attached page whenever
     * the scroll position is changed. This allows the application to apply custom property
     * transformations to each page, overriding the default sliding behavior.
     * <p>
     * <p><em>Note:</em> By default, calling this method will cause contained pages to use
     * {@link View#LAYER_TYPE_HARDWARE}. This layer type allows custom alpha transformations,
     * but it will cause issues if any of your pages contain a {@link android.view.SurfaceView}
     * and you have not called {@link android.view.SurfaceView#setZOrderOnTop(boolean)} to put that
     * {@link android.view.SurfaceView} above your app content. To disable this behavior, call
     * {@link #setPageTransformer(boolean, ViewPager.PageTransformer, int)} and pass
     * {@link View#LAYER_TYPE_NONE} for {@code pageLayerType}.</p>
     *
     * @param reverseDrawingOrder true if the supplied PageTransformer requires page views
     *                            to be drawn from last to first instead of first to last.
     * @param transformer         PageTransformer that will modify each page's animation properties
     */
    public void setPageTransformer(boolean reverseDrawingOrder, ViewPager.PageTransformer transformer) {
        setPageTransformer(reverseDrawingOrder, transformer, View.LAYER_TYPE_HARDWARE);
    }

    /**
     * Sets a {@link ViewPager.PageTransformer} that will be called for each attached page whenever
     * the scroll position is changed. This allows the application to apply custom property
     * transformations to each page, overriding the default sliding behavior.
     *
     * @param reverseDrawingOrder true if the supplied PageTransformer requires page views
     *                            to be drawn from last to first instead of first to last.
     * @param transformer         PageTransformer that will modify each page's animation properties
     * @param pageLayerType       View layer type that should be used for ViewPager pages. It should be
     *                            either {@link View#LAYER_TYPE_HARDWARE},
     *                            {@link View#LAYER_TYPE_SOFTWARE}, or
     *                            {@link View#LAYER_TYPE_NONE}.
     */
    public void setPageTransformer(boolean reverseDrawingOrder, ViewPager.PageTransformer transformer,
                                   int pageLayerType) {
        final boolean hasTransformer = transformer != null;
        final boolean needsPopulate = hasTransformer != (mPageTransformer != null);
        mPageTransformer = transformer;
        setChildrenDrawingOrderEnabled(hasTransformer);
        if (hasTransformer) {
            mDrawingOrder = reverseDrawingOrder ? DRAW_ORDER_REVERSE : DRAW_ORDER_FORWARD;
            mPageTransformerLayerType = pageLayerType;
        } else {
            mDrawingOrder = DRAW_ORDER_DEFAULT;
        }
        if (needsPopulate) populate();
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        final int index = mDrawingOrder == DRAW_ORDER_REVERSE ? childCount - 1 - i : i;
        final int result =
                ((LayoutParams) mDrawingOrderedChildren.get(index).getLayoutParams()).childIndex;
        return result;
    }

    /**
     * Set a separate OnPageChangeListener for internal use by the support library.
     *
     * @param listener Listener to set
     * @return The old listener that was set, if any.
     */
    ViewPager.OnPageChangeListener setInternalPageChangeListener(ViewPager.OnPageChangeListener listener) {
        ViewPager.OnPageChangeListener oldListener = mInternalPageChangeListener;
        mInternalPageChangeListener = listener;
        return oldListener;
    }

    /**
     * Returns the number of pages that will be retained to either side of the
     * current page in the view hierarchy in an idle state. Defaults to 1.
     *
     * @return How many pages will be kept offscreen on either side
     * @see #setOffscreenPageLimit(int)
     */
    public int getOffscreenPageLimit() {
        return mOffscreenPageLimit;
    }

    /**
     * Set the number of pages that should be retained to either side of the
     * current page in the view hierarchy in an idle state. Pages beyond this
     * limit will be recreated from the adapter when needed.
     * <p>
     * <p>This is offered as an optimization. If you know in advance the number
     * of pages you will need to support or have lazy-loading mechanisms in place
     * on your pages, tweaking this setting can have benefits in perceived smoothness
     * of paging animations and interaction. If you have a small number of pages (3-4)
     * that you can keep active all at once, less time will be spent in layout for
     * newly created view subtrees as the user pages back and forth.</p>
     * <p>
     * <p>You should keep this limit low, especially if your pages have complex layouts.
     * This setting defaults to 1.</p>
     *
     * @param limit How many pages will be kept offscreen in an idle state.
     */
    public void setOffscreenPageLimit(int limit) {
        if (limit < DEFAULT_OFFSCREEN_PAGES) {
            Log.w(TAG, "Requested offscreen page limit " + limit + " too small; defaulting to "
                    + DEFAULT_OFFSCREEN_PAGES);
            limit = DEFAULT_OFFSCREEN_PAGES;
        }
        if (limit != mOffscreenPageLimit) {
            mOffscreenPageLimit = limit;
            populate();
        }
    }

    /**
     * Return the margin between pages.
     *
     * @return The size of the margin in pixels
     */
    public int getPageMargin() {
        return mPageMargin;
    }

    /**
     * Set the margin between pages.
     *
     * @param marginPixels Distance between adjacent pages in pixels
     * @see #getPageMargin()
     * @see #setPageMarginDrawable(Drawable)
     * @see #setPageMarginDrawable(int)
     */
    public void setPageMargin(int marginPixels) {
        final int oldMargin = mPageMargin;
        mPageMargin = marginPixels;

        final int size = (mOrientation == Orientation.VERTICAL) ? getHeight() : getWidth();
        recomputeScrollPosition(size, size, marginPixels, oldMargin);

        requestLayout();
    }

    /**
     * Set a drawable that will be used to fill the margin between pages.
     *
     * @param d Drawable to display between pages
     */
    public void setPageMarginDrawable(Drawable d) {
        mMarginDrawable = d;
        if (d != null) refreshDrawableState();
        setWillNotDraw(d == null);
        invalidate();
    }

    /**
     * Set a drawable that will be used to fill the margin between pages.
     *
     * @param resId Resource ID of a drawable to display between pages
     */
    public void setPageMarginDrawable(@DrawableRes int resId) {
        setPageMarginDrawable(ContextCompat.getDrawable(getContext(), resId));
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mMarginDrawable;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        final Drawable d = mMarginDrawable;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
    }

    // We want the duration of the page snap animation to be influenced by the distance that
    // the screen has to travel, however, we don't want this duration to be effected in a
    // purely linear fashion. Instead, we use this method to moderate the effect that the distance
    // of travel has on the overall snap duration.
    float distanceInfluenceForSnapDuration(float f) {
        f -= 0.5f; // center the values about 0.
        f *= 0.3f * Math.PI / 2.0f;
        return (float) Math.sin(f);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x the number of pixels to scroll by on the X axis
     * @param y the number of pixels to scroll by on the Y axis
     */
    void smoothScrollTo(int x, int y) {
        smoothScrollTo(x, y, 0);
    }

    /**
     * Like {@link View#scrollBy}, but scroll smoothly instead of immediately.
     *
     * @param x        the number of pixels to scroll by on the X axis
     * @param y        the number of pixels to scroll by on the Y axis
     * @param velocity the velocity associated with a fling, if applicable. (0 otherwise)
     */
    void smoothScrollTo(int x, int y, int velocity) {
        if (getChildCount() == 0) {
            // Nothing to do.
            setScrollingCacheEnabled(false);
            return;
        }

        int sx;
        boolean wasScrolling = (mScroller != null) && !mScroller.isFinished();
        if (wasScrolling) {
            // We're in the middle of a previously initiated scrolling. Check to see
            // whether that scrolling has actually started (if we always call getStartX
            // we can get a stale value from the scroller if it hadn't yet had its first
            // computeScrollOffset call) to decide what is the current scrolling position.
            sx = mIsScrollStarted ? mScroller.getCurrX() : mScroller.getStartX();
            // And abort the current scrolling.
            mScroller.abortAnimation();
            setScrollingCacheEnabled(false);
        } else {
            sx = getScrollX();
        }
        int sy;
        if (wasScrolling) {
            sy = mIsScrollStarted ? mScroller.getCurrY() : mScroller.getStartY();
            mScroller.abortAnimation();
            setScrollingCacheEnabled(false);
        } else {
            sy = getScrollY();
        }
        int dx = x - sx;
        int dy = y - sy;
        if (dx == 0 && dy == 0) {
            completeScroll(false);
            populate();
            setScrollState(SCROLL_STATE_IDLE);
            return;
        }

        setScrollingCacheEnabled(true);
        setScrollState(SCROLL_STATE_SETTLING);

        final int size = getClientSize();
        final int halfSize = size / 2;
        final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / size);
        final float distance = halfSize + halfSize *
                distanceInfluenceForSnapDuration(distanceRatio);

        int duration = 0;
        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
        } else {
            final float pageSize = size * mAdapter.getPageWidth(mCurItem);
            final float pageDelta = (float) Math.abs(dx) / (pageSize + mPageMargin);
            duration = (int) ((pageDelta + 1) * 100);
        }
        duration = Math.min(duration, MAX_SETTLE_DURATION);
        if (isLoop = true) {
            duration = 600;
        }

        // Reset the "scroll started" flag. It will be flipped to true in all places
        // where we call computeScrollOffset().
        mIsScrollStarted = false;
        mScroller.startScroll(sx, sy, dx, dy, duration);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    ItemInfo addNewItem(int position, int index) {
        ItemInfo ii = new ItemInfo();
        ii.position = position;
        ii.object = mAdapter.instantiateItem(this, position);
        ii.sizeFactor = mAdapter.getPageWidth(position);
        if (index < 0 || index >= mItems.size()) {
            mItems.add(ii);
        } else {
            mItems.add(index, ii);
        }
        return ii;
    }

    void dataSetChanged() {
        // This method only gets called if our observer is attached, so mAdapter is non-null.

        final int adapterCount = mAdapter.getCount();
        mExpectedAdapterCount = adapterCount;
        boolean needPopulate = mItems.size() < mOffscreenPageLimit * 2 + 1
                && mItems.size() < adapterCount;
        int newCurrItem = mCurItem;

        boolean isUpdating = false;
        for (int i = 0; i < mItems.size(); i++) {
            final ItemInfo ii = mItems.get(i);
            final int newPos = mAdapter.getItemPosition(ii.object);

            if (newPos == PagerAdapter.POSITION_UNCHANGED) {
                continue;
            }

            if (newPos == PagerAdapter.POSITION_NONE) {
                mItems.remove(i);
                i--;

                if (!isUpdating) {
                    mAdapter.startUpdate(this);
                    isUpdating = true;
                }

                mAdapter.destroyItem(this, ii.position, ii.object);
                needPopulate = true;

                if (mCurItem == ii.position) {
                    // Keep the current item in the valid range
                    newCurrItem = Math.max(0, Math.min(mCurItem, adapterCount - 1));
                    needPopulate = true;
                }
                continue;
            }

            if (ii.position != newPos) {
                if (ii.position == mCurItem) {
                    // Our current item changed position. Follow it.
                    newCurrItem = newPos;
                }

                ii.position = newPos;
                needPopulate = true;
            }
        }

        if (isUpdating) {
            mAdapter.finishUpdate(this);
        }

        Collections.sort(mItems, COMPARATOR);

        if (needPopulate) {
            // Reset our known page widths; populate will recompute them.
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (!lp.isDecor) {
                    lp.widthFactor = 0.f;
                    lp.heightFactor = 0.f;
                }
            }

            setCurrentItemInternal(newCurrItem, false, true);
            requestLayout();
        }
    }

    void populate() {
        populate(mCurItem);
    }

    void populate(int newCurrentItem) {
        ItemInfo oldCurInfo = null;
        if (mCurItem != newCurrentItem) {
            oldCurInfo = infoForPosition(mCurItem);
            mCurItem = newCurrentItem;
        }

        if (mAdapter == null) {
            sortChildDrawingOrder();
            return;
        }

        // Bail now if we are waiting to populate.  This is to hold off
        // on creating views from the time the user releases their finger to
        // fling to a new position until we have finished the scroll to
        // that position, avoiding glitches from happening at that point.
        if (mPopulatePending) {
            if (DEBUG) Log.i(TAG, "populate is pending, skipping for now...");
            sortChildDrawingOrder();
            return;
        }

        // Also, don't populate until we are attached to a window.  This is to
        // avoid trying to populate before we have restored our view hierarchy
        // state and conflicting with what is restored.
        if (getWindowToken() == null) {
            return;
        }

        mAdapter.startUpdate(this);

        final int pageLimit = mOffscreenPageLimit;
        final int startPos = Math.max(0, mCurItem - pageLimit);
        final int N = mAdapter.getCount();
        final int endPos = Math.min(N - 1, mCurItem + pageLimit);

        if (N != mExpectedAdapterCount) {
            String resName;
            try {
                resName = getResources().getResourceName(getId());
            } catch (Resources.NotFoundException e) {
                resName = Integer.toHexString(getId());
            }
            throw new IllegalStateException("The application's PagerAdapter changed the adapter's"
                    + " contents without calling PagerAdapter#notifyDataSetChanged!"
                    + " Expected adapter item count: " + mExpectedAdapterCount + ", found: " + N
                    + " Pager id: " + resName
                    + " Pager class: " + getClass()
                    + " Problematic adapter: " + mAdapter.getClass());
        }

        // Locate the currently focused item or add it if needed.
        int curIndex = -1;
        ItemInfo curItem = null;
        for (curIndex = 0; curIndex < mItems.size(); curIndex++) {
            final ItemInfo ii = mItems.get(curIndex);
            if (ii.position >= mCurItem) {
                if (ii.position == mCurItem) curItem = ii;
                break;
            }
        }

        if (curItem == null && N > 0) {
            curItem = addNewItem(mCurItem, curIndex);
        }

        // Fill 3x the available width or up to the number of offscreen
        // pages requested to either side, whichever is larger.
        // If we have no current item we have no work to do.
        if (curItem != null) {
            float extraSizeTopLeft = 0.f;
            int itemIndex = curIndex - 1;
            ItemInfo ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
            final int clientSize = getClientSize();
            final float topLeftSizeNeeded = clientSize <= 0 ? 0 :
                    2.f - curItem.sizeFactor + (float) getPaddingLeft() / (float) clientSize;
            for (int pos = mCurItem - 1; pos >= 0; pos--) {
                if (extraSizeTopLeft >= topLeftSizeNeeded && pos < startPos) {
                    if (ii == null) {
                        break;
                    }
                    if (pos == ii.position && !ii.scrolling) {
                        mItems.remove(itemIndex);
                        mAdapter.destroyItem(this, pos, ii.object);
                        if (DEBUG) {
                            Log.i(TAG, "populate() - destroyItem() with pos: " + pos +
                                    " view: " + ((View) ii.object));
                        }
                        itemIndex--;
                        curIndex--;
                        ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                    }
                } else if (ii != null && pos == ii.position) {
                    extraSizeTopLeft += ii.sizeFactor;
                    itemIndex--;
                    ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                } else {
                    ii = addNewItem(pos, itemIndex + 1);
                    extraSizeTopLeft += ii.sizeFactor;
                    curIndex++;
                    ii = itemIndex >= 0 ? mItems.get(itemIndex) : null;
                }
            }

            float extraSizeBottomRight = curItem.sizeFactor;
            itemIndex = curIndex + 1;
            if (extraSizeBottomRight < 2.f) {
                ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                final float bottomRightSizeNeeded = clientSize <= 0 ? 0 :
                        (float) getPaddingRight() / (float) clientSize + 2.f;
                for (int pos = mCurItem + 1; pos < N; pos++) {
                    if (extraSizeBottomRight >= bottomRightSizeNeeded && pos > endPos) {
                        if (ii == null) {
                            break;
                        }
                        if (pos == ii.position && !ii.scrolling) {
                            mItems.remove(itemIndex);
                            mAdapter.destroyItem(this, pos, ii.object);
                            if (DEBUG) {
                                Log.i(TAG, "populate() - destroyItem() with pos: " + pos +
                                        " view: " + ((View) ii.object));
                            }
                            ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                        }
                    } else if (ii != null && pos == ii.position) {
                        extraSizeBottomRight += ii.sizeFactor;
                        itemIndex++;
                        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                    } else {
                        ii = addNewItem(pos, itemIndex);
                        itemIndex++;
                        extraSizeBottomRight += ii.sizeFactor;
                        ii = itemIndex < mItems.size() ? mItems.get(itemIndex) : null;
                    }
                }
            }

            calculatePageOffsets(curItem, curIndex, oldCurInfo);
        }

        if (DEBUG) {
            Log.i(TAG, "Current page list:");
            for (int i = 0; i < mItems.size(); i++) {
                Log.i(TAG, "#" + i + ": page " + mItems.get(i).position);
            }
        }

        mAdapter.setPrimaryItem(this, mCurItem, curItem != null ? curItem.object : null);

        mAdapter.finishUpdate(this);

        // Check width measurement of current pages and drawing sort order.
        // Update LayoutParams as needed.
        final int childCount = getChildCount();
        if (mOrientation == Orientation.VERTICAL) {
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                lp.childIndex = i;
                if (!lp.isDecor && lp.heightFactor == 0.f) {
                    // 0 means requery the adapter for this, it doesn't have a valid width
                    // .
                    final ItemInfo ii = infoForChild(child);
                    if (ii != null) {
                        lp.heightFactor = ii.sizeFactor;
                        lp.position = ii.position;
                    }
                }
            }
        } else {
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                lp.childIndex = i;
                if (!lp.isDecor && lp.widthFactor == 0.f) {
                    // 0 means requery the adapter for this, it doesn't have a valid width.
                    final ItemInfo ii = infoForChild(child);
                    if (ii != null) {
                        lp.widthFactor = ii.sizeFactor;
                        lp.position = ii.position;
                    }
                }
            }
        }
        sortChildDrawingOrder();

        if (hasFocus()) {
            View currentFocused = findFocus();
            ItemInfo ii = currentFocused != null ? infoForAnyChild(currentFocused) : null;
            if (ii == null || ii.position != mCurItem) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    ii = infoForChild(child);
                    if (ii != null && ii.position == mCurItem) {
                        if (child.requestFocus(View.FOCUS_FORWARD)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void sortChildDrawingOrder() {
        if (mDrawingOrder != DRAW_ORDER_DEFAULT) {
            if (mDrawingOrderedChildren == null) {
                mDrawingOrderedChildren = new ArrayList<View>();
            } else {
                mDrawingOrderedChildren.clear();
            }
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                mDrawingOrderedChildren.add(child);
            }
            Collections.sort(mDrawingOrderedChildren, sPositionComparator);
        }
    }

    private void calculatePageOffsets(ItemInfo curItem, int curIndex, ItemInfo oldCurInfo) {
        final int N = mAdapter.getCount();
        final int size = getClientSize();
        final float marginOffset = size > 0 ? (float) mPageMargin / size : 0;
        // Fix up offsets for later layout.
        if (oldCurInfo != null) {
            final int oldCurPosition = oldCurInfo.position;
            // Base offsets off of oldCurInfo.
            if (oldCurPosition < curItem.position) {
                int itemIndex = 0;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset + oldCurInfo.sizeFactor + marginOffset;
                for (int pos = oldCurPosition + 1;
                     pos <= curItem.position && itemIndex < mItems.size(); pos++) {
                    ii = mItems.get(itemIndex);
                    while (pos > ii.position && itemIndex < mItems.size() - 1) {
                        itemIndex++;
                        ii = mItems.get(itemIndex);
                    }
                    while (pos < ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset += mAdapter.getPageWidth(pos) + marginOffset;
                        pos++;
                    }
                    ii.offset = offset;
                    offset += ii.sizeFactor + marginOffset;
                }
            } else if (oldCurPosition > curItem.position) {
                int itemIndex = mItems.size() - 1;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset;
                for (int pos = oldCurPosition - 1;
                     pos >= curItem.position && itemIndex >= 0; pos--) {
                    ii = mItems.get(itemIndex);
                    while (pos < ii.position && itemIndex > 0) {
                        itemIndex--;
                        ii = mItems.get(itemIndex);
                    }
                    while (pos > ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset -= mAdapter.getPageWidth(pos) + marginOffset;
                        pos--;
                    }
                    offset -= ii.sizeFactor + marginOffset;
                    ii.offset = offset;
                }
            }
        }

        // Base all offsets off of curItem.
        final int itemCount = mItems.size();
        float offset = curItem.offset;
        int pos = curItem.position - 1;
        mFirstOffset = curItem.position == 0 ? curItem.offset : -Float.MAX_VALUE;
        mLastOffset = curItem.position == N - 1
                ? curItem.offset + curItem.sizeFactor - 1 : Float.MAX_VALUE;
        // Previous pages
        for (int i = curIndex - 1; i >= 0; i--, pos--) {
            final ItemInfo ii = mItems.get(i);
            while (pos > ii.position) {
                offset -= mAdapter.getPageWidth(pos--) + marginOffset;
            }
            offset -= ii.sizeFactor + marginOffset;
            ii.offset = offset;
            if (ii.position == 0) mFirstOffset = offset;
        }
        offset = curItem.offset + curItem.sizeFactor + marginOffset;
        pos = curItem.position + 1;
        // Next pages
        for (int i = curIndex + 1; i < itemCount; i++, pos++) {
            final ItemInfo ii = mItems.get(i);
            while (pos < ii.position) {
                offset += mAdapter.getPageWidth(pos++) + marginOffset;
            }
            if (ii.position == N - 1) {
                mLastOffset = offset + ii.sizeFactor - 1;
            }
            ii.offset = offset;
            offset += ii.sizeFactor + marginOffset;
        }

        mNeedCalculatePageOffsets = false;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.position = mCurItem;
        if (mAdapter != null) {
            ss.adapterState = mAdapter.saveState();
        }
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (mAdapter != null) {
            mAdapter.restoreState(ss.adapterState, ss.loader);
            setCurrentItemInternal(ss.position, false, true);
        } else {
            mRestoredCurItem = ss.position;
            mRestoredAdapterState = ss.adapterState;
            mRestoredClassLoader = ss.loader;
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }
        final LayoutParams lp = (LayoutParams) params;
        // Any views added via inflation should be classed as part of the decor
        lp.isDecor |= isDecorView(child);
        if (mInLayout) {
            if (lp != null && lp.isDecor) {
                throw new IllegalStateException("Cannot add pager decor view during layout");
            }
            lp.needsMeasure = true;
            addViewInLayout(child, index, params);
        } else {
            super.addView(child, index, params);
        }

        if (USE_CACHE) {
            if (child.getVisibility() != GONE) {
                child.setDrawingCacheEnabled(mScrollingCacheEnabled);
            } else {
                child.setDrawingCacheEnabled(false);
            }
        }
    }

    @Override
    public void removeView(View view) {
        if (mInLayout) {
            removeViewInLayout(view);
        } else {
            super.removeView(view);
        }
    }

    ItemInfo infoForChild(View child) {
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (mAdapter.isViewFromObject(child, ii.object)) {
                return ii;
            }
        }
        return null;
    }

    ItemInfo infoForAnyChild(View child) {
        ViewParent parent;
        while ((parent = child.getParent()) != this) {
            if (parent == null || !(parent instanceof View)) {
                return null;
            }
            child = (View) parent;
        }
        return infoForChild(child);
    }

    ItemInfo infoForPosition(int position) {
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (ii.position == position) {
                return ii;
            }
        }
        return null;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // For simple implementation, our internal size is always 0.
        // We depend on the container to specify the layout size of
        // our view.  We can't really know what it is since we will be
        // adding and removing different arbitrary views and do not
        // want the layout to change as this happens.
        setMeasuredDimension(getDefaultSize(0, widthMeasureSpec),
                getDefaultSize(0, heightMeasureSpec));

        final int measuredSize = (mOrientation == Orientation.VERTICAL) ? getMeasuredHeight() : getMeasuredWidth();
        final int maxGutterSize = measuredSize / 10;
        mGutterSize = Math.min(maxGutterSize, mDefaultGutterSize);

        // Children are just made to fill our space.
        int childWidthSize;
        int childHeightSize;
        if (mOrientation == Orientation.VERTICAL) {
            childWidthSize = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
            childHeightSize = measuredSize - getPaddingTop() - getPaddingBottom();
        } else {
            childWidthSize = measuredSize - getPaddingLeft() - getPaddingRight();
            childHeightSize = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        }

        /*
         * Make sure all children have been properly measured. Decor views first.
         * Right now we cheat and make this less complicated by assuming decor
         * views won't intersect. We will pin to edges based on gravity.
         */
        int size = getChildCount();
        for (int i = 0; i < size; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp != null && lp.isDecor) {
                    final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                    int widthMode = MeasureSpec.AT_MOST;
                    int heightMode = MeasureSpec.AT_MOST;
                    boolean consumeVertical = vgrav == Gravity.TOP || vgrav == Gravity.BOTTOM;
                    boolean consumeHorizontal = hgrav == Gravity.LEFT || hgrav == Gravity.RIGHT;

                    if (consumeVertical) {
                        widthMode = MeasureSpec.EXACTLY;
                    } else if (consumeHorizontal) {
                        heightMode = MeasureSpec.EXACTLY;
                    }

                    int widthSize = childWidthSize;
                    int heightSize = childHeightSize;
                    if (lp.width != LayoutParams.WRAP_CONTENT) {
                        widthMode = MeasureSpec.EXACTLY;
                        if (lp.width != LayoutParams.MATCH_PARENT) {
                            widthSize = lp.width;
                        }
                    }
                    if (lp.height != LayoutParams.WRAP_CONTENT) {
                        heightMode = MeasureSpec.EXACTLY;
                        if (lp.height != LayoutParams.MATCH_PARENT) {
                            heightSize = lp.height;
                        }
                    }
                    final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, widthMode);
                    final int heightSpec = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
                    child.measure(widthSpec, heightSpec);

                    if (consumeVertical) {
                        childHeightSize -= child.getMeasuredHeight();
                    } else if (consumeHorizontal) {
                        childWidthSize -= child.getMeasuredWidth();
                    }
                }
            }
        }

        mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY);
        mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeightSize, MeasureSpec.EXACTLY);

        // Make sure we have created all fragments that we need to have shown.
        mInLayout = true;
        populate();
        mInLayout = false;

        // Page views next.
        size = getChildCount();
        for (int i = 0; i < size; ++i) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                if (DEBUG) {
                    Log.v(TAG, "Measuring #" + i + " " + child + ": " + mChildWidthMeasureSpec);
                }

                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp == null || !lp.isDecor) {
                    if (mOrientation == Orientation.VERTICAL) {
                        final int heightSpec = MeasureSpec.makeMeasureSpec(
                                (int) (childHeightSize * lp.heightFactor), MeasureSpec.EXACTLY);
                        child.measure(mChildWidthMeasureSpec, heightSpec);
                    } else {
                        final int widthSpec = MeasureSpec.makeMeasureSpec(
                                (int) (childWidthSize * lp.widthFactor), MeasureSpec.EXACTLY);
                        child.measure(widthSpec, mChildHeightMeasureSpec);
                    }
                }
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Make sure scroll position is set correctly.
        if (mOrientation == Orientation.VERTICAL) {
            if (h != oldh) {
                recomputeScrollPosition(h, oldh, mPageMargin, mPageMargin);
            }
        } else {
            if (w != oldw) {
                recomputeScrollPosition(w, oldw, mPageMargin, mPageMargin);
            }
        }
    }

    private void recomputeScrollPosition(int size, int oldSize, int margin, int oldMargin) {
        if (mOrientation == Orientation.VERTICAL) {
            if (oldSize > 0 && !mItems.isEmpty()) {
                if (!mScroller.isFinished()) {
                    mScroller.setFinalY(getCurrentItem() * getClientSize());
                } else {
                    final int heightWithMargin = size - getPaddingTop() - getPaddingBottom() + margin;
                    final int oldHeightWithMargin = oldSize - getPaddingTop() - getPaddingBottom()
                            + oldMargin;
                    final int ypos = getScrollY();
                    final float pageOffset = (float) ypos / oldHeightWithMargin;
                    final int newOffsetPixels = (int) (pageOffset * heightWithMargin);

                    scrollTo(getScrollX(), newOffsetPixels);
                }
            } else {
                final ItemInfo ii = infoForPosition(mCurItem);
                final float scrollOffset = ii != null ? Math.min(ii.offset, mLastOffset) : 0;
                final int scrollPos = (int) (scrollOffset *
                        (size - getPaddingTop() - getPaddingBottom()));
                if (scrollPos != getScrollY()) {
                    completeScroll(false);
                    scrollTo(getScrollX(), scrollPos);
                }
            }
        } else {
            if (oldSize > 0 && !mItems.isEmpty()) {
                if (!mScroller.isFinished()) {
                    mScroller.setFinalX(getCurrentItem() * getClientSize());
                } else {
                    final int widthWithMargin = size - getPaddingLeft() - getPaddingRight() + margin;
                    final int oldWidthWithMargin = oldSize - getPaddingLeft() - getPaddingRight()
                            + oldMargin;
                    final int xpos = getScrollX();
                    final float pageOffset = (float) xpos / oldWidthWithMargin;
                    final int newOffsetPixels = (int) (pageOffset * widthWithMargin);

                    scrollTo(newOffsetPixels, getScrollY());
                }
            } else {
                final ItemInfo ii = infoForPosition(mCurItem);
                final float scrollOffset = ii != null ? Math.min(ii.offset, mLastOffset) : 0;
                final int scrollPos =
                        (int) (scrollOffset * (size - getPaddingLeft() - getPaddingRight()));
                if (scrollPos != getScrollX()) {
                    completeScroll(false);
                    scrollTo(scrollPos, getScrollY());
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        int width = r - l;
        int height = b - t;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        final int scroll = (mOrientation == Orientation.VERTICAL) ? getScrollY() : getScrollX();

        int decorCount = 0;

        // First pass - decor views. We need to do this in two passes so that
        // we have the proper offsets for non-decor views later.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int childLeft = 0;
                int childTop = 0;
                if (lp.isDecor) {
                    final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                    switch (hgrav) {
                        default:
                            childLeft = paddingLeft;
                            break;
                        case Gravity.LEFT:
                            childLeft = paddingLeft;
                            paddingLeft += child.getMeasuredWidth();
                            break;
                        case Gravity.CENTER_HORIZONTAL:
                            childLeft = Math.max((width - child.getMeasuredWidth()) / 2,
                                    paddingLeft);
                            break;
                        case Gravity.RIGHT:
                            childLeft = width - paddingRight - child.getMeasuredWidth();
                            paddingRight += child.getMeasuredWidth();
                            break;
                    }
                    switch (vgrav) {
                        default:
                            childTop = paddingTop;
                            break;
                        case Gravity.TOP:
                            childTop = paddingTop;
                            paddingTop += child.getMeasuredHeight();
                            break;
                        case Gravity.CENTER_VERTICAL:
                            childTop = Math.max((height - child.getMeasuredHeight()) / 2,
                                    paddingTop);
                            break;
                        case Gravity.BOTTOM:
                            childTop = height - paddingBottom - child.getMeasuredHeight();
                            paddingBottom += child.getMeasuredHeight();
                            break;
                    }
                    if (mOrientation == Orientation.VERTICAL) {
                        childTop += scroll;
                    } else {
                        childLeft += scroll;
                    }
                    child.layout(childLeft, childTop,
                            childLeft + child.getMeasuredWidth(),
                            childTop + child.getMeasuredHeight());
                    decorCount++;
                }
            }
        }

//        final int childWidth = width - paddingLeft - paddingRight;
        final int childSize = (mOrientation == Orientation.VERTICAL) ? height - paddingTop - paddingBottom : width - paddingLeft - paddingRight;
        // Page views. Do this once we have the right padding offsets from above.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                ItemInfo ii;
                if (!lp.isDecor && (ii = infoForChild(child)) != null) {
                    int topLeftoff = (int) (childSize * ii.offset);
                    int childLeft;
                    int childTop;
                    if (mOrientation == Orientation.VERTICAL) {
                        childLeft = paddingLeft;
                        childTop = paddingTop + topLeftoff;
                        if (lp.needsMeasure) {
                            // This was added during layout and needs measurement.
                            // Do it now that we know what we're working with.
                            lp.needsMeasure = false;
                            final int widthSpec = MeasureSpec.makeMeasureSpec(
                                    (int) (width - paddingLeft - paddingRight),
                                    MeasureSpec.EXACTLY);
                            final int heightSpec = MeasureSpec.makeMeasureSpec(
                                    (int) (childSize * lp.heightFactor),
                                    MeasureSpec.EXACTLY);
                            child.measure(widthSpec, heightSpec);
                        }
                    } else {
                        childLeft = paddingLeft + topLeftoff;
                        childTop = paddingTop;
                        if (lp.needsMeasure) {
                            // This was added during layout and needs measurement.
                            // Do it now that we know what we're working with.
                            lp.needsMeasure = false;
                            final int widthSpec = MeasureSpec.makeMeasureSpec(
                                    (int) (childSize * lp.widthFactor),
                                    MeasureSpec.EXACTLY);
                            final int heightSpec = MeasureSpec.makeMeasureSpec(
                                    (int) (height - paddingTop - paddingBottom),
                                    MeasureSpec.EXACTLY);
                            child.measure(widthSpec, heightSpec);
                        }
                    }
                    if (DEBUG) {
                        Log.v(TAG, "Positioning #" + i + " " + child + " f=" + ii.object
                                + ":" + childLeft + "," + childTop + " " + child.getMeasuredWidth()
                                + "x" + child.getMeasuredHeight());
                    }
                    child.layout(childLeft, childTop,
                            childLeft + child.getMeasuredWidth(),
                            childTop + child.getMeasuredHeight());
                }
            }
        }

        mTopLeftPageBounds = (mOrientation == Orientation.VERTICAL) ? paddingLeft : paddingTop;
        mBottomRightPageBounds = (mOrientation == Orientation.VERTICAL) ? width - paddingRight : height - paddingBottom;
        mDecorChildCount = decorCount;

        if (mFirstLayout) {
            scrollToItem(mCurItem, false, 0, false);
        }
        mFirstLayout = false;
    }

    @Override
    public void computeScroll() {
        mIsScrollStarted = true;
        if (!mScroller.isFinished() && mScroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();

            if (oldX != x || oldY != y) {
                scrollTo(x, y);
                if (mOrientation == Orientation.VERTICAL) {
                    if (!pageScrolled(y)) {
                        mScroller.abortAnimation();
                        scrollTo(x, 0);
                    }
                } else {
                    if (!pageScrolled(x)) {
                        mScroller.abortAnimation();
                        scrollTo(0, y);
                    }
                }
            }

            // Keep on drawing until the animation has finished.
            ViewCompat.postInvalidateOnAnimation(this);
            return;
        }

        // Done with scroll, clean up state.
        completeScroll(true);
    }

    private boolean pageScrolled(int pos) {
        if (mItems.size() == 0) {
            if (mFirstLayout) {
                // If we haven't been laid out yet, we probably just haven't been populated yet.
                // Let's skip this call since it doesn't make sense in this state
                return false;
            }
            mCalledSuper = false;
            onPageScrolled(0, 0, 0);
            if (!mCalledSuper) {
                throw new IllegalStateException(
                        "onPageScrolled did not call superclass implementation");
            }
            return false;
        }
        final ItemInfo ii = infoForCurrentScrollPosition();
        final int size = getClientSize();
        final int sizeWithMargin = size + mPageMargin;
        final float marginOffset = (float) mPageMargin / size;
        final int currentPage = ii.position;
        final float pageOffset = (((float) pos / size) - ii.offset)
                / (ii.sizeFactor + marginOffset);
        final int offsetPixels = (int) (pageOffset * sizeWithMargin);

        mCalledSuper = false;
        onPageScrolled(currentPage, pageOffset, offsetPixels);
        if (!mCalledSuper) {
            throw new IllegalStateException(
                    "onPageScrolled did not call superclass implementation");
        }
        return true;
    }

    /**
     * This method will be invoked when the current page is scrolled, either as part
     * of a programmatically initiated smooth scroll or a user initiated touch scroll.
     * If you override this method you must call through to the superclass implementation
     * (e.g. super.onPageScrolled(position, offset, offsetPixels)) before onPageScrolled
     * returns.
     *
     * @param position     Position index of the first page currently being displayed.
     *                     Page position+1 will be visible if positionOffset is nonzero.
     * @param offset       Value from [0, 1) indicating the offset from the page at position.
     * @param offsetPixels Value in pixels indicating the offset from position.
     */
    @CallSuper
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        // Offset any decor views if needed - keep them on-screen at all times.
        if (mDecorChildCount > 0) {
            if (mOrientation == Orientation.VERTICAL) {
                final int scrollY = getScrollY();
                int paddingTop = getPaddingTop();
                int paddingBottom = getPaddingBottom();
                final int height = getHeight();
                final int childCount = getChildCount();
                for (int i = 0; i < childCount; i++) {
                    final View child = getChildAt(i);
                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    if (!lp.isDecor) continue;

                    final int vgrav = lp.gravity & Gravity.VERTICAL_GRAVITY_MASK;
                    int childTop = 0;
                    switch (vgrav) {
                        default:
                            childTop = paddingTop;
                            break;
                        case Gravity.TOP:
                            childTop = paddingTop;
                            paddingTop += child.getHeight();
                            break;
                        case Gravity.CENTER_VERTICAL:
                            childTop = Math.max((height - child.getMeasuredHeight()) / 2,
                                    paddingTop);
                            break;
                        case Gravity.BOTTOM:
                            childTop = height - paddingBottom - child.getMeasuredHeight();
                            paddingBottom += child.getMeasuredHeight();
                            break;
                    }
                    childTop += scrollY;
                    final int childOffset = childTop - child.getTop();
                    if (childOffset != 0) {
                        child.offsetTopAndBottom(childOffset);
                    }
                }
            } else {
                final int scrollX = getScrollX();
                int paddingLeft = getPaddingLeft();
                int paddingRight = getPaddingRight();
                final int width = getWidth();
                final int childCount = getChildCount();
                for (int i = 0; i < childCount; i++) {
                    final View child = getChildAt(i);
                    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    if (!lp.isDecor) continue;

                    final int hgrav = lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
                    int childLeft = 0;
                    switch (hgrav) {
                        default:
                            childLeft = paddingLeft;
                            break;
                        case Gravity.LEFT:
                            childLeft = paddingLeft;
                            paddingLeft += child.getWidth();
                            break;
                        case Gravity.CENTER_HORIZONTAL:
                            childLeft = Math.max((width - child.getMeasuredWidth()) / 2,
                                    paddingLeft);
                            break;
                        case Gravity.RIGHT:
                            childLeft = width - paddingRight - child.getMeasuredWidth();
                            paddingRight += child.getMeasuredWidth();
                            break;
                    }
                    childLeft += scrollX;

                    final int childOffset = childLeft - child.getLeft();
                    if (childOffset != 0) {
                        child.offsetLeftAndRight(childOffset);
                    }
                }
            }
        }

        dispatchOnPageScrolled(position, offset, offsetPixels);

        if (mPageTransformer != null) {
            final int scroll = (mOrientation == Orientation.VERTICAL) ? getScrollY() : getScrollX();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                if (lp.isDecor) continue;
                final float transformPos = (float) (((mOrientation == Orientation.VERTICAL) ? child.getTop() : child.getLeft()) - scroll) / getClientSize();
                mPageTransformer.transformPage(child, transformPos);
            }
        }

        mCalledSuper = true;
    }

    private void dispatchOnPageScrolled(int position, float offset, int offsetPixels) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrolled(position, offset, offsetPixels);
        }
        if (mOnPageChangeListeners != null) {
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                ViewPager.OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageScrolled(position, offset, offsetPixels);
                }
            }
        }
        if (mInternalPageChangeListener != null) {
            mInternalPageChangeListener.onPageScrolled(position, offset, offsetPixels);
        }
    }

    private void dispatchOnPageSelected(int position) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageSelected(position);
        }
        if (mOnPageChangeListeners != null) {
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                ViewPager.OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageSelected(position);
                }
            }
        }
        if (mInternalPageChangeListener != null) {
            mInternalPageChangeListener.onPageSelected(position);
        }
    }

    private void dispatchOnScrollStateChanged(int state) {
        if (mOnPageChangeListener != null) {
            mOnPageChangeListener.onPageScrollStateChanged(state);
        }
        if (mOnPageChangeListeners != null) {
            for (int i = 0, z = mOnPageChangeListeners.size(); i < z; i++) {
                ViewPager.OnPageChangeListener listener = mOnPageChangeListeners.get(i);
                if (listener != null) {
                    listener.onPageScrollStateChanged(state);
                }
            }
        }
        if (mInternalPageChangeListener != null) {
            mInternalPageChangeListener.onPageScrollStateChanged(state);
        }
    }

    private void completeScroll(boolean postEvents) {
        boolean needPopulate = mScrollState == SCROLL_STATE_SETTLING;
        if (needPopulate) {
            // Done with scroll, no longer want to cache view drawing.
            setScrollingCacheEnabled(false);
            boolean wasScrolling = !mScroller.isFinished();
            if (wasScrolling) {
                mScroller.abortAnimation();
                int oldX = getScrollX();
                int oldY = getScrollY();
                int x = mScroller.getCurrX();
                int y = mScroller.getCurrY();
                if (oldX != x || oldY != y) {
                    scrollTo(x, y);
                    if (x != oldX) {
                        pageScrolled(x);
                    }
                    if (y != oldY) {
                        pageScrolled(y);
                    }
                }
            }
        }
        mPopulatePending = false;
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            if (ii.scrolling) {
                needPopulate = true;
                ii.scrolling = false;
            }
        }
        if (needPopulate) {
            if (postEvents) {
                ViewCompat.postOnAnimation(this, mEndScrollRunnable);
            } else {
                mEndScrollRunnable.run();
            }
        }
    }

    private boolean isGutterDrag(float axis, float dAxis) {
        return (axis < mGutterSize && dAxis > 0) || (axis > (mOrientation == Orientation.VERTICAL ? getHeight() : getWidth()) - mGutterSize && dAxis < 0);
    }

    private void enableLayers(boolean enable) {
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final int layerType = enable
                    ? mPageTransformerLayerType : View.LAYER_TYPE_NONE;
            getChildAt(i).setLayerType(layerType, null);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */

        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        // Always take care of the touch gesture being complete.
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            // Release the drag.
            if (DEBUG) Log.v(TAG, "Intercept done!");
            resetTouch();
            return false;
        }

        // Nothing more to do here if we have decided whether or not we
        // are dragging.
        if (action != MotionEvent.ACTION_DOWN) {
            if (mIsBeingDragged) {
                if (DEBUG) Log.v(TAG, "Intercept returning true!");
                return true;
            }
            if (mIsUnableToDrag) {
                if (DEBUG) Log.v(TAG, "Intercept returning false!");
                return false;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */

                /*
                * Locally do absolute value. mLastMotionY is set to the y value
                * of the down event.
                */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = ev.findPointerIndex(activePointerId);
                if (mOrientation == Orientation.VERTICAL) {
                    final float y = MotionEventCompat.getY(ev, pointerIndex);
                    final float dy = y - mLastMotionY;
                    final float yDiff = Math.abs(dy);
                    final float x = MotionEventCompat.getX(ev, pointerIndex);
                    final float xDiff = Math.abs(x - mInitialMotionX);
                    if (DEBUG)
                        Log.v(TAG, "Moved x to " + x + "," + y + " diff=" + xDiff + "," + yDiff);

                    if (dy != 0 && !isGutterDrag(mLastMotionY, dy) &&
                            canScroll(this, false, (int) dy, (int) x, (int) y)) {
                        // Nested view has scrollable area under this point. Let it be handled there.
                        mLastMotionX = x;
                        mLastMotionY = y;
                        mIsUnableToDrag = true;
                        return false;
                    }
                    if (yDiff > mTouchSlop && yDiff * 0.5f > xDiff) {
                        if (DEBUG) Log.v(TAG, "Starting drag!");
                        mIsBeingDragged = true;
                        requestParentDisallowInterceptTouchEvent(true);
                        setScrollState(SCROLL_STATE_DRAGGING);
                        mLastMotionY = dy > 0 ? mInitialMotionY + mTouchSlop :
                                mInitialMotionY - mTouchSlop;
                        mLastMotionX = x;
                        setScrollingCacheEnabled(true);
                    } else if (xDiff > mTouchSlop) {
                        // The finger has moved enough in the vertical
                        // direction to be counted as a drag...  abort
                        // any attempt to drag horizontally, to work correctly
                        // with children that have scrolling containers.
                        if (DEBUG) Log.v(TAG, "Starting unable to drag!");
                        mIsUnableToDrag = true;
                    }
                    if (mIsBeingDragged) {
                        // Scroll to follow the motion event
                        if (performDrag(y)) {
                            ViewCompat.postInvalidateOnAnimation(this);
                        }
                    }
                } else {
                    final float x = ev.getX(pointerIndex);
                    final float dx = x - mLastMotionX;
                    final float xDiff = Math.abs(dx);
                    final float y = ev.getY(pointerIndex);
                    final float yDiff = Math.abs(y - mInitialMotionY);
                    if (DEBUG)
                        Log.v(TAG, "Moved x to " + x + "," + y + " diff=" + xDiff + "," + yDiff);

                    if (dx != 0 && !isGutterDrag(mLastMotionX, dx)
                            && canScroll(this, false, (int) dx, (int) x, (int) y)) {
                        // Nested view has scrollable area under this point. Let it be handled there.
                        mLastMotionX = x;
                        mLastMotionY = y;
                        mIsUnableToDrag = true;
                        return false;
                    }
                    if (xDiff > mTouchSlop && xDiff * 0.5f > yDiff) {
                        if (DEBUG) Log.v(TAG, "Starting drag!");
                        mIsBeingDragged = true;
                        requestParentDisallowInterceptTouchEvent(true);
                        setScrollState(SCROLL_STATE_DRAGGING);
                        mLastMotionX = dx > 0
                                ? mInitialMotionX + mTouchSlop : mInitialMotionX - mTouchSlop;
                        mLastMotionY = y;
                        setScrollingCacheEnabled(true);
                    } else if (yDiff > mTouchSlop) {
                        // The finger has moved enough in the vertical
                        // direction to be counted as a drag...  abort
                        // any attempt to drag horizontally, to work correctly
                        // with children that have scrolling containers.
                        if (DEBUG) Log.v(TAG, "Starting unable to drag!");
                        mIsUnableToDrag = true;
                    }
                    if (mIsBeingDragged) {
                        // Scroll to follow the motion event
                        if (performDrag(x)) {
                            ViewCompat.postInvalidateOnAnimation(this);
                        }
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = ev.getPointerId(0);
                mIsUnableToDrag = false;

                mIsScrollStarted = true;
                mScroller.computeScrollOffset();
                if (mOrientation == Orientation.VERTICAL) {
                    if (mScrollState == SCROLL_STATE_SETTLING &&
                            Math.abs(mScroller.getFinalY() - mScroller.getCurrY()) > mCloseEnough) {
                        // Let the user 'catch' the pager as it animates.
                        mScroller.abortAnimation();
                        mPopulatePending = false;
                        populate();
                        mIsBeingDragged = true;
                        requestParentDisallowInterceptTouchEvent(true);
                        setScrollState(SCROLL_STATE_DRAGGING);
                    } else {
                        completeScroll(false);
                        mIsBeingDragged = false;
                    }
                } else {
                    if (mScrollState == SCROLL_STATE_SETTLING
                            && Math.abs(mScroller.getFinalX() - mScroller.getCurrX()) > mCloseEnough) {
                        // Let the user 'catch' the pager as it animates.
                        mScroller.abortAnimation();
                        mPopulatePending = false;
                        populate();
                        mIsBeingDragged = true;
                        requestParentDisallowInterceptTouchEvent(true);
                        setScrollState(SCROLL_STATE_DRAGGING);
                    } else {
                        completeScroll(false);
                        mIsBeingDragged = false;
                    }
                }
                if (DEBUG) {
                    Log.v(TAG, "Down at " + mLastMotionX + "," + mLastMotionY
                            + " mIsBeingDragged=" + mIsBeingDragged
                            + "mIsUnableToDrag=" + mIsUnableToDrag);
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mFakeDragging) {
            // A fake drag is in progress already, ignore this real one
            // but still eat the touch events.
            // (It is likely that the user is multi-touching the screen.)
            return true;
        }

        if (ev.getAction() == MotionEvent.ACTION_DOWN && ev.getEdgeFlags() != 0) {
            // Don't handle edge touches immediately -- they may actually belong to one of our
            // descendants.
            return false;
        }

        if (mAdapter == null || mAdapter.getCount() == 0) {
            // Nothing to present or scroll; nothing to touch.
            return false;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        boolean needsInvalidate = false;

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                mScroller.abortAnimation();
                mPopulatePending = false;
                populate();

                // Remember where the motion event started
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;
            }
            case MotionEvent.ACTION_MOVE:
                //停止计时
                stopLoop();
                if (!mIsBeingDragged) {
                    final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                    if (pointerIndex == -1) {
                        // A child has consumed some touch events and put us into an inconsistent
                        // state.
                        needsInvalidate = resetTouch();
                        break;
                    }
                    final float x = ev.getX(pointerIndex);
                    final float xDiff = Math.abs(x - mLastMotionX);
                    final float y = ev.getY(pointerIndex);
                    final float yDiff = Math.abs(y - mLastMotionY);
                    if (DEBUG) {
                        Log.v(TAG, "Moved x to " + x + "," + y + " diff=" + xDiff + "," + yDiff);
                    }
                    if (mOrientation == Orientation.VERTICAL) {
                        if (yDiff > mTouchSlop && yDiff > xDiff) {
                            if (DEBUG) Log.v(TAG, "Starting drag!");
                            mIsBeingDragged = true;
                            requestParentDisallowInterceptTouchEvent(true);
                            mLastMotionY = y - mInitialMotionY > 0 ? mInitialMotionY + mTouchSlop :
                                    mInitialMotionY - mTouchSlop;
                            mLastMotionX = x;
                            setScrollState(SCROLL_STATE_DRAGGING);
                            setScrollingCacheEnabled(true);

                            // Disallow Parent Intercept, just in case
                            ViewParent parent = getParent();
                            if (parent != null) {
                                parent.requestDisallowInterceptTouchEvent(true);
                            }
                        }
                    } else {
                        if (xDiff > mTouchSlop && xDiff > yDiff) {
                            if (DEBUG) Log.v(TAG, "Starting drag!");
                            mIsBeingDragged = true;
                            requestParentDisallowInterceptTouchEvent(true);
                            mLastMotionX = x - mInitialMotionX > 0 ? mInitialMotionX + mTouchSlop :
                                    mInitialMotionX - mTouchSlop;
                            mLastMotionY = y;
                            setScrollState(SCROLL_STATE_DRAGGING);
                            setScrollingCacheEnabled(true);

                            // Disallow Parent Intercept, just in case
                            ViewParent parent = getParent();
                            if (parent != null) {
                                parent.requestDisallowInterceptTouchEvent(true);
                            }
                        }
                    }
                }
                // Not else! Note that mIsBeingDragged can be set above.
                if (mIsBeingDragged) {
                    // Scroll to follow the motion event
                    final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                    if (mOrientation == Orientation.VERTICAL) {
                        final float y = ev.getY(activePointerIndex);
                        needsInvalidate |= performDrag(y);
                    } else {
                        final float x = ev.getX(activePointerIndex);
                        needsInvalidate |= performDrag(x);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                //开始计时
                startLoop();
                if (mIsBeingDragged) {
                    int currentPage;
                    int initialVelocity;
                    int totalDelta;
                    float pageOffset;
                    if (mOrientation == Orientation.VERTICAL) {
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        initialVelocity = (int) VelocityTrackerCompat.getYVelocity(
                                velocityTracker, mActivePointerId);
                        mPopulatePending = true;
                        final int height = getClientSize();
                        final int scrollY = getScrollY();
                        final ItemInfo ii = infoForCurrentScrollPosition();
                        currentPage = ii.position;
                        pageOffset = (((float) scrollY / height) - ii.offset) / ii.sizeFactor;
                        final int activePointerIndex =
                                MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                        final float y = MotionEventCompat.getY(ev, activePointerIndex);
                        totalDelta = (int) (y - mInitialMotionY);
                    } else {
                        final VelocityTracker velocityTracker = mVelocityTracker;
                        velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                        initialVelocity = (int) VelocityTrackerCompat.getXVelocity(
                                velocityTracker, mActivePointerId);
                        mPopulatePending = true;
                        final int width = getClientSize();
                        final int scrollX = getScrollX();
                        final ItemInfo ii = infoForCurrentScrollPosition();
                        currentPage = ii.position;
                        pageOffset = (((float) scrollX / width) - ii.offset) / ii.sizeFactor;
                        final int activePointerIndex =
                                MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                        final float x = MotionEventCompat.getX(ev, activePointerIndex);
                        totalDelta = (int) (x - mInitialMotionX);
                    }
                    int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity,
                            totalDelta);
                    setCurrentItemInternal(nextPage, true, true, initialVelocity);

                    needsInvalidate = resetTouch();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsBeingDragged) {
                    scrollToItem(mCurItem, true, 0, false);
                    needsInvalidate = resetTouch();
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                int index = ev.getActionIndex();
                if (mOrientation == Orientation.VERTICAL) {
                    final float y = ev.getY(index);
                    mLastMotionY = y;
                } else {
                    final float x = ev.getX(index);
                    mLastMotionX = x;
                }
                mActivePointerId = ev.getPointerId(index);
                break;
            }
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                if (mOrientation == Orientation.VERTICAL) {
                    mLastMotionY = ev.getY(ev.findPointerIndex(mActivePointerId));
                } else {
                    mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId));
                }
                break;
        }
        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
        return true;
    }

    private boolean resetTouch() {
        boolean needsInvalidate;
        mActivePointerId = INVALID_POINTER;
        endDrag();
        mTopLeftEdge.onRelease();
        mRightBottomEdge.onRelease();
        needsInvalidate = mTopLeftEdge.isFinished() | mRightBottomEdge.isFinished();
        return needsInvalidate;
    }

    private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
        final ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    private boolean performDrag(float dimen) {
        boolean needsInvalidate = false;

        if (mOrientation == Orientation.VERTICAL) {
            final float deltaY = mLastMotionY - dimen;
            mLastMotionY = dimen;

            float oldScrollY = getScrollY();
            float scrollY = oldScrollY + deltaY;
            final int height = getClientSize();

            float topBound = height * mFirstOffset;
            float bottomBound = height * mLastOffset;
            boolean topAbsolute = true;
            boolean bottomAbsolute = true;

            final ItemInfo firstItem = mItems.get(0);
            final ItemInfo lastItem = mItems.get(mItems.size() - 1);
            if (firstItem.position != 0) {
                topAbsolute = false;
                topBound = firstItem.offset * height;
            }
            if (lastItem.position != mAdapter.getCount() - 1) {
                bottomAbsolute = false;
                bottomBound = lastItem.offset * height;
            }

            if (scrollY < topBound) {
                if (topAbsolute) {
                    float over = topBound - scrollY;
                    mTopLeftEdge.onPull(Math.abs(over) / height);
                    needsInvalidate = true;
                }
                scrollY = topBound;
            } else if (scrollY > bottomBound) {
                if (bottomAbsolute) {
                    float over = scrollY - bottomBound;
                    mRightBottomEdge.onPull(Math.abs(over) / height);
                    needsInvalidate = true;
                }
                scrollY = bottomBound;
            }
            // Don't lose the rounded component
            mLastMotionX += scrollY - (int) scrollY;
            scrollTo(getScrollX(), (int) scrollY);
            pageScrolled((int) scrollY);

        } else {
            final float deltaX = mLastMotionX - dimen;
            mLastMotionX = dimen;

            float oldScrollX = getScrollX();
            float scrollX = oldScrollX + deltaX;
            final int width = getClientSize();

            float leftBound = width * mFirstOffset;
            float rightBound = width * mLastOffset;
            boolean leftAbsolute = true;
            boolean rightAbsolute = true;

            final ItemInfo firstItem = mItems.get(0);
            final ItemInfo lastItem = mItems.get(mItems.size() - 1);
            if (firstItem.position != 0) {
                leftAbsolute = false;
                leftBound = firstItem.offset * width;
            }
            if (lastItem.position != mAdapter.getCount() - 1) {
                rightAbsolute = false;
                rightBound = lastItem.offset * width;
            }

            if (scrollX < leftBound) {
                if (leftAbsolute) {
                    float over = leftBound - scrollX;
                    mTopLeftEdge.onPull(Math.abs(over) / width);
                    needsInvalidate = true;
                }
                scrollX = leftBound;
            } else if (scrollX > rightBound) {
                if (rightAbsolute) {
                    float over = scrollX - rightBound;
                    mRightBottomEdge.onPull(Math.abs(over) / width);
                    needsInvalidate = true;
                }
                scrollX = rightBound;
            }
            // Don't lose the rounded component
            mLastMotionX += scrollX - (int) scrollX;
            scrollTo((int) scrollX, getScrollY());
            pageScrolled((int) scrollX);
        }
        return needsInvalidate;
    }

    /**
     * @return Info about the page at the current scroll position.
     * This can be synthetic for a missing middle page; the 'object' field can be null.
     */
    private ItemInfo infoForCurrentScrollPosition() {
        final int size = getClientSize();
        final float scrollOffset = size > 0 ? (float) ((mOrientation == Orientation.VERTICAL) ? getScrollY() : getScrollX()) / size : 0;
        final float marginOffset = size > 0 ? (float) mPageMargin / size : 0;
        int lastPos = -1;
        float lastOffset = 0.f;
        float lastSize = 0.f;
        boolean first = true;

        ItemInfo lastItem = null;
        for (int i = 0; i < mItems.size(); i++) {
            ItemInfo ii = mItems.get(i);
            float offset;
            if (!first && ii.position != lastPos + 1) {
                // Create a synthetic item for a missing page.
                ii = mTempItem;
                ii.offset = lastOffset + lastSize + marginOffset;
                ii.position = lastPos + 1;
                ii.sizeFactor = mAdapter.getPageWidth(ii.position);
                i--;
            }
            offset = ii.offset;

            final float topLeftBound = offset;
            final float bottomRightBound = offset + ii.sizeFactor + marginOffset;
            if (first || scrollOffset >= topLeftBound) {
                if (scrollOffset < bottomRightBound || i == mItems.size() - 1) {
                    return ii;
                }
            } else {
                return lastItem;
            }
            first = false;
            lastPos = ii.position;
            lastOffset = offset;
            lastSize = ii.sizeFactor;
            lastItem = ii;
        }

        return lastItem;
    }

    private int determineTargetPage(int currentPage, float pageOffset, int velocity, int deltaDimen) {
        int targetPage;
        if (Math.abs(deltaDimen) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
            targetPage = velocity > 0 ? currentPage : currentPage + 1;
        } else {
            final float truncator = currentPage >= mCurItem ? 0.4f : 0.6f;
            targetPage = currentPage + (int) (pageOffset + truncator);
        }

        if (mItems.size() > 0) {
            final ItemInfo firstItem = mItems.get(0);
            final ItemInfo lastItem = mItems.get(mItems.size() - 1);

            // Only let the user target pages we have items for
            targetPage = Math.max(firstItem.position, Math.min(targetPage, lastItem.position));
        }

        return targetPage;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        boolean needsInvalidate = false;

        final int overScrollMode = getOverScrollMode();
        if (overScrollMode == View.OVER_SCROLL_ALWAYS
                || (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS
                && mAdapter != null && mAdapter.getCount() > 1)) {
            if (mOrientation == Orientation.VERTICAL) {
                if (!mTopLeftEdge.isFinished()) {
                    final int restoreCount = canvas.save();
                    final int height = getHeight();
                    final int width = getWidth() - getPaddingLeft() - getPaddingRight();


                    canvas.translate(getPaddingLeft(), mFirstOffset * height);
                    mTopLeftEdge.setSize(width, height);
                    needsInvalidate |= mTopLeftEdge.draw(canvas);
                    canvas.restoreToCount(restoreCount);
                }
                if (!mRightBottomEdge.isFinished()) {
                    final int restoreCount = canvas.save();
                    final int height = getHeight();
                    final int width = getWidth() - getPaddingLeft() - getPaddingRight();

                    canvas.rotate(180);
                    canvas.translate(-width - getPaddingLeft(), -(mLastOffset + 1) * height);
                    mRightBottomEdge.setSize(width, height);
                    needsInvalidate |= mRightBottomEdge.draw(canvas);
                    canvas.restoreToCount(restoreCount);
                }
            } else {
                if (!mTopLeftEdge.isFinished()) {
                    final int restoreCount = canvas.save();
                    final int height = getHeight() - getPaddingTop() - getPaddingBottom();
                    final int width = getWidth();

                    canvas.rotate(270);
                    canvas.translate(-height + getPaddingTop(), mFirstOffset * width);
                    mTopLeftEdge.setSize(height, width);
                    needsInvalidate |= mTopLeftEdge.draw(canvas);
                    canvas.restoreToCount(restoreCount);
                }
                if (!mRightBottomEdge.isFinished()) {
                    final int restoreCount = canvas.save();
                    final int width = getWidth();
                    final int height = getHeight() - getPaddingTop() - getPaddingBottom();

                    canvas.rotate(90);
                    canvas.translate(-getPaddingTop(), -(mLastOffset + 1) * width);
                    mRightBottomEdge.setSize(height, width);
                    needsInvalidate |= mRightBottomEdge.draw(canvas);
                    canvas.restoreToCount(restoreCount);
                }
            }
        } else {
            mTopLeftEdge.finish();
            mRightBottomEdge.finish();
        }

        if (needsInvalidate) {
            // Keep animating
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the margin drawable between pages if needed.
        if (mPageMargin > 0 && mMarginDrawable != null && mItems.size() > 0 && mAdapter != null) {
            if (mOrientation == Orientation.VERTICAL) {
                final int scrollY = getScrollY();
                final int height = getHeight();

                final float marginOffset = (float) mPageMargin / height;
                int itemIndex = 0;
                ItemInfo ii = mItems.get(0);
                float offset = ii.offset;
                final int itemCount = mItems.size();
                final int firstPos = ii.position;
                final int lastPos = mItems.get(itemCount - 1).position;
                for (int pos = firstPos; pos < lastPos; pos++) {
                    while (pos > ii.position && itemIndex < itemCount) {
                        ii = mItems.get(++itemIndex);
                    }

                    float drawAt;
                    if (pos == ii.position) {
                        drawAt = (ii.offset + ii.sizeFactor) * height;
                        offset = ii.offset + ii.sizeFactor + marginOffset;
                    } else {
                        float heightFactor = mAdapter.getPageWidth(pos);
                        drawAt = (offset + heightFactor) * height;
                        offset += heightFactor + marginOffset;
                    }

                    if (drawAt + mPageMargin > scrollY) {
                        mMarginDrawable.setBounds(mTopLeftPageBounds, Math.round(drawAt),
                                mBottomRightPageBounds, Math.round(drawAt + mPageMargin));
                        mMarginDrawable.draw(canvas);
                    }

                    if (drawAt > scrollY + height) {
                        break; // No more visible, no sense in continuing
                    }
                }
            } else {
                final int scrollX = getScrollX();
                final int width = getWidth();

                final float marginOffset = (float) mPageMargin / width;
                int itemIndex = 0;
                ItemInfo ii = mItems.get(0);
                float offset = ii.offset;
                final int itemCount = mItems.size();
                final int firstPos = ii.position;
                final int lastPos = mItems.get(itemCount - 1).position;
                for (int pos = firstPos; pos < lastPos; pos++) {
                    while (pos > ii.position && itemIndex < itemCount) {
                        ii = mItems.get(++itemIndex);
                    }

                    float drawAt;
                    if (pos == ii.position) {
                        drawAt = (ii.offset + ii.sizeFactor) * width;
                        offset = ii.offset + ii.sizeFactor + marginOffset;
                    } else {
                        float widthFactor = mAdapter.getPageWidth(pos);
                        drawAt = (offset + widthFactor) * width;
                        offset += widthFactor + marginOffset;
                    }

                    if (drawAt + mPageMargin > scrollX) {
                        mMarginDrawable.setBounds(Math.round(drawAt), mTopLeftPageBounds,
                                Math.round(drawAt + mPageMargin), mBottomRightPageBounds);
                        mMarginDrawable.draw(canvas);
                    }

                    if (drawAt > scrollX + width) {
                        break; // No more visible, no sense in continuing
                    }
                }
            }
        }
    }

    /**
     * Start a fake drag of the pager.
     * <p>
     * <p>A fake drag can be useful if you want to synchronize the motion of the ViewPager
     * with the touch scrolling of another view, while still letting the ViewPager
     * control the snapping motion and fling behavior. (e.g. parallax-scrolling tabs.)
     * Call {@link #fakeDragBy(float)} to simulate the actual drag motion. Call
     * {@link #endFakeDrag()} to complete the fake drag and fling as necessary.
     * <p>
     * <p>During a fake drag the ViewPager will ignore all touch events. If a real drag
     * is already in progress, this method will return false.
     *
     * @return true if the fake drag began successfully, false if it could not be started.
     * @see #fakeDragBy(float)
     * @see #endFakeDrag()
     */
    public boolean beginFakeDrag() {
        if (mIsBeingDragged) {
            return false;
        }
        mFakeDragging = true;
        setScrollState(SCROLL_STATE_DRAGGING);
        if (mOrientation == Orientation.VERTICAL) {
            mInitialMotionY = mLastMotionY = 0;
        } else {
            mInitialMotionX = mLastMotionX = 0;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
        final long time = SystemClock.uptimeMillis();
        final MotionEvent ev = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 0, 0, 0);
        mVelocityTracker.addMovement(ev);
        ev.recycle();
        mFakeDragBeginTime = time;
        return true;
    }

    /**
     * End a fake drag of the pager.
     *
     * @see #beginFakeDrag()
     * @see #fakeDragBy(float)
     */
    public void endFakeDrag() {
        if (!mFakeDragging) {
            throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
        }

        if (mAdapter != null) {
            final VelocityTracker velocityTracker = mVelocityTracker;
            velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
            if (mOrientation == Orientation.VERTICAL) {
                int initialVelocity = (int) velocityTracker.getYVelocity(mActivePointerId);
                mPopulatePending = true;
                final int size = getClientSize();
                final int scrollY = getScrollY();
                final ItemInfo ii = infoForCurrentScrollPosition();
                final int currentPage = ii.position;
                final float pageOffset = (((float) scrollY / size) - ii.offset) / ii.sizeFactor;
                final int totalDelta = (int) (mLastMotionY - mInitialMotionY);
                int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity,
                        totalDelta);
                setCurrentItemInternal(nextPage, true, true, initialVelocity);
            } else {
                int initialVelocity = (int) velocityTracker.getXVelocity(mActivePointerId);
                mPopulatePending = true;
                final int size = getClientSize();
                final int scrollX = getScrollX();
                final ItemInfo ii = infoForCurrentScrollPosition();
                final int currentPage = ii.position;
                final float pageOffset = (((float) scrollX / size) - ii.offset) / ii.sizeFactor;
                final int totalDelta = (int) (mLastMotionX - mInitialMotionX);
                int nextPage = determineTargetPage(currentPage, pageOffset, initialVelocity,
                        totalDelta);
                setCurrentItemInternal(nextPage, true, true, initialVelocity);
            }
        }
        endDrag();

        mFakeDragging = false;
    }

    /**
     * Fake drag by an offset in pixels. You must have called {@link #beginFakeDrag()} first.
     *
     * @param offset Offset in pixels to drag by.
     * @see #beginFakeDrag()
     * @see #endFakeDrag()
     */
    public void fakeDragBy(float offset) {
        if (!mFakeDragging) {
            throw new IllegalStateException("No fake drag in progress. Call beginFakeDrag first.");
        }

        if (mAdapter == null) {
            return;
        }

        if (mOrientation == Orientation.VERTICAL) {
            mLastMotionY += offset;

            float oldScrollY = getScrollY();
            float scrollY = oldScrollY - offset;
            final int height = getClientSize();

            float topBound = height * mFirstOffset;
            float bottomBound = height * mLastOffset;

            final ItemInfo firstItem = mItems.get(0);
            final ItemInfo lastItem = mItems.get(mItems.size() - 1);
            if (firstItem.position != 0) {
                topBound = firstItem.offset * height;
            }
            if (lastItem.position != mAdapter.getCount() - 1) {
                bottomBound = lastItem.offset * height;
            }

            if (scrollY < topBound) {
                scrollY = topBound;
            } else if (scrollY > bottomBound) {
                scrollY = bottomBound;
            }
            // Don't lose the rounded component
            mLastMotionY += scrollY - (int) scrollY;
            scrollTo(getScrollX(), (int) scrollY);
            pageScrolled((int) scrollY);

            // Synthesize an event for the VelocityTracker.
            final long time = SystemClock.uptimeMillis();
            final MotionEvent ev = MotionEvent.obtain(mFakeDragBeginTime, time, MotionEvent.ACTION_MOVE,
                    0, mLastMotionY, 0);
            mVelocityTracker.addMovement(ev);
            ev.recycle();
        } else {
            mLastMotionX += offset;

            float oldScrollX = getScrollX();
            float scrollX = oldScrollX - offset;
            final int width = getClientSize();

            float leftBound = width * mFirstOffset;
            float rightBound = width * mLastOffset;

            final ItemInfo firstItem = mItems.get(0);
            final ItemInfo lastItem = mItems.get(mItems.size() - 1);
            if (firstItem.position != 0) {
                leftBound = firstItem.offset * width;
            }
            if (lastItem.position != mAdapter.getCount() - 1) {
                rightBound = lastItem.offset * width;
            }

            if (scrollX < leftBound) {
                scrollX = leftBound;
            } else if (scrollX > rightBound) {
                scrollX = rightBound;
            }
            // Don't lose the rounded component
            mLastMotionX += scrollX - (int) scrollX;
            scrollTo((int) scrollX, getScrollY());
            pageScrolled((int) scrollX);

            // Synthesize an event for the VelocityTracker.
            final long time = SystemClock.uptimeMillis();
            final MotionEvent ev = MotionEvent.obtain(mFakeDragBeginTime, time, MotionEvent.ACTION_MOVE,
                    mLastMotionX, 0, 0);
            mVelocityTracker.addMovement(ev);
            ev.recycle();
        }
    }

    /**
     * Returns true if a fake drag is in progress.
     *
     * @return true if currently in a fake drag, false otherwise.
     * @see #beginFakeDrag()
     * @see #fakeDragBy(float)
     * @see #endFakeDrag()
     */
    public boolean isFakeDragging() {
        return mFakeDragging;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            if (mOrientation == Orientation.VERTICAL) {
                mLastMotionY = ev.getY(newPointerIndex);
            } else {
                mLastMotionX = ev.getX(newPointerIndex);
            }
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    private void endDrag() {
        mIsBeingDragged = false;
        mIsUnableToDrag = false;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
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
     * Check if this ViewPager can be scrolled horizontally in a certain direction.
     *
     * @param direction Negative to check scrolling left, positive to check scrolling right.
     * @return Whether this ViewPager can be scrolled in the specified direction. It will always
     * return false if the specified direction is 0.
     */
    public boolean canScrollHorizontally(int direction) {
        if (mAdapter == null) {
            return false;
        }

        final int size = getClientSize();
        final int scroll = (mOrientation == Orientation.VERTICAL) ? getScrollY() : getScrollX();
        if (direction < 0) {
            return (scroll > (int) (size * mFirstOffset));
        } else if (direction > 0) {
            return (scroll < (int) (size * mLastOffset));
        } else {
            return false;
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v      View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param delta  Delta scrolled in pixels
     * @param x      X coordinate of the active touch point
     * @param y      Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScroll(View v, boolean checkV, int delta, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);
                if (mOrientation == Orientation.VERTICAL) {
                    if (y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
                            && x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
                            && canScroll(child, true, delta, x + scrollX - child.getLeft(),
                            y + scrollY - child.getTop())) {
                        return true;
                    }
                } else {
                    if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
                            && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
                            && canScroll(child, true, delta, x + scrollX - child.getLeft(),
                            y + scrollY - child.getTop())) {
                        return true;
                    }
                }
            }
        }
        if (mOrientation == Orientation.VERTICAL) {
            return checkV && v.canScrollVertically(-delta);
        } else {
            return checkV && v.canScrollHorizontally(-delta);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Let the focused view and/or our descendants get the key first
        return super.dispatchKeyEvent(event) || executeKeyEvent(event);
    }

    /**
     * You can call this function yourself to have the scroll view perform
     * scrolling from a key event, just as if the event had been dispatched to
     * it by the view hierarchy.
     *
     * @param event The key event to execute.
     * @return Return true if the event was handled, else false.
     */
    public boolean executeKeyEvent(KeyEvent event) {
        boolean handled = false;
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    handled = arrowScroll(FOCUS_LEFT);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    handled = arrowScroll(FOCUS_RIGHT);
                    break;
                case KeyEvent.KEYCODE_TAB:
                    if (event.hasNoModifiers()) {
                        handled = arrowScroll(FOCUS_FORWARD);
                    } else if (event.hasModifiers(KeyEvent.META_SHIFT_ON)) {
                        handled = arrowScroll(FOCUS_BACKWARD);
                    }
                    break;
            }
        }
        return handled;
    }

    /**
     * Handle scrolling in response to a left or right arrow click.
     *
     * @param direction The direction corresponding to the arrow key that was pressed. It should be
     *                  either {@link View#FOCUS_LEFT} or {@link View#FOCUS_RIGHT}.
     * @return Whether the scrolling was handled successfully.
     */
    public boolean arrowScroll(int direction) {
        View currentFocused = findFocus();
        if (currentFocused == this) {
            currentFocused = null;
        } else if (currentFocused != null) {
            boolean isChild = false;
            for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup;
                 parent = parent.getParent()) {
                if (parent == this) {
                    isChild = true;
                    break;
                }
            }
            if (!isChild) {
                // This would cause the focus search down below to fail in fun ways.
                final StringBuilder sb = new StringBuilder();
                sb.append(currentFocused.getClass().getSimpleName());
                for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup;
                     parent = parent.getParent()) {
                    sb.append(" => ").append(parent.getClass().getSimpleName());
                }
                Log.e(TAG, "arrowScroll tried to find focus based on non-child "
                        + "current focused view " + sb.toString());
                currentFocused = null;
            }
        }

        boolean handled = false;

        View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused,
                direction);
        if (mOrientation == Orientation.VERTICAL) {
            if (nextFocused != null && nextFocused != currentFocused) {
                if (direction == View.FOCUS_UP) {
                    // If there is nothing to the left, or this is causing us to
                    // jump to the right, then what we really want to do is page left.
                    final int nextTop = getChildRectInPagerCoordinates(mTempRect, nextFocused).top;
                    final int currTop = getChildRectInPagerCoordinates(mTempRect, currentFocused).top;
                    if (currentFocused != null && nextTop >= currTop) {
                        handled = pageLeftUp();
                    } else {
                        handled = nextFocused.requestFocus();
                    }
                } else if (direction == View.FOCUS_DOWN) {
                    // If there is nothing to the right, or this is causing us to
                    // jump to the left, then what we really want to do is page right.
                    final int nextDown = getChildRectInPagerCoordinates(mTempRect, nextFocused).bottom;
                    final int currDown = getChildRectInPagerCoordinates(mTempRect, currentFocused).bottom;
                    if (currentFocused != null && nextDown <= currDown) {
                        handled = pageRightDown();
                    } else {
                        handled = nextFocused.requestFocus();
                    }
                }
            } else if (direction == FOCUS_UP || direction == FOCUS_BACKWARD) {
                // Trying to move left and nothing there; try to page.
                handled = pageLeftUp();
            } else if (direction == FOCUS_DOWN || direction == FOCUS_FORWARD) {
                // Trying to move right and nothing there; try to page.
                handled = pageRightDown();
            }
        } else {
            if (nextFocused != null && nextFocused != currentFocused) {
                if (direction == View.FOCUS_LEFT) {
                    // If there is nothing to the left, or this is causing us to
                    // jump to the right, then what we really want to do is page left.
                    final int nextLeft = getChildRectInPagerCoordinates(mTempRect, nextFocused).left;
                    final int currLeft = getChildRectInPagerCoordinates(mTempRect, currentFocused).left;
                    if (currentFocused != null && nextLeft >= currLeft) {
                        handled = pageLeftUp();
                    } else {
                        handled = nextFocused.requestFocus();
                    }
                } else if (direction == View.FOCUS_RIGHT) {
                    // If there is nothing to the right, or this is causing us to
                    // jump to the left, then what we really want to do is page right.
                    final int nextLeft = getChildRectInPagerCoordinates(mTempRect, nextFocused).left;
                    final int currLeft = getChildRectInPagerCoordinates(mTempRect, currentFocused).left;
                    if (currentFocused != null && nextLeft <= currLeft) {
                        handled = pageRightDown();
                    } else {
                        handled = nextFocused.requestFocus();
                    }
                }
            } else if (direction == FOCUS_LEFT || direction == FOCUS_BACKWARD) {
                // Trying to move left and nothing there; try to page.
                handled = pageLeftUp();
            } else if (direction == FOCUS_RIGHT || direction == FOCUS_FORWARD) {
                // Trying to move right and nothing there; try to page.
                handled = pageRightDown();
            }
        }
        if (handled) {
            playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
        }
        return handled;
    }

    private Rect getChildRectInPagerCoordinates(Rect outRect, View child) {
        if (outRect == null) {
            outRect = new Rect();
        }
        if (child == null) {
            outRect.set(0, 0, 0, 0);
            return outRect;
        }
        outRect.left = child.getLeft();
        outRect.right = child.getRight();
        outRect.top = child.getTop();
        outRect.bottom = child.getBottom();

        ViewParent parent = child.getParent();
        while (parent instanceof ViewGroup && parent != this) {
            final ViewGroup group = (ViewGroup) parent;
            outRect.left += group.getLeft();
            outRect.right += group.getRight();
            outRect.top += group.getTop();
            outRect.bottom += group.getBottom();

            parent = group.getParent();
        }
        return outRect;
    }

    boolean pageLeftUp() {
        if (mCurItem > 0) {
            setCurrentItem(mCurItem - 1, true);
            return true;
        }
        return false;
    }

    boolean pageRightDown() {
        if (mAdapter != null && mCurItem < (mAdapter.getCount() - 1)) {
            setCurrentItem(mCurItem + 1, true);
            return true;
        }
        return false;
    }

    /**
     * We only want the current page that is being shown to be focusable.
     */
    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        final int focusableCount = views.size();

        final int descendantFocusability = getDescendantFocusability();

        if (descendantFocusability != FOCUS_BLOCK_DESCENDANTS) {
            for (int i = 0; i < getChildCount(); i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == VISIBLE) {
                    ItemInfo ii = infoForChild(child);
                    if (ii != null && ii.position == mCurItem) {
                        child.addFocusables(views, direction, focusableMode);
                    }
                }
            }
        }

        // we add ourselves (if focusable) in all cases except for when we are
        // FOCUS_AFTER_DESCENDANTS and there are some descendants focusable.  this is
        // to avoid the focus search finding layouts when a more precise search
        // among the focusable children would be more interesting.
        if (descendantFocusability != FOCUS_AFTER_DESCENDANTS
                || (focusableCount == views.size())) { // No focusable descendants
            // Note that we can't call the superclass here, because it will
            // add all views in.  So we need to do the same thing View does.
            if (!isFocusable()) {
                return;
            }
            if ((focusableMode & FOCUSABLES_TOUCH_MODE) == FOCUSABLES_TOUCH_MODE
                    && isInTouchMode() && !isFocusableInTouchMode()) {
                return;
            }
            if (views != null) {
                views.add(this);
            }
        }
    }

    /**
     * We only want the current page that is being shown to be touchable.
     */
    @Override
    public void addTouchables(ArrayList<View> views) {
        // Note that we don't call super.addTouchables(), which means that
        // we don't call View.addTouchables().  This is okay because a ViewPager
        // is itself not touchable.
        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == mCurItem) {
                    child.addTouchables(views);
                }
            }
        }
    }

    /**
     * We only want the current page that is being shown to be focusable.
     */
    @Override
    protected boolean onRequestFocusInDescendants(int direction,
                                                  Rect previouslyFocusedRect) {
        int index;
        int increment;
        int end;
        int count = getChildCount();
        if ((direction & FOCUS_FORWARD) != 0) {
            index = 0;
            increment = 1;
            end = count;
        } else {
            index = count - 1;
            increment = -1;
            end = -1;
        }
        for (int i = index; i != end; i += increment) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == mCurItem) {
                    if (child.requestFocus(direction, previouslyFocusedRect)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Dispatch scroll events from this ViewPager.
        if (event.getEventType() == AccessibilityEventCompat.TYPE_VIEW_SCROLLED) {
            return super.dispatchPopulateAccessibilityEvent(event);
        }

        // Dispatch all other accessibility events from the current page.
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                final ItemInfo ii = infoForChild(child);
                if (ii != null && ii.position == mCurItem
                        && child.dispatchPopulateAccessibilityEvent(event)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return generateDefaultLayoutParams();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /**
     * 如果是循环的会自动跳转到对应的位置，而且要放在监听方法的最上面调用
     * SimpleOnPageChangeListener或者OnPageChangeListener都可以
     *
     * @param position
     * @param positionOffset
     * @param positionOffsetPixels
     */
    public void setPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (position == 0 && positionOffsetPixels == 0) {//首位
            setCurrentItem((mAdapter.getCount() - 1) / 2, false);
        } else if (position == (mAdapter.getCount() + 1) / 2) {
            setCurrentItem(1, false);
        } else if (position > (mAdapter.getCount() + 1) / 2) {
            setCurrentItem(1, false);
        }
    }

    public enum Orientation {
        VERTICAL, HORIZONTAL
    }

    /**
     * Callback interface for responding to adapter changes.
     */
    public interface OnAdapterChangeListener {
        /**
         * Called when the adapter for the given view pager has changed.
         *
         * @param viewPager  ViewPager where the adapter change has happened
         * @param oldAdapter the previously set adapter
         * @param newAdapter the newly set adapter
         */
        void onAdapterChanged(@NonNull OrientedAutoViewPager viewPager,
                              @Nullable PagerAdapter oldAdapter, @Nullable PagerAdapter newAdapter);
    }

    /**
     * Annotation which allows marking of views to be decoration views when added to a view
     * pager.
     * <p>
     * <p>Views marked with this annotation can be added to the view pager with a layout resource.
     * An example being {@link android.support.v4.view.PagerTitleStrip}.</p>
     * <p>
     * <p>You can also control whether a view is a decor view but setting
     * {@link LayoutParams#isDecor} on the child's layout params.</p>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    public @interface DecorView {
    }

    static class ItemInfo {
        Object object;
        int position;
        boolean scrolling;
        float sizeFactor;
        float offset;
    }

    /**
     * Simple implementation of the {@link ViewPager.OnPageChangeListener} interface with stub
     * implementations of each method. Extend this if you do not intend to override
     * every method of {@link ViewPager.OnPageChangeListener}.
     */
    public static class SimpleOnPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // This space for rent,此处添加判断
        }

        @Override
        public void onPageSelected(int position) {
            // This space for rent
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // This space for rent
        }
    }

    /**
     * This is the persistent state that is saved by ViewPager.  Only needed
     * if you are creating a sublass of ViewPager that must save its own
     * state, in which case it should implement a subclass of this which
     * contains that state.
     */
    public static class SavedState extends AbsSavedState {
        public static final Creator<SavedState> CREATOR = ParcelableCompat.newCreator(
                new ParcelableCompatCreatorCallbacks<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                        return new SavedState(in, loader);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                });
        int position;
        Parcelable adapterState;
        ClassLoader loader;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressLint("NewApi")
        SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            position = in.readInt();
            adapterState = in.readParcelable(loader);
            this.loader = loader;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(position);
            out.writeParcelable(adapterState, flags);
        }

        @Override
        public String toString() {
            return "FragmentPager.SavedState{"
                    + Integer.toHexString(System.identityHashCode(this))
                    + " position=" + position + "}";
        }
    }

    /**
     * Layout parameters that should be supplied for views added to a
     * ViewPager.
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {
        /**
         * true if this view is a decoration on the pager itself and not
         * a view supplied by the adapter.
         */
        public boolean isDecor;

        /**
         * Gravity setting for use on decor views only:
         * Where to position the view page within the overall ViewPager
         * container; constants are defined in {@link Gravity}.
         */
        public int gravity;

        /**
         * Width as a 0-1 multiplier of the measured pager width
         */
        float widthFactor = 0.f;
        /**
         * Width as a 0-1 multiplier of the measured pager height
         */
        float heightFactor = 0.f;

        /**
         * true if this view was added during layout and needs to be measured
         * before being positioned.
         */
        boolean needsMeasure;

        /**
         * Adapter position this view is for if !isDecor
         */
        int position;

        /**
         * Current child index within the ViewPager that this view occupies
         */
        int childIndex;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);

            final TypedArray a = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
            gravity = a.getInteger(0, Gravity.TOP);
            a.recycle();
        }
    }

    static class ViewPositionComparator implements Comparator<View> {
        @Override
        public int compare(View lhs, View rhs) {
            final LayoutParams llp = (LayoutParams) lhs.getLayoutParams();
            final LayoutParams rlp = (LayoutParams) rhs.getLayoutParams();
            if (llp.isDecor != rlp.isDecor) {
                return llp.isDecor ? 1 : -1;
            }
            return llp.position - rlp.position;
        }
    }

    class MyAccessibilityDelegate extends AccessibilityDelegateCompat {

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);
            event.setClassName(ViewPager.class.getName());
            final AccessibilityRecordCompat recordCompat =
                    AccessibilityEventCompat.asRecord(event);
            recordCompat.setScrollable(canScroll());
            if (event.getEventType() == AccessibilityEventCompat.TYPE_VIEW_SCROLLED
                    && mAdapter != null) {
                recordCompat.setItemCount(mAdapter.getCount());
                recordCompat.setFromIndex(mCurItem);
                recordCompat.setToIndex(mCurItem);
            }
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setClassName(ViewPager.class.getName());
            info.setScrollable(canScroll());
            if (canScrollHorizontally(1) || canScrollVertically(1)) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
            }
            if (canScrollHorizontally(-1) || canScrollVertically(-1)) {
                info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
            }
        }

        @Override
        public boolean performAccessibilityAction(View host, int action, Bundle args) {
            if (super.performAccessibilityAction(host, action, args)) {
                return true;
            }
            switch (action) {
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD: {
                    if (canScrollHorizontally(1) || canScrollVertically(1)) {
                        setCurrentItem(mCurItem + 1);
                        return true;
                    }
                }
                return false;
                case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD: {
                    if (canScrollHorizontally(-1) || canScrollVertically(-1)) {
                        setCurrentItem(mCurItem - 1);
                        return true;
                    }
                }
                return false;
            }
            return false;
        }

        private boolean canScroll() {
            return (mAdapter != null) && (mAdapter.getCount() > 1);
        }
    }

    private class PagerObserver extends DataSetObserver {
        PagerObserver() {
        }

        @Override
        public void onChanged() {
            dataSetChanged();
        }

        @Override
        public void onInvalidated() {
            dataSetChanged();
        }
    }
}
