package com.asd.memorygame.utils

import android.graphics.Bitmap

object BitmapScaler {

    public fun scaleToFitWeight(bitmap: Bitmap, width: Int): Bitmap {
        val scaleFactor = width / bitmap.width.toFloat()
        return Bitmap.createScaledBitmap(bitmap, width , (bitmap.height * scaleFactor).toInt(), true)
    }


    public fun scaleToFitHeight(bitmap: Bitmap, height: Int): Bitmap {
        val scaleFactor = height / bitmap.height.toFloat()
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scaleFactor).toInt(), height, true)

    }

}
