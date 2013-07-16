package org.mariotaku.anokicert.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import net.tuxed.gjokii.Gjokii;
import net.tuxed.gjokii.Gjokii.DeviceInfo;
import net.tuxed.misc.Utils;
import net.tuxed.nokicert.CertListParser.CertListItem;
import net.tuxed.nokicert.NokiCert;

import org.mariotaku.anokicert.BuildConfig;

import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;

public class AsyncNokiCertWrapper {

	private final BluetoothDevice mDevice;

	public AsyncNokiCertWrapper(final BluetoothDevice device) {
		mDevice = device;
	}

	public void getDeviceInfo(final TaskListener<Gjokii.DeviceInfo> listener) {
		new GetDeviceInfoTask(mDevice, listener).execute();
	}

	public void installCert(final File certFile, final int keyUsage, final TaskListener<Void> listener) {
		new InstallCertificateTask(mDevice, certFile, keyUsage, listener).execute();
	}

	public void listCertificates(final TaskListener<ArrayList<CertListItem>> taskListener) {
		new ListCertificatesTask(mDevice, taskListener).execute();
	}

	public interface DeviceConnectionListener {
		void onDeviceConnected();

		void onDeviceConnectionError(Exception reason);

		void onDeviceDisconnected();
	}

	public interface TaskListener<Result> {

		public void onError(Exception reason);

		public void onFinished(Result result);

		public void onStart();
	}

	static abstract class BaseTask<L extends TaskListener<R>, R> extends AsyncTask<Void, Void, BaseTask.Result<R>> {

		private final L mListener;
		private final BluetoothDevice mDevice;

		BaseTask(final BluetoothDevice device, final L listener) {
			mDevice = device;
			mListener = listener;
		}

		protected abstract R doInBackground(NokiCert nokicert) throws IOException;

		@Override
		protected final Result<R> doInBackground(final Void... params) {
			NokiCert nokicert = null;
			try {
				nokicert = new NokiCert(mDevice, BuildConfig.DEBUG);
				return new Result<R>(doInBackground(nokicert), null);
			} catch (final IOException e) {
				return new Result<R>(null, e);
			} finally {
				Utils.closeSliently(nokicert);
			}
		}

		@Override
		protected final void onPostExecute(final Result<R> result) {
			if (mListener == null) return;
			if (result.exception == null) {
				mListener.onFinished(result.result);
			} else {
				mListener.onError(result.exception);
			}
		}

		@Override
		protected void onPreExecute() {
			if (mListener == null) return;
			mListener.onStart();
		}

		static class Result<R> {
			private final R result;
			private final Exception exception;

			Result(final R result, final Exception reason) {
				this.result = result;
				this.exception = reason;
			}

			static <R> Result<R> getInstance(final Exception reason) {
				return new Result<R>(null, reason);
			}

			static <R> Result<R> getInstance(final R result) {
				return new Result<R>(result, null);
			}
		}

	}

	static class GetDeviceInfoTask extends BaseTask<TaskListener<Gjokii.DeviceInfo>, Gjokii.DeviceInfo> {

		GetDeviceInfoTask(final BluetoothDevice device, final TaskListener<DeviceInfo> listener) {
			super(device, listener);
		}

		@Override
		protected DeviceInfo doInBackground(final NokiCert nokicert) throws IOException {
			return nokicert.getInfo();
		}

	}

	static class InstallCertificateTask extends BaseTask<TaskListener<Void>, Void> {

		private final int mKeyUsage;
		private final File mCertFile;

		InstallCertificateTask(final BluetoothDevice device, final File certFile, final int keyUsage,
				final TaskListener<Void> listener) {
			super(device, listener);
			mCertFile = certFile;
			mKeyUsage = keyUsage;
		}

		@Override
		protected Void doInBackground(final NokiCert nokicert) throws IOException {
			nokicert.installCertificate(mCertFile.getAbsolutePath(), mKeyUsage);
			return null;
		}

	}

	static class ListCertificatesTask extends BaseTask<TaskListener<ArrayList<CertListItem>>, ArrayList<CertListItem>> {

		ListCertificatesTask(final BluetoothDevice device, final TaskListener<ArrayList<CertListItem>> listener) {
			super(device, listener);
		}

		@Override
		protected ArrayList<CertListItem> doInBackground(final NokiCert nokicert) throws IOException {
			return nokicert.listCertificates();
		}

	}

}
