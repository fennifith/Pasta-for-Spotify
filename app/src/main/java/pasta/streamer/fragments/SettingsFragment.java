package pasta.streamer.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.player.PlaybackBitrate;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import pasta.streamer.R;
import pasta.streamer.activities.HomeActivity;
import pasta.streamer.utils.Settings;
import pasta.streamer.utils.StaticUtils;
import pasta.streamer.views.CustomImageView;

public class SettingsFragment extends Fragment {

    @Bind(R.id.primary_color)
    CustomImageView primary;
    @Bind(R.id.accent_color)
    CustomImageView accent;

    private View rootView;
    private Snackbar snackbar;

    private int selectedLimit, selectedQuality, selectedOrder;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = DataBindingUtil.inflate(inflater, R.layout.fragment_settings, container, false).getRoot();
        ButterKnife.bind(this, rootView);

        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (snackbar != null) snackbar.dismiss();
        ButterKnife.unbind(this);
    }

    @OnCheckedChanged(R.id.preload)
    public void changePreload(boolean preload) {
        if (prefs == null) return;
        if (preload != Settings.isPreload(getContext())) onChange();
        prefs.edit().putBoolean(Settings.PRELOAD, preload).apply();
    }

    @OnClick(R.id.limit)
    public void changeLimit() {
        new AlertDialog.Builder(getContext()).setTitle(R.string.limit).setSingleChoiceItems(R.array.limits, Settings.getLimit(getContext()), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedLimit = which;
            }
        }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putInt(Settings.LIMIT, selectedLimit).apply();
                onChange();
                dialog.dismiss();
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).create().show();
    }

    @OnClick(R.id.signout)
    public void signOut() {
        new AlertDialog.Builder(getContext()).setTitle(R.string.sign_out).setMessage(R.string.sign_out_msg).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AuthenticationClient.clearCookies(getContext().getApplicationContext());

                try {
                    File cache = getContext().getCacheDir();
                    File appDir = new File(cache.getParent());
                    if(appDir.exists()){
                        String[] children = appDir.list();
                        for(String child : children){
                            if(!child.equals("lib")) deleteDir(new File(appDir, child));
                        }
                    }

                    getActivity().finish();
                } catch (Exception e) {
                    e.printStackTrace();

                    try {
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:pasta.streamer"));
                        startActivity(intent);
                    } catch (ActivityNotFoundException ex) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                        startActivity(intent);
                    }
                    Toast.makeText(getContext(), "You can clear the app data from this screen", Toast.LENGTH_SHORT).show();
                }
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).create().show();
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
        }

        return dir.delete();
    }

    @OnCheckedChanged(R.id.darkmode)
    public void changeDarkMode(boolean dark) {
        if (prefs == null) return;
        if (dark != Settings.isDarkTheme(getContext())) onChange();
        prefs.edit().putBoolean(Settings.DARK_THEME, dark).apply();
    }

    @OnCheckedChanged(R.id.thumbnails)
    public void changeThumbnails(boolean thumbnails) {
        if (prefs == null) return;
        if (thumbnails != Settings.isThumbnails(getContext())) onChange();
        prefs.edit().putBoolean(Settings.THUMBNAILS, thumbnails).apply();
    }

    @OnCheckedChanged(R.id.cards)
    public void changeCards(boolean cards) {
        if (prefs == null) return;
        if (cards != Settings.isCards(getContext())) onChange();
        prefs.edit().putBoolean(Settings.CARDS, cards).apply();
    }

    @OnCheckedChanged(R.id.palette)
    public void changePalette(boolean palette) {
        if (prefs == null) return;
        if (palette != Settings.isPalette(getContext())) onChange();
        prefs.edit().putBoolean(Settings.PALETTE, palette).apply();
    }

    @OnClick(R.id.primary)
    public void setPrimary() {
        new ColorChooserDialog.Builder((HomeActivity) getActivity(), R.string.primary_color)
                .titleSub(R.string.primary_color)
                .doneButton(R.string.save)
                .cancelButton(R.string.cancel)
                .backButton(R.string.md_back_label)
                .preselect(Settings.getPrimaryColor(getContext()))
                .show();
    }

    @OnClick(R.id.accent)
    public void setAccent() {
        new ColorChooserDialog.Builder((HomeActivity) getActivity(), R.string.accent_color)
                .titleSub(R.string.accent_color)
                .doneButton(R.string.save)
                .cancelButton(R.string.cancel)
                .backButton(R.string.md_back_label)
                .accentMode(true)
                .preselect(Settings.getAccentColor(getContext()))
                .show();
    }

    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        switch (dialog.getTitle()) {
            case R.string.primary_color:
                if (Settings.getPrimaryColor(getContext()) == selectedColor) return;
                prefs.edit().putInt(Settings.PRIMARY, selectedColor).apply();
                primary.transition(new ColorDrawable(selectedColor));
                break;
            case R.string.accent_color:
                if (Settings.getAccentColor(getContext()) == selectedColor) return;
                prefs.edit().putInt(Settings.ACCENT, selectedColor).apply();
                accent.transition(new ColorDrawable(selectedColor));
                break;
            default:
                return;
        }
        onChange();
    }

    @OnClick(R.id.quality)
    public void setQuality() {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.quality)
                .setSingleChoiceItems(R.array.qualities, Settings.getSelectedQuality(getContext()), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectedQuality = which;
                    }
                }).setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int id) {
                switch (selectedQuality) {
                    case 0:
                        prefs.edit().putString(Settings.QUALITY, PlaybackBitrate.BITRATE_LOW.toString()).apply();
                        break;
                    case 1:
                        prefs.edit().putString(Settings.QUALITY, PlaybackBitrate.BITRATE_NORMAL.toString()).apply();
                        break;
                    case 2:
                        prefs.edit().putString(Settings.QUALITY, PlaybackBitrate.BITRATE_HIGH.toString()).apply();
                        break;
                }
                onChange();
                d.dismiss();
            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int id) {
                d.dismiss();
            }
        }).create().show();
    }

    @OnClick(R.id.organize)
    public void setOrganization() {
        Settings.getOrderingDialog(getContext(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedOrder = which;
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putInt(Settings.ORDER, selectedOrder).apply();
                onChange();
                d.dismiss();
            }
        }).show();
    }

    private void onChange() {
        final Activity activity = getActivity();
        snackbar = Snackbar.make(rootView, R.string.restart_msg, Snackbar.LENGTH_INDEFINITE).setAction(R.string.restart, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StaticUtils.restart(activity);
            }
        });

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) snackbar.getView().getLayoutParams();
        params.bottomMargin = getActivity().getResources().getDimensionPixelSize(R.dimen.playbar_size);
        snackbar.getView().setLayoutParams(params);

        snackbar.show();
    }
}
