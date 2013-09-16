package com.opensistemas.nxdroid.logic;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

class PictureFileFilter implements FileFilter, FilenameFilter {

	@Override
	public boolean accept(File pathname) {
		if (pathname == null) return false;
		return accept(pathname.getParentFile(), pathname.getName());
	}

	@Override
	public boolean accept(File dir, String filename) {
		String ext = FileUtil.getExtension(filename);
		if (ext!=null) { 
			if (ext.equals(".jpg") || ext.equals(".png") || ext.equals(".gif")) {
				return true;
			}
		}		
		return false;
	}
	
}