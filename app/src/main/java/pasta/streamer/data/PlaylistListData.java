package pasta.streamer.data;

import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.UserPrivate;

public class PlaylistListData implements Parcelable {
    public static final Parcelable.Creator<PlaylistListData> CREATOR = new Parcelable.Creator<PlaylistListData>() {
        public PlaylistListData createFromParcel(Parcel in) {
            return new PlaylistListData(in);
        }

        public PlaylistListData[] newArray(int size) {
            return new PlaylistListData[size];
        }
    };

    public String playlistName;
    public String playlistId;
    public String playlistImage;
    public String playlistImageLarge;
    public String playlistOwnerName;
    public String playlistOwnerId;
    public int tracks;
    public boolean editable;
    public boolean playlistPublic;

    public PlaylistListData(Playlist playlist, UserPrivate me) {
        playlistName = playlist.name;
        playlistId = playlist.id;
        tracks = playlist.tracks.total;
        playlistOwnerName = playlist.owner.display_name;
        playlistOwnerId = playlist.owner.id;
        editable = playlist.owner.id.matches(me.id);
        playlistPublic = playlist.is_public;

        try {
            playlistImage = playlist.images.get(playlist.images.size() / 2).url;
        } catch (IndexOutOfBoundsException e) {
            return;
        }

        playlistImageLarge = "";
        int res = 0;
        for (Image image : playlist.images) {
            if (image.height * image.width > res) {
                playlistImageLarge = image.url;
                res = image.height * image.width;
            }
        }
    }

    public PlaylistListData(PlaylistSimple playlist, UserPrivate me) {
        playlistName = playlist.name;
        playlistId = playlist.id;
        tracks = playlist.tracks.total;
        playlistOwnerName = playlist.owner.display_name;
        playlistOwnerId = playlist.owner.id;
        editable = playlist.owner.id.matches(me.id);
        if (editable) playlistPublic = playlist.is_public;

        try {
            playlistImage = playlist.images.get(playlist.images.size() / 2).url;
        } catch (IndexOutOfBoundsException e) {
            return;
        }

        playlistImageLarge = "";
        int res = 0;
        for (Image image : playlist.images) {
            try {
                if (image.height * image.width > res) {
                    playlistImageLarge = image.url;
                    res = image.height * image.width;
                }
            } catch (NullPointerException ignored) {
            }
        }
    }

    public PlaylistListData(Parcel in) {
        ReadFromParcel(in);
    }

    private void ReadFromParcel(Parcel in) {
        playlistName = in.readString();
        playlistId = in.readString();
        playlistImage = in.readString();
        playlistImageLarge = in.readString();
        playlistOwnerName = in.readString();
        playlistOwnerId = in.readString();
        tracks = in.readInt();
        editable = in.readInt() == 1;
        playlistPublic = in.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(playlistName);
        out.writeString(playlistId);
        out.writeString(playlistImage);
        out.writeString(playlistImageLarge);
        out.writeString(playlistOwnerName);
        out.writeString(playlistOwnerId);
        out.writeInt(tracks);
        out.writeInt(editable ? 1 : 0);
        out.writeInt(playlistPublic ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
