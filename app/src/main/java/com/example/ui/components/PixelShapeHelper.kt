package com.example.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.PixelIconShape

object PixelShapeHelper {

    fun toComposeShape(shape: PixelIconShape, size: Dp = 56.dp): Shape {
        return when (shape) {
            PixelIconShape.CIRCLE -> CircleShape
            PixelIconShape.ROUNDED_SQUARE -> RoundedCornerShape(size * 0.28f)
            PixelIconShape.SQUIRCLE -> GenericShape { layoutSize, _ ->
                val w = layoutSize.width
                val h = layoutSize.height
                val c = w * 0.08f
                moveTo(w / 2f, 0f)
                cubicTo(w - c, 0f, w, c, w, h / 2f)
                cubicTo(w, h - c, w - c, h, w / 2f, h)
                cubicTo(c, h, 0f, h - c, 0f, h / 2f)
                cubicTo(0f, c, c, 0f, w / 2f, 0f)
                close()
            }
            PixelIconShape.TEARDROP -> RoundedCornerShape(
                topStart = size * 0.5f,
                topEnd = size * 0.5f,
                bottomStart = size * 0.5f,
                bottomEnd = size * 0.14f
            )
        }
    }
}
