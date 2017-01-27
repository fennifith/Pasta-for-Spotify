package pasta.streamer.data;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.async.Action;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;

import java.util.ArrayList;
import java.util.List;

import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import pasta.streamer.R;
import pasta.streamer.fragments.ArtistFragment;
import pasta.streamer.utils.ImageUtils;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;

public class ArtistListData extends ListData<ArtistListData.ViewHolder> implements Parcelable {
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
    public int followers = -1;

    @Nullable
    public String artistImage, artistImageLarge;

    @Nullable
    public List<String> genres;

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
        genres = artist.genres;
    }

    public ArtistListData(ArtistSimple artist) {
        artistName = artist.name;
        artistId = artist.id;
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
        genres = new ArrayList<>();
        in.readStringList(genres);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(artistName);
        out.writeString(artistId);
        out.writeString(artistImage);
        out.writeString(artistImageLarge);
        out.writeInt(followers);
        out.writeStringList(genres);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public ViewHolder getViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.artist_item_card, parent, false), this);
    }

    @Override
    public void bindView(final ViewHolder holder) {
        holder.name.setText(artistName);
        holder.extra.setText(String.valueOf(followers) + " followers");

        if (!PreferenceUtils.isThumbnails(holder.activity)) holder.image.setVisibility(View.GONE);
        else {
            Glide.with(holder.activity).load(artistImage).asBitmap().placeholder(ImageUtils.getVectorDrawable(holder.activity, R.drawable.preload)).thumbnail(0.2f).into(new BitmapImageViewTarget(holder.image) {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    super.onResourceReady(resource, glideAnimation);

                    if (holder.bg == null) return;
                    Palette.from(resource).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), Color.DKGRAY, palette.getDarkVibrantColor(Color.DKGRAY));
                            animator.setDuration(250);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    int color = (int) animation.getAnimatedValue();
                                    holder.bg.setBackgroundColor(color);
                                }
                            });
                            animator.start();
                        }
                    });
                }
            });
        }
    }

    static class ViewHolder extends ListData.ViewHolder implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        private ArtistListData listData;

        private TextView name, extra;
        private ImageView image;
        private View bg;

        private ViewHolder(View itemView, ArtistListData listData) {
            super(itemView);
            this.listData = listData;

            name = (TextView) itemView.findViewById(R.id.name);
            extra = (TextView) itemView.findViewById(R.id.extra);
            image = (ImageView) itemView.findViewById(R.id.image);
            bg = itemView.findViewById(R.id.bg);

            itemView.findViewById(R.id.menu).setOnClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.menu:
                    PopupMenu popup = new PopupMenu(v.getContext(), v);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.menu_basic, popup.getMenu());

                    final MenuItem fav = popup.getMenu().findItem(R.id.action_fav);
                    new Action<Boolean>() {
                        @NonNull
                        @Override
                        public String id() {
                            return "isTrackFav";
                        }

                        @Nullable
                        @Override
                        protected Boolean run() throws InterruptedException {
                            return pasta.isFavorite(listData);
                        }

                        @Override
                        protected void done(@Nullable Boolean result) {
                            if (result == null) return;
                            if (result) {
                                fav.setTitle(R.string.unfav);
                            } else {
                                fav.setTitle(R.string.fav);
                            }
                        }

                    }.execute();

                    popup.setOnMenuItemClickListener(this);
                    popup.show();
                    break;
                default:
                    Bundle args = new Bundle();
                    args.putParcelable("artist", listData);

                    Fragment f = new ArtistFragment();
                    f.setArguments(args);

                    activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                    break;
            }
        }

        @Override
        public boolean onMenuItemClick(final MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_fav:
                    new Action<Boolean>() {
                        @NonNull
                        @Override
                        public String id() {
                            return "favArtist";
                        }

                        @Nullable
                        @Override
                        protected Boolean run() throws InterruptedException {
                            if (!pasta.toggleFavorite(listData)) {
                                return null;
                            } else
                                return pasta.isFavorite(listData);
                        }

                        @Override
                        protected void done(@Nullable Boolean result) {
                            if (result == null) {
                                pasta.onError(activity, "favorite artist menu action");
                                return;
                            }
                            if (result)
                                item.setTitle(R.string.unfav);
                            else item.setTitle(R.string.fav);
                        }

                    }.execute();
                    break;
                case R.id.action_web:
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticUtils.getArtistUrl(listData.artistId))));
                    break;
                case R.id.action_share:
                    Intent s = new Intent(android.content.Intent.ACTION_SEND);
                    s.setType("text/plain");
                    s.putExtra(Intent.EXTRA_SUBJECT, listData.artistName);
                    s.putExtra(Intent.EXTRA_TEXT, StaticUtils.getArtistUrl(listData.artistId));
                    activity.startActivity(Intent.createChooser(s, listData.artistName));
                    break;
            }
            return false;
        }
    }
}