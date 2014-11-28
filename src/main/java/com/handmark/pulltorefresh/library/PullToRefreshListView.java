/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.handmark.pulltorefresh.library;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.handmark.pulltorefresh.library.internal.EmptyViewMethodAccessor;
import com.handmark.pulltorefresh.library.internal.LoadingLayout;

public class PullToRefreshListView extends PullToRefreshAdapterViewBase<ListView> {

	private LoadingLayout mHeaderLoadingView;
	private LoadingLayout mFooterLoadingView;

	private FrameLayout mLvFooterLoadingFrame;

	private boolean mListViewExtrasEnabled;

	public PullToRefreshListView(Context context) {
		super(context);
	}

	public PullToRefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PullToRefreshListView(Context context, Mode mode) {
		super(context, mode);
	}

	public PullToRefreshListView(Context context, Mode mode, AnimationStyle style) {
		super(context, mode, style);
	}

	@Override
	public final Orientation getPullToRefreshScrollDirection() {
		return Orientation.VERTICAL;
	}

	@Override
	protected void onRefreshing(final boolean doScroll,
                                final boolean smoothScroll) {
		/**
		 * If we're not showing the Refreshing view, or the list is empty, the
		 * the header/footer views won't show so we use the normal method.
		 */
		ListAdapter adapter = mRefreshableView.getAdapter();
		if (!mListViewExtrasEnabled || !getShowViewWhileRefreshing() || null == adapter || adapter.isEmpty()) {
			super.onRefreshing(doScroll,
                               smoothScroll);
			return;
		}

		super.onRefreshing(false,
                           false);

		final LoadingLayout origLoadingView, listViewLoadingView, oppositeListViewLoadingView;
		final int selection, scrollToY;

		switch (getCurrentMode()) {
			case MANUAL_REFRESH_ONLY:
			case PULL_FROM_END:
				origLoadingView = getFooterLayout();
				listViewLoadingView = mFooterLoadingView;
				oppositeListViewLoadingView = mHeaderLoadingView;
				selection = mRefreshableView.getCount() - 1;
				scrollToY = getScrollY() - getFooterSize();
				break;
			case PULL_FROM_START:
			default:
				origLoadingView = getHeaderLayout();
				listViewLoadingView = mHeaderLoadingView;
				oppositeListViewLoadingView = mFooterLoadingView;
				selection = 0;
				scrollToY = getScrollY() + getHeaderSize();
				break;
		}

		// Hide our original Loading View
		origLoadingView.reset();
		origLoadingView.hideAllViews();

		// Make sure the opposite end is hidden too
		oppositeListViewLoadingView.setVisibility(View.GONE);

		// Show the ListView Loading View and set it to refresh.
		listViewLoadingView.setVisibility(View.VISIBLE);
		listViewLoadingView.refreshing();

		if (doScroll) {
			// We need to disable the automatic visibility changes for now
			disableLoadingLayoutVisibilityChanges();

			// We scroll slightly so that the ListView's header/footer is at the
			// same Y position as our normal header/footer
			setHeaderScroll(scrollToY, 0);

			// Make sure the ListView is scrolled to show the loading
			// header/footer
			mRefreshableView.setSelection(selection);

			// Smooth scroll as normal
            if (smoothScroll) {
                smoothScrollTo(0);
            } else {
                scrollTo(0, 0);
            }
		}
	}

	@Override
	protected void onReset() {
		/**
		 * If the extras are not enabled, just call up to super and return.
		 */
		if (!mListViewExtrasEnabled) {
			super.onReset();
			return;
		}

		final LoadingLayout originalLoadingLayout, listViewLoadingLayout;
		final int scrollToHeight, selection;
		final boolean scrollLvToEdge;

		switch (getCurrentMode()) {
			case MANUAL_REFRESH_ONLY:
			case PULL_FROM_END:
				originalLoadingLayout = getFooterLayout();
				listViewLoadingLayout = mFooterLoadingView;
				selection = mRefreshableView.getCount() - 1;
				scrollToHeight = getFooterSize();
				scrollLvToEdge = Math.abs(mRefreshableView.getLastVisiblePosition() - selection) <= 1;
				break;
			case PULL_FROM_START:
			default:
				originalLoadingLayout = getHeaderLayout();
				listViewLoadingLayout = mHeaderLoadingView;
				scrollToHeight = -getHeaderSize();
				selection = 0;
				scrollLvToEdge = Math.abs(mRefreshableView.getFirstVisiblePosition() - selection) <= 1;
				break;
		}

		// If the ListView header loading layout is showing, then we need to
		// flip so that the original one is showing instead
		if (listViewLoadingLayout.getVisibility() == View.VISIBLE) {

			// Set our Original View to Visible
			originalLoadingLayout.showInvisibleViews();

			// Hide the ListView Header/Footer
			listViewLoadingLayout.setVisibility(View.GONE);

			/**
			 * Scroll so the View is at the same Y as the ListView
			 * header/footer, but only scroll if: we've pulled to refresh, it's
			 * positioned correctly
			 */
			if (scrollLvToEdge && getState() != State.MANUAL_REFRESHING) {
				mRefreshableView.setSelection(selection);
				setHeaderScroll(scrollToHeight, 0);
			}
		}

		// Finally, call up to super
		super.onReset();
	}

	@Override
	protected LoadingLayoutProxy createLoadingLayoutProxy(final boolean includeStart, final boolean includeEnd) {
		LoadingLayoutProxy proxy = super.createLoadingLayoutProxy(includeStart, includeEnd);

		if (mListViewExtrasEnabled) {
			final Mode mode = getMode();

			if (includeStart && mode.showHeaderLoadingLayout()) {
				proxy.addLayout(mHeaderLoadingView);
			}
			if (includeEnd && mode.showFooterLoadingLayout()) {
				proxy.addLayout(mFooterLoadingView);
			}
		}

		return proxy;
	}

	protected ListView createListView(Context context, AttributeSet attrs) {
		final ListView lv;
		if (VERSION.SDK_INT >= VERSION_CODES.GINGERBREAD) {
			lv = new InternalListViewSDK9(context, attrs);
		} else {
			lv = new InternalListView(context, attrs);
		}
		return lv;
	}

	@Override
	protected ListView createRefreshableView(Context context, AttributeSet attrs) {
		ListView lv = createListView(context, attrs);

		// Set it to this so it can be used in ListActivity/ListFragment
		lv.setId(android.R.id.list);
		return lv;
	}

	@Override
	protected void handleStyledAttributes(TypedArray a) {
		super.handleStyledAttributes(a);

		mListViewExtrasEnabled = a.getBoolean(R.styleable.PullToRefresh_ptrListViewExtrasEnabled, true);

		if (mListViewExtrasEnabled) {
			final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
					FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL);

			// Create Loading Views ready for use later
			FrameLayout frame = new FrameLayout(getContext());
			mHeaderLoadingView = createLoadingLayout(getContext(), Mode.PULL_FROM_START, a);
			mHeaderLoadingView.setVisibility(View.GONE);
			frame.addView(mHeaderLoadingView, lp);
			mRefreshableView.addHeaderView(frame, null, false);

			mLvFooterLoadingFrame = new FrameLayout(getContext());
			mFooterLoadingView = createLoadingLayout(getContext(), Mode.PULL_FROM_END, a);
			mFooterLoadingView.setVisibility(View.GONE);
			mLvFooterLoadingFrame.addView(mFooterLoadingView, lp);

			/**
			 * If the value for Scrolling While Refreshing hasn't been
			 * explicitly set via XML, enable Scrolling While Refreshing.
			 */
			if (!a.hasValue(R.styleable.PullToRefresh_ptrScrollingWhileRefreshingEnabled)) {
				setScrollingWhileRefreshingEnabled(true);
			}
		}
	}

	@TargetApi(9)
	final class InternalListViewSDK9 extends InternalListView {

		public InternalListViewSDK9(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		protected boolean overScrollBy(int deltaX, int deltaY, int scrollX, int scrollY, int scrollRangeX,
				int scrollRangeY, int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {

			final boolean returnValue = super.overScrollBy(deltaX, deltaY, scrollX, scrollY, scrollRangeX,
					scrollRangeY, maxOverScrollX, maxOverScrollY, isTouchEvent);

			// Does all of the hard work...
			OverscrollHelper.overScrollBy(PullToRefreshListView.this, deltaX, scrollX, deltaY, scrollY, isTouchEvent);

			return returnValue;
		}
	}

	protected class InternalListView extends ListView implements EmptyViewMethodAccessor {

		private boolean mAddedLvFooter = false;

		public InternalListView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		protected void dispatchDraw(Canvas canvas) {
			/**
			 * This is a bit hacky, but Samsung's ListView has got a bug in it
			 * when using Header/Footer Views and the list is empty. This masks
			 * the issue so that it doesn't cause an FC. See Issue #66.
			 */
			try {
				super.dispatchDraw(canvas);
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev) {
			/**
			 * This is a bit hacky, but Samsung's ListView has got a bug in it
			 * when using Header/Footer Views and the list is empty. This masks
			 * the issue so that it doesn't cause an FC. See Issue #66.
			 */
			try {
				return super.dispatchTouchEvent(ev);
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
				return false;
			}
		}

		@Override
		public void setAdapter(ListAdapter adapter) {
			//Log.v("TAG", "SET MAH ADAPTER!");
			// Add the Footer View at the last possible moment
			if (null != mLvFooterLoadingFrame && !mAddedLvFooter) {
				addFooterView(mLvFooterLoadingFrame, null, false);
				mAddedLvFooter = true;
			}

			setDataSetObservor(adapter);
			
			super.setAdapter(adapter);
		}

		@Override
		public void setEmptyView(View emptyView) {
			PullToRefreshListView.this.setEmptyView(emptyView);
		}

		@Override
		public void setEmptyViewInternal(View emptyView) {
			super.setEmptyView(emptyView);
		}

	}
	
	// New code from AgileMD:
	// We need to toggle the "fake" header view (getHeaderLayout())
	// and the true ListView header view (mHeaderLoadingView) visibility
	// and scroll if suddenly our adapter becomes empty or non-empty.
	private void setDataSetObservor(ListAdapter adapter) {
		if (DEBUG) Log.v(LOG_TAG, "setDataSetObservor");
		if (myDataSetObservor == null) {
			myDataSetObservor = new MyDataSetObserver(adapter);
			adapter.registerDataSetObserver(myDataSetObservor);
		} else {
            myDataSetObservor.getAdapter().unregisterDataSetObserver(myDataSetObservor);
			myDataSetObservor.setAdapter(adapter);
			adapter.registerDataSetObserver(myDataSetObservor);
		}
	}
	
	// myDataSetObservor is a member variable for the ListView. Inside, we keep track of wasEmpty,
	// across any arbitrary change in adapters or data sets. We simple want to know if
	// it was empty before or not so I can perform the necessary toggling of our real/fake
	// headers if the empty or not status changes. This presumably could only even change
	// if you call notifyDataSetChanged (calling onChanged in my DataSetObserver), or by
	// calling setAdapter.
	private MyDataSetObserver myDataSetObservor = null;
	private class MyDataSetObserver extends DataSetObserver {
		
		private ListAdapter adapter = null;
		
		public MyDataSetObserver(ListAdapter adapter) {
			this.adapter = adapter;
		}
	
		public ListAdapter getAdapter() {
			return adapter;
		}

		public void setAdapter(ListAdapter adapter) {
			this.adapter = adapter;
			checkEmptyChangeAndToggleIfNecessary();
		}

		@Override
		public void onChanged() {
			checkEmptyChangeAndToggleIfNecessary();
		}
		
		private void checkEmptyChangeAndToggleIfNecessary() {
            // This is a hack to make sure that if the progress bar happens to be animating
            // (in the middle of a smoothScrollTo) when data set goes from empty to non-empty,
            // we don't show the non-empty progress bar and attempt to scroll us where we should
            // be, only to be overridden by the smoothScrollTo currently running. That would
            // lead to a white space above the non-empty ListView progress bar because it was
            // scrolling us to show that the empty ListView progress bar but we hid it, and
            // tried to scroll it where it should be, but like I said since it was in the middle
            // of smoothScrollTo it got overridden. The reason I'm doing a hack is to CORRECTLY
            // fix this would be too much work... you would have to order ALL operations, so
            // like a Handler or something. His code is not thread-safe, and issues like this
            // are probably still in the code somewhere which is perhaps one reason he abanonded
            // this project.
            postDelayed(new Runnable() {
                public void run() {
                    switch (mState) {
                        case RESET:
                            if (DEBUG) Log.v(LOG_TAG, "RESET");
                            onReset();
                            break;
                        case REFRESHING:
                        case MANUAL_REFRESHING:
                            if (DEBUG) Log.v(LOG_TAG, "REFRESHING");
                            
                            boolean isEmptyHeaderVisible = (getHeaderLayout().getVisibility() == View.VISIBLE);
                            boolean isNonEmptyHeaderVisible = (mHeaderLoadingView.getVisibility() == View.VISIBLE);
                            boolean isAdapterEmpty = adapter.isEmpty();

                            if (DEBUG) Log.v(LOG_TAG, "isEmptyHeaderVisible = " + isEmptyHeaderVisible + ", isNonEmptyHeaderVisible = " + isNonEmptyHeaderVisible + ", isAdapterEmpty = " + isAdapterEmpty);
                            if (isEmptyHeaderVisible &&
                                !isNonEmptyHeaderVisible &&
                                !isAdapterEmpty) {
                                if (DEBUG) Log.v(LOG_TAG, "toggle it from empty to non-empty!");
                                toggleLoadingLayoutsForEmptyChange(isAdapterEmpty);
                            } else if (isNonEmptyHeaderVisible &&
                                       !isEmptyHeaderVisible &&
                                       isAdapterEmpty) {
                                if (DEBUG) Log.v(LOG_TAG, "toggle it from non-empty to empty!");
                                toggleLoadingLayoutsForEmptyChange(isAdapterEmpty);
                            } else {
                                if (DEBUG) Log.v(LOG_TAG, "no toggling to do!");
                            }
                            
                            break;

                    }
                }
            }, 500);
		}
		
		private void toggleLoadingLayoutsForEmptyChange(boolean isEmpty) {
			if (DEBUG) Log.v(LOG_TAG, "toggleLoadingLayoutsForEmptyChange isEmpty = " + isEmpty);
			final LoadingLayout origLoadingView, listViewLoadingView, oppositeListViewLoadingView;
			final int selection, scrollToY;
			final int origLoadingViewVisibility;

			switch (getCurrentMode()) {
				case MANUAL_REFRESH_ONLY:
				case PULL_FROM_END: {
					if (DEBUG) Log.v(LOG_TAG, "PULL_FROM_END");
					origLoadingView = getFooterLayout();
					origLoadingViewVisibility = View.INVISIBLE;
					listViewLoadingView = mFooterLoadingView;
					oppositeListViewLoadingView = mHeaderLoadingView;
					selection = mRefreshableView.getCount() - 1;
					scrollToY = getScrollY() - getFooterSize();
					break;
				}
				case PULL_FROM_START:
				default: {
					if (isEmpty) {
						if (DEBUG) Log.v(LOG_TAG, "isEmpty!");
						origLoadingView = mHeaderLoadingView;
						origLoadingViewVisibility = View.GONE;
						listViewLoadingView = getHeaderLayout();
						oppositeListViewLoadingView = mFooterLoadingView;
						selection = 0;
						scrollToY = -getHeaderSize();
					} else {
						if (DEBUG) Log.v(LOG_TAG, "not isEmpty!");
						origLoadingView = getHeaderLayout();
						origLoadingViewVisibility = View.INVISIBLE;
						listViewLoadingView = mHeaderLoadingView;
						oppositeListViewLoadingView = mFooterLoadingView;
						selection = 0;
						scrollToY = getScrollY() + getHeaderSize();
					}
					break;
				}
			}
			
			// Hide our original Loading View
			origLoadingView.setVisibility(origLoadingViewVisibility);
			origLoadingView.reset();
			origLoadingView.hideAllViews();

			// Make sure the opposite end is hidden too
			oppositeListViewLoadingView.setVisibility(View.GONE);

			// Show the ListView Loading View and set it to refresh.
			listViewLoadingView.setVisibility(View.VISIBLE);
			listViewLoadingView.refreshing();
			listViewLoadingView.showInvisibleViews();

			// We need to disable the automatic visibility changes for now
			disableLoadingLayoutVisibilityChanges();

			// We scroll slightly so that the ListView's header/footer is at the
			// same Y position as our normal header/footer
			setHeaderScroll(scrollToY, 0);
			if (DEBUG) Log.v(LOG_TAG, "scrollToY = " + scrollToY);

			// Make sure the ListView is scrolled to show the loading
			// header/footer
			mRefreshableView.setSelection(selection);
		}
	}
	
	// New code from AgileMD
	public LoadingLayout getHeaderLoadingView() {
		return mHeaderLoadingView;
	}
	
	// If mHeaderLayout (the fake header view on top of your true refreshable view)
	// is visible, and mHeaderLoadingView (the true header view of your ListView)
	// is currently not visible, we want to scroll up such that mHeaderLayout
	// will still be visible and not disappear off the top.
	// In the original library this would happen in this case:
	// - an empty ListView is refreshing and you try to drag down again
	// (Basically, we want to show SOMETHING. We prefer to show the ListView's true
	//  header view (mHeaderLoadingView), but if that one is not visible, we want to
	//  make sure we can fall back to showing mHeaderLayout (the fake one) by saying
	//  "hey don't scroll up all the way in this particular case).
	@Override
	protected int getInitialYOffsetHeader() {
		if (DEBUG) Log.v(LOG_TAG, "getInitialYOffsetHeader");
		if (isRefreshing() &&
			mHeaderLayout.getVisibility() == View.VISIBLE &&
			mHeaderLoadingView.getVisibility() != View.VISIBLE) {
			switch (mCurrentMode) {
				case MANUAL_REFRESH_ONLY:
				case PULL_FROM_END:
					return getFooterSize();
				default:
				case PULL_FROM_START:
					if (DEBUG) Log.v(LOG_TAG, "GET NEGATIVE HEADER");
					return -getHeaderSize();
			}
		} else {
			return super.getInitialYOffsetHeader();
		}
	}
}
