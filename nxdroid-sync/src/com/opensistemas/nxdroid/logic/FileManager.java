package com.opensistemas.nxdroid.logic;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.opensistemas.nxdroid.logic.SyncFile.DocumentType;

import android.content.Context;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This class deals with processes regarding the local file system
 * 
 * @author loran
 * @author jiyarza
 *
 */
class FileManager {
	
	private static FileManager instance;
	
	public static FileManager getInstance() {
		if (instance == null) {
			instance = new FileManager();
		}
		return instance;
	}
	
	private FileManager(){
	}
	
	/**
	 * Returns all image files within a directory and its subdirectories
	 * @return
	 */
	public File[] getLocalPictures(Context context) {		
		String dir = PreferenceManager.getDefaultSharedPreferences(context).getString(DoMa.PREF_KEY_PICTURES_FOLDER,"");
		if (dir == null || dir.equals("Default")) dir = Environment.DIRECTORY_PICTURES;
		List<File> files = getFiles(new File(dir), new PictureFileFilter());
		Log.i("FileManager.getLocalPictures", dir + " : "+files.size());		
		return files.toArray(new File[files.size()]);
	}

	/**
	 * Returns all audio files within a directory and its subdirectories
	 * @return
	 */
	public File[] getLocalMusic(Context context) {
		String dir = PreferenceManager.getDefaultSharedPreferences(context).getString(DoMa.PREF_KEY_MUSIC_FOLDER,"");
		if (dir == null || dir.equals("Default")) dir = Environment.DIRECTORY_MUSIC;		
		List<File> files = getFiles(new File(dir), new MusicFileFilter());
		Log.i("FileManager.getLocalMusic", ""+files.size());
		return files.toArray(new File[files.size()]);
	}		
	
	/**
	 * Returns all video files within a directory and its subdirectories
	 * @return
	 */
	public File[] getLocalMovies(Context context) {		
		String dir = PreferenceManager.getDefaultSharedPreferences(context).getString(DoMa.PREF_KEY_MOVIES_FOLDER,"");
		if (dir == null || dir.equals("Default")) dir = Environment.DIRECTORY_MOVIES;
		List<File> files = getFiles(new File(dir), new MovieFileFilter());
		Log.i("**** getLocalPictures = ", ""+files.size());
		return files.toArray(new File[files.size()]);
	}
	
	/**
	 * Returns all document files within a directory and its subdirectories
	 * @return
	 */
	public File[] getLocalDocuments(Context context) {
		String dir = PreferenceManager.getDefaultSharedPreferences(context).getString(DoMa.PREF_KEY_DOCUMENTS_FOLDER,"");
		if (dir == null || dir.equals("Default")) dir = Environment.DIRECTORY_DOWNLOADS;		
		List<File> files = getFiles(new File(dir), new DocumentFileFilter());
		return files.toArray(new File[files.size()]);
	}	
	
	/**
	 * Returns all files within a directory and its subdirectories that do not belong to any other category
	 * @return
	 */
	public File[] getLocalOthers(Context context) {
		String dir = PreferenceManager.getDefaultSharedPreferences(context).getString(DoMa.PREF_KEY_OTHERS_FOLDER,"");
		if (dir == null || dir.equals("Default")) dir = Environment.DIRECTORY_DOWNLOADS;		
		List<File> files = getFiles(new File(dir), new OtherFileFilter());
		return files.toArray(new File[files.size()]);
	}	
	
	/**
	 * Returns all files within a directory 
	 * and its subdirectories that satisfy the
	 * given FileFilter.
	 * @param f
	 * @return
	 */
	private List<File> getFiles(final File f, final FileFilter filter) {
		List<File> files = new ArrayList<File>(); 
		if (f == null) return files;
		if (!f.isDirectory()) return files; 

		// add root level files to the result list
		File[] aux = (f.listFiles(filter));
		if (aux != null) {
			for (File a: aux) {
				files.add(a);
			}
		}
		
		// retrieve subdirectories and add files recursively
		File[] dirs = f.listFiles(new DirectoryFileFilter());
		if (dirs != null) {
			for (File d: dirs) {
				files.addAll(getFiles(d, filter));
			}
		}
		if (files != null) {
			Collections.sort(files);
		}
		return files;
	}
	
	/**
	 * Returns a list of local root public directories in the sdcard
	 * that can be used as root folders
	 * @return
	 */
	public List<File> getPublicDirectories() {
		List<File> files = new ArrayList<File>(); 
		File[] aux = (Environment.getExternalStorageDirectory().listFiles(new PublicDirectoryFileFilter()));
		if (aux != null) {
			for (File a: aux) {
				files.add(a);
				File[] children = a.listFiles(new PublicDirectoryFileFilter());
				if(children != null){
					for(File child: children)
						files.add(child);
				}
			}
		}
		if (files != null) {
			Collections.sort(files);
		}
		return files;
	}
	
	/**
	 * Returns the local files of a given type
	 * @param context
	 * @param type
	 * @return
	 */
	public File[] getLocalFiles(Context context, DocumentType type) {
		switch (type) { 
		case DOCUMENT: return getLocalDocuments(context);
		case MOVIE: return getLocalMovies(context);
		case MUSIC: return getLocalMusic(context);
		case OTHER: return getLocalOthers(context);
		case PICTURE: return getLocalPictures(context);
		default: return null;
		}
	}	
}
