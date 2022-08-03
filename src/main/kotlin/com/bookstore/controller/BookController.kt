package com.bookstore.controller

import com.bookstore.repository.BookRepository
import com.bookstore.repository.mapping.BookDto
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*


@RestController
@Transactional
class BookController(val repository: BookRepository) {

    @GetMapping("/bookstore/book")
    fun get(@RequestParam allParams: Map<String, String>): List<BookDto> {
        return repository.find(allParams)
    }

    @PostMapping("/bookstore/book")
    fun post(@RequestBody book: BookDto): Int {
        return repository.add(book)
    }

    @PutMapping("/bookstore/book/{id}")
    fun put(@PathVariable id: Int, @RequestBody book: BookDto) {
        return repository.update(id, book)
    }
    @DeleteMapping("/bookstore/book/{id}")
    fun delete(@PathVariable id: Int) {
        repository.remove(id)
    }
}
