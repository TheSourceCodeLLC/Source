package net.sourcebot.api.misc

import java.awt.Color

private fun rgb(r: Int, g: Int, b: Int) = Color(r, g, b)

enum class SourceColor(val color: Color) {
    INFO(rgb(52, 152, 219)),
    SUCCESS(rgb(46, 204, 113)),
    WARNING(rgb(230, 126, 34)),
    ERROR(rgb(231, 76, 60));
}