# Introduction

## Preface

*Hibernate* is currently the most popular ORM framework for JVM based system. Still, there are a number
of alternatives available, i.e. *JOOQ* for Java, *Exposed* or *Ktorm* for Kotlin. In this article I will take
a look at one of these - *Exposed*. *Exposed* is a JetBrains developed alternative to *Hibernate*. It provides support
for various database dialects:

- H2
- MySQL and MariaDB
- Oracle
- PostgresSQL
- SQL Server
- SQLite

It offers two methods of DB access - via custom DSL and via DAO objects that can be defined over the DSL layer.
It also offers some Spring support for datasource and transactions in separate plugins. In this article I will show how
to
set up, write and test a typical REST based service using this framework.

## "Bookstore" service

Let's create a simple bookstore REST app service. These are some sample basic requirements:

- it allows to create and persist books
- each book consists of various data fields - ISBN, title, author, genre, width
- the genre is one of Action, Romance, SF, Fantasy, Criminal, Other
- the width specifies the width of the book in centimeters
- the bookstore can add any number of persistable shelves
- each shelf have a specified width
- any book that was created can be assigned/unassigned to an existing shelf
- the width of the books cannot exceed the assigned shelf width
- multiple copies of the same book can be added to the store
- we want to track/update/delete number of copies of specific book edition for future reference

# Setup

To start create a new Gradle or Maven Spring Boot project.
You can also use [Spring IO](https://start.spring.io) to quickly set up the Spring project.
I will be showing Gradle setup, but there should be no problem with Maven setup as well. Next, add Spring
dependencies:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-web")
testImplementation("org.springframework.boot:spring-boot-starter-test")
```

The first one is used to process the config and add the REST capabilities, the second one for REST testing purposes.
Next we
have:

```kotlin
implementation("org.flywaydb:flyway-core")
```

This one will be used for setting up database migrations in production and for testing. Finally, we get *Exposed*
dependencies:

```kotlin
implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
```

This packed is all-in-one dependency for *Exposed* - it adds JDBC, DSL and DAO from the framework, as well as Spring
transaction and datasource layer. You can find additional packages with
support for additional features - Java DateTime, Kotlin DateTime, Money, etc. - in the
[maven repository](https://mvnrepository.com/artifact/org.jetbrains.exposed). Finally, we add:

```kotlin
runtimeOnly("org.postgresql:postgresql")
testImplementation("io.zonky.test:embedded-database-spring-test:2.1.1")
```

These provide the required Postgresql driver and the embedded database support for integration testing.

Finally, let's take a look at the Application main class:

```kotlin
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
```

Now, this looks like a typical simple Spring Boot starter. The only exception is **SpringTransactionManager** which
provides the transaction management for Spring, courtesy of **exposed-spring-boot-starter**. The package provides
the transaction management bean out of the box, but if you want to modify its configuration (ie. add SQL logging), you
need to provide your own bean. Ok, but where is the datasource bean? Let's take a look at the application configuration:

```yaml
server:
  port: 9997

spring:
  application:
    name: bookstore
  datasource:
    url: jdbc:postgresql://localhost:5432/bookstore
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
flyway:
  enabled: true
```

Here we've got basic server and flyway configuration, as well as the datasource itself. Again,
**exposed-spring-boot-starter** allows us to define the datasource in the configuration and creates the necessary bean
ready for dependency injection.

# ORM(DTO) mapping

There are two ways to design a service:

- bottom-up - we start by designing the database and go up defining repositories, services and finally controllers
- top-down - starting from REST layer and moving down towards the database

Now, the second approach is what I actually used, since it was easier to design REST methods and add functionality
later. I will however take the first approach to present the app, since we are more interested in persistence layer. So,
let's start with SQL.

## SQL setup

```postgresql
CREATE TABLE public.shelf
(
    id              serial PRIMARY KEY,
    width_in_meters REAL NOT NULL
);


CREATE TABLE public.book
(
    id                   serial PRIMARY KEY,
    isbn                 VARCHAR(26)  NOT NULL,
    title                VARCHAR(255) NOT NULL,
    author               VARCHAR(255) NOT NULL,
    genre                INTEGER      NOT NULL,
    width_in_centimeters SMALLINT     NOT NULL,
    shelf_id             INTEGER
        CONSTRAINT fk_book_shelf_id__id REFERENCES shelf ON UPDATE RESTRICT ON DELETE RESTRICT,
    copy                 INT          NOT NULL,

    constraint isbn_copy unique (isbn, copy)
);
```

Now, there is nothing unusual here, but there are two things of note. First - **isbn_copy** constraint. It
basically says that when we add a new copy of the book we need to assign it new unique number to make it distinct from
the previous copies. Second, the reference on the shelf is nullable, which means that we can have books that are on a
virtual pile, not being assigned to any shelf. Only later we assign them to a proper bookshelf. Let's see then how
*Exposed* maps these tables to proper objects.

## Tables and DTOs

**Shelf** is the simpler of the two, so let's see what is required for the basic mapping.

```kotlin
object Shelf : IntIdTable() {
    val widthInMeters = float("width_in_meters")
}

data class ShelfDto(val id: Int? = null, val width: Float) {
    companion object {
        fun toDto(it: ResultRow): ShelfDto =
            ShelfDto(
                it[Shelf.id].value,
                it[Shelf.widthInMeters]
            )
    }
}
```

First, you need to define the table mapping itself, as a static object. It needs to extend **Table** class and define a
primary key. Usually it is easier to use one of the classes that extend the **Table** and provide predefined
auto-incremented **id** column - in this case it is **IntIdTable**. We also should define all the columns with proper
types. The columns in *Exposed* are not null and not unique by default.

Second, you need to define the actual DTO. **Shelf** only represents the table and should not be passed around without
mapping it to something serialization friendly. Since we are using Kotlin, it is easy - a data class suffices. Still, we
should add some means to map it out from the **ResultRow** to **ShelfDto**. Easiest way is to provide companion object
with transformation function.

Next, let's take a look at a more elaborate **Book** class.

```kotlin
object Book : IntIdTable() {
    val isbn = varchar("isbn", 26)
    val title = varchar("title", 255)
    val author = varchar("author", 255)
    val genre = enumeration("genre", Genre::class)
    val widthInCentimeters = byte("width_in_centimeters")
    val assignedShelf = (integer("shelf_id").references(Shelf.id)).nullable()
    val copy = integer("copy")

    private val isbnCopy = uniqueIndex("isbn_copy", isbn, copy)
}

data class BookDto(
    val id: Int? = null,
    val isbn: String,
    val title: String,
    val author: String,
    val genre: Genre,
    val width: Byte,
    val shelfId: Int? = null,
    val copy: Int = 0
) {
    companion object {
        fun toDto(it: ResultRow): BookDto =
            BookDto(
                it[Book.id].value,
                it[Book.isbn],
                it[Book.title],
                it[Book.author],
                it[Book.genre],
                it[Book.widthInCentimeters],
                it[Book.assignedShelf],
                it[Book.copy]
            )
    }
}
enum class Genre {
    ACTION, ROMANCE, SF, FANTASY, CRIMINAL, OTHER;

    companion object {
        fun fromString(s: String): Genre = valueOf(s.uppercase())
    }
}
```

The book table contains a few more complex mappings. The **enumeration** column type allows us to map out the **Genre**
enum onto integer column in the database. *Exposed* provides most of the common ORM mapping columns, but some less
standardized ones might require manual implementation. JSON is one such column type - since it is not available in every
RDBMS. Please refer to [this issue](https://github.com/JetBrains/Exposed/issues/127) for more information.

Next we have the constraint definitions. The foreign key is self-explanatory and the **isbnCopy** is made private since
the **uniqueIndex** for multiple columns does not return any useful reference.

Finally, the DTO itself is pretty obvious as well. There are some defaults provided to make top-down mapping slightly
easier, but apart from that there is nothing noteworthy here.

## Repositories

Let's go through all CRUD methods and showcase them for both **Shelf** and **Book**.

### CREATE

```kotlin
fun create(shelf: ShelfDto): Int =
    Shelf.insertAndGetId {
        it[widthInMeters] = shelf.width
    }.value

fun create(book: BookDto): Int {
    return Book.insertAndGetId {
        it[isbn] = book.isbn
        it[title] = book.title
        it[author] = book.author
        it[genre] = book.genre
        it[widthInCentimeters] = book.width
        it[copy] = book.copy
        it[assignedShelf] = book.shelfId
    }.value
}
```

In order to create a new entry in the database we need to map the DTO onto the table rows. This can be done by calling
**insert** or **insertAndGetId**. The second one allows us to create and retrieve a corresponding id. You can also
use **batchInsert** for optimization purposes. This is what an example resulting SQL for **Shelf** will look like:

```postgresql
INSERT INTO shelf (width_in_meters)
VALUES (1.5)
```

### RETRIEVE

```kotlin
fun retrieve(id: Int?): List<ShelfDto> {
    val query = Shelf.selectAll()
    id?.let { query.andWhere { Shelf.id eq it } }
    return query.map { ShelfDto.toDto(it) }
}

fun retrieve(params: Map<String, String>): List<BookDto> {
    val query = Book.selectAll()
    params["id"]?.let { query.andWhere { Book.id eq it.toInt() } }
    params["isbn"]?.let { query.andWhere { Book.isbn eq it } }
    params["title"]?.let { query.andWhere { Book.title like it } }
    params["author"]?.let { query.andWhere { Book.author like it } }
    params["genre"]?.let { query.andWhere { Book.genre eq Genre.fromString(it) } }
    params["width"]?.let { query.andWhere { Book.widthInCentimeters eq it.toByte() } }
    params["copy"]?.let { query.andWhere { Book.copy eq it.toInt() } }
    if (params.containsKey("shelfId")) params["shelfId"]?.let {
        query.andWhere { Book.assignedShelf eq it.toInt() }
    } ?: query.andWhere { Book.assignedShelf.isNull() }

    return query.map { toDto(it) }
}
```

The way queries work in *Exposed* is the same as how **Stream** works in Java. If you are familiar with JOOQ, it is
basically analogous to how it works there. We start with **selectAll** on the target table, and then we go through
various stages of SQL query, in this case just WHERE clause, passing through building objects and end it with a terminal
operation - in this case **map**. The conditions use the previously defined tables and their columns, analogous to
**CriteriaQuery** interface in *Hibernate*. With Kotlin, we can add various conditional statements to the WHERE clause
easily, just as shown above. Let's see what the result SQL would look like when some parameters are provided:

```postgresql
SELECT book.id,
       book.isbn,
       book.title,
       book.author,
       book.genre,
       book.width_in_centimeters,
       book.shelf_id,
       book."copy"
FROM book
WHERE book.title LIKE 'Hobbit'
```

Now, in normal app we would probably provide some service to translate the conditions into something more elaborate than
**Map<String,String**, allowing us to create more complex queries with type/parameter name validation, but it is outside
the scope of this article.

### UPDATE

```kotlin
fun update(id: Int, book: BookDto) {
    Book.update({ Book.id eq id }) {
        it[isbn] = book.isbn
        it[title] = book.title
        it[author] = book.author
        it[genre] = book.genre
        it[widthInCentimeters] = book.width
        it[assignedShelf] = book.shelfId
    }
}
```

With update the situation is similar to CREATE, but we need to provide the condition to find the rows that need to be
updated, similar to normal SQL query. Again, let's look at the resulting SQL:

```postgresql
UPDATE book
SET isbn='978-1-56619-909-4',
    title='Hobbit 2',
    author='J.R.R Tolken',
    genre=3,
    width_in_centimeters=15,
    shelf_id=NULL
WHERE book.id = 1
```

### DELETE

```kotlin
fun delete(id: Int) {
    Book.deleteWhere {
        Book.id.eq(id)
    }
}

fun delete(id: Int) {
    Shelf.deleteWhere {
        Shelf.id.eq(id)
    }
}
```

DELETE is the simplest of all CRUD operations to express in DSL - the only thing needed is the selection query for the
row we are deleting. As expected, the result query looks like this:

```postgresql
DELETE
FROM book
WHERE book.id = 1
```

### Other queries

The examples show above are only a simple subset of the queries available in SQL and *Exposed* DSL. I included one for
the JOIN clause, which returns all the books assigned to specified shelf:

```kotlin
fun retrieveBooks(shelfId: Int): List<BookDto> =
    Book.innerJoin(Shelf).select { Shelf.id eq shelfId }.map(BookDto::toDto)
```

```postgresql
SELECT book.id,
       book.isbn,
       book.title,
       book.author,
       book.genre,
       book.width_in_centimeters,
       book.shelf_id,
       book."copy",
       shelf.id,
       shelf.width_in_meters
FROM book
         INNER JOIN shelf ON shelf.id = book.shelf_id
WHERE shelf.id = 1
```

*Exposed* DSL supports all the main SQL query clauses - COUNT, GROUP BY, UNION, etc. You can find them, with
examples, [here](https://github.com/JetBrains/Exposed/wiki/DSL).

## Service

The app has a single service that encompasses two business rules - that we want to trace amount of books and that we
cannot exceed shelf width. Let's look at one of these and see how we can leverage smaller queries to build more complex
logic:

```kotlin
fun create(book: BookDto): Int {
    val copies = repository.retrieve(mapOf("isbn" to book.isbn)).map { it.copy }
    val nextCopy = copies.maxByOrNull { it }?.let { max ->
        (1..(max + 1)).toSet().minus(copies.toSet()).minOfOrNull { it }
    } ?: 1
    return repository.create(book.copy(copy = nextCopy))
}
```

This function adds a new book to the bookstore. First, it retrieves all the existing copies with the same **isbn** and
stores the available copies in the **Set**. Next, it generates new copy number, reusing old ones if some were deleted.
Finally, it assigns it to the book and uses the repository to add now properly filled **BookDto** to database. As shown,
this allows us to combine to simple CRUD operations - RETRIEVE and CREATE to implement something a bit more elaborate.
The source code attached contains some more examples of this approach, as well as controllers for the application. Since
they are just your typical, run-of-the-mill, REST controllers, there is no point in showcasing them here. Instead, let's
just move straight to testing.

# Testing

There are two layers you can test the data access on - controllers or repositories. Both require integration testing and
both are viable, depending on amount of logic/mockable dependencies you have in the service layer. Here I prefer to run
tests on the controller layer, but I also provide an example of testing on repository layer.

## Controller

```kotlin
@AutoConfigureEmbeddedDatabase(refresh = AFTER_EACH_TEST_METHOD)
@SpringBootTest(webEnvironment = RANDOM_PORT)
internal class BookControllerTest(@Autowired val restTemplate: TestRestTemplate) 
```

To start off you need to include the standard Spring test wrappings - **@SpringBootTest** and **TestRestTemplate**. Both
are necessary for controller and repository test setup. The one that is less familiar is
**@AutoConfigureEmbeddedDatabase**. Now, it is perfectly valid to run the tests on the external database, especially if
you are running this service in the microservice architecture, since you probably already have a separate service for
the database. If, however, you run your service standalone it is a viable choice to choose something like
**io.zonky.test** package. It allows to set up a PostgresSQL database to work similar to the H2
database - in memory. The way it does it is by deploying microservice containing the database, since Postgres does not
provide embeddable databases yet. So, the annotation sets up the database in memory, and the extra parameter
**AFTER_EACH_TEST_METHOD** takes care of the cleanup after each test. You can tweak the parallelization of the available
databases by tweaking the configuration:

```yaml
spring:
  spring:
    flyway:
      schemas: public

zonky:
  test:
    database:
      prefetching:
        thread-name-prefix: prefetching-
        concurrency: 3
        pipeline-cache-size: 5
        max-prepared-templates: 10
```

The first section is used by the tests to determine the default schema for the Flyway migrations that are run before
tests. The second section determines the concurrency factor of available test databases, and as said before, can be used
to tweak the performance of the tests. The tests themselves are fairly simple:

```kotlin
@Test
fun shouldAllowToAddMultipleCopiesOfTheBook() {
    //given
    val hobbit = BookDto(
        isbn = "978-1-56619-909-4", title = "Hobbit", author = "J.R.R Tolken", genre = FANTASY, width = 15
    )
    //when
    val response1 = restTemplate.postForEntity("/bookstore/book", hobbit, String::class.java)
    val response2 = restTemplate.postForEntity("/bookstore/book", hobbit, String::class.java)
    val books = restTemplate.getForObject("/bookstore/book", Array<BookDto>::class.java)
    //then
    assertEquals(OK, response1.statusCode)
    assertEquals(OK, response2.statusCode)
    assertArrayEquals(arrayOf(hobbit.copy(id = 1, copy = 1), hobbit.copy(id = 2, copy = 2)), books)
}
```

Now, if you need additional setup before test, zonky's library supports a couple of helpful annotations for that -
**@SQL** or **@Flyway** are two example ones. Both allow you to run extra scripts before the test, which allows you to
store helpful database setup in the SQL format - you can find the
docs [here](https://github.com/zonkyio/embedded-database-spring-test). Of course, you can also use *Exposed* DSL to
set up tests, if that is your preference.

## Repository

```kotlin
@AutoConfigureEmbeddedDatabase(refresh = AFTER_EACH_TEST_METHOD)
@SpringBootTest
internal class ShelfRepositoryTest(@Autowired val shelfRepository: ShelfRepository)
```

The repository tests have almost the same setup. Since we are not setting up the controller there is no need for
**RANDOM_PORT**, mocks will suffice. Let's take a look at the test:

```kotlin
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
```

The main difference here is that, since it is a mock application, we need to open the transaction by hand, since it is
not provided automatically. Other than that, the test is very typical for repository test. Again, zonky's library takes
care of database cleanup.

# Conclusions

*Exposed* is one of many alternatives to *Hibernate* for ORM access. What are its biggest strengths? Let's see:

- setup is fairly easy - all you need is a single package, and you can integrate it with Spring almost seamlessly,
- it leverages Kotlin strengths - simplicity and conciseness of syntax - to allow for creation of very powerful yet
  compact queries,
- it is almost a WYSWIG experience - unlike JPA, I have yet to encounter a DSL query that would expand to SQL in an
  unexpected way,
- it supports all the most popular databases, as well as the core of SQL query expressions,
- for more advanced use, it offers DAO mapping to make buffering and performance tweaking of a service easier.

Still, there are some things that you need to take into an account when approaching it:

- I still haven't found an easy way to integrate Spring exception translation to make the *Exposed* SQL exceptions
  easier to handle (but that might be just me),
- even though it supports most of the SQL Query syntax, it can skip some less known, yet useful morsels - like upsert,
  requiring you to implement those by hand,
- it supports most of the SQL mapping types, but some, like Json or Jsonb or UUID, need manual implementation to work,
- it is a Kotlin library, which makes it difficult to integrate with pure Java apps.

All in all, in my opinion, it is a good alternative to other ORM libraries. Unlike *Hibernate* it behaves in a
predictable manner, and unlike *JOOQ* it does not need complex setup to map entities (plus it offers slightly better
syntax). It's direct competitor - *Ktorm* - seemed a bit lacking in terms of functionalities, especially when it comes
to many-to-many relations. I found the implementation experience pretty easy and satisfying, and can endorse it as a
good library to try out.

# References

- [Spring IO starter](https://start.spring.io)
- [Exposed Maven repository](https://mvnrepository.com/artifact/org.jetbrains.exposed)
- [How to implement your own JSON column](https://github.com/JetBrains/Exposed/issues/127)
- [Exposed DSL Wiki](https://github.com/JetBrains/Exposed/wiki/DSL)
- [Zonky's embedded databases testing framework](https://github.com/zonkyio/embedded-database-spring-test)
- [This project's sources](https://github.com/kongus99/bookstore)