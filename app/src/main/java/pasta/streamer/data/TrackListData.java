package pasta.streamer.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TrackSimple;

public class TrackListData implements Parcelable {

    public static final Parcelable.Creator<TrackListData> CREATOR = new Parcelable.Creator<TrackListData>() {
        public TrackListData createFromParcel(Parcel in) {
            return new TrackListData(in);
        }

        public TrackListData[] newArray(int size) {
            return new TrackListData[size];
        }
    };
    public String trackName, albumName, albumId, trackImage, trackImageLarge, trackDuration;
    public List<ArtistListData> artists;
    public String trackUri, trackId;

    public TrackListData(final Track track) {
        this.trackName = track.name;
        this.albumName = track.album.name;
        this.albumId = track.album.id;
        if (track.album.images.size() > 0) {
            if (track.album.images.size() > 1) this.trackImage = track.album.images.get(1).url;
            else this.trackImage = track.album.images.get(0).url;
            this.trackImageLarge = track.album.images.get(0).url;
        } else {
            this.trackImage = "";
            this.trackImageLarge = "";
        }
        this.trackDuration = String.valueOf(track.duration_ms);

        artists = new ArrayList<>();
        for (ArtistSimple artist : track.artists) {
            artists.add(new ArtistListData(artist));
        }

        this.trackUri = track.uri;
        this.trackId = track.id;
    }

    public TrackListData(final TrackSimple track, String albumName, String albumId, String trackImage, String trackImageLarge) {
        this.trackName = track.name;
        this.albumName = albumName;
        this.albumId = albumId;
        this.trackImage = trackImage;
        this.trackImageLarge = trackImageLarge;
        this.trackDuration = String.valueOf(track.duration_ms);

        artists = new ArrayList<>();
        for (ArtistSimple artist : track.artists) {
            artists.add(new ArtistListData(artist));
        }

        this.trackUri = track.uri;
        this.trackId = track.id;
    }

    public TrackListData(Parcel in) {
        ReadFromParcel(in);
    }

    public TrackListData clone() {
        try {
            return (TrackListData) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    private void ReadFromParcel(Parcel in) {
        trackName = in.readString();
        albumName = in.readString();
        albumId = in.readString();
        trackImage = in.readString();
        trackImageLarge = in.readString();
        trackDuration = in.readString();
        artists = new ArrayList<>();
        in.readList(artists, ArtistListData.class.getClassLoader());
        trackUri = in.readString();
        trackId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(trackName);
        out.writeString(albumName);
        out.writeString(albumId);
        out.writeString(trackImage);
        out.writeString(trackImageLarge);
        out.writeString(trackDuration);
        out.writeList(artists);
        out.writeString(trackUri);
        out.writeString(trackId);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
