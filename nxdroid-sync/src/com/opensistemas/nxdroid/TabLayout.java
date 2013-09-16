package com.opensistemas.nxdroid;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;

public class TabLayout extends TabActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final TabHost tabHost = getTabHost();

        tabHost.addTab(tabHost.newTabSpec(getString(R.string.pictures_tab))
                .setIndicator(getString(R.string.pictures_tab), 
                		getResources().getDrawable(R.drawable.pictures_tab))
                .setContent(new Intent(this, PictureExplorer.class)));
        
        tabHost.addTab(tabHost.newTabSpec(getString(R.string.music_tab))
                .setIndicator(getString(R.string.music_tab),
                		getResources().getDrawable(R.drawable.music_tab))
                .setContent(new Intent(this, MusicExplorer.class)));
        
        tabHost.addTab(tabHost.newTabSpec(getString(R.string.movies_tab))
                .setIndicator(getString(R.string.movies_tab),
                		getResources().getDrawable(R.drawable.movies_tab))
                .setContent(new Intent(this, MovieExplorer.class)));
        
        tabHost.addTab(tabHost.newTabSpec(getString(R.string.docs_tab))
                .setIndicator(getString(R.string.docs_tab),
                		getResources().getDrawable(R.drawable.documents_tab))
                .setContent(new Intent(this, DocumentExplorer.class)));
        
        tabHost.addTab(tabHost.newTabSpec(getString(R.string.other_tab))
                .setIndicator(getString(R.string.other_tab),
                		getResources().getDrawable(R.drawable.others_tab))
                .setContent(new Intent(this, OtherExplorer.class)));               
    }
   
}
