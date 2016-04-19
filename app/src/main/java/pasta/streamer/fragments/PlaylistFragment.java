package pasta.streamer.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.async.Action;
import com.afollestad.async.Async;
import com.afollestad.async.Pool;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kaaes.spotify.webapi.android.models.Playlist;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.activities.PlayerActivity;
import pasta.streamer.adapters.OmniAdapter;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.data.TrackListData;
import pasta.streamer.utils.Downloader;
import pasta.streamer.utils.Settings;
import pasta.streamer.utils.StaticUtils;
import pasta.streamer.views.CustomImageView;
import retrofit.RetrofitError;

public class PlaylistFragment extends FullScreenFragment {

    @Bind(R.id.topTenTrackListView)
    RecyclerView topTenTrackView;
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
    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.appbar)
    AppBarLayout appbar;
    @Bind(R.id.tracksLength)
    TextView tracksLength;

    private PlaylistListData data;
    private ArrayList<TrackListData> trackList;
    private Pool pool;
    private int selectedOrder;
    private OmniAdapter adapter;
    private boolean palette;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = DataBindingUtil.inflate(inflater, R.layout.fragment_tracks, container, false).getRoot();
        ButterKnife.bind(this, rootView);

        data = getArguments().getParcelable("playlist");

        palette = Settings.isPalette(getContext());

        fab.setBackgroundTintList(ColorStateList.valueOf(Settings.getAccentColor(getContext())));

        collapsingToolbarLayout.setTitle(data.playlistName);
        tracksLength.setText(String.valueOf(data.tracks) + (data.tracks == 1 ? " track" : " tracks"));

        appbar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (appBarLayout.getHeight() / 2 < -verticalOffset) {
                    fab.hide();
                } else {
                    fab.show();
                }
            }
        });

        spinner.setVisibility(View.VISIBLE);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        adapter = new OmniAdapter((AppCompatActivity) getActivity(), null);
        if (data.editable) adapter.setPlaylistBehavior(data);
        topTenTrackView.setAdapter(adapter);
        topTenTrackView.setLayoutManager(new GridLayoutManager(getContext(), metrics.widthPixels > metrics.heightPixels ? 3 : 2));
        topTenTrackView.setHasFixedSize(true);

        pool = Async.parallel(new Action<ArrayList<TrackListData>>() {
            @NonNull
            @Override
            public String id() {
                return "getPlaylistTracks";
            }

            @Nullable
            @Override
            protected ArrayList<TrackListData> run() throws InterruptedException {
                Playlist playlist;
                try {
                    playlist = ((Pasta) getContext().getApplicationContext()).spotifyService.getPlaylist(data.playlistOwnerId, data.playlistId);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

                ArrayList<TrackListData> trackList = new ArrayList<>();
                for (PlaylistTrack track : playlist.tracks.items) {
                    trackList.add(new TrackListData(track.track));
                }
                return trackList;
            }

            @Override
            protected void done(@Nullable ArrayList<TrackListData> result) {
                spinner.setVisibility(View.GONE);
                if (result == null) {
                    StaticUtils.onNetworkError(getActivity());
                    return;
                }
                adapter.swapData(result);
                adapter.sort(Settings.getTrackOrder(getContext()));
                trackList = result;
            }
        }, new Action<Bitmap>() {
            @NonNull
            @Override
            public String id() {
                return "getPlaylistHeader";
            }

            @Nullable
            @Override
            protected Bitmap run() throws InterruptedException {
                return Downloader.downloadImage(getActivity(), data.playlistImageLarge);
            }

            @Override
            protected void done(@Nullable Bitmap result) {
                if (result == null) return;

                header.transition(new BitmapDrawable(getResources(), result));

                if (!palette) return;
                Palette.from(result).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette palette) {
                        int primary = palette.getMutedColor(Color.GRAY);
                        int accent = palette.getVibrantColor(StaticUtils.darkColor(primary));
                        collapsingToolbarLayout.setContentScrimColor(primary);
                        fab.setBackgroundTintList(ColorStateList.valueOf(accent));
                        bar.setBackgroundColor(primary);
                        setData(data.playlistName, primary, accent);
                    }
                });
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

        Intent intent = new Intent(getActivity(), PlayerActivity.class);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StaticUtils.drawableToBitmap(header.getDrawable()).compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();
        intent.putExtra("preload", b);

        ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(v, (int) v.getX(), (int) v.getY(), v.getWidth(), v.getHeight());
        startActivity(intent, options.toBundle());
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
                    return ((Pasta) getContext().getApplicationContext()).isFavorite(data);
                } catch (RetrofitError e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done(@Nullable Boolean result) {
                if (result == null) {
                    StaticUtils.onNetworkError(getActivity());
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
                        if (!((Pasta) getContext().getApplicationContext()).toggleFavorite(data)) {
                            return null;
                        } else return ((Pasta) getContext().getApplicationContext()).isFavorite(data);
                    }

                    @Override
                    protected void done(@Nullable Boolean result) {
                        if (result == null) {
                            StaticUtils.onNetworkError(getActivity());
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
                            Toast.makeText(getContext(), R.string.no_playlist_text, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final Map<String, Object> map = new HashMap<String, Object>();
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
                                    ((Pasta) getContext().getApplicationContext()).spotifyService.changePlaylistDetails(((Pasta) getContext().getApplicationContext()).me.id, data.playlistId, map);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            protected void done(@Nullable Boolean result) {
                                if (result == null || !result) {
                                    StaticUtils.onNetworkError(getActivity());
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
        if (pool != null && pool.isExecuting()) pool.cancel();
        ButterKnife.unbind(this);
    }
}
