package com.phovl.cinemaphovlmobile.adapter;

import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.phovl.cinemaphovlmobile.model.Asiento;

import java.util.ArrayList;
import java.util.List;

public class AsientoAdapter extends RecyclerView.Adapter<AsientoAdapter.ViewHolder> {

    private final List<Asiento> asientos;
    private final List<Asiento> seleccionados = new ArrayList<>();
    private final OnSeleccionChangeListener listener;
    private final int limite;

    public interface OnSeleccionChangeListener {
        void onChange(List<Asiento> seleccionados);
    }

    public AsientoAdapter(List<Asiento> asientos, OnSeleccionChangeListener listener, int limite) {
        this.asientos = asientos;
        this.listener = listener;
        this.limite = limite;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        Button btnAsiento;
        public ViewHolder(Button view) {
            super(view);
            btnAsiento = view;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Button btn = new Button(parent.getContext());
        btn.setLayoutParams(new ViewGroup.LayoutParams(80, 80)); // tamaño cuadrado
        btn.setTextSize(10f);
        return new ViewHolder(btn);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Asiento asiento = asientos.get(position);
        Button btn = holder.btnAsiento;
        btn.setText(asiento.getId());

        btn.setBackgroundColor(Color.GRAY);
        btn.setEnabled(true);

        btn.setOnClickListener(v -> {
            if (seleccionados.contains(asiento)) {
                seleccionados.remove(asiento);
                btn.setBackgroundColor(Color.GRAY);
            } else {
                if (seleccionados.size() < limite) {
                    seleccionados.add(asiento);
                    btn.setBackgroundColor(Color.YELLOW);
                }
            }
            listener.onChange(seleccionados);
        });
    }

    @Override
    public int getItemCount() {
        return asientos.size();
    }

    public List<Asiento> getSeleccionados() {
        return seleccionados;
    }
}
