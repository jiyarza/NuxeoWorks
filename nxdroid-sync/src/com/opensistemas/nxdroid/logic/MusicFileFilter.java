package com.opensistemas.nxdroid.logic;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

class MusicFileFilter implements FileFilter, FilenameFilter {

	@Override
	public boolean accept(File pathname) {
		if (pathname == null) return false;
		return accept(pathname.getParentFile(), pathname.getName());
	}	
	
	@Override
	public boolean accept(File dir, String filename) {
		if (filename == null) return false;
		String ext = FileUtil.getExtension(filename);
		if (ext != null) {			
			if (ext.equals(".mp3")) {
				return true;
			}
		}
		return false;
	}
	
}