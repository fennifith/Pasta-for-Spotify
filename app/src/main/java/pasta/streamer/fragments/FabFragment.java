package pasta.streamer.fragments;

import android.support.v4.app.Fragment;
import android.view.View;

public class FabFragment extends Fragment {
    public interface FabListener {
        void onDataReady(boolean visible, int iconRes, View.OnClickListener clickListener);
    }

    private FabListener listener;

    public void setFabListener(FabListener listener) {
        this.listener = listener;
    }

    public void setFab(boolean visible, int iconRes, View.OnClickListener clickListener) {
        if (listener != null) listener.onDataReady(visible, iconRes, clickListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        listener = null;
    }
}
