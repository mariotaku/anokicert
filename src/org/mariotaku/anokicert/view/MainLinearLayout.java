package org.mariotaku.anokicert.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class MainLinearLayout extends LinearLayout {

	public MainLinearLayout(final Context context) {
		super(context);
	}

	public MainLinearLayout(final Context context, final AttributeSet attrs) {
		super(context, attrs);
	}

	public MainLinearLayout(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean onInterceptTouchEvent(final MotionEvent ev) {
		if (!isEnabled()) return true;
		return super.onInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		return true;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		super.setEnabled(enabled);
		setAlpha(enabled ? 1 : 0.5f);
	}

}
