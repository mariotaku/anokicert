package org.mariotaku.anokicert.activity;

import java.util.Set;

import org.mariotaku.anokicert.Constants;
import org.mariotaku.anokicert.R;
import org.mariotaku.anokicert.adapter.BluetoothDevicesListAdapter;
import org.mariotaku.anokicert.adapter.BluetoothDevicesListAdapter.SearchAgainAction;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class DeviceSelectorActivity extends ListActivity implements Constants {

	private BluetoothDevicesListAdapter mDevicesAdapter;
	private BluetoothAdapter mBluetoothAdapter;

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			// When discovery finds a device
			if (BluetoothDevice.ACTION_FOUND.equals(action) || BluetoothDevice.ACTION_NAME_CHANGED.equals(action)) {
				// Get the BluetoothDevice object from the Intent
				final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				mDevicesAdapter.putDevice(device);
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				mDevicesAdapter.setIsDiscovering(true);
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				mDevicesAdapter.setIsDiscovering(false);
			} else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				final int state = mBluetoothAdapter.getState();
				if (state != BluetoothAdapter.STATE_ON && state != BluetoothAdapter.STATE_TURNING_ON) {
					DeviceSelectorActivity.this.setResult(RESULT_CANCELED);
					finish();
				}
			}
		}
	};

	protected void cancelDiscovery() {
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled() || !mBluetoothAdapter.isDiscovering()) return;
		mBluetoothAdapter.cancelDiscovery();
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_selector);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
			finish();
			return;
		}
		mDevicesAdapter = new BluetoothDevicesListAdapter(this);
		setListAdapter(mDevicesAdapter);
		final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		for (final BluetoothDevice device : pairedDevices) {
			mDevicesAdapter.putDevice(device);
		}
	}

	@Override
	protected void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final Object item = mDevicesAdapter.getItem(position);
		if (item instanceof BluetoothDevice) {
			final BluetoothDevice device = (BluetoothDevice) item;
			final Intent data = new Intent();
			data.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
			setResult(RESULT_OK, data);
		} else if (item instanceof SearchAgainAction) {
			startDiscovery();
			return;
		} else {
			setResult(RESULT_CANCELED);
		}
		finish();
	}

	@Override
	protected void onStart() {
		super.onStart();
		final IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(mReceiver, filter);
		startDiscovery();
	}

	@Override
	protected void onStop() {
		cancelDiscovery();
		unregisterReceiver(mReceiver);
		super.onStop();
	}

	protected void startDiscovery() {
		if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled() || mBluetoothAdapter.isDiscovering()) return;
		mBluetoothAdapter.startDiscovery();
	}

}
