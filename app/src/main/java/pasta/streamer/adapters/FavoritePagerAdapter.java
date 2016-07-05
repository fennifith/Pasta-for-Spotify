package pasta.streamer.adapters;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.afollestad.async.Action;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistsCursorPager;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import pasta.streamer.Pasta;
import pasta.streamer.data.ArtistListData;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.fragments.OmniFragment;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;

public class FavoritePagerAdapter extends FragmentStatePagerAdapter {

    Activity activity;
    Pasta pasta;

    OmniFragment playlistFragment;
    OmniFragment albumFragment;
    OmniFragment trackFragment;
    OmniFragment artistFragment;

    public FavoritePagerAdapter(Activity activity, FragmentManager manager) {
        super(manager);
        this.activity = activity;
        pasta = (Pasta) activity.getApplicationContext();

        Bundle args = new Bundle();
        args.putBoolean("favorite", true);

        playlistFragment = new OmniFragment();
        playlistFragment.setArguments(args);

        albumFragment = new OmniFragment();
        albumFragment.setArguments(args);

        trackFragment = new OmniFragment();
        trackFragment.setArguments(args);

        artistFragment = new OmniFragment();
        artistFragment.setArguments(args);

        load();
    }

    public void load() {
        playlistFragment.clear();
        albumFragment.clear();
        trackFragment.clear();
        artistFragment.clear();

        new Action<ArrayList<PlaylistListData>>() {
            @NonNull
            @Override
            public String id() {
                return "getFavPlaylists";
            }

            @Nullable
            @Override
            protected ArrayList<PlaylistListData> run() throws InterruptedException {
                ArrayList<PlaylistListData> playlists = new ArrayList<>();
                Pager<PlaylistSimple> my = pasta.getMyPlaylists();
                if (my == null) return null;

                for (PlaylistSimple playlist : my.items) {
                    playlists.add(new PlaylistListData(playlist, pasta.me));
                }

                return playlists;
            }

            @Override
            protected void done(@Nullable ArrayList<PlaylistListData> result) {
                if (result == null) {
                    pasta.onError(activity, "favorite playlist action");
                    return;
                }
                playlistFragment.swapData(result);
            }
        }.execute();

        albumFragment.swapData(pasta.getFavoriteAlbums()) ;
        trackFragment.swapData(pasta.getFavoriteTracks());

        new Action<ArrayList<ArtistListData>>() {
            @NonNull
            @Override
            public String id() {
                return "getFavArtists";
            }

            @Nullable
            @Override
            protected ArrayList<ArtistListData> run() throws InterruptedException {
                ArtistsCursorPager followed = null;
                for (int i = 0; followed == null && i < PreferenceUtils.getRetryCount(activity); i++) {
                    try {
                        followed = pasta.spotifyService.getFollowedArtists();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (StaticUtils.shouldResendRequest(e)) Thread.sleep(200);
                        else break;
                    }
                }
                if (followed == null) return null;

                ArrayList<ArtistListData> artists = new ArrayList<>();
                for (Artist artist : followed.artists.items) {
                    artists.add(new ArtistListData(artist));
                }

                return artists;
            }

            @Override
            protected void done(@Nullable ArrayList<ArtistListData> result) {
                if (result == null) {
                    pasta.onError(activity, "favorite artist action");
                    return;
                }
                artistFragment.swapData(result);
            }
        }.execute();
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return playlistFragment;
            case 1:
                return albumFragment;
            case 2:
                return trackFragment;
            case 3:
                return artistFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 4;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Playlists";
            case 1:
                return "Albums";
            case 2:
                return "Songs";
            case 3:
                return "Artists";
            default:
                return null;
        }
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();

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
