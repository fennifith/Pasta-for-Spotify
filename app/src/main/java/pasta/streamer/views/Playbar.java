package pasta.streamer.views;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import java.util.ArrayList;

import pasta.streamer.PlayerService;
import pasta.streamer.R;
import pasta.streamer.activities.PlayerActivity;
import pasta.streamer.adapters.NowPlayingAdapter;
import pasta.streamer.data.TrackListData;
import pasta.streamer.utils.Settings;
import pasta.streamer.utils.StaticUtils;

public class Playbar {
    public boolean registered, playing, first;
    private String lastUri;
    private ArrayList<TrackListData> trackList = new ArrayList<>();
    private TrackListData data;
    private Activity activity;

    private View bg;
    private CustomImageView art;
    private ImageView prev, toggle, next;
    private TextView title, subtitle;
    private ProgressBar bar;
    private RecyclerView nextPlaying;
    private BottomSheetBehavior behavior;

    private NowPlayingAdapter adapter;
    private UpdateReceiver receiver;
    private PlaybarListener listener;

    private Drawable play, pause;
    private boolean thumbnails, palette, dark;

    public Playbar(Activity activity) {
        this.activity = activity;
    }

    public interface PlaybarListener {
        void onHide(boolean hidden);
    }

    public void setPlaybarListener(PlaybarListener listener) {
        this.listener = listener;
    }

    public void initPlayBar(View playbar) {
        thumbnails = Settings.isThumbnails(activity);
        palette = Settings.isPalette(activity);
        dark = Settings.isDarkTheme(activity);

        art = (CustomImageView) playbar.findViewById(R.id.art);
        prev = (ImageView) playbar.findViewById(R.id.prev);
        toggle = (ImageView) playbar.findViewById(R.id.toggle);
        next = (ImageView) playbar.findViewById(R.id.next);
        bar = (ProgressBar) playbar.findViewById(R.id.progress);
        title = (TextView) playbar.findViewById(R.id.title);
        subtitle = (TextView) playbar.findViewById(R.id.subtitle);
        bg = playbar.findViewById(R.id.bg);

        behavior = BottomSheetBehavior.from(playbar);
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                listener.onHide(newState == BottomSheetBehavior.STATE_HIDDEN);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        TypedValue tv = new TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.textColorPrimaryInverse, tv, true);

        play = ContextCompat.getDrawable(activity, R.drawable.ic_notify_play);
        pause = ContextCompat.getDrawable(activity, R.drawable.ic_notify_pause);

        bg.setClickable(false);
        toggle.setClickable(false);
        next.setClickable(false);

        nextPlaying = (RecyclerView) playbar.findViewById(R.id.nextPlaying);
        nextPlaying.setLayoutManager(new GridLayoutManager(activity, 1));
        adapter = new NowPlayingAdapter(activity, trackList, 0);
        nextPlaying.setAdapter(adapter);

        first = true;
        registerPlaybar();
    }

    public void registerPlaybar() {
        if (!registered) {
            lastUri = null;
            receiver = new UpdateReceiver();
            activity.registerReceiver(receiver, new IntentFilter(PlayerService.STATE_UPDATE));
            registered = true;
        }
    }

    public void unregisterPlaybar() {
        if (registered) {
            activity.unregisterReceiver(receiver);
            registered = false;
        }
    }

    private class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isPlaying = intent.getBooleanExtra(PlayerService.EXTRA_PLAYING, false);
            int progress = intent.getIntExtra(PlayerService.EXTRA_CUR_TIME, 0);
            int maxProgress = intent.getIntExtra(PlayerService.EXTRA_MAX_TIME, 0);
            int songPos = intent.getIntExtra(PlayerService.EXTRA_CUR_POSITION, 0);
            data = intent.getParcelableExtra(PlayerService.EXTRA_CUR_TRACK);
            ArrayList<TrackListData> list = intent.getParcelableArrayListExtra(PlayerService.EXTRA_TRACK_LIST);

            if (trackList == null || !trackList.equals(list)) trackList = list;

            if (lastUri == null || !data.trackUri.matches(lastUri)) {
                if (first) {
                    bg.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(activity, PlayerActivity.class);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, art, "image");
                                activity.startActivity(i, options.toBundle());
                            } else activity.startActivity(i);
                        }
                    });

                    if (prev != null) {
                        prev.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                StaticUtils.previous(v.getContext());
                                toggle.setImageDrawable(pause);
                            }
                        });
                    }

                    toggle.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((ImageView) v).setImageDrawable(playing ? play : pause);
                            StaticUtils.togglePlay(v.getContext());
                        }
                    });

                    next.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            StaticUtils.next(v.getContext());
                            toggle.setImageDrawable(pause);
                        }
                    });

                    first = false;
                }

                if (thumbnails) {
                    Glide.with(activity).load(data.trackImage).placeholder(R.drawable.preload).into(new GlideDrawableImageViewTarget(art) {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                            art.transition(resource);

                            if (!palette) return;
                            Palette.from(StaticUtils.drawableToBitmap(resource)).generate(new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette palette) {
                                    int color = palette.getDarkVibrantColor(Color.DKGRAY);
                                    if (dark) color = palette.getLightVibrantColor(Color.LTGRAY);

                                    Drawable prev = bg.getBackground();
                                    if (prev instanceof TransitionDrawable) prev = ((TransitionDrawable) prev).getDrawable(1);

                                    TransitionDrawable td = new TransitionDrawable(new Drawable[]{prev, new ColorDrawable(color)});
                                    bg.setBackground(td);
                                    td.startTransition(250);

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        activity.getWindow().setNavigationBarColor(StaticUtils.darkColor(color));
                                    }
                                }
                            });
                        }
                    });
                }

                title.setText(data.trackName);
                subtitle.setText(data.artistName);

                bg.setClickable(true);
                toggle.setClickable(true);
                next.setClickable(true);

                adapter.swapData(trackList, songPos);
                if (trackList != null && trackList.size() > 0) nextPlaying.setVisibility(View.VISIBLE);
                else nextPlaying.setVisibility(View.GONE);

                if (behavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    listener.onHide(false);
                }

                lastUri = data.trackUri;
            }

            if (bar.getProgress() != progress) ObjectAnimator.ofInt(bar, "progress", progress).setDuration(PlayerService.UPDATE_INTERVAL).start();
            if (bar.getMax() != maxProgress) bar.setMax(maxProgress);

            if (isPlaying != playing) {
                playing = isPlaying;
                toggle.setImageDrawable(playing ? pause : play);
            }
        }
    }
}
