package pasta.streamer.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.async.Action;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.TrackToRemove;
import kaaes.spotify.webapi.android.models.TracksToRemove;
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
import pasta.streamer.utils.Downloader;
import pasta.streamer.utils.Settings;
import pasta.streamer.utils.StaticUtils;
import pasta.streamer.views.CustomImageView;

public class OmniAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList original;
    private ArrayList list;
    private AppCompatActivity activity;
    private int menures = R.menu.menu_track;
    private PlaylistListData playlistdata;
    private Drawable preload;
    private Drawable art_preload;
    private boolean thumbnails, cards, trackList, palette, dark;
    private int behavior = 0;
    public final static int BEHAVIOR_NONE = 0, BEHAVIOR_PLAYLIST = 1, BEHAVIOR_FAVORITE = 2, BEHAVIOR_ALBUM = 3;

    public OmniAdapter(AppCompatActivity activity, ArrayList list) {
        original = list;
        if (list != null) {
            this.list = new ArrayList();
            try {
                this.list.addAll(original);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.activity = activity;

        thumbnails = Settings.isThumbnails(activity);
        cards = Settings.isCards(activity);
        trackList = Settings.isListTracks(activity);
        palette = Settings.isPalette(activity);
        dark = Settings.isDarkTheme(activity);

        preload = ContextCompat.getDrawable(activity, R.drawable.preload);
        art_preload = new ColorDrawable(Color.TRANSPARENT);
        behavior = BEHAVIOR_NONE;
    }

    public void setPlaylistBehavior(PlaylistListData data) {
        menures = R.menu.menu_playlist_track;
        playlistdata = data;
        behavior = BEHAVIOR_PLAYLIST;
    }

    public void setAlbumBehavior() {
        menures = R.menu.menu_album_track;
        behavior = BEHAVIOR_ALBUM;
    }

    public void setFavoriteBehavior() {
        behavior = BEHAVIOR_FAVORITE;
    }

    public void addData(Parcelable data) {
        if (data == null) return;
        original.add(data);
        int pos = list.size();
        list.add(pos, data);
        notifyItemInserted(pos);
    }

    public void swapData(ArrayList list) {
        original = list;
        this.list = new ArrayList();
        this.list.addAll(list);
        notifyDataSetChanged();
    }

    public void removeData(int position) {
        if (position < 0 || position > original.size()) return;
        original.remove(original.indexOf(list.get(position)));
        list.remove(position);
        notifyItemRemoved(position);
    }

    public void removeData(Parcelable data) {
        original.remove(original.indexOf(data));
        int pos = list.indexOf(data);
        list.remove(pos);
        notifyItemRemoved(pos);
    }

    public void sort(int order) {
        switch (order) {
            case Settings.ORDER_ADDED:
                list = new ArrayList();
                list.addAll(original);
                break;
            case Settings.ORDER_NAME:
                Collections.sort(list, new Comparator<TrackListData>() {
                    public int compare(TrackListData first, TrackListData second) {
                        return first.trackName.compareTo(second.trackName);
                    }
                });
                break;
            case Settings.ORDER_ARTIST:
                Collections.sort(list, new Comparator<TrackListData>() {
                    public int compare(TrackListData first, TrackListData second) {
                        return first.artistName.compareTo(second.artistName);
                    }
                });
                break;
            case Settings.ORDER_ALBUM:
                Collections.sort(list, new Comparator<TrackListData>() {
                    public int compare(TrackListData first, TrackListData second) {
                        return first.albumName.compareTo(second.albumName);
                    }
                });
                break;
            case Settings.ORDER_LENGTH:
                Collections.sort(list, new Comparator<TrackListData>() {
                    public int compare(TrackListData first, TrackListData second) {
                        return first.trackDuration.compareTo(second.trackDuration);
                    }
                });
                break;
            case Settings.ORDER_RANDOM:
                Collections.shuffle(list);
                break;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position >= list.size() || position < 0) return -1;
        Object o = list.get(position);
        if (o instanceof TrackListData) return 0;
        else if (o instanceof AlbumListData) return 1;
        else if (o instanceof PlaylistListData) return 2;
        else if (o instanceof ArtistListData) return 3;
        else return -1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        switch (viewType) {
            case 0:
                return new TrackViewHolder(inflater.inflate(trackList ? R.layout.track_item : (cards ? R.layout.track_item_card : R.layout.track_item_tile), null));
            case 1:
                return new AlbumViewHolder(inflater.inflate(cards ? R.layout.album_item_card : R.layout.album_item_tile, null));
            case 2:
                return new PlaylistViewHolder(inflater.inflate(cards ? R.layout.playlist_item_card : R.layout.playlist_item_tile, null));
            case 3:
                return new ArtistViewHolder(inflater.inflate(cards ? R.layout.artist_item_card : R.layout.artist_item_tile, null));
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case 0:
                TrackViewHolder trackView = (TrackViewHolder) holder;
                ((ImageView) trackView.v.findViewById(R.id.image)).setImageDrawable(preload);

                View trackBg = trackView.v.findViewById(R.id.bg);
                if (trackBg != null) trackBg.setBackground(art_preload);

                View trackMenu = trackView.v.findViewById(R.id.menu);
                if (trackMenu.getVisibility() == View.GONE) {
                    trackMenu.setVisibility(View.VISIBLE);
                }
                trackMenu.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(v.getContext(), v);
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(menures, popup.getMenu());

                        final MenuItem fav = popup.getMenu().findItem(R.id.action_fav);
                        if (behavior == BEHAVIOR_FAVORITE) fav.setTitle(R.string.unfav);
                        else new Action<Boolean>() {
                            @NonNull
                            @Override
                            public String id() {
                                return "isTrackFav";
                            }

                            @Nullable
                            @Override
                            protected Boolean run() throws InterruptedException {
                                return ((Pasta) activity.getApplicationContext()).isFavorite((TrackListData) list.get(holder.getAdapterPosition()));
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
                                                if (!((Pasta) activity.getApplicationContext()).toggleFavorite((TrackListData) list.get(holder.getAdapterPosition()))) {
                                                    return null;
                                                } else
                                                    return ((Pasta) activity.getApplicationContext()).isFavorite((TrackListData) list.get(holder.getAdapterPosition()));
                                            }

                                            @Override
                                            protected void done(@Nullable Boolean result) {
                                                if (result == null) {
                                                    Toast.makeText(activity, "An error occured", Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                                if (result) {
                                                    item.setTitle(R.string.unfav);
                                                } else {
                                                    item.setTitle(R.string.fav);
                                                    if (behavior == BEHAVIOR_FAVORITE) removeData(holder.getAdapterPosition());
                                                }
                                            }

                                        }.execute();
                                        break;
                                    case R.id.action_add:
                                        StaticUtils.showAddToDialog(activity, ((TrackListData) list.get(holder.getAdapterPosition())));
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
                                                Pasta pasta = (Pasta) activity.getApplicationContext();
                                                Album album = pasta.spotifyService.getAlbum(((TrackListData) list.get(holder.getAdapterPosition())).albumId);
                                                Artist artist = pasta.spotifyService.getArtist(album.artists.get(0).id);

                                                String image = "";
                                                if (artist.images.size() > 0) image = artist.images.get(artist.images.size() / 2).url;

                                                return new AlbumListData(album, image);
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
                                        new Action<ArtistListData>() {
                                            @NonNull
                                            @Override
                                            public String id() {
                                                return "gotoArtist";
                                            }

                                            @Nullable
                                            @Override
                                            protected ArtistListData run() throws InterruptedException {
                                                return new ArtistListData(((Pasta) activity.getApplicationContext()).spotifyService.getArtist(((TrackListData) list.get(holder.getAdapterPosition())).artistId));
                                            }

                                            @Override
                                            protected void done(@Nullable ArtistListData result) {
                                                Bundle args = new Bundle();
                                                args.putParcelable("artist", result);

                                                Fragment f = new ArtistFragment();
                                                f.setArguments(args);

                                                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                                            }
                                        }.execute();
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
                                                trackRemove.uri = ((TrackListData) list.get(holder.getAdapterPosition())).trackUri;
                                                tracksRemove.tracks = Collections.singletonList(trackRemove);
                                                if (behavior == BEHAVIOR_PLAYLIST && playlistdata != null) {
                                                    ((Pasta) activity.getApplicationContext()).spotifyService.removeTracksFromPlaylist(playlistdata.playlistOwnerId, playlistdata.playlistId, tracksRemove);
                                                    return true;
                                                } else return false;
                                            }

                                            @Override
                                            protected void done(@Nullable Boolean result) {
                                                if (result == null || !result) {
                                                    Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                                Toast.makeText(activity, "Removed from Playlist", Toast.LENGTH_SHORT).show();
                                                original.remove(original.indexOf(list.get(holder.getAdapterPosition())));
                                                list.remove(holder.getAdapterPosition());
                                                notifyDataSetChanged();
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

                TrackListData trackData = (TrackListData) list.get(position);

                ((TextView) trackView.v.findViewById(R.id.name)).setText(trackData.trackName);
                ((TextView) trackView.v.findViewById(R.id.extra)).setText(trackData.artistName);

                trackView.v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int position = 0;
                        ArrayList<TrackListData> trackList = new ArrayList<TrackListData>();
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i) instanceof TrackListData) {
                                if (i == holder.getAdapterPosition()) position = trackList.size();
                                trackList.add((TrackListData) list.get(i));
                            }
                        }
                        StaticUtils.play(position, trackList, activity);

                        Intent intent = new Intent(view.getContext(), PlayerActivity.class);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        StaticUtils.drawableToBitmap(((ImageView) view.findViewById(R.id.image)).getDrawable()).compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] b = baos.toByteArray();
                        intent.putExtra("preload", b);

                        ActivityOptionsCompat options = ActivityOptionsCompat.makeThumbnailScaleUpAnimation(view, StaticUtils.drawableToBitmap(((ImageView) view.findViewById(R.id.image)).getDrawable()), 5, 5);
                        view.getContext().startActivity(intent, options.toBundle());
                    }
                });
                break;
            case 1:
                AlbumViewHolder albumView = (AlbumViewHolder) holder;
                ((ImageView) albumView.v.findViewById(R.id.image)).setImageDrawable(preload);
                albumView.v.findViewById(R.id.bg).setBackground(art_preload);

                ImageView artistImage = (ImageView) albumView.v.findViewById(R.id.artist_image);
                if (artistImage != null) artistImage.setImageDrawable(preload);

                albumView.v.findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(v.getContext(), v);
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.menu_basic, popup.getMenu());

                        final MenuItem fav = popup.getMenu().findItem(R.id.action_fav);
                        if (behavior == BEHAVIOR_FAVORITE) fav.setTitle(R.string.unfav);
                        else new Action<Boolean>() {
                            @NonNull
                            @Override
                            public String id() {
                                return "isAlbumFav";
                            }

                            @Nullable
                            @Override
                            protected Boolean run() throws InterruptedException {
                                return ((Pasta) activity.getApplicationContext()).isFavorite((AlbumListData) list.get(holder.getAdapterPosition()));
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
                                                if (!((Pasta) activity.getApplicationContext()).toggleFavorite((AlbumListData) list.get(holder.getAdapterPosition()))) {
                                                    return null;
                                                } else
                                                    return ((Pasta) activity.getApplicationContext()).isFavorite((AlbumListData) list.get(holder.getAdapterPosition()));
                                            }

                                            @Override
                                            protected void done(@Nullable Boolean result) {
                                                if (result == null) {
                                                    Toast.makeText(activity, "An error occured", Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                                if (result) {
                                                    item.setTitle(R.string.unfav);
                                                } else {
                                                    item.setTitle(R.string.fav);

                                                    if (behavior == BEHAVIOR_FAVORITE) removeData(holder.getAdapterPosition());
                                                }
                                            }

                                        }.execute();
                                        break;
                                    case R.id.action_web:
                                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticUtils.getAlbumUrl(((AlbumListData) list.get(holder.getAdapterPosition())).albumId))));
                                        break;
                                    case R.id.action_share:
                                        AlbumListData data = (AlbumListData) list.get(holder.getAdapterPosition());

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

                AlbumListData albumData = (AlbumListData) list.get(position);

                View artist = albumView.v.findViewById(R.id.artist);
                if (artist != null) {
                    ((TextView) albumView.v.findViewById(R.id.artist_name)).setText(albumData.artistName);
                    ((TextView) albumView.v.findViewById(R.id.artist_extra)).setText(albumData.albumDate);

                    artist.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new Action<ArtistListData>() {
                                @NonNull
                                @Override
                                public String id() {
                                    return "gotoArtist";
                                }

                                @Nullable
                                @Override
                                protected ArtistListData run() throws InterruptedException {
                                    return new ArtistListData(((Pasta) activity.getApplicationContext()).spotifyService.getArtist(((AlbumListData) list.get(holder.getAdapterPosition())).artistId));
                                }

                                @Override
                                protected void done(@Nullable ArtistListData result) {
                                    Bundle args = new Bundle();
                                    args.putParcelable("artist", result);

                                    Fragment f = new ArtistFragment();
                                    f.setArguments(args);

                                    activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                                }
                            }.execute();
                        }
                    });
                }

                ((TextView) albumView.v.findViewById(R.id.name)).setText(albumData.albumName);
                ((TextView) albumView.v.findViewById(R.id.extra)).setText(String.valueOf(albumData.tracks) + " tracks");

                albumView.v.findViewById(R.id.album).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle args = new Bundle();
                        args.putParcelable("album", (AlbumListData) list.get(holder.getAdapterPosition()));

                        Fragment f = new AlbumFragment();
                        f.setArguments(args);

                        activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                    }
                });
                break;
            case 2:
                PlaylistViewHolder playlistView = (PlaylistViewHolder) holder;
                ((ImageView) playlistView.v.findViewById(R.id.image)).setImageDrawable(preload);
                playlistView.v.findViewById(R.id.bg).setBackground(art_preload);

                playlistView.v.findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(v.getContext(), v);
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.menu_playlist, popup.getMenu());

                        final MenuItem fav = popup.getMenu().findItem(R.id.action_fav);
                        if (behavior == BEHAVIOR_FAVORITE) fav.setTitle(R.string.unfav);
                        else new Action<Boolean>() {
                            @NonNull
                            @Override
                            public String id() {
                                return "isPlaylistFav";
                            }

                            @Nullable
                            @Override
                            protected Boolean run() throws InterruptedException {
                                return ((Pasta) activity.getApplicationContext()).isFavorite((PlaylistListData) list.get(holder.getAdapterPosition()));
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
                                                return "favPlaylist";
                                            }

                                            @Nullable
                                            @Override
                                            protected Boolean run() throws InterruptedException {
                                                if (!((Pasta) activity.getApplicationContext()).toggleFavorite((PlaylistListData) list.get(holder.getAdapterPosition()))) {
                                                    return null;
                                                } else
                                                    return ((Pasta) activity.getApplicationContext()).isFavorite((PlaylistListData) list.get(holder.getAdapterPosition()));
                                            }

                                            @Override
                                            protected void done(@Nullable Boolean result) {
                                                if (result == null) {
                                                    Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                                if (result) {
                                                    item.setTitle(R.string.unfav);
                                                } else {
                                                    item.setTitle(R.string.fav);
                                                    if (behavior == BEHAVIOR_FAVORITE) removeData(holder.getAdapterPosition());
                                                }
                                            }

                                        }.execute();
                                        break;
                                    case R.id.action_edit:
                                        final PlaylistListData editData = (PlaylistListData) list.get(holder.getAdapterPosition());
                                        final View layout = activity.getLayoutInflater().inflate(R.layout.dialog_layout, null);

                                        ((AppCompatEditText) layout.findViewById(R.id.title)).setText(editData.playlistName);
                                        ((AppCompatCheckBox) layout.findViewById(R.id.pub)).setChecked(editData.playlistPublic);

                                        new AlertDialog.Builder(activity).setTitle(R.string.playlist_modify).setView(layout).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(final DialogInterface dialog, int which) {
                                                if (((AppCompatEditText) layout.findViewById(R.id.title)).getText().toString().length() < 1) {
                                                    Toast.makeText(activity, R.string.no_playlist_text, Toast.LENGTH_SHORT).show();
                                                    return;
                                                }

                                                final Map<String, Object> map = new HashMap<>();
                                                map.put("name", ((AppCompatEditText) layout.findViewById(R.id.title)).getText().toString());
                                                map.put("public", ((AppCompatCheckBox) layout.findViewById(R.id.pub)).isChecked());

                                                new Action<Boolean>() {
                                                    @NonNull
                                                    @Override
                                                    public String id() {
                                                        return "modifyPlaylist";
                                                    }

                                                    @Nullable
                                                    @Override
                                                    protected Boolean run() throws InterruptedException {
                                                        try {
                                                            ((Pasta) activity.getApplicationContext()).spotifyService.changePlaylistDetails(((Pasta) activity.getApplicationContext()).me.id, editData.playlistId, map);
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                            return false;
                                                        }
                                                        editData.playlistName = (String) map.get("name");
                                                        editData.playlistPublic = (Boolean) map.get("public");
                                                        return true;
                                                    }

                                                    @Override
                                                    protected void done(@Nullable Boolean result) {
                                                        if (result == null || !result) {
                                                            Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show();
                                                        } else notifyItemChanged(holder.getAdapterPosition());
                                                    }
                                                }.execute();

                                                dialog.dismiss();
                                            }
                                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        }).create().show();
                                        break;
                                    case R.id.action_web:
                                        PlaylistListData data = (PlaylistListData) list.get(holder.getAdapterPosition());
                                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticUtils.getPlaylistUrl(data.playlistOwnerId, data.playlistId))));
                                        break;
                                    case R.id.action_share:
                                        PlaylistListData data2 = (PlaylistListData) list.get(holder.getAdapterPosition());

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

                        if (((PlaylistListData) list.get(holder.getAdapterPosition())).editable)
                            popup.getMenu().findItem(R.id.action_edit).setVisible(true);

                        popup.show();
                    }
                });

                PlaylistListData playlistData = (PlaylistListData) list.get(position);

                ((TextView) playlistView.v.findViewById(R.id.name)).setText(playlistData.playlistName);
                ((TextView) playlistView.v.findViewById(R.id.extra)).setText(playlistData.tracks + " tracks");

                playlistView.v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle args = new Bundle();
                        args.putParcelable("playlist", (PlaylistListData) list.get(holder.getAdapterPosition()));

                        Fragment f = new PlaylistFragment();
                        f.setArguments(args);

                        activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                    }
                });
                break;
            case 3:
                ArtistViewHolder artistView = (ArtistViewHolder) holder;
                ((ImageView) artistView.v.findViewById(R.id.image)).setImageDrawable(preload);
                artistView.v.findViewById(R.id.bg).setBackground(art_preload);

                artistView.v.findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(v.getContext(), v);
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.menu_basic, popup.getMenu());

                        final MenuItem fav = popup.getMenu().findItem(R.id.action_fav);
                        if (behavior == BEHAVIOR_FAVORITE) fav.setTitle(R.string.unfav);
                        else new Action<Boolean>() {
                            @NonNull
                            @Override
                            public String id() {
                                return "isTrackFav";
                            }

                            @Nullable
                            @Override
                            protected Boolean run() throws InterruptedException {
                                return ((Pasta) activity.getApplicationContext()).isFavorite((ArtistListData) list.get(holder.getAdapterPosition()));
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
                                                if (!((Pasta) activity.getApplicationContext()).toggleFavorite((ArtistListData) list.get(holder.getAdapterPosition()))) {
                                                    return null;
                                                } else
                                                    return ((Pasta) activity.getApplicationContext()).isFavorite((ArtistListData) list.get(holder.getAdapterPosition()));
                                            }

                                            @Override
                                            protected void done(@Nullable Boolean result) {
                                                if (result == null) {
                                                    Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show();
                                                    return;
                                                }
                                                if (result) {
                                                    item.setTitle(R.string.unfav);
                                                } else {
                                                    item.setTitle(R.string.fav);
                                                    if (behavior == BEHAVIOR_FAVORITE) removeData(holder.getAdapterPosition());
                                                }
                                            }

                                        }.execute();
                                        break;
                                    case R.id.action_web:
                                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticUtils.getArtistUrl(((ArtistListData) list.get(holder.getAdapterPosition())).artistId))));
                                        break;
                                    case R.id.action_share:
                                        ArtistListData data = (ArtistListData) list.get(holder.getAdapterPosition());

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

                ArtistListData artistData = (ArtistListData) list.get(position);

                ((TextView) artistView.v.findViewById(R.id.name)).setText(artistData.artistName);
                ((TextView) artistView.v.findViewById(R.id.extra)).setText(String.valueOf(artistData.followers) + " followers");

                artistView.v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bundle args = new Bundle();
                        args.putParcelable("artist", (ArtistListData) list.get(holder.getAdapterPosition()));

                        Fragment f = new ArtistFragment();
                        f.setArguments(args);

                        activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                    }
                });
                break;
            default:
                return;
        }

        new Action<Bitmap[]>() {
            @NonNull
            @Override
            public String id() {
                return "bindVH";
            }

            @Nullable
            @Override
            protected Bitmap[] run() throws InterruptedException {
                if (!thumbnails) return null;
                int position = holder.getAdapterPosition();
                switch (getItemViewType(position)) {
                    case 0:
                        return new Bitmap[]{Downloader.downloadImage(activity, ((TrackListData) OmniAdapter.this.list.get(position)).trackImage)};
                    case 1:
                        AlbumListData album = (AlbumListData) OmniAdapter.this.list.get(position);
                        return new Bitmap[]{Downloader.downloadImage(activity, album.albumImage), Downloader.downloadImage(activity, album.artistImage)};
                    case 2:
                        return new Bitmap[]{Downloader.downloadImage(activity, ((PlaylistListData) OmniAdapter.this.list.get(position)).playlistImage)};
                    case 3:
                        return new Bitmap[]{Downloader.downloadImage(activity, ((ArtistListData) OmniAdapter.this.list.get(position)).artistImage)};
                    default:
                        return null;
                }
            }

            @Override
            protected void done(@Nullable Bitmap[] result) {
                final View holderView;
                switch (getItemViewType(holder.getAdapterPosition())) {
                    case 0:
                        holderView = ((TrackViewHolder) holder).v;
                        break;
                    case 1:
                        holderView = ((AlbumViewHolder) holder).v;

                        ImageView artistImage = (ImageView) holderView.findViewById(R.id.artist_image);
                        if (artistImage != null) {
                            if (!thumbnails || result == null) {
                                artistImage.setVisibility(View.GONE);
                            } else if (artistImage instanceof CustomImageView) {
                                ((CustomImageView) artistImage).transition(new BitmapDrawable(activity.getResources(), result[1]));
                            } else {
                                TransitionDrawable td = new TransitionDrawable(new Drawable[]{preload, new BitmapDrawable(activity.getResources(), result[1])});
                                artistImage.setImageDrawable(td);
                                td.startTransition(250);
                            }
                        }
                        break;
                    case 2:
                        holderView = ((PlaylistViewHolder) holder).v;
                        break;
                    case 3:
                        holderView = ((ArtistViewHolder) holder).v;
                        break;
                    default:
                        return;
                }

                ImageView imageView = (ImageView) holderView.findViewById(R.id.image);
                if (!thumbnails || result == null) {
                    imageView.setVisibility(View.GONE);
                } else if (imageView instanceof CustomImageView) {
                    ((CustomImageView) imageView).transition(new BitmapDrawable(activity.getResources(), result[0]));
                } else {
                    TransitionDrawable td = new TransitionDrawable(new Drawable[]{preload, new BitmapDrawable(activity.getResources(), result[0])});
                    imageView.setImageDrawable(td);
                    td.startTransition(250);
                }

                View bg = holderView.findViewById(R.id.bg);
                if (!thumbnails || !palette || result == null || bg == null) return;
                Palette.from(result[0]).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        int color = palette.getLightVibrantColor(Color.WHITE);
                        if (dark) color = palette.getDarkVibrantColor(Color.DKGRAY);

                        TransitionDrawable td = new TransitionDrawable(new Drawable[]{art_preload, new ColorDrawable(color)});
                        holderView.findViewById(R.id.bg).setBackground(td);
                        td.startTransition(250);

                        View artist = holderView.findViewById(R.id.artist);
                        if (artist != null) artist.setBackgroundColor(Color.argb(255, Math.max(Color.red(color) - 10, 0), Math.max(Color.green(color) - 10, 0), Math.max(Color.blue(color) - 10, 0)));
                    }
                });
            }
        }.execute();
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        public View v;

        public TrackViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }

    public static class AlbumViewHolder extends RecyclerView.ViewHolder {
        public View v;

        public AlbumViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }

    public static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        public View v;

        public PlaylistViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }

    public static class ArtistViewHolder extends RecyclerView.ViewHolder {
        public View v;

        public ArtistViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }
}
