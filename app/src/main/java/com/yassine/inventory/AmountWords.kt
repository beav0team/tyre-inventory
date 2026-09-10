package com.yassine.inventory

import kotlin.math.round

object AmountWords {

    private val UNITS = arrayOf(
        "", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
        "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
    )
    private val TENS = arrayOf(
        "", "dix", "vingt", "trente", "quarante", "cinquante",
        "soixante", "soixante-dix", "quatre-vingt", "quatre-vingt-dix",
    )

    fun words(value: Long): String = wholeNumber(value)

    fun inFrench(total: Double): String {
        val cents = round(total * 100).toLong()
        val whole = cents / 100
        val part = cents % 100
        return buildString {
            append(wholeNumber(whole))
            append(' ')
            append(if (whole > 1) "dirhams" else "dirham")
            if (part > 0) {
                append(" et ")
                append(belowHundred(part.toInt()))
                append(if (part > 1) " centimes" else " centime")
            }
        }
    }

    fun sentence(total: Double): String =
        "Arrêtée la présente facture à la somme de : ${inFrench(total)}".uppercase()

    private fun wholeNumber(n: Long): String = when {
        n == 0L -> "zéro"
        n >= 1_000_000_000L -> {
            val billions = n / 1_000_000_000L
            val label = if (billions == 1L) "un milliard" else "${belowThousand(billions.toInt())} milliards"
            label + remainderSuffix(n % 1_000_000_000L)
        }
        n >= 1_000_000L -> {
            val millions = n / 1_000_000L
            val label = if (millions == 1L) "un million" else "${belowThousand(millions.toInt())} millions"
            label + remainderSuffix(n % 1_000_000L)
        }
        n >= 1_000L -> {
            val thousands = n / 1_000L
            val label = if (thousands == 1L) "mille" else "${belowThousand(thousands.toInt())} mille"
            label + remainderSuffix(n % 1_000L)
        }
        else -> belowThousand(n.toInt())
    }

    private fun remainderSuffix(rest: Long): String =
        if (rest == 0L) "" else " ${wholeNumber(rest)}"

    private fun belowThousand(n: Int): String {
        if (n == 0) return ""
        val h = n / 100
        val rest = n % 100
        return buildString {
            when {
                h == 0 -> Unit
                h == 1 -> append("cent")
                else -> append("${belowHundred(h)} cent")
            }
            if (rest > 0) {
                if (isNotEmpty()) append(' ')
                append(belowHundred(rest))
            } else if (h > 1) {
                append('s')
            }
        }.toString()
    }

    private fun belowHundred(n: Int): String = when {
        n == 0 -> ""
        n < 17 -> UNITS[n]
        n < 20 -> "dix-" + UNITS[n - 10]
        n < 71 -> {
            val t = TENS[n / 10]
            val u = n % 10
            when {
                u == 1 -> "$t et un"
                u == 0 -> t
                else -> "$t-${UNITS[u]}"
            }
        }
        n < 80 -> if (n == 71) "soixante et onze" else "soixante-${belowHundred(n - 60)}"
        n == 80 -> "quatre-vingts"
        else -> "quatre-vingt-${belowHundred(n - 80)}"
    }
}