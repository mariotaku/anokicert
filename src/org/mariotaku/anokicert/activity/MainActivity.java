package org.mariotaku.anokicert.activity;

import java.io.File;
import java.util.ArrayList;

import net.tuxed.gjokii.Gjokii.DeviceInfo;
import net.tuxed.gjokii.GjokiiException;
import net.tuxed.nokicert.CertListParser.CertListItem;
import net.tuxed.nokicert.CertParser;
import net.tuxed.nokicert.NokiCertUtils;

import org.mariotaku.anokicert.Constants;
import org.mariotaku.anokicert.R;
import org.mariotaku.anokicert.fragment.AlertDialogFragment;
import org.mariotaku.anokicert.util.AsyncNokiCertWrapper;
import org.mariotaku.anokicert.util.AsyncNokiCertWrapper.TaskListener;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.TextView;

public class MainActivity extends Activity implements Constants, OnClickListener {

	private static final String EXTRA_DEVICE = "file";
	private static final String EXTRA_FILE = "file";

	private ActionBar mActionBar;

	private BluetoothDevice mBluetoothDevice;
	private File mCertFile;
	private boolean mHasRunningTask;

	private AsyncNokiCertWrapper mNokiCert;

	private BluetoothAdapter mBluetoothAdapter;

	private View mEmptyView, mContentView, mContentScroller, mInstallCertificateContainer;
	private TextView mPhoneModelView, mFirmwareVersionView, mFirmwareDateView;

	private final BroadcastReceiver mBluetoothStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
				if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
					finish();
				}
			}
		}

	};

	@Override
	public void onClick(final View v) {
		if (mNokiCert == null || hasRunningTask()) return;
		switch (v.getId()) {
			case R.id.list_certificates: {
				listCertificates();
				break;
			}
			case R.id.choose_certificate: {
				final Intent intent = new Intent(this, FilePickerActivity.class);
				intent.putExtra(EXTRA_FILE_EXTENSIONS, new String[] { "cer", "der", "crt", "pem" });
				startActivityForResult(intent, REQUEST_PICK_FILE);
				break;
			}
			case R.id.perform_installation: {
				installCertificate(mCertFile);
				break;
			}
			case R.id.view_info: {
				showCertificateInfo(mCertFile);
				break;
			}
		}
	}

	@Override
	public void onContentChanged() {
		super.onContentChanged();
		mEmptyView = findViewById(R.id.no_device_selected);
		mContentView = findViewById(R.id.content);
		mContentScroller = findViewById(R.id.content_scroller);
		mPhoneModelView = (TextView) findViewById(R.id.phone_model);
		mFirmwareVersionView = (TextView) findViewById(R.id.firmware_version);
		mFirmwareDateView = (TextView) findViewById(R.id.firmware_date);
		mInstallCertificateContainer = findViewById(R.id.install_certificate_container);
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (hasRunningTask()) return false;
		switch (item.getItemId()) {
			case R.id.select_device: {
				final Intent intent = new Intent(this, DeviceSelectorActivity.class);
				startActivityForResult(intent, REQUEST_SELECT_DEVICE);
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		switch (requestCode) {
			case REQUEST_ENABLE_BT: {
				if (resultCode == RESULT_OK) {
					mActionBar.setSubtitle(null);
				} else if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
					finish();
					return;
				}
				break;
			}
			case REQUEST_SELECT_DEVICE: {
				if (resultCode == RESULT_OK) {
					mBluetoothDevice = data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					mEmptyView.setVisibility(View.GONE);
					mContentScroller.setVisibility(View.VISIBLE);
					mNokiCert = new AsyncNokiCertWrapper(mBluetoothDevice);
					showDeviceInfo();
				} else if (mNokiCert == null && (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())) {
					mEmptyView.setVisibility(View.VISIBLE);
					mContentScroller.setVisibility(View.GONE);
					mNokiCert = null;
				}
				break;
			}
			case REQUEST_PICK_FILE: {
				if (resultCode == RESULT_OK) {
					mCertFile = new File(data.getData().getPath());
					if (mCertFile.isFile()) {
						mInstallCertificateContainer.setVisibility(View.VISIBLE);
					}
				} else if (mCertFile == null || !mCertFile.isFile()) {
					mInstallCertificateContainer.setVisibility(View.GONE);
				}
				break;
			}
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mActionBar = getActionBar();
		mContentScroller.setVisibility(View.GONE);
		mEmptyView.setVisibility(View.VISIBLE);
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth
			AlertDialogFragment.show(this, R.string.bluetooth_not_supported, R.string.bluetooth_not_supported_message,
					true, getFragmentManager());
			return;
		}
		if (savedInstanceState != null) {
			mBluetoothDevice = savedInstanceState.getParcelable(EXTRA_DEVICE);

			mNokiCert = new AsyncNokiCertWrapper(mBluetoothDevice);
			mCertFile = (File) savedInstanceState.getSerializable(EXTRA_FILE);
			if (mCertFile.isFile()) {
				mInstallCertificateContainer.setVisibility(View.VISIBLE);
			} else if (mCertFile == null || !mCertFile.isFile()) {
				mInstallCertificateContainer.setVisibility(View.GONE);
			}
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(EXTRA_DEVICE, mBluetoothDevice);
		outState.putSerializable(EXTRA_FILE, mCertFile);
	}

	@Override
	protected void onStart() {
		super.onStart();
		setHasRunningTask(false);
		final IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mBluetoothStateReceiver, filter);
		if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
			mActionBar.setSubtitle(R.string.bluetooth_is_off);
			final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}

	@Override
	protected void onStop() {
		unregisterReceiver(mBluetoothStateReceiver);
		super.onStop();
	}

	private boolean hasRunningTask() {
		return mHasRunningTask;
	}

	private void installCertificate(final File certFile) {
		if (mNokiCert == null || certFile == null || !certFile.exists()) return;
		int keyUsage = 0;
		if (((CheckBox) findViewById(R.id.applications_signing)).isChecked()) {
			keyUsage |= NokiCertUtils.APPS_SIGNING;
		}
		if (((CheckBox) findViewById(R.id.cross_certification)).isChecked()) {
			keyUsage |= NokiCertUtils.CROSS_CERTIFICATION;
		}
		if (((CheckBox) findViewById(R.id.server_authentication)).isChecked()) {
			keyUsage |= NokiCertUtils.SERVER_AUTHENTIC;
		}
		mNokiCert.installCert(certFile, keyUsage, new InstallCertListener(this));
	}

	private void listCertificates() {
		if (mNokiCert == null || hasRunningTask()) return;
		mNokiCert.listCertificates(new ListCertsListener(this));
	}

	private void setHasRunningTask(final boolean hasRunningTask) {
		setProgressBarIndeterminateVisibility(hasRunningTask);
		mContentView.setEnabled(!hasRunningTask);
		mHasRunningTask = hasRunningTask;
	}

	private void showCertificateInfo(final File certFile) {
		if (certFile == null || !certFile.exists()) return;
		try {
			final CertParser cp = new CertParser(certFile);
			AlertDialogFragment.show(getString(R.string.cert_info), cp.toString(), false, getFragmentManager());
		} catch (final GjokiiException e) {
			e.printStackTrace();
		}

	}

	private void showDeviceInfo() {
		if (mNokiCert == null || hasRunningTask()) return;
		mNokiCert.getDeviceInfo(new GetPhoneInfoListener(this));

	}

	private void showErrorDialog(final Exception e, final boolean finish_activity) {
		final String message;
		if (e instanceof GjokiiException) {
			final GjokiiException ge = (GjokiiException) e;
			switch (ge.getErrorCode()) {
				case GjokiiException.CONNECTION_PROBLEM: {
					message = getString(R.string.connection_problem_message);
					break;
				}
				case GjokiiException.INVALID_CERT_FILE: {
					message = getString(R.string.invalid_cert_message);
					break;
				}
				default: {
					message = ge.getMessage();
					break;
				}
			}
		} else {
			message = e.getMessage();
		}
		AlertDialogFragment.show(getString(R.string.error), message, finish_activity, getFragmentManager());
	}

	private void showPhoneInfo(final DeviceInfo info) {
		if (info != null) {
			mPhoneModelView.setText(info.getPhoneModel());
			mFirmwareVersionView.setText(info.getFirmwareVersion());
			mFirmwareDateView.setText(info.getFirmwareDate());
		} else {
			AlertDialogFragment.show(this, R.string.phone_unsupported, R.string.phone_unsupported_message, true,
					getFragmentManager());
		}
	}

	private static class GetPhoneInfoListener implements TaskListener<DeviceInfo> {

		private final MainActivity mActivity;

		public GetPhoneInfoListener(final MainActivity activity) {
			mActivity = activity;
		}

		@Override
		public void onError(final Exception reason) {
			mActivity.setHasRunningTask(false);
			mActivity.showErrorDialog(reason, true);
		}

		@Override
		public void onFinished(final DeviceInfo result) {
			mActivity.setHasRunningTask(false);
			mActivity.showPhoneInfo(result);
		}

		@Override
		public void onStart() {
			mActivity.setHasRunningTask(true);
		}

	}

	private static class InstallCertListener implements TaskListener<Void> {
		private final MainActivity mActivity;

		private InstallCertListener(final MainActivity activity) {
			mActivity = activity;
		}

		@Override
		public void onError(final Exception reason) {
			mActivity.setHasRunningTask(false);
			mActivity.showErrorDialog(reason, false);
		}

		@Override
		public void onFinished(final Void result) {
			mActivity.setHasRunningTask(false);
			AlertDialogFragment.show(mActivity, R.string.successfully_installed,
					R.string.successfully_installed_message, false, mActivity.getFragmentManager());
		}

		@Override
		public void onStart() {
			mActivity.setHasRunningTask(true);
		}
	}

	private static class ListCertsListener implements TaskListener<ArrayList<CertListItem>> {
		private final MainActivity mActivity;

		private ListCertsListener(final MainActivity activity) {
			mActivity = activity;
		}

		@Override
		public void onError(final Exception reason) {
			mActivity.setHasRunningTask(false);
			mActivity.showErrorDialog(reason, false);
		}

		@Override
		public void onFinished(final ArrayList<CertListItem> result) {
			mActivity.setHasRunningTask(false);
			final Intent intent = new Intent(mActivity, DeviceCertListActivity.class);
			intent.putExtra(EXTRA_CERT_LIST, result);
			mActivity.startActivity(intent);
		}

		@Override
		public void onStart() {
			mActivity.setHasRunningTask(true);
		}
	}

}
