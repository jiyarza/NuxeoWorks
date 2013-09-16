package com.opensistemas.nxdroid.logic;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.ksoap2.serialization.SoapObject;

import com.opensistemas.nxdroid.logic.SyncFile.DocumentType;
import com.opensistemas.nxdroid.logic.SyncFile.SyncEvent;
import com.opensistemas.nxdroid.logic.SyncFile.SyncState;
import com.opensistemas.nxdroid.logic.SyncManager.ServerTypeRoot;
import com.opensistemas.nxdroid.logic.db.DBAdapter;
import com.opensistemas.nxdroid.logic.ws.SoapManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

class DoMaImpl implements DoMa {
	
	private static DoMa instance;
	
	private static Context context;
	
	private DBAdapter dbAdapter;
	
	private DoMaImpl() {
	}
	
	public static DoMa getInstance() {
		if (context == null) {
			throw new IllegalStateException("DoMa Context not initialized!");
		}		
		if (instance == null) {
			instance = new DoMaImpl();
		}
		((DoMaImpl)instance).initialize();
		return instance;
	}
	
	public static DoMa getInstance(Context ctx) {
		context = ctx;
		return getInstance();
	}
	
	protected void initialize() {
		if (context == null) {
			throw new IllegalStateException("DoMa Context not initialized!");
		}		
		dbAdapter = new DBAdapter(context);				
	}

	/**
	 * Returns true if all preferences are set, otherwise returns false.
	 * 
	 * @param context
	 * @return
	 */
	@Override
	public boolean validatePreferences(Context context) {		
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        
        if (pref.getString(DoMa.PREF_KEY_USERNAME, "").trim().equals("") ||
	        pref.getString(DoMa.PREF_KEY_DOCUMENTS_FOLDER, "").trim().equals("") ||
	        pref.getString(DoMa.PREF_KEY_MOVIES_FOLDER, "").trim().equals("") ||
	        pref.getString(DoMa.PREF_KEY_MUSIC_FOLDER, "").trim().equals("") ||
	        pref.getString(DoMa.PREF_KEY_OTHERS_FOLDER, "").trim().equals("") ||
	        pref.getString(DoMa.PREF_KEY_PASSWORD, "").trim().equals("") ||
	        pref.getString(DoMa.PREF_KEY_PICTURES_FOLDER, "").trim().equals("") ||
	        pref.getString(DoMa.PREF_KEY_SYNC_OPTION, "").trim().equals("") ||
	        pref.getString(DoMa.PREF_KEY_URL, "").trim().equals("") ||
	        pref.getString(DoMa.PREF_KEY_VIEW_MODE, "").trim().equals("")) {        	
        	return false;
        }
        return true;
	}
	
	@Override
	public String getPreference(Context context, String key) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key,"");
	}	

	@Override
	public String getPreference(String key) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key,"");
	}
	
	private boolean isServerMode() {
		String viewMode = getPreference(PREF_KEY_VIEW_MODE);
		return viewMode.equals("Local and Server");		
	}
	
	@Override
	public SyncFile[] getSyncFilesByType(DocumentType type) {
		return dbAdapter.getSyncFilesByType(type, isServerMode());
	}
	
	@Override
	public List<File> getPublicDirectories() {
		return FileManager.getInstance().getPublicDirectories();
	}
	
	/**
	 * Synchronizes all the files of one type according to their current SyncState (folder sync)
	 */
	@Override
	public void synchronizeSelectedType(Context context, Handler handler, DocumentType type) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Synchronizes only the selected files referenced in the given array,
	 * according to their current SyncState
	 */
	@Override
	public synchronized void synchronizeSelectedFiles(Context context, Handler handler, SyncFile[] files) {
		Log.w("SynchronizingSelectedFiles", ""+files.length);
		try {
			SoapManager.getInstance().login();
			for(SyncFile f: files){
				Log.w("synchronizeSelectedFiles", "SyncState = " + f.getSyncState().name());
				switch (f.getSyncState()) {
				case Conflict:
					resolveConflict(f);
					break;
				case Deleted:
					delete(f);
					break;
				case LocallyDeleted:
					// FIXM depends on sync option?
					delete(f);
					break;
				case LocallyModified:
					uploadChanges(f);
					break;
				case LocallyNew:
					uploadNew(f);
					break;
				case RemotelyDeleted:
					// FIXM depends on sync option?
					delete(f);
				case RemotelyModified:
					download(f);
				case RemotelyNew:
					download(f);
					break;
				case Synchronized:
					break;				
				default:
					break;
				}
			}
		} finally {
			SoapManager.getInstance().logout();
		}
	}

	private void uploadChanges(SyncFile sf) {
		//update the current file in remote server
		Log.i("DoMaImpl.uploadChanges", "File modified locally -> Updating file " + sf.getName() + " remotely.");
		Log.i("DoMaImpl.uploadChanges", "Remote path ->" + sf.getRemotePath());
		SoapObject parent = SyncManager.getInstance().getDocumentByPath(sf.getRemotePath());
		SoapObject res = SyncManager.getInstance().updateDocument(sf, parent);
		if(res != null){
			dbAdapter.save(sf);
		}
	}

	/**
	 * Creates in the server all the folders in the given path
	 * that do not exist yet.
	 * @param file containing the path relative to the root folder of its type
	 * @return the SoapObject representing the full path
	 */
	private SoapObject createPath(SoapObject root, File f) {
		if (f == null) return null;
		SoapObject parent = createPath(root, f.getParentFile());
		if (parent == null) {
			parent = root;
		} 
		String parentId = parent.getProperty("id").toString();
		String name = f.getName();
		if (name == null || name.equals("")) {
			name = f.getPath().substring(f.getPath().lastIndexOf("/"));
		}
		Log.i("DoMaImpl.createPath()->", name);
		SoapObject result = SyncManager.getInstance().getDocumentByTitle(parent, f.getName());
		if (result == null) {
			Log.i("DoMaImpl.createPath()->", "Root type: " + root.getProperty("type").toString());
			if (root.getProperty("type").toString().equals(ServerTypeRoot.OrangeImageGallery.name())) {
				result = SyncManager.getInstance().createOrangeImageGallery(parentId, f.getName());
			} else {
				result = SyncManager.getInstance().createFolder(parentId, f.getName());
			}
		} else {
			Log.i("getDocumentByTitle.result=", result.toString());
		}
		return result;
	}
	
	private void uploadNew(SyncFile sf) {
		//sync the current file
		Log.i("MainScreenView.uploadToServer()", "Uploading file: " + sf.getName());
		//String parentId = SyncManager.getInstance().getFileIdFromPath(sf.getRemotePath());
		// We need to create all the relative local path segments (folders) in the server and 
		// then retrieve the id of the parent folder
		
		// The common part of the local path
		String commonPath = sf.getLocalPathRelativeToType();
		Log.i("Common path: ", commonPath);
		// The absolute base path in the server 
		SoapObject root = SyncManager.getInstance().getRootFolder(sf.getDocumentType());
		if (root == null) {
			Log.e("DoMaImpl.uploadNew", "Upload FAILED: ROOT is NULL");
			//logout();
			return;
		}
		File f = new File(commonPath).getParentFile();
		String rootPath = root.getProperty("path").toString();
		Log.i("uploadNew","rootPath="+rootPath);
		SoapObject parent = null;
		if (f != null) {
			parent = SyncManager.getInstance().getDocumentByPath(rootPath + "/" + f.getPath().toLowerCase());
		}
		if (parent == null) {
			parent = createPath(root, new File(commonPath).getParentFile());
		}
		if (parent == null) {
			parent = root;
			Log.i("DoMaImpl.uploadNew", "Using root as parent: " + root.getProperty("type").toString());
		}
		// Upload the document
		SoapObject res = null;
		String parentId = null;
		if (parent != null) {
			parentId = parent.getProperty("id").toString();
			res = SyncManager.getInstance().createDocument(sf, parentId);		
			if(res != null){
				Object o = res.getProperty("dateModify");
				if (o == null) {
					o = res.getProperty("dateCreate");
				}
				if (o != null) {
					String date = o.toString();
					sf.setRemoteModifiedDate(SyncManager.getInstance().getDateFromString(date));						
				}
				
				sf.setRemoteId(res.getProperty("id").toString());
				sf.setRemoteName(res.getProperty("title").toString());
				//sf.setRemoteName(res.getProperty("filename").toString());
				sf.setRemotePath(res.getProperty("path").toString());
				sf.updateSyncState(SyncEvent.Uploaded);						
				dbAdapter.save(sf);
				Log.d("DoMaImpl.uploadNew", "BD Result: " + sf.toString());
			}
		}		
	}	

	/**
	 * When downloading a document, the remote data prevails over the local data.
	 * This means using the remote relative path within the docs type folder (not including the
	 * type root folder)
	 * @param sf
	 */
	private void download(SyncFile sf){
		//login();
		SyncManager.getInstance().downloadDocument(getBasePath(sf.getDocumentType()), sf);
		//logout();
		sf.updateSyncState(SyncEvent.Downloaded);
		dbAdapter.save(sf);
	}	
	
	
	private void delete(SyncFile sf){
		Log.i("MainScreenView.deleteFromDB()", "Deleting file from DB " + sf.getName());
		dbAdapter.delete(sf);
	}
	
	private void resolveConflict(SyncFile sf){		
		SyncOption opt = SyncOption.valueOf(getPreference(PREF_KEY_SYNC_OPTION));
		if(opt.equals(SyncOption.Upload)) {
			uploadNew(sf);
		} else if(opt.equals(SyncOption.Download)) {
			//sf.setRemoteName(sf.getFileName());			
			download(sf);
		}
		//TODO ask the user
		/*else if(cManager.getSyncOption().equals(SyncOption.Ask.name())){
			askUser(file);
		}*/
		//do nothing
		else if(opt.equals(SyncOption.DoNothing)){
			sf.setSyncStateDate(System.currentTimeMillis());
			dbAdapter.save(sf);
		}
	}	
	
	/**
	 * Publishes a document in the user's Facebook account
	 */
	@Override
	public boolean uploadFacebook(SyncFile sf) {
		return SyncManager.getInstance().publish(sf,
				PublishOptions.upload_facebook.name());	}

	/**
	 * Publishes a Picture in the user's Twitter account
	 */
	@Override
	public boolean uploadTwitter(SyncFile sf) {
		return SyncManager.getInstance().publish(sf,
				PublishOptions.upload_twitter.name());		
	}

	/**
	 * Returns the local base path for a given document type
	 */
	public String getBasePath(DocumentType type) {
		switch (type) {
		case DOCUMENT:
			return getPreference(PREF_KEY_DOCUMENTS_FOLDER) + "/";
		case MOVIE:
			return getPreference(PREF_KEY_MOVIES_FOLDER) + "/";
		case MUSIC:
			return getPreference(PREF_KEY_MUSIC_FOLDER) + "/";
		case OTHER:
			return getPreference(PREF_KEY_OTHERS_FOLDER) + "/";
		case PICTURE:
			return getPreference(PREF_KEY_PICTURES_FOLDER) + "/";
		default:
			return getPreference(PREF_KEY_OTHERS_FOLDER) + "/";
		}
	}
	
	public File getBasePathFile(DocumentType type) {
		return new File(getBasePath(type));
	}
	
	/**
	 * Updates the database and recalculates each file sync state according 
	 * to the current ViewMode.
	 * 
	 * Handler is used to notify progress to the calling thread.
	 */
	@Override
	public synchronized void refreshState(Context context, Handler handler, DocumentType type) {
		int n = dbAdapter.deleteInconsistencies();
		Log.i("DoMaImpl.refreshState", n + " inconsistent records deleted.");
		
		// ViewMode determines if sync is local or also server 
		String viewMode = PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_KEY_VIEW_MODE, PREF_VALUE_VIEW_MODE_LOCAL);		
		
		updateLocallyNewAndModified(context, handler, type);		
		updateLocallyDeleted(context, handler, type);
		
		if (viewMode.equals(DoMa.PREF_VALUE_VIEW_MODE_LOCAL_AND_SERVER)) {
			SoapManager.getInstance().login();
			updateRemotelyNewAndModified(context, handler, type);
			updateRemotelyDeleted(context, handler, type);
			SoapManager.getInstance().logout();
		}
		
        Message msg = handler.obtainMessage();
        msg.arg1 = 0;
        msg.arg2 = 100;		
		handler.sendMessage(msg);
	}
		
	/**
	 * Reads the filesystem and inserts all new files of a given type 
	 * into the database. Updates existing SyncFiles whose file's 
	 * modified date has changed.
	 * 
	 * @param context
	 *
	 */	
	private void updateLocallyNewAndModified(Context context, Handler handler, DocumentType type) {
		File[] files = FileManager.getInstance().getLocalFiles(context, type);
		int i = 0;
		int n = files.length;
		for (File f : files) {
			updateDatabaseWithLocalFile(f);
	        Message msg = handler.obtainMessage();
	        msg.arg1 = 0;
	        msg.arg2 = i++ * 25 / n;
			handler.sendMessage(msg);			
		}		
	}
	
	/**
	 * Reads the database and checks if the referenced files still exist.
	 * Updates the SyncFile states accordingly. 
	 * Deletes SyncFiles with state Deleted. 
	 * 
	 * @param context
	 */
	private void updateLocallyDeleted(Context context, Handler handler, DocumentType type) {
		SyncFile[] files;
		files = dbAdapter.getSyncFilesByType(type, false);
		int i = 0;
		int n = files.length;		
		for (SyncFile f : files) {
			if (f.getLocalPath() != null) {
				if (f.getFile() == null || !f.getFile().exists()) {
					dbAdapter.delete(f);
				}
			}
	        Message msg = handler.obtainMessage();
	        msg.arg1 = 0;
	        msg.arg2 = 25 + (i++ * 25 / n);
			handler.sendMessage(msg);			
		}
	}	

	private void updateRemotelyNewAndModified(Context context, Handler handler, DocumentType type) {
		Log.d("**** DoMaImpl.updateRemotelyNewAndModified ****","ENTER");
		SoapObject[] files = SyncManager.getInstance().getDocumentsByType(context, type);
		int i = 0;
		int n = files.length;
		for (SoapObject f : files) {
			updateDatabaseWithSoapObject(f, type);				
	        Message msg = handler.obtainMessage();
	        msg.arg1 = 0;
	        msg.arg2 = 50 + (i++ * 25 / n);
			handler.sendMessage(msg);
		}
		Log.d("**** DoMaImpl.updateRemotelyNewAndModified ****","EXIT");
	}
	
	private void updateRemotelyDeleted(Context context, Handler handler, DocumentType type) {
		Log.d("**** DoMaImpl.updateRemotelyDeleted ****","ENTER");
		SyncFile[] files;
		files = dbAdapter.getSyncFilesByType(type, false);
		int i = 0;
		int n = files.length;		
		for (SyncFile f : files) {
			if (f.getRemoteId() != null) {
				if (!SyncManager.getInstance().exists(f)) {
					f.updateSyncState(SyncEvent.RemotelyDeleted);
					if (f.getSyncState().equals(SyncState.Deleted)) {
						dbAdapter.delete(f);
					} else {
					// nullify remote data
						f.setRemoteId(null);
						f.setRemoteModifiedDate(0);
						f.setRemoteName(null);
						f.setRemotePath(null);
						dbAdapter.save(f);
					}
				}
			}
	        Message msg = handler.obtainMessage();
	        msg.arg1 = 0;
	        msg.arg2 = 75 + (i++ * 25 / n);
			handler.sendMessage(msg);			
		}
		Log.d("**** DoMaImpl.updateRemotelyDeleted ****","EXIT");
	}
	
	/**
	 * Synchronizes the state of a SyncFile in the database, given a local File.<br>
	 * 
	 * - If !exists a file with the same local path, then inserts the File
	 * filling all local information available and sets its sync state as
	 * locally new.<br>
	 * 
	 * - If does exist, updates its last local modification date if it has
	 * changed and sets its sync state as locally modified.<br>
	 * 
	 * @param f
	 * @return The SyncFile representing the passed File
	 */
	private SyncFile updateDatabaseWithLocalFile(File f) {
		if (f == null) {
			Log.i("updateDatabaseWithLocalFile", "File is NULL");
			return null;
		}
		SyncFile sf = dbAdapter.getSyncFileByLocalPath(f.getPath());
		// TODO check if already exists as remote?		
		if (sf == null) {
			sf = new SyncFile(f.getPath());
			dbAdapter.save(sf);
			Log.i("updateDatabaseWithLocalFile", "New SyncFile inserted: " + sf.getLocalPath());
		} else {
			final long lastModified = f.lastModified();
			if (sf.getLocalModifiedDate() < lastModified) {
				sf.setLocalModifiedDate(lastModified);
				sf.updateSyncState(SyncEvent.LocallyModified);
				dbAdapter.save(sf);				
			}
			Log.i(getClass().getName()+".updateLocalFileWithDatabase()", sf.getLocalPath() + " updated." + sf.getDocumentType());
		}
		return sf;
	}
	
	private SyncFile updateDatabaseWithSoapObject(SoapObject f, DocumentType type) {
		// NOTE: Two files are stored in DB as the same if their path is the same
		// using as path just after the root folder (ie removing the local/remote root folders)		
		if (f == null) {
			Log.i("updateDatabaseWithSoapObject", "SoapObject is NULL");
			return null;
		}
		SyncFile sf = dbAdapter.getSyncFileByRemoteId(f.getProperty("id").toString());
		// TODO check if already exists as local?
		if (sf == null) {
			sf = SyncManager.getInstance().buildSyncFile(f, type);
			dbAdapter.save(sf);
			Log.i("updateDatabaseWithSoapObject", "New SyncFile inserted: " + sf.getRemoteName());
		} else {
			boolean changed = false;
			if (sf.getRemotePath() == null || sf.getRemotePath().length() == 0) {
				sf.setRemotePath(f.getProperty("path").toString());
				changed = true;
			}
			if (sf.getRemoteName() == null || sf.getRemoteName().length() == 0) {
				sf.setRemoteName(f.getProperty("filename").toString());
				changed = true;
			}			
			final long lastModified = SyncManager.getInstance().getRemoteModifiedDate(f);
			if (sf.getRemoteModifiedDate() < lastModified) {
				long diff = lastModified - sf.getRemoteModifiedDate(); 
				Log.d("DoMaImpl.updateDatabaseWithSoapObject", "About to be marked as RemotelyModified:" + sf.toString());
				Log.d("DoMaImpl.updateDatabaseWithSoapObject", "Diff: " + diff);
				sf.setRemoteModifiedDate(lastModified);
				if (diff > 50000) {
					sf.updateSyncState(SyncEvent.RemotelyModified);
				}				
				changed = true;				
			}
			if (changed) {
				dbAdapter.save(sf);
			}
			Log.i(getClass().getName()+".updateDatabaseWithSoapObject()", sf.getLocalPath() + " updated." + sf.getDocumentType());
		}
	return sf;
	}

	@Override
	public int deleteSyncFiles(DocumentType type) {
		return dbAdapter.delete(type);
	}


	//////////////////////////////////////
	// COMM								//
	//////////////////////////////////////
	
	public boolean isConnectionEnabled(){
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if(netInfo != null && netInfo.isConnected())
	    	return true;
	    else
	    	return false;
	}
	
	
	public boolean isHostReachable(){
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		String curUrl = getPreference(PREF_KEY_URL);
		URL urlAddress = null;
		try {
			urlAddress = new URL(curUrl);
		} catch (MalformedURLException e) {
			Log.e("hostAccess", "URL incorrect");
		}
		if(urlAddress != null){
			int host = lookupHost(urlAddress);
			//Log.i("hostAccess", "host: " + host);
			boolean access = cm.requestRouteToHost(ConnectivityManager.TYPE_WIFI, host);
			if(!access)
				access = cm.requestRouteToHost(ConnectivityManager.TYPE_MOBILE, host);
			Log.d("hostAccess", "Host access? " + access);
			//return access;
			return true;
		}
		else
			return true;
	}

	private int lookupHost(URL urlAddress) {
		String hostname = urlAddress.getHost();
	    InetAddress inetAddress;
	    try {
	        inetAddress = InetAddress.getByName(hostname);
	    } catch (UnknownHostException e) {
	        return -1;
	    }
	    byte[] addrBytes;
	    int addr;
	    addrBytes = inetAddress.getAddress();
	    addr = ((addrBytes[3] & 0xff) << 24)
	            | ((addrBytes[2] & 0xff) << 16)
	            | ((addrBytes[1] & 0xff) << 8)
	            |  (addrBytes[0] & 0xff);
	    return addr;
	}
	
	@Override
	public String getMACAddress() {
		String MAC = null;
		try{
			//MAC Address
			WifiManager wMng = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			wMng.setWifiEnabled(true);
			//Log.i("DomaSync" , "WIFI: " + wMng.isWifiEnabled());
			WifiInfo wInfo = wMng.getConnectionInfo();
			MAC = wInfo.getMacAddress();
			//Log.i("DomaSync" , "MAC: " + MAC);
			Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
			editor.putString(PREF_KEY_MAC, MAC);
			editor.commit();
			return MAC;
		} catch (Exception e){
			Log.e("DomaSync.getMACAddress()", "Exception: ", e);
			return null;
		}
	}

	@Override
	public InputStream getRemoteThumbnail(String id) {
		return SyncManager.getInstance().getRemoteThumbnail(id);
	}
	
	public void quit() {
		SoapManager.getInstance().logout();
	}
}
