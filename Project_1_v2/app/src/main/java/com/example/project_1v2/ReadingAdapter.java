package com.example.project_1v2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class ReadingAdapter extends RecyclerView.Adapter<ReadingAdapter.ViewHolder> {

    private final List<ReadingItem> readings;

    public ReadingAdapter(List<ReadingItem> readings) {
        this.readings = readings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reading, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReadingItem item = readings.get(position);
        holder.tvTimestamp.setText(item.getTimestamp());
        holder.tvTemp.setText(String.format(Locale.getDefault(), "Temp: %.1f°C", item.getTemperature()));
        holder.tvHumidity.setText(String.format(Locale.getDefault(), "Hum: %.0f%%", item.getHumidity()));
        holder.tvLuminosity.setText(String.format(Locale.getDefault(), "Light: %.0f lx", item.getLuminosity()));
    }

    @Override
    public int getItemCount() {
        return readings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTimestamp;
        final TextView tvTemp;
        final TextView tvHumidity;
        final TextView tvLuminosity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvHumidity = itemView.findViewById(R.id.tvHumidity);
            tvLuminosity = itemView.findViewById(R.id.tvLuminosity);
        }
    }
}
