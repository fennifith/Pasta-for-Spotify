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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
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

import com.afollestad.async.Action;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.activities.PlayerActivity;
import pasta.streamer.adapters.TrackAdapter;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.data.TrackListData;
import pasta.streamer.dialogs.NewPlaylistDialog;
import pasta.streamer.utils.ImageUtils;
import pasta.streamer.utils.PreferenceUtils;
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

        palette = PreferenceUtils.isPalette(getContext());

        fab.setBackgroundTintList(ColorStateList.valueOf(PreferenceUtils.getAccentColor(getContext())));
        fab.setImageDrawable(ImageUtils.getVectorDrawable(getContext(), R.drawable.ic_play));

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
        recycler.setLayoutManager(new GridLayoutManager(getContext(), PreferenceUtils.isListTracks(getContext()) ? 1 : PreferenceUtils.getColumnNumber(getContext(), metrics.widthPixels > metrics.heightPixels)));
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
                adapter.sort(PreferenceUtils.getTrackOrder(getContext()));
                trackList = result;
            }
        };
        action.execute();

        Glide.with(getContext()).load(data.playlistImageLarge).placeholder(ImageUtils.getVectorDrawable(getContext(), R.drawable.preload)).into(new GlideDrawableImageViewTarget(header) {
            @Override
            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                if (header != null) header.transition(resource);

                if (palette) {
                    Palette.from(ImageUtils.drawableToBitmap(resource)).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            int primary = palette.getMutedColor(Color.GRAY);
                            int accent = palette.getVibrantColor(ImageUtils.darkColor(primary));
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
        toolbar.setNavigationIcon(R.drawable.ic_back);
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
                new NewPlaylistDialog(getContext()).setPlaylist(data).setOnCreateListener(new NewPlaylistDialog.OnCreateListener() {
                    @Override
                    public void onCreate() {
                        toolbar.setTitle(data.playlistName);
                    }
                }).show();
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
                PreferenceUtils.getOrderingDialog(getContext(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedOrder = which;
                    }
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(PreferenceUtils.ORDER, selectedOrder).apply();
                        adapter.sort(PreferenceUtils.getTrackOrder(getContext()));
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
