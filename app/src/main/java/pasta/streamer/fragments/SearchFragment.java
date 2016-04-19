package pasta.streamer.fragments;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import pasta.streamer.R;
import pasta.streamer.adapters.SectionedOmniAdapter;

public class SearchFragment extends Fragment {

    private SectionedOmniAdapter adapter;
    private ArrayList list;
    private GridLayoutManager manager;

    @Bind(R.id.progressBar)
    ProgressBar spinner;
    @Bind(R.id.recyclerView)
    RecyclerView recycler;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recycler, container, false);
        ButterKnife.bind(this, rootView);

        if (list == null) list = new ArrayList<>();
        else spinner.setVisibility(View.GONE);

        adapter = new SectionedOmniAdapter((AppCompatActivity) getActivity(), list) ;
        manager = new GridLayoutManager(getContext(), 1);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) != 0) return manager.getSpanCount();
                return 1;
            }
        });
        recycler.setLayoutManager(manager);
        recycler.setAdapter(adapter);

        return rootView;
    }

    public void addData(Parcelable data) {
        if (adapter != null) adapter.addData(data);
        if (spinner != null) spinner.setVisibility(View.GONE);
    }

    public void addData(ArrayList data) {
        if (adapter != null) adapter.addData(data);
        if (spinner != null) spinner.setVisibility(View.GONE);
    }

    public void swapData(ArrayList list) {
        if (adapter != null) adapter.swapData(list);
        if (spinner != null) spinner.setVisibility(View.GONE);
    }

    public void clear() {
        if (adapter != null) adapter.clear();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}
