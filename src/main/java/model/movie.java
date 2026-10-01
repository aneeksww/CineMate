package model;

public class movie {
    private int id;
    private String title;
    private String genre;
    private String description;

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
    public String hetTitle() { return title; }
    public String getGenre() { return genre; }
    public String getDesc() { return description; }
}
