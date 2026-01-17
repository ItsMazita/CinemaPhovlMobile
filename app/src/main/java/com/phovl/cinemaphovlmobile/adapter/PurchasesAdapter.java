package com.phovl.cinemaphovlmobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.model.PurchaseItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // Título: nombre de la película (extraído del filename si aplica)
        String title = it.name != null ? extractMovieTitle(it.name) : "Ticket";
        holder.tvTitle.setText(title);

        // Subtítulo: asiento y función (intenta usar funcionId, si no parsea del nombre)
        String asiento = it.funcionId != null ? it.funcionId : extractSeatFromName(it.name);
        String funcion = extractFuncionIdFromName(it.name);
        String subtitle = "";
        if (asiento != null && !asiento.isEmpty()) subtitle += "Asiento " + asiento;
        if (funcion != null && !funcion.isEmpty()) {
            if (!subtitle.isEmpty()) subtitle += " • ";
            subtitle += "Función " + funcion;
        }
        if (subtitle.isEmpty()) subtitle = "Asiento N/A";
        holder.tvSubtitle.setText(subtitle);

        // Meta: fecha/hora y precio
        String fecha = it.dateMillis > 0
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(it.dateMillis))
                : "Fecha N/A";
        String precio = getPriceForItem(it);
        holder.tvMeta.setText(fecha + " • " + precio);

        // Click principal: abrir PDF
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(it);
        });

        // Accesibilidad: descripción breve
        holder.itemView.setContentDescription(title + ", " + subtitle + ", " + fecha);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle, tvMeta;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
        }
    }

    // --- Helpers para extraer datos del nombre de archivo ---

    private String extractMovieTitle(String filename) {
        if (filename == null) return "Ticket";
        String s = filename.replaceAll("(?i)\\.pdf$", "");
        // Quitar sufijos comunes: _asiento_..., timestamps, etc.
        s = s.replaceAll("(?i)_asiento_.*$", "");
        s = s.replaceAll("_\\d{8}_\\d{6}$", "");
        s = s.replaceAll("_+", " ").trim();
        return s.isEmpty() ? filename : s;
    }

    private String extractSeatFromName(String filename) {
        if (filename == null) return null;
        Pattern p = Pattern.compile("asiento[_-]?([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(filename);
        if (m.find()) return m.group(1);
        // alternativa: letra+numero (ej. D3)
        p = Pattern.compile("([A-Z]\\d{1,2})", Pattern.CASE_INSENSITIVE);
        m = p.matcher(filename);
        return m.find() ? m.group(1) : null;
    }

    private String extractFuncionIdFromName(String filename) {
        if (filename == null) return null;
        Pattern p = Pattern.compile("funcion[_-]?(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(filename);
        return m.find() ? m.group(1) : null;
    }

    private String getPriceForItem(PurchaseItem it) {
        // Si no tienes precio por ticket, devuelve un valor por defecto.
        // Puedes adaptar esta función para calcular precio real si lo tienes disponible.
        return "85.00 MXN";
    }
}
