package com.opensistemas.nxdroid.logic.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.opensistemas.nxdroid.logic.SyncFile;
import com.opensistemas.nxdroid.logic.SyncFileComparator;
import com.opensistemas.nxdroid.logic.SyncFile.DocumentType;
import com.opensistemas.nxdroid.logic.SyncFile.SyncState;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Table to save local & remote {@link SyncFile}
 * 
 * {@link SyncState} If state_date < local_date => SyncState = Local If
 * state_date < server_date => SyncState = Server If state_date = local_date &
 * server_date => SyncState = Sync File modified in local -> update changes in
 * server File modified in remote -> update changes in local File deleted in
 * local -> delete file in server File deleted in remote -> delete file in local
 */
public class SyncFileTable implements BaseColumns {

	/** Table name */
	public static final String TABLE_NAME = "sync_files";

	// Table fields

	/** Synchronization state {@link SyncState} */
	public static final String SYNC_STATE = "sync_state";
	/** File path in the SD_CARD or device */
	public static final String LOCAL_PATH = "local_path";
	/** Remote document id */
	public static final String REMOTE_ID = "remote_id";
	public static final String REMOTE_NAME = "remote_name";
	public static final String REMOTE_PATH = "remote_path";
	/** DOCUMENT type {@link DocumentType} */
	public static final String DOCUMENT_TYPE = "document_type";
	/** Date and time in which the file's SyncState was last updated */
	public static final String SYNC_STATE_DATE = "sync_state_date";
	/**
	 * Last date and time in which the file has been modified locally. Compare
	 * this date ONLY with File.lastModified()
	 */
	public static final String LOCAL_MODIFICATION_DATE = "local_modification_date";
	/**
	 * Last date and time in which the file was modified in the remote system.
	 * Compare this date ONLY with the modified date reported by the server.
	 */
	public static final String REMOTE_MODIFICATION_DATE = "remote_modification_date";

	public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
			+ " (" + LOCAL_PATH + " TEXT, " + REMOTE_ID + " TEXT,"
			+ REMOTE_PATH + " TEXT," + REMOTE_NAME + " TEXT,"
			+ DOCUMENT_TYPE + " TEXT, " + SYNC_STATE + " TEXT, "
			+ SYNC_STATE_DATE + " INTEGER, " + LOCAL_MODIFICATION_DATE
			+ " INTEGER, " + REMOTE_MODIFICATION_DATE + " INTEGER);";

	private static String[] columns = { LOCAL_PATH, 
										REMOTE_ID, 
										REMOTE_PATH, 
										REMOTE_NAME, 
										DOCUMENT_TYPE,
										SYNC_STATE, 
										SYNC_STATE_DATE, 
										LOCAL_MODIFICATION_DATE,
										REMOTE_MODIFICATION_DATE };

	/**
	 * Checks whether the path passed as parameter has a corresponding SyncFile
	 * stored in the database
	 * 
	 * @param db
	 * @param path
	 * @return true if the object passed as parameter has a local path stored in
	 *         the database
	 */
	public static boolean existsByLocalPath(SQLiteDatabase db, String path) {
		boolean result = false;
		if (path != null) {
			String[] columns = { LOCAL_PATH };
			String[] params = new String[1];
			params[0] = path;
			Cursor cursor = db.query(TABLE_NAME, columns, LOCAL_PATH + "=?",
					params, null, null, null);
			result = cursor.moveToFirst();
			cursor.close();
		}
		return result;
	}

	/**
	 * Checks whether the object passed as parameter has a remote ID stored in
	 * the database
	 * 
	 * @param db
	 * @param o
	 * @return true if the object passed as parameter has a remote ID stored in
	 *         the database
	 */
	public static boolean existsByRemoteId(SQLiteDatabase db, SyncFile o) {
		boolean result = false;
		if (o.getRemoteId() != null) {
			String[] columns = { REMOTE_ID };
			String[] params = new String[1];
			params[0] = o.getRemoteId();
			Cursor cursor = db.query(TABLE_NAME, columns, REMOTE_ID + "=?",
					params, null, null, null);
			result = cursor.moveToFirst();
			cursor.close();
		}
		return result;
	}

	/**
	 * Inserts a new SyncFile in the database, and adds it to the cache
	 * 
	 * @param o
	 * @return new row _ID
	 */
	private static long insert(SQLiteDatabase db, SyncFile o) {
		ContentValues cv = buildContentValues(o);
		return db.insert(TABLE_NAME, null, cv);
	}

	/**
	 * Updates an existing SyncFile in the database
	 * 
	 * @param o
	 */
	private static int update(SQLiteDatabase db, SyncFile o) {
		ContentValues cv = buildContentValues(o);
		String[] params = new String[1];
		if (existsByLocalPath(db, o.getLocalPath())) {
			params[0] = o.getLocalPath();
			int result = db.update(TABLE_NAME, cv, LOCAL_PATH + "=?", params);
			//Log.i("update", "SyncFile updated with result = " + result);
			return result;
		} else if (existsByRemoteId(db, o)) {
			params[0] = o.getRemoteId();
			int result = db.update(TABLE_NAME, cv, REMOTE_ID + "=?", params);
			//Log.i("update", "SyncFile updated with result = " + result);
			return result;
		} else {
			Log.i("SyncFileTable.update", "SyncFile is new");
			return 0;
		}
	}

	/**
	 * Use this method to create a ContentValues object from a SyncFile and
	 * avoid redundancy.
	 * 
	 * @param o
	 * @return
	 */
	private static ContentValues buildContentValues(SyncFile o) {
		ContentValues cv = new ContentValues();		
		cv.put(LOCAL_PATH, o.getLocalPath());
		cv.put(REMOTE_ID, o.getRemoteId());
		cv.put(REMOTE_PATH, o.getRemotePath());
		cv.put(REMOTE_NAME, o.getRemoteName());
		cv.put(DOCUMENT_TYPE, o.getDocumentType().name());
		cv.put(SYNC_STATE, o.getSyncState().name());
		cv.put(SYNC_STATE_DATE, o.getSyncStateDate());
		cv.put(LOCAL_MODIFICATION_DATE, o.getLocalModifiedDate());
		cv.put(REMOTE_MODIFICATION_DATE, o.getRemoteModifiedDate());
		return cv;
	}

	/**
	 * Given the file local path, this method creates a new instance of SyncFile
	 * and loads its data from the database.
	 * 
	 * @param db
	 * @param localPath
	 * @return SyncFile
	 */
	public static SyncFile getSyncFileByLocalPath(SQLiteDatabase db,
			String localPath) {
		if (localPath == null) {
			return null;
		}
		SyncFile o = null;
		String[] params = new String[1];
		params[0] = localPath;
		Cursor cursor = db.query(TABLE_NAME, columns, LOCAL_PATH + "=?",
				params, null, null, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				o = buildSyncFile(cursor);
			}
			cursor.close();
		}
		return o;
	}

	/**
	 * Loads a SyncFile from the database using its remote id
	 * 
	 * @param db
	 * @param remoteId
	 * @return
	 */
	public static SyncFile getSyncFileByRemoteId(SQLiteDatabase db,
			String remoteId) {
		if (remoteId == null)
			return null;
		SyncFile o = null;
		String[] params = new String[1];
		params[0] = remoteId;
		Cursor cursor = db.query(TABLE_NAME, null, REMOTE_ID + "=?", params,
				null, null, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				o = buildSyncFile(cursor);
			}
			cursor.close();
		}
		return o;
	}

	/**
	 * Maps all columns from the cursor to a new SyncFile. Returns the new
	 * SyncFile. The cursor must have been created using the global columns
	 * array to respect the column order.
	 * 
	 * Used internally in queries.
	 * 
	 * @param c
	 * @return SyncFile
	 */
	private static SyncFile buildSyncFile(Cursor cursor) {
		int i = 0;
		String path = cursor.getString(i++);
		String remoteId = cursor.getString(i++);
		String remotePath = cursor.getString(i++);
		String remoteName = cursor.getString(i++);
		String documentType = cursor.getString(i++);
		String syncState = cursor.getString(i++);
		long syncStateDate = cursor.getLong(i++);
		long localModifiedDate = cursor.getLong(i++);
		long remoteModifiedDate = cursor.getLong(i++);
		SyncFile sf = new SyncFile(path, remoteId, remotePath, remoteName, DocumentType
				.valueOf(documentType), SyncState.valueOf(syncState),
				syncStateDate, localModifiedDate, remoteModifiedDate);
		return sf;
	}

	/**
	 * Inserts or updates (if it already exists) the object in the database.
	 * 
	 * @param o
	 *            object to save
	 */
	public static void save(SQLiteDatabase db, SyncFile o) {
		db.beginTransaction();
		try {
			if (update(db, o) == 0) {
				long n = insert(db, o);
				if (n > -1) {
					db.setTransactionSuccessful();
					//Log.i("save", "SyncFile saved. " + o.getLocalPath());
				} else {
					//Log.i("save", "SyncFile NOT saved. " + o.getLocalPath());
				}
			} else {
				db.setTransactionSuccessful();
			}
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * Deletes a SyncFile from the database
	 * 
	 * @param db
	 * @param o
	 * @return
	 */
	public static int delete(SQLiteDatabase db, SyncFile o) {
		db.beginTransaction();
		int n = 0;
		try {
			if (o.getLocalPath() != null) {
				String[] params = new String[1];
				params[0] = o.getLocalPath();
				n = db.delete(TABLE_NAME, LOCAL_PATH + "=?", params);
			}
			if (o.getRemoteId() != null) {
				String[] params = new String[1];
				params[0] = o.getRemoteId();
				n += db.delete(TABLE_NAME, REMOTE_ID + "=?", params);
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		return n;
	}
	
	/**
	 * Deletes from the database all the SyncFiles of the given type
	 * @param db
	 * @param type
	 * @return
	 */
	public static int delete(SQLiteDatabase db, SyncFile.DocumentType type) {
		db.beginTransaction();
		int n = 0;
		try {
			String[] params = new String[1];
			params[0] = type.name();
			n = db.delete(TABLE_NAME, DOCUMENT_TYPE + "=?", params);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		return n;
	}

	/**
	 * Deletes inconsistent records from the database
	 * @param db
	 * @return number of records deleted
	 */
	public static int deleteInconsistencies(SQLiteDatabase db) {
		db.beginTransaction();
		int n = 0;
		try {
			n = db.delete(TABLE_NAME, LOCAL_PATH + " IS null AND " + REMOTE_ID + " IS null", new String[0]);
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
		return n;		
	}	
	
	/**
	 * Returns all SyncFiles of a given type
	 * 
	 * @param db
	 * @param type
	 * @return
	 */
	public static SyncFile[] getSyncFilesByType(SQLiteDatabase db,
			SyncFile.DocumentType type, boolean server) {
		List<SyncFile> list = new ArrayList<SyncFile>();
		String[] params = new String[1];
		params[0] = type.name();
		
		String whereClause = DOCUMENT_TYPE + "=?";
		if (server) {
			whereClause.concat(" and " + LOCAL_PATH + " is not null");
		} 
		
		Cursor cursor = db.query(TABLE_NAME, columns, whereClause,
				params, null, null, null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					SyncFile o = buildSyncFile(cursor);
					list.add(o);
					//Log.i(SyncFileTable.class.getName(), "getSyncFilesByType() " + o.toString());
					
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		Collections.sort(list, new SyncFileComparator());
		return list.toArray(new SyncFile[list.size()]);
	}

	
	
	/**
	 * Returns all SyncFiles
	 * 
	 * @param db
	 * @param type
	 * @return
	 */
	public static SyncFile[] getAll(SQLiteDatabase db) {
		List<SyncFile> list = new ArrayList<SyncFile>();
		SyncFile o = null;
		Cursor cursor = db.query(TABLE_NAME, columns, null, null, null, null,
				null);
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				do {
					o = buildSyncFile(cursor);
					list.add(o);
				} while (cursor.moveToNext());
			}
			cursor.close();
		}
		return list.toArray(new SyncFile[list.size()]);
	}
}
