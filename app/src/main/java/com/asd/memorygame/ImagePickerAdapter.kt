package com.asd.memorygame

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.asd.memorygame.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(private val context: Context,
                         private val boardSize: BoardSize,
                         private val imagesList: List<Uri>,
                         private val onClickInterface: HandleImageClick) :
    RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface HandleImageClick {
        fun onClick(){

        }
    }

    private lateinit var imageView: ImageView

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        private val imageView = itemView.findViewById<ImageView>(R.id.imageView_card_image)
        fun bind(uri: Uri) {
            imageView.setImageURI(uri)
            imageView.setOnClickListener(null)
        }

        fun bind() {
            imageView.setOnClickListener{
                    onClickInterface.onClick()
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var layout = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardWidth = parent.width / boardSize.getWidth()
        val cardHeight = parent.height / boardSize.getHeight()
        val minSize = min(cardWidth,cardHeight)
        val layoutParams = layout.findViewById<ImageView>(R.id.imageView_card_image).layoutParams
        layoutParams.width = minSize
        layoutParams.height = minSize
        return ViewHolder(layout)
    }

    override fun onBindViewHolder(holder: ImagePickerAdapter.ViewHolder, position: Int) {
        if(position < imagesList.size)
            holder.bind(imagesList[position])
        else
            holder.bind()
    }

    override fun getItemCount(): Int {
        return boardSize.getNumPairs()
    }


}
