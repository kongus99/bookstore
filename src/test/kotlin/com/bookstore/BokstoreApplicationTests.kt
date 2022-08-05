package com.bookstore

import com.bookstore.controller.BookController
import com.bookstore.controller.ShelfController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class PlayerApplicationTest(
    @Autowired val bookController: BookController,
    @Autowired val shelfController: ShelfController
) {
    @Test
    fun contextLoads() {
    }
}