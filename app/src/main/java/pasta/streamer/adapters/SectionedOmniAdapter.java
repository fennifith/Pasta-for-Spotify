package pasta.streamer.adapters;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
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
import android.widget.Toast;

import com.afollestad.async.Action;

import java.io.ByteArrayOutputStream;
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
import pasta.streamer.utils.Downloader;
import pasta.streamer.utils.Settings;
import pasta.streamer.utils.StaticUtils;
import pasta.streamer.views.CustomImageView;

public class SectionedOmniAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<TrackListData> tracks;
    private ArrayList<AlbumListData> albums;
    private ArrayList<PlaylistListData> playlists;
    private ArrayList<ArtistListData> artists;
    private AppCompatActivity activity;
    private ArrayList list;
    private Drawable preload;
    private Drawable art_preload;
    private boolean thumbnails, palette, dark;

    public SectionedOmniAdapter(AppCompatActivity activity, ArrayList list) {
        tracks = new ArrayList<>();
        albums = new ArrayList<>();
        playlists = new ArrayList<>();
        artists = new ArrayList<>();

        this.activity = activity;

        thumbnails = Settings.isThumbnails(activity);
        palette = Settings.isPalette(activity);
        dark = Settings.isDarkTheme(activity);

        preload = ContextCompat.getDrawable(activity, R.drawable.preload);
        art_preload = new ColorDrawable(Color.TRANSPARENT);

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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        switch (viewType) {
            case 0:
                return new TrackViewHolder(inflater.inflate(R.layout.track_item, null));
            case 1:
                return new AlbumViewHolder(inflater.inflate(R.layout.album_item_card, null));
            case 2:
                return new PlaylistViewHolder(inflater.inflate(R.layout.playlist_item_card, null));
            case 3:
                return new ArtistViewHolder(inflater.inflate(R.layout.artist_item_card, null));
            case 4:
                return new HeaderViewHolder(DataBindingUtil.inflate(inflater, R.layout.header_item, null, false).getRoot());
            default:
                return null;
        }
    }


    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == 4) {
            ((TextView) ((HeaderViewHolder) holder).v).setText(getSectionName(position));
            return;
        }

        switch (getItemViewType(holder.getAdapterPosition())) {
            case 0:
                TrackViewHolder trackView = (TrackViewHolder) holder;
                ((ImageView) trackView.v.findViewById(R.id.image)).setImageDrawable(preload);

                TrackListData trackData = tracks.get(getRelPosition(holder.getAdapterPosition()));

                ((TextView) trackView.v.findViewById(R.id.name)).setText(trackData.trackName);
                ((TextView) trackView.v.findViewById(R.id.extra)).setText(trackData.artistName);

                trackView.v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        StaticUtils.play(getRelPosition(holder.getAdapterPosition()), tracks, activity);

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
                ((ImageView) albumView.v.findViewById(R.id.artist_image)).setImageDrawable(preload);

                albumView.v.findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
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
                                return ((Pasta) activity.getApplicationContext()).isFavorite(albums.get(getRelPosition(holder.getAdapterPosition())));
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
                                                if (!((Pasta) activity.getApplicationContext()).toggleFavorite(albums.get(getRelPosition(holder.getAdapterPosition())))) return null;
                                                else return ((Pasta) activity.getApplicationContext()).isFavorite(albums.get(getRelPosition(holder.getAdapterPosition())));
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

                ((TextView) albumView.v.findViewById(R.id.artist_name)).setText(albumData.artistName);
                ((TextView) albumView.v.findViewById(R.id.artist_extra)).setText(albumData.albumDate);

                if (!(activity.getSupportFragmentManager().findFragmentById(R.id.fragment) instanceof ArtistFragment)) {
                    albumView.v.findViewById(R.id.artist).setOnClickListener(new View.OnClickListener() {
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
                                    return new ArtistListData(((Pasta) activity.getApplicationContext()).spotifyService.getArtist(albums.get(getRelPosition(holder.getAdapterPosition())).artistId));
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

                albumView.v.setOnClickListener(new View.OnClickListener() {
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
                PlaylistViewHolder playlistView = (PlaylistViewHolder) holder;
                ((ImageView) playlistView.v.findViewById(R.id.image)).setImageDrawable(preload);
                playlistView.v.findViewById(R.id.bg).setBackground(art_preload);

                playlistView.v.findViewById(R.id.menu).setOnClickListener(new View.OnClickListener() {
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
                                                if (!((Pasta) activity.getApplicationContext()).toggleFavorite(playlists.get(getRelPosition(holder.getAdapterPosition())))) {
                                                    return null;
                                                } else
                                                    return ((Pasta) activity.getApplicationContext()).isFavorite(playlists.get(getRelPosition(holder.getAdapterPosition())));
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
                                return ((Pasta) activity.getApplicationContext()).isFavorite(playlists.get(getRelPosition(holder.getAdapterPosition())));
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

                ((TextView) playlistView.v.findViewById(R.id.name)).setText(playlistData.playlistName);
                ((TextView) playlistView.v.findViewById(R.id.extra)).setText(playlistData.tracks + " tracks");

                playlistView.v.setOnClickListener(new View.OnClickListener() {
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
                        new Action<Boolean>() {
                            @NonNull
                            @Override
                            public String id() {
                                return "isArtistFav";
                            }

                            @Nullable
                            @Override
                            protected Boolean run() throws InterruptedException {
                                return ((Pasta) activity.getApplicationContext()).isFavorite(artists.get(getRelPosition(holder.getAdapterPosition())));
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
                                                if (!((Pasta) activity.getApplicationContext()).toggleFavorite(artists.get(getRelPosition(holder.getAdapterPosition())))) {
                                                    return null;
                                                } else
                                                    return ((Pasta) activity.getApplicationContext()).isFavorite(artists.get(getRelPosition(holder.getAdapterPosition())));
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

                ((TextView) artistView.v.findViewById(R.id.name)).setText(artistData.artistName);
                ((TextView) artistView.v.findViewById(R.id.extra)).setText(String.valueOf(artistData.followers) + " followers");

                artistView.v.setOnClickListener(new View.OnClickListener() {
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
                switch (getItemViewType(holder.getAdapterPosition())) {
                    case 0:
                        return new Bitmap[]{Downloader.downloadImage(activity, tracks.get(getRelPosition(holder.getAdapterPosition())).trackImage)};
                    case 1:
                        AlbumListData album = albums.get(getRelPosition(holder.getAdapterPosition()));
                        return new Bitmap[]{Downloader.downloadImage(activity, album.albumImage), Downloader.downloadImage(activity, album.artistImage)};
                    case 2:
                        return new Bitmap[]{Downloader.downloadImage(activity, playlists.get(getRelPosition(holder.getAdapterPosition())).playlistImage)};
                    case 3:
                        return new Bitmap[]{Downloader.downloadImage(activity, artists.get(getRelPosition(holder.getAdapterPosition())).artistImage)};
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

                if (getItemViewType(holder.getAdapterPosition()) == 1) {
                    ImageView artistView = (ImageView) holderView.findViewById(R.id.artist_image);
                    if (!thumbnails || result == null) {
                        imageView.setVisibility(View.GONE);
                    } else if (artistView instanceof CustomImageView) {
                        ((CustomImageView) artistView).transition(new BitmapDrawable(activity.getResources(), result[1]));
                    } else {
                        TransitionDrawable td = new TransitionDrawable(new Drawable[]{preload, new BitmapDrawable(activity.getResources(), result[1])});
                        artistView.setImageDrawable(td);
                        td.startTransition(250);
                    }
                }

                if (!thumbnails || !palette || result == null || getItemViewType(holder.getAdapterPosition()) == 0) return;
                Palette.from(result[0]).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        int color = palette.getLightVibrantColor(Color.LTGRAY);
                        if (dark) color = palette.getDarkVibrantColor(Color.DKGRAY);

                        TransitionDrawable td = new TransitionDrawable(new Drawable[]{art_preload, new ColorDrawable(color)});
                        holderView.findViewById(R.id.bg).setBackground(td);
                        td.startTransition(250);
                    }
                });
            }
        }.execute();
    }

    public String getSectionName(int position) {
        if (position == tracks.size() + albums.size() + playlists.size() + 3) return "Artists";
        else if (position == tracks.size() + albums.size() + 2) return "Playlists";
        else if (position == tracks.size() + 1) return "Albums";
        else if (position == 0) return "Tracks";
        else return null;
    }

    public int getAbsPosition(int position) {
        if (position > tracks.size() + albums.size() + playlists.size() + 3 && position < tracks.size() + albums.size() + playlists.size() + artists.size() + 4) return position - 4;
        else if (position > tracks.size() + albums.size() + 2 && position < tracks.size() + albums.size() + playlists.size() + 3) return position - 3;
        else if (position > tracks.size() + 1 && position < tracks.size() + albums.size() + 2) return position - 2;
        else if (position > 0 && position < tracks.size() + 1) return position - 1;
        else return -1;
    }

    public int getRelPosition(int position) {
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

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public View v;

        public HeaderViewHolder(View v) {
            super(v);
            this.v = v;
        }
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
