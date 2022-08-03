package com.bookstore.controller

import com.bookstore.repository.BookRepository
import com.bookstore.repository.mapping.BookDto
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@Transactional
class BookController(val repository: BookRepository) {

    @GetMapping("/bookstore/book")
    fun get(@RequestParam allParams: Map<String, String>): List<BookDto> {
        return repository.find(allParams)
    }
}
