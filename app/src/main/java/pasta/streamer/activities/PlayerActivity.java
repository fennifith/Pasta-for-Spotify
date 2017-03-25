package pasta.streamer.activities;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.async.Action;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ooo.oxo.library.widget.PullBackLayout;
import pasta.streamer.Pasta;
import pasta.streamer.PlayerService;
import pasta.streamer.R;
import pasta.streamer.adapters.NowPlayingAdapter;
import pasta.streamer.data.AlbumListData;
import pasta.streamer.data.ArtistListData;
import pasta.streamer.data.TrackListData;
import pasta.streamer.dialogs.AddToPlaylistDialog;
import pasta.streamer.utils.ImageUtils;
import pasta.streamer.utils.StaticUtils;
import pasta.streamer.views.CustomImageView;

public class PlayerActivity extends AppCompatActivity implements PullBackLayout.Callback {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.trackImage)
    CustomImageView art;
    @Nullable
    @BindView(R.id.backgroundImage)
    CustomImageView backgroundImage;
    @BindView(R.id.currentDuration)
    TextView currentDuration;
    @BindView(R.id.finalDuration)
    TextView finalDuration;
    @BindView(R.id.prevButton)
    CustomImageView prevButton;
    @BindView(R.id.playButton)
    CustomImageView playButton;
    @BindView(R.id.nextButton)
    CustomImageView nextButton;
    @BindView(R.id.bg)
    View bg;
    @BindView(R.id.seekBar)
    AppCompatSeekBar seekBar;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.title)
    TextView title;
    @Nullable
    @BindView(R.id.subtitle2)
    TextView subtitle2;
    @BindView(R.id.subtitle)
    TextView subtitle;
    @BindView(R.id.rv)
    RecyclerView rv;
    @BindView(R.id.puller)
    PullBackLayout puller;

    private ArrayList<TrackListData> trackList;
    private MenuItem fav;

    private boolean playing, playbarDragging;
    private int curPosition = -1;
    private TrackListData data;
    private String lastUri;
    private Drawable play, pause;
    private UpdateReceiver updateReceiver;
    private NowPlayingAdapter adapter;
    private Drawable imageDrawable;
    private Integer imageColor;
    private Pasta pasta;
    private Action action;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);

        puller.setCallback(this);

        pasta = (Pasta) getApplicationContext();
        pasta.setScreen(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && subtitle2 == null) {
            BottomSheetBehavior.from(rv).setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                int scroll = 0;

                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) scroll = 0;
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    int padding = (int) (StaticUtils.getStatusBarMargin(PlayerActivity.this) * (slideOffset < 0 ? slideOffset + 1 : slideOffset));
                    rv.setPadding(0, padding, 0, 0);

                    rv.scrollBy(0, scroll - padding);
                    scroll = padding;
                }
            });
        }

        play = ImageUtils.getVectorDrawable(this, R.drawable.ic_play);
        pause = ImageUtils.getVectorDrawable(this, R.drawable.ic_pause);
        playButton.setImageDrawable(play);

        if (backgroundImage != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TypedValue value = new TypedValue();
            getTheme().resolveAttribute(android.R.attr.actionBarSize, value, true);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, TypedValue.complexToDimensionPixelSize(value.data, getResources().getDisplayMetrics()));
            params.topMargin = StaticUtils.getStatusBarMargin(this);
            toolbar.setLayoutParams(params);
        }

        setLoading(true);
        setClickable(false);

        rv.setLayoutManager(new GridLayoutManager(this, 1));
        adapter = new NowPlayingAdapter(this, trackList, 0);
        rv.setAdapter(adapter);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) StaticUtils.jumpToPositionInTrack(progress, seekBar.getContext());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                playbarDragging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                playbarDragging = false;
            }
        });

        updateReceiver = new UpdateReceiver();
        registerReceiver(updateReceiver, new IntentFilter(PlayerService.STATE_UPDATE));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
    }

    @OnClick(R.id.prevButton)
    public void prev() {
        imageDrawable = null;
        StaticUtils.previous(this);
        setLoading(true);
        setClickable(false);
    }

    @OnClick(R.id.playButton)
    public void toggle(ImageView v) {
        v.setImageDrawable(playing ? play : pause);
        StaticUtils.togglePlay(this);
    }

    @OnClick(R.id.nextButton)
    public void next() {
        imageDrawable = null;
        StaticUtils.next(this);
        setLoading(true);
        setClickable(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        lastUri = null;
        trackList = null;
        if (updateReceiver != null)
            registerReceiver(updateReceiver, new IntentFilter(PlayerService.STATE_UPDATE));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(updateReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_track, menu);
        fav = menu.findItem(R.id.action_fav);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_fav:
                new Action<Boolean>() {
                    @NonNull
                    @Override
                    public String id() {
                        return "favTrack";
                    }

                    @Nullable
                    @Override
                    protected Boolean run() throws InterruptedException {
                        if (trackList == null || !pasta.toggleFavorite(trackList.get(curPosition))) {
                            return null;
                        } else return pasta.isFavorite(trackList.get(curPosition));
                    }

                    @Override
                    protected void done(@Nullable Boolean result) {
                        if (result == null) {
                            pasta.onError(PlayerActivity.this, "favorite track menu action");
                            return;
                        }
                        if (result) {
                            item.setIcon(R.drawable.ic_fav);
                        } else {
                            item.setIcon(R.drawable.ic_unfav);
                        }
                    }

                }.execute();
                break;
            case R.id.action_add:
                if (trackList != null)
                    new AddToPlaylistDialog(this, trackList.get(curPosition)).show();
                else pasta.onError(PlayerActivity.this, "add playlist dialog");
                break;
            case R.id.action_album:
                new Action<AlbumListData>() {
                    @NonNull
                    @Override
                    public String id() {
                        return "gotoAlbum";
                    }

                    @Nullable
                    @Override
                    protected AlbumListData run() throws InterruptedException {
                        if (trackList != null)
                            return pasta.getAlbum(trackList.get(curPosition).albumId);
                        else return null;
                    }

                    @Override
                    protected void done(@Nullable AlbumListData result) {
                        if (result == null) {
                            pasta.onError(PlayerActivity.this, "album menu action");
                            return;
                        }
                        Intent i = new Intent(PlayerActivity.this, HomeActivity.class);
                        i.putExtra("album", result);
                        startActivity(i);
                    }
                }.execute();
                break;
            case R.id.action_artist:
                TrackListData track = trackList.get(curPosition);
                if (track.artists.size() > 0) {
                    new Action<ArtistListData>() {
                        @NonNull
                        @Override
                        public String id() {
                            return "gotoArtist";
                        }

                        @Nullable
                        @Override
                        protected ArtistListData run() throws InterruptedException {
                            if (trackList != null)
                                return pasta.getArtist(trackList.get(curPosition).artists.get(0).artistId);
                            else return null;
                        }

                        @Override
                        protected void done(@Nullable ArtistListData result) {
                            if (result == null) {
                                pasta.onError(PlayerActivity.this, "artist menu action");
                                return;
                            }
                            Intent i = new Intent(PlayerActivity.this, HomeActivity.class);
                            i.putExtra("artist", result);
                            startActivity(i);
                        }
                    }.execute();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //orientation changed, layout is invalid
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_down);
    }

    private void setClickable(boolean clickable) {
        playButton.setClickable(clickable);
        prevButton.setClickable(clickable);
        nextButton.setClickable(clickable);
        seekBar.setClickable(clickable);
    }

    private boolean isClickable() {
        return playButton.isClickable();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private boolean isLoading() {
        return progressBar.getVisibility() == View.VISIBLE;
    }

    @Override
    public void onPullStart() {
    }

    @Override
    public void onPull(float v) {
        if (backgroundImage != null) {
            backgroundImage.setScaleX(1 - (v / 3));
            backgroundImage.setScaleY(1 - (v / 3));
        }
    }

    @Override
    public void onPullCancel() {
    }

    @Override
    public void onPullComplete() {
        finish();
    }

    private class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, final Intent intent) {
            playing = intent.getBooleanExtra(PlayerService.EXTRA_PLAYING, false);
            curPosition = intent.getIntExtra(PlayerService.EXTRA_CUR_POSITION, -1);
            int curTime = intent.getIntExtra(PlayerService.EXTRA_CUR_TIME, -1);
            int maxTime = intent.getIntExtra(PlayerService.EXTRA_MAX_TIME, -1);
            data = intent.getParcelableExtra(PlayerService.EXTRA_CUR_TRACK);
            ArrayList<TrackListData> list = intent.getParcelableArrayListExtra(PlayerService.EXTRA_TRACK_LIST);

            if (trackList == null || !trackList.equals(list)) trackList = list;

            if ((lastUri == null || !data.trackUri.matches(lastUri))) {
                if (!isLoading()) setLoading(true);
                if (action != null && action.isExecuting()) action.cancel();

                Glide.with(PlayerActivity.this).load(data.trackImageLarge).placeholder(ImageUtils.getVectorDrawable(PlayerActivity.this, R.drawable.preload)).thumbnail(0.2f).into(new GlideDrawableImageViewTarget(art) {
                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> animation) {
                        if (imageDrawable == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            int cx = art.getWidth() / 2, cy = art.getHeight() / 2;

                            art.setImageDrawable(resource);
                            ViewAnimationUtils.createCircularReveal(art, cx, cy, 0, (float) Math.hypot(cx, cy)).start();
                        } else art.transition(resource);

                        final Bitmap bitmap = ImageUtils.drawableToBitmap(resource);
                        if (backgroundImage != null && imageDrawable == null) {
                            new Thread() {
                                @Override
                                public void run() {
                                    final Bitmap blurredBitmap = ImageUtils.blurBitmap(PlayerActivity.this, bitmap);
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (backgroundImage != null)
                                                backgroundImage.transition(blurredBitmap);
                                        }
                                    });
                                }
                            }.start();
                        }

                        if (imageDrawable == null) {
                            Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(Palette palette) {
                                    int color = palette.getDarkVibrantColor(Color.DKGRAY);
                                    if (imageColor == null)
                                        imageColor = ContextCompat.getColor(PlayerActivity.this, R.color.colorPrimary);

                                    ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), imageColor, color);
                                    animator.setDuration(250);
                                    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                        @Override
                                        public void onAnimationUpdate(ValueAnimator animation) {
                                            int value = (int) animation.getAnimatedValue();
                                            bg.setBackgroundColor(value);
                                        }
                                    });
                                    animator.start();

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                        setTaskDescription(new ActivityManager.TaskDescription(data.trackName, ImageUtils.drawableToBitmap(ContextCompat.getDrawable(PlayerActivity.this, R.mipmap.ic_launcher)), color));
                                }
                            });
                        }

                        imageDrawable = resource;
                        if (isLoading()) setLoading(false);
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        super.onLoadFailed(e, errorDrawable);
                        if (backgroundImage != null)
                            backgroundImage.transition(ContextCompat.getDrawable(PlayerActivity.this, R.drawable.image_gradient));
                        if (isLoading()) setLoading(false);
                    }
                });

                action = new Action<Boolean>() {
                    @NonNull
                    @Override
                    public String id() {
                        return "isTrackFav";
                    }

                    @Nullable
                    @Override
                    protected Boolean run() throws InterruptedException {
                        return pasta.isFavorite(trackList.get(curPosition));
                    }

                    @Override
                    protected void done(@Nullable Boolean result) {
                        if (result == null) {
                            pasta.onError(PlayerActivity.this, "favorite action");
                            return;
                        }

                        if (result) {
                            fav.setIcon(R.drawable.ic_fav);
                        } else {
                            fav.setIcon(R.drawable.ic_unfav);
                        }
                    }

                };

                title.setText(data.trackName);
                if (data.artists.size() > 0) subtitle.setText(data.artists.get(0).artistName);
                else subtitle.setText("");
                if (subtitle2 != null) subtitle2.setText(data.albumName);

                adapter.swapData(trackList, curPosition);

                lastUri = data.trackUri;
            }

            playButton.setImageDrawable(playing ? pause : play);

            if (!isClickable()) setClickable(true);

            if (seekBar.getProgress() != curTime && !playbarDragging) {
                ObjectAnimator.ofInt(seekBar, "progress", curTime).setDuration(PlayerService.UPDATE_INTERVAL).start();

                String current = String.valueOf(curTime);
                currentDuration.setText(StaticUtils.timeToString((Integer.parseInt(current) / 1000) / 60, (Integer.parseInt(current) / 1000) % 60));
            }

            if (seekBar.getMax() != maxTime) {
                seekBar.setMax(maxTime);
                String duration = String.valueOf(maxTime);
                finalDuration.setText(StaticUtils.timeToString((Integer.parseInt(duration) / 1000) / 60, (Integer.parseInt(duration) / 1000) % 60));
            }
        }
    }
}
