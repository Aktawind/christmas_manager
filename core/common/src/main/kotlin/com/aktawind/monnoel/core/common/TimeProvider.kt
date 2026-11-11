package com.aktawind.monnoel.core.common

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/** Source de temps injectable, pour pouvoir tester la logique liée aux dates. */
interface TimeProvider {
    fun now(): Instant

    fun today(): LocalDate
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): Instant = Instant.now()

    override fun today(): LocalDate = LocalDate.now(Clock.systemDefaultZone())
}

/** Générateur d'identifiants (UUID texte, voir ADR 0003). */
interface IdGenerator {
    fun newId(): String
}

class UuidGenerator @Inject constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
