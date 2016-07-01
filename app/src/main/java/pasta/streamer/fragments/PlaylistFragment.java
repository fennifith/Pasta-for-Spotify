package pasta.streamer.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.async.Action;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.activities.PlayerActivity;
import pasta.streamer.adapters.TrackAdapter;
import pasta.streamer.data.ArtistListData;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.data.TrackListData;
import pasta.streamer.utils.Settings;
import pasta.streamer.utils.StaticUtils;
import pasta.streamer.views.CustomImageView;

public class PlaylistFragment extends FullScreenFragment {

    @Bind(R.id.topTenTrackListView)
    RecyclerView recycler;
    @Bind(R.id.progressBar2)
    ProgressBar spinner;
    @Bind(R.id.fab)
    FloatingActionButton fab;
    @Bind(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;
    @Bind(R.id.header)
    CustomImageView header;
    @Bind(R.id.bar)
    View bar;
    @Bind(R.id.artists)
    FlexboxLayout artists;
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.appbar)
    AppBarLayout appbar;
    @Bind(R.id.tracksLength)
    TextView tracksLength;

    private Pasta pasta;
    private PlaylistListData data;
    private ArrayList<TrackListData> trackList;
    private Action action;
    private int selectedOrder;
    private TrackAdapter adapter;
    private boolean palette;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = DataBindingUtil.inflate(inflater, R.layout.fragment_tracks, container, false).getRoot();
        ButterKnife.bind(this, rootView);

        pasta = (Pasta) getContext().getApplicationContext();
        data = getArguments().getParcelable("playlist");

        palette = Settings.isPalette(getContext());

        fab.setBackgroundTintList(ColorStateList.valueOf(Settings.getAccentColor(getContext())));
        fab.setImageDrawable(StaticUtils.getVectorDrawable(getContext(), R.drawable.ic_play));

        collapsingToolbarLayout.setTitle(data.playlistName);
        tracksLength.setText(String.valueOf(data.tracks) + (data.tracks == 1 ? " track" : " tracks"));

        appbar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (appBarLayout.getHeight() / 2 < -verticalOffset) {
                    if (fab != null) fab.hide();
                } else {
                    if (fab != null) fab.show();
                }
            }
        });

        spinner.setVisibility(View.VISIBLE);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        adapter = new TrackAdapter((AppCompatActivity) getActivity(), null);
        if (data.editable) adapter.setPlaylistBehavior(data);
        recycler.setAdapter(adapter);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), Settings.isListTracks(getContext()) ? 1 : Settings.getColumnNumber(getContext(), metrics.widthPixels > metrics.heightPixels)));
        recycler.setHasFixedSize(true);

        action = new Action<ArrayList<TrackListData>>() {
            @NonNull
            @Override
            public String id() {
                return "getPlaylistTracks";
            }

            @Nullable
            @Override
            protected ArrayList<TrackListData> run() throws InterruptedException {
                return pasta.getTracks(data);
            }

            @Override
            protected void done(@Nullable ArrayList<TrackListData> result) {
                if (spinner != null) spinner.setVisibility(View.GONE);
                if (result == null) {
                    pasta.onCriticalError(getContext(), "playlist tracks action");
                    return;
                }
                adapter.swapData(result);
                adapter.sort(Settings.getTrackOrder(getContext()));
                trackList = result;

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
                        for (TrackListData track : trackList) {
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
                        if (result != null && result.size() > 0 && artists != null) {
                            for (ArtistListData artist : result) {
                                View v = LayoutInflater.from(getContext()).inflate(R.layout.artist_item_chip, null);
                                ((TextView) v.findViewById(R.id.title)).setText(artist.artistName);
                                Glide.with(getContext()).load(artist.artistImage).into(new GlideDrawableImageViewTarget((ImageView) v.findViewById(R.id.image)) {
                                    @Override
                                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                                        ((CustomImageView) getView()).transition(resource);
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

                                        getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.fragment, f).addToBackStack(null).commit();
                                    }
                                });
                                artists.addView(v);
                            }

                            artists.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    if (recycler != null)
                                        recycler.setPadding(0, artists.getHeight(), 0, 0);
                                    if (artists != null)
                                        artists.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            });
                        } else if (artists != null) artists.setVisibility(View.GONE);
                    }
                }.execute();
            }
        };
        action.execute();

        Glide.with(getContext()).load(data.playlistImageLarge).placeholder(StaticUtils.getVectorDrawable(getContext(), R.drawable.preload)).into(new GlideDrawableImageViewTarget(header) {
            @Override
            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                if (header != null) header.transition(resource);

                if (palette) {
                    Palette.from(StaticUtils.drawableToBitmap(resource)).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            int primary = palette.getMutedColor(Color.GRAY);
                            int accent = palette.getVibrantColor(StaticUtils.darkColor(primary));
                            if (collapsingToolbarLayout != null)
                                collapsingToolbarLayout.setContentScrimColor(primary);
                            if (fab != null)
                                fab.setBackgroundTintList(ColorStateList.valueOf(accent));
                            if (bar != null) bar.setBackgroundColor(primary);
                            setData(data.playlistName, primary, accent);
                        }
                    });
                }
            }
        });

        setHasOptionsMenu(true);
        toolbar.setNavigationIcon(R.drawable.drawer_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        toolbar.inflateMenu(R.menu.menu_playlist_view);
        modifyMenu(toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onMenuClick(item);
                return false;
            }
        });

        return rootView;
    }

    @OnClick(R.id.fab)
    public void startFirst(View v) {
        if (trackList == null || trackList.size() < 1) return;
        StaticUtils.play(0, trackList, getContext());
        startActivity(new Intent(getActivity(), PlayerActivity.class));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_playlist_view, menu);
        modifyMenu(menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        onMenuClick(item);
        return false;
    }

    public void modifyMenu(final Menu menu) {
        if (data.editable) {
            menu.findItem(R.id.action_edit).setVisible(true);
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
                    pasta.onError(getActivity(), "playlist favorite action");
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

    public void onMenuClick(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                break;
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
                        if (!pasta.toggleFavorite(data)) {
                            return null;
                        } else return pasta.isFavorite(data);
                    }

                    @Override
                    protected void done(@Nullable Boolean result) {
                        if (result == null) {
                            pasta.onError(getContext(), "playlist favorite menu action");
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
            case R.id.action_edit:
                final View layout = LayoutInflater.from(getContext()).inflate(R.layout.dialog_layout, null);

                ((AppCompatEditText) layout.findViewById(R.id.title)).setText(data.playlistName);
                ((AppCompatCheckBox) layout.findViewById(R.id.pub)).setChecked(data.playlistPublic);

                new AlertDialog.Builder(getContext()).setTitle(R.string.playlist_modify).setView(layout).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        if (((AppCompatEditText) layout.findViewById(R.id.title)).getText().toString().length() < 1) {
                            pasta.showToast(getString(R.string.no_playlist_text));
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
                                    pasta.spotifyService.changePlaylistDetails(pasta.me.id, data.playlistId, map);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            protected void done(@Nullable Boolean result) {
                                if (result == null || !result) {
                                    pasta.onError(getContext(), "playlist edit action");
                                } else {
                                    data.playlistName = (String) map.get("name");
                                    data.playlistPublic = (Boolean) map.get("public");
                                    toolbar.setTitle(data.playlistName);
                                }
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
            case R.id.action_share:
                Intent s = new Intent(android.content.Intent.ACTION_SEND);
                s.setType("text/plain");
                s.putExtra(Intent.EXTRA_SUBJECT, data.playlistName);
                s.putExtra(Intent.EXTRA_TEXT, StaticUtils.getPlaylistUrl(data.playlistOwnerId, data.playlistId));
                startActivity(Intent.createChooser(s, data.playlistName));
                break;
            case R.id.action_web:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(StaticUtils.getPlaylistUrl(data.playlistOwnerId, data.playlistId))));
                break;
            case R.id.action_order:
                Settings.getOrderingDialog(getContext(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedOrder = which;
                    }
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(Settings.ORDER, selectedOrder).apply();
                        adapter.sort(Settings.getTrackOrder(getContext()));
                        d.dismiss();
                    }
                }).show();
                break;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (action != null && action.isExecuting()) action.cancel();
        ButterKnife.unbind(this);
    }
}
