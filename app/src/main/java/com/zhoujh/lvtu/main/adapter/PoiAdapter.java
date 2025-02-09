package com.zhoujh.lvtu.main.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amap.api.services.core.PoiItemV2;
import com.zhoujh.lvtu.R;
import com.zhoujh.lvtu.main.AddTravelPlanActivity;

import java.util.List;

public class PoiAdapter extends RecyclerView.Adapter<PoiAdapter.PoiViewHolder>{
    private final List<PoiItemV2> poiItemV2List;
    private final Context context;
    private AddTravelPlanActivity.PoiItemClickListener listener;

    public PoiAdapter(List<PoiItemV2> poiItemV2List, Context context, AddTravelPlanActivity.PoiItemClickListener poiItemClickListener) {
        this.poiItemV2List = poiItemV2List;
        this.context = context;
        this.listener = poiItemClickListener;
    }

    @NonNull
    @Override
    public PoiAdapter.PoiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poi, parent, false);
        return new PoiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PoiAdapter.PoiViewHolder holder, int position) {
        PoiItemV2 poiItemV2 = poiItemV2List.get(position);
        holder.textView1.setText(poiItemV2.getTitle());
        holder.textView2.setText(poiItemV2.getSnippet());
        holder.linearLayout.setOnClickListener(v -> {
            listener.onPoiItemClick(poiItemV2);
        });
    }

    @Override
    public int getItemCount() {
        return poiItemV2List.size();
    }

    static class PoiViewHolder extends RecyclerView.ViewHolder{
        LinearLayout linearLayout;
        TextView textView1;
        TextView textView2;
        public PoiViewHolder(@NonNull View itemView) {
            super(itemView);
            linearLayout = itemView.findViewById(R.id.poi);
            textView1 = itemView.findViewById(R.id.text1);
            textView2 = itemView.findViewById(R.id.text2);
        }
    }
}
