package com.example.vilage_wise.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vilage_wise.R;
import com.example.vilage_wise.models.AnimalSurvey;
import com.example.vilage_wise.utils.Constants;
import com.example.vilage_wise.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class SurveyAdapter extends RecyclerView.Adapter<SurveyAdapter.SurveyViewHolder> {

    public interface OnSurveyClickListener {
        void onSurveyClick(AnimalSurvey survey);
    }

    private List<AnimalSurvey> surveyList = new ArrayList<>();
    private final OnSurveyClickListener listener;

    public SurveyAdapter(OnSurveyClickListener listener) {
        this.listener = listener;
    }

    public void setSurveys(List<AnimalSurvey> list) {
        this.surveyList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SurveyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_survey, parent, false);
        return new SurveyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SurveyViewHolder holder, int position) {
        AnimalSurvey survey = surveyList.get(position);
        holder.bind(survey, listener);
    }

    @Override
    public int getItemCount() {
        return surveyList.size();
    }

    static class SurveyViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAnimalIcon;
        private final TextView tvAnimalType;
        private final TextView tvOwnerAndVillage;
        private final TextView tvQuantity;
        private final TextView tvCategory;
        private final TextView tvBreedAge;
        private final TextView tvSurveyDate;
        private final LinearLayout layoutQuantityBadge;

        public SurveyViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAnimalIcon = itemView.findViewById(R.id.ivAnimalIcon);
            tvAnimalType = itemView.findViewById(R.id.tvAnimalType);
            tvOwnerAndVillage = itemView.findViewById(R.id.tvOwnerAndVillage);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvBreedAge = itemView.findViewById(R.id.tvBreedAge);
            tvSurveyDate = itemView.findViewById(R.id.tvSurveyDate);
            layoutQuantityBadge = itemView.findViewById(R.id.layoutQuantityBadge);
        }

        public void bind(AnimalSurvey survey, OnSurveyClickListener listener) {
            tvAnimalType.setText(survey.getDisplayAnimalType());

            String subInfo = (survey.getOwnerName() != null ? survey.getOwnerName() : "Owner") +
                    " • " + (survey.getVillageName() != null ? survey.getVillageName() : "Village");
            tvOwnerAndVillage.setText(subInfo);

            tvQuantity.setText(String.valueOf(survey.getQuantity()));

            String cat = survey.getAnimalCategory();
            tvCategory.setText(cat != null && !cat.isEmpty() ? cat : "General");

            // Category styling
            if (Constants.CATEGORY_PETS.equalsIgnoreCase(cat)) {
                tvCategory.setBackgroundResource(R.drawable.bg_badge_pets);
                tvCategory.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.badge_pets_text));
            } else if (Constants.CATEGORY_POULTRY.equalsIgnoreCase(cat)) {
                tvCategory.setBackgroundResource(R.drawable.bg_badge_poultry);
                tvCategory.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.badge_poultry_text));
            } else if (Constants.CATEGORY_OTHER.equalsIgnoreCase(cat)) {
                tvCategory.setBackgroundResource(R.drawable.bg_badge_other);
                tvCategory.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.badge_other_text));
            } else {
                tvCategory.setBackgroundResource(R.drawable.bg_badge_livestock);
                tvCategory.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.badge_livestock_text));
            }

            // Breed / Age details
            StringBuilder details = new StringBuilder();
            if (survey.getBreed() != null && !survey.getBreed().trim().isEmpty()) {
                details.append("Breed: ").append(survey.getBreed().trim());
            }
            if (survey.getAge() != null && !survey.getAge().trim().isEmpty()) {
                if (details.length() > 0) details.append(" | ");
                details.append("Age: ").append(survey.getAge().trim());
            }
            if (details.length() == 0) {
                details.append("Standard Entry");
            }
            tvBreedAge.setText(details.toString());

            tvSurveyDate.setText(DateUtils.formatDate(survey.getSurveyDate()));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSurveyClick(survey);
            });
        }
    }
}
