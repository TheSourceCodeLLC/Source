package net.sourcebot.api

import java.time.Duration
import java.time.temporal.ChronoUnit

object DurationUtils {
    @JvmStatic
    private val pattern = "(\\d+)([Mdhms])".toRegex()

    @JvmStatic
    fun parseDuration(input: String): Duration {
        if (!pattern.matches(input)) throw IllegalArgumentException("Parse input does not match pattern!")
        var seconds: Long = 0
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
            seconds += (asLong * asChrono.duration.seconds)
        }
        return Duration.ofSeconds(seconds)
    }

    @JvmStatic
    fun formatDuration(duration: Duration): String {
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
            var name = unit.name
            if (amount == 1L) name = name.substring(0, name.length - 1)
            "$amount $name"
        }.toLowerCase()
    }

    private fun ChronoUnit.seconds() = this.duration.seconds
}