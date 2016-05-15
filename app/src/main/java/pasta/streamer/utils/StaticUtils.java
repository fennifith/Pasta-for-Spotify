package pasta.streamer.utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.afollestad.async.Action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import pasta.streamer.Pasta;
import pasta.streamer.PlayerService;
import pasta.streamer.R;
import pasta.streamer.data.PlaylistListData;
import pasta.streamer.data.TrackListData;

public class StaticUtils {

    public static void onNetworkError(final Activity activity) {
        ((Pasta) activity.getApplicationContext()).onNetworkError(activity);
    }

    public static void restart(Activity activity) {
        try {
            Intent i = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int mPendingIntentId = 223344;
            PendingIntent mPendingIntent = PendingIntent.getActivity(activity, mPendingIntentId, i, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            activity.finish();
        }
    }

    public static void showAddToDialog(final Context context, final TrackListData data) {
        new Action<ArrayList<PlaylistListData>>() {
            @NonNull
            @Override
            public String id() {
                return "showAddToDialog";
            }

            @Nullable
            @Override
            protected ArrayList<PlaylistListData> run() throws InterruptedException {
                Pasta pasta = (Pasta) context.getApplicationContext();
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
                if (result == null) return;
                String[] names = new String[result.size()];
                for (int i = 0; i < result.size(); i++) {
                    names[i] = result.get(i).playlistName;
                }
                new AlertDialog.Builder(context).setTitle(R.string.add).setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        dialog.dismiss();
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
                                    PlaylistListData playlist = result.get(which);
                                    Map<String, Object> tracks = new HashMap<>();
                                    tracks.put("uris", "spotify:track:" + data.trackId);
                                    ((Pasta) context.getApplicationContext()).spotifyService.addTracksToPlaylist(playlist.playlistOwnerId, playlist.playlistId, tracks, tracks);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            protected void done(@Nullable Boolean result) {
                                if (result == null) result = false;
                                Toast.makeText(context, result ? R.string.added : R.string.error, Toast.LENGTH_SHORT).show();
                            }
                        }.execute();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
            }
        }.execute();
    }

    public static String timeToString(int minutes, int seconds) {
        return String.format(Locale.getDefault(), "%1$02d", minutes) + ":" + String.format(Locale.getDefault(), "%1$02d", seconds);
    }

    public static int darkColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f;
        return Color.HSVToColor(hsv);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        width = width > 0 ? width : 1;
        int height = drawable.getIntrinsicHeight();
        height = height > 0 ? height : 1;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static String getAlbumUrl(String id) {
        return "https://open.spotify.com/album/" + id;
    }

    public static String getPlaylistUrl(String user, String id) {
        return "https://open.spotify.com/user/" + user + "/playlist/" + id;
    }

    public static String getArtistUrl(String id) {
        return "https://open.spotify.com/artist/" + id;
    }

    public static int getStatusBarMargin(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0;

        int height = 0;
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) height = context.getResources().getDimensionPixelSize(resId);
        return height;
    }

    public static void play(int startPos, ArrayList<TrackListData> trackList, Context context) {
        Intent intent = new Intent(PlayerService.ACTION_PLAY);
        intent.setClass(context, PlayerService.class);
        intent.putExtra(PlayerService.ACTION_PLAY_EXTRA_START_POS, startPos);
        intent.putParcelableArrayListExtra(PlayerService.ACTION_PLAY_EXTRA_TRACKS, trackList);
        context.startService(intent);
    }

    public static void togglePlay(Context context) {
        Intent intent = new Intent(PlayerService.ACTION_TOGGLE);
        intent.setClass(context, PlayerService.class);
        context.startService(intent);
    }

    public static void next(Context context) {
        Intent intent = new Intent(PlayerService.ACTION_NEXT);
        intent.setClass(context, PlayerService.class);
        context.startService(intent);
    }

    public static void previous(Context context) {
        Intent intent = new Intent(PlayerService.ACTION_PREV);
        intent.setClass(context, PlayerService.class);
        context.startService(intent);
    }

    public static void jumpToPositionInTrack(int position, Context context) {
        Intent intent = new Intent(PlayerService.ACTION_MOVE_POS);
        intent.setClass(context, PlayerService.class);
        intent.putExtra(PlayerService.ACTION_MOVE_POS_EXTRA_POS, position);
        context.startService(intent);
    }

}
