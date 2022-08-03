package com.bookstore.repository

import com.bookstore.repository.mapping.Book
import com.bookstore.repository.mapping.BookDto
import com.bookstore.repository.mapping.BookDto.Companion.toDto
import com.bookstore.repository.mapping.Genre
import org.jetbrains.exposed.sql.*
import org.springframework.stereotype.Repository


@Repository
class BookRepository {
    fun find(params: Map<String, String>): List<BookDto> {
        val query = Book.selectAll()

        params["id"]?.let { query.andWhere { Book.id eq it.toInt() } }
        params["isbn"]?.let { query.andWhere { Book.isbn eq it } }
        params["title"]?.let { query.andWhere { Book.title like it } }
        params["author"]?.let { query.andWhere { Book.author like it } }
        params["genre"]?.let { query.andWhere { Book.genre eq Genre.fromString(it) } }
        params["width"]?.let { query.andWhere { Book.widthInCentimeters eq it.toByte() } }
        params["copy"]?.let { query.andWhere { Book.copy eq it.toInt() } }
        if (params.containsKey("shelfId")) params["shelfId"]?.let {
            query.andWhere { Book.assignedShelf eq it.toInt() }
        } ?: query.andWhere { Book.assignedShelf.isNull() }

        return query.map { toDto(it) }
    }

    fun add(book: BookDto): Int {
        val copies = find(mapOf("isbn" to book.isbn)).map { it.copy }
        val nextCopy = copies.maxByOrNull { it }?.let { max ->
            (1..(max + 1)).minus(copies.toSet()).minOfOrNull { it }
        } ?: 1
        return Book.insertAndGetId {
            it[isbn] = book.isbn
            it[title] = book.title
            it[author] = book.author
            it[genre] = book.genre
            it[widthInCentimeters] = book.width
            it[copy] = nextCopy
        }.value
    }

    fun remove(id: Int) {
        Book.deleteWhere {
            Book.id.eq(id)
        }
    }

    fun update(id: Int, book: BookDto) {
//        find(mapOf("id" to book.id))
    }
}