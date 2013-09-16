package com.opensistemas.nxdroid.logic;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import com.opensistemas.nxdroid.logic.SyncFile.DocumentType;

import android.content.Context;
import android.os.Handler;

/**
 * This interface is the junction point between the View layer and the Logic layer.
 * 
 *  Still, some elements need to be passed down such as the Android Context.
 * 
 * @author jiyarza
 *
 */
public interface DoMa {
	
	public static final String PREF_KEY_USERNAME = "username";
	public static final String PREF_KEY_PASSWORD = "password";
	public static final String PREF_KEY_MAC = "mac";
	public static final String PREF_KEY_URL = "url";
	public static final String PREF_KEY_SYNC_OPTION = "sync_option";
	public static final String PREF_KEY_VIEW_MODE = "view_mode";
	public static final String PREF_KEY_PICTURES_FOLDER = "pictures_folder";
	public static final String PREF_KEY_MUSIC_FOLDER = "music_folder";
	public static final String PREF_KEY_MOVIES_FOLDER = "movies_folder";
	public static final String PREF_KEY_DOCUMENTS_FOLDER = "documents_folder";
	public static final String PREF_KEY_OTHERS_FOLDER = "others_folder";	
	
	public static final String PREF_VALUE_VIEW_MODE_LOCAL = "Local";
	public static final String PREF_VALUE_VIEW_MODE_LOCAL_AND_SERVER = "Local and Server";
	
	public boolean validatePreferences(Context context);
	public String getPreference(Context context, String key);
	public String getPreference(String key);
	
	public SyncFile[] getSyncFilesByType(DocumentType type);
	public List<File> getPublicDirectories();
	public String getBasePath(DocumentType type);
	public File getBasePathFile(DocumentType type);
	
	/**
	 * Deletes from the database all syncfiles of a given type
	 * Happens when the user changes the folder settings 
	 * @param type
	 */
	public int deleteSyncFiles(DocumentType type);
	
	/**
	 * Refreshes the sync state of all files of a given type
	 * @param context
	 * @param handler
	 * @param type
	 */
	public void refreshState(Context context, Handler handler, DocumentType type);	
	
	/**
	 * Syncs all files of a given type
	 * @param type
	 */
	public void synchronizeSelectedType(Context context, Handler handler, DocumentType type);
	
	/**
	 * Syncs the selected files
	 * @param files
	 */
	public void synchronizeSelectedFiles(Context context, Handler handler, SyncFile[] files);
	
	public boolean uploadFacebook(SyncFile sf);
	
	public boolean uploadTwitter(SyncFile sf);
	
	public boolean isConnectionEnabled();
	public boolean isHostReachable();
	//public boolean login();
	//public void logout();
	public String getMACAddress();
	
	public InputStream getRemoteThumbnail(String id);
	
	public void quit();

}
