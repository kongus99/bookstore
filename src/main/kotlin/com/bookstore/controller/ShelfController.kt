package com.bookstore.controller

import com.bookstore.repository.BookRepository
import com.bookstore.repository.ShelfRepository
import com.bookstore.repository.mapping.BookDto
import com.bookstore.repository.mapping.ShelfDto
import com.bookstore.service.BookService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@Transactional
class ShelfController(
    val shelfRepository: ShelfRepository,
    val bookRepository: BookRepository,
    val bookService: BookService
) {

    @GetMapping("/bookstore/shelf")
    fun get(@RequestParam id: Int?): List<ShelfDto> {
        return shelfRepository.retrieve(id)
    }

    @PostMapping("/bookstore/shelf")
    fun post(@RequestBody shelf: ShelfDto): Int {
        return shelfRepository.create(shelf)
    }

    @DeleteMapping("/bookstore/shelf/{id}")
    fun delete(@PathVariable id: Int) {
        shelfRepository.delete(id)
    }

    @GetMapping("/bookstore/shelf/{shelfId}/book")
    fun getBooksOnShelf(@PathVariable shelfId: Int): List<BookDto> {
//        val retrieve = bookRepository.retrieve(mapOf("shelfId" to shelfId.toString()))
        return shelfRepository.retrieveBooks(shelfId)
    }

    @PutMapping("/bookstore/shelf/{shelfId}/book/{bookId}")
    fun addBookToShelf(@PathVariable shelfId: Int, @PathVariable bookId: Int) {
        return bookService.addToShelf(bookId, shelfId)
    }

    @DeleteMapping("/bookstore/shelf/{shelfId}/book/{bookId}")
    fun removeBookFromShelf(@PathVariable shelfId: Int, @PathVariable bookId: Int) {
        return bookService.removeFromShelf(bookId)
    }

}