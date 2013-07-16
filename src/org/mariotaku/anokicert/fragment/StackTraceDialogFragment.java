package org.mariotaku.anokicert.fragment;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.mariotaku.anokicert.Constants;
import org.mariotaku.anokicert.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class StackTraceDialogFragment extends DialogFragment implements Constants, OnClickListener {

	private static final String EXTRA_THROWABLE = "throwable";
	private static final String SEREVE_PROBLEM = "finish_activity";
	private boolean mFinishActivity;
	private Throwable mThrowable;

	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		if (mFinishActivity) {
			getActivity().finish();
		}
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final Bundle args = getArguments();
		if (args != null) {
			mThrowable = (Throwable) args.getSerializable(EXTRA_THROWABLE);
			mFinishActivity = args.getBoolean(SEREVE_PROBLEM);
		}
		setCancelable(!mFinishActivity);
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.error);
		if (mThrowable != null) {
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			mThrowable.printStackTrace(pw);
			builder.setMessage(sw.toString());
		}
		builder.setPositiveButton(mFinishActivity ? R.string.close : android.R.string.ok, this);
		return builder.create();
	}

	public static void show(final Throwable t, final boolean finish_activity, final FragmentManager fm) {
		show(t, finish_activity, fm, StackTraceDialogFragment.class.getName());
	}

	public static void show(final Throwable t, final boolean finish_activity, final FragmentManager fm, final String tag) {
		final StackTraceDialogFragment f = new StackTraceDialogFragment();
		final Bundle args = new Bundle();
		args.putSerializable(EXTRA_THROWABLE, t);
		args.putBoolean(SEREVE_PROBLEM, finish_activity);
		f.setArguments(args);
		f.show(fm, tag);
	}

}
