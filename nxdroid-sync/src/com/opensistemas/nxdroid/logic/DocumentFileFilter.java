package com.opensistemas.nxdroid.logic;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

class DocumentFileFilter implements FileFilter, FilenameFilter {

	@Override
	public boolean accept(File pathname) {
		if (pathname == null) return false;
		return accept(pathname.getParentFile(), pathname.getName());
	}	
	
	@Override
	public boolean accept(File dir, String filename) {
		if (filename == null) return false;
		FilenameFilter filter = new PictureFileFilter();
		if (!filter.accept(dir, filename)) {
			filter = new MusicFileFilter();
			if (!filter.accept(dir, filename)) {
				filter = new MovieFileFilter();
				if (!filter.accept(dir, filename)) {
					String ext = FileUtil.getExtension(filename);
					if (ext != null) {
						return true;
					}
				}
			}
		}		
		return false;
	}
	
}