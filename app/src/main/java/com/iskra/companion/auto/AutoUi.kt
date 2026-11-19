package com.iskra.companion.auto

import android.text.SpannableString
import android.text.Spanned
import androidx.annotation.DrawableRes
import androidx.car.app.CarContext
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.Row
import androidx.core.graphics.drawable.IconCompat

/**
 * Small helpers that give the Android Auto templates the Iskra look:
 * tinted vector icons and colour-coded headline text. Everything stays inside
 * the Car App Library's allowed model (icons + ForegroundCarColorSpan only).
 */
object AutoUi {

    val OK: CarColor = CarColor.GREEN
    val WARN: CarColor = CarColor.YELLOW
    val BAD: CarColor = CarColor.RED
    val SPARK: CarColor = CarColor.BLUE

    fun icon(context: CarContext, @DrawableRes res: Int, tint: CarColor): CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(context, res)).setTint(tint).build()

    /** Whole string tinted with [color]. */
    fun colored(text: String, color: CarColor): CarText {
        val s = SpannableString(text)
        s.setSpan(ForegroundCarColorSpan.create(color), 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return CarText.create(s)
    }

    /**
     * One dashboard row: plain title (row titles may not carry colour spans),
     * a colour-coded status line, and a detail line.
     */
    fun row(
        context: CarContext,
        @DrawableRes iconRes: Int,
        tint: CarColor,
        title: String,
        status: String,
        statusColor: CarColor,
        detail: String
    ): Row =
        Row.Builder()
            .setImage(icon(context, iconRes, tint), Row.IMAGE_TYPE_ICON)
            .setTitle(title)
            .addText(colored(status, statusColor))
            .addText(detail)
            .build()

    fun socColor(soc: Float): CarColor = when {
        soc > 25f -> OK
        soc > 12f -> WARN
        else -> BAD
    }

    fun tyreColor(psi: Float): CarColor = when {
        psi <= 0f -> CarColor.DEFAULT
        psi < 30f -> BAD
        psi < 33f -> WARN
        else -> OK
    }

    /** Worst colour among the four tyres, so one glance tells you if something is off. */
    fun worstTyre(vararg psi: Float): CarColor {
        val colors = psi.map { tyreColor(it) }
        return when {
            colors.contains(BAD) -> BAD
            colors.contains(WARN) -> WARN
            else -> OK
        }
    }

    fun deltaColor(mv: Float): CarColor = when {
        mv < 30f -> OK
        mv < 60f -> WARN
        else -> BAD
    }
}
