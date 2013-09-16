package com.opensistemas.nxdroid;

import com.opensistemas.nxdroid.logic.DoMa;
import com.opensistemas.nxdroid.logic.SyncFile.DocumentType;

import android.content.SharedPreferences;
import android.widget.BaseAdapter;

public class OtherExplorer extends AbstractExplorer {
	
	@Override
	protected DocumentType getType() {
		return DocumentType.OTHER;
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		if (key.equals(DoMa.PREF_KEY_OTHERS_FOLDER)) {
			folderChanged();
			((BaseAdapter)getListAdapter()).notifyDataSetChanged();
		} else if (key.equals(DoMa.PREF_KEY_VIEW_MODE)) {
			viewModeChanged();
			((BaseAdapter)getListAdapter()).notifyDataSetChanged();
		}
	}
}
