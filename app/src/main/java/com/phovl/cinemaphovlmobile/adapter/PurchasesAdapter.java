package com.phovl.cinemaphovlmobile.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.model.PurchaseItem;
import java.text.DateFormat;
import java.util.List;

public class PurchasesAdapter extends RecyclerView.Adapter<PurchasesAdapter.VH> {

    public interface Listener { void onOpen(PurchaseItem item); }

    private final List<PurchaseItem> items;
    private final Listener listener;
    private final DateFormat df;

    public PurchasesAdapter(List<PurchaseItem> items, Listener listener, Context ctx) {
        this.items = items;
        this.listener = listener;
        this.df = DateFormat.getDateTimeInstance();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PurchaseItem it = items.get(position);
        holder.tvFilename.setText(it.filename);
        String date = it.timestamp > 0 ? df.format(it.timestamp) : "-";
        holder.tvInfoSmall.setText("Función: " + (it.funcionId != null ? it.funcionId : "-") + "\n" + date);
        String size = it.sizeBytes > 0 ? (it.sizeBytes / 1024) + " KB" : "-";
        holder.tvMeta.setText(size + " • " + date);
        holder.itemView.setOnClickListener(v -> { if (listener != null) listener.onOpen(it); });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvFilename, tvMeta, tvInfoSmall, tvCardTitle;
        VH(@NonNull View itemView) {
            super(itemView);
            tvFilename = itemView.findViewById(R.id.tvFilename);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvInfoSmall = itemView.findViewById(R.id.tvInfoSmall);
            tvCardTitle = itemView.findViewById(R.id.tvCardTitle);
        }
    }
}
