package pasta.streamer.data;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
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

import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Track;
import kaaes.spotify.webapi.android.models.TrackSimple;
import pasta.streamer.R;
import pasta.streamer.activities.PlayerActivity;
import pasta.streamer.dialogs.AddToPlaylistDialog;
import pasta.streamer.fragments.AlbumFragment;
import pasta.streamer.fragments.ArtistFragment;
import pasta.streamer.utils.ImageUtils;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;

public class TrackListData extends ListData<TrackListData.ViewHolder> implements Parcelable {

    public static final Parcelable.Creator<TrackListData> CREATOR = new Parcelable.Creator<TrackListData>() {
        public TrackListData createFromParcel(Parcel in) {
            return new TrackListData(in);
        }

        public TrackListData[] newArray(int size) {
            return new TrackListData[size];
        }
    };

    public String trackName, albumName, albumId, trackImage, trackImageLarge, trackDuration;
    public List<ArtistListData> artists;
    public String trackUri, trackId;

    private List<TrackListData> tracks;

    public TrackListData(final Track track) {
        this.trackName = track.name;
        this.albumName = track.album.name;
        this.albumId = track.album.id;
        if (track.album.images.size() > 0) {
            if (track.album.images.size() > 1) this.trackImage = track.album.images.get(1).url;
            else this.trackImage = track.album.images.get(0).url;
            this.trackImageLarge = track.album.images.get(0).url;
        } else {
            this.trackImage = "";
            this.trackImageLarge = "";
        }
        this.trackDuration = String.valueOf(track.duration_ms);

        artists = new ArrayList<>();
        for (ArtistSimple artist : track.artists) {
            artists.add(new ArtistListData(artist));
        }

        this.trackUri = track.uri;
        this.trackId = track.id;
    }

    public TrackListData(final TrackSimple track, String albumName, String albumId, String trackImage, String trackImageLarge) {
        this.trackName = track.name;
        this.albumName = albumName;
        this.albumId = albumId;
        this.trackImage = trackImage;
        this.trackImageLarge = trackImageLarge;
        this.trackDuration = String.valueOf(track.duration_ms);

        artists = new ArrayList<>();
        for (ArtistSimple artist : track.artists) {
            artists.add(new ArtistListData(artist));
        }

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
        artists = new ArrayList<>();
        in.readList(artists, ArtistListData.class.getClassLoader());
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
        out.writeList(artists);
        out.writeString(trackUri);
        out.writeString(trackId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public ViewHolder getViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.track_item_card, parent, false), this);
    }

    @Override
    public void bindView(final ViewHolder holder) {
        holder.name.setText(trackName);
        if (artists.size() > 0)
            holder.extra.setText(artists.get(0).artistName);
        else holder.extra.setText("");

        if (!PreferenceUtils.isThumbnails(holder.activity)) holder.image.setVisibility(View.GONE);
        else {
            Glide.with(holder.activity).load(trackImage).asBitmap().placeholder(ImageUtils.getVectorDrawable(holder.activity, R.drawable.preload)).thumbnail(0.2f).into(new BitmapImageViewTarget(holder.image) {
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

    public void setTracks(List<TrackListData> tracks) {
        this.tracks = tracks;
    }

    static class ViewHolder extends ListData.ViewHolder implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {

        private TrackListData listData;

        private TextView name, extra;
        private ImageView image;
        private View bg;

        private ViewHolder(View itemView, TrackListData listData) {
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
                    inflater.inflate(R.menu.menu_track, popup.getMenu());

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
                    ArrayList<TrackListData> trackList = new ArrayList<TrackListData>();
                    if (listData.tracks != null)
                        trackList.addAll(listData.tracks);
                    else trackList.add(listData);

                    StaticUtils.play(trackList.indexOf(listData), trackList, activity);

                    ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(activity, R.anim.slide_up, R.anim.blank);
                    activity.startActivity(new Intent(activity, PlayerActivity.class), options.toBundle());
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
                            return "favTrack";
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
                                pasta.onError(activity, "favorite track action");
                                return;
                            }
                            if (result) item.setTitle(R.string.unfav);
                            else item.setTitle(R.string.fav);
                        }

                    }.execute();
                    break;
                case R.id.action_add:
                    new AddToPlaylistDialog(activity, listData).show();
                    break;
                case R.id.action_album:
                    new Action<AlbumListData>() {
                        @NonNull
                        @Override
                        public String id() {
                            return "gotoAlbum";
                        }

                        @Nullable
                        @Override
                        protected AlbumListData run() throws InterruptedException {
                            return pasta.getAlbum(listData.albumId);
                        }

                        @Override
                        protected void done(@Nullable AlbumListData result) {
                            if (result == null) {
                                pasta.onError(activity, "album menu action");
                                return;
                            }

                            Bundle args = new Bundle();
                            args.putParcelable("album", result);

                            Fragment f = new AlbumFragment();
                            f.setArguments(args);

                            activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                        }
                    }.execute();
                    break;
                case R.id.action_artist:
                    if (listData.artists.size() > 0) {
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
                                    pasta.onError(activity, "artist menu action");
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
            }
            return false;
        }
    }
}
