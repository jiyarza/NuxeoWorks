package com.opensistemas.nxdroid.logic.db;

import com.opensistemas.nxdroid.logic.SyncFile;
import com.opensistemas.nxdroid.logic.SyncFile.DocumentType;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter {


	private static final String DATABASE_NAME = "DomaDB";
	private static final int DATABASE_VERSION = 1;	
	private final Context context;
	private DatabaseHelper dbHelper;
	private SQLiteDatabase db;

	public DBAdapter(Context ctx){
		this.context = ctx;
		dbHelper = new DatabaseHelper(context);
		db = dbHelper.getWritableDatabase();
		if (!existsDatabase(db)) {			
			dbHelper.onCreate(db);		
		}
		if (db.isOpen()) {
			db.close();
		}
	}

	private boolean existsDatabase(SQLiteDatabase db) {
		try {
			if (db == null) return false;
			db = dbHelper.getWritableDatabase();
			db.rawQuery("SELECT count(*) from " + SyncFileTable.TABLE_NAME, null);
			return true;
		} catch (Throwable t) {
			Log.d("existDatabase", "It seems the database does not exist", t);
			return false;
		}
	}
	
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {			
			db.execSQL(SyncFileTable.CREATE_TABLE);
			Log.i("DBAdapter.onCreate()", "DB created!");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w("FileOpenHelper.onUpgrade()", "Upgrading database from version " + oldVersion 
					+ " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + SyncFileTable.TABLE_NAME);
			onCreate(db);
		}
	}

	/**
	 * Open the database to write
	 * @return db object
	 * @throws SQLException
	 */
	private DBAdapter open() throws SQLException {
		db = dbHelper.getWritableDatabase();
		return this;
	}

	/**
	 * closes the database   
	 */
	private void close() {
		db.close();
	}

	/**
	 * Deletes from the database all occurrences of the given SyncFile
	 * 
	 * @param o
	 * @return number of rows deleted
	 */
	public int delete(SyncFile o) {
		open();
		int result = SyncFileTable.delete(db, o);
		close();
		return result;
	}

	/**
	 * Deletes from the database all the SyncFiles of the given type
	 * 
	 * @param type
	 * @return
	 */	
	public int delete(DocumentType type) {
		open();
		int result = SyncFileTable.delete(db, type);
		close();
		return result;
	}
	
	/**
	 * Deletes inconsistent records from the database
	 * @param db
	 * @return number of records deleted
	 */
	public int deleteInconsistencies() {
		open();
		int result = SyncFileTable.deleteInconsistencies(db);
		close();
		return result;
	}
				
	/**
	 * Checks whether the object passed as parameter exists in the database 
	 * @param o
	 * @return true if the object exists in the database, false otherwise
	 */
	public boolean exists(SyncFile o) {
		open();
		boolean result = ( SyncFileTable.existsByLocalPath(db, o.getLocalPath())
				|| SyncFileTable.existsByRemoteId(db, o));
		close();
		return result;
	}
			
	/**
	 * Inserts or updates (if it already exists) the object in the database.
	 * @param o
	 */	
	public void save(SyncFile o) {
		open();
		SyncFileTable.save(db, o);
		close();
	}
	
	/**
	 * Given the file local path, this method creates a new instance of SyncFile and loads 
	 * its data from the database.
	 * @param localPath
	 * @return SyncFile
	 */
	public SyncFile getSyncFileByLocalPath(String localPath) {
		open();
		SyncFile result = SyncFileTable.getSyncFileByLocalPath(db, localPath);
		close();
		return result;
	}
	
	/**
	 * Given the remote id, this method creates a new instance of SyncFile and loads 
	 * its data from the database.
	 * @param SyncFile
	 * @return SyncFile
	 */
	public SyncFile getSyncFileByRemoteId(String id) {
		open();
		SyncFile result = SyncFileTable.getSyncFileByRemoteId(db, id);
		close();
		return result;
	}
	
	public synchronized SyncFile[] getAll() {
		open();
		SyncFile[] result = SyncFileTable.getAll(db);
		close();
		return result;
	}
	
	public synchronized SyncFile[] getSyncFilesByType(DocumentType type, boolean server) {
		open();
		SyncFile[] files = SyncFileTable.getSyncFilesByType(db, type, server);
		close();
		Log.i("getSyncFilesByType", type.name() + "= " +files.length);
		return files;
	}
}