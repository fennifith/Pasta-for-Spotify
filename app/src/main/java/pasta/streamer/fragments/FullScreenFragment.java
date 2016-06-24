package pasta.streamer.fragments;

import android.support.v4.app.Fragment;

public class FullScreenFragment extends Fragment {
    public interface DataListener {
        void onDataReady(String title, int statusColor, int windowColor);
    }

    private DataListener listener;

    public void setDataListener(DataListener listener) {
        this.listener = listener;
    }

    public void setData(String title, int statusColor, int windowColor) {
        if (listener != null) listener.onDataReady(title, statusColor, windowColor);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        listener = null;
    }
}
