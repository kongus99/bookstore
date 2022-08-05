package com.bookstore.repository

import com.bookstore.repository.mapping.ShelfDto
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest


@AutoConfigureEmbeddedDatabase(refresh = AFTER_EACH_TEST_METHOD)
@SpringBootTest
internal class BookRepositoryTest(@Autowired val shelfRepository: ShelfRepository) {

    @Test
    fun shouldNotBeAnyShelvesInDefaultBookstore() {
        transaction {
            val shelves = shelfRepository.retrieve(null).toTypedArray()
            assertArrayEquals(arrayOf(), shelves)
        }
    }

    @Test
    fun shouldAllowToAddMultipleShelves() {
        transaction {
            //given
            val shelf = ShelfDto(width = 1.5f)
            //when
            val id = shelfRepository.create(shelf)
            //then
            assertArrayEquals(arrayOf(shelf.copy(id = id)), shelfRepository.retrieve(id).toTypedArray())
        }

    }
}