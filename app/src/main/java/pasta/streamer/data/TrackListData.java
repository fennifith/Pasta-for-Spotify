package pasta.streamer.data;

import android.os.Parcel;
import android.os.Parcelable;

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
    public String trackName;
    public String albumName;
    public String albumId;
    public String trackImage;
    public String trackImageLarge;
    public String trackDuration;
    public String artistName;
    public String artistId;
    public String artistUrl;
    public String trackUri;
    public String trackId;

    public TrackListData(Track track) {
        this.trackName = track.name;
        this.albumName = track.album.name;
        this.albumId = track.album.id;
        this.trackImage = track.album.images.get(1).url;
        this.trackImageLarge = track.album.images.get(0).url;
        this.trackDuration = String.valueOf(track.duration_ms);
        this.artistName = track.artists.get(0).name;
        this.artistId = track.artists.get(0).id;
        this.artistUrl = track.artists.get(0).uri;
        this.trackUri = track.uri;
        this.trackId = track.id;
    }

    public TrackListData(TrackSimple track, String albumName, String albumId, String trackImage, String trackImageLarge) {
        this.trackName = track.name;
        this.albumName = albumName;
        this.albumId = albumId;
        this.trackImage = trackImage;
        this.trackImageLarge = trackImageLarge;
        this.trackDuration = String.valueOf(track.duration_ms);
        this.artistName = track.artists.get(0).name;
        this.artistId = track.artists.get(0).id;
        this.artistUrl = track.artists.get(0).uri;
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
        artistName = in.readString();
        artistId = in.readString();
        artistUrl = in.readString();
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
        out.writeString(artistName);
        out.writeString(artistId);
        out.writeString(artistUrl);
        out.writeString(trackUri);
        out.writeString(trackId);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
