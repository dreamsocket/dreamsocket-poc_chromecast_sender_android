package com.dreamsocket.castmonkey;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

/**
 * Created by keithpeters on 2/3/15.
 */
public class CastMonkey {
    protected final String TAG = "CastMonkey";
//    protected final String RECEIVER_ID = "ABF33862";
    protected final String RECEIVER_ID = CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;
//    protected final String MEDIA_URL = "https://github.com/dreamsocket/dreamsocket-poc_chromecast_sender_android/raw/master/media/mickey.mp4";
//    protected final String MEDIA_URL = "http://trailers.apple.com/movies/sony_pictures/afterearth/afterearth-tlr1_h640w.mov";
    protected final String MEDIA_URL = "http://movietrailers.apple.com/movies/universal/minions/minions-tlr2_i320.m4v";

    protected Context m_context;
    protected MediaRouter m_mediaRouter;
    protected MediaRouteSelector m_mediaRouteSelector;
    protected MediaRouter.Callback m_mediaRouterCallback;
    protected CastDevice m_castDevice;
    protected Cast.Listener m_castListener;
    protected GoogleApiClient m_googleApiClient;
    protected RemoteMediaPlayer m_remoteMediaPlayer;
    protected CastMonkeyListener m_castMonkeyListener;
    protected Boolean m_isPlaying = false;
    protected Boolean m_hasLoadedMedia = false;

    public CastMonkey(Context p_context) {
        this.m_context = p_context;
        createRouter();
    }

    protected void createRouter() {
        m_mediaRouter = MediaRouter.getInstance(this.m_context);

        m_mediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .build();

        m_mediaRouterCallback = new MediaRouter.Callback() {
            @Override
            public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
                super.onRouteSelected(router, route);
                m_castDevice = CastDevice.getFromBundle(route.getExtras());
                connectClient();
            }

            @Override
            public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
                super.onRouteUnselected(router, route);
                // Stop media if playing???
            }
        };
    }

    protected void connectClient() {
        GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                try {
                    launchReceiverApp();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch application", e);
                }
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.d(TAG, "Connection suspended");
            }
        };

        GoogleApiClient.OnConnectionFailedListener connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                Log.d(TAG, "Connection failed");
            }
        };

        this.m_castListener = new Cast.Listener() {
            @Override
            public void onApplicationStatusChanged() {
                super.onApplicationStatusChanged();
            }
        };

        Cast.CastOptions.Builder builder = Cast.CastOptions.builder(this.m_castDevice, this.m_castListener);

        m_googleApiClient = new GoogleApiClient.Builder(this.m_context)
                .addApi(Cast.API, builder.build())
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(connectionFailedListener)
                .build();
        m_googleApiClient.connect();

    }

    public void launchReceiverApp() {
        Cast.CastApi.launchApplication(this.m_googleApiClient, RECEIVER_ID, false)
                .setResultCallback(
                        new ResultCallback<Cast.ApplicationConnectionResult>() {
                            @Override
                            public void onResult(Cast.ApplicationConnectionResult result) {
                                Status status = result.getStatus();
                                if (status.isSuccess()) {
                                    ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
                                    String sessionId = result.getSessionId();
                                    String applicationStatus = result.getApplicationStatus();
                                    boolean wasLaunched = result.getWasLaunched();
                                    createPlayer();
                                } else {
//                                    teardown();
                                }
                            }
                        });


    }

    public void createPlayer() {
        this.m_remoteMediaPlayer = new RemoteMediaPlayer();
        this.m_remoteMediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {
            @Override
            public void onStatusUpdated() {
                MediaStatus status = m_remoteMediaPlayer.getMediaStatus();

            }
        });
        this.m_remoteMediaPlayer.setOnMetadataUpdatedListener(new RemoteMediaPlayer.OnMetadataUpdatedListener() {
            @Override
            public void onMetadataUpdated() {
                MediaInfo info = m_remoteMediaPlayer.getMediaInfo();
                if(info != null) {
                    MediaMetadata metadata = info.getMetadata();
                }
            }
        });
        try {
            Cast.CastApi.setMessageReceivedCallbacks(m_googleApiClient, m_remoteMediaPlayer.getNamespace(), m_remoteMediaPlayer);
        } catch (IOException e) {
            Log.e(TAG, "Exception while creating media channel", e);
        }

        this.m_remoteMediaPlayer
                .requestStatus(m_googleApiClient)
                .setResultCallback(
                        new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                            @Override
                            public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                                if (result.getStatus().isSuccess()) {
                                    m_castMonkeyListener.onReadyToPlay();
                                }
                                else {
                                    Log.e(TAG, "Failed to request status.");
                                }
                            }
                        });
    }

    public void togglePlay() {
        if(this.m_isPlaying) {
            this.m_remoteMediaPlayer.pause(m_googleApiClient);
            this.m_isPlaying = false;
        }
        else if(this.m_hasLoadedMedia) {
            this.m_remoteMediaPlayer.play(m_googleApiClient);
            this.m_isPlaying = true;
        }
        else {
            this.loadMedia();
            this.m_isPlaying = true;
        }
    }

    protected void loadMedia() {
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, "Minions Movie");
        MediaInfo mediaInfo = new MediaInfo.Builder(
                MEDIA_URL)
                .setContentType("video/mp4")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
        try {
            m_remoteMediaPlayer.load(m_googleApiClient, mediaInfo, true)
                    .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                            if (result.getStatus().isSuccess()) {
                                m_hasLoadedMedia = true;
                                Log.d(TAG, "Media loaded successfully");
                            }
                        }
                    });
        } catch (IllegalStateException e) {
            Log.e(TAG, "Problem occurred with media during loading", e);
        } catch (Exception e) {
            Log.e(TAG, "Problem opening media during loading", e);
        }
    }

    public void addMediaRouterCallback() {
        m_mediaRouter.addCallback(m_mediaRouteSelector, m_mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    public void removeMediaRouterCallback() {
        m_mediaRouter.removeCallback(m_mediaRouterCallback);
    }

    public void setMediaRouteMenuItem(MenuItem p_item) {
        MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat.getActionProvider(p_item);
        mediaRouteActionProvider.setRouteSelector(this.m_mediaRouteSelector);

    }

    public void setListener(CastMonkeyListener p_listener) {
        this.m_castMonkeyListener = p_listener;
    }

    public Boolean isPlaying() {
        return this.m_isPlaying;
    }
}
