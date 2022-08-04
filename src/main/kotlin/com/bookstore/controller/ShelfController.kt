package com.bookstore.controller

import com.bookstore.repository.ShelfRepository
import com.bookstore.repository.mapping.ShelfDto
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

@RestController
@Transactional
class ShelfController(val repository: ShelfRepository) {

    @GetMapping("/bookstore/shelf")
    fun get(@RequestParam id: Int?): List<ShelfDto> {
        return repository.retrieve(id)
    }

    @PostMapping("/bookstore/shelf")
    fun post(@RequestBody shelf: ShelfDto): Int {
        return repository.create(shelf)
    }

    @PutMapping("/bookstore/shelf/{id}")
    fun put(@PathVariable id: Int, @RequestBody shelf: ShelfDto) {
        return repository.update(id, shelf)
    }

    @DeleteMapping("/bookstore/shelf/{id}")
    fun delete(@PathVariable id: Int) {
        repository.delete(id)
    }

}