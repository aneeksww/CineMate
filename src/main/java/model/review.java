package model;

public class review {
    private int id;
    private final String movieTitle;
    private final int rating;
    private final String comment;
    private String rateDate;

    public review(int id, String movieTitle, int rating,
                  String comment, String rateDate) {
        this.id = id;
        this.movieTitle = movieTitle;
        this.rating = rating;
        this.comment = comment;
        this.rateDate = rateDate;
    }

    public review(String movieTitle, int rating, String comment) {
        this.movieTitle = movieTitle;
        this.rating = rating;
        this.comment = comment;
    }

    public int getId() { return id; }
    public String getMovieTitle() { return movieTitle; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getRateDate() { return rateDate; }
}

