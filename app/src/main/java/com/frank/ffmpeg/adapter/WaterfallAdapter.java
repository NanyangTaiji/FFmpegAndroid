package com.frank.ffmpeg.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.listener.OnItemClickListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WaterfallAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int DEFAULT_MARGIN = 5;
    private List<String> itemList;
    private ArrayList<Integer> heightList;
    private OnItemClickListener onItemClickListener;

    public WaterfallAdapter() {}

    public WaterfallAdapter(List<String> itemList) {
        this.itemList = itemList;
        this.heightList = new ArrayList<>(itemList.size());
        Random random = new Random();
        for (int i = 0; i < itemList.size(); i++) {
            int height = random.nextInt(20) + DEFAULT_MARGIN;
            heightList.add(height);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_waterfall, parent, false);
        return new RandomHolder(view);
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        RandomHolder randomHolder = (RandomHolder) holder;
        ViewGroup.MarginLayoutParams layoutParams =
                (ViewGroup.MarginLayoutParams) randomHolder.txtComponent.getLayoutParams();

        int margin = DEFAULT_MARGIN;
        if (heightList.get(position) > DEFAULT_MARGIN) {
            margin = heightList.get(position);
        }
        layoutParams.topMargin = margin;
        layoutParams.bottomMargin = margin;
        randomHolder.txtComponent.setLayoutParams(layoutParams);

        randomHolder.txtComponent.setText(itemList.get(position));
        if (onItemClickListener != null) {
            randomHolder.txtComponent.setOnClickListener(v -> {
                onItemClickListener.onItemClick(randomHolder.getAbsoluteAdapterPosition());
            });
        }
    }

    private class RandomHolder extends RecyclerView.ViewHolder {
        TextView txtComponent;

        RandomHolder(View itemView) {
            super(itemView);
            txtComponent = itemView.findViewById(R.id.txt_component);
        }
    }
}
