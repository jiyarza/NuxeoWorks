package com.opensistemas.nxdroid.logic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;

import com.opensistemas.nxdroid.logic.SyncFile.DocumentType;
import com.opensistemas.nxdroid.logic.SyncFile.SyncEvent;
import com.opensistemas.nxdroid.logic.ws.Base64;
import com.opensistemas.nxdroid.logic.ws.SoapManager;

import android.content.Context;
import android.util.Log;

public class SyncManager {
	/** Web service URL */
	// private static final String URL =
	// "http://doma.ilabspain.com/nuxeo/webservices/domasync?wsdl";
	// private static final String URL =
	// "http://192.168.1.128:8080/nuxeo/webservices/domasync?wsdl";

	private SoapObject userWorkspace;

	/**
	 * These are the Nuxeo document types defined in the server for each type of
	 * content. In the server there is not a definition for Documents or Others.
	 */
	public enum ServerDocumentType {
		OrangeFile, OrangeImage, OrangeAudio, OrangeVideo
	}

	/**
	 * These are the Nuxeo document types defined in the server for the root
	 * folders. There is on for each type of content.
	 */
	public enum ServerTypeRoot {
		DocumentRootFolder, VideoRootFolder, OrangeImageGallery, MusicRootFolder, OtherRootFolder;

		private SoapObject document;

		protected void setDocument(SoapObject so) {
			document = so;
		}

		protected SoapObject getDocument() {
			return document;
		}

	}

	/** Date format in the SOAP envelope */
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	/** singleton instance */
	private static SyncManager instance;

	/**
	 * singleton's private constructor
	 */
	private SyncManager() {
	}

	/**
	 * creates and returns the unique instance
	 * 
	 * @return
	 */
	public static SyncManager getInstance() {
		if (instance == null)
			instance = new SyncManager();
		return instance;
	}

	/**
	 * Returns an array of the SoapObjects of a given document type for whom the
	 * *synchronized* flag is set to true in the server.
	 * 
	 * @param context
	 * @param type
	 * @return
	 */
	public SoapObject[] getDocumentsByType(Context context, DocumentType type) {
		Log.d("**** SyncManager.getDocumentsByType ****","ENTER");
		List<SoapObject> soapObjects = new ArrayList<SoapObject>();
		// Get the server side root folder for this type of document
		String serverTypeName = getServerType(type).name();
		SoapObject rootFolder = getRootFolder(type);
		if (rootFolder == null) {
			Log.w("getSoapObjects", serverTypeName
					.concat(" not found in the server."));
			Log.d("**** SyncManager.getDocumentsByType ****","EXIT");
			return soapObjects.toArray(new SoapObject[soapObjects.size()]);
		}
		// Proceed only if the rootFolder exists
		String rootFolderPath = rootFolder.getProperty("path").toString();
		try {
			SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_GET_SYNCHRONIZED_DOCUMENTS);
			SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE.concat(SoapManager.WS_GET_SYNCHRONIZED_DOCUMENTS));
			if (response != null) {
				int size = response.getPropertyCount();
				for (int i = 0; i < size; i++) {
					SoapObject o = (SoapObject) response.getProperty(i);

					String wsType = o.getProperty("type").toString();
					String wsPath = o.getProperty("path").toString();

					// Must be of the required type and also in the correct
					// server folder
					if (wsType.equals(serverTypeName)
							&& wsPath.startsWith(rootFolderPath)) {
						soapObjects.add(o);
						//Log.i("SyncManager.getDocumentsByType()", "Path: " + o.getProperty("path").toString() + " / Type: " + o.getProperty("type").toString());
					}
				}
			}
		} catch (Exception e) {
			Log.e("SyncManager.getSoapObjects()", "Error: ", e);
		}
		Log.d("**** SyncManager.getDocumentsByType ****","EXIT");
		return soapObjects.toArray(new SoapObject[soapObjects.size()]);
	}

	/**
	 * Returns the server side root folder for a given DocumentType, as a
	 * SoapObject.
	 * 
	 * @param type
	 * @return
	 */
	public SoapObject getRootFolder(DocumentType type) {
		Log.d("**** SyncManager.getRootFolder ****","ENTER");
		ServerTypeRoot rootType = getServerTypeRoot(type);
		SoapObject rootFolder = rootType.getDocument();
		SoapObject usrWorkspc = getUserWorkspace();
		if (rootFolder == null) {				
			SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_SEARCH);
			SoapObject prop = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_SEARCH);
			prop.addProperty("parentUUID", usrWorkspc.getProperty("id")
					.toString());
			prop.addProperty("type", getServerTypeRoot(type).name());
			request.addProperty("search", prop);
			SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE.concat(SoapManager.WS_SEARCH));
			SoapObject result = null;
			if (response != null && response.getPropertyCount() > 0) {
				result = (SoapObject) response.getProperty(0);
			}
			rootType.setDocument(result);
		}
		Log.d("**** SyncManager.getRootFolder ****","EXIT");
		return rootType.getDocument();
	}

	/**
	 * Given the DocumentType, returns the corresponding type of the root folder
	 * in the server
	 * 
	 * @param type
	 * @return
	 */
	public ServerTypeRoot getServerTypeRoot(DocumentType type) {
		switch (type) {
		case DOCUMENT:
			return ServerTypeRoot.DocumentRootFolder;
		case MOVIE:
			return ServerTypeRoot.VideoRootFolder;
		case MUSIC:
			return ServerTypeRoot.MusicRootFolder;
		case OTHER:
			return ServerTypeRoot.OtherRootFolder;
		case PICTURE:
			return ServerTypeRoot.OrangeImageGallery;
		default:
			return null;
		}
	}

	/**
	 * Builds a SyncFile from a SoapObject.
	 * 
	 * @param f
	 * @param type
	 * @return
	 */
	public SyncFile buildSyncFile(SoapObject f, DocumentType type) {
		String remoteId = f.getProperty("id").toString();
		String remoteName = f.getProperty("title").toString();
		String remotePath = f.getProperty("path").toString();
		return new SyncFile(remoteId, remotePath, remoteName, type,
				getRemoteModifiedDate(f));
	}

	public long getRemoteModifiedDate(SoapObject f) {
		String dateModify = f.getProperty("dateModify").toString();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date date = null;
		try {
			date = df.parse(dateModify);
		} catch (ParseException e) {
			Log.e("FileManager.updateDBWithServer()", "Date format incorrect "
					+ date);
		}
		long modDate = 0;
		if (date != null) {
			modDate = date.getTime();
		}
		return modDate;
	}

	/**
	 * Checks whether a SyncFile exists in the server
	 * 
	 * @param f
	 * @return
	 */
	public boolean exists(SyncFile f) {
		boolean result = (getDocumentById(f.getRemoteId()) != null);
		return result;
	}

	public ServerDocumentType getServerType(DocumentType type) {
		switch (type) {
		case DOCUMENT:
			return ServerDocumentType.OrangeFile;
		case MOVIE:
			return ServerDocumentType.OrangeVideo;
		case OTHER:
			return ServerDocumentType.OrangeFile;
		case MUSIC:
			return ServerDocumentType.OrangeAudio;
		case PICTURE:
			return ServerDocumentType.OrangeImage;
		default:
			return ServerDocumentType.OrangeFile;
		}
	}

	/**
	 * Parses a WS date string into a long
	 * 
	 * @param s
	 * @return
	 */
	public long getDateFromString(String s) {
		long date = 0;
		try {
			date = dateFormat.parse(s).getTime();
		} catch (ParseException e) {
			Log.e("FileManager.getDateFromString()",
					"Parse exception with date " + date);
		}
		return date;
	}

	
	

	/**
	 * Method to upload a file that already exists in the server
	 * 
	 * @param sf
	 * @param parentId
	 *            cannot be null
	 * @return SoapObject the updated document
	 */
	public SoapObject updateDocument(SyncFile sf, SoapObject parent) {
		Log.d("**** SyncManager.updateDocument ****","ENTER");
		SoapObject res = null;		
		SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_SYNC);
		// documentBlob properties -> blob, name
		String blobObject = Base64.encodeObject(sf.getFile());
		SoapObject blob = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_SYNC);
		blob.addProperty("blob", blobObject);
		blob.addProperty("name", sf.getFile().getName());
		// domaDocumentDescriptor properties -> dateModify, title
		SoapObject descriptor = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_SYNC);
		descriptor.addProperty("id", sf.getRemoteId());
		descriptor.addProperty("title", sf.getName());
		// syncDocument properties -> documentBlob, parentUuid, sync
		SoapObject syncDoc = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_SYNC);
		syncDoc.addProperty("documentBlob", blob);
		syncDoc.addProperty("domaDocumentDescriptor", descriptor);

		String parentId = null;
		if (parent != null) {
			parentId = parent.getProperty("id").toString();
		}
		syncDoc.addProperty("parentUuid", parentId);
		syncDoc.addProperty("sync", true);
		request.addProperty("syncDocument", syncDoc);
		// synchronize the file
		SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE.concat(SoapManager.WS_SYNC));
		if (response != null) {
			int returns = response.getPropertyCount();
			for (int i = 0; i < returns; i++) {
				res = (SoapObject) response.getProperty(i);
				String date = res.getProperty("modifiedDate").toString();
				sf.setRemoteModifiedDate(getDateFromString(date));
				sf.setRemoteId(res.getProperty("id").toString());
				// sf.setRemoteName(res.getProperty("title").toString());
				sf.setRemoteName(res.getProperty("filename").toString());
				sf.setRemotePath(res.getProperty("path").toString());
				sf.updateSyncState(SyncEvent.Uploaded);
			}
		}
		Log.d("**** SyncManager.updateDocument ****","EXIT");
		return res;
	}

	/**
	 * Method to upload a new file to the server
	 * 
	 * @param sf
	 * @param parentId
	 * @return remote id
	 */
	public SoapObject createDocument(SyncFile sf, String parentId) {
		Log.d("**** SyncManager.createDocument ****","ENTER");
		SoapObject res = null;
		String remoteId = null;
		SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_CREATE_DOCUMENT);
		//Log.i("SyncManager.createDocument", "Parent id: " + parentId);

		request.addProperty("parentUUID", parentId);
		SoapObject props = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_CREATE_DOCUMENT);
		props.addProperty("title", sf.getName());
		props.addProperty("type", getServerType(sf.getDocumentType())
				.name());
		props.addProperty("orangeFile", true);
		props.addProperty("synchronize", true);
		request.addProperty("document", props);
		// create document metadata
		SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE
				.concat(SoapManager.WS_CREATE_DOCUMENT));

		if (response != null) {
			int returns = response.getPropertyCount();
			for (int i = 0; i < returns; i++) {
				res = (SoapObject) response.getProperty(i);
			}
			remoteId = res.getProperty("id").toString();
		} else {
			Log.e("SyncManager.createDocument",
					"SoapObject response: NULL.");
		}
		
		if (remoteId != null) {
			// upload Blob
			//Log.i("SyncManager.createDocument", "Uploading blob for remoteId: " + remoteId);
			String URL = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_URL);
			String username = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_USERNAME);
			String password = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_PASSWORD);
			String msg = UploadFile.uploadFile(URL, sf.getFile(), remoteId,
					sf.getName(), username.concat(":").concat(password));

			Log.i("SyncManager.createDocument", "UploadFile result: " + msg);
		}
		
		// read again for updating local data
		res = getDocumentById(remoteId);
		
		Log.d("**** SyncManager.createDocument ****","EXIT");
		return res;
	}

	/**
	 * Method to download a document from the server
	 * 
	 * @param uuid
	 * @return SoapObject downloaded object
	 */
	public void downloadDocument(String basePath, SyncFile sf) {
		String filename = null;
		SoapObject res = null;
		boolean found = false;
		SoapObject response = getDocumentProperties(sf);
		int returns = response.getPropertyCount();
		for (int i = returns - 1; i >= 0 && !found; i--) {
			res = (SoapObject) response.getProperty(i);
			// Log.i("SyncManager.downloadDocument", "Res: " + res);
			int results = res.getPropertyCount();
			for (int j = 0; j < results; j++) {
				SoapPrimitive prop = (SoapPrimitive) res.getProperty(j);
				// Log.i("SyncManager.downloadDocument", "Props: " + prop);
				if (prop.toString().equals("filename")) {
					filename = res.getProperty(j + 1).toString();
					// Log.i("SyncManager.downloadDocument", "Filename: " +
					// filename);
					found = true;
				}
			}
		}
		// get blob
		String URL = DoMaService.getDoMaInstance().getPreference(
				DoMa.PREF_KEY_URL).concat("nuxeo/restAPI/default/");
		String username = DoMaService.getDoMaInstance().getPreference(
				DoMa.PREF_KEY_USERNAME);
		String password = DoMaService.getDoMaInstance().getPreference(
				DoMa.PREF_KEY_PASSWORD);

		String path = sf.getLocalPath();
		if (path == null) {
			// The file is new so we need to give it a new path in the sdcard
			// String basePath =
			// DoMaService.getDoMaInstance().getBasePath(sf.getDocumentType());

			//Log.i("SyncManager.downloadDocument", "basePath: " + basePath);
			String rprtt = sf.getRemotePathRelativeToType();
			//Log.i("SyncManager.downloadDocument", "RemotePathRelativeToType: " + rprtt);
			path = basePath.concat("/").concat(rprtt);
			while (path.contains("//")) {
				path = path.replaceAll("//", "/");
			}
			//Log.i("SyncManager.downloadDocument", "Path: " + path);
		}
		//Log.i("SyncManager.downloadDocument", "Filename remote: " + filename);
		if (filename != null) {
			// path = path.replace(sf.getName(), filename);
			path = path.substring(0, path.lastIndexOf("/") + 1)
					.concat(filename);
		}

		//Log.i("SyncManager.downloadDocument", "Local path: " + path);

		URL urlFile = null;
		try {
			urlFile = new URL(URL.concat(sf.getRemoteId()).concat(
					"/downloadFile?blobFile=content"));
			//Log.i("SyncManager.downloadDocument", urlFile);
		} catch (MalformedURLException e) {
			Log.e("SyncManager.downloadDocument", "Malformed URL: " + e);
		}

		HttpURLConnection conexion = null;
		InputStream in = null;
		File f = null;
		try {
			conexion = (HttpURLConnection) urlFile.openConnection();
			String encondingB64 = Base64.encodeBytes(new String(username
					.concat(":").concat(password)).getBytes());
			conexion.addRequestProperty("Authorization", "Basic: "
					+ encondingB64);
			conexion.connect();
			// Get input stream
			in = conexion.getInputStream();
			// Log.i("SyncManager.downloadDoc", "InputStream " + in);
			if (in != null) {
				// create file
				f = new File(path);
				if (!f.exists()) {
					// create all directories and then the file itself
					File parent = f.getParentFile();
					if (parent != null && !parent.exists()) {
						parent.mkdirs();
					}
					// create the file
					f.createNewFile();
				} else {
					// This may happen if there are two files with the same name
					// in the same relative path in the server and the device
					f.delete();
					f.createNewFile();
				}
				FileOutputStream fileOS = new FileOutputStream(f);
				byte[] bytes = new byte[1024];
				int leng = 0;
				while ((leng = in.read(bytes)) > 0) {
					fileOS.write(bytes, 0, leng);
				}
				fileOS.close();
			}
			Log.i("SyncManager.downloadDoc", "Response "
					+ conexion.getResponseMessage());
			in.close();
			sf.setLocalPath(f.getPath());
			sf.setLocalModifiedDate(f.lastModified());
		} catch (IOException e) {
			Log.e("SyncManager.downloadDocument()", "Error ", e);
		}
	}

	public SoapObject createFolder(String parentId, String name) {
		Log.d("**** SyncManager.createFolder ****","ENTER");
		SoapObject res = null;
		if (parentId == null) {
			SoapObject ws = getUserWorkspace();
			parentId = ws.getProperty("id").toString();
			Log.w("createFolder", "parentId was NULL, using UserWorkspace");
		}
		SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_CREATE_DOCUMENT);
		request.addProperty("parentUUID", parentId);
		SoapObject prop = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_CREATE_DOCUMENT);
		prop.addProperty("title", name);
		prop.addProperty("type", "Folder");
		request.addProperty("document", prop);
		SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE
				.concat(SoapManager.WS_CREATE_DOCUMENT));
		if (response != null) {
			res = (SoapObject) response.getProperty("return");
		}
		Log.d("**** SyncManager.createFolder ****","EXIT");
		return res;
	}

	/**
	 * Method to create a new OrangeImageGallery in the server.
	 * 
	 * If the folder is inside the OrangeImageGallery root, then it must be of
	 * type OrangeImageGallery.
	 * 
	 * @param parentId
	 * @param name
	 * @return remote id
	 */
	public SoapObject createOrangeImageGallery(String parentId, String name) {
		Log.d("**** SyncManager.createOrangeImageGallery ****","ENTER");
		SoapObject res = null;
		if (parentId == null) {
			SoapObject ws = getUserWorkspace();
			parentId = ws.getProperty("id").toString();
			Log.w("createOrangeImageGallery",
					"parentId was NULL, using UserWorkspace");
		}
		SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_CREATE_DOCUMENT);
		request.addProperty("parentUUID", parentId);
		SoapObject prop = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_CREATE_DOCUMENT);
		prop.addProperty("title", name);
		prop.addProperty("type", ServerTypeRoot.OrangeImageGallery.name());
		request.addProperty("document", prop);
		SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE
				.concat(SoapManager.WS_CREATE_DOCUMENT));
		if (response != null) {
			res = (SoapObject) response.getProperty("return");
		}
		Log.d("**** SyncManager.createOrangeImageGallery ****","EXIT");
		return res;
	}

	public SoapObject createMusicFolder() {
		return createRootFolder(ServerTypeRoot.MusicRootFolder, "Music");
	}

	public SoapObject createPicturesFolder() {
		return createRootFolder(ServerTypeRoot.OrangeImageGallery, "Pictures");
	}

	public SoapObject createMoviesFolder() {
		return createRootFolder(ServerTypeRoot.VideoRootFolder, "Movies");
	}

	public SoapObject createDocumentsFolder() {
		return createRootFolder(ServerTypeRoot.DocumentRootFolder, "Documents");
	}

	public SoapObject createOthersFolder() {
		return createRootFolder(ServerTypeRoot.OtherRootFolder, "Others");
	}

	/**
	 * Method to create a new image gallery on the server
	 * 
	 * @param name
	 * @return remote id
	 */
	private SoapObject createRootFolder(ServerTypeRoot type, String name) {
		Log.d("**** SyncManager.createRootFolder ****","ENTER");
		SoapObject res = null;
		SoapObject ws = getUserWorkspace();
		String parentId = ws.getProperty("id").toString();
		SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_CREATE_DOCUMENT);
		request.addProperty("parentUUID", parentId);
		SoapObject prop = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_CREATE_DOCUMENT);
		prop.addProperty("title", name);
		prop.addProperty("type", type.name());
		request.addProperty("document", prop);
		SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE
				.concat(SoapManager.WS_CREATE_DOCUMENT));
		if (response != null) {
			res = (SoapObject) response.getProperty("return");
		}
		Log.d("**** SyncManager.createRootFolder ****","EXIT");
		return res;
	}

	/**
	 * Method to get the current user workspace
	 * 
	 * @return SoapObject UserWorkspace
	 */
	private SoapObject getUserWorkspace() {
		Log.d("**** SyncManager.getUserWorkspace ****","ENTER");
		if (userWorkspace == null) {
			SoapObject resProps = null;
			SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_SEARCH);
			SoapObject prop = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_SEARCH);
			prop.addProperty("type", "Workspace");
			request.addProperty("search", prop);
			SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE.concat(SoapManager.WS_SEARCH));
			if (response != null) {
				resProps = (SoapObject) response.getProperty("return");
				userWorkspace = resProps;
			}
		}
		Log.d("**** SyncManager.getUserWorkspace ****","EXIT");
		return userWorkspace;
	}

	/**
	 * Method to get the server document from an id
	 * 
	 * @param id
	 * @return soapobject
	 */
	private SoapObject getDocumentById(String id) {
		Log.d("**** SyncManager.getDocumentById ****","ENTER");
		SoapObject res = null;

		SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_GET_DOCUMENT);
		request.addProperty("uuid", id);
		SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE
				.concat(SoapManager.WS_GET_DOCUMENT));
		if (response != null) {
			res = (SoapObject) response.getProperty("return");
			Log.d("SyncManager.getDocumentById", res.toString());
		}
		Log.d("**** SyncManager.getDocumentById ****","EXIT");
		return res;
	}

	/**
	 * 
	 * @param path
	 * @return
	 */
	public SoapObject getDocumentByPath(String path) {
		Log.d("**** SyncManager.getDocumentByPath ****","ENTER");
		Log.d("SyncManager.getDocumentByPath", "Path: " + path);
		SoapObject result = null;
		SoapObject request = new SoapObject(SoapManager.NAMESPACE,
				SoapManager.WS_GET_DOCUMENT_FROM_PATH);
		request.addProperty("path", path);
		SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE
				.concat(SoapManager.WS_GET_DOCUMENT_FROM_PATH));
		if (response != null) {
			result = (SoapObject) response.getProperty("return");
		}
		Log.d("**** SyncManager.getDocumentByPath ****","EXIT");
		return result;
	}

	/**
	 * Looks for a document with the given title inside the passed parent
	 * document.
	 * 
	 * Normally used to check if a path already exists in the server.
	 * 
	 * @param parent
	 * @param title
	 * @return
	 */
	public SoapObject getDocumentByTitle(SoapObject parent, String title) {
		Log.d("**** SyncManager.getDocumentByTitle ****","ENTER");
		SoapObject result = null;
		Log.d("SyncManager.getDocumentByTitle()", "Title: " + title);
		SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_SEARCH);
		SoapObject prop = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_SEARCH);
		prop.addProperty("parentUUID", parent.getProperty("id").toString());
		prop.addProperty("title", title);
		request.addProperty("search", prop);
		SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE.concat(SoapManager.WS_SEARCH));
		if (response != null) {
			int returns = response.getPropertyCount();
			Log.i("SyncManager.getDocumentByTitle()", "Found: " + returns);
			if (returns > 0) {
				result = (SoapObject) response.getProperty(0);
			}
		}
		Log.d("**** SyncManager.getDocumentByTitle ****","EXIT");
		return result;

	}

	/**
	 * Method to delete a document from the server
	 * 
	 * @param id
	 * @return true if the document has been deleted
	 */
	public void deleteDocument(String id) {
		Log.d("**** SyncManager.deleteDocument ****","ENTER");
		SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_DELETE);
		request.addProperty("uuid", id);
		SoapManager.getInstance().execute(request, SoapManager.NAMESPACE.concat(SoapManager.WS_DELETE));
		Log.d("**** SyncManager.deleteDocument ****","EXIT");
	}

	/**
	 * Method to publish a file on Twitter or on Facebook
	 * 
	 * @param syncFile
	 * @param publish
	 */
	/** TODO publish on facebook do not use this method */
	public boolean publish(SyncFile syncFile, String publish) {
		SoapObject request = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_PUBLISH_DOCUMENT);
		request.addProperty("uuid", syncFile.getRemoteId());
		SoapObject metaprop = new SoapObject(SoapManager.NAMESPACE, SoapManager.WS_PUBLISH_DOCUMENT);
		metaprop.addProperty("name", publish);
		request.addProperty("metaproperties", metaprop);
		try {
			SoapObject response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE
					.concat(SoapManager.WS_PUBLISH_DOCUMENT));
			if (response != null) {
				int returns = response.getPropertyCount();
				for (int i = 0; i < returns; i++) {
					SoapObject ret = (SoapObject) response.getProperty(i);
					ret.getProperty("id").toString();
				}
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			Log.e("SyncManager.publish()", "Error publishing on twitter: ", e);
			return false;
		}
	}

	private SoapObject getDocumentProperties(SyncFile sf) {
		Log.d("**** SyncManager.getDocumentProperties ****","ENTER");
		SoapObject response = null;
		SoapObject request = new SoapObject(SoapManager.NAMESPACE,
				SoapManager.WS_GET_DOCUMENT_PROPERTIES);
		request.addProperty("uuid", sf.getRemoteId());
		response = SoapManager.getInstance().execute(request, SoapManager.NAMESPACE
				.concat(SoapManager.WS_GET_DOCUMENT_PROPERTIES));
		Log.d("**** SyncManager.getDocument ****","EXIT");
		return response;
	}

	public InputStream getRemoteThumbnail(String id) {
		// get parameters
		String URL = DoMaService.getDoMaInstance().getPreference(
				DoMa.PREF_KEY_URL).concat("nuxeo/restAPI/default/");
		String username = DoMaService.getDoMaInstance().getPreference(
				DoMa.PREF_KEY_USERNAME);
		String password = DoMaService.getDoMaInstance().getPreference(
				DoMa.PREF_KEY_PASSWORD);

		// connect with the server
		URL urlFile = null;
		try {
			urlFile = new URL(
					URL
							.concat(id)
							.concat("/downloadFile?schema=orangefile&blobField=cover&filenameField=cover_title"));
			// Log.i("SyncManager.getRemoteThumbnail()", "URL: " + urlFile);
		} catch (MalformedURLException e) {
			Log.e("SyncManager.getRemoteThumbnail()", "Malformed URL: " + e);
		}

		HttpURLConnection conexion = null;
		InputStream in = null;
		try {
			conexion = (HttpURLConnection) urlFile.openConnection();
			String encondingB64 = Base64.encodeBytes(new String(username
					.concat(":").concat(password)).getBytes());
			conexion.addRequestProperty("Authorization", "Basic: "
					+ encondingB64);
			conexion.connect();
			// Get input stream
			in = conexion.getInputStream();
			// Log.i("SyncManager.getRemoteThumbnail()", "Response: " +
			// conexion.getResponseCode() + " - " +
			// conexion.getResponseMessage());
		} catch (IOException e) {
			Log.e("SyncManager.getRemoteThumbnail()", "Error ", e);
		}
		return in;
	}
}
