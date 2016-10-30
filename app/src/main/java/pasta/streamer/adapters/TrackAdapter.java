package pasta.streamer.adapters;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.async.Action;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kaaes.spotify.webapi.android.models.TrackToRemove;
import kaaes.spotify.webapi.android.models.TracksToRemove;
import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.activities.PlayerActivity;
import pasta.streamer.data.AlbumListData;
import pasta.streamer.data.ArtistListData;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.data.TrackListData;
import pasta.streamer.dialogs.AddToPlaylistDialog;
import pasta.streamer.fragments.AlbumFragment;
import pasta.streamer.fragments.ArtistFragment;
import pasta.streamer.utils.ImageUtils;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;
import pasta.streamer.views.CustomImageView;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {

    private ArrayList<TrackListData> original;
    private ArrayList<TrackListData> list;
    private AppCompatActivity activity;
    private Pasta pasta;
    private int menuRes = R.menu.menu_track;
    private PlaylistListData playlistdata;
    private boolean thumbnails, cards, trackList, palette, dark;

    public TrackAdapter(AppCompatActivity activity, ArrayList<TrackListData> list) {
        original = list;
        if (list != null) {
            this.list = new ArrayList<>();
            try {
                this.list.addAll(original);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.activity = activity;
        pasta = (Pasta) activity.getApplicationContext();

        thumbnails = PreferenceUtils.isThumbnails(activity);
        cards = PreferenceUtils.isCards(activity);
        trackList = PreferenceUtils.isListTracks(activity);
        palette = PreferenceUtils.isPalette(activity);
        dark = PreferenceUtils.isDarkTheme(activity);
    }

    public void setPlaylistBehavior(PlaylistListData data) {
        menuRes = R.menu.menu_playlist_track;
        playlistdata = data;
    }

    public void setAlbumBehavior() {
        menuRes = R.menu.menu_album_track;
    }

    public void swapData(ArrayList<TrackListData> list) {
        original = list;
        this.list = new ArrayList<>();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public void removeData(int position) {
        if (position < 0 || position > original.size()) return;
        original.remove(original.indexOf(list.get(position)));
        list.remove(position);
        notifyItemRemoved(position);
    }

    public void removeData(TrackListData data) {
        original.remove(original.indexOf(data));
        int pos = list.indexOf(data);
        list.remove(pos);
        notifyItemRemoved(pos);
    }

    public void sort(int order) {
        switch (order) {
            case PreferenceUtils.ORDER_ADDED:
                list = new ArrayList<>();
                list.addAll(original);
                break;
            case PreferenceUtils.ORDER_NAME:
                Collections.sort(list, new Comparator<TrackListData>() {
                    public int compare(TrackListData first, TrackListData second) {
                        return first.trackName.compareTo(second.trackName);
                    }
                });
                break;
            case PreferenceUtils.ORDER_ARTIST:
                Collections.sort(list, new Comparator<TrackListData>() {
                    public int compare(TrackListData first, TrackListData second) {
                        if (first.artists.size() > 0 && second.artists.size() > 0)
                            return first.artists.get(0).artistName.compareTo(second.artists.get(0).artistName);
                        else return 0;
                    }
                });
                break;
            case PreferenceUtils.ORDER_ALBUM:
                Collections.sort(list, new Comparator<TrackListData>() {
                    public int compare(TrackListData first, TrackListData second) {
                        return first.albumName.compareTo(second.albumName);
                    }
                });
                break;
            case PreferenceUtils.ORDER_LENGTH:
                Collections.sort(list, new Comparator<TrackListData>() {
                    public int compare(TrackListData first, TrackListData second) {
                        return first.trackDuration.compareTo(second.trackDuration);
                    }
                });
                break;
            case PreferenceUtils.ORDER_RANDOM:
                Collections.shuffle(list);
                break;
        }
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            return new ArtistsViewHolder(LayoutInflater.from(activity).inflate(R.layout.artists_item, parent, false));
        } else {
            return new TrackViewHolder(LayoutInflater.from(activity).inflate(trackList ? R.layout.track_item : (cards ? R.layout.track_item_card : R.layout.track_item_tile), parent, false));
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (holder instanceof ArtistsViewHolder) {
            new Action<ArrayList<ArtistListData>>() {
                @NonNull
                @Override
                public String id() {
                    return "getCommonArtists";
                }

                @Nullable
                @Override
                protected ArrayList<ArtistListData> run() throws InterruptedException {
                    ArrayList<ArtistListData> allArtists = new ArrayList<>();
                    for (TrackListData track : list) {
                        allArtists.addAll(track.artists);
                    }

                    HashMap<String, Integer> frequencies = new HashMap<>();

                    for (int i = 0; i < allArtists.size(); i++) {
                        int frequency = 0;
                        ArtistListData artist1 = allArtists.get(i);
                        for (ArtistListData artist2 : allArtists) {
                            if (artist1.artistId.matches(artist2.artistId)) frequency++;
                        }

                        frequencies.put(artist1.artistId, frequency);
                    }

                    List<Map.Entry<String, Integer>> entries = new ArrayList<>(frequencies.entrySet());
                    Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
                        public int compare(Map.Entry<String, Integer> lhs, Map.Entry<String, Integer> rhs) {
                            return lhs.getValue() < rhs.getValue() ? -1 : (lhs.getValue().equals(rhs.getValue()) ? 0 : 1);
                        }
                    });

                    ArrayList<ArtistListData> commonArtists = new ArrayList<>();
                    for (int i = 0; i < 5 && i < entries.size(); i++) {
                        ArtistListData artist = pasta.getArtist(entries.get(i).getKey());
                        if (artist != null) commonArtists.add(artist);
                    }

                    return commonArtists;
                }

                @Override
                protected void done(@Nullable ArrayList<ArtistListData> result) {
                    if (result != null && result.size() > 0) {
                        FlexboxLayout layout = (FlexboxLayout) holder.v.findViewById(R.id.artists);
                        layout.removeAllViewsInLayout();

                        for (ArtistListData artist : result) {
                            View v = LayoutInflater.from(activity).inflate(R.layout.artist_item_chip, null);
                            ((TextView) v.findViewById(R.id.title)).setText(artist.artistName);
                            Glide.with(activity).load(artist.artistImage).thumbnail(0.2f).into(new GlideDrawableImageViewTarget((ImageView) v.findViewById(R.id.image)) {
                                @Override
                                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                                    super.onResourceReady(resource, animation);
                                }
                            });

                            v.setTag(artist);
                            v.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Bundle args = new Bundle();
                                    args.putParcelable("artist", (ArtistListData) v.getTag());

                                    Fragment f = new ArtistFragment();
                                    f.setArguments(args);

                                    activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                                }
                            });
                            layout.addView(v);
                        }

                        layout.setVisibility(View.VISIBLE);
                    } else holder.v.findViewById(R.id.artists).setVisibility(View.GONE);
                }
            }.execute();


        } else if (holder instanceof TrackViewHolder) {
            position -= 1;

            View trackMenu = holder.v.findViewById(R.id.menu);
            if (trackMenu.getVisibility() == View.GONE) {
                trackMenu.setVisibility(View.VISIBLE);
            }

            trackMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popup = new PopupMenu(v.getContext(), v);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(menuRes, popup.getMenu());

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
                            return pasta.isFavorite(list.get(holder.getAdapterPosition() - 1));
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

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
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
                                            if (!pasta.toggleFavorite(list.get(holder.getAdapterPosition() - 1))) {
                                                return null;
                                            } else
                                                return pasta.isFavorite(list.get(holder.getAdapterPosition() - 1));
                                        }

                                        @Override
                                        protected void done(@Nullable Boolean result) {
                                            if (result == null) {
                                                pasta.onError(activity, "favorite track menu action");
                                                return;
                                            }
                                            if (result) item.setTitle(R.string.unfav);
                                            else {
                                                item.setTitle(R.string.fav);
                                            }
                                        }

                                    }.execute();
                                    break;
                                case R.id.action_add:
                                    new AddToPlaylistDialog(activity, list.get(holder.getAdapterPosition() - 1)).show();
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
                                            return pasta.getAlbum(list.get(holder.getAdapterPosition() - 1).albumId);
                                        }

                                        @Override
                                        protected void done(@Nullable AlbumListData result) {
                                            Bundle args = new Bundle();
                                            args.putParcelable("album", result);

                                            Fragment f = new AlbumFragment();
                                            f.setArguments(args);

                                            activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                                        }
                                    }.execute();
                                    break;
                                case R.id.action_artist:
                                    TrackListData track = list.get(holder.getAdapterPosition() - 1);
                                    if (track.artists.size() > 0) {
                                        new Action<ArtistListData>() {
                                            @NonNull
                                            @Override
                                            public String id() {
                                                return "gotoArtist";
                                            }

                                            @Nullable
                                            @Override
                                            protected ArtistListData run() throws InterruptedException {
                                                return pasta.getArtist(list.get(holder.getAdapterPosition() - 1).artists.get(0).artistId);
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
                                case R.id.action_playlist_remove:
                                    new Action<Boolean>() {
                                        @NonNull
                                        @Override
                                        public String id() {
                                            return "removeFromPlaylist";
                                        }

                                        @Nullable
                                        @Override
                                        protected Boolean run() throws InterruptedException {
                                            TracksToRemove tracksRemove = new TracksToRemove();
                                            TrackToRemove trackRemove = new TrackToRemove();
                                            trackRemove.uri = list.get(holder.getAdapterPosition() - 1).trackUri;
                                            tracksRemove.tracks = Collections.singletonList(trackRemove);
                                            if (playlistdata != null) {
                                                pasta.spotifyService.removeTracksFromPlaylist(playlistdata.playlistOwnerId, playlistdata.playlistId, tracksRemove);
                                                return true;
                                            } else return false;
                                        }

                                        @Override
                                        protected void done(@Nullable Boolean result) {
                                            if (result == null || !result) {
                                                pasta.onError(activity, "remove (from playlist) track action");
                                                return;
                                            }
                                            pasta.showToast(activity.getString(R.string.playlist_removed));
                                            removeData(list.get(holder.getAdapterPosition() - 1));
                                        }
                                    }.execute();
                                    break;
                            }
                            return false;
                        }
                    });
                    popup.show();
                }
            });

            TrackListData trackData = list.get(position);

            ((TextView) holder.v.findViewById(R.id.name)).setText(trackData.trackName);
            TextView extra = (TextView) holder.v.findViewById(R.id.extra);
            if (trackData.artists.size() > 0) extra.setText(trackData.artists.get(0).artistName);

            holder.v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ArrayList<TrackListData> trackList = new ArrayList<>();
                    trackList.addAll(list);
                    StaticUtils.play(holder.getAdapterPosition() - 1, trackList, activity);

                    Bundle bundle = ActivityOptionsCompat.makeScaleUpAnimation(view, (int) view.getX(), (int) view.getY(), view.getWidth(), view.getHeight()).toBundle();
                    activity.startActivity(new Intent(activity, PlayerActivity.class), bundle);
                }
            });

            View bg = holder.v.findViewById(R.id.bg);
            if (bg != null) bg.setBackgroundColor(dark ? Color.DKGRAY : Color.WHITE);

            Glide.with(activity).load(list.get(position).trackImage).asBitmap().placeholder(ImageUtils.getVectorDrawable(activity, R.drawable.preload)).into(new BitmapImageViewTarget((ImageView) holder.v.findViewById(R.id.image)) {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    ((CustomImageView) getView()).transition(resource);

                    View bg = holder.v.findViewById(R.id.bg);
                    if (!thumbnails || !palette || bg == null) return;
                    Palette.from(resource).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            int defaultColor = dark ? Color.DKGRAY : Color.WHITE;
                            int color = palette.getLightVibrantColor(defaultColor);
                            if (dark) color = palette.getDarkVibrantColor(defaultColor);

                            ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), defaultColor, color);
                            animator.setDuration(250);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    int color = (int) animation.getAnimatedValue();
                                    holder.v.findViewById(R.id.bg).setBackgroundColor(color);

                                    View artist = holder.v.findViewById(R.id.artist);
                                    if (artist != null)
                                        artist.setBackgroundColor(Color.argb(255, Math.max(Color.red(color) - 10, 0), Math.max(Color.green(color) - 10, 0), Math.max(Color.blue(color) - 10, 0)));
                                }
                            });
                            animator.start();
                        }
                    });
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }

    public static class TrackViewHolder extends ViewHolder {
        public View v;

        public TrackViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }

    public static class ArtistsViewHolder extends ViewHolder {
        public View v;

        public ArtistsViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View v;

        public ViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }
}
