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

import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.UserPrivate;
import pasta.streamer.R;
import pasta.streamer.dialogs.NewPlaylistDialog;
import pasta.streamer.fragments.PlaylistFragment;
import pasta.streamer.utils.ImageUtils;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;

public class PlaylistListData extends ListData<PlaylistListData.ViewHolder> implements Parcelable {
    public static final Parcelable.Creator<PlaylistListData> CREATOR = new Parcelable.Creator<PlaylistListData>() {
        public PlaylistListData createFromParcel(Parcel in) {
            return new PlaylistListData(in);
        }

        public PlaylistListData[] newArray(int size) {
            return new PlaylistListData[size];
        }
    };

    public String playlistName;
    public String playlistId;
    public String playlistImage;
    public String playlistImageLarge;
    public String playlistOwnerName;
    public String playlistOwnerId;
    public int tracks;
    public boolean editable;
    public boolean playlistPublic;

    public PlaylistListData(Playlist playlist, UserPrivate me) {
        playlistName = playlist.name;
        playlistId = playlist.id;
        tracks = playlist.tracks.total;
        playlistOwnerName = playlist.owner.display_name;
        playlistOwnerId = playlist.owner.id;
        editable = playlist.owner.id.matches(me.id);
        playlistPublic = playlist.is_public;

        try {
            playlistImage = playlist.images.get(playlist.images.size() / 2).url;
        } catch (IndexOutOfBoundsException e) {
            return;
        }

        playlistImageLarge = "";
        int res = 0;
        for (Image image : playlist.images) {
            if (image.height * image.width > res) {
                playlistImageLarge = image.url;
                res = image.height * image.width;
            }
        }
    }

    public PlaylistListData(PlaylistSimple playlist, UserPrivate me) {
        playlistName = playlist.name;
        playlistId = playlist.id;
        tracks = playlist.tracks.total;
        playlistOwnerName = playlist.owner.display_name;
        playlistOwnerId = playlist.owner.id;
        editable = playlist.owner.id.matches(me.id);
        if (editable) playlistPublic = playlist.is_public;

        try {
            playlistImage = playlist.images.get(playlist.images.size() / 2).url;
        } catch (IndexOutOfBoundsException e) {
            return;
        }

        playlistImageLarge = "";
        int res = 0;
        for (Image image : playlist.images) {
            try {
                if (image.height * image.width > res) {
                    playlistImageLarge = image.url;
                    res = image.height * image.width;
                }
            } catch (NullPointerException ignored) {
            }
        }
    }

    public PlaylistListData(Parcel in) {
        ReadFromParcel(in);
    }

    private void ReadFromParcel(Parcel in) {
        playlistName = in.readString();
        playlistId = in.readString();
        playlistImage = in.readString();
        playlistImageLarge = in.readString();
        playlistOwnerName = in.readString();
        playlistOwnerId = in.readString();
        tracks = in.readInt();
        editable = in.readInt() == 1;
        playlistPublic = in.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(playlistName);
        out.writeString(playlistId);
        out.writeString(playlistImage);
        out.writeString(playlistImageLarge);
        out.writeString(playlistOwnerName);
        out.writeString(playlistOwnerId);
        out.writeInt(tracks);
        out.writeInt(editable ? 1 : 0);
        out.writeInt(playlistPublic ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public ViewHolder getViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.playlist_item_card, parent, false), this);
    }

    @Override
    public void bindView(final ViewHolder holder) {
        holder.name.setText(playlistName);
        holder.extra.setText(String.format("%d %s", tracks, tracks == 1 ? "track" : "tracks"));

        if (!PreferenceUtils.isThumbnails(holder.activity)) holder.image.setVisibility(View.GONE);
        else {
            Glide.with(holder.activity).load(playlistImage).asBitmap().placeholder(ImageUtils.getVectorDrawable(holder.activity, R.drawable.preload)).thumbnail(0.2f).into(new BitmapImageViewTarget(holder.image) {
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

        private PlaylistListData listData;

        private TextView name, extra;
        private ImageView image;
        private View bg;

        private ViewHolder(View itemView, PlaylistListData listData) {
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
                    inflater.inflate(R.menu.menu_playlist, popup.getMenu());

                    final MenuItem fav = popup.getMenu().findItem(R.id.action_fav);
                    new Action<Boolean>() {
                        @NonNull
                        @Override
                        public String id() {
                            return "isPlaylistFav";
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
                    popup.getMenu().findItem(R.id.action_edit).setVisible(listData.editable);
                    popup.show();
                    break;
                default:
                    Bundle args = new Bundle();
                    args.putParcelable("playlist", listData);

                    Fragment f = new PlaylistFragment();
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
                            return "favPlaylist";
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
                                pasta.onError(activity, "favorite playlist menu action");
                                return;
                            }
                            if (result) {
                                item.setTitle(R.string.unfav);
                            } else {
                                item.setTitle(R.string.fav);
                            }
                        }

                    }.execute();
                    break;
                case R.id.action_edit:
                    new NewPlaylistDialog(activity).setPlaylist(listData).show();
                    break;
                case R.id.action_web:
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticUtils.getPlaylistUrl(listData.playlistOwnerId, listData.playlistId))));
                    break;
                case R.id.action_share:
                    Intent s = new Intent(android.content.Intent.ACTION_SEND);
                    s.setType("text/plain");
                    s.putExtra(Intent.EXTRA_SUBJECT, listData.playlistName);
                    s.putExtra(Intent.EXTRA_TEXT, StaticUtils.getPlaylistUrl(listData.playlistOwnerId, listData.playlistId));
                    activity.startActivity(Intent.createChooser(s, listData.playlistName));
                    break;
            }
            return false;
        }
    }
}
