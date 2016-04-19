package pasta.streamer.data;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class TextListData implements Parcelable {

    public static final Creator<TextListData> CREATOR = new Creator<TextListData>() {
        public TextListData createFromParcel(Parcel in) {
            return new TextListData(in);
        }

        public TextListData[] newArray(int size) {
            return new TextListData[size];
        }
    };

    public String image, title, subtitle;
    public Uri primary;

    public TextListData(String image, String title, String subtitle, Uri primary) {
        this.image = image;
        this.title = title;
        this.subtitle = subtitle;
        this.primary = primary;
    }

    public TextListData(Parcel in) {
        ReadFromParcel(in);
    }

    private void ReadFromParcel(Parcel in) {
        image = in.readString();
        title = in.readString();
        subtitle = in.readString();
        primary = Uri.parse(in.readString());
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(image);
        out.writeString(title);
        out.writeString(subtitle);
        out.writeString(primary.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
