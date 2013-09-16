package com.opensistemas.nxdroid.logic;

import java.io.File;
import java.io.FileFilter;

class DirectoryFileFilter implements FileFilter {

	@Override
	public boolean accept(File pathname) {
		if (pathname == null) return false;
		return (pathname.isDirectory());
	}	
}