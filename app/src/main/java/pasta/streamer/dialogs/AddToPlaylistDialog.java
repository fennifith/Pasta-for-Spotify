package pasta.streamer.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.afollestad.async.Action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import pasta.streamer.Pasta;
import pasta.streamer.R;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.data.TrackListData;
import pasta.streamer.utils.ImageUtils;

public class AddToPlaylistDialog extends AppCompatDialog {

    private Pasta pasta;
    private TrackListData data;

    ListView listView;
    ArrayList<PlaylistListData> playlists;


    public AddToPlaylistDialog(Context context, TrackListData data) {
        super(context, R.style.AppTheme_Dialog);
        pasta = (Pasta) context.getApplicationContext();
        this.data = data;
    }

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_to_playlist);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Drawable close = ImageUtils.getVectorDrawable(getContext(), R.drawable.ic_close);
        DrawableCompat.setTint(close, Color.WHITE);
        toolbar.setNavigationIcon(close);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShowing()) dismiss();
            }
        });

        listView = (ListView) findViewById(R.id.listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new Action<Boolean>() {
                    @NonNull
                    @Override
                    public String id() {
                        return "addToPlaylist";
                    }

                    @Nullable
                    @Override
                    protected Boolean run() throws InterruptedException {
                        try {
                            PlaylistListData playlist = playlists.get(position);
                            Map<String, Object> tracks = new HashMap<>();
                            tracks.put("uris", "spotify:track:" + data.trackId);
                            pasta.spotifyService.addTracksToPlaylist(playlist.playlistOwnerId, playlist.playlistId, tracks, tracks);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }

                        return true;
                    }

                    @Override
                    protected void done(@Nullable Boolean result) {
                        if (result == null) result = false;
                        pasta.showToast(pasta.getString(result ? R.string.added : R.string.error));
                    }
                }.execute();

                if (isShowing()) dismiss();
            }
        });

        new Action<ArrayList<PlaylistListData>>() {
            @NonNull
            @Override
            public String id() {
                return "showAddToDialog";
            }

            @Nullable
            @Override
            protected ArrayList<PlaylistListData> run() throws InterruptedException {
                Pager<PlaylistSimple> pager;
                try {
                    pager = pasta.spotifyService.getMyPlaylists();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }

                ArrayList<PlaylistListData> playlists = new ArrayList<PlaylistListData>();
                for (PlaylistSimple playlist : pager.items) {
                    PlaylistListData data = new PlaylistListData(playlist, pasta.me);
                    if (data.editable) playlists.add(data);
                }
                return playlists;
            }

            @Override
            protected void done(@Nullable final ArrayList<PlaylistListData> result) {
                if (result == null) {
                    pasta.onError(getContext(), "add to playlist dialog");
                    AddToPlaylistDialog.this.dismiss();
                    return;
                }

                AddToPlaylistDialog.this.playlists = result;

                String[] names = new String[result.size()];
                for (int i = 0; i < result.size(); i++) {
                    names[i] = result.get(i).playlistName;
                }

                listView.setAdapter(new ArrayAdapter<>(getContext(), R.layout.playlist_item_text, R.id.title, names));
            }
        }.execute();
    }
}
