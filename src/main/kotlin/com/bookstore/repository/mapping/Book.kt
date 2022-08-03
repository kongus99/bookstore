package com.bookstore.repository.mapping

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

object Book : IntIdTable() {
    val isbn = varchar("isbn", 13).uniqueIndex()
    val title = varchar("title", 255)
    val genre = enumeration("genre", Genre::class)
    val widthInCentimeters = byte("width_in_centimeters")
    val assignedShelf = (integer("shelf_id").references(Shelf.id)).nullable()
    val shelfIndex = byte("shelf_index").nullable()

    val shelfPosition = uniqueIndex("shelf_position", assignedShelf, shelfIndex)
}

data class BookDto(
    val id: Int?,
    val isbn: String,
    val title: String,
    val genre: Genre,
    val width: Byte,
    val shelf: ShelfPosition?
) {
    companion object {
        fun toDto(it: ResultRow): BookDto =
            BookDto(
                it[Book.id].value,
                it[Book.isbn],
                it[Book.title],
                it[Book.genre],
                it[Book.widthInCentimeters],
                it[Book.assignedShelf]?.let { shelf ->
                    it[Book.shelfIndex]?.let { index ->
                        ShelfPosition(shelf, index)
                    }
                }
            )
    }
}

data class ShelfPosition(val shelfId: Int, val index: Byte)

enum class Genre {
    ACTION, ROMANCE, SF, FANTASY, CRIMINAL, OTHER;

    companion object {
        fun fromString(s: String): Genre = valueOf(s.uppercase())
    }
}