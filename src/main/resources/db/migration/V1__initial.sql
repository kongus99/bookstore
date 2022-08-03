CREATE TABLE public.shelf
(
    id              serial PRIMARY KEY,
    width_in_meters REAL NOT NULL
);


CREATE TABLE public.book
(
    id                   serial PRIMARY KEY,
    isbn                 VARCHAR(26)  NOT NULL,
    title                VARCHAR(255) NOT NULL,
    author               VARCHAR(255) NOT NULL,
    genre                INTEGER      NOT NULL,
    width_in_centimeters SMALLINT     NOT NULL,
    shelf_id             INTEGER
        CONSTRAINT fk_book_shelf_id__id REFERENCES shelf ON UPDATE RESTRICT ON DELETE RESTRICT,
    copy                 serial,

    constraint isbn_copy unique (isbn, copy)
);