package com.bookstore.repository

import com.bookstore.repository.mapping.Book
import com.bookstore.repository.mapping.BookDto
import com.bookstore.repository.mapping.BookDto.Companion.toDto
import com.bookstore.repository.mapping.Genre
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.selectAll
import org.springframework.stereotype.Repository


@Repository
class BookRepository {
    fun find(params: Map<String, String>): List<BookDto> {
        val booksQuery = Book.selectAll()

        params["id"]?.let { booksQuery.andWhere { Book.id eq it.toInt() } }
        params["title"]?.let { booksQuery.andWhere { Book.title like it } }
        params["genre"]?.let { booksQuery.andWhere { Book.genre eq Genre.fromString(it) } }
        params["width"]?.let { booksQuery.andWhere { Book.widthInCentimeters eq it.toByte() } }
        booksQuery.condition(params, "shelfId", Book.assignedShelf, String::toInt)
        booksQuery.condition(params, "index", Book.shelfIndex, String::toByte)

        return booksQuery.map { toDto(it) }
    }

    private fun <T> Query.condition(
        params: Map<String, String>,
        key: String,
        column: Column<T?>,
        value: (String) -> T
    ) {
        if (params.containsKey(key)) params[key]?.let { this.andWhere { column eq value(it) } }
            ?: this.andWhere { column.isNull() }
    }
}