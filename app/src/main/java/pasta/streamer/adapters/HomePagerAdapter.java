package pasta.streamer.adapters;

import android.app.Activity;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.afollestad.async.Action;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.AlbumSimple;
import kaaes.spotify.webapi.android.models.FeaturedPlaylists;
import kaaes.spotify.webapi.android.models.NewReleases;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import pasta.streamer.Pasta;
import pasta.streamer.data.AlbumListData;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.fragments.OmniFragment;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;

public class HomePagerAdapter extends FragmentStatePagerAdapter {

    Activity activity;
    Pasta pasta;

    OmniFragment albumFragment;
    OmniFragment playlistFragment;

    public HomePagerAdapter(final Activity activitiy, final FragmentManager manager) {
        super(manager);
        this.activity = activitiy;
        pasta = (Pasta) activity.getApplicationContext();

        albumFragment = new OmniFragment();
        playlistFragment = new OmniFragment();

        new Action<ArrayList<String>>() {
            @NonNull
            @Override
            public String id() {
                return "getNewReleases";
            }

            @Nullable
            @Override
            protected ArrayList<String> run() throws InterruptedException {
                ArrayList<String> albums = new ArrayList<>();
                NewReleases releases = null;
                for (int i = 0; releases == null && i < PreferenceUtils.getRetryCount(activity); i++) {
                    try {
                        releases = pasta.spotifyService.getNewReleases();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (StaticUtils.shouldResendRequest(e)) Thread.sleep(200);
                        else break;
                    }
                }
                if (releases == null) return null;

                for (AlbumSimple album : releases.albums.items) {
                    albums.add(album.id);
                }
                return albums;
            }

            @Override
            protected void done(@Nullable ArrayList<String> result) {
                if (result == null) {
                    pasta.onCriticalError(activity, "new releases action");
                    return;
                }

                for (final String id : result) {
                    new Action<AlbumListData>() {
                        @NonNull
                        @Override
                        public String id() {
                            return "getAlbum";
                        }

                        @Nullable
                        @Override
                        protected AlbumListData run() throws InterruptedException {
                            return pasta.getAlbum(id);
                        }

                        @Override
                        protected void done(@Nullable AlbumListData result) {
                            if (result == null) return;
                            albumFragment.addData(result);
                        }
                    }.execute();
                }
            }
        }.execute();

        new Action<ArrayList<PlaylistListData>>() {
            @NonNull
            @Override
            public String id() {
                return "getFeaturedPlaylists";
            }

            @Nullable
            @Override
            protected ArrayList<PlaylistListData> run() throws InterruptedException {
                FeaturedPlaylists featured = null;
                for (int i = 0; featured == null && i < PreferenceUtils.getRetryCount(activity); i++) {
                    try {
                        featured = pasta.spotifyService.getFeaturedPlaylists();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (StaticUtils.shouldResendRequest(e)) Thread.sleep(200);
                        else break;
                    }
                }
                if (featured == null) return null;

                ArrayList<PlaylistListData> playlists = new ArrayList<>();
                for (PlaylistSimple playlist : featured.playlists.items) {
                    playlists.add(new PlaylistListData(playlist, pasta.me));
                }
                return playlists;
            }

            @Override
            protected void done(@Nullable ArrayList<PlaylistListData> result) {
                if (result == null) {
                    pasta.onCriticalError(activity, "featured playlists action");
                    return;
                }
                playlistFragment.swapData(result);
                notifyDataSetChanged();
            }
        }.execute();
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return albumFragment;
            case 1:
                return playlistFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "New Releases";
            case 1:
                return "Featured";
            default:
                return null;
        }
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void restoreState(Parcelable state, ClassLoader loader) {
    }
}
