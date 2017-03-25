package pasta.streamer.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistsPager;
import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.adapters.ListAdapter;
import pasta.streamer.data.CategoryListData;
import pasta.streamer.data.ListData;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;

public class CategoryFragment extends FullScreenFragment {

    @BindView(R.id.topTenTrackListView)
    RecyclerView recycler;
    @BindView(R.id.progressBar2)
    ProgressBar spinner;
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private Unbinder unbinder;

    ListAdapter adapter;
    GridLayoutManager manager;
    CategoryListData data;
    Action action;
    Map<String, Object> limitMap;
    Pasta pasta;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_category, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        pasta = (Pasta) getContext().getApplicationContext();
        data = getArguments().getParcelable("category");
        limitMap = new HashMap<>();
        limitMap.put(SpotifyService.LIMIT, (PreferenceUtils.getLimit(getContext()) + 1) * 10);

        toolbar.setTitle(data.categoryName);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        int color = ContextCompat.getColor(getContext(), R.color.colorPrimary);
        setData(data.categoryName, color, color);

        spinner.setVisibility(View.VISIBLE);

        manager = new GridLayoutManager(getContext(), 1);

        recycler.setLayoutManager(manager);
        adapter = new ListAdapter(new ArrayList<ListData>());
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
                for (int i = 0; pager == null && i < PreferenceUtils.getRetryCount(getContext()); i++) {
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
                    pasta.onCriticalError(getActivity(), "category playlists action");
                    return;
                }
                adapter.setList(result);
            }
        };
        action.execute();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (action != null && action.isExecuting()) action.cancel();
        unbinder.unbind();
    }
}
