package com.phovl.cinemaphovlmobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.phovl.cinemaphovlmobile.R;
import com.phovl.cinemaphovlmobile.model.Section;

import java.util.List;

public class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.SectionViewHolder> {

    private List<Section> sections;

    public SectionAdapter(List<Section> sections) {
        this.sections = sections;
    }

    @NonNull
    @Override
    public SectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_section, parent, false);
        return new SectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SectionViewHolder holder, int position) {
        Section section = sections.get(position);

        holder.txtTitle.setText(section.getTitle());

        holder.recyclerMovies.setLayoutManager(
                new LinearLayoutManager(holder.itemView.getContext(),
                        LinearLayoutManager.HORIZONTAL, false));

        holder.recyclerMovies.setAdapter(
                new PeliculasAdapter(section.getMovies()));
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {

        TextView txtTitle;
        RecyclerView recyclerMovies;

        public SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txt_section_title);
            recyclerMovies = itemView.findViewById(R.id.recycler_movies);
        }
    }
}
