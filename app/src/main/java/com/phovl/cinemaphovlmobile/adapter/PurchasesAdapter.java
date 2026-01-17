package com.phovl.cinemaphovlmobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.model.PurchaseItem;

import java.util.List;

public class PurchasesAdapter extends RecyclerView.Adapter<PurchasesAdapter.VH> {
    public interface OnClick { void onClick(PurchaseItem item); }

    private final List<PurchaseItem> items;
    private final OnClick listener;

    public PurchasesAdapter(List<PurchaseItem> items, OnClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pdf, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PurchaseItem it = items.get(position);
        holder.tvName.setText(it.name != null ? it.name : "PDF");
        holder.tvPath.setText(it.uri != null ? it.uri.toString() : "");
        holder.itemView.setOnClickListener(v -> listener.onClick(it));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvPath;
        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvPath = itemView.findViewById(R.id.tvPath);
        }
    }
}
