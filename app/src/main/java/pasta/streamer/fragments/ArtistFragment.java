package pasta.streamer.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.async.Action;
import com.afollestad.async.Async;
import com.afollestad.async.Pool;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Artist;
import kaaes.spotify.webapi.android.models.Artists;
import kaaes.spotify.webapi.android.models.Pager;
import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.activities.HomeActivity;
import pasta.streamer.adapters.SectionedOmniAdapter;
import pasta.streamer.data.AlbumListData;
import pasta.streamer.data.ArtistListData;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.data.TrackListData;
import pasta.streamer.utils.ImageUtils;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;
import pasta.streamer.views.CustomImageView;

public class ArtistFragment extends FullScreenFragment {

    @BindView(R.id.progressBar2)
    ProgressBar spinner;
    @BindView(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;
    @BindView(R.id.topTenTrackListView)
    RecyclerView recycler;
    @BindView(R.id.header)
    CustomImageView header;
    @BindView(R.id.title)
    TextView title;
    @BindView(R.id.extra)
    TextView extra;
    @BindView(R.id.genres)
    FlexboxLayout genres;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.somethingbar)
    View somethingbar;
    @BindView(R.id.appbar)
    AppBarLayout appbar;
    @Nullable
    @BindView(R.id.backgroundImage)
    CustomImageView backgroundImage;

    private Unbinder unbinder;

    private ArtistListData data;
    private SectionedOmniAdapter adapter;
    private GridLayoutManager manager;
    private Pool pool;
    private Pasta pasta;
    private Map<String, Object> limitMap;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        data = getArguments().getParcelable("artist");

        pasta = (Pasta) getContext().getApplicationContext();
        limitMap = new HashMap<>();
        limitMap.put(SpotifyService.LIMIT, (PreferenceUtils.getLimit(getContext()) + 1) * 10);

        setHasOptionsMenu(true);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        toolbar.inflateMenu(R.menu.menu_basic);
        modifyMenu(toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onMenuClick(item);
                return false;
            }
        });

        title.setText(data.artistName);
        extra.setText(String.format("%s followers", String.valueOf(data.followers)));

        if (data.genres != null && data.genres.size() > 0) {
            for (int i = 0; i < data.genres.size() && i < 10; i++) {
                View v = LayoutInflater.from(getContext()).inflate(R.layout.genre_item, genres, false);
                ((TextView) v.findViewById(R.id.title)).setText(data.genres.get(i));
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getActivity(), HomeActivity.class);
                        i.putExtra("query", ((TextView) v.findViewById(R.id.title)).getText().toString());
                        startActivity(i);
                    }
                });
                genres.addView(v);
            }
        } else genres.setVisibility(View.GONE);

        spinner.setVisibility(View.VISIBLE);

        manager = new GridLayoutManager(getContext(), PreferenceUtils.getColumnNumber(getContext(), false));
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) == 0 || adapter.getItemViewType(position) == 2 || adapter.getItemViewType(position) == 4)
                    return manager.getSpanCount();
                else return 1;
            }
        });
        recycler.setLayoutManager(manager);


        adapter = new SectionedOmniAdapter((AppCompatActivity) getActivity(), null);
        recycler.setAdapter(adapter);

        pool = Async.parallel(new Action<ArrayList<TrackListData>>() {
            @NonNull
            @Override
            public String id() {
                return "searchTracks";
            }

            @Nullable
            @Override
            protected ArrayList<TrackListData> run() throws InterruptedException {
                return pasta.getTracks(data);
            }

            @Override
            protected void done(@Nullable ArrayList<TrackListData> result) {
                if (spinner != null) spinner.setVisibility(View.GONE);
                if (result == null) pasta.onError(getActivity(), "artist tracks action");
                else adapter.addData(result);
            }
        }, new Action<ArrayList<String>>() {
            @NonNull
            @Override
            public String id() {
                return "searchAlbums";
            }

            @Nullable
            @Override
            protected ArrayList<String> run() throws InterruptedException {
                Pager<Album> albums = getAlbums();
                if (albums == null) return null;

                ArrayList<String> list = new ArrayList<>();
                for (Album album : albums.items) {
                    list.add(album.id);
                }

                return list;
            }

            @Nullable
            private Pager<Album> getAlbums() throws InterruptedException {
                Pager<Album> albums = null;
                for (int i = 0; albums == null && i < PreferenceUtils.getRetryCount(getContext()); i++) {
                    try {
                        albums = pasta.spotifyService.getArtistAlbums(data.artistId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (StaticUtils.shouldResendRequest(e)) Thread.sleep(200);
                        else break;
                    }
                }
                return albums;
            }

            @Override
            protected void done(@Nullable ArrayList<String> result) {
                if (result == null) {
                    pasta.onError(getActivity(), "artist albums action");
                    return;
                }
                for (final String id : result) {
                    new Action<AlbumListData>() {
                        @NonNull
                        @Override
                        public String id() {
                            return "getAlbum";
                        }

                        @Nullable
                        @Override
                        protected AlbumListData run() throws InterruptedException {
                            return pasta.getAlbum(id);
                        }

                        @Override
                        protected void done(@Nullable AlbumListData result) {
                            if (spinner != null) spinner.setVisibility(View.GONE);
                            if (result != null) adapter.addData(result);
                        }
                    }.execute();
                }
            }
        }, new Action<ArrayList<PlaylistListData>>() {
            @NonNull
            @Override
            public String id() {
                return "getPlaylists";
            }

            @Nullable
            @Override
            protected ArrayList<PlaylistListData> run() throws InterruptedException {
                return pasta.searchPlaylists(data.artistName, limitMap);
            }

            @Override
            protected void done(@Nullable ArrayList<PlaylistListData> result) {
                if (spinner != null) spinner.setVisibility(View.GONE);
                if (result == null) pasta.onError(getContext(), "artist playlists action");
                else adapter.addData(result);
            }
        }, new Action<ArrayList<ArtistListData>>() {
            @NonNull
            @Override
            public String id() {
                return "getArtists";
            }

            @Nullable
            @Override
            protected ArrayList<ArtistListData> run() throws InterruptedException {
                Artists artists = getArtists();
                if (artists == null) return null;

                ArrayList<ArtistListData> list = new ArrayList<>();
                for (Artist artist : artists.artists) {
                    ArtistListData artistData = new ArtistListData(artist);
                    list.add(artistData);
                }

                return list;
            }

            @Nullable
            private Artists getArtists() throws InterruptedException {
                Artists artists = null;
                for (int i = 0; artists == null && i < PreferenceUtils.getRetryCount(getContext()); i++) {
                    try {
                        artists = pasta.spotifyService.getRelatedArtists(data.artistId);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (StaticUtils.shouldResendRequest(e)) Thread.sleep(200);
                        else break;
                    }
                }
                return artists;
            }

            @Override
            protected void done(@Nullable ArrayList<ArtistListData> result) {
                if (spinner != null) spinner.setVisibility(View.GONE);
                if (result == null)
                    pasta.onError(getContext(), "artist related artists action");
                else adapter.addData(result);
            }
        });

        Glide.with(getContext()).load(data.artistImage).placeholder(ImageUtils.getVectorDrawable(getContext(), R.drawable.preload)).thumbnail(0.2f).into(new GlideDrawableImageViewTarget(header) {
            @Override
            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                if (header != null) header.transition(resource);

                final Bitmap bitmap = ImageUtils.drawableToBitmap(resource);
                if (backgroundImage != null) {
                    new Thread() {
                        @Override
                        public void run() {
                            final Bitmap blurredBitmap = ImageUtils.blurBitmap(getContext(), bitmap);
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (backgroundImage != null)
                                        backgroundImage.transition(blurredBitmap);
                                }
                            });
                        }
                    }.start();
                }

                Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        int primary = palette.getMutedColor(Color.GRAY);
                        if (collapsingToolbarLayout != null)
                            collapsingToolbarLayout.setContentScrimColor(primary);

                        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), ContextCompat.getColor(getContext(), R.color.colorPrimary), primary);
                        animator.setDuration(250);
                        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                if (somethingbar != null)
                                    somethingbar.setBackgroundColor((int) animation.getAnimatedValue());
                            }
                        });
                        animator.start();

                        setData(data.artistName, primary, palette.getDarkVibrantColor(primary));
                    }
                });
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_basic, menu);
        modifyMenu(menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        onMenuClick(item);
        return false;
    }

    @Override
    public void onDestroyView() {
        if (pool != null && pool.isExecuting()) pool.cancel();
        super.onDestroyView();
        unbinder.unbind();
    }

    private void modifyMenu(final Menu menu) {
        new Action<Boolean>() {
            @NonNull
            @Override
            public String id() {
                return "isArtistFav";
            }

            @Nullable
            @Override
            protected Boolean run() throws InterruptedException {
                try {
                    return pasta.isFavorite(data);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done(@Nullable Boolean result) {
                if (result == null) {
                    pasta.onError(getActivity(), "artist favorite action");
                    return;
                }
                if (result) {
                    menu.findItem(R.id.action_fav).setIcon(R.drawable.ic_fav);
                } else {
                    menu.findItem(R.id.action_fav).setIcon(R.drawable.ic_unfav);
                }
            }

        }.execute();
    }

    private void onMenuClick(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                break;
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
                        if (!pasta.toggleFavorite(data)) {
                            return null;
                        } else return pasta.isFavorite(data);
                    }

                    @Override
                    protected void done(@Nullable Boolean result) {
                        if (result == null) {
                            pasta.onError(getActivity(), "artist favorite menu action");
                            return;
                        }
                        if (result) {
                            item.setIcon(R.drawable.ic_fav);
                        } else {
                            item.setIcon(R.drawable.ic_unfav);
                        }
                    }

                }.execute();
                break;
            case R.id.action_share:
                Intent s = new Intent(android.content.Intent.ACTION_SEND);
                s.setType("text/plain");
                s.putExtra(Intent.EXTRA_SUBJECT, data.artistName);
                s.putExtra(Intent.EXTRA_TEXT, StaticUtils.getArtistUrl(data.artistId));
                startActivity(Intent.createChooser(s, data.artistName));
                break;
            case R.id.action_web:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticUtils.getArtistUrl(data.artistId))));
                break;
        }
    }
}
