package com.phovl.cinemaphovlmobile.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.model.Asiento;
import java.util.ArrayList;
import java.util.List;

public class AsientoAdapter extends RecyclerView.Adapter<AsientoAdapter.VH> {

    private final List<Asiento> asientos;
    private final List<Asiento> seleccionados = new ArrayList<>();
    private final OnSeleccionChangeListener listener;
    private final int limite;
    private final int spanCount;
    private final int spacingPx;

    public interface OnSeleccionChangeListener {
        void onChange(List<Asiento> seleccionados);
    }

    public AsientoAdapter(List<Asiento> asientos, OnSeleccionChangeListener listener, int limite, int spanCount, int spacingPx) {
        // hacemos copia defensiva para poder modificar la lista internamente
        this.asientos = new ArrayList<>(asientos);
        this.listener = listener;
        this.limite = limite;
        this.spanCount = spanCount;
        this.spacingPx = spacingPx;
    }

    public static class VH extends RecyclerView.ViewHolder {
        CardView card;
        TextView txt;
        public VH(@NonNull View v) {
            super(v);
            card = v.findViewById(R.id.card_asiento);
            txt = v.findViewById(R.id.txt_asiento);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_asiento, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Asiento a = asientos.get(position);
        holder.txt.setText(a.getId());

        // Ajustar tamaño del CardView para que quepan spanCount columnas
        ViewGroup.LayoutParams lp = holder.card.getLayoutParams();
        int itemSize = calculateItemSize(holder.card.getContext(), spanCount, spacingPx);
        lp.width = itemSize;
        lp.height = itemSize;
        holder.card.setLayoutParams(lp);

        // Estado visual
        if (a.isOcupado()) {
            holder.card.setCardBackgroundColor(Color.parseColor("#FF4444")); // rojo
            holder.txt.setTextColor(Color.WHITE);
            holder.card.setClickable(false);
        } else if (seleccionados.contains(a)) {
            holder.card.setCardBackgroundColor(Color.parseColor("#FFEB3B")); // amarillo
            holder.txt.setTextColor(Color.BLACK);
            holder.card.setClickable(true);
        } else {
            holder.card.setCardBackgroundColor(Color.parseColor("#EEEEEE")); // gris claro
            holder.txt.setTextColor(Color.BLACK);
            holder.card.setClickable(true);
        }

        holder.card.setOnClickListener(v -> {
            if (a.isOcupado()) return;
            if (seleccionados.contains(a)) seleccionados.remove(a);
            else {
                if (seleccionados.size() < limite) seleccionados.add(a);
                else return;
            }
            notifyDataSetChanged();
            listener.onChange(new ArrayList<>(seleccionados));
        });
    }

    @Override
    public int getItemCount() {
        return asientos.size();
    }

    public List<Asiento> getSeleccionados() {
        return new ArrayList<>(seleccionados);
    }

    /**
     * Reemplaza toda la lista de asientos (útil si recargas desde servidor).
     */
    public void replaceAsientos(List<Asiento> nuevaLista) {
        asientos.clear();
        if (nuevaLista != null) asientos.addAll(nuevaLista);
        seleccionados.clear();
        notifyDataSetChanged();
        listener.onChange(new ArrayList<>(seleccionados));
    }

    /**
     * Marca como ocupados los asientos cuyos id estén en la lista 'ocupadosIds'.
     * También elimina de 'seleccionados' cualquier asiento que ahora esté ocupado.
     */
    public void markOcupados(List<Integer> ocupadosIds) {
        if (ocupadosIds == null) ocupadosIds = new ArrayList<>();
        boolean changed = false;

        for (Asiento a : asientos) {
            boolean shouldBeOcupado = ocupadosIds.contains(a.getIdAsientoDb());
            if (a.isOcupado() != shouldBeOcupado) {
                a.setOcupado(shouldBeOcupado);
                changed = true;
            }
        }

        List<Asiento> toRemove = new ArrayList<>();
        for (Asiento s : seleccionados) {
            if (ocupadosIds.contains(s.getIdAsientoDb())) toRemove.add(s);
        }
        if (!toRemove.isEmpty()) {
            seleccionados.removeAll(toRemove);
            changed = true;
        }

        if (changed) {
            notifyDataSetChanged();
            listener.onChange(new ArrayList<>(seleccionados));
        }
    }

    private int calculateItemSize(Context ctx, int spanCount, int spacingPx) {
        DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        int sidePadding = spacingPx * 2;
        int totalPx = dm.widthPixels - (spacingPx * (spanCount + 1)) - sidePadding;
        int size = totalPx / spanCount;
        int minPx = (int) (40 * dm.density + 0.5f);
        return Math.max(size, minPx);
    }
}
