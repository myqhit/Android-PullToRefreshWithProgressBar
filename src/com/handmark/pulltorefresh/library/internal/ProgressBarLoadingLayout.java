/*******************************************************************************
 * New code from AgileMD
 *******************************************************************************/
package com.handmark.pulltorefresh.library.internal;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Orientation;
import com.handmark.pulltorefresh.library.R;

public class ProgressBarLoadingLayout extends LoadingLayout {

	private final ProgressBar progressBar;

	@SuppressWarnings("rawtypes")
	public ProgressBarLoadingLayout(Context context, final Mode mode, final Orientation scrollDirection, TypedArray attrs) {
		super(context, mode, scrollDirection, attrs, true);
		progressBar = (ProgressBar)mInnerLayout.findViewById(R.id.progress);
	}

	// Override any header text changing because I handle that differently.
	@Override
	public void onPull(float scaleOfLayout) {
	}

	@Override
	public void pullToRefresh() {
	}

	@Override
	public void refreshing() {
	}

	@Override
	public void releaseToRefresh() {
	}

	@Override
	public void reset() {
	}

	@Override
	public void setLastUpdatedLabel(CharSequence label) {
	}

	@Override
	public void setLoadingDrawable(Drawable imageDrawable) {
	}

	@Override
	public void setPullLabel(CharSequence pullLabel) {
	}

	@Override
	public void setRefreshingLabel(CharSequence refreshingLabel) {
	}

	@Override
	public void setReleaseLabel(CharSequence releaseLabel) {
	}
	
	@Override
	protected int getDefaultDrawableResId() {
		return R.drawable.default_ptr_rotate;
	}

	@Override
	protected void onLoadingDrawableSet(Drawable imageDrawable) {
	}

	@Override
	protected void onPullImpl(float scaleOfLayout) {
	}

	@Override
	protected void pullToRefreshImpl() {
	}

	@Override
	protected void refreshingImpl() {
	}

	@Override
	protected void releaseToRefreshImpl() {
	}

	@Override
	protected void resetImpl() {
	}

	@Override
	@SuppressLint("NewApi")
	public void updateProgress(int progress,
							   boolean animate) {
		// Animate progress bar on new phones, just advance it on old phones
		if(android.os.Build.VERSION.SDK_INT >= 11 &&
		   animate) {
		    ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progress); 
		    animation.setDuration(250);
		    animation.setInterpolator(new DecelerateInterpolator());
		    animation.start();
		} else {
			// no animation on Gingerbread or lower
			progressBar.setProgress(progress);
		}
	}
}
