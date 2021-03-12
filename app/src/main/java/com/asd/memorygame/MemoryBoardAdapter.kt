package com.asd.memorygame

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.asd.memorygame.models.BoardSize
import com.asd.memorygame.models.MemoryCard
import com.squareup.picasso.Picasso

import kotlin.math.min

class MemoryBoardAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val card: List<MemoryCard>,
    private val cardClicked: CardFlip
) :
    RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            val isFaceUp = card[position].isFaceUp

            if(isFaceUp) {
                if(card[position].imageUrl != null)
                    Picasso.get().load(card[position].imageUrl).placeholder(R.drawable.loading).into(imageButton)
                else
                    imageButton.setImageResource(card[position].identifier)
            } else {
                imageButton.setImageResource(R.drawable.card_image)
            }

            imageButton.alpha = if(card[position].isMatched) .4f else 1.0f

            val colorStateList = if(card[position].isMatched){
                ContextCompat.getColorStateList(context,R.color.color_gray)
            } else {
                null
            }
            ViewCompat.setBackgroundTintList(imageButton,colorStateList)

            imageButton.setOnClickListener {
                cardClicked.onCardClicked(position)
            }
        }
    }

    interface CardFlip {
        fun onCardClicked(position: Int) {

        }
    }

    //singleton
    companion object {
        private const val MARGIN_SIZE = 10
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {// how to create one view of the recyclerview
        val cardWidth = parent.width/boardSize.getWidth() - (2 * MARGIN_SIZE);
        val cardHeight = parent.height/boardSize.getHeight() - (2 * MARGIN_SIZE);
        val sideLength = min(cardWidth,cardHeight)
        val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent , false)
        val layoutParams: ViewGroup.MarginLayoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = sideLength
        layoutParams.height = sideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = boardSize.numCards

}


