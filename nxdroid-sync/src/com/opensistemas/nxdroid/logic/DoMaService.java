package com.opensistemas.nxdroid.logic;

import android.content.Context;

public class DoMaService {

	public static DoMa getDoMaInstance() {
		return DoMaImpl.getInstance();
	}
	
	public static DoMa getDoMaInstance(Context ctx) {
		return DoMaImpl.getInstance(ctx);
	}
}
