package com.example.vilage_wise.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vilage_wise.R;
import com.example.vilage_wise.models.Owner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnerAdapter extends RecyclerView.Adapter<OwnerAdapter.OwnerViewHolder> {

    public interface OnOwnerClickListener {
        void onOwnerClick(Owner owner);
        void onEditClick(Owner owner);
        void onDeleteClick(Owner owner);
    }

    private List<Owner> ownerList = new ArrayList<>();
    private Map<String, Integer> ownerAnimalCounts = new HashMap<>();
    private final OnOwnerClickListener listener;

    public OwnerAdapter(OnOwnerClickListener listener) {
        this.listener = listener;
    }

    public void setOwners(List<Owner> list) {
        this.ownerList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOwnerAnimalCounts(Map<String, Integer> counts) {
        this.ownerAnimalCounts = counts != null ? counts : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OwnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_owner, parent, false);
        return new OwnerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OwnerViewHolder holder, int position) {
        Owner owner = ownerList.get(position);
        int animalCount = ownerAnimalCounts.getOrDefault(owner.getId(), 0);
        holder.bind(owner, animalCount, listener);
    }

    @Override
    public int getItemCount() {
        return ownerList.size();
    }

    static class OwnerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOwnerName;
        private final TextView tvVillageName;
        private final TextView tvAnimalCountBadge;
        private final TextView tvContactInfo;
        private final ImageButton btnEditOwner;
        private final ImageButton btnDeleteOwner;

        public OwnerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOwnerName = itemView.findViewById(R.id.tvOwnerName);
            tvVillageName = itemView.findViewById(R.id.tvVillageName);
            tvAnimalCountBadge = itemView.findViewById(R.id.tvAnimalCountBadge);
            tvContactInfo = itemView.findViewById(R.id.tvContactInfo);
            btnEditOwner = itemView.findViewById(R.id.btnEditOwner);
            btnDeleteOwner = itemView.findViewById(R.id.btnDeleteOwner);
        }

        public void bind(Owner owner, int animalCount, OnOwnerClickListener listener) {
            tvOwnerName.setText(owner.getName());

            String villText = "Village: " + (owner.getVillageName() != null && !owner.getVillageName().isEmpty() ? owner.getVillageName() : "Assigned");
            tvVillageName.setText(villText);

            tvAnimalCountBadge.setText(animalCount + " Animals");

            if (owner.getContactNumber() != null && !owner.getContactNumber().trim().isEmpty()) {
                tvContactInfo.setVisibility(View.VISIBLE);
                tvContactInfo.setText("Phone: " + owner.getContactNumber().trim());
            } else {
                tvContactInfo.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOwnerClick(owner);
            });

            btnEditOwner.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(owner);
            });

            btnDeleteOwner.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(owner);
            });
        }
    }
}
