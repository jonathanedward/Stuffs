package com.stampbook.app.ui.stamp

import android.content.Context
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.stampbook.app.R
import com.stampbook.app.core.Scripts
import com.stampbook.app.core.StampFace
import com.stampbook.app.core.TextScript

/**
 * The lettering a stamp is set in.
 *
 * Three faces ship with the app, for Latin, Greek and Cyrillic. Everything else —
 * CJK, Arabic, Thai, Devanagari — is left to the device's own serif, which is not
 * a compromise: a serif CJK face is Mincho in Japan, Song in China and Myeongjo in
 * Korea, all of them the formal register, and the device's Arabic serif is Naskh,
 * the hand used for government documents. Bundling those families is not an
 * option; Noto's CJK set alone dwarfs this app.
 */
class StampFonts(
    private val documentary: Pair<Typeface, Typeface>,
    private val institutional: Pair<Typeface, Typeface>,
    private val registry: Pair<Typeface, Typeface>,
    /** Serial numbers and codes, which a real stamp sets apart from its lettering. */
    val typewriter: Typeface,
) {

    /**
     * The face for one line of text. A line in a script the bundled faces do not
     * carry goes to the device rather than being rendered in a fallback that
     * would ignore the register entirely.
     */
    fun typeface(face: StampFace, bold: Boolean, text: String): Typeface {
        if (Scripts.scriptOf(text) == TextScript.OTHER) {
            return Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        val pair = when (face) {
            StampFace.DOCUMENTARY -> documentary
            StampFace.INSTITUTIONAL -> institutional
            StampFace.REGISTRY -> registry
        }
        return if (bold) pair.second else pair.first
    }

    companion object {
        @Volatile private var cached: StampFonts? = null

        fun from(context: Context): StampFonts = cached ?: synchronized(this) {
            cached ?: build(context).also { cached = it }
        }

        private fun build(context: Context): StampFonts {
            fun font(id: Int, fallback: Int): Typeface =
                runCatching { ResourcesCompat.getFont(context, id) }.getOrNull()
                    ?: Typeface.create(Typeface.SERIF, fallback)

            return StampFonts(
                documentary = font(R.font.eb_garamond_regular, Typeface.NORMAL) to
                    font(R.font.eb_garamond_bold, Typeface.BOLD),
                institutional = font(R.font.archivo_regular, Typeface.NORMAL) to
                    font(R.font.archivo_bold, Typeface.BOLD),
                registry = font(R.font.roboto_slab_regular, Typeface.NORMAL) to
                    font(R.font.roboto_slab_bold, Typeface.BOLD),
                typewriter = font(R.font.cutive_mono_regular, Typeface.NORMAL),
            )
        }
    }
}

@Composable
fun rememberStampFonts(): StampFonts {
    val context = LocalContext.current
    return remember(context) { StampFonts.from(context) }
}
