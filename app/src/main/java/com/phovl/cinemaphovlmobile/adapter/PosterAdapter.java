package com.phovl.cinemaphovlmobile.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.model.Pelicula;

import java.util.List;

public class PosterAdapter extends RecyclerView.Adapter<PosterAdapter.ViewHolder> {

    private final List<Pelicula> peliculas;
    private final Context context;

    public PosterAdapter(Context context, List<Pelicula> peliculas) {
        this.context = context;
        this.peliculas = peliculas;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgPoster);
        }
    }

    @NonNull
    @Override
    public PosterAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_poster, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PosterAdapter.ViewHolder holder, int position) {
        Pelicula p = peliculas.get(position);

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground);

        String url = p.getCarteleraUrl();
        if (url == null || url.trim().isEmpty()) {
            Glide.with(context).load(R.drawable.ic_launcher_foreground).apply(options).into(holder.imgPoster);
        } else {
            Glide.with(context).load(url).apply(options).into(holder.imgPoster);
        }
    }

    @Override
    public int getItemCount() {
        return peliculas.size();
    }
}
