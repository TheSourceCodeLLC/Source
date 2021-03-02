package net.sourcebot.api

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal

object DurationUtils {
    @JvmStatic
    private val pattern = "(\\d+)([yMwdhms])".toRegex()

    @JvmStatic
    fun parseDuration(input: String): Duration {
        var seconds: Long = 0
        pattern.findAll(input).forEach {
            val (amount, unit) = it.destructured
            val asLong = amount.toLong()
            val asChrono = when (unit) {
                "y" -> ChronoUnit.YEARS
                "M" -> ChronoUnit.MONTHS
                "w" -> ChronoUnit.WEEKS
                "d" -> ChronoUnit.DAYS
                "h" -> ChronoUnit.HOURS
                "m" -> ChronoUnit.MINUTES
                "s" -> ChronoUnit.SECONDS
                else -> throw IllegalStateException("Invalid unit parsed!")
            }
            seconds += (asLong * asChrono.duration.seconds)
        }
        return Duration.ofSeconds(seconds)
    }

    @JvmStatic
    fun formatDuration(
        duration: Duration, strategy: FormatStrategy = FormatStrategy.EXPANDED
    ): String {
        val perMonth = ChronoUnit.MONTHS.seconds()
        val perDay = ChronoUnit.DAYS.seconds()
        val perHour = ChronoUnit.HOURS.seconds()
        val perMinute = ChronoUnit.MINUTES.seconds()

        val months = duration.dividedBy(perMonth).seconds
        val minusMonths = duration.minusSeconds(months * perMonth)
        val days = minusMonths.dividedBy(perDay).seconds
        val minusDays = minusMonths.minusSeconds(days * perDay)
        val hours = minusDays.dividedBy(perHour).seconds
        val minusHours = minusDays.minusSeconds(hours * perHour)
        val minutes = minusHours.dividedBy(perMinute).seconds
        val minusMinutes = minusHours.minusSeconds(minutes * perMinute)
        val seconds = minusMinutes.seconds
        return mapOf(
            ChronoUnit.MONTHS to months,
            ChronoUnit.DAYS to days,
            ChronoUnit.HOURS to hours,
            ChronoUnit.MINUTES to minutes,
            ChronoUnit.SECONDS to seconds
        ).entries.filter { (_, a) -> a != 0L }.joinToString { (unit, amount) ->
            "$amount${strategy.getSubstitute(amount, unit)}"
        }
    }

    @JvmStatic
    fun formatMillis(
        millis: Long, strategy: FormatStrategy = FormatStrategy.EXPANDED
    ) = formatDuration(Duration.ofMillis(millis), strategy)

    @JvmStatic
    fun formatSeconds(
        seconds: Long, strategy: FormatStrategy = FormatStrategy.EXPANDED
    ) = formatDuration(Duration.ofSeconds(seconds), strategy)

    private fun ChronoUnit.seconds() = this.duration.seconds
}

fun durationOf(format: String) = DurationUtils.parseDuration(format)
fun Duration.formatLong() = DurationUtils.formatDuration(this)
fun Duration.formatShort() = DurationUtils.formatDuration(this, FormatStrategy.SHORT)

fun differenceBetween(first: Temporal, second: Temporal): String {
    return Duration.between(first, second).truncatedTo(ChronoUnit.SECONDS).formatLong()
}

interface FormatStrategy {
    fun getSubstitute(amount: Long, unit: ChronoUnit): String

    companion object {
        @JvmStatic val EXPANDED = object : FormatStrategy {
            override fun getSubstitute(
                amount: Long,
                unit: ChronoUnit
            ) = unit.name.let { plural ->
                " " + if (amount == 1L) plural.substring(0, plural.length - 1) else plural
            }.toLowerCase()
        }

        @JvmStatic val SHORT = object : FormatStrategy {
            private val substitutes = mapOf(
                ChronoUnit.MONTHS to "M",
                ChronoUnit.DAYS to "d",
                ChronoUnit.HOURS to "h",
                ChronoUnit.MINUTES to "m",
                ChronoUnit.SECONDS to "s"
            )

            override fun getSubstitute(
                amount: Long,
                unit: ChronoUnit
            ) = substitutes[unit]!!
        }
    }
}
