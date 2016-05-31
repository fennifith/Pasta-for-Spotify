package pasta.streamer.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.afollestad.async.Action;
import com.afollestad.async.Async;
import com.afollestad.async.Done;
import com.afollestad.async.Result;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.SavedAlbum;
import kaaes.spotify.webapi.android.models.SavedTrack;
import pasta.streamer.Pasta;
import pasta.streamer.PlayerService;
import pasta.streamer.R;
import pasta.streamer.data.AlbumListData;
import pasta.streamer.data.TrackListData;

public class MainActivity extends AppCompatActivity {
    private static final String REDIRECT_URI = "spotifystreamer://callback";
    private static final int REQUEST_CODE = 1234;
    Pasta pasta;
    SharedPreferences prefs;

    @Bind(R.id.start)
    View start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        pasta = (Pasta) getApplicationContext();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (prefs.getBoolean("first_time", true)) {
            startActivity(new Intent(MainActivity.this, IntroActivity.class));
            start.setVisibility(View.VISIBLE);
        } else openRequest();
    }

    @OnClick(R.id.start)
    public void firstStart() {
        prefs.edit().putBoolean("first_time", false).apply();
        openRequest();
    }

    private void openRequest() {
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(pasta.CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "user-read-email", "streaming", "user-follow-read", "user-follow-modify", "user-library-read", "playlist-read-private", "playlist-modify-public", "playlist-modify-private"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                case TOKEN:
                    pasta.token = response.getAccessToken();
                    pasta.spotifyApi = new SpotifyApi();
                    pasta.spotifyApi.setAccessToken(pasta.token);
                    pasta.spotifyService = pasta.spotifyApi.getService();

                    Async.parallel(new Action<Boolean>() {
                        @NonNull
                        @Override
                        public String id() {
                            return "getMe";
                        }

                        @Nullable
                        @Override
                        protected Boolean run() throws InterruptedException {
                            try {
                                pasta.me = pasta.spotifyService.getMe();
                            } catch (Exception e) {
                                e.printStackTrace();
                                return false;
                            }

                            return true;
                        }

                        @Override
                        protected void done(@Nullable Boolean result) {
                            if (result == null || !result) pasta.onNetworkError(MainActivity.this);
                        }
                    }, new Action<Boolean>() {
                        @NonNull
                        @Override
                        public String id() {
                            return "getFavAlbums";
                        }

                        @Nullable
                        @Override
                        protected Boolean run() throws InterruptedException {
                            Pager<SavedAlbum> albumPager;
                            try {
                                albumPager = pasta.spotifyService.getMySavedAlbums();
                            } catch (Exception e) {
                                e.printStackTrace();
                                return false;
                            }

                            ArrayList<AlbumListData> albums = new ArrayList<>();
                            for (SavedAlbum album : albumPager.items) {
                                Artist a = pasta.spotifyService.getArtist(album.album.artists.get(0).id);

                                String image = "";
                                if (a.images.size() > 0) image = a.images.get(a.images.size() / 2).url;

                                albums.add(new AlbumListData(album.album, image));
                            }
                            pasta.albums = albums;

                            return true;
                        }

                        @Override
                        protected void done(@Nullable Boolean result) {
                            if (result == null || !result) pasta.onNetworkError(MainActivity.this);
                        }
                    }, new Action<Boolean>() {
                        @NonNull
                        @Override
                        public String id() {
                            return "getFavTracks";
                        }

                        @Nullable
                        @Override
                        protected Boolean run() throws InterruptedException {
                            Pager<SavedTrack> trackPager;
                            try {
                                trackPager = pasta.spotifyService.getMySavedTracks();
                            } catch (Exception e) {
                                e.printStackTrace();
                                return false;
                            }

                            ArrayList<TrackListData> tracks = new ArrayList<>();
                            for (SavedTrack track : trackPager.items) {
                                tracks.add(new TrackListData(track.track));
                            }
                            pasta.tracks = tracks;

                            return true;
                        }

                        @Override
                        protected void done(@Nullable Boolean result) {
                            if (result == null || !result) pasta.onNetworkError(MainActivity.this);
                        }
                    }).done(new Done() {
                        @Override
                        public void result(@NonNull Result result) {
                            if (!pasta.me.product.matches("premium")) {
                                new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle(R.string.user_premium).setMessage(R.string.user_premium_msg).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        startService(new Intent(MainActivity.this, PlayerService.class));
                                        startActivity(new Intent(MainActivity.this, HomeActivity.class));
                                        finish();
                                    }
                                }).setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        finish();
                                    }
                                }).create().show();
                            } else {
                                startService(new Intent(MainActivity.this, PlayerService.class));
                                startActivity(new Intent(MainActivity.this, HomeActivity.class));
                                finish();
                            }
                        }
                    });
                    break;
                case ERROR:
                    pasta.onNetworkError(MainActivity.this);
            }
        }
    }
}
