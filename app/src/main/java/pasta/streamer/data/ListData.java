package pasta.streamer.data;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class ListData<T extends ListData.ViewHolder> {

    public abstract T getViewHolder(LayoutInflater inflater, ViewGroup parent);

    public abstract void bindView(T holder);

    public static abstract class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

}
