package net.sourcebot.api.alert

import java.awt.Color

/**
 * Creates a color using RGB values.
 * @param[r] The 'R' value for the desired color.
 * @param[g] The 'G' value for the desired color.
 * @param[b] The 'B' value for the desired color.
 *
 * @return A [Color] built using the specified values.
 */
private fun rgb(r: Int, g: Int, b: Int) = Color(r, g, b)

/**
 * Constant definitions of colors used throughout Source.
 * Colors are sourced from https://flatuicolors.com/palette/defo
 */
enum class SourceColor(val color: Color) {
    /* Peter River */ INFO(rgb(52, 152, 219)),
    /* Emerald */ SUCCESS(rgb(46, 204, 113)),
    /* Carrot */ WARNING(rgb(230, 126, 34)),
    /* Alizarin */ ERROR(rgb(231, 76, 60));
}