package com.personalbookkeeping.common

import java.time.Instant
import java.util.UUID

fun interface AppClock {
    fun now(): Instant
}

object SystemAppClock : AppClock {
    override fun now(): Instant = Instant.now()
}

fun interface IdGenerator {
    fun newId(): String
}

object UuidGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
