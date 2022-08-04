package com.bookstore.service

import com.bookstore.repository.BookRepository
import com.bookstore.repository.mapping.BookDto
import org.springframework.stereotype.Service

@Service
class BookService(val repository: BookRepository) {
    fun create(book: BookDto): Int {
        val copies = repository.retrieve(mapOf("isbn" to book.isbn)).map { it.copy }
        val nextCopy = copies.maxByOrNull { it }?.let { max ->
            (1..(max + 1)).minus(copies.toSet()).minOfOrNull { it }
        } ?: 1
        return repository.create(book.copy(copy = nextCopy))
    }
}