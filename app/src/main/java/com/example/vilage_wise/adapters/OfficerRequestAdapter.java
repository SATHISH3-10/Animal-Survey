package com.example.vilage_wise.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vilage_wise.R;
import com.example.vilage_wise.models.User;
import com.example.vilage_wise.utils.Constants;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class OfficerRequestAdapter extends RecyclerView.Adapter<OfficerRequestAdapter.OfficerViewHolder> {

    public interface OnOfficerActionListener {
        void onApprove(User user);
        void onReject(User user);
    }

    private List<User> officerList = new ArrayList<>();
    private final OnOfficerActionListener listener;

    public OfficerRequestAdapter(OnOfficerActionListener listener) {
        this.listener = listener;
    }

    public void setOfficers(List<User> list) {
        this.officerList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OfficerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_officer_request, parent, false);
        return new OfficerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfficerViewHolder holder, int position) {
        User user = officerList.get(position);
        holder.bind(user, listener);
    }

    @Override
    public int getItemCount() {
        return officerList.size();
    }

    static class OfficerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOfficerName;
        private final TextView tvBadgeId;
        private final TextView tvStatusBadge;
        private final TextView tvOfficerEmail;
        private final TextView tvOfficerPhone;
        private final TextView tvAssignedVillage;
        private final LinearLayout layoutActionButtons;
        private final MaterialButton btnApprove;
        private final MaterialButton btnReject;

        public OfficerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOfficerName = itemView.findViewById(R.id.tvOfficerName);
            tvBadgeId = itemView.findViewById(R.id.tvBadgeId);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvOfficerEmail = itemView.findViewById(R.id.tvOfficerEmail);
            tvOfficerPhone = itemView.findViewById(R.id.tvOfficerPhone);
            tvAssignedVillage = itemView.findViewById(R.id.tvAssignedVillage);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }

        public void bind(User user, OnOfficerActionListener listener) {
            Context context = itemView.getContext();

            tvOfficerName.setText(user.getFullName().isEmpty() ? "Unnamed Officer" : user.getFullName());
            tvBadgeId.setText(user.getOfficerBadgeId().isEmpty() ? "No Badge Assigned" : "Badge: " + user.getOfficerBadgeId());
            tvOfficerEmail.setText(user.getEmail());
            tvOfficerPhone.setText(user.getPhone().isEmpty() ? "No contact number" : user.getPhone());
            tvAssignedVillage.setText(user.getAssignedVillage().isEmpty() ? "Assigned: General Region" : "Assigned: " + user.getAssignedVillage());

            String status = user.getStatus();
            tvStatusBadge.setText(status.toUpperCase());

            if (Constants.STATUS_APPROVED.equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_livestock);
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.success));
                layoutActionButtons.setVisibility(View.GONE);
            } else if (Constants.STATUS_REJECTED.equalsIgnoreCase(status)) {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_other);
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.error));
                layoutActionButtons.setVisibility(View.VISIBLE);
                btnApprove.setText("Re-Approve");
                btnReject.setVisibility(View.GONE);
            } else {
                // PENDING
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_poultry);
                tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.secondary_dark));
                layoutActionButtons.setVisibility(View.VISIBLE);
                btnApprove.setText("Approve");
                btnReject.setVisibility(View.VISIBLE);
            }

            btnApprove.setOnClickListener(v -> {
                if (listener != null) listener.onApprove(user);
            });

            btnReject.setOnClickListener(v -> {
                if (listener != null) listener.onReject(user);
            });
        }
    }
}
