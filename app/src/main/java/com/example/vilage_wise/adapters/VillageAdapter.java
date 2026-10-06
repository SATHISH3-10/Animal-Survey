package com.example.vilage_wise.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vilage_wise.R;
import com.example.vilage_wise.models.Village;

import java.util.ArrayList;
import java.util.List;

public class VillageAdapter extends RecyclerView.Adapter<VillageAdapter.VillageViewHolder> {

    public interface OnVillageClickListener {
        void onVillageClick(Village village);
        void onEditClick(Village village);
        void onDeleteClick(Village village);
    }

    private List<Village> villageList = new ArrayList<>();
    private final OnVillageClickListener listener;

    public VillageAdapter(OnVillageClickListener listener) {
        this.listener = listener;
    }

    public void setVillages(List<Village> list) {
        this.villageList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VillageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_village, parent, false);
        return new VillageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VillageViewHolder holder, int position) {
        Village village = villageList.get(position);
        holder.bind(village, listener);
    }

    @Override
    public int getItemCount() {
        return villageList.size();
    }

    static class VillageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvVillageName;
        private final TextView tvDistrictState;
        private final TextView tvVillageCode;
        private final TextView tvVillageStatsBadge;
        private final ImageButton btnEditVillage;
        private final ImageButton btnDeleteVillage;

        public VillageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVillageName = itemView.findViewById(R.id.tvVillageName);
            tvDistrictState = itemView.findViewById(R.id.tvDistrictState);
            tvVillageCode = itemView.findViewById(R.id.tvVillageCode);
            tvVillageStatsBadge = itemView.findViewById(R.id.tvVillageStatsBadge);
            btnEditVillage = itemView.findViewById(R.id.btnEditVillage);
            btnDeleteVillage = itemView.findViewById(R.id.btnDeleteVillage);
        }

        public void bind(Village village, OnVillageClickListener listener) {
            tvVillageName.setText(village.getName());
            
            String loc = village.getDistrict();
            if (village.getState() != null && !village.getState().trim().isEmpty()) {
                loc = loc + ", " + village.getState();
            }
            tvDistrictState.setText(loc);

            if (village.getCode() != null && !village.getCode().trim().isEmpty()) {
                tvVillageCode.setVisibility(View.VISIBLE);
                tvVillageCode.setText(village.getCode().trim());
            } else {
                tvVillageCode.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onVillageClick(village);
            });

            btnEditVillage.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(village);
            });

            btnDeleteVillage.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(village);
            });
        }
    }
}
