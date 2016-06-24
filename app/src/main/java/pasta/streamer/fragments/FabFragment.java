package pasta.streamer.fragments;

import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.View;

public class FabFragment extends Fragment {
    public interface FabListener {
        void onDataReady(boolean visible, int iconRes, View.OnClickListener clickListener);

        Snackbar showSnackbar(String text, @Nullable String button, @Nullable View.OnClickListener clickListener);
    }

    private FabListener listener;

    public void setFabListener(FabListener listener) {
        this.listener = listener;
    }

    public void setFab(boolean visible, int iconRes, View.OnClickListener clickListener) {
        if (listener != null) listener.onDataReady(visible, iconRes, clickListener);
    }

    public Snackbar showSnackbar(String text, @Nullable String button, @Nullable View.OnClickListener clickListener) {
        if (listener != null) return listener.showSnackbar(text, button, clickListener);
        else return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        listener = null;
    }
}
