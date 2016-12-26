package pasta.streamer.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pasta.streamer.data.ListData;
import pasta.streamer.data.TrackListData;
import pasta.streamer.utils.PreferenceUtils;

public class ListAdapter extends RecyclerView.Adapter<ListData.ViewHolder> {

    private List<ListData> list;
    private Integer order;

    private List<TrackListData> tracks;

    public ListAdapter(List<ListData> list) {
        this.list = new ArrayList<>(list);
        tracks = new ArrayList<>();
        for (ListData listData : list) {
            if (listData instanceof TrackListData)
                tracks.add((TrackListData) listData);
        }
    }

    public void setList(List<? extends ListData> list) {
        this.list = new ArrayList<>(list);

        tracks.clear();
        for (ListData listData : list) {
            if (listData instanceof TrackListData)
                tracks.add((TrackListData) listData);
        }

        if (order != null) {
            sort(list, order);
            sort(tracks, order);
        }

        notifyDataSetChanged();
    }

    public List<ListData> getList() {
        return new ArrayList<>(list);
    }

    public void addListData(ListData listData) {
        list.add(listData);
        if (listData instanceof TrackListData) tracks.add((TrackListData) listData);

        if (order != null) {
            sort(list, order);
            sort(tracks, order);
        }

        notifyItemInserted(list.indexOf(listData));
    }

    public void setOrder(int order) {
        this.order = order;
        sort(list, order);
        sort(tracks, order);
        notifyDataSetChanged();
    }

    //TODO: re-organize, implement sorting for items other than tracks
    private static void sort(List<? extends ListData> list, int order) {
        switch (order) {
            case PreferenceUtils.ORDER_ADDED:
                return;
            case PreferenceUtils.ORDER_NAME:
                Collections.sort(list, new Comparator<ListData>() {
                    public int compare(ListData first, ListData second) {
                        return getName(first).compareTo(getName(second));
                    }
                });
                break;
            case PreferenceUtils.ORDER_ARTIST:
                Collections.sort(list, new Comparator<ListData>() {
                    public int compare(ListData first, ListData second) {
                        return getArtistName(first).compareTo(getArtistName(second));
                    }
                });
                break;
            case PreferenceUtils.ORDER_ALBUM:
                Collections.sort(list, new Comparator<ListData>() {
                    public int compare(ListData first, ListData second) {
                        return getAlbumName(first).compareTo(getAlbumName(second));
                    }
                });
                break;
            case PreferenceUtils.ORDER_LENGTH:
                Collections.sort(list, new Comparator<ListData>() {
                    public int compare(ListData first, ListData second) {
                        return getLength(first).compareTo(getLength(second));
                    }
                });
                break;
            case PreferenceUtils.ORDER_RANDOM:
                Collections.shuffle(list);
                break;
        }
    }

    private static String getName(ListData listData) {
        if (listData instanceof TrackListData)
            return ((TrackListData) listData).trackName;
        else return "";
    }

    private static String getArtistName(ListData listData) {
        if (listData instanceof TrackListData && ((TrackListData) listData).artists.size() > 0)
            return ((TrackListData) listData).artists.get(0).artistName;
        else return "";
    }

    private static String getAlbumName(ListData listData) {
        if (listData instanceof TrackListData)
            return ((TrackListData) listData).albumName;
        else return "";
    }

    private static String getLength(ListData listData) {
        if (listData instanceof TrackListData)
            return ((TrackListData) listData).trackDuration;
        else return "";
    }

    @Override
    public ListData.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return list.get(viewType).getViewHolder(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public void onBindViewHolder(ListData.ViewHolder holder, int position) {
        list.get(position).bindView(holder);

        if (list.get(position) instanceof TrackListData)
            ((TrackListData) list.get(position)).setTracks(tracks);
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
