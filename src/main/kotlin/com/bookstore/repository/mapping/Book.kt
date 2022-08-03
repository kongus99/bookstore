package com.bookstore.repository.mapping

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

object Book : IntIdTable() {
    val isbn = varchar("isbn", 26)
    val title = varchar("title", 255)
    val author = varchar("author", 255)
    val genre = enumeration("genre", Genre::class)
    val widthInCentimeters = byte("width_in_centimeters")
    val assignedShelf = (integer("shelf_id").references(Shelf.id)).nullable()
    val copy = integer("copy").autoIncrement()

    val isbnCopy = uniqueIndex("isbn_copy", isbn, copy)
}

data class BookDto(
    val id: Int?,
    val isbn: String,
    val title: String,
    val author: String,
    val genre: Genre,
    val width: Byte,
    val shelfId: Int?,
    val copy: Int
) {
    companion object {
        fun toDto(it: ResultRow): BookDto =
            BookDto(
                it[Book.id].value,
                it[Book.isbn],
                it[Book.title],
                it[Book.author],
                it[Book.genre],
                it[Book.widthInCentimeters],
                it[Book.assignedShelf],
                it[Book.copy]
            )
    }
}
enum class Genre {
    ACTION, ROMANCE, SF, FANTASY, CRIMINAL, OTHER;

    companion object {
        fun fromString(s: String): Genre = valueOf(s.uppercase())
    }
}