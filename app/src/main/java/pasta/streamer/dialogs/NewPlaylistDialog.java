package pasta.streamer.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.afollestad.async.Action;

import java.util.HashMap;
import java.util.Map;

import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.utils.ImageUtils;

public class NewPlaylistDialog extends AppCompatDialog {

    private Pasta pasta;
    private PlaylistListData data;
    private OnCreateListener listener;

    private Toolbar toolbar;
    private EditText titleView;
    private CheckBox isPublicView;

    Map<String, Object> map;

    public NewPlaylistDialog(Context context) {
        super(context, R.style.AppTheme_Dialog);
        pasta = (Pasta) context.getApplicationContext();
        map = new HashMap<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_new_playlist);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(ImageUtils.getVectorDrawable(getContext(), R.drawable.ic_close));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShowing()) dismiss();
            }
        });

        toolbar.setTitle(data == null ? R.string.playlist_create : R.string.playlist_modify);

        titleView = (EditText) findViewById(R.id.playlistTitle);
        isPublicView = (CheckBox) findViewById(R.id.pub);

        if (data != null) {
            titleView.setText(data.playlistName);
            isPublicView.setChecked(data.playlistPublic);
        }

        titleView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (titleView.getText().length() < 1)
                    titleView.setError(getContext().getString(R.string.no_playlist_text));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShowing()) dismiss();
            }
        });

        findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (titleView.getText().length() < 1) {
                    titleView.setError(getContext().getString(R.string.no_playlist_text));
                    return;
                }

                map.put("name", titleView.getText().toString());
                map.put("public", isPublicView.isChecked());

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
                            if (data != null)
                                pasta.spotifyService.changePlaylistDetails(pasta.me.id, data.playlistId, map);
                            else pasta.spotifyService.createPlaylist(pasta.me.id, map);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                        return true;
                    }

                    @Override
                    protected void done(@Nullable Boolean result) {
                        if (result == null || !result) {
                            pasta.onError(getContext(), "modify playlist action");
                        } else {
                            listener.onCreate();
                            if (data != null) {
                                data.playlistName = (String) map.get("name");
                                data.playlistPublic = (boolean) map.get("public");
                            }
                        }
                    }
                }.execute();

                if (isShowing()) dismiss();
            }
        });
    }

    public NewPlaylistDialog setPlaylist(@NonNull PlaylistListData data) {
        this.data = data;
        map.put("name", data.playlistName);
        map.put("public", data.playlistPublic);
        if (titleView != null) titleView.setText(data.playlistName);
        if (isPublicView != null) isPublicView.setChecked(data.playlistPublic);
        if (toolbar != null) toolbar.setTitle(R.string.playlist_modify);
        return this;
    }

    public NewPlaylistDialog setOnCreateListener(OnCreateListener listener) {
        this.listener = listener;
        return this;
    }

    public interface OnCreateListener {
        void onCreate();
    }

}
