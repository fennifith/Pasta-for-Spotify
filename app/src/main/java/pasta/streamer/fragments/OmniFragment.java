package pasta.streamer.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import pasta.streamer.R;
import pasta.streamer.adapters.OmniAdapter;

public class OmniFragment extends Fragment {

    private OmniAdapter adapter;
    private ArrayList list;
    private GridLayoutManager manager;
    private int behavior;

    @Bind(R.id.progressBar)
    ProgressBar spinner;
    @Bind(R.id.recyclerView)
    RecyclerView recycler;
    @Bind(R.id.empty)
    View empty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recycler, container, false);
        ButterKnife.bind(this, rootView);

        if (list == null) list = new ArrayList<>();
        else {
            empty.setVisibility(list.size() == 0 ? View.VISIBLE : View.GONE);
            spinner.setVisibility(View.GONE);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        adapter = new OmniAdapter((AppCompatActivity) getActivity(), list);
        manager = new GridLayoutManager(getContext(), metrics.widthPixels > metrics.heightPixels ? 3 : 2);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) != 0) return manager.getSpanCount();
                return 1;
            }
        });

        if (behavior == OmniAdapter.BEHAVIOR_FAVORITE) {
            adapter.setFavoriteBehavior();
        }

        recycler.setLayoutManager(manager);
        recycler.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    public void addData(Parcelable data) {
        if (adapter != null) adapter.addData(data);
        else {
            if (list == null) list = new ArrayList();
            list.add(data);
        }
        if (spinner != null) spinner.setVisibility(View.GONE);
        if (empty != null) empty.setVisibility(View.GONE);
    }

    public void swapData(ArrayList list) {
        this.list = list;
        if (adapter != null) adapter.swapData(this.list);
        if (spinner != null) spinner.setVisibility(View.GONE);
        if (empty != null) {
            empty.setVisibility(list.size() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    public void clear() {
        if (list != null) list.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        if (empty != null) empty.setVisibility(View.VISIBLE);
    }

    public void setFavoriteBehavior() {
        if (adapter != null) adapter.setFavoriteBehavior();
        behavior = OmniAdapter.BEHAVIOR_FAVORITE;
    }
}
