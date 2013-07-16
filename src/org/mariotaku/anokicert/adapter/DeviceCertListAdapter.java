package org.mariotaku.anokicert.adapter;

import net.tuxed.nokicert.CertListParser.CertListItem;

import org.mariotaku.anokicert.util.Utils;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DeviceCertListAdapter extends ArrayAdapter<CertListItem> {

	public DeviceCertListAdapter(final Context context) {
		super(context, android.R.layout.simple_list_item_2);
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);
		final CertListItem item = getItem(position);
		final TextView text1 = (TextView) view.findViewById(android.R.id.text1);
		final TextView text2 = (TextView) view.findViewById(android.R.id.text2);
		text1.setText(item.getFileName());
		text1.setSingleLine(true);
		text2.setTypeface(Typeface.MONOSPACE);
		text2.setSingleLine(true);
		text2.setText(Utils.hexEncode(item.getFingerprint()));
		return view;
	}

}
