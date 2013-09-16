package com.opensistemas.nxdroid.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {

	public static String getExtension(File f) {
		return getExtension(f.getName());
	}
	
	public static String getExtension(String f) {
		if (f == null) return null;
		int dot = f.lastIndexOf(".");
		if (dot > 0) {
			return f.substring(dot).toLowerCase();			
		} else {
			return null;
		}		
	}
	
	public static byte[] getBytes(File file){
		byte[] imgData = new byte[(int) file.length()];
		InputStream is;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new IllegalArgumentException("File not found: " + file.getName());
		}

		int offset = 0; 
		int numRead = 0; 
		try {
			while (offset < imgData.length && 
					(numRead=is.read(imgData, offset, imgData.length-offset)) >= 0) { 
				offset += numRead; 
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("Error getting bytes from: " + file.getName());
		} finally {
			try {
				is.close();
			} catch (IOException e) {}
		}
		return imgData;
	}	
}
