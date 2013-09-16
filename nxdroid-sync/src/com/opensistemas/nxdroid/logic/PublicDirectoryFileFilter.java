package com.opensistemas.nxdroid.logic;

import java.io.File;
import java.io.FileFilter;

class PublicDirectoryFileFilter implements FileFilter {

	@Override
	public boolean accept(File pathname) {
		if (pathname == null) return false;
		if (!pathname.isDirectory()) return false;
		String name = pathname.getName();
		if (name == null) return false;
		if (name.startsWith(".")) return false;
		if (name.equalsIgnoreCase("LOST.DIR")) return false;
		if (name.equalsIgnoreCase("tmp")) return false;
		if (name.equalsIgnoreCase("Android")) return false;
		if (name.equalsIgnoreCase("rssreader")) return false;
		if (name.equalsIgnoreCase("rosie_scroll")) return false;
		if (name.equalsIgnoreCase("backup")) return false;
		
		return true;
	}
}
