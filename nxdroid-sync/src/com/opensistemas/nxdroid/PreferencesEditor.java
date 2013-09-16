package com.opensistemas.nxdroid;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.opensistemas.nxdroid.R;
import com.opensistemas.nxdroid.logic.DoMa;
import com.opensistemas.nxdroid.logic.DoMaService;

import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class PreferencesEditor extends PreferenceActivity {
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
             
        addPreferencesFromResource(R.xml.preferences);
        try {
        	List<File> list = DoMaService.getDoMaInstance().getPublicDirectories();
        	List<String> names = new ArrayList<String>();
        	List<String> values = new ArrayList<String>();
        	for (File f: list) {
        		values.add(f.getPath());
        		names.add(f.getPath().replaceFirst(Environment.getExternalStorageDirectory().getPath(), ""));        		
        	}
        	CharSequence[] nchars = names.toArray(new CharSequence[names.size()]);
        	CharSequence[] vchars = values.toArray(new CharSequence[values.size()]);
        	
        	ListPreference p = (ListPreference) getPreferenceScreen().findPreference(DoMa.PREF_KEY_DOCUMENTS_FOLDER);
        	p.setEntries(nchars);
        	p.setEntryValues(vchars);
        	p = (ListPreference) getPreferenceScreen().findPreference(DoMa.PREF_KEY_OTHERS_FOLDER);
        	p.setEntries(nchars);
        	p.setEntryValues(vchars);
        	p = (ListPreference) getPreferenceScreen().findPreference(DoMa.PREF_KEY_MUSIC_FOLDER);
        	p.setEntries(nchars);
        	p.setEntryValues(vchars);
        	p = (ListPreference) getPreferenceScreen().findPreference(DoMa.PREF_KEY_MOVIES_FOLDER);
        	p.setEntries(nchars);
        	p.setEntryValues(vchars);
        	p = (ListPreference) getPreferenceScreen().findPreference(DoMa.PREF_KEY_PICTURES_FOLDER);
        	p.setEntries(nchars);
        	p.setEntryValues(vchars);        	
        } catch (Throwable t) {
        	Log.e("tag", "tog", t);
        }
        
    }	
}
