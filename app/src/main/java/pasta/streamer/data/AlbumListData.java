package pasta.streamer.data;

import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.ArtistSimple;

public class AlbumListData implements Parcelable {
    public static final Creator<AlbumListData> CREATOR = new Creator<AlbumListData>() {
        public AlbumListData createFromParcel(Parcel in) {
            return new AlbumListData(in);
        }

        public AlbumListData[] newArray(int size) {
            return new AlbumListData[size];
        }
    };

    public String albumName;
    public String albumId;
    public String albumDate;
    public String albumImage;
    public String albumImageLarge;
    public String artistName;
    public String artistId;
    public String artistImage;
    public int tracks;

    public AlbumListData(Album album, String artistImage) {
        albumName = album.name;
        albumId = album.id;
        albumDate = album.release_date;
        albumImage = album.images.get(1).url;
        albumImageLarge = album.images.get(0).url;
        ArtistSimple artist = album.artists.get(0);
        artistName = artist.name;
        artistId = artist.id;
        this.artistImage = artistImage;
        tracks = album.tracks.items.size();
    }

    public AlbumListData(Parcel in) {
        ReadFromParcel(in);
    }

    private void ReadFromParcel(Parcel in) {
        albumName = in.readString();
        albumId = in.readString();
        albumDate = in.readString();
        albumImage = in.readString();
        albumImageLarge = in.readString();
        artistName = in.readString();
        artistId = in.readString();
        artistImage = in.readString();
        tracks = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(albumName);
        out.writeString(albumId);
        out.writeString(albumDate);
        out.writeString(albumImage);
        out.writeString(albumImageLarge);
        out.writeString(artistName);
        out.writeString(artistId);
        out.writeString(artistImage);
        out.writeInt(tracks);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
