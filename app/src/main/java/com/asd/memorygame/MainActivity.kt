package com.asd.memorygame

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asd.memorygame.MemoryBoardAdapter.*
import com.asd.memorygame.models.BoardSize
import com.asd.memorygame.models.MemoryGame
import com.asd.memorygame.models.UserImageList
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso

class MainActivity : AppCompatActivity() {

    /* lateinit : Variable declared here and initialized later*/
    private lateinit var memoryGame: MemoryGame
    private lateinit var adapter: MemoryBoardAdapter
    private lateinit var movesTextView: TextView
    private lateinit var pairsTextView: TextView
    private lateinit var recylerView:RecyclerView
    private lateinit var root: CoordinatorLayout
    private var boardSize: BoardSize = BoardSize.EASY
    private var gameName: String? = null
    private var customGameImages: List<String>? = null

    companion object {
        val TAG = "MainActivity"
        val CREATE_NEW_GAME = 1
        val CUSTOM_BOARD_SIZE = "SIZE"
        val EXTRA_GAME_NAME = "GAME_NAME"
    }

    private val storage = Firebase.storage // external storage to store images
    private val db = Firebase.firestore// database -> cloud storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        movesTextView= findViewById(R.id.tvNumMoves_main_activity)
        pairsTextView  = findViewById(R.id.tvNumPairs_main_activity)
        recylerView = findViewById(R.id.recyclerView_main_activity)
        root = findViewById(R.id.root_main_activity)

        setUpGame()
    }

    private fun setUpGame() {
        supportActionBar?.title = gameName?: getString(R.string.app_name)

        when(boardSize) {
            BoardSize.EASY -> {
                movesTextView.text = "Easy: 4 * 2"
                pairsTextView.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                movesTextView.text = "Medium: 6 * 3"
                pairsTextView.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                movesTextView.text = "Hard: 6 * 4"
                pairsTextView.text = "Pairs: 0 / 12"
            }
        }

        pairsTextView.setTextColor(ContextCompat.getColor(this,R.color.color_progress_start))
        memoryGame = MemoryGame(boardSize, customGameImages)

        var cardFlipInterface = object : CardFlip {
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }
        }
        adapter = MemoryBoardAdapter(this,boardSize, memoryGame.cards, cardFlipInterface)//provide a binding for the data set to the views of the RecylerView
        recylerView.adapter = adapter
        recylerView.setHasFixedSize(true)
        recylerView.layoutManager  = GridLayoutManager(this, boardSize.getWidth())// measures and positions items views
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.refresh_menu -> {
                if(memoryGame.getNumberOfMoves() > 0 && !memoryGame.hasWon()) {
                    showAlertDialog("Quit your current game?", null, View.OnClickListener {
                        setUpGame()
                    })
                } else {
                    setUpGame()
                }
                return true
            }

            R.id.gameType -> {
                showSelectGameDialog()
            }

            R.id.yourGame -> {
                showCreationDialog()
            }

            R.id.downloadCustomGame -> {
                handleDownloadCustomGame()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleDownloadCustomGame() {
        val view = LayoutInflater.from(this).inflate(R.layout.download_custom_game, null)
        showAlertDialog("Enter name of game",view, View.OnClickListener {
            val editTextCustomGame = view.findViewById<EditText>(R.id.edittext_custom_game)
            val gameToLoad = editTextCustomGame.text.toString().trim()
            Log.i(TAG,"Game Name to load = " + gameToLoad)
            downloadNewGame(gameToLoad)
        })
    }

    private fun showCreationDialog() {

        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)

        showAlertDialog("Create your memory game ", boardSizeView, View.OnClickListener {
            val desriedboardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.radioButtonEasy -> BoardSize.EASY
                R.id.radioButtonMedium -> BoardSize.MEDIUM
                R.id.radioButtonHard -> BoardSize.HARD
                else -> BoardSize.EASY
            }
            // New activity
            val intent = Intent(this,CreateActivity::class.java)
            intent.putExtra(CUSTOM_BOARD_SIZE,desriedboardSize)
            startActivityForResult(intent, CREATE_NEW_GAME)
        })
    }

    private fun showSelectGameDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.radioButtonEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.radioButtonMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.radioButtonHard)
        }
        showAlertDialog("Choose game type", boardSizeView, View.OnClickListener {
                boardSize = when (radioGroupSize.checkedRadioButtonId) {
                    R.id.radioButtonEasy -> BoardSize.EASY
                    R.id.radioButtonMedium -> BoardSize.MEDIUM
                    R.id.radioButtonHard -> BoardSize.HARD
                    else -> BoardSize.EASY
                }
                gameName = null
                customGameImages = null
                setUpGame()
        })
    }

    private fun showAlertDialog(title:String, view: View?,positiveButtonClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Ok") {_,_ ->
                positiveButtonClickListener.onClick(null)
            }.show()
    }

    private fun updateGameWithFlip(position: Int) {
        if (memoryGame.hasWon()) {
            Snackbar.make(root,"You won !!!", Snackbar.LENGTH_LONG).show()
            return
        }

        if(memoryGame.isCardFlipUp(position)) {
            Snackbar.make(root,"Invalid move !!!", Snackbar.LENGTH_SHORT).show()
            return
        }
        if(memoryGame.flipCard(position)) {
            var valText = "Pairs: ${memoryGame.numPairsFound}/${boardSize.getNumPairs()}"
            pairsTextView.text = valText
            var color = ArgbEvaluator().evaluate(
                (memoryGame.numPairsFound).toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this,R.color.color_progress_start),
                ContextCompat.getColor(this, R.color.color_progress_end)
                ) as Int
            pairsTextView.setTextColor(color)
            if(memoryGame.hasWon()) {
                Snackbar.make(root, "You won !!!", Snackbar.LENGTH_LONG).show()
                CommonConfetti.rainingConfetti(root, intArrayOf(Color.GREEN, Color.RED, Color.MAGENTA)).oneShot()
            }
        }
        movesTextView.text = "Moves: ${memoryGame.getNumberOfMoves()}"
        adapter.notifyDataSetChanged()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == CREATE_NEW_GAME){
            if(resultCode == Activity.RESULT_OK) {
                val gameName = data?.getStringExtra(EXTRA_GAME_NAME)
                if (gameName == null) {
                    Log.e(TAG, "Error in game creation as game name is null")
                    return;
                }
                downloadNewGame(gameName)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun downloadNewGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            val userImageList = document.toObject(UserImageList::class.java)
            if(userImageList?.images == null) {
                Log.e(TAG, "Invalid custom game data from firestore")
                Snackbar.make(root,"Invalid custom game data from firestore",Snackbar.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            val numCards = userImageList.images.size * 2
            Log.d(TAG,"numCards $numCards")
            val customBoardSize = BoardSize.getByValue(numCards)
            Log.d(TAG,"customBoardSize $customBoardSize")
            for(imageUrl in userImageList.images) {
                Picasso.get().load(imageUrl).fetch()
            }
            boardSize = customBoardSize
            customGameImages = userImageList.images
            gameName = customGameName

            Snackbar.make(root, "You are now playing your game: $customGameName", Snackbar.LENGTH_LONG).show()

            setUpGame()
        }.addOnFailureListener {
            Log.e(TAG,"Error creating custom game")
        }

    }
}