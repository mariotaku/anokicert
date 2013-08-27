package org.mariotaku.anokicert.adapter;

import java.util.ArrayList;

import org.mariotaku.anokicert.R;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class BluetoothDevicesListAdapter extends BaseAdapter {

	private final LayoutInflater mInflater;

	private final ArrayList<BluetoothDevice> mPairedDevices, mSearchedDevices;

	private final Context mContext;

	private boolean mIsDiscovering;

	public BluetoothDevicesListAdapter(final Context context) {
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mPairedDevices = new ArrayList<BluetoothDevice>();
		mSearchedDevices = new ArrayList<BluetoothDevice>();
	}

	@Override
	public int getCount() {
		final int paired_size = mPairedDevices.size(), searched_size = mSearchedDevices.size();
		return 1 + paired_size + 1 + (searched_size == 0 && !mIsDiscovering ? 1 : searched_size);
	}

	public BluetoothDevice getDeviceAt(final int position) {
		final Object item = getItem(position);
		if (item instanceof BluetoothDevice) return (BluetoothDevice) item;
		return null;
	}

	@Override
	public Object getItem(final int position) {
		final int paired_size = mPairedDevices.size(), searched_size = mSearchedDevices.size();
		if (position == 0) return new Category(mContext.getString(R.string.paired_devices), false);
		if (position == paired_size + 1) return new Category(mContext.getString(R.string.available_devices), true);
		if (paired_size > 0 && position > 0 && position < paired_size + 1) return mPairedDevices.get(position - 1);
		if (searched_size > 0 && position > 1 + paired_size && position < 1 + paired_size + 1 + searched_size)
			return mSearchedDevices.get(position - 1 - paired_size - 1);
		if (searched_size == 0 && position == getCount() - 1) return new SearchAgainAction();
		return null;
	}

	@Override
	public long getItemId(final int position) {
		return position;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		final View view = convertView != null ? convertView : mInflater.inflate(R.layout.device_list_item, null);
		final Object item = getItem(position);
		final View category_container = view.findViewById(R.id.category_container);
		final TextView name_view = (TextView) view.findViewById(R.id.name);
		final View search_again = view.findViewById(R.id.search_again);
		category_container.setVisibility(View.GONE);
		name_view.setVisibility(View.GONE);
		search_again.setVisibility(View.GONE);
		if (item instanceof BluetoothDevice) {
			final BluetoothDevice device = (BluetoothDevice) item;
			final String name = device.getName(), address = device.getAddress();
			name_view.setVisibility(View.VISIBLE);
			name_view.setText(TextUtils.isEmpty(name) ? address : name);
		} else if (item instanceof Category) {
			final Category category = (Category) item;
			final TextView category_name = (TextView) view.findViewById(R.id.category_name);
			final View category_progress = view.findViewById(R.id.category_progress);
			category_container.setVisibility(View.VISIBLE);
			category_progress.setVisibility(mIsDiscovering && category.isShowProgress() ? View.VISIBLE : View.GONE);
			category_name.setText(category.getName());
		} else if (item instanceof SearchAgainAction) {
			search_again.setVisibility(View.VISIBLE);
		}
		return view;
	}

	@Override
	public boolean isEnabled(final int position) {
		final Object item = getItem(position);
		return item instanceof BluetoothDevice || item instanceof SearchAgainAction;
	}

	public void putDevice(final BluetoothDevice device) {
		if (device == null) return;
		final int major = device.getBluetoothClass().getMajorDeviceClass();
		// ignore if it's not a phone
		if (major != BluetoothClass.Device.Major.PHONE) return;
		final int paired_idx = findDeviceIndex(mPairedDevices, device);
		final int searched_idx = findDeviceIndex(mSearchedDevices, device);
		if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
			if (paired_idx != -1) {
				mPairedDevices.set(paired_idx, device);
			} else {
				mPairedDevices.add(device);
			}
			if (searched_idx != -1) {
				mSearchedDevices.remove(searched_idx);
			}
		} else {
			if (searched_idx != -1) {
				mSearchedDevices.set(searched_idx, device);
			} else {
				mSearchedDevices.add(device);
			}
			if (paired_idx != -1) {
				mPairedDevices.remove(paired_idx);
			}
		}
		notifyDataSetChanged();
	}

	public void setIsDiscovering(final boolean is_discovering) {
		if (mIsDiscovering == is_discovering) return;
		mIsDiscovering = is_discovering;
		notifyDataSetChanged();
	}

	private static int findDeviceIndex(final ArrayList<BluetoothDevice> list, final BluetoothDevice device) {
		if (device == null) return -1;
		final int size = list.size();
		for (int i = 0; i < size; i++) {
			if (list.get(i).getAddress().equalsIgnoreCase(device.getAddress())) return i;
		}
		return -1;
	}

	public static class SearchAgainAction {

	}

	private static class Category {
		private final boolean show_progress;
		private final String name;

		private Category(final String name, final boolean show_progress) {
			this.name = name;
			this.show_progress = show_progress;
		}

		private String getName() {
			return name;
		}

		private boolean isShowProgress() {
			return show_progress;
		}
	}

}
