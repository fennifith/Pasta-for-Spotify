package pasta.streamer.views;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import pasta.streamer.PlayerService;
import pasta.streamer.R;
import pasta.streamer.activities.PlayerActivity;
import pasta.streamer.data.TrackListData;
import pasta.streamer.utils.ImageUtils;
import pasta.streamer.utils.PreferenceUtils;
import pasta.streamer.utils.StaticUtils;

public class Playbar {
    public boolean registered, playing, first;
    private String lastUri;
    private TrackListData data;
    private Activity activity;

    private View playbar;
    private CustomImageView art;
    private ImageView prev, toggle, next;
    private TextView title, subtitle;
    private ProgressBar bar;
    private BottomSheetBehavior behavior;

    private UpdateReceiver receiver;
    private PlaybarListener listener;

    private Drawable play, pause;
    private boolean thumbnails;

    public Playbar(Activity activity) {
        this.activity = activity;
    }

    public void setPlaybarListener(PlaybarListener listener) {
        this.listener = listener;
    }

    public void initPlayBar(View playbar) {
        this.playbar = playbar;
        ViewCompat.setElevation(playbar, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, activity.getResources().getDisplayMetrics()));

        thumbnails = PreferenceUtils.isThumbnails(activity);

        art = (CustomImageView) playbar.findViewById(R.id.art);
        prev = (ImageView) playbar.findViewById(R.id.prev);
        toggle = (ImageView) playbar.findViewById(R.id.toggle);
        next = (ImageView) playbar.findViewById(R.id.next);
        bar = (ProgressBar) playbar.findViewById(R.id.progress);
        title = (TextView) playbar.findViewById(R.id.title);
        subtitle = (TextView) playbar.findViewById(R.id.subtitle);

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

        play = ImageUtils.getVectorDrawable(activity, R.drawable.ic_play);
        pause = ImageUtils.getVectorDrawable(activity, R.drawable.ic_pause);

        playbar.setClickable(false);
        toggle.setClickable(false);
        next.setClickable(false);

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

    public interface PlaybarListener {
        void onHide(boolean hidden);
    }

    private class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isPlaying = intent.getBooleanExtra(PlayerService.EXTRA_PLAYING, false);
            int progress = intent.getIntExtra(PlayerService.EXTRA_CUR_TIME, 0);
            int maxProgress = intent.getIntExtra(PlayerService.EXTRA_MAX_TIME, 0);
            data = intent.getParcelableExtra(PlayerService.EXTRA_CUR_TRACK);

            if (lastUri == null || !data.trackUri.matches(lastUri)) {
                if (first) {
                    playbar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Bundle bundle = ActivityOptionsCompat.makeScaleUpAnimation(v, (int) v.getX(), (int) v.getY(), v.getWidth(), v.getHeight()).toBundle();
                            activity.startActivity(new Intent(activity, PlayerActivity.class), bundle);
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
                    Glide.with(activity).load(data.trackImage).placeholder(ImageUtils.getVectorDrawable(activity, R.drawable.preload)).thumbnail(0.2f).into(new GlideDrawableImageViewTarget(art) {
                        @Override
                        public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                            art.transition(resource);

                            Palette.from(ImageUtils.drawableToBitmap(resource)).generate(new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette palette) {
                                    int color = palette.getLightVibrantColor(Color.LTGRAY);

                                    Drawable prev = playbar.getBackground();
                                    if (prev instanceof TransitionDrawable) prev = ((TransitionDrawable) prev).getDrawable(1);

                                    TransitionDrawable td = new TransitionDrawable(new Drawable[]{prev, new ColorDrawable(color)});
                                    playbar.setBackground(td);
                                    td.startTransition(250);

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        activity.getWindow().setNavigationBarColor(ImageUtils.darkColor(color));
                                    }
                                }
                            });
                        }
                    });
                }

                title.setText(data.trackName);
                if (data.artists.size() > 0) subtitle.setText(data.artists.get(0).artistName);
                else subtitle.setText("");

                playbar.setClickable(true);
                toggle.setClickable(true);
                next.setClickable(true);

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
