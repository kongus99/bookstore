package com.bookstore.controller

import com.bookstore.repository.mapping.BookDto
import com.bookstore.repository.mapping.Genre.*
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus.*

@AutoConfigureEmbeddedDatabase(refresh = AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = RANDOM_PORT)
internal class BookControllerTest(@Autowired val restTemplate: TestRestTemplate) {

    private val hobbit = BookDto(
        isbn = "978-1-56619-909-4", title = "Hobbit", author = "J.R.R Tolken", genre = FANTASY, width = 15
    )
    private val silmarillion = BookDto(
        isbn = "978-1-56619-909-5", title = "The Silmarillion", author = "J.R.R Tolken", genre = FANTASY, width = 20
    )

    @Test
    fun shouldNotBeAnyBooksInDefaultBookstore() {
        val books = restTemplate.getForObject("/bookstore/book", Array<BookDto>::class.java)
        assertArrayEquals(arrayOf<BookDto>(), books)
    }


    @Test
    fun shouldAllowToAddMultipleCopiesOfTheBook() {
        //given
        //when
        val response1 = restTemplate.postForEntity("/bookstore/book", hobbit, String::class.java)
        val response2 = restTemplate.postForEntity("/bookstore/book", hobbit, String::class.java)
        val books = restTemplate.getForObject("/bookstore/book", Array<BookDto>::class.java)
        //then
        assertEquals(OK, response1.statusCode)
        assertEquals(OK, response2.statusCode)
        assertArrayEquals(arrayOf(hobbit.copy(id = 1, copy = 1), hobbit.copy(id = 2, copy = 2)), books)
    }

    @Test
    fun shouldAllowToAddMultipleBooks() {
        //given
        //when
        val responses =
            (1..3).map { restTemplate.postForEntity("/bookstore/book", hobbit, String::class.java) }.plus(
                (1..2).map { restTemplate.postForEntity("/bookstore/book", silmarillion, String::class.java) })
        val books = restTemplate.getForObject("/bookstore/book", Array<BookDto>::class.java)
        //then
        responses.forEach { assertEquals(OK, it.statusCode) }
        assertEquals(
            arrayOf(hobbit, hobbit, hobbit, silmarillion, silmarillion)
                .mapIndexed { index, it -> it.copy(id = index + 1, copy = index % 3 + 1) }.toSet(), books.toSet()
        )
    }

}