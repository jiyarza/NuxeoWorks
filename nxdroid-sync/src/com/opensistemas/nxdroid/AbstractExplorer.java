package com.opensistemas.nxdroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.R.drawable;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.BaseRequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;
import com.facebook.android.SessionEvents;
import com.opensistemas.nxdroid.logic.DoMa;
import com.opensistemas.nxdroid.logic.DoMaService;
import com.opensistemas.nxdroid.logic.FileUtil;
import com.opensistemas.nxdroid.logic.FlushedInputStream;
import com.opensistemas.nxdroid.logic.SyncFile;
import com.opensistemas.nxdroid.logic.SyncFile.DocumentType;
import com.opensistemas.nxdroid.logic.SyncFile.SyncState;

/**
 * 
 * This is a base class for each content tabs (Pics, Music, Movies, Docs,
 * Other). Most UI logic is in this class and there is a specific class for each
 * type of content that simply tells what DocumentType it handles and implements
 * listener interfaces.
 * 
 * 
 * 
 * @author loran
 * @author jiyarza
 * 
 */
public abstract class AbstractExplorer extends ListActivity implements
		SimpleAdapter.ViewBinder, OnItemClickListener,
		OnSharedPreferenceChangeListener {
	// A token with which the preferences subactivity is launched
	private static final int REQUEST_CODE_PREFERENCES = 1;

	// Each row's data
	private List<Map<String, Object>> data;

	// view fields
	public static final String VIEW_FILE_ICON = "fileIcon";
	public static final String VIEW_FILENAME = "filename";
	public static final String VIEW_PATH = "path";
	public static final String VIEW_DATE = "date";
	public static final String VIEW_STATE_ICON = "stateIcon";
	public static final String VIEW_CHECKBOX = "checkbox";

	// Backing files array
	private SyncFile[] files;

	// Sync state icons
	private static Drawable[] syncStateIcons;

	private static Drawable musicIcon = null;
	private static Drawable imageIcon = null;
	private static Drawable movieIcon = null;
	private static Drawable documentIcon = null;
	private static Drawable otherIcon = null;

	// DoMa interface
	private static DoMa doma = null;

	// Dialogs / threading
	private static final int LOADING_DIALOG = 0;
	private static final int SYNC_ALERT_DIALOG = 1;
	private static final int SYNCHRONIZING_DIALOG = 2;

	private ProgressDialog loadingDialog;
	private ProgressDialog synchronizingDialog;
	private Dialog alertDialog;

	private static final int TASK_REFRESHING = 0;
	private static final int TASK_LOADING = 1;
	private static final int TASK_SYNCHRONIZING = 2;

	LoadingThread loadingThread;
	RefreshingThread refreshingThread;
	ChangingFolderThread changingFolderThread;
	SynchronizingSelectedFilesThread synchronizingSelectedFilesThread;
	SynchronizingSelectedTypeThread synchronizingSelectedTypeThread;

	private Facebook mFacebook;
	private AsyncFacebookRunner mAsyncRunner;
	public static final String DEFAULT_API_KEY = "c11eb0deb07d0ce8f8ee060a1e9e9f37";
	private static final String[] FACE_PERMS = new String[] { "publish_stream",
			"offline_access" };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (doma == null) {
			doma = DoMaService.getDoMaInstance(this);
		}

		// Check preferences
		if (!doma.validatePreferences(this)) {
			showPreferencesEditor();
		}

		// do UI stuff
		syncStateIcons = new Drawable[SyncState.length()];

		syncStateIcons[SyncState.Conflict.ordinal()] = getResources()
				.getDrawable(R.drawable.conflict);
		syncStateIcons[SyncState.LocallyNew.ordinal()] = getResources()
				.getDrawable(R.drawable.arrow_up);
		syncStateIcons[SyncState.LocallyDeleted.ordinal()] = getResources()
				.getDrawable(R.drawable.deleted);
		syncStateIcons[SyncState.LocallyModified.ordinal()] = getResources()
				.getDrawable(R.drawable.arrow_up);
		syncStateIcons[SyncState.RemotelyNew.ordinal()] = getResources()
				.getDrawable(R.drawable.arrow_down);
		syncStateIcons[SyncState.RemotelyDeleted.ordinal()] = getResources()
				.getDrawable(R.drawable.deleted);
		syncStateIcons[SyncState.RemotelyModified.ordinal()] = getResources()
				.getDrawable(R.drawable.arrow_down);
		syncStateIcons[SyncState.Synchronized.ordinal()] = getResources()
				.getDrawable(R.drawable.sync);

		musicIcon = getResources().getDrawable(R.drawable.music);
		imageIcon = getResources().getDrawable(drawable.ic_menu_camera);
		movieIcon = getResources().getDrawable(R.drawable.movie);
		documentIcon = getResources().getDrawable(R.drawable.documento_100x100);
		otherIcon = getResources().getDrawable(R.drawable.other_100x100);

		PreferenceManager.getDefaultSharedPreferences(this)
				.registerOnSharedPreferenceChangeListener(this);

		data = new ArrayList<Map<String, Object>>();

		SimpleAdapter adapter = new SimpleAdapter(this, data,
				R.layout.simple_list_icon_item_multiple_choice, new String[] {
						VIEW_FILE_ICON, VIEW_PATH, VIEW_FILENAME, VIEW_DATE,
						VIEW_STATE_ICON, VIEW_CHECKBOX }, new int[] {
						R.id.fileIcon, R.id.path, R.id.filename, R.id.date,
						R.id.stateIcon, R.id.checkbox });
		adapter.setViewBinder(this);
		setListAdapter(adapter);

		ListView listView = getListView();
		listView.setItemsCanFocus(false);
		listView.setClickable(true);
		listView.setDescendantFocusability(ListView.FOCUS_BLOCK_DESCENDANTS);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);

		// obtain MAC
		String mac = doma.getPreference(DoMa.PREF_KEY_MAC);
		if (mac == null || mac.length() == 0) {
			String MAC = doma.getMACAddress();
			if (MAC == null) {
				new AlertDialog.Builder(this).setTitle(R.string.mac_error)
						.setMessage(R.string.mac_failed).setIcon(
								R.drawable.warning).setNeutralButton(
								R.string.close, null).show();
			}
		}

		// Refresh sync state and data
		if (doma.validatePreferences(this)) {
			reload();
		}
	}

	/**
	 * Called when the user changes the ViewMode setting
	 */
	protected void viewModeChanged() {
		refresh();
	}

	/**
	 * Returns this explorer document type
	 * 
	 * @return
	 */
	protected abstract DocumentType getType();

	/**
	 * The list of SyncFiles of this explorer type
	 * 
	 * @return
	 */
	protected SyncFile[] getSyncFiles() {
		return DoMaService.getDoMaInstance().getSyncFilesByType(getType());
	}

	/**
	 * Deletes all syncfiles of this type from the database
	 * 
	 * @return
	 */
	protected int deleteSyncFiles() {
		return DoMaService.getDoMaInstance().deleteSyncFiles(getType());
	}

	/**
	 * Returns the image that will be displayed as the file icon
	 * 
	 * @param f
	 * @return
	 */
	private final Drawable getIcon(SyncFile f) {
		if (f == null) {
			// Log.e("private final Drawable getIcon(SyncFile f)","NULL SyncFile!!");
			return null;
		}
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inTempStorage = new byte[1024 * 16];
		opts.outHeight = 60;
		opts.outWidth = 60;
		Drawable icon = null;
		// orange images display a miniature

		switch (f.getDocumentType()) {
		case OTHER:
			icon = otherIcon;
			break;
		case MUSIC:
			icon = musicIcon;
			break;
		case DOCUMENT:
			icon = documentIcon;
			break;
		case PICTURE:
			// Try to build a thumb from the source image file, if unsuccessful
			// use a default icon
			try {
				if (f.getFile() != null && f.getFile().exists()) {
					long len = f.getFile().length();
					//Log.i("AbstractExplorer.getIcon", "Length: " + len);
					if (len > 16 * 1024) {
						opts.inSampleSize = 8;
					} else {
						opts.inSampleSize = 1;
					}
					FileInputStream is = new FileInputStream(f.getFile());
					final Drawable thumb = new BitmapDrawable(BitmapFactory
							.decodeStream(is, null, opts));					
					icon = thumb;
				} else if (f.getRemoteId() != null) {
					opts.inSampleSize = 1;
					InputStream in = DoMaService.getDoMaInstance()
							.getRemoteThumbnail(f.getRemoteId());
					final Drawable thumbServer = new BitmapDrawable(
							BitmapFactory.decodeStream(new FlushedInputStream(
									in), null, opts));					
					in.close();
					//Log.i("AbstractExplorer.getIcon()", "ThumbServer height: " + thumbServer.getIntrinsicHeight());
					//Log.i("AbstractExplorer.getIcon()", "ThumbServer width: " + thumbServer.getIntrinsicWidth());
					icon = thumbServer;					
				} else {
					icon = imageIcon;
				}
			} catch (Exception e) {
				Log.e(null, null, e);
				icon = imageIcon;
			}
			break;
		case MOVIE:
			icon = movieIcon;
			break;
		default:
			icon = otherIcon;
			break;
		}
		return icon;
	}

	/**
	 * Loads the data from the database and binds it with this ListActivity.
	 * Note that every tab will call this method when reloading its data,
	 * creating a new thread for each. This happens the first time it is loaded
	 * and when the user changes certain preferences. For a single tab this
	 * method might be called twice before it has finished, and therefore needs
	 * to be thread safe.
	 * 
	 * mHandler is the mechanism provided by Android to communicate this thread
	 * with its parent thread through messages.
	 * 
	 */
	protected synchronized void readData(Handler mHandler) {
		files = getSyncFiles();
		int n = files.length;
		// Log.i(this.getClass().getName() + ":readData()", ""+n);
		int i = 0;
		if (files != null && files.length > 0) {
			for (SyncFile f : files) {
				Map<String, Object> m = new HashMap<String, Object>();
				m.put(VIEW_FILE_ICON, getIcon(f));

				if (f.getFile() == null) {
					String rp = f.getRemotePathRelativeToUser();
					m.put(VIEW_PATH, rp);
				} else {
					m.put(VIEW_PATH, f.getLocalPath());
				}

				m.put(VIEW_FILENAME, f.getName());

				if (f.getFile() != null) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(f.getFile().lastModified());
					DateFormat format = DateFormat.getDateTimeInstance(
							DateFormat.MEDIUM, DateFormat.MEDIUM);
					m.put(VIEW_DATE, format.format(cal.getTime()));
				} else if (f.getRemoteModifiedDate() > 0) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(f.getRemoteModifiedDate());
					DateFormat format = DateFormat.getDateTimeInstance(
							DateFormat.MEDIUM, DateFormat.MEDIUM);
					m.put(VIEW_DATE, format.format(cal.getTime()));
				} else {
					m.put(VIEW_DATE, "n/a");
				}

				m.put(VIEW_STATE_ICON, syncStateIcons[f.getSyncState()
						.ordinal()]);
				m.put(VIEW_CHECKBOX, false);

				// this reference to the file will be used to retrieve the
				// selected files
				m.put("file", f);

				// progress notification
				Message msg = mHandler.obtainMessage();
				msg.arg1 = 1;
				msg.arg2 = i++ * 100 / n;
				msg.obj = m;
				mHandler.sendMessage(msg);
			}
		}
	}

	/**
	 * Assigns the given data to a View in the ListViews. This is called for
	 * each item in the list to fill its values for the icon, the filename, the
	 * path, the date, the sync state and the checkbox.
	 */
	@Override
	public boolean setViewValue(View view, Object data,
			String textRepresentation) {
		if (view instanceof ImageView) {
			((ImageView) view).setImageDrawable((Drawable) data);
		} else if (view instanceof CheckBox) {
			((CheckBox) view).setChecked(data != null ? (Boolean) data : false);
		} else if (view instanceof TextView) {
			((TextView) view).setText((CharSequence) data);
		} else {
			return false;
		}

		return true;
	}

	/**
	 * Checkbox controller. Toggles file selection when clicked
	 */
	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int n, long l) {
		Map<String, Object> m = data.get(n);
		Boolean b = (Boolean) m.get(VIEW_CHECKBOX);
		if (b == null)
			b = true;
		else
			b = !b;
		m.put(VIEW_CHECKBOX, b);
	}

	/**
	 * Creates the options menu.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.optionmenu, menu);
		return true;
	}

	/**
	 * Options menu event handler for processing the user selection.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.option_refresh:
			refresh();
			break;
		case R.id.option_settings:
			showPreferencesEditor();
			break;
		case R.id.option_quit:
			quit();
			break;
		case R.id.option_sync_selection:
			showDialog(SYNC_ALERT_DIALOG);
			break;
		case R.id.option_upload_face:
			uploadFace();
			break;
		case R.id.option_upload_twit:
			uploadTwit();
			break;
		}
		return true;
	}

	/**
	 * Returns an array with the SyncFiles of this type selected by the user
	 * 
	 * @return
	 */
	private SyncFile[] getSelectedFiles() {
		List<SyncFile> selected = new ArrayList<SyncFile>();
		Iterator<Map<String, Object>> it = data.iterator();
		while (it.hasNext()) {
			Map<String, Object> map = it.next();
			if ((Boolean) map.get(VIEW_CHECKBOX)) {
				selected.add((SyncFile) map.get("file"));
			}
		}
		return selected.toArray(new SyncFile[selected.size()]);
	}

	/**
	 * Returns an array with all the SyncFiles of this type
	 * 
	 * TODO Unused, it is meant for the "Sync all files of this Type" option
	 * that is not implemented so far.
	 * 
	 * @return all the files in this tab
	 */
	@SuppressWarnings("unused")
	private SyncFile[] getFiles() {
		List<SyncFile> selected = new ArrayList<SyncFile>();
		Iterator<Map<String, Object>> it = data.iterator();
		while (it.hasNext()) {
			Map<String, Object> map = it.next();
			selected.add((SyncFile) map.get("file"));
		}
		return selected.toArray(new SyncFile[selected.size()]);
	}

	/**
	 * Shows the preferences window.
	 */
	private void showPreferencesEditor() {
		Intent i = new Intent(this, PreferencesEditor.class);
		startActivityForResult(i, REQUEST_CODE_PREFERENCES);
	}

	private void quit() {
		doma.quit();
		this.finish();
		System.exit(0);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		String s;
		switch (id) {
		case LOADING_DIALOG:
			loadingDialog = new ProgressDialog(AbstractExplorer.this);
			loadingDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			loadingDialog.setIndeterminate(false);
			s = getResources().getString(R.string.loading_message);
			loadingDialog.setMessage(s + " - " + getType().name());
			return loadingDialog;
		case SYNCHRONIZING_DIALOG:
			synchronizingDialog = new ProgressDialog(AbstractExplorer.this);
			synchronizingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			synchronizingDialog.setIndeterminate(true);
			s = getResources().getString(R.string.sync_progress_title);
			synchronizingDialog.setMessage(s + " - " + getType().name());
			return synchronizingDialog;
		case SYNC_ALERT_DIALOG:
			alertDialog = new Dialog(AbstractExplorer.this);
			alertDialog.setContentView(R.layout.alertsync);
			alertDialog.setTitle(R.string.alert_sync_title);
			Button buttonOK = (Button) alertDialog
					.findViewById(R.id.alert_buttonOK);
			buttonOK.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// sync
					alertDialog.dismiss();
					String mac = doma.getPreference(DoMa.PREF_KEY_MAC);
					if (mac != null && mac.length() > 0) {
						if (doma.isConnectionEnabled()) {
							if (doma.isHostReachable()) {
//								if (doma.login()) {
//									doma.logout();
									synchronizeSelectedFiles();
							} else
								errorHostAccess();
						} else
							errorConnectionEnabled();
					} else
						errorMAC();
				}
			});
			Button buttonCancel = (Button) alertDialog
					.findViewById(R.id.alert_buttonCancel);
			buttonCancel.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					alertDialog.dismiss();
				}
			});
			return alertDialog;
		default:
			return null;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case LOADING_DIALOG:
			loadingDialog.setProgress(0);
			break;
		case SYNCHRONIZING_DIALOG:
			break;
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// THREADING //
	// /////////////////////////////////////////////////////////////////////////

	/**
	 * Loads all data from DB (depending on viewMode)
	 */
	protected synchronized void reload() {
		if (loadingThread != null && loadingThread.isAlive()) {
			return;
		}
		// showDialog(LOADING_DIALOG);
		// loadingDialog.setMessage(getString(R.string.loading_message));
		data.clear();
		loadingThread = new LoadingThread(handler);
		loadingThread.start();
	}

	/**
	 * Updates the DB with local and server data (depending on viewMode)
	 */
	protected synchronized void refresh() {
		if (refreshingThread != null && refreshingThread.isAlive()) {
			return;
		}
		showDialog(LOADING_DIALOG);
		loadingDialog.setMessage(getString(R.string.refreshing));
		data.clear();
		refreshingThread = new RefreshingThread(handler);
		refreshingThread.start();
	}

	/**
	 * Called when the folder setting for this type has been changed
	 */
	public synchronized void folderChanged() {
		if (changingFolderThread != null && changingFolderThread.isAlive()) {
			return;
		}
		showDialog(LOADING_DIALOG);
		loadingDialog.setMessage(getString(R.string.refreshing));
		data.clear();
		changingFolderThread = new ChangingFolderThread(handler);
		changingFolderThread.start();
	}

	/**
	 * Called when the user requests the synchronization of the selected files.
	 */
	public synchronized void synchronizeSelectedFiles() {
		if (synchronizingSelectedFilesThread != null
				&& synchronizingSelectedFilesThread.isAlive()) {
			return;
		}
		showDialog(SYNCHRONIZING_DIALOG);
		synchronizingDialog.setMessage(getString(R.string.sync_progress_title));
		synchronizingSelectedFilesThread = new SynchronizingSelectedFilesThread(
				handler);
		synchronizingSelectedFilesThread.start();
	}

	/**
	 * Called when the user requests the synchronization of all the files in
	 * this tab.
	 */
	public synchronized void synchronizeSelectedType() {
		if (synchronizingSelectedTypeThread != null
				&& synchronizingSelectedTypeThread.isAlive()) {
			return;
		}
		showDialog(SYNCHRONIZING_DIALOG);
		synchronizingDialog.setMessage(getString(R.string.sync_progress_title));
		synchronizingSelectedTypeThread = new SynchronizingSelectedTypeThread(
				handler);
		synchronizingSelectedTypeThread.start();
	}

	/**
	 * This Handler receives progress messages from other threads and updates
	 * the UI (data, progress bar, texts, etc)
	 */
	final Handler handler = new Handler() {
		@SuppressWarnings("unchecked")
		public void handleMessage(Message msg) {
			try {
				if (msg.arg1 == TASK_REFRESHING) {
					loadingDialog.setProgress(msg.arg2);
				} else if (msg.arg1 == TASK_LOADING) {
					data.add((Map<String, Object>) msg.obj);
					((BaseAdapter) getListAdapter()).notifyDataSetChanged();
					// loadingDialog.setProgress(msg.arg2);
				} else if (msg.arg1 == TASK_SYNCHRONIZING) {
					((BaseAdapter) getListAdapter()).notifyDataSetChanged();
				}
			} catch (Exception e) {
				Log.e("Loading", "Error", e);
			}
		}
	};

	/**
	 * This thread loads all the data for this tab.
	 */
	private class LoadingThread extends Thread {
		Handler handler;

		LoadingThread(Handler h) {
			handler = h;
		}

		public void run() {
			if (doma.validatePreferences(AbstractExplorer.this)) {
				readData(handler);
			}
			// loadingDialog.dismiss();
		}
	}

	/**
	 * This thread recalculates the sync state of all the files of this tab with
	 * the server and then reloads the data to refresh the UI.
	 */
	private class RefreshingThread extends Thread {
		Handler handler;

		RefreshingThread(Handler h) {
			handler = h;
		}

		public void run() {
			if (doma.validatePreferences(AbstractExplorer.this)) {
				DoMaService.getDoMaInstance().refreshState(
						AbstractExplorer.this, handler, getType());
				readData(handler);
			}
			loadingDialog.dismiss();
		}
	}

	/**
	 * When the user changes the root folder for this tab, this thread is called
	 * to delete from the database the old files of this tab, then calculates
	 * the state of the files of the new folder and reloads the UI.
	 */
	private class ChangingFolderThread extends Thread {
		Handler handler;

		ChangingFolderThread(Handler h) {
			handler = h;
		}

		public void run() {
			if (doma.validatePreferences(AbstractExplorer.this)) {
				deleteSyncFiles();
				DoMaService.getDoMaInstance().refreshState(
						AbstractExplorer.this, handler, getType());
				readData(handler);
			}
			loadingDialog.dismiss();
		}
	}

	/**
	 * Thread for synchronizing all the files of this tab's type.
	 */
	private class SynchronizingSelectedTypeThread extends Thread {
		Handler handler;

		SynchronizingSelectedTypeThread(Handler h) {
			handler = h;
		}

		public void run() {
			if (doma.validatePreferences(AbstractExplorer.this)) {
				DoMaService.getDoMaInstance().synchronizeSelectedType(
						AbstractExplorer.this, handler, getType());
				readData(handler);
			}
			synchronizingDialog.dismiss();
		}
	}

	/**
	 * Thread for synchronizing the selected files.
	 */
	private class SynchronizingSelectedFilesThread extends Thread {
		Handler handler;

		SynchronizingSelectedFilesThread(Handler h) {
			handler = h;
		}

		public void run() {
			if (doma.validatePreferences(AbstractExplorer.this)) {
				DoMaService.getDoMaInstance().synchronizeSelectedFiles(
						AbstractExplorer.this, handler, getSelectedFiles());
				data.clear();
				readData(handler);
			} else {
				Log.w("SynchronizingSelectedFilesThread",
						"Preferences not validated.");
				PreferenceManager.getDefaultSharedPreferences(
						AbstractExplorer.this).toString();
			}
			synchronizingDialog.dismiss();
		}
	}

	// ///////////////////////////////////////////////////////////////////////
	// ERROR DIALOGS //
	// ///////////////////////////////////////////////////////////////////////

	@SuppressWarnings("unused")
	private void errorLogin() {
		new AlertDialog.Builder(this).setTitle(R.string.login_error)
				.setMessage(R.string.login_failed).setIcon(R.drawable.warning)
				.setNeutralButton(R.string.close, null).show();
	}

	private void errorType() {
		new AlertDialog.Builder(this).setTitle(R.string.error).setMessage(
				R.string.error_type).setIcon(R.drawable.warning)
				.setNeutralButton(R.string.close, null).show();
	}

	private void errorMAC() {
		new AlertDialog.Builder(this).setTitle(R.string.mac_error).setMessage(
				R.string.mac_failed).setIcon(R.drawable.warning)
				.setNeutralButton(R.string.close, null).show();
	}

	private void errorPublishTwitter() {
		new AlertDialog.Builder(this).setTitle(R.string.twitter_error)
				.setMessage(R.string.publish_failed)
				.setIcon(R.drawable.warning).setNeutralButton(R.string.close,
						null).show();
	}

	private void errorConnectionEnabled() {
		new AlertDialog.Builder(this).setTitle(R.string.connection_error)
				.setMessage(R.string.connection_not_enabled).setIcon(
						R.drawable.warning).setNeutralButton(R.string.close,
						null).show();
	}

	private void errorHostAccess() {
		new AlertDialog.Builder(this).setTitle(R.string.connection_error)
				.setMessage(R.string.no_access_host)
				.setIcon(R.drawable.warning).setNeutralButton(R.string.close,
						null).show();
	}

	private void okPublishTwitter() {
		new AlertDialog.Builder(this).setTitle(R.string.twitter_ok).setMessage(
				R.string.publish_ok).setIcon(R.drawable.ok).setNeutralButton(
				R.string.close, null).show();
	}

	private void okPublishFacebook() {
		new AlertDialog.Builder(this).setTitle(R.string.facebook_ok)
				.setMessage(R.string.publish_ok).setIcon(R.drawable.ok)
				.setNeutralButton(R.string.close, null).show();
	}

	// ////////////////////
	// Publishing //
	// ////////////////////

	private void publishFacebook(SyncFile[] files) {
		boolean error = false;
		for (SyncFile file : files) {
			if (file.getDocumentType().equals(SyncFile.DocumentType.MOVIE)) {
				videoPublish(file);
			} else if (file.getDocumentType().equals(
					SyncFile.DocumentType.PICTURE)) {
				imagePublish(file);
			} else
				error = true;
		}
		if (error)
			errorType();
	}

	private void imagePublish(SyncFile file) {
		File photo = file.getFile();
		if (mFacebook.isSessionValid()) {
			Bundle params = new Bundle();
			params.putString("method", "photos.upload");
			byte[] imgData = FileUtil.getBytes(photo);
			params.putByteArray("picture", imgData);
			mAsyncRunner.request(null, params, "POST", new UploadListener(),
					file.getDocumentType());
			okPublishFacebook();
		}
	}

	private void videoPublish(SyncFile file) {
		// File video = file.getFile();
		if (mFacebook.isSessionValid()) {
			/*
			 * Bundle params = new Bundle(); params.putString("method",
			 * "facebook.video.upload"); params.putString("title",
			 * file.getFileName()); byte[] videoData = getBytes(video);
			 * params.putByteArray("video", videoData);
			 * mAsyncRunner.request(null, params, "POST", new UploadListener(),
			 * file.getDocumentType());
			 */
			new AlertDialog.Builder(this).setTitle(R.string.error).setMessage(
					R.string.not_available_face).setIcon(R.drawable.warning)
					.setNeutralButton(R.string.close, null).show();
		}
	}

	private final class LoginDialogListener implements DialogListener {
		public void onComplete(Bundle values) {
			SessionEvents.onLoginSuccess();
			// publish on facebook
			publishFacebook(getSelectedFiles());
			// sync files
			String mac = doma.getPreference(DoMa.PREF_KEY_MAC);
			if (mac != null && !mac.equals("")) {
				if (doma.isConnectionEnabled()) {
					if (doma.isHostReachable()) {
						//if (doma.login()) {
							//doma.logout();
							synchronizeSelectedFiles();							
						//} else
							//errorLogin();
					} else
						errorHostAccess();
				} else
					errorConnectionEnabled();
			} else
				errorMAC();
		}

		public void onFacebookError(FacebookError error) {
			SessionEvents.onLoginError(error.getMessage());
		}

		public void onError(DialogError error) {
			SessionEvents.onLoginError(error.getMessage());
		}

		public void onCancel() {
			SessionEvents.onLoginError("Action Canceled");
		}
	}

	private void uploadFace() {
		// upload to Nuxeo server & publish on facebook
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.alertfacebook);
		dialog.setTitle(R.string.alert_face_title);
		dialog.show();

		Button buttonOK = (Button) dialog.findViewById(R.id.alertface_buttonOK);
		buttonOK.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// sync
				dialog.dismiss();
				if (doma.isConnectionEnabled()) {
					if (doma.isHostReachable()) {
						// publish on facebook & upload to Nuxeo server
						mFacebook = new Facebook();
						mFacebook.authorize(AbstractExplorer.this,
								DEFAULT_API_KEY, FACE_PERMS,
								new LoginDialogListener());
						mAsyncRunner = new AsyncFacebookRunner(mFacebook);
					} else
						errorHostAccess();
				} else
					errorConnectionEnabled();
			}
		});
		Button buttonCancel = (Button) dialog
				.findViewById(R.id.alertface_buttonCancel);
		buttonCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
	}

	public class UploadListener extends BaseRequestListener {

		public void onComplete(final String response) {
			Log.i("MainScreenView.uploadListener()",
					"File published on facebook!");
		}
	}

	private void uploadTwit() {
		// upload to Nuxeo server & publish on twitter
		final Dialog dialog = new Dialog(this);
		dialog.setContentView(R.layout.alerttwitter);
		dialog.setTitle(R.string.alert_twit_title);
		dialog.show();

		Button buttonOK = (Button) dialog.findViewById(R.id.alerttwit_buttonOK);
		buttonOK.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// sync
				dialog.dismiss();
				// upload to Nuxeo server
				String mac = doma.getPreference(DoMa.PREF_KEY_MAC);
				if (mac != null && !mac.equals("")) {
					if (doma.isConnectionEnabled()) {
						if (doma.isHostReachable()) {
							//if (doma.login()) {
								//doma.logout();
								synchronizeSelectedFiles();
								publishTwitter(getSelectedFiles());
							//} else
								//errorLogin();
						} else
							errorHostAccess();
					} else
						errorConnectionEnabled();
				} else
					errorMAC();
			}
		});
		Button buttonCancel = (Button) dialog
				.findViewById(R.id.alerttwit_buttonCancel);
		buttonCancel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
	}

	private void publishTwitter(SyncFile[] files) {
		boolean error = false;
		for (SyncFile file : files) {
			if (file.getDocumentType().equals(SyncFile.DocumentType.PICTURE)) {
				boolean ok = doma.uploadTwitter(file);
				if (ok) {
					Log.i("MainScreenView.publishTwitter()",
							"File published on twitter " + file.getName());
					okPublishTwitter();
				} else {
					errorPublishTwitter();
				}
			} else
				error = true;
		}
		if (error)
			errorType();
	}

}