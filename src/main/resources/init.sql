CREATE TABLE if NOT EXISTS users(
    tg_id INTEGER PRIMARY KEY,
    username TEXT
);

CREATE TABLE if NOT EXISTS genres(
    id INTEGER PRIMARY KEY,
    name TEXT UNIQUE
);

CREATE TABLE if NOT EXISTS movies(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT,
    source TEXT NOT NULL DEFAULT 'LOCAL',
    CHECK (source IN ('LOCAL', 'API')),
    external_id TEXT,
    api_provider TEXT
);

CREATE TABLE if NOT EXISTS movie_genres(
    movie_id  INTEGER NOT NULL,
    genre_id INTEGER NOT NULL,
    PRIMARY KEY(movie_id, genre_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id),
    FOREIGN KEY (genre_id) REFERENCES genres(id)
);

CREATE TABLE if NOT EXISTS reviews(
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tg_id INTEGER,
    movie_id INTEGER,
    rating INTEGER CHECK (rating BETWEEN 1 AND 10),
    comment TEXT,
    rate_date TEXT,
    FOREIGN KEY (tg_id) REFERENCES users(tg_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id)
);

CREATE TABLE if NOT EXISTS user_movies(
    tg_id INTEGER,
    movie_id INTEGER,
    PRIMARY KEY (tg_id, movie_id),
    FOREIGN KEY (tg_id) REFERENCES users(tg_id),
    FOREIGN KEY (movie_id) REFERENCES movies(id)
);

CREATE INDEX if NOT EXISTS idx_movie_title
ON movies(title);

INSERT or IGNORE INTO genres (name)
VALUES
    ('Драма'),
    ('Хоррор'),
    ('Триллер'),
    ('Комедия'),
    ('Детектив'),
    ('Фэнтези'),
    ('Фантастика');