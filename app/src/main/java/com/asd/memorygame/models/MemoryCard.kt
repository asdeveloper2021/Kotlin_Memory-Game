package com.asd.memorygame.models

/* Data classes act like data containers. Constructor properties
* need to be marked with val or var
* If the constructor is parameterless, default values for all paramters
* need to be specified
* In this example only identifier is part of default constructor*/
data class MemoryCard(val identifier: Int,
                      val imageUrl: String? = null,
                      var isFaceUp: Boolean = false,
                      var isMatched: Boolean = false) {
}