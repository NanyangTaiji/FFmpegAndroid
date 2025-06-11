package com.frank.ffmpeg.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.recyclerview.widget.RecyclerView;
import com.frank.ffmpeg.R;
import com.frank.ffmpeg.listener.OnItemClickListener;
import java.util.List;

public class HorizontalAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<String> itemList;
    private OnItemClickListener onItemClickListener;
    private int lastClickPosition = 0;

    public HorizontalAdapter(List<String> itemList) {
        this.itemList = itemList;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select, parent, false);
        return new OkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        OkViewHolder okViewHolder = (OkViewHolder) holder;
        okViewHolder.btnSelect.setText(itemList.get(position));
        okViewHolder.btnSelect.setTextColor(Color.DKGRAY);

        if (onItemClickListener != null) {
            okViewHolder.btnSelect.setOnClickListener(v -> {
                notifyItemChanged(lastClickPosition);
                okViewHolder.btnSelect.setTextColor(Color.BLUE);
                onItemClickListener.onItemClick(okViewHolder.getAbsoluteAdapterPosition());
                lastClickPosition = okViewHolder.getAbsoluteAdapterPosition();
            });
        }
    }

    @Override
    public int getItemCount() {
        return itemList != null ? itemList.size() : 0;
    }

    private class OkViewHolder extends RecyclerView.ViewHolder {
        Button btnSelect;

        OkViewHolder(View itemView) {
            super(itemView);
            btnSelect = itemView.findViewById(R.id.btn_select);
        }
    }
}
