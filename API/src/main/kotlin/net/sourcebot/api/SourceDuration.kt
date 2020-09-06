package net.sourcebot.api

import java.time.temporal.*
import java.util.*

class SourceDuration private constructor(
    private val timeMap: EnumMap<ChronoUnit, Long> = EnumMap(ChronoUnit::class.java)
) : TemporalAmount {
    private val units = listOf(
        ChronoUnit.MONTHS,
        ChronoUnit.DAYS,
        ChronoUnit.HOURS,
        ChronoUnit.MINUTES,
        ChronoUnit.SECONDS
    )
    private val seconds: Long
        get() = units.sumOf { (timeMap[it] ?: 0L) * it.duration.seconds }

    override fun get(
        unit: TemporalUnit
    ): Long {
        if (unit !in units) throw UnsupportedTemporalTypeException("Unsupported unit: $unit")
        return timeMap[unit] ?: 0L
    }

    override fun getUnits() = units

    override fun addTo(
        temporal: Temporal
    ): Temporal = temporal.plus(seconds, ChronoUnit.SECONDS)

    override fun subtractFrom(
        temporal: Temporal
    ): Temporal = temporal.minus(seconds, ChronoUnit.SECONDS)

    fun isEmpty() = seconds == 0.toLong()

    companion object {
        @JvmStatic private val pattern = "(\\d+)([Mdhms])".toRegex()

        @JvmStatic fun parse(input: String): SourceDuration {
            if (!pattern.matches(input)) throw IllegalArgumentException("Parse input does not match pattern!")
            val map = EnumMap<ChronoUnit, Long>(ChronoUnit::class.java)
            pattern.findAll(input).forEach {
                val (amount, unit) = it.destructured
                val asLong = amount.toLong()
                val asChrono = when (unit) {
                    "M" -> ChronoUnit.MONTHS
                    "d" -> ChronoUnit.DAYS
                    "h" -> ChronoUnit.HOURS
                    "m" -> ChronoUnit.MINUTES
                    "s" -> ChronoUnit.SECONDS
                    else -> throw IllegalStateException("Invalid unit parsed!")
                }
                map.compute(asChrono) { _,v -> if (v == null) asLong else v + asLong }
            }
            return SourceDuration(map)
        }
    }

    override fun toString() = timeMap.entries.joinToString{ (unit, amount) ->
        var name = unit.name.toLowerCase().capitalize()
        if (amount == 1.toLong()) name = name.substring(0, name.length - 1)
        "$amount $name"
    }
}