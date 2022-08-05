package com.bookstore

import org.jetbrains.exposed.spring.SpringTransactionManager
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource


@SpringBootApplication
@EnableTransactionManagement
class BookstoreApplication {
    @Bean
    fun myTransactional(dataSource: DataSource): SpringTransactionManager {
        return SpringTransactionManager(dataSource, showSql = true)
    }
}

fun main(args: Array<String>) {
    runApplication<BookstoreApplication>(*args)
}