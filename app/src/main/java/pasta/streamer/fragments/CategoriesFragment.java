package pasta.streamer.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.afollestad.async.Action;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import kaaes.spotify.webapi.android.models.CategoriesPager;
import kaaes.spotify.webapi.android.models.Category;
import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.adapters.ListAdapter;
import pasta.streamer.data.CategoryListData;
import pasta.streamer.data.ListData;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;

public class CategoriesFragment extends Fragment {

    @BindView(R.id.recyclerView)
    RecyclerView recycler;
    @BindView(R.id.progressBar)
    ProgressBar spinner;

    private Unbinder unbinder;

    ListAdapter adapter;
    Pasta pasta;
    Action action;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recycler, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        pasta = (Pasta) getContext().getApplicationContext();

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        recycler.setLayoutManager(new GridLayoutManager(getContext(), PreferenceUtils.getColumnNumber(getContext(), metrics.widthPixels > metrics.heightPixels)));
        adapter = new ListAdapter(new ArrayList<ListData>());
        recycler.setAdapter(adapter);
        recycler.setHasFixedSize(true);

        action = new Action<List<ListData>>() {
            @NonNull
            @Override
            public String id() {
                return "getCategories";
            }

            @Nullable
            @Override
            protected List<ListData> run() throws InterruptedException {
                CategoriesPager categories = null;
                for (int i = 0; categories == null && i < PreferenceUtils.getRetryCount(getContext()); i++) {
                    try {
                        categories = pasta.spotifyService.getCategories(null);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (StaticUtils.shouldResendRequest(e)) Thread.sleep(200);
                        else break;
                    }
                }
                if (categories == null) return null;

                List<ListData> list = new ArrayList<>();
                for (Category category : categories.categories.items) {
                    list.add(new CategoryListData(category));
                }
                return list;
            }

            @Override
            protected void done(@Nullable List<ListData> result) {
                if (spinner != null) spinner.setVisibility(View.GONE);
                if (result == null) {
                    pasta.onCriticalError(getActivity(), "categories action");
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
