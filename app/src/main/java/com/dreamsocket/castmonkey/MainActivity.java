package com.dreamsocket.castmonkey;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;


public class MainActivity extends ActionBarActivity implements CastMonkeyListener{
    protected final String TAG = "MainActivity";

    protected CastMonkey m_castMonkey;
    protected Button m_playPauseBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.m_castMonkey = new CastMonkey(this);
        this.m_castMonkey.setListener(this);

        this.m_playPauseBtn = (Button)this.findViewById(R.id.play_pause_button);
        this.m_playPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(m_castMonkey.isPlaying()) {
                    m_playPauseBtn.setText("Play");
                }
                else {
                    m_playPauseBtn.setText("Pause");
                }
                m_castMonkey.togglePlay();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.m_castMonkey.addMediaRouterCallback();
    }

    @Override
    protected void onPause() {
        if(isFinishing()) {
            this.m_castMonkey.removeMediaRouterCallback();
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.m_castMonkey.setMediaRouteMenuItem(menu.findItem(R.id.media_route_menu_item));
        return true;
    }


    @Override
    public void onReadyToPlay() {
        this.m_playPauseBtn.setEnabled(true);
    }
}

