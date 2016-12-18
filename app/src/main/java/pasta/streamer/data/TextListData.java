package pasta.streamer.data;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import pasta.streamer.R;
import pasta.streamer.views.CustomImageView;

public class TextListData extends ListData<TextListData.ViewHolder> implements Parcelable {

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

    @Override
    public ViewHolder getViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.text_item, parent, false), this);
    }

    @Override
    public void bindView(ViewHolder holder) {
        if (title != null) {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(title);
        } else holder.title.setVisibility(View.GONE);

        if (subtitle != null) {
            holder.subtitle.setVisibility(View.VISIBLE);
            holder.subtitle.setText(subtitle);
        } else holder.subtitle.setVisibility(View.GONE);

        if (image != null) {
            holder.image.setVisibility(View.VISIBLE);
            holder.image.setImageDrawable(new ColorDrawable(Color.parseColor("#bdbdbd")));

            Glide.with(holder.image.getContext()).load(image).thumbnail(0.2f).into(holder.image);
        } else holder.image.setVisibility(View.GONE);

        if (primary != null) holder.itemView.setClickable(true);
        else holder.itemView.setClickable(false);
    }

    static class ViewHolder extends ListData.ViewHolder implements View.OnClickListener {

        private TextListData listData;
        private TextView title, subtitle;
        private CustomImageView image;

        private ViewHolder(View itemView, TextListData listData) {
            super(itemView);
            this.listData = listData;
            title = (TextView) itemView.findViewById(R.id.title);
            subtitle = (TextView) itemView.findViewById(R.id.subtitle);
            image = (CustomImageView) itemView.findViewById(R.id.image);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (listData.primary != null)
                v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, listData.primary));
        }
    }
}
