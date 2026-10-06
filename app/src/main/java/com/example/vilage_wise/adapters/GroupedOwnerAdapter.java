package com.example.vilage_wise.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vilage_wise.R;
import com.example.vilage_wise.models.OwnerAnimalCount;

import java.util.ArrayList;
import java.util.List;

public class GroupedOwnerAdapter extends RecyclerView.Adapter<GroupedOwnerAdapter.GroupedOwnerViewHolder> {

    public interface OnGroupedOwnerClickListener {
        void onOwnerClick(OwnerAnimalCount item);
    }

    private List<OwnerAnimalCount> countList = new ArrayList<>();
    private final OnGroupedOwnerClickListener listener;

    public GroupedOwnerAdapter(OnGroupedOwnerClickListener listener) {
        this.listener = listener;
    }

    public void setCountList(List<OwnerAnimalCount> list) {
        this.countList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupedOwnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_owner_animal_count, parent, false);
        return new GroupedOwnerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupedOwnerViewHolder holder, int position) {
        OwnerAnimalCount item = countList.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return countList.size();
    }

    static class GroupedOwnerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOwnerName;
        private final TextView tvVillageAndRecords;
        private final TextView tvAnimalCount;
        private final TextView tvAnimalTypeLabel;

        public GroupedOwnerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOwnerName = itemView.findViewById(R.id.tvOwnerName);
            tvVillageAndRecords = itemView.findViewById(R.id.tvVillageAndRecords);
            tvAnimalCount = itemView.findViewById(R.id.tvAnimalCount);
            tvAnimalTypeLabel = itemView.findViewById(R.id.tvAnimalTypeLabel);
        }

        public void bind(OwnerAnimalCount item, OnGroupedOwnerClickListener listener) {
            tvOwnerName.setText(item.getOwnerName());

            String vill = item.getVillageName() != null && !item.getVillageName().isEmpty() ? item.getVillageName() : "Village";
            String recs = item.getSurveyRecordsCount() == 1 ? "1 record" : item.getSurveyRecordsCount() + " records";
            tvVillageAndRecords.setText(vill + " • " + recs);

            tvAnimalCount.setText(String.valueOf(item.getTotalCount()));
            tvAnimalTypeLabel.setText(item.getAnimalType() != null && !item.getAnimalType().isEmpty() ? item.getAnimalType().toLowerCase() : "count");

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOwnerClick(item);
            });
        }
    }
}
