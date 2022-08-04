package com.bookstore.repository

import com.bookstore.repository.mapping.Book
import com.bookstore.repository.mapping.Shelf
import com.bookstore.repository.mapping.ShelfDto
import org.jetbrains.exposed.sql.*
import org.springframework.stereotype.Repository

@Repository
class ShelfRepository {

    fun retrieve(id: Int?): List<ShelfDto> {
        val query = Shelf.selectAll()
        id?.let { query.andWhere { Shelf.id eq it } }
        return query.map { ShelfDto.toDto(it) }
    }

    fun create(shelf: ShelfDto): Int =
        Shelf.insertAndGetId {
            it[widthInMeters] = shelf.width
        }.value

    fun delete(id: Int) {
        Shelf.deleteWhere {
            Shelf.id.eq(id)
        }
    }

    fun update(id: Int, shelf: ShelfDto) {
        Shelf.update({ Shelf.id eq id }) {
            it[widthInMeters] = shelf.width
        }
    }

}