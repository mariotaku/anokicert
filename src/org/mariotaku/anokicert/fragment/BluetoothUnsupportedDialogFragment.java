package org.mariotaku.anokicert.fragment;

import org.mariotaku.anokicert.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class BluetoothUnsupportedDialogFragment extends DialogFragment implements OnClickListener {

	@Override
	public void onActivityCreated(final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setCancelable(false);
	}

	@Override
	public void onClick(final DialogInterface dialog, final int which) {
		getActivity().finish();
	}

	@Override
	public Dialog onCreateDialog(final Bundle savedInstanceState) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.bluetooth_not_supported);
		builder.setMessage(R.string.bluetooth_not_supported_message);
		builder.setPositiveButton(android.R.string.ok, this);
		return builder.create();
	}

}
