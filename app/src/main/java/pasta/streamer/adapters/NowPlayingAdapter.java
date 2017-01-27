package pasta.streamer.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.ButterKnife;
import pasta.streamer.PlayerService;
import pasta.streamer.R;
import pasta.streamer.data.TrackListData;
import pasta.streamer.utils.StaticUtils;

public class NowPlayingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Activity activity;
    private ArrayList<TrackListData> trackList;
    private int trackPos;

    public NowPlayingAdapter(final Activity activity, ArrayList<TrackListData> trackList, int curPosition) {
        this.activity = activity;
        this.trackList = trackList;
        this.trackPos = curPosition;
    }

    public void swapData(ArrayList<TrackListData> trackList, int curPosition) {
        this.trackList = trackList;
        this.trackPos = curPosition;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 || position == 2) return 1;
        else return 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (viewType == 1)
            return new HeaderViewHolder(inflater.inflate(R.layout.header_item, parent, false));
        else return new ViewHolder(inflater.inflate(R.layout.track_item, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (getItemViewType(position) == 1) {
            ((TextView) ((HeaderViewHolder) viewHolder).v).setText(position == 0 ? "Now Playing" : "Next Up");
            return;
        }

        final ViewHolder holder = (ViewHolder) viewHolder;
        TrackListData track = trackList.get(getAbsPosition(position));

        ButterKnife.findById(holder.v, R.id.menu).setVisibility(View.GONE);
        ButterKnife.findById(holder.v, R.id.image).setVisibility(View.GONE);

        ((TextView) ButterKnife.findById(holder.v, R.id.name)).setText(track.trackName);
        TextView extra = ButterKnife.findById(holder.v, R.id.extra);
        if (track.artists.size() > 0) extra.setText(track.artists.get(0).artistName);
        else extra.setText("");

        String duration = String.valueOf(track.trackDuration);
        ((TextView) ButterKnife.findById(holder.v, R.id.time)).setText(StaticUtils.timeToString((Integer.parseInt(duration) / 1000) / 60, (Integer.parseInt(duration) / 1000) % 60));

        holder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PlayerService.ACTION_MOVE_TRACK);
                intent.setClass(v.getContext(), PlayerService.class);
                intent.putExtra(PlayerService.ACTION_MOVE_TRACK_EXTRA_POS, getAbsPosition(holder.getAdapterPosition()));
                v.getContext().startService(intent);
            }
        });
    }

    public int getAbsPosition(int position) {
        position = position > 2 ? position - 2 : position - 1;
        if (position + trackPos < trackList.size()) return position + trackPos;
        else return Math.abs(trackList.size() - (trackPos + position));
    }

    @Override
    public int getItemCount() {
        if (trackList != null && trackList.size() > 0) {
            return trackList.size() + 2;
        } else return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public View v;

        public ViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public View v;

        public HeaderViewHolder(View v) {
            super(v);
            this.v = v;
        }
    }
}
