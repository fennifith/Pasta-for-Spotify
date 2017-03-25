package pasta.streamer.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import pasta.streamer.R;
import pasta.streamer.adapters.ListAdapter;
import pasta.streamer.data.ListData;
import pasta.streamer.data.TextListData;

public class AboutFragment extends FabFragment {

    @BindView(R.id.recyclerView)
    RecyclerView rv;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    private Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_about, container, false);
        unbinder = ButterKnife.bind(this, v);

        progressBar.setVisibility(View.GONE);

        List<ListData> textList = new ArrayList<>();

        textList.add(new TextListData(null, getResources().getString(R.string.contributors), null, null));

        String[] peopleNames = getResources().getStringArray(R.array.people_names);
        String[] peopleDescs = getResources().getStringArray(R.array.people_descs);
        String[] peopleImgs = getResources().getStringArray(R.array.people_icons);
        String[] peoplePrimary = getResources().getStringArray(R.array.people_primary);

        for (int i = 0; i < peopleNames.length; i++) {
            textList.add(new TextListData(peopleImgs[i], peopleNames[i], peopleDescs[i], Uri.parse(peoplePrimary[i])));
        }

        textList.add(new TextListData(null, getResources().getString(R.string.libraries), null, null));

        String[] libNames = getResources().getStringArray(R.array.library_names);
        String[] libDescs = getResources().getStringArray(R.array.library_descs);
        String[] libPrimary = getResources().getStringArray(R.array.library_urls);

        for (int i = 0; i < libNames.length; i++) {
            textList.add(new TextListData(null, libNames[i], libDescs[i], Uri.parse(libPrimary[i])));
        }

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new ListAdapter(textList));

        setFab(true, R.drawable.ic_star, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=pasta.streamer")));
            }
        });

        return v;
    }

    @OnClick(R.id.source)
    public void showSource() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TheAndroidMaster/Pasta-for-Spotify")));
    }

    @OnClick(R.id.issues)
    public void showIssues() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TheAndroidMaster/Pasta-for-Spotify/issues")));
    }

    @OnClick(R.id.website)
    public void showWebsite() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://theandroidmaster.github.io/apps/pasta")));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
