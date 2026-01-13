package com.phovl.cinemaphovlmobile.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
            // Cargar placeholder local si no hay URL
            Glide.with(context)
                    .load(R.drawable.ic_launcher_foreground)
                    .apply(options)
                    .into(holder.imgCartelera);
        } else {
            // Intentar cargar la URL remota
            try {
                Glide.with(context)
                        .load(url)
                        .apply(options)
                        .into(holder.imgCartelera);
            } catch (Exception e) {
                // En caso de error inesperado, cargar placeholder
                Glide.with(context)
                        .load(R.drawable.ic_launcher_foreground)
                        .apply(options)
                        .into(holder.imgCartelera);
            }
        }

        holder.btnTrailer.setOnClickListener(v ->
                Toast.makeText(context, "Ver tráiler de " + p.getTitulo(), Toast.LENGTH_SHORT).show()
        );

        holder.horariosDoblados.removeAllViews();
        for (Funcion f : p.getFuncionesDobladas()) {
            Button btn = crearBotonHorario(f);
            holder.horariosDoblados.addView(btn);
        }

        holder.horariosSubtitulados.removeAllViews();
        for (Funcion f : p.getFuncionesSubtituladas()) {
            Button btn = crearBotonHorario(f);
            holder.horariosSubtitulados.addView(btn);
        }
    }

    private Button crearBotonHorario(Funcion f) {
        Button btn = new Button(context);
        btn.setText(f.getHora());
        btn.setBackgroundColor(Color.YELLOW);
        btn.setTextColor(Color.BLACK);
        btn.setPadding(16, 8, 16, 8);
        btn.setOnClickListener(v -> listener.onHorarioClick(f));
        return btn;
    }

    @Override
    public int getItemCount() { return lista.size(); }
}
