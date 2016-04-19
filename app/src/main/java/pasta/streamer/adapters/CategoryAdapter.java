package pasta.streamer.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.async.Action;

import java.util.ArrayList;

import pasta.streamer.R;
import pasta.streamer.data.CategoryListData;
import pasta.streamer.fragments.CategoryFragment;
import pasta.streamer.utils.Downloader;
import pasta.streamer.views.CustomImageView;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    AppCompatActivity activity;
    ArrayList<CategoryListData> list;
    Drawable preload;

    public CategoryAdapter(AppCompatActivity activity, ArrayList<CategoryListData> list) {
        this.activity = activity;
        this.list = list;
        preload = ContextCompat.getDrawable(activity, R.drawable.preload);
    }

    public void swapData(ArrayList<CategoryListData> list) {
        this.list = list;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return new ViewHolder(inflater.inflate(R.layout.category_item, null));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        ((CustomImageView) holder.v.findViewById(R.id.image)).setImageDrawable(preload);
        new Action<Bitmap>() {
            @NonNull
            @Override
            public String id() {
                return "getCategoryImage";
            }

            @Nullable
            @Override
            protected Bitmap run() throws InterruptedException {
                return Downloader.downloadImage(activity, list.get(holder.getAdapterPosition()).categoryImage);
            }

            @Override
            protected void done(@Nullable Bitmap result) {
                if (result == null) return;
                ((CustomImageView) holder.v.findViewById(R.id.image)).transition(new BitmapDrawable(activity.getResources(), result));
            }
        }.execute();

        ((TextView) holder.v.findViewById(R.id.title)).setText(list.get(position).categoryName);

        holder.v.findViewById(R.id.bg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle args = new Bundle();
                args.putParcelable("category", list.get(holder.getAdapterPosition()));

                Fragment f = new CategoryFragment();
                f.setArguments(args);

                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View v;

        public ViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }
}
