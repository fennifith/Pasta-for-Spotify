package pasta.streamer;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.async.Action;
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
import pasta.streamer.utils.Downloader;
import pasta.streamer.utils.Settings;

public class PlayerService extends Service {

    public static final String ACTION_PLAY = "pasta.ACTION_PLAY";
    public static final String ACTION_PLAY_EXTRA_START_POS = "pasta.ACTION_PLAY_EXTRA_START_POS";
    public static final String ACTION_PLAY_EXTRA_TRACKS = "pasta.ACTION_PLAY_EXTRA_TRACKS";
    public static final String ACTION_TOGGLE = "pasta.ACTION_TOGGLE";
    public static final String ACTION_NEXT = "pasta.ACTION_NEXT";
    public static final String ACTION_PREV = "pasta.ACTION_PREV";
    public static final String ACTION_MOVE_TRACK = "pasta.ACTION_MOVE_TRACK";
    public static final String ACTION_MOVE_TRACK_EXTRA_POS = "pasta.ACTION_MOVE_TRACK_EXTRA_POS";
    public static final String ACTION_MOVE_POS = "pasta.ACTION_MOVE_POS";
    public static final String ACTION_MOVE_POS_EXTRA_POS = "pasta.ACTION_MOVE_POS_EXTRA_POS";

    public static final String STATE_UPDATE = "pasta.STATE_UPDATE";

    public static final String EXTRA_PLAYING = "pasta.EXTRA_PLAYING";
    public static final String EXTRA_CUR_POSITION = "pasta.EXTRA_CUR_POSITION";
    public static final String EXTRA_CUR_TIME = "pasta.EXTRA_CUR_TIME";
    public static final String EXTRA_MAX_TIME = "pasta.EXTRA_MAX_TIME";
    public static final String EXTRA_CUR_TRACK = "pasta.EXTRA_SONG";
    public static final String EXTRA_TRACK_LIST = "pasta.EXTRA_TRACK_LIST";

    public static final int UPDATE_INTERVAL = 500;

    private static final int NOTIFICATION_ID = 12345;
    private Player spotifyPlayer;
    private PlayConfig spotifyPlayConfig;
    private PlayerState spotifyPlayerState;
    private ArrayList<TrackListData> trackList;
    private int curPos, errorCount;
    private boolean debugPlaying;

    @Override
    public void onCreate() {
        super.onCreate();
        initPlayer();
    }

    private void initPlayer() {
        debugPlaying = false;
        Pasta pasta = (Pasta) getApplicationContext();

        Config playerConfig = new Config(this, pasta.token, pasta.CLIENT_ID);
        playerConfig.useCache(false);

        spotifyPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
            @Override
            public void onInitialized(Player player) {
                spotifyPlayer.setPlaybackBitrate(Settings.getQuality(getApplicationContext()));
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
        errorCount++;
        if (errorCount < 5) {
            Toast.makeText(getApplicationContext(), message + ", please restart the app.", Toast.LENGTH_LONG).show();
            stopSelf();
        } else Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !spotifyPlayer.isInitialized()) return START_STICKY;
        String action = intent.getAction();
        if (action == null) return START_STICKY;
        switch (action) {
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
                        checkForState();
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
        return new NotificationCompat.Builder(PlayerService.this)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.preload))
                .setContentTitle(trackList.get(curPos).trackName)
                .setContentText(trackList.get(curPos).artistName)
                .addAction(R.drawable.ic_notify_prev, "Previous", PendingIntent.getService(getApplicationContext(), 1, new Intent(getApplicationContext(), PlayerService.class).setAction(PlayerService.ACTION_PREV), PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(spotifyPlayerState.playing ? R.drawable.ic_notify_pause : R.drawable.ic_notify_play, spotifyPlayerState.playing ? "Pause" : "Play", PendingIntent.getService(getApplicationContext(), 1, new Intent(getApplicationContext(), PlayerService.class).setAction(PlayerService.ACTION_TOGGLE), PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_notify_next, "Next", PendingIntent.getService(getApplicationContext(), 1, new Intent(getApplicationContext(), PlayerService.class).setAction(PlayerService.ACTION_NEXT), PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentIntent(PendingIntent.getActivities(PlayerService.this, 0, new Intent[]{new Intent(PlayerService.this, PlayerActivity.class)}, 0));
    }

    private void showNotification() {
        startForeground(NOTIFICATION_ID, getNotificationBuilder().build());

        new Action<Object[]>() {
            @NonNull
            @Override
            public String id() {
                return "notification";
            }

            @Nullable
            @Override
            protected Object[] run() throws InterruptedException {
                Bitmap b = Downloader.downloadImage(PlayerService.this, trackList.get(curPos).trackImage);
                int color = Palette.from(b).generate().getVibrantColor(Color.GRAY);
                return new Object[]{b, color};
            }

            @Override
            protected void done(@Nullable Object[] result) {
                if (result != null) startForeground(NOTIFICATION_ID, getNotificationBuilder().setLargeIcon((Bitmap) result[0]).setColor((int) result[1]).build());
            }
        }.execute();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
