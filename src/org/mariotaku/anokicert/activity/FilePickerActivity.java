/*
 *				Twidere - Twitter client for Android
 * 
 * Copyright (C) 2012 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.anokicert.activity;

import static android.os.Environment.getExternalStorageDirectory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.mariotaku.anokicert.Constants;
import org.mariotaku.anokicert.R;
import org.mariotaku.anokicert.adapter.ArrayAdapter;

import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

public class FilePickerActivity extends ListActivity implements Constants, LoaderCallbacks<List<File>> {

	private static final String SHARED_PREFERENCES_NAME_FILE_PICKER = "file_picker";
	private static final String SHARED_PREFERENCES_KEY_LAST_PATH = "last_path";

	private File mCurrentDirectory;
	private FilesAdapter mAdapter;

	@Override
	public void onBackPressed() {
		setResult(RESULT_CANCELED);
		finish();
	}

	@Override
	public Loader<List<File>> onCreateLoader(final int id, final Bundle args) {
		final String[] extensions = args != null ? args.getStringArray(EXTRA_FILE_EXTENSIONS) : null;
		return new FilesLoader(this, mCurrentDirectory, extensions);
	}

	@Override
	public void onListItemClick(final ListView l, final View v, final int position, final long id) {
		final File file = mAdapter.getItem(position);
		if (file == null) return;
		if (file.isDirectory()) {
			mCurrentDirectory = file;
			getLoaderManager().restartLoader(0, getIntent().getExtras(), this);
		} else if (file.isFile()) {
			final Intent intent = new Intent();
			intent.setData(Uri.fromFile(file));
			setResult(RESULT_OK, intent);
			finish();
		}
	}

	@Override
	public void onLoaderReset(final Loader<List<File>> loader) {
		mAdapter.setData(null, null);

	}

	@Override
	public void onLoadFinished(final Loader<List<File>> loader, final List<File> data) {
		mAdapter.setData(data, mCurrentDirectory);
		if (mCurrentDirectory != null) {
			final String name = mCurrentDirectory.getName();
			setTitle(TextUtils.isEmpty(name) ? mCurrentDirectory.getAbsolutePath() : name);
			final SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME_FILE_PICKER, MODE_PRIVATE);
			prefs.edit().putString(SHARED_PREFERENCES_KEY_LAST_PATH, mCurrentDirectory.getAbsolutePath()).apply();
		}
	}

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent();
		final Uri data = intent.getData();
		mCurrentDirectory = data != null ? new File(data.getPath()) : null;
		if (mCurrentDirectory == null) {
			final SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME_FILE_PICKER, MODE_PRIVATE);
			final String last_path = prefs.getString(SHARED_PREFERENCES_KEY_LAST_PATH, null);
			if (last_path != null) {
				mCurrentDirectory = new File(last_path);
			}
			if (mCurrentDirectory == null || !mCurrentDirectory.exists()) {
				final File extDir = getExternalStorageDirectory();
				mCurrentDirectory = extDir != null ? extDir : new File("/");
			}
		}
		mAdapter = new FilesAdapter(this);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, getIntent().getExtras(), this);
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	static class FilesAdapter extends ArrayAdapter<File> {

		private final int mPadding;
		private File mCurrent;

		public FilesAdapter(final Context context) {
			super(context, android.R.layout.simple_list_item_1);
			mPadding = (int) (4 * context.getResources().getDisplayMetrics().density);
		}

		@Override
		public long getItemId(final int position) {
			return getItem(position).hashCode();
		}

		@Override
		public View getView(final int position, final View convertView, final ViewGroup parent) {
			final View view = super.getView(position, convertView, parent);
			final TextView text = (TextView) (view instanceof TextView ? view : view.findViewById(android.R.id.text1));
			final File file = getItem(position);
			if (file == null || text == null) return view;
			if (mCurrent != null && file.equals(mCurrent.getParentFile())) {
				text.setText("..");
			} else {
				text.setText(file.getName());
			}
			text.setSingleLine(true);
			text.setEllipsize(TruncateAt.MARQUEE);
			text.setPadding(mPadding, mPadding, position, mPadding);
			text.setCompoundDrawablesWithIntrinsicBounds(
					file.isDirectory() ? R.drawable.ic_folder : R.drawable.ic_file, 0, 0, 0);
			return view;
		}

		public void setData(final List<File> data, final File current) {
			clear();
			mCurrent = current;
			if (data != null) {
				addAll(data);
			}
		}

	}

	static class FilesLoader extends AsyncTaskLoader<List<File>> {

		private final File path;
		private final String[] extensions;

		private static final Comparator<File> NAME_COMPARATOR = new Comparator<File>() {
			@Override
			public int compare(final File file1, final File file2) {
				return file1.getName().toLowerCase().compareTo(file2.getName().toLowerCase());
			}
		};

		public FilesLoader(final Context context, final File path, final String[] extensions) {
			super(context);
			this.path = path;
			this.extensions = extensions;
		}

		@Override
		public List<File> loadInBackground() {
			if (path == null || !path.isDirectory()) return Collections.emptyList();
			final File[] listed_files = path.listFiles();
			if (listed_files == null) return Collections.emptyList();
			final List<File> dirs = new ArrayList<File>();
			final List<File> files = new ArrayList<File>();
			for (final File file : listed_files) {
				if (!file.canRead() || file.isHidden()) {
					continue;
				}
				if (file.isDirectory()) {
					dirs.add(file);
				} else if (file.isFile()) {
					final String name = file.getName();
					final int idx = name.lastIndexOf(".");
					if (extensionMatch(name.substring(idx + 1))) {
						files.add(file);
					}
				}
			}
			Collections.sort(dirs, NAME_COMPARATOR);
			Collections.sort(files, NAME_COMPARATOR);
			final List<File> list = new ArrayList<File>(dirs);
			list.addAll(files);
			if (path.getParent() != null) {
				list.add(0, path.getParentFile());
			}
			return list;
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}

		private boolean extensionMatch(final String extension) {
			if (extensions == null) return true;
			if (extensions.length == 0 && TextUtils.isEmpty(extension)) return true;
			for (final String ext : extensions) {
				if (ext.equalsIgnoreCase(extension)) return true;
			}
			return false;
		}
	}

}
