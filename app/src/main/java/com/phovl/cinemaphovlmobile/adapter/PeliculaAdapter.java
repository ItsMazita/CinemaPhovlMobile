package com.phovl.cinemaphovlmobile.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.model.Funcion;
import com.phovl.cinemaphovlmobile.model.PeliculaConFunciones;

import java.util.List;

public class PeliculaAdapter extends RecyclerView.Adapter<PeliculaAdapter.ViewHolder> {

    public interface OnHorarioClickListener { void onHorarioClick(Funcion funcion); }

    private final List<PeliculaConFunciones> lista;
    private final OnHorarioClickListener listener;
    private Context context;

    public PeliculaAdapter(List<PeliculaConFunciones> lista, OnHorarioClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCartelera;
        TextView txtTitulo, txtClasificacionDuracion, txtSinopsis;
        LinearLayout horariosDoblados, horariosSubtitulados;
        Button btnTrailer;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCartelera = itemView.findViewById(R.id.imgCartelera);
            txtTitulo = itemView.findViewById(R.id.txtTitulo);
            txtClasificacionDuracion = itemView.findViewById(R.id.txtClasificacionDuracion);
            txtSinopsis = itemView.findViewById(R.id.txtSinopsis);
            horariosDoblados = itemView.findViewById(R.id.horariosDoblados);
            horariosSubtitulados = itemView.findViewById(R.id.horariosSubtitulados);
            btnTrailer = itemView.findViewById(R.id.btnTrailer);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_pelicula_horarios, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PeliculaConFunciones p = lista.get(position);

        holder.txtTitulo.setText(p.getTitulo());
        holder.txtClasificacionDuracion.setText(p.getClasificacion() + " • " + p.getDuracion());
        holder.txtSinopsis.setText(p.getSinopsis());

        // RequestOptions para placeholder, error y centerCrop
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground);

        // Manejo de URL nula o vacía
        String url = p.getCarteleraUrl();
        if (url == null || url.trim().isEmpty()) {
            Glide.with(context)
                    .load(R.drawable.ic_launcher_foreground)
                    .apply(options)
                    .into(holder.imgCartelera);
        } else {
            try {
                Glide.with(context)
                        .load(url)
                        .apply(options)
                        .into(holder.imgCartelera);
            } catch (Exception e) {
                Glide.with(context)
                        .load(R.drawable.ic_launcher_foreground)
                        .apply(options)
                        .into(holder.imgCartelera);
            }
        }

        holder.btnTrailer.setOnClickListener(v ->
                Toast.makeText(context, "Ver tráiler de " + p.getTitulo(), Toast.LENGTH_SHORT).show()
        );

        // --- DOBLADAS ---
        holder.horariosDoblados.removeAllViews();
        if (p.getFuncionesDobladas() != null) {
            for (Funcion f : p.getFuncionesDobladas()) {
                Button btn = crearBotonHorario(f);
                holder.horariosDoblados.addView(btn);
            }
        }

        // --- SUBTITULADAS ---
        holder.horariosSubtitulados.removeAllViews();
        if (p.getFuncionesSubtituladas() != null) {
            for (Funcion f : p.getFuncionesSubtituladas()) {
                Button btn = crearBotonHorario(f);
                holder.horariosSubtitulados.addView(btn);
            }
        }
    }

    /**
     * Crea un botón de horario con márgenes, padding y colores consistentes.
     * No cambia tu paleta: mantiene fondo amarillo y texto negro por defecto.
     */
    private Button crearBotonHorario(Funcion f) {
        Button btn = new Button(context);

        // Texto y apariencia básica
        btn.setText(f.getHora());
        btn.setAllCaps(false);
        btn.setTextColor(Color.BLACK);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);

        // Padding en dp -> px
        int padH = dpToPx(12);
        int padV = dpToPx(6);
        btn.setPadding(padH, padV, padH, padV);

        // Color de fondo (amarillo) usando tint para compatibilidad
        int amarillo = ContextCompat.getColor(context, R.color.colorAccent /* reemplaza si tienes otro amarillo */);
        // Si quieres mantener exactamente Color.YELLOW, descomenta la línea siguiente y comenta la de arriba:
        // int amarillo = Color.YELLOW;
        btn.setBackgroundTintList(ColorStateList.valueOf(amarillo));

        // LayoutParams con márgenes para que no queden pegados
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = dpToPx(6);
        lp.setMargins(margin, margin, margin, margin);
        btn.setLayoutParams(lp);

        // Tamaño mínimo para facilitar toque
        btn.setMinHeight(dpToPx(40));
        btn.setMinWidth(dpToPx(64));

        // Click
        btn.setOnClickListener(v -> {
            // Llamar al listener con la función seleccionada
            if (listener != null) listener.onHorarioClick(f);
        });

        return btn;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() { return lista.size(); }
}
