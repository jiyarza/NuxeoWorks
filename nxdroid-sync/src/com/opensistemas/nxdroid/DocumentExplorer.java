package com.opensistemas.nxdroid;

import com.opensistemas.nxdroid.logic.DoMa;
import com.opensistemas.nxdroid.logic.SyncFile.DocumentType;

import android.content.SharedPreferences;
import android.widget.BaseAdapter;

public class DocumentExplorer extends AbstractExplorer {
	  
	@Override
	protected DocumentType getType() {
		return DocumentType.DOCUMENT;
	}	
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
		if (key.equals(DoMa.PREF_KEY_DOCUMENTS_FOLDER)) {
			folderChanged();
			((BaseAdapter)getListAdapter()).notifyDataSetChanged();
		} else if (key.equals(DoMa.PREF_KEY_VIEW_MODE)) {
			viewModeChanged();
			((BaseAdapter)getListAdapter()).notifyDataSetChanged();
		}
	}
}
