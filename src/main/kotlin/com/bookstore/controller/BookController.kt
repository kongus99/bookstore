package com.bookstore.controller

import com.bookstore.repository.BookRepository
import com.bookstore.repository.mapping.BookDto
import com.bookstore.service.BookService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*


@RestController
@Transactional("myTransactional")
class BookController(val service: BookService, val repository: BookRepository) {

    @GetMapping("/bookstore/book")
    fun get(@RequestParam allParams: Map<String, String>): List<BookDto> {
        return repository.retrieve(allParams)
    }

    @PostMapping("/bookstore/book")
    fun post(@RequestBody book: BookDto): Int {
        return service.create(book)
    }

    @PutMapping("/bookstore/book/{id}")
    fun put(@PathVariable id: Int, @RequestBody book: BookDto) {
        return repository.update(id, book)
    }

    @DeleteMapping("/bookstore/book/{id}")
    fun delete(@PathVariable id: Int) {
        repository.delete(id)
    }
}
