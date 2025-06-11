package com.frank.ffmpeg.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.listener.OnSeekBarListener;
import java.util.ArrayList;
import java.util.List;

public class EqualizerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private OnSeekBarListener onSeekBarListener;
    private ArrayList<Pair<String, Integer>> equalizerList = new ArrayList<>();
    private ArrayList<SeekBar> seekBarList = new ArrayList<>();
    private int maxProgress;

    public EqualizerAdapter(Context context, OnSeekBarListener onSeekBarListener) {
        this.context = context;
        this.onSeekBarListener = onSeekBarListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_equalizer, null);
        return new EqualizerHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, @SuppressLint("RecyclerView") int i) {
        EqualizerHolder holder = (EqualizerHolder) viewHolder;
        if (equalizerList != null) {
            String centerFreq = (String) equalizerList.get(i).first;
            holder.txtFrequency.setText(centerFreq);
        }
        seekBarList.add(holder.barEqualizer);
        holder.barEqualizer.setMax(maxProgress);
        int currentProgress = (Integer) equalizerList.get(i).second;
        holder.barEqualizer.setProgress(currentProgress);

        holder.barEqualizer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (onSeekBarListener != null) {
                    onSeekBarListener.onProgress(i, seekBar.getProgress());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return equalizerList != null ? equalizerList.size() : 0;
    }

    private class EqualizerHolder extends RecyclerView.ViewHolder {
        TextView txtFrequency;
        SeekBar barEqualizer;

        EqualizerHolder(View itemView) {
            super(itemView);
            txtFrequency = itemView.findViewById(R.id.txt_frequency);
            barEqualizer = itemView.findViewById(R.id.bar_equalizer);
        }
    }

    public void setMaxProgress(int maxProgress) {
        if (maxProgress > 0) {
            this.maxProgress = maxProgress;
        }
    }

    public void setEqualizerList(ArrayList<Pair<String, Integer>> equalizerList) {
        if (equalizerList != null) {
            this.equalizerList = equalizerList;
            notifyDataSetChanged();
        }
    }

    public List<SeekBar> getSeekBarList() {
        return seekBarList;
    }
}