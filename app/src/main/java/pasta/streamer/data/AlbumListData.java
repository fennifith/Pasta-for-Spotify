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

import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import pasta.streamer.R;
import pasta.streamer.fragments.AlbumFragment;
import pasta.streamer.fragments.ArtistFragment;
import pasta.streamer.utils.ImageUtils;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;

public class AlbumListData extends ListData<AlbumListData.ViewHolder> implements Parcelable {
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

    @Override
    public ViewHolder getViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.album_item_card, parent, false), this);
    }

    @Override
    public void bindView(final ViewHolder holder) {
        if (holder.artist != null) {
            if (artists.size() > 0) {
                holder.artistName.setText(artists.get(0).artistName);
                holder.artistExtra.setText(albumDate);

                new Action<ArtistListData>() {
                    @NonNull
                    @Override
                    public String id() {
                        return "gotoArtist";
                    }

                    @Nullable
                    @Override
                    protected ArtistListData run() throws InterruptedException {
                        return holder.pasta.getArtist(artists.get(0).artistId);
                    }

                    @Override
                    protected void done(@Nullable ArtistListData result) {
                        if (result == null) {
                            holder.pasta.onError(holder.activity, "artist action");
                            return;
                        }

                        holder.artist.setTag(result);
                    }
                }.execute();
            } else holder.artist.setVisibility(View.GONE);
        }

        holder.name.setText(albumName);
        holder.extra.setText(String.valueOf(tracks) + " track" + (tracks == 1 ? "" : "s"));

        if (!PreferenceUtils.isThumbnails(holder.pasta)) holder.image.setVisibility(View.GONE);
        else {
            Glide.with(holder.pasta).load(albumImage).asBitmap().placeholder(ImageUtils.getVectorDrawable(holder.pasta, R.drawable.preload)).into(new BitmapImageViewTarget(holder.image) {
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
                                    holder.artist.setBackgroundColor(Color.argb(255, Math.max(Color.red(color) - 10, 0), Math.max(Color.green(color) - 10, 0), Math.max(Color.blue(color) - 10, 0)));
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

        private AlbumListData listData;
        private TextView name, extra, artistName, artistExtra;
        private ImageView image;
        private View artist;
        private View bg;

        private ViewHolder(View itemView, AlbumListData listData) {
            super(itemView);
            this.listData = listData;
            name = (TextView) itemView.findViewById(R.id.name);
            extra = (TextView) itemView.findViewById(R.id.extra);
            image = (ImageView) itemView.findViewById(R.id.image);
            artistName = (TextView) itemView.findViewById(R.id.artist_name);
            artistExtra = (TextView) itemView.findViewById(R.id.artist_extra);
            artist = itemView.findViewById(R.id.artist);
            bg = itemView.findViewById(R.id.bg);

            itemView.findViewById(R.id.menu).setOnClickListener(this);
            itemView.findViewById(R.id.album).setOnClickListener(this);
            if (artist != null) artist.setOnClickListener(this);
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
                            return "isAlbumFav";
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
                case R.id.artist:
                    ArtistListData artist = (ArtistListData) v.getTag();
                    if (artist != null) {
                        Bundle args = new Bundle();
                        args.putParcelable("artist", artist);

                        Fragment f = new ArtistFragment();
                        f.setArguments(args);

                        activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                    } else {
                        new Action<ArtistListData>() {
                            @NonNull
                            @Override
                            public String id() {
                                return "gotoArtist";
                            }

                            @Nullable
                            @Override
                            protected ArtistListData run() throws InterruptedException {
                                return pasta.getArtist(listData.artists.get(0).artistId);
                            }

                            @Override
                            protected void done(@Nullable ArtistListData result) {
                                if (result == null) {
                                    pasta.onError(activity, "artist action");
                                    return;
                                }

                                Bundle args = new Bundle();
                                args.putParcelable("artist", result);

                                Fragment f = new ArtistFragment();
                                f.setArguments(args);

                                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                            }
                        }.execute();
                    }
                    break;
                default:
                    Bundle args = new Bundle();
                    args.putParcelable("album", listData);

                    Fragment f = new AlbumFragment();
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
                            return "favAlbum";
                        }

                        @Nullable
                        @Override
                        protected Boolean run() throws InterruptedException {
                            if (!pasta.toggleFavorite(listData)) {
                                return null;
                            } else return pasta.isFavorite(listData);
                        }

                        @Override
                        protected void done(@Nullable Boolean result) {
                            if (result == null) {
                                pasta.onError(activity, "favorite album menu action");
                                return;
                            }

                            item.setTitle(result ? R.string.unfav : R.string.fav);
                        }

                    }.execute();
                    break;
                case R.id.action_web:
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticUtils.getAlbumUrl(listData.albumId))));
                    break;
                case R.id.action_share:
                    Intent s = new Intent(android.content.Intent.ACTION_SEND);
                    s.setType("text/plain");
                    s.putExtra(Intent.EXTRA_SUBJECT, listData.albumName);
                    s.putExtra(Intent.EXTRA_TEXT, StaticUtils.getAlbumUrl(listData.albumId));
                    activity.startActivity(Intent.createChooser(s, listData.albumName));
                    break;
            }
            return false;
        }
    }
}
