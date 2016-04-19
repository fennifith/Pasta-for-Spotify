package pasta.streamer;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;

import java.util.ArrayList;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.UserPrivate;
import pasta.streamer.data.AlbumListData;
import pasta.streamer.data.ArtistListData;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.data.TrackListData;
import pasta.streamer.utils.StaticUtils;

public class Pasta extends Application {
    //below is where the client id is stored, which has been removed from the public repo for security reasons
    //instructions to obtain a client id are here: https://developer.spotify.com/web-api/tutorial/
    public final String CLIENT_ID = "INSERT_CLIENT_ID_HERE";
    public String token;
    public SpotifyApi spotifyApi;
    public SpotifyService spotifyService;
    public UserPrivate me;

    private AppCompatDialog errorDialog;

    public void onNetworkError(final Activity activity) {
        if (errorDialog == null || !errorDialog.isShowing()) {
            errorDialog = new AlertDialog.Builder(activity).setIcon(R.drawable.ic_error).setTitle(R.string.error).setMessage(R.string.error_msg).setPositiveButton(R.string.restart, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    StaticUtils.restart(activity);
                }
            }).setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    System.exit(0);
                }
            }).setCancelable(false).create();
            errorDialog.show();
        }
    }

    public void onNetworkError(Context context) {
        if (errorDialog == null || !errorDialog.isShowing()) {
            errorDialog = new AlertDialog.Builder(context).setIcon(R.drawable.ic_error).setTitle(R.string.error).setMessage(R.string.error_msg).setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    System.exit(0);
                }
            }).setCancelable(false).create();
            errorDialog.show();
        }
    }

    public ArrayList<AlbumListData> albums;
    public ArrayList<TrackListData> tracks;

    public ArrayList<AlbumListData> getFavoriteAlbums() {
        if (albums == null) {
            onNetworkError(this);
            return null;
        }
        return (ArrayList<AlbumListData>) albums.clone();
    }

    public ArrayList<TrackListData> getFavoriteTracks() {
        if (tracks == null) {
            onNetworkError(this);
            return null;
        }
        return (ArrayList<TrackListData>) tracks.clone();
    }

    public boolean toggleFavorite(PlaylistListData data) {
        try {
            if (isFavorite(data)) {
                spotifyService.unfollowPlaylist(data.playlistOwnerId, data.playlistId);
            } else {
                spotifyService.followPlaylist(data.playlistOwnerId, data.playlistId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean toggleFavorite(AlbumListData data) {
        try {
            if (isFavorite(data)) {
                spotifyService.removeFromMySavedAlbums(data.albumId);
                for (int i = 0; i < albums.size(); i++) {
                    if (albums.get(i).albumId.matches(data.albumId)) albums.remove(i);
                }
            } else {
                spotifyService.addToMySavedAlbums(data.albumId);
                albums.add(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean toggleFavorite(TrackListData data) {
        try {
            if (isFavorite(data)) {
                spotifyService.removeFromMySavedTracks(data.trackId);
                for (int i = 0; i < tracks.size(); i++) {
                    if (tracks.get(i).trackId.matches(data.trackId)) tracks.remove(i);
                }
            } else {
                spotifyService.addToMySavedTracks(data.trackId);
                tracks.add(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean toggleFavorite(ArtistListData data) {
        try {
            if (isFavorite(data)) {
                spotifyService.unfollowArtists(data.artistId);
            } else {
                spotifyService.followArtists(data.artistId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean isFavorite(PlaylistListData data) {
        return spotifyService.areFollowingPlaylist(data.playlistOwnerId, data.playlistId, me.id)[0];
    }

    public boolean isFavorite(AlbumListData data) {
        if (albums == null) return false;
        for (AlbumListData album : albums) {
            if (album.albumId.matches(data.albumId)) return true;
        }
        return false;
    }

    public boolean isFavorite(TrackListData data) {
        if (tracks == null) return false;
        for (TrackListData track : tracks) {
            if (track.trackId.matches(data.trackId)) return true;
        }
        return false;
    }

    public boolean isFavorite(ArtistListData data) {
        return spotifyService.isFollowingArtists(data.artistId)[0];
    }

}
