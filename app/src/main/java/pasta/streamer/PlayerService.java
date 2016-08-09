package pasta.streamer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.graphics.Palette;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.PlayConfig;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import com.spotify.sdk.android.player.Spotify;

import java.util.ArrayList;
import java.util.List;

import pasta.streamer.activities.PlayerActivity;
import pasta.streamer.data.TrackListData;
import pasta.streamer.utils.PreferenceUtils;

public class PlayerService extends Service {

    public static final String
            ACTION_INIT = "pasta.ACTION_INIT",
            ACTION_PLAY = "pasta.ACTION_PLAY",
            ACTION_PLAY_EXTRA_START_POS = "pasta.ACTION_PLAY_EXTRA_START_POS",
            ACTION_PLAY_EXTRA_TRACKS = "pasta.ACTION_PLAY_EXTRA_TRACKS",
            ACTION_TOGGLE = "pasta.ACTION_TOGGLE",
            ACTION_NEXT = "pasta.ACTION_NEXT",
            ACTION_PREV = "pasta.ACTION_PREV",
            ACTION_MOVE_TRACK = "pasta.ACTION_MOVE_TRACK",
            ACTION_MOVE_TRACK_EXTRA_POS = "pasta.ACTION_MOVE_TRACK_EXTRA_POS",
            ACTION_MOVE_POS = "pasta.ACTION_MOVE_POS",
            ACTION_MOVE_POS_EXTRA_POS = "pasta.ACTION_MOVE_POS_EXTRA_POS",
            STATE_UPDATE = "pasta.STATE_UPDATE",
            EXTRA_TOKEN = "pasta.EXTRA_TOKEN",
            EXTRA_CLIENT_ID = "pasta.EXTRA_CLIENT_ID",
            EXTRA_PLAYING = "pasta.EXTRA_PLAYING",
            EXTRA_CUR_POSITION = "pasta.EXTRA_CUR_POSITION",
            EXTRA_CUR_TIME = "pasta.EXTRA_CUR_TIME",
            EXTRA_MAX_TIME = "pasta.EXTRA_MAX_TIME",
            EXTRA_CUR_TRACK = "pasta.EXTRA_SONG",
            EXTRA_TRACK_LIST = "pasta.EXTRA_TRACK_LIST";

    public static final int UPDATE_INTERVAL = 500;

    private static final int NOTIFICATION_ID = 12345;

    private Player spotifyPlayer;
    private Config playerConfig;
    private PlayConfig spotifyPlayConfig;
    private PlayerState spotifyPlayerState;
    private ArrayList<TrackListData> trackList;
    private int curPos, errorCount;
    private boolean debugPlaying;

    private Pasta pasta;

    @Override
    public void onCreate() {
        super.onCreate();
        pasta = (Pasta) getApplicationContext();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (spotifyPlayer != null) spotifyPlayer.shutdownNow();
    }

    private void initPlayer(String token, String clientId) {
        debugPlaying = false;

        if (playerConfig == null) {
            playerConfig = new Config(this, token, clientId);
            playerConfig.useCache(false);
        }

        spotifyPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
            @Override
            public void onInitialized(Player player) {
                spotifyPlayer.setPlaybackBitrate(PreferenceUtils.getQuality(getApplicationContext()));
                spotifyPlayer.setRepeat(true);

                checkForState();
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                PlayerService.this.onError(throwable.getMessage());
            }
        });

        spotifyPlayer.addPlayerNotificationCallback(new PlayerNotificationCallback() {
            @Override
            public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
                spotifyPlayerState = playerState;
                if (trackList != null) {
                    if (!trackList.get(curPos).trackUri.matches(playerState.trackUri)) {
                        if (trackList.get(getInfinitePos(curPos + 1)).trackUri.matches(playerState.trackUri)) curPos = getInfinitePos(curPos + 1);
                        else {
                            for (int i = 0; i < trackList.size(); i++) {
                                if (trackList.get(i).trackUri.matches(playerState.trackUri)) {
                                    curPos = i;
                                    break;
                                }
                            }
                        }
                    }
                    showNotification();
                }
            }

            @Override
            public void onPlaybackError(ErrorType errorType, String s) {
                Log.e("PlayerService", errorType.name() + " " + s);
                onError(errorType.name() + " " + s);
            }
        });

        spotifyPlayer.addConnectionStateCallback(new ConnectionStateCallback() {
            @Override
            public void onLoggedIn() {
            }

            @Override
            public void onLoggedOut() {
            }

            @Override
            public void onLoginFailed(Throwable throwable) {
                onError("Login Failed: " + throwable.getMessage());
            }

            @Override
            public void onTemporaryError() {
                onError("Random error");
            }

            @Override
            public void onConnectionMessage(String s) {
            }
        });
    }

    private void onError(String message) {
        if (spotifyPlayer != null) {
            errorCount++;
            if (errorCount > 5 && errorCount < 20) {
                if (PreferenceUtils.isDebug(this))
                    pasta.showToast(message + ", attempting to restart...");

                stopService(new Intent(this, PlayerService.class));

                Intent intent = new Intent(PlayerService.ACTION_INIT);
                intent.setClass(this, PlayerService.class);
                intent.putExtra(PlayerService.EXTRA_TOKEN, playerConfig.oauthToken);
                intent.putExtra(PlayerService.EXTRA_CLIENT_ID, playerConfig.clientId);
                startService(intent);
                errorCount = 20;
            } else if (PreferenceUtils.isDebug(this))
                pasta.showToast(message);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            pasta.onError(this, "random start command");
            return START_STICKY;
        }

        if (spotifyPlayer == null && !intent.getAction().matches(ACTION_INIT)) {
            pasta.onError(this, "null spotify player");
            return START_STICKY;
        }

        switch (intent.getAction()) {
            case ACTION_INIT:
                initPlayer(intent.getStringExtra(EXTRA_TOKEN), intent.getStringExtra(EXTRA_CLIENT_ID));
                break;
            case ACTION_PLAY:
                spotifyPlayer.pause();

                trackList = intent.getParcelableArrayListExtra(ACTION_PLAY_EXTRA_TRACKS);

                List<String> trackUris = new ArrayList<>();
                for (TrackListData trackListData : trackList) {
                    trackUris.add(trackListData.trackUri);
                }

                curPos = getInfinitePos(intent.getIntExtra(ACTION_PLAY_EXTRA_START_POS, -1));
                spotifyPlayConfig = PlayConfig.createFor(trackUris).withTrackIndex(curPos);
                spotifyPlayer.play(spotifyPlayConfig);

                debugPlaying = true;
                break;
            case ACTION_TOGGLE:
                if (spotifyPlayerState.playing) {
                    spotifyPlayer.pause();
                } else {
                    spotifyPlayer.resume();
                }

                debugPlaying = !debugPlaying;
                break;
            case ACTION_NEXT:
                spotifyPlayer.pause();
                curPos = getInfinitePos(curPos + 1);
                spotifyPlayer.play(spotifyPlayConfig.withTrackIndex(curPos));

                debugPlaying = true;
                break;
            case ACTION_PREV:
                spotifyPlayer.pause();
                curPos = getInfinitePos(curPos - 1);
                spotifyPlayer.play(spotifyPlayConfig.withTrackIndex(curPos));

                debugPlaying = true;
                break;
            case ACTION_MOVE_TRACK:
                spotifyPlayer.pause();
                curPos = getInfinitePos(intent.getIntExtra(ACTION_MOVE_TRACK_EXTRA_POS, 0));
                spotifyPlayer.play(spotifyPlayConfig.withTrackIndex(curPos));

                debugPlaying = true;
                break;
            case ACTION_MOVE_POS:
                spotifyPlayer.seekToPosition(intent.getIntExtra(ACTION_MOVE_POS_EXTRA_POS, -1));
                return START_STICKY;
        }
        return START_STICKY;
    }

    private int getInfinitePos(int pos) {
        if (pos >= trackList.size()) return 0;
        else if (pos < 0) return trackList.size() - 1;
        else return pos;
    }

    private void checkForState() {
        spotifyPlayer.getPlayerState(new PlayerStateCallback() {
            @Override
            public void onPlayerState(PlayerState playerState) {
                spotifyPlayerState = playerState;
                if (trackList != null && trackList.size() > 0) {
                    sendUpdateToUI();
                }

                if (debugPlaying && !playerState.playing && playerState.durationInMs == 0) {
                    onError("Unknown error");
                    return;
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!spotifyPlayer.isShutdown()) checkForState();
                    }
                }, UPDATE_INTERVAL);
            }
        });
    }

    private void sendUpdateToUI() {
        TrackListData curTrack = trackList.get(curPos);

        Intent intent = new Intent(STATE_UPDATE);
        intent.putExtra(EXTRA_PLAYING, spotifyPlayerState.playing);
        intent.putExtra(EXTRA_CUR_POSITION, curPos);
        intent.putExtra(EXTRA_CUR_TIME, spotifyPlayerState.positionInMs);
        intent.putExtra(EXTRA_MAX_TIME, spotifyPlayerState.durationInMs);
        intent.putExtra(EXTRA_CUR_TRACK, curTrack);
        intent.putExtra(EXTRA_TRACK_LIST, trackList);
        sendBroadcast(intent);
    }

    private NotificationCompat.Builder getNotificationBuilder() {
        boolean vectors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(PlayerService.this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(trackList.get(curPos).trackName)
                .addAction(vectors ? R.drawable.ic_prev : 0, "Previous", PendingIntent.getService(getApplicationContext(), 1, new Intent(getApplicationContext(), PlayerService.class).setAction(PlayerService.ACTION_PREV), PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(vectors ? (spotifyPlayerState.playing ? R.drawable.ic_pause : R.drawable.ic_play) : 0, spotifyPlayerState.playing ? "Pause" : "Play", PendingIntent.getService(getApplicationContext(), 1, new Intent(getApplicationContext(), PlayerService.class).setAction(PlayerService.ACTION_TOGGLE), PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(vectors ? R.drawable.ic_next : 0, "Next", PendingIntent.getService(getApplicationContext(), 1, new Intent(getApplicationContext(), PlayerService.class).setAction(PlayerService.ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentIntent(PendingIntent.getActivities(PlayerService.this, 0, new Intent[]{new Intent(PlayerService.this, PlayerActivity.class)}, 0));

        if (trackList.get(curPos).artists.size() > 0)
            builder.setContentText(trackList.get(curPos).artists.get(0).artistName);

        return builder;
    }

    private void showNotification() {
        startForeground(NOTIFICATION_ID, getNotificationBuilder().build());

        Glide.with(this).load(trackList.get(curPos).trackImage).asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(final Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                Palette.from(resource).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        startForeground(NOTIFICATION_ID, getNotificationBuilder().setLargeIcon(resource).setColor(palette.getVibrantColor(Color.GRAY)).build());
                    }
                });
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
