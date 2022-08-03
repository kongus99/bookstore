CREATE TABLE public.shelf
(
    id              serial PRIMARY KEY,
    width_in_meters REAL NOT NULL
);


CREATE TABLE public.book
(
    id                   serial PRIMARY KEY,
    isbn                 VARCHAR(13)  NOT NULL
        CONSTRAINT book_isbn_unique UNIQUE,
    title                VARCHAR(255) NOT NULL,
    genre                INTEGER      NOT NULL,
    width_in_centimeters SMALLINT     NOT NULL,
    shelf_id             INTEGER
        CONSTRAINT fk_book_shelf_id__id REFERENCES shelf ON UPDATE RESTRICT ON DELETE RESTRICT,
    shelf_index          SMALLINT,

    CONSTRAINT shelf_position UNIQUE (shelf_id, shelf_index)
);