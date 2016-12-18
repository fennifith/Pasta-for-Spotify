package pasta.streamer.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pasta.streamer.data.ListData;

public class ListAdapter extends RecyclerView.Adapter<ListData.ViewHolder> {

    private List<ListData> list;
    private Comparator<ListData> comparator;

    public ListAdapter(List<ListData> list) {
        this.list = new ArrayList<>(list);
    }

    public void setList(List<ListData> list) {
        this.list = new ArrayList<>(list);
        if (comparator != null) Collections.sort(list, comparator);
        notifyDataSetChanged();
    }

    public List<ListData> getList() {
        return new ArrayList<>(list);
    }

    public void setComparator(Comparator<ListData> comparator) {
        this.comparator = comparator;
        Collections.sort(list, comparator);
        notifyDataSetChanged();
    }

    @Override
    public ListData.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return list.get(viewType).getViewHolder(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public void onBindViewHolder(ListData.ViewHolder holder, int position) {
        list.get(position).bindView(holder);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
