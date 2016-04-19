package pasta.streamer.data;

import android.os.Parcel;
import android.os.Parcelable;

import kaaes.spotify.webapi.android.models.Artist;

public class ArtistListData implements Parcelable {
    public static final Parcelable.Creator<ArtistListData> CREATOR = new Parcelable.Creator<ArtistListData>() {
        public ArtistListData createFromParcel(Parcel in) {
            return new ArtistListData(in);
        }

        public ArtistListData[] newArray(int size) {
            return new ArtistListData[size];
        }
    };

    public String artistName;
    public String artistId;
    public String artistImage;
    public String artistImageLarge;
    public String genres;
    public int followers;

    public ArtistListData(Artist artist) {
        artistName = artist.name;
        artistId = artist.id;
        if (artist.images.size() > 1) {
            artistImage = artist.images.get(1).url;
            artistImageLarge = artist.images.get(0).url;
        } else if (artist.images.size() > 0) {
            artistImage = artist.images.get(0).url;
            artistImageLarge = artist.images.get(0).url;
        } else {
            artistImage = "";
            artistImageLarge = "";
        }
        followers = artist.followers.total;

        genres = "";
        for (String genre : artist.genres) {
            genres += genre + ", ";
        }
        if (genres.length() > 2) genres = genres.substring(0, genres.length() - 2);
    }

    public ArtistListData(Parcel in) {
        ReadFromParcel(in);
    }

    private void ReadFromParcel(Parcel in) {
        artistName = in.readString();
        artistId = in.readString();
        artistImage = in.readString();
        artistImageLarge = in.readString();
        followers = in.readInt();
        genres = in.readString();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(artistName);
        out.writeString(artistId);
        out.writeString(artistImage);
        out.writeString(artistImageLarge);
        out.writeInt(followers);
        out.writeString(genres);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}