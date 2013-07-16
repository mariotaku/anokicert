package org.mariotaku.anokicert.activity;

import java.util.ArrayList;

import net.tuxed.nokicert.CertListParser.CertListItem;

import org.mariotaku.anokicert.Constants;
import org.mariotaku.anokicert.adapter.DeviceCertListAdapter;

import android.app.ListActivity;
import android.os.Bundle;

public class DeviceCertListActivity extends ListActivity implements Constants {

	private DeviceCertListAdapter mAdapter;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		@SuppressWarnings("unchecked")
		final ArrayList<CertListItem> list = (ArrayList<CertListItem>) getIntent()
				.getSerializableExtra(EXTRA_CERT_LIST);
		mAdapter = new DeviceCertListAdapter(this);
		mAdapter.addAll(list);
		setListAdapter(mAdapter);
	}

}
