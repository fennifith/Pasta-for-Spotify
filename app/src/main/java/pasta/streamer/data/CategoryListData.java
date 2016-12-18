package pasta.streamer.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import kaaes.spotify.webapi.android.models.Category;
import pasta.streamer.R;
import pasta.streamer.fragments.CategoryFragment;

public class CategoryListData extends ListData<CategoryListData.ViewHolder> implements Parcelable {
    public static final Creator<CategoryListData> CREATOR = new Creator<CategoryListData>() {
        public CategoryListData createFromParcel(Parcel in) {
            return new CategoryListData(in);
        }

        public CategoryListData[] newArray(int size) {
            return new CategoryListData[size];
        }
    };

    public String categoryId;
    public String categoryName;
    public String categoryImage;

    public CategoryListData(Category category) {
        categoryId = category.id;
        categoryName = category.name;
        try {
            categoryImage = category.icons.get(0).url;
        } catch (IndexOutOfBoundsException e) {
            categoryImage = "";
        }
    }

    public CategoryListData(Parcel in) {
        ReadFromParcel(in);
    }

    private void ReadFromParcel(Parcel in) {
        categoryId = in.readString();
        categoryName = in.readString();
        categoryImage = in.readString();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(categoryId);
        out.writeString(categoryName);
        out.writeString(categoryImage);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public ViewHolder getViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.category_item, parent, false), this);
    }

    @Override
    public void bindView(ViewHolder holder) {
        Glide.with(holder.image.getContext()).load(categoryImage).thumbnail(0.2f).into(holder.image);
        holder.title.setText(categoryName);
    }

    static class ViewHolder extends ListData.ViewHolder implements View.OnClickListener {

        private CategoryListData listData;
        private TextView title;
        private ImageView image;

        private ViewHolder(View itemView, CategoryListData listData) {
            super(itemView);
            this.listData = listData;
            title = (TextView) itemView.findViewById(R.id.title);
            image = (ImageView) itemView.findViewById(R.id.image);
            itemView.findViewById(R.id.bg).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Bundle args = new Bundle();
            args.putParcelable("category", listData);

            Fragment f = new CategoryFragment();
            f.setArguments(args);

            ((AppCompatActivity) v.getContext()).getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
        }
    }
}
