package pasta.streamer.activities;

import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;

import com.alexandrepiveteau.library.tutorial.TutorialActivity;
import com.alexandrepiveteau.library.tutorial.TutorialFragment;

import pasta.streamer.R;
import pasta.streamer.utils.StaticUtils;

public class IntroActivity extends TutorialActivity {
    private int[] BACKGROUND_COLORS = {Color.parseColor("#009688"), Color.parseColor("#0D47A1"), Color.parseColor("#F44336"), Color.parseColor("#303030"), Color.parseColor("#2196F3")};

    @Override
    public String getIgnoreText() {
        return "Skip";
    }

    @Override
    public int getCount() {
        return 5;
    }

    @Override
    public int getBackgroundColor(int position) {
        return BACKGROUND_COLORS[position];
    }

    @Override
    public int getNavigationBarColor(int position) {
        return StaticUtils.darkColor(BACKGROUND_COLORS[position]);
    }

    @Override
    public int getStatusBarColor(int position) {
        return StaticUtils.darkColor(BACKGROUND_COLORS[position]);
    }

    @Override
    public Fragment getTutorialFragmentFor(int position) {
        switch (position) {
            case 0:
                return new TutorialFragment.Builder()
                        .setTitle(getResources().getString(R.string.app_name))
                        .setDescription(getResources().getString(R.string.app_desc_long))
                        .setImageResourceBackground(R.mipmap.ic_launcher_web)
                        .build();
            case 1:
                return new TutorialFragment.Builder()
                        .setTitle(getResources().getString(R.string.new_releases))
                        .setDescription(getResources().getString(R.string.new_releases_msg))
                        .setImageResourceForeground(R.drawable.album_fg)
                        .setImageResource(R.drawable.album)
                        .build();
            case 2:
                return new TutorialFragment.Builder()
                        .setTitle(getResources().getString(R.string.featured))
                        .setDescription(getResources().getString(R.string.featured_msg))
                        .setImageResourceForeground(R.drawable.playlist_fg)
                        .setImageResource(R.drawable.playlist)
                        .setImageResourceBackground(R.drawable.playlist_bg)
                        .build();
            case 3:
                return new TutorialFragment.Builder()
                        .setTitle(getResources().getString(R.string.favorite_tutorial))
                        .setDescription(getResources().getString(R.string.favorite_tutorial_msg))
                        .setImageResource(R.drawable.favorite)
                        .setImageResourceBackground(R.drawable.favorite_bg)
                        .build();
            case 4:
                return new TutorialFragment.Builder()
                        .setTitle(getResources().getString(R.string.feedback))
                        .setDescription(getResources().getString(R.string.feedback_msg))
                        .setImageResourceForeground(R.drawable.rate)
                        .setImageResourceBackground(R.drawable.rate_bg)
                        .build();
            default:
                return new TutorialFragment.Builder().build();
        }
    }

    @Override
    public boolean isNavigationBarColored() {
        return true;
    }

    @Override
    public boolean isStatusBarColored() {
        return true;
    }

    @Override
    public ViewPager.PageTransformer getPageTransformer() {
        return TutorialFragment.getParallaxPageTransformer(2.5f);
    }
}
