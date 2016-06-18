package pasta.streamer.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.afollestad.async.Action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistsPager;
import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.adapters.OmniAdapter;
import pasta.streamer.data.CategoryListData;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.utils.Settings;
import pasta.streamer.utils.StaticUtils;

public class CategoryFragment extends FullScreenFragment {

    @Bind(R.id.topTenTrackListView)
    RecyclerView recycler;
    @Bind(R.id.progressBar2)
    ProgressBar spinner;
    @Bind(R.id.toolbar)
    Toolbar toolbar;

    OmniAdapter adapter;
    GridLayoutManager manager;
    CategoryListData data;
    Action action;
    Map<String, Object> limitMap;
    Pasta pasta;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = DataBindingUtil.inflate(inflater, R.layout.fragment_category, container, false).getRoot();
        ButterKnife.bind(this, rootView);

        pasta = (Pasta) getContext().getApplicationContext();
        data = getArguments().getParcelable("category");
        limitMap = new HashMap<>();
        limitMap.put(SpotifyService.LIMIT, (Settings.getLimit(getContext()) + 1) * 10);

        toolbar.setTitle(data.categoryName);
        toolbar.setNavigationIcon(R.drawable.drawer_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        setData(data.categoryName, Settings.getPrimaryColor(getContext()), Settings.getPrimaryColor(getContext()));

        spinner.setVisibility(View.VISIBLE);

        manager = new GridLayoutManager(getContext(), Settings.getColumnNumber(getContext(), false));

        if (Settings.isCards(getContext())) {
            manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return manager.getSpanCount();
                }
            });
        }

        recycler.setLayoutManager(manager);
        adapter = new OmniAdapter((AppCompatActivity) getActivity(), null);
        recycler.setAdapter(adapter);
        recycler.setHasFixedSize(true);

        action = new Action<ArrayList<PlaylistListData>>() {
            @NonNull
            @Override
            public String id() {
                return "getCategoryPlaylists";
            }

            @Nullable
            @Override
            protected ArrayList<PlaylistListData> run() throws InterruptedException {
                PlaylistsPager pager = null;
                for (int i = 0; pager == null && i < Settings.getRetryCount(getContext()); i++) {
                    try {
                        pager = pasta.spotifyService.getPlaylistsForCategory(data.categoryId, limitMap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (StaticUtils.shouldResendRequest(e)) Thread.sleep(200);
                        else break;
                    }
                }
                if (pager == null) return null;

                ArrayList<PlaylistListData> list = new ArrayList<>();
                for (PlaylistSimple playlist : pager.playlists.items) {
                    list.add(new PlaylistListData(playlist, pasta.me));
                }
                return list;
            }

            @Override
            protected void done(@Nullable ArrayList<PlaylistListData> result) {
                if (spinner != null) spinner.setVisibility(View.GONE);
                if (result == null) {
                    pasta.onNetworkError(getActivity(), "category playlists action");
                    return;
                }
                adapter.swapData(result);
            }
        };
        action.execute();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (action != null && action.isExecuting()) action.cancel();
        ButterKnife.unbind(this);
    }
}
