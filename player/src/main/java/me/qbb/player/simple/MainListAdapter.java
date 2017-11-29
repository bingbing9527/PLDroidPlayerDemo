package me.qbb.player.simple;

import android.app.Activity;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import me.qbb.player.R;
import me.qbb.player.player.TCPlayer;

/**
 * 创建时间 2017/11/29 16:48
 *
 * @author Qian Bing Bing
 *         类说明
 */

public class MainListAdapter extends RecyclerView.Adapter {


    private Activity mActivity;
    private final LayoutInflater inflater;


    public MainListAdapter(Activity mActivity) {
        this.mActivity = mActivity;
        inflater = LayoutInflater.from(mActivity);
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            case 0:
                view = inflater.inflate(R.layout.item_know_details_video, parent, false);
                viewHolder = new VideoViewHolder(view);
                break;
            default:
                TextView textView = new TextView(mActivity);
                textView.setText("item" + viewType);
                textView.setTextColor(Color.RED);
                viewHolder = new NormalViewHolder(textView);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int itemViewType = getItemViewType(position);
        switch (itemViewType) {
            case 0:
                VideoViewHolder viewHolder = (VideoViewHolder) holder;
                break;
            default:
                break;
        }
    }

    @Override
    public int getItemCount() {
        return 100;
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {

        public final FrameLayout adapter_player_container;


        public VideoViewHolder(View itemView) {
            super(itemView);
            adapter_player_container = (FrameLayout) itemView.findViewById(R.id.adapter_player_container);
        }
    }

    static class NormalViewHolder extends RecyclerView.ViewHolder {

        public NormalViewHolder(View itemView) {
            super(itemView);
        }
    }

    private OnPlayClickListener mOnPlayClickListener;

    public void setPlayClickListener(OnPlayClickListener mOnPlayClickListener) {
        this.mOnPlayClickListener = mOnPlayClickListener;
    }

    public interface OnPlayClickListener {
        void onPlayclick(int position, TCPlayer view_controller);
    }

}
