package org.mariotaku.anokicert.fragment;

import org.mariotaku.anokicert.Constants;
import org.mariotaku.anokicert.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class AlertDialogFragment extends DialogFragment implements Constants, OnClickListener {

	private static final String EXTRA_TITLE = "title";
	private static final String EXTRA_MESSAGE = "message";
	private static final String CLOSE_ACTIVITY = "finish_activity";
	private boolean mFinishActivity;
	private String mTitle, mMessage;

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
			mTitle = args.getString(EXTRA_TITLE);
			mMessage = args.getString(EXTRA_MESSAGE);
			mFinishActivity = args.getBoolean(CLOSE_ACTIVITY);
		}
		setCancelable(!mFinishActivity);
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(mTitle);
		builder.setMessage(mMessage);
		builder.setPositiveButton(mFinishActivity ? R.string.close : android.R.string.ok, this);
		return builder.create();
	}

	public static void show(final Context context, final int title, final int message, final boolean finish_activity,
			final FragmentManager fm) {
		show(context.getString(title), context.getString(message), finish_activity, fm,
				AlertDialogFragment.class.getName());
	}

	public static void show(final Context context, final int title, final int message, final boolean finish_activity,
			final FragmentManager fm, final String tag) {
		show(context.getString(title), context.getString(message), finish_activity, fm, tag);
	}

	public static void show(final String title, final String message, final boolean finish_activity,
			final FragmentManager fm) {
		show(title, message, finish_activity, fm, AlertDialogFragment.class.getName());
	}

	public static void show(final String title, final String message, final boolean finish_activity,
			final FragmentManager fm, final String tag) {
		final AlertDialogFragment f = new AlertDialogFragment();
		final Bundle args = new Bundle();
		args.putString(EXTRA_TITLE, title);
		args.putString(EXTRA_MESSAGE, message);
		args.putBoolean(CLOSE_ACTIVITY, finish_activity);
		f.setArguments(args);
		f.show(fm, tag);
	}

}
