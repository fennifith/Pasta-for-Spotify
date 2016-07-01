package pasta.streamer.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

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
    public List<ArtistListData> artists;
    public int tracks;

    public AlbumListData(Album album) {
        albumName = album.name;
        albumId = album.id;
        albumDate = album.release_date;
        albumImage = album.images.get(1).url;
        albumImageLarge = album.images.get(0).url;

        artists = new ArrayList<>();
        for (ArtistSimple artist : album.artists) {
            artists.add(new ArtistListData(artist));
        }

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
        artists = new ArrayList<>();
        in.readList(artists, ArtistListData.class.getClassLoader());
        tracks = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(albumName);
        out.writeString(albumId);
        out.writeString(albumDate);
        out.writeString(albumImage);
        out.writeString(albumImageLarge);
        out.writeList(artists);
        out.writeInt(tracks);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
