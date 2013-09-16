package com.opensistemas.nxdroid.logic;

import java.util.Comparator;



public class SyncFileComparator implements Comparator<SyncFile> {

	@Override
	public int compare(SyncFile a, SyncFile b) {
		return (a.getName().compareTo(b.getName()));
	}

}
