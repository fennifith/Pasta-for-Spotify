package pasta.streamer.fragments;

import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.async.Action;

import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnPageChange;
import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.adapters.FavoritePagerAdapter;
import pasta.streamer.utils.Settings;

public class FavoritesFragment extends FabFragment {

    @Bind(R.id.vp)
    ViewPager vp;
    @Bind(R.id.tl)
    TabLayout tl;

    private FavoritePagerAdapter adapter;
    private Pasta pasta;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = DataBindingUtil.inflate(inflater, R.layout.fragment_favorites, container, false).getRoot();
        ButterKnife.bind(this, rootView);

        pasta = (Pasta) getContext().getApplicationContext();

        adapter = new FavoritePagerAdapter(getActivity(), getActivity().getSupportFragmentManager());
        vp.setAdapter(adapter);
        tl.setupWithViewPager(vp);
        tl.setSelectedTabIndicatorColor(Settings.getAccentColor(getContext()));

        setFab(true, R.drawable.ic_add, getFabClickListener());

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

    @OnPageChange(R.id.vp)
    public void onPageSelected(int position) {
        if (position == 0) setFab(true, R.drawable.ic_add, getFabClickListener());
        else setFab(false, R.drawable.ic_add, getFabClickListener());
    }

    public View.OnClickListener getFabClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View layout = getActivity().getLayoutInflater().inflate(R.layout.dialog_layout, null);

                new AlertDialog.Builder(getContext()).setTitle(R.string.playlist_create).setView(layout).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        if (((AppCompatEditText) layout.findViewById(R.id.title)).getText().toString().length() < 1) {
                            Toast.makeText(getContext(), R.string.no_playlist_text, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        final Map<String, Object> map = new HashMap<>();
                        map.put("name", ((AppCompatEditText) layout.findViewById(R.id.title)).getText().toString());
                        map.put("public", ((AppCompatCheckBox) layout.findViewById(R.id.pub)).isChecked());

                        new Action<Boolean>() {
                            @NonNull
                            @Override
                            public String id() {
                                return "makePlaylist";
                            }

                            @Nullable
                            @Override
                            protected Boolean run() throws InterruptedException {
                                try {
                                    pasta.spotifyService.createPlaylist(pasta.me.id, map);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            protected void done(@Nullable Boolean result) {
                                if (result == null || !result) {
                                    pasta.onNetworkError(getActivity());
                                } else {
                                    adapter.load();
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
            }
        };
    }
}
