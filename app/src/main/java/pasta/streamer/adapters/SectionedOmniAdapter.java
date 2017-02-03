package pasta.streamer.adapters;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
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
import android.widget.TextView;

import com.afollestad.async.Action;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.activities.PlayerActivity;
import pasta.streamer.data.AlbumListData;
import pasta.streamer.data.ArtistListData;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.data.TrackListData;
import pasta.streamer.fragments.AlbumFragment;
import pasta.streamer.fragments.ArtistFragment;
import pasta.streamer.fragments.PlaylistFragment;
import pasta.streamer.utils.ImageUtils;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;
import pasta.streamer.views.CustomImageView;

public class SectionedOmniAdapter extends RecyclerView.Adapter<SectionedOmniAdapter.ViewHolder> {

    private ArrayList<TrackListData> tracks;
    private ArrayList<AlbumListData> albums;
    private ArrayList<PlaylistListData> playlists;
    private ArrayList<ArtistListData> artists;
    private AppCompatActivity activity;
    private ArrayList list;
    private boolean isThumbnails;
    private Pasta pasta;

    public SectionedOmniAdapter(AppCompatActivity activity, ArrayList list) {
        tracks = new ArrayList<>();
        albums = new ArrayList<>();
        playlists = new ArrayList<>();
        artists = new ArrayList<>();

        this.activity = activity;
        pasta = (Pasta) activity.getApplicationContext();

        isThumbnails = PreferenceUtils.isThumbnails(activity);


        if (list == null) {
            this.list = new ArrayList();
            return;
        }
        this.list = list;
        for (Object o : list) {
            if (o instanceof TrackListData) tracks.add((TrackListData) o);
            else if (o instanceof AlbumListData) albums.add((AlbumListData) o);
            else if (o instanceof PlaylistListData) playlists.add((PlaylistListData) o);
            else if (o instanceof ArtistListData) artists.add((ArtistListData) o);
        }
    }

    public void addData(Parcelable data) {
        int pos, offset;
        if (data instanceof TrackListData) {
            pos = tracks.size();
            offset = 1;
            tracks.add((TrackListData) data);
        } else if (data instanceof AlbumListData) {
            pos = tracks.size() + albums.size();
            offset = 2;
            albums.add((AlbumListData) data);
        } else if (data instanceof PlaylistListData) {
            pos = tracks.size() + albums.size() + playlists.size();
            offset = 3;
            playlists.add((PlaylistListData) data);
        } else if (data instanceof ArtistListData) {
            pos = tracks.size() + albums.size() + playlists.size() + artists.size();
            offset = 4;
            artists.add((ArtistListData) data);
        } else {
            return;
        }
        list.add(pos, data);
        notifyItemInserted(pos + offset);
    }

    public void addData(ArrayList datas) {
        datas.addAll(list);
        swapData(datas);
    }

    public void swapData(ArrayList list) {
        this.list = list;

        tracks.clear();
        albums.clear();
        playlists.clear();
        artists.clear();

        for (Object o : list) {
            if (o instanceof TrackListData) tracks.add((TrackListData) o);
            else if (o instanceof AlbumListData) albums.add((AlbumListData) o);
            else if (o instanceof PlaylistListData) playlists.add((PlaylistListData) o);
            else if (o instanceof ArtistListData) artists.add((ArtistListData) o);
        }

        Collections.sort(list, new Comparator() {
            @Override
            public int compare(Object lhs, Object rhs) {
                int comparison = 0;

                if (lhs instanceof TrackListData) comparison += 1;
                else if (lhs instanceof AlbumListData) comparison += 2;
                else if (lhs instanceof PlaylistListData) comparison += 3;
                else if (lhs instanceof ArtistListData) comparison += 4;

                if (rhs instanceof TrackListData) comparison -= 1;
                else if (rhs instanceof AlbumListData) comparison -= 2;
                else if (rhs instanceof PlaylistListData) comparison -= 3;
                else if (rhs instanceof ArtistListData) comparison -= 4;

                return comparison;
            }
        });

        notifyDataSetChanged();
    }

    public void clear() {
        list.clear();
        tracks.clear();
        albums.clear();
        playlists.clear();
        artists.clear();

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (getAbsPosition(position) < 0) return 4;

        Object o = list.get(getAbsPosition(position));
        if (o instanceof TrackListData) return 0;
        else if (o instanceof AlbumListData) return 1;
        else if (o instanceof PlaylistListData) return 2;
        else if (o instanceof ArtistListData) return 3;
        else return 4;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        switch (viewType) {
            case 0:
                return new TrackViewHolder(inflater.inflate(R.layout.track_item, parent, false));
            case 1:
                return new AlbumViewHolder(inflater.inflate(R.layout.album_item_card, parent, false));
            case 2:
                return new PlaylistViewHolder(inflater.inflate(R.layout.playlist_item_card, parent, false));
            case 3:
                return new ArtistViewHolder(inflater.inflate(R.layout.artist_item_card, parent, false));
            case 4:
                return new HeaderViewHolder(inflater.inflate(R.layout.header_item, parent, false));
            default:
                return null;
        }
    }


    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        String image;

        switch (getItemViewType(holder.getAdapterPosition())) {
            case 0:
                TrackListData trackData = tracks.get(getRelPosition(holder.getAdapterPosition()));

                image = trackData.trackImage;

                ((TextView) holder.v.findViewById(R.id.name)).setText(trackData.trackName);
                TextView extra = (TextView) holder.v.findViewById(R.id.extra);
                if (trackData.artists.size() > 0)
                    extra.setText(trackData.artists.get(0).artistName);

                holder.v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        StaticUtils.play(getRelPosition(holder.getAdapterPosition()), tracks, activity);

                        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(activity, R.anim.slide_up, R.anim.blank);
                        activity.startActivity(new Intent(activity, PlayerActivity.class), options.toBundle());
                    }
                });
                break;
            case 1:
                holder.v.findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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
                                return pasta.isFavorite(albums.get(getRelPosition(holder.getAdapterPosition())));
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
                                                return "favAlbum";
                                            }

                                            @Nullable
                                            @Override
                                            protected Boolean run() throws InterruptedException {
                                                if (!pasta.toggleFavorite(albums.get(getRelPosition(holder.getAdapterPosition())))) return null;
                                                else return pasta.isFavorite(albums.get(getRelPosition(holder.getAdapterPosition())));
                                            }

                                            @Override
                                            protected void done(@Nullable Boolean result) {
                                                if (result == null) {
                                                    pasta.onError(activity, "favorite album menu action");
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
                                    case R.id.action_web:
                                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticUtils.getAlbumUrl(albums.get(getRelPosition(holder.getAdapterPosition())).albumId))));
                                        break;
                                    case R.id.action_share:
                                        AlbumListData data = albums.get(getRelPosition(holder.getAdapterPosition()));

                                        Intent s = new Intent(android.content.Intent.ACTION_SEND);
                                        s.setType("text/plain");
                                        s.putExtra(Intent.EXTRA_SUBJECT, data.albumName);
                                        s.putExtra(Intent.EXTRA_TEXT, StaticUtils.getAlbumUrl(data.albumId));
                                        activity.startActivity(Intent.createChooser(s, data.albumName));
                                        break;
                                }
                                return false;
                            }
                        });
                        popup.show();
                    }
                });

                AlbumListData albumData = albums.get(getRelPosition(holder.getAdapterPosition()));

                image = albumData.albumImage;

                if (albumData.artists.size() > 0) {
                    ((TextView) holder.v.findViewById(R.id.artist_name)).setText(albumData.artists.get(0).artistName);
                    ((TextView) holder.v.findViewById(R.id.artist_extra)).setText(albumData.albumDate);

                    new Action<ArtistListData>() {
                        @NonNull
                        @Override
                        public String id() {
                            return "gotoArtist";
                        }

                        @Nullable
                        @Override
                        protected ArtistListData run() throws InterruptedException {
                            return pasta.getArtist(albums.get(getRelPosition(holder.getAdapterPosition())).artists.get(0).artistId);
                        }

                        @Override
                        protected void done(@Nullable ArtistListData result) {
                            if (result == null) {
                                pasta.onError(activity, "artist action");
                                return;
                            }

                            holder.v.findViewById(R.id.artist).setTag(result);
                        }
                    }.execute();

                    holder.v.findViewById(R.id.artist).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
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
                                        return pasta.getArtist(albums.get(getRelPosition(holder.getAdapterPosition())).artists.get(0).artistId);
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
                        }
                    });
                } else holder.v.findViewById(R.id.artist).setVisibility(View.GONE);

                ((TextView) holder.v.findViewById(R.id.name)).setText(albumData.albumName);
                ((TextView) holder.v.findViewById(R.id.extra)).setText(String.format("%d %s", albumData.tracks, albumData.tracks == 1 ? "track" : "tracks"));

                holder.v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle args = new Bundle();
                        args.putParcelable("album", albums.get(getRelPosition(holder.getAdapterPosition())));

                        Fragment f = new AlbumFragment();
                        f.setArguments(args);

                        activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                    }
                });
                break;
            case 2:
                holder.v.findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(v.getContext(), v);
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.menu_playlist, popup.getMenu());

                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
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
                                                if (!pasta.toggleFavorite(playlists.get(getRelPosition(holder.getAdapterPosition())))) {
                                                    return null;
                                                } else
                                                    return pasta.isFavorite(playlists.get(getRelPosition(holder.getAdapterPosition())));
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
                                    case R.id.action_web:
                                        PlaylistListData data = playlists.get(getRelPosition(holder.getAdapterPosition()));
                                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticUtils.getPlaylistUrl(data.playlistOwnerId, data.playlistId))));
                                        break;
                                    case R.id.action_share:
                                        PlaylistListData data2 = playlists.get(getRelPosition(holder.getAdapterPosition()));

                                        Intent s = new Intent(android.content.Intent.ACTION_SEND);
                                        s.setType("text/plain");
                                        s.putExtra(Intent.EXTRA_SUBJECT, data2.playlistName);
                                        s.putExtra(Intent.EXTRA_TEXT, StaticUtils.getPlaylistUrl(data2.playlistOwnerId, data2.playlistId));
                                        activity.startActivity(Intent.createChooser(s, data2.playlistName));
                                        break;
                                }
                                return false;
                            }
                        });

                        final MenuItem fav = popup.getMenu().findItem(R.id.action_fav);

                        if (((PlaylistListData) list.get(holder.getAdapterPosition())).editable) {
                            fav.setVisible(false);
                            popup.show();
                            return;
                        }

                        new Action<Boolean>() {
                            @NonNull
                            @Override
                            public String id() {
                                return "isPlaylistFav";
                            }

                            @Nullable
                            @Override
                            protected Boolean run() throws InterruptedException {
                                return pasta.isFavorite(playlists.get(getRelPosition(holder.getAdapterPosition())));
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

                        popup.show();
                    }
                });

                PlaylistListData playlistData = playlists.get(getRelPosition(holder.getAdapterPosition()));

                image = playlistData.playlistImage;

                ((TextView) holder.v.findViewById(R.id.name)).setText(playlistData.playlistName);
                ((TextView) holder.v.findViewById(R.id.extra)).setText(String.format("%d %s", playlistData.tracks, playlistData.tracks == 1 ? "track" : "tracks"));

                holder.v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle args = new Bundle();
                        args.putParcelable("playlist", playlists.get(getRelPosition(holder.getAdapterPosition())));

                        Fragment f = new PlaylistFragment();
                        f.setArguments(args);

                        activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                    }
                });
                break;
            case 3:
                holder.v.findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(v.getContext(), v);
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.menu_basic, popup.getMenu());

                        final MenuItem fav = popup.getMenu().findItem(R.id.action_fav);
                        new Action<Boolean>() {
                            @NonNull
                            @Override
                            public String id() {
                                return "isArtistFav";
                            }

                            @Nullable
                            @Override
                            protected Boolean run() throws InterruptedException {
                                return pasta.isFavorite(artists.get(getRelPosition(holder.getAdapterPosition())));
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
                                                return "favArtist";
                                            }

                                            @Nullable
                                            @Override
                                            protected Boolean run() throws InterruptedException {
                                                if (!pasta.toggleFavorite(artists.get(getRelPosition(holder.getAdapterPosition())))) {
                                                    return null;
                                                } else
                                                    return pasta.isFavorite(artists.get(getRelPosition(holder.getAdapterPosition())));
                                            }

                                            @Override
                                            protected void done(@Nullable Boolean result) {
                                                if (result == null) {
                                                    pasta.onError(activity, "favorite artist menu action");
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
                                    case R.id.action_web:
                                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticUtils.getArtistUrl(artists.get(getRelPosition(holder.getAdapterPosition())).artistId))));
                                        break;
                                    case R.id.action_share:
                                        ArtistListData data = artists.get(getRelPosition(holder.getAdapterPosition()));

                                        Intent s = new Intent(android.content.Intent.ACTION_SEND);
                                        s.setType("text/plain");
                                        s.putExtra(Intent.EXTRA_SUBJECT, data.artistName);
                                        s.putExtra(Intent.EXTRA_TEXT, StaticUtils.getArtistUrl(data.artistId));
                                        activity.startActivity(Intent.createChooser(s, data.artistName));
                                        break;
                                }
                                return false;
                            }
                        });
                        popup.show();
                    }
                });

                ArtistListData artistData = artists.get(getRelPosition(holder.getAdapterPosition()));

                image = artistData.artistImage;

                ((TextView) holder.v.findViewById(R.id.name)).setText(artistData.artistName);
                ((TextView) holder.v.findViewById(R.id.extra)).setText(String.format("%s followers", String.valueOf(artistData.followers)));

                holder.v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle args = new Bundle();
                        args.putParcelable("artist", artists.get(getRelPosition(holder.getAdapterPosition())));

                        Fragment f = new ArtistFragment();
                        f.setArguments(args);

                        activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                    }
                });
                break;
            case 4:
                ((TextView) holder.v).setText(getSectionName(position));
                return;
            default:
                return;
        }

        CustomImageView imageView = (CustomImageView) holder.v.findViewById(R.id.image);
        if (!isThumbnails) imageView.setVisibility(View.GONE);
        else {
            imageView.load(Glide.with(activity).load(image).thumbnail(0.2f).listener(new RequestListener<String, GlideDrawable>() {
                @Override
                public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                    return false;
                }

                @Override
                public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                    if (!isThumbnails) holder.v.findViewById(R.id.image).setVisibility(View.GONE);

                    View bg = holder.v.findViewById(R.id.bg);
                    if (!isThumbnails || bg == null) return false;
                    Palette.from(ImageUtils.drawableToBitmap(resource)).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), Color.DKGRAY, palette.getDarkVibrantColor(Color.DKGRAY));
                            animator.setDuration(250);
                            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animation) {
                                    int color = (int) animation.getAnimatedValue();
                                    holder.v.findViewById(R.id.bg).setBackgroundColor(color);

                                    View artist = holder.v.findViewById(R.id.artist);
                                    if (artist != null) artist.setBackgroundColor(Color.argb(255, Math.max(Color.red(color) - 10, 0), Math.max(Color.green(color) - 10, 0), Math.max(Color.blue(color) - 10, 0)));
                                }
                            });
                            animator.start();
                        }
                    });
                    return false;
                }
            }));
        }
    }

    private String getSectionName(int position) {
        if (position == tracks.size() + albums.size() + playlists.size() + 3) return "Artists";
        else if (position == tracks.size() + albums.size() + 2) return "Playlists";
        else if (position == tracks.size() + 1) return "Albums";
        else if (position == 0) return "Tracks";
        else return null;
    }

    private int getAbsPosition(int position) {
        if (position > tracks.size() + albums.size() + playlists.size() + 3 && position < tracks.size() + albums.size() + playlists.size() + artists.size() + 4) return position - 4;
        else if (position > tracks.size() + albums.size() + 2 && position < tracks.size() + albums.size() + playlists.size() + 3) return position - 3;
        else if (position > tracks.size() + 1 && position < tracks.size() + albums.size() + 2) return position - 2;
        else if (position > 0 && position < tracks.size() + 1) return position - 1;
        else return -1;
    }

    private int getRelPosition(int position) {
        if (position > tracks.size() + albums.size() + playlists.size() + 3 && position < tracks.size() + albums.size() + playlists.size() + artists.size() + 4) return position - (tracks.size() + albums.size() + playlists.size() + 4);
        else if (position > tracks.size() + albums.size() + 2 && position < tracks.size() + albums.size() + playlists.size() + 3) return position - (tracks.size() + albums.size() + 3);
        else if (position > tracks.size() + 1 && position < tracks.size() + albums.size() + 2) return position - (tracks.size() + 2);
        else if (position > 0 && position < tracks.size() + 1) return position - 1;
        else return -1;
    }

    @Override
    public int getItemCount() {
        if (list == null) return 0;

        int sections = 0;
        if (tracks.size() > 0) sections++;
        if (albums.size() > 0) sections++;
        if (playlists.size() > 0) sections++;
        if (artists.size() > 0) sections++;
        return list.size() + sections;
    }

    private static class HeaderViewHolder extends ViewHolder {
        public View v;

        HeaderViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }

    private static class TrackViewHolder extends ViewHolder {
        public View v;

        TrackViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }

    private static class AlbumViewHolder extends ViewHolder {
        public View v;

        AlbumViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }

    private static class PlaylistViewHolder extends ViewHolder {
        public View v;

        PlaylistViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }

    private static class ArtistViewHolder extends ViewHolder {
        public View v;

        ArtistViewHolder(View v) {
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
