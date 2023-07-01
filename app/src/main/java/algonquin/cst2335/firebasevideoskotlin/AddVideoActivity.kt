package algonquin.cst2335.firebasevideoskotlin

import algonquin.cst2335.firebasevideoskotlin.databinding.ActivityAddVideoBinding
import algonquin.cst2335.firebasevideoskotlin.databinding.ActivityVideosBinding
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.MediaController
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

/*
 This activity will be used to add a new video
 */
class AddVideoActivity : AppCompatActivity() {

    //actionbar
    private lateinit var actionBar: ActionBar

    //constants to pick video
    private val VIDEO_PICK_GALLERY_CODE = 100
    private val VIDEO_PICK_CAMERA_CODE = 101
    //Constant to request camera permission to record video from camera
    private val CAMERA_REQUEST_CODE =102

    //array for camera request permission
    private lateinit var cameraPermissions: Array<String>

    private lateinit var progressDialog: ProgressDialog

    private var videoUri: Uri? = null //uri of picked video

    private var title:String = "";

    private lateinit var binding: ActivityAddVideoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVideoBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //init actionbar
        actionBar = supportActionBar!!
        //title
        actionBar.title = "Add New Video"
        //add back button
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setDisplayShowHomeEnabled(true)

        //Init camera permission array
        cameraPermissions = arrayOf(android.Manifest.permission.CAMERA,android.Manifest.permission.WRITE_EXTERNAL_STORAGE )

        //init progressbar
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setMessage("Uploading video...")
        progressDialog.setCanceledOnTouchOutside(false)
        //Handle click, upload video
        binding.uploadVideoBtn.setOnClickListener{
            //get title
            title = binding.titleEt.text.toString().trim()
            if(TextUtils.isEmpty(title)){
                //no title is entered
                Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            }
            else if(videoUri==null){
                //video is not pick
                Toast.makeText(this, "Pick the video first", Toast.LENGTH_SHORT).show()
            }
            else{
                //title entered, video picked, now upload video
                uploadVideoFirebase()
            }
        }

        binding.pickVideoFab.setOnClickListener{
            videoPickDialog()
        }

    }

    private fun uploadVideoFirebase(){
        //show progress
        progressDialog.show()

        //timestampt
        val timestamp = ""+ System.currentTimeMillis()

        //file path and name in firebase storage
        val filePathAndName = "Videos/video_$timestamp"

        //storeage reference
        val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
        //upload video using uri of video of storage
        storageReference.putFile(videoUri!!).addOnSuccessListener {taskSnapshot ->
            //uploaded, get url of uploaded video
            val uriTask = taskSnapshot.storage.downloadUrl
            while(!uriTask.isSuccessful);
            val downloadUri = uriTask.result
            if(uriTask.isSuccessful){
                //video url is received successfully

                //now we can add video details to firebase db
                val hashMap = HashMap<String, Any>()
                hashMap["id"] = "$timestamp"
                hashMap["title"] = "$title"
                hashMap["timestamp"] = "$timestamp"
                hashMap["videoUri"] = "$downloadUri"

                //put the above info to db
                val dbReference = FirebaseDatabase.getInstance().getReference("Videos")
                dbReference.child(timestamp).setValue(hashMap)
                    .addOnSuccessListener {
                        //video info added successfully
                        progressDialog.dismiss()
                        Toast.makeText(this, "Video Uploaded", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener{e->
                        //failed adding video info
                        progressDialog.dismiss()
                        Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()

                    }
            }
        }
            .addOnFailureListener{e ->
                progressDialog.dismiss()
                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setVideoToVideoView(){
        //set the picked video to video view

        //video play controls
        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoView)

        //set media controller
        binding.videoView.setMediaController(mediaController)
        //set video uri
        binding.videoView.setVideoURI(videoUri)
        binding.videoView.requestFocus()
        binding.videoView.setOnPreparedListener{
            //when video is ready, by default don't play automatically
            binding.videoView.pause()
        }
    }

    private fun videoPickDialog(){

        //options to display in dialog
        val options = arrayOf("Camera", "Gallery")

        //alart dialog
        val builder = AlertDialog.Builder(this)
        //title
        builder.setTitle("Pick Video From").setItems(options){
            dialogInterface, i->
            //handle item clicks
            if(i==0){
                //camera clicked
                if(!checkCameraPermissions()){
                    //permission was not allowed, request
                requestCameraPermission()}
                else{
                    //permission was allowed, pick video
                    videoPickCamera()
                }
            }
            else{
                //gallery clicked
                videoPickGallery()
            }
        }.show()
    }

    private fun requestCameraPermission(){
        //request camera permissions
        ActivityCompat.requestPermissions(
            this,
            cameraPermissions,
            CAMERA_REQUEST_CODE
        )
    }

    private fun checkCameraPermissions():Boolean{
        //check if camera permissions: camera and storage is allowed or not
        val result1 = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val result2 = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        //return result as true/false
        return result1&&result2
    }

    private fun videoPickGallery(){
        //video pick intent gallery
        val intent = Intent()
        intent.type =  "video/*"
        intent.action = Intent.ACTION_GET_CONTENT

        videoPickLauncher.launch(Intent.createChooser(intent, "Choose video"))
    }

    private fun videoPickCamera() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        videoPickCameraLauncher.launch(intent)
    }

    private val videoPickLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            // Handle the selected video here
            if (data != null && data.data != null) {
                videoUri = data.data
                setVideoToVideoView()
            } else {
                // Handle the case when the intent data or video URI is null
                Toast.makeText(this, "Failed to retrieve video URI", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            //cancelled picking vide
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT)
        }
    }

    private val videoPickCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            // Handle the captured video here

            if (data != null && data.data != null) {
                videoUri = data.data
                setVideoToVideoView()
            } else {
                // Handle the case when the intent data or video URI is null
                Toast.makeText(this, "Failed to retrieve video URI", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            //cancelled picking vide
            Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            CAMERA_REQUEST_CODE ->
                if (grantResults.size>0){
                    //check if permission allowed or denied
                    val cameraAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1]==PackageManager.PERMISSION_GRANTED
                    if(cameraAccepted && storageAccepted){
                        //both permissions allowed
                        videoPickCamera()
                    }
                    else{
                        //both or one of those are denied
                        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                    }
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}