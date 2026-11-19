package model;

public class movie {
    private int id;
    private final String title;
    private final String genre;
    private final String description;

    public movie(int id, String title, String genre, String description) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.description = description;
    }

    public movie(String title, String genre, String description) {
        this.title = title;
        this.genre = genre;
        this.description = description;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public String getDesc() { return description; }
}
