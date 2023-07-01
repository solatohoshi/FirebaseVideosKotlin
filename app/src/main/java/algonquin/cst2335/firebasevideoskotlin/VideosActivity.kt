package algonquin.cst2335.firebasevideoskotlin

import algonquin.cst2335.firebasevideoskotlin.databinding.ActivityVideosBinding
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/* this activity will display list of videos*/
class VideosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideosBinding
    //array list for video list
    private lateinit var videoArrayList: ArrayList<ModelVideo>
    //adapter
    private lateinit var adapterVideo: AdapterVideo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideosBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        title = "Video"

        //function call to load videos fromfirebase
        loadVideosFromFirebase()
        //handle click
        binding.addVideoFab.setOnClickListener{
            //start AddVideoActivity to add new video
            startActivity(Intent(this, AddVideoActivity::class.java))

        }
    }

    private fun loadVideosFromFirebase() {
        //init arraylist before adding data into it
        videoArrayList = ArrayList()

        //reference of firebase db
        val ref = FirebaseDatabase.getInstance().getReference("Videos")
        ref.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot){
                videoArrayList.clear()
                for(ds in snapshot.children){
                    //get data as model
                    val modelVideo = ds.getValue(ModelVideo::class.java)
                    //add to array list
                    videoArrayList.add(modelVideo!!)
                }
                //setup adapter
                adapterVideo = AdapterVideo(this@VideosActivity, videoArrayList )
                //set adapter to recyclerview
                binding.videosRv.adapter = adapterVideo
            }

            override fun onCancelled(error: DatabaseError){

            }
        })
    }

}