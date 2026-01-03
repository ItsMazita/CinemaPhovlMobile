package com.phovl.cinemaphovlmobile.model;

import java.util.List;

public class Section {

    private String title;
    private List<String> movies;

    public Section(String title, List<String> movies) {
        this.title = title;
        this.movies = movies;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getMovies() {
        return movies;
    }
}
