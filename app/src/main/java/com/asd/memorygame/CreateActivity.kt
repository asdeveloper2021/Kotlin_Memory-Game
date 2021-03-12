package com.asd.memorygame

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.asd.memorygame.models.BoardSize
import com.asd.memorygame.utils.BitmapScaler
import com.asd.memorygame.utils.isPermissionGranted
import com.asd.memorygame.utils.requestStoragePermission
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object {
        val TAG = "CreateActivity"
        val CUSTOM_BOARD_SIZE = "SIZE"
        val EXTRA_GAME_NAME = "GAME_NAME"
        val PICK_PHOTO_CODE = 2
        val READ_EXTERNAL_PHOTOS_CODE = 3
        val READ_EXTERNAL_STORAGE_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        val MIN_GAME_NAME_LENGTH = 3
        val MAX_GAME_NAME_LENGTH = 14
    }

    private lateinit var boardSize: BoardSize
    private var numOfImagesRequired = -1

    private lateinit var recylerView: RecyclerView
    private lateinit var editText: EditText
    private lateinit var button: Button
    private lateinit var progressBar: ProgressBar

    private var imagesList= mutableListOf<Uri>()
    private lateinit var gameName: String

    private lateinit var imAdapter: ImagePickerAdapter

    private val storage = Firebase.storage // external storage to store images
    private val db = Firebase.firestore// database -> cloud storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val intent = getIntent()
        boardSize = intent.getSerializableExtra(CUSTOM_BOARD_SIZE) as BoardSize
        numOfImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics ( 0 / ${numOfImagesRequired} )"

        recylerView = findViewById(R.id.rec_create_activity)
        editText = findViewById(R.id.editText_create_activity)
        button = findViewById(R.id.button_create_activity)
        progressBar = findViewById(R.id.progressBar_create_activity)

        button.setOnClickListener{
            Log.i(TAG, "on Button Save clicked")
            saveToFirebase()
        }

        editText.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        editText.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(p0: Editable?) {
                button.isEnabled = shouldEnableSaveButton()
            }

        })

        var onCLickInterface = object: ImagePickerAdapter.HandleImageClick {
            override fun onClick(){
                    /* Check if permission is granted*/
                    if(isPermissionGranted(this@CreateActivity,READ_EXTERNAL_STORAGE_PERMISSION))
                        launchIntentForPhotos()
                    else
                        requestStoragePermission(this@CreateActivity, READ_EXTERNAL_STORAGE_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
            }
        }

        imAdapter = ImagePickerAdapter(this, boardSize, imagesList,onCLickInterface)
        recylerView.adapter = imAdapter
        recylerView.setHasFixedSize(true)// not changing dimentions of recylerView
        recylerView.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    private fun saveToFirebase() {
        button.isEnabled = false
        gameName = editText.text.toString()
        // Check if gameName already exists
        db.collection("games").document(gameName).get().addOnSuccessListener {
            document ->
                if(document !=null && document.data !=null) {
                    AlertDialog.Builder(this)
                            .setTitle("Rename Game")
                            .setMessage("Game game taken. Try another name")
                            .setPositiveButton("OK", null)
                            .show()
                    button.isEnabled = true
                } else {
                    handleImageUpload()
                }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Error occured in saveToFirebase $exception")
            button.isEnabled = false
            Toast.makeText(this,"Error in game creation", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleImageUpload() {
        progressBar.visibility = View.VISIBLE

        var errorOccured = false
        val uploadedImageUri = mutableListOf<String>()

        for((index, uri) in imagesList.withIndex()){
            val imageByteArray = getImageByteArray(uri)
            val filePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                    .continueWithTask { photoUploadTask ->
                        Log.i(TAG, "uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                        photoReference.downloadUrl
                    }. addOnCompleteListener { downloadUrlTask ->

                        if(!downloadUrlTask.isSuccessful){
                            Log.e(TAG, "Exception in firefase upload")
                            Toast.makeText(this,"Failed to upload image", Toast.LENGTH_LONG).show()
                            errorOccured = true
                            progressBar.visibility = View.GONE

                            return@addOnCompleteListener
                        }

                        if(errorOccured)
                            return@addOnCompleteListener

                        val downloadUrl = downloadUrlTask.result.toString()
                        uploadedImageUri.add(downloadUrl)

                        uploadedImageUri
                        progressBar.progress = uploadedImageUri.size * 100 / imagesList.size


                        if(uploadedImageUri.size == imagesList.size)
                        {
                            handleAllImagesDownloaded(gameName, uploadedImageUri)
                        }

                    }
        }

    }

    private fun handleAllImagesDownloaded(gameName: String, uploadedImageUri: MutableList<String>) {
        db.collection("games").document(gameName)
                .set(mapOf("images" to uploadedImageUri))
                .addOnCompleteListener { gameCreationTask ->
                    progressBar.visibility = View.GONE
                    if(!gameCreationTask.isSuccessful) {
                        Log.i(TAG,"Exception game creation ", gameCreationTask.exception)
                        return@addOnCompleteListener
                    }
                    Log.i(TAG,"Successfully uploaded to db $gameName")
                    AlertDialog.Builder(this)
                            .setTitle("Upload complete. You can play your game now")
                            .setPositiveButton("OK") {_,_ ->
                                val resultData = Intent()
                                resultData.putExtra(EXTRA_GAME_NAME, gameName)
                                setResult(Activity.RESULT_OK,resultData)
                                finish()
                            }.show()
                }
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "size = ${originalBitmap.width} ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap,250)
        Log.i(TAG, "size scaled down = ${scaledBitmap.width} ${scaledBitmap.height}")
        val byteArrayStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayStream)
        return byteArrayStream.toByteArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == READ_EXTERNAL_PHOTOS_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                launchIntentForPhotos()
            else
                Toast.makeText(this, "Grant storage permission to creat new game",
                    Toast.LENGTH_LONG).show()
        }
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true)// select multiple images

        startActivityForResult(Intent.createChooser(intent,"Choose images"), PICK_PHOTO_CODE)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null)
        {
            Log.i(TAG, "Error in onActivity Result")
            return;
        }
        /* For some devices or applications which allow only one image to be selected*/
        val selectedUri = data.data
        /* When multiple images are selected*/
        val clipData = data.clipData

        if(clipData != null){
            Log.i(TAG, "Size of clipData ${clipData.itemCount}")
            for(i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                if(imagesList.size < numOfImagesRequired) {
                    imagesList.add(clipItem.uri)
                }
            }
        } else if( selectedUri != null) {
            imagesList.add(selectedUri)
        }

        imAdapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics (${imagesList.size} / $numOfImagesRequired )"
        if(shouldEnableSaveButton())
            button.isEnabled = true
    }

    private fun shouldEnableSaveButton(): Boolean {
        gameName = editText.text.toString()
        if(imagesList.size == boardSize.getNumPairs() &&
            gameName.length > MIN_GAME_NAME_LENGTH)
            return true

        return false
    }
}