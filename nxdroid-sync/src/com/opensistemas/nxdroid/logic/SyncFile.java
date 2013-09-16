package com.opensistemas.nxdroid.logic;

import java.io.File;

import android.util.Log;

public class SyncFile {

	public enum DocumentType {
		DOCUMENT, PICTURE, MUSIC, MOVIE, OTHER;

		public static int length() {
			return 5;
		}
	}

	/**
	 * These events are the conditions that produce transitions in the SyncFile
	 * state machine.
	 * 
	 */
	public static enum SyncEvent {
		/** A SyncFile has been uploaded to the server */
		Uploaded,
		/** A SyncFile has been downloaded from the server */
		Downloaded,
		/** A SyncFile has been deleted locally */
		LocallyDeleted,
		/** A SyncFile has been deleted remotely */
		RemotelyDeleted,
		/** A SyncFile has been locally modified */
		LocallyModified,
		/** A SyncFile has been remotely modified */
		RemotelyModified
	}

	public static enum SyncState {
		/** Locally new SyncFile */
		LocallyNew,
		/** Remotely new SyncFile */
		RemotelyNew,
		/** SyncFile is synchronized */
		Synchronized,
		/** SyncFile has been modified in local -> update changes in server */
		LocallyModified,
		/** SyncFile modified in remote -> update changes in local */
		RemotelyModified,
		/** File deleted in local -> delete file in server */
		LocallyDeleted,
		/** File deleted in remote -> delete file in local */
		RemotelyDeleted,
		/** Modified locally and remotely -> perform user preferred sync action */
		Conflict,
		/**
		 * Has been deleted locally and remotely. The SyncFile is going to be
		 * deleted from DB immediately.
		 */
		Deleted;
		
		public static int length() {
			return 8;
		}
	}

	/** Local path, this serves as the local identifier */
	private String localPath;
	/** DOCUMENT ID in the Server */
	private String remoteId;
	/** Path in the server */
	private String remotePath;
	/** DOCUMENT name in the Server */
	private String remoteName;
	/** DOCUMENT type in both Server and Local */
	private DocumentType documentType;
	/** Synchronization state */
	private SyncState syncState;;
	/** Last sync state change date (milliseconds since 1970) */
	private long syncStateDate;
	/** Last locally modified date */
	private long localModifiedDate;
	/** Last remotely modified date */
	private long remoteModifiedDate;

	
	public SyncFile(String remoteId, String remotePath, String remoteName, DocumentType type, long remoteModifiedDate) {
		this.remoteId = remoteId;
		this.remotePath = remotePath;
		this.remoteName = remoteName;
		this.documentType = type;		
		this.syncState = SyncState.RemotelyNew;
		this.syncStateDate = System.currentTimeMillis();
		this.remoteModifiedDate = remoteModifiedDate;
	}

	public SyncFile(String localPath) {
		this.localPath = localPath;
		File file = new File(localPath);
		if (file != null && file.exists()) {
			this.documentType = getDocumentTypeByFilename(file.getName());
			this.localModifiedDate = file.lastModified();
			this.remoteId = null;
			this.remotePath = null;
			this.remoteName = null;			
			this.remoteModifiedDate = 0;
			this.syncState = SyncState.LocallyNew;
			this.syncStateDate = System.currentTimeMillis();
		}
	}

	public SyncFile(String localPath, String remoteId, String remotePath, String remoteName,
			DocumentType documentType, SyncState syncState, long syncStateDate,
			long localModifiedDate, long remoteModifiedDate) {
		this.localPath = localPath;
		this.remoteId = remoteId;
		this.remotePath = remotePath;
		this.remoteName = remoteName;
		this.documentType = documentType;
		this.syncState = syncState;
		this.syncStateDate = syncStateDate;
		this.localModifiedDate = localModifiedDate;
		this.remoteModifiedDate = remoteModifiedDate;
	}

	/**
	 * Calculates the {@link DocumentType} of this SyncFile, based upon its
	 * local or remote names. Default type is DOCUMENT.
	 * 
	 * @return this SyncFile {@link DocumentType}
	 * @deprecated
	 */
	private DocumentType calculateDocumentType() {
		File file = getFile();
		if (file == null && remoteName == null)
			return null;
		if (file != null) {
			if (file.isDirectory())
				return DocumentType.OTHER;
			// check filename's extension
			String filename = file.getName();
			DocumentType dt = getDocumentTypeByFilename(filename);
			if (dt != null)
				return dt;
		}
		// if it is a remote document, try to guess from remote document name
		if (remoteName != null) {
			DocumentType dt = getDocumentTypeByFilename(remoteName);
			if (dt != null)
				return dt;
		}
		return DocumentType.DOCUMENT;
	}

	/**
	 * Calculates a DocumentType from the extension of the given filename. If
	 * the extension does not match any type, returns null.
	 * @deprecated
	 * @param filename
	 * @return
	 */
	private DocumentType getDocumentTypeByFilename(String filename) {
		String ext = FileUtil.getExtension(filename);
		
		if (ext == null) {
			return DocumentType.OTHER;
		}
		ext = ext.toLowerCase();
		if (ext.equals(".odt") || ext.equals(".doc") || ext.equals(".pdf") || ext.equals(".ppt") || ext.equals(".xls")) {
			return DocumentType.DOCUMENT;
		}
		if (ext.equals(".mp3")) {
			return DocumentType.MUSIC;
		}
		if (ext.equals(".jpg") || ext.equals(".png") || ext.equals(".gif")) {
			return DocumentType.PICTURE;
		}
		if (ext.equals(".mp4") || ext.equals(".flv") || ext.equals(".f4v")
				|| ext.equals(".3gp") || ext.equals(".3g2")) {
			return DocumentType.MOVIE;
		}
		return DocumentType.OTHER;		
	}

	public DocumentType getDocumentType() {
		if (documentType == null)
			documentType = calculateDocumentType();
		return documentType;
	}

	public String getFileName() {
		if (localPath == null) {
			return null;
		}
		File file = new File(localPath);
		if (file.exists()) {
			return file.getName();
		} else {
			return null;
		}
	}

	/**
	 * Convenience method to get a name usable for the UI. Do not use in logic
	 * rules.
	 * 
	 * @return The filename, or the remote document title if the former is null.
	 */
	public String getName() {
		String name = getFileName();
		if (name == null) {
			if (remoteName == null) {
				return "???";
			} else {
				return remoteName;
			}
		} else {
			return name;
		}
	}

	/**
	 * Returns the File only if it exists locally, otherwise returns null
	 * 
	 * @return
	 */
	public File getFile() {
		if (localPath == null) {
			return null;
		}
		File f = new File(localPath);
		if (f != null && f.exists()) {
			return f;
		}
		return null;
	}

	public String getRemoteName() {
		return remoteName;
	}

	public String getRemoteId() {
		return remoteId;
	}

	public SyncState getSyncState() {
		return syncState;
	}

	public long getSyncStateDate() {
		return syncStateDate;
	}

	public void setDocumentType(DocumentType type) {
		this.documentType = type;
	}

	private void setSyncState(SyncState syncState) {
		if (this.syncState != syncState) {
			this.syncState = syncState;
			this.syncStateDate = System.currentTimeMillis();
		}
	}

	public void setSyncStateDate(long syncStateDate) {
		this.syncStateDate = syncStateDate;
	}

	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
	}

	public void setRemoteName(String remoteName) {
		this.remoteName = remoteName;
	}

	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public long getLocalModifiedDate() {
		return localModifiedDate;
	}

	public void setLocalModifiedDate(long lastLocalModifiedDate) {
		this.localModifiedDate = lastLocalModifiedDate;
	}

	public long getRemoteModifiedDate() {
		return remoteModifiedDate;
	}

	public void setRemoteModifiedDate(long remoteModifiedDate) {
		this.remoteModifiedDate = remoteModifiedDate;
	}

	public SyncState updateSyncState(SyncEvent event) {
		Log.d("SyncFile.updateSyncState", "Event: " + event.name());
		Log.d("SyncFile.updateSyncState", "SyncState0: " + syncState.name());
		switch (event) {
		case Downloaded:
			if (syncState.equals(SyncState.RemotelyNew)
					|| syncState.equals(SyncState.RemotelyModified)
					|| syncState.equals(SyncState.Conflict)
					|| syncState.equals(SyncState.LocallyDeleted)) {
				setSyncState(SyncState.Synchronized);
			}
			break;
		case LocallyDeleted:
			if (syncState.equals(SyncState.Synchronized)) {
				setSyncState(SyncState.RemotelyNew);
			} else if (syncState.equals(SyncState.LocallyModified)) {
				setSyncState(SyncState.RemotelyNew);				
			} else if (syncState.equals(SyncState.RemotelyModified)) {
				setSyncState(SyncState.RemotelyNew);
			} else if (syncState.equals(SyncState.RemotelyDeleted)) {
				setSyncState(SyncState.Deleted);
			} else if (syncState.equals(SyncState.LocallyNew)) {
				setSyncState(SyncState.Deleted);
			}
			break;
		case LocallyModified:
			if (syncState.equals(SyncState.Synchronized)) {
				setSyncState(SyncState.LocallyModified);
			} else if (syncState.equals(SyncState.RemotelyModified)) {
				setSyncState(SyncState.Conflict);
			}
			break;
		case RemotelyDeleted:
			if (syncState.equals(SyncState.Synchronized)) {
				setSyncState(SyncState.LocallyNew);
			} else if (syncState.equals(SyncState.LocallyModified)) {
				setSyncState(SyncState.LocallyNew);
			} else if (syncState.equals(SyncState.RemotelyModified)) {
				setSyncState(SyncState.LocallyNew);
			} else if (syncState.equals(SyncState.LocallyDeleted)) {
				setSyncState(SyncState.Deleted);
			} else if (syncState.equals(SyncState.RemotelyNew)) {
				setSyncState(SyncState.Deleted);
			}
			break;
		case RemotelyModified:
			if (syncState.equals(SyncState.Synchronized)) {
				setSyncState(SyncState.RemotelyModified);
			} else if (syncState.equals(SyncState.LocallyModified)) {
				setSyncState(SyncState.Conflict);
			}
			break;
		case Uploaded:
			if (syncState.equals(SyncState.LocallyNew)
					|| syncState.equals(SyncState.LocallyModified)
					|| syncState.equals(SyncState.Conflict)
					|| syncState.equals(SyncState.RemotelyDeleted)) {
				setSyncState(SyncState.Synchronized);
			}
			break;
		}
		Log.d("SyncFile.updateSyncState", "SyncState1: " + syncState.name());
		return syncState;
	}

	public String getRemotePath() {
		return remotePath;
	}

	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}
	
	public String getRemotePathRelativeToType() {
		// Need to remove the first 4 path segments '/'
		if (remotePath != null) {
			Log.i("SyncFile.getRemotePahtRelativeToType()", "Remote path: " + remotePath);
			String result = remotePath.replaceFirst("/[^/]+/[^/]+/[^/]+/[^/]+/", "/");
			return result;
		} else {
			return "?";
		}
	}
	
	public String getRemotePathRelativeToUser() {
		// Need to remove the first 3 path segments '/'		
		if (remotePath != null) {
			String result = remotePath.replaceFirst("/[^/]+/[^/]+/[^/]+/", "/");
			return result;
		} else {
			return "?";
		}
		
	}

	public String getLocalPathRelativeToType() {		
		if (localPath != null) {
			Log.i("getLocalPathRelativeToType", "localPath="+localPath);			
			String basePath = DoMaService.getDoMaInstance().getBasePath(this.documentType);
			Log.i("getLocalPathRelativeToType", "basePath="+basePath);
			// now remove the base path from the localPath, the resulting string is what we need			
			String result = localPath.replaceFirst(basePath, "");
			return result;
		} else {
			return "?";
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SyncFile [documentType=").append(documentType).append(
				", localModifiedDate=").append(localModifiedDate).append(
				", localPath=").append(localPath).append(", remoteId=").append(
				remoteId).append(", remoteModifiedDate=").append(
				remoteModifiedDate).append(", remoteName=").append(remoteName)
				.append(", remotePath=").append(remotePath).append(
						", syncState=").append(syncState).append(
						", syncStateDate=").append(syncStateDate).append("]");
		return builder.toString();
	}
	
	
}
