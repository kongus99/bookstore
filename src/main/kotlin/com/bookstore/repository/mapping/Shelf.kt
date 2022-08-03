package com.bookstore.repository.mapping

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow

object Shelf : IntIdTable() {
    val widthInMeters = float("width_in_meters")
}

data class ShelfDto(val id: Int?, val widthInMeters: Float) {
    companion object {
        fun toDto(it: ResultRow): ShelfDto =
            ShelfDto(
                it[Shelf.id].value,
                it[Shelf.widthInMeters]
            )
    }
}




