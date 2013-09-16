package com.opensistemas.nxdroid.logic;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * There is a bug in the Android InputStream implementation (up to 2.2) that
 * will cause a BitmapFactory.decodeStream malfunction in some situations.<BR>
 * Using this class to wrap the InpuStream solves that bug.<BR>
 * 
 * {@link http://code.google.com/p/android/issues/detail?id=6066}
 * 
 */
public class FlushedInputStream extends FilterInputStream {
	public FlushedInputStream(InputStream inputStream) {
		super(inputStream);
	}

	@Override
	public long skip(long n) throws IOException {
		long totalBytesSkipped = 0L;
		while (totalBytesSkipped < n) {
			long bytesSkipped = in.skip(n - totalBytesSkipped);
			if (bytesSkipped == 0L) {
				int _byte = read();
				if (_byte < 0) {
					break; // we reached EOF
				} else {
					bytesSkipped = 1; // we read one byte
				}
			}
			totalBytesSkipped += bytesSkipped;
		}
		return totalBytesSkipped;
	}
}