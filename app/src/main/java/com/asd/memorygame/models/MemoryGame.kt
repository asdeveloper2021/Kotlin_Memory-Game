package com.asd.memorygame.models

import com.asd.memorygame.utils.DEFAULT_ICONS

/* private val here means the setter is private and getter is public by default*/
class MemoryGame(private val boardSize: BoardSize, customImages: List<String>?) {
    val cards: List<MemoryCard>
    var numPairsFound = 0
    private var indexOfSingleSelectedCard : Int? = null
    private var noOfMoves = 0

    init {
        if(customImages == null) {
            val chosenImages = DEFAULT_ICONS.shuffled().take(boardSize.getNumPairs())
            val randomImages = (chosenImages + chosenImages).shuffled()
            cards = randomImages.map { MemoryCard(it) }
        } else {
            val radomImages = (customImages + customImages).shuffled()
            cards = radomImages.map { MemoryCard(it.hashCode(),it)}
        }
    }

    fun flipCard(position: Int) : Boolean {
        noOfMoves++
        var foundMatch = false
        if(indexOfSingleSelectedCard == null) {
            indexOfSingleSelectedCard = position
            restoreCards()
        } else {
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!,position)
            indexOfSingleSelectedCard = null
        }
        val card = cards[position]
        card.isFaceUp = !card.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {

        if(cards[position1].identifier != cards[position2].identifier)
            return false

        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsFound++
        return true
    }

    private fun restoreCards() {
        for(card in cards ){
            if(!card.isMatched)
                card.isFaceUp = false
        }
    }

    fun hasWon(): Boolean {
        if(numPairsFound == boardSize.getNumPairs())
            return true

        return false

    }

    fun isCardFlipUp(position: Int): Boolean {
        val card = cards[position]
        if(card.isFaceUp)
            return true

        return false

    }

    fun getNumberOfMoves(): Int{
        return noOfMoves / 2
    }
}