package com.bookstore.service

import com.bookstore.repository.BookRepository
import com.bookstore.repository.ShelfRepository
import com.bookstore.repository.mapping.BookDto
import com.bookstore.repository.mapping.ShelfDto
import org.springframework.stereotype.Service

@Service
class BookService(val repository: BookRepository, val shelfRepository: ShelfRepository) {
    fun create(book: BookDto): Int {
        val copies = repository.retrieve(mapOf("isbn" to book.isbn)).map { it.copy }
        val nextCopy = copies.maxByOrNull { it }?.let { max ->
            (1..(max + 1)).toSet().minus(copies.toSet()).minOfOrNull { it }
        } ?: 1
        return repository.create(book.copy(copy = nextCopy))
    }

    fun addToShelf(bookId: Int, shelfId: Int) {
        shelfRepository.retrieve(shelfId).firstOrNull()?.let { shelf ->
            val existingBooks = repository.retrieve(mapOf("shelfId" to shelf.id.toString()))
            val addedBooks = repository.retrieve(mapOf("id" to bookId.toString()))
            if (bookCanBeAddedToShelf(existingBooks + addedBooks, shelf))
                reassignBook(bookId, shelfId)
        }
    }

    private fun bookCanBeAddedToShelf(books: List<BookDto>, shelf: ShelfDto): Boolean =
        books.sumOf { it.width.toInt() + 1 }.toFloat() / 100 < shelf.width

    fun removeFromShelf(bookId: Int) = reassignBook(bookId, null)

    private fun reassignBook(bookId: Int, shelfId: Int?) {
        repository.retrieve(mapOf("id" to bookId.toString())).firstOrNull()?.let { book ->
            repository.update(bookId, book.copy(shelfId = shelfId))
        }
    }
}