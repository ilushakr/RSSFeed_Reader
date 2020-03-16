package com.example.rss_feed_reader

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_activity.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import java.io.ByteArrayInputStream


class ItemActivity : AppCompatActivity() {

    var db = DataBase(this)
    private var job : Job = Job()
    private lateinit var gotCategoryAndTitle : List<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.item_activity)
        db.readableDatabase
        var actionBar = supportActionBar
        actionBar?.title = "Back to news"

        var intent : Intent = intent
        var title = intent.getStringExtra("item")
        gotCategoryAndTitle = title.split("___")

        //get img from URL if its not in DB
        if(db.getImg(gotCategoryAndTitle[1]) == null) {

            fromUrlToImgView(db.getEnclosureByTitle(gotCategoryAndTitle[1]))
        }
        else {

            CoroutineScope(IO).launch{ fromDBtoImgView() }
        }

        full_text.text = db.getText(gotCategoryAndTitle[1])

    }

    //Set to mainActivity selected category
    override fun onOptionsItemSelected(item: MenuItem): Boolean
    {
        if(job.isActive) job.cancel()
        when (item.itemId) {
            android.R.id.home -> {startActivity(Intent(this, MainActivity::class.java).apply { putExtra("back", gotCategoryAndTitle[0]) })}
        }
        return true
    }

    //Get img from DB and resize by width
    private suspend fun fromDBtoImgView()
    {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width: Int = size.x


        var imgByteArray = db.getImg(gotCategoryAndTitle[1])
        val arrayInputStream = ByteArrayInputStream(imgByteArray)
        val bitMap = BitmapFactory.decodeStream(arrayInputStream)
        withContext(Dispatchers.Main) {
            image.setImageBitmap(Bitmap.createScaledBitmap(bitMap, width, (width / bitMap.width) * bitMap.height, false))
        }
    }

    //Get img from URL and resize by width
    private fun fromUrlToImgView(enclosure : String)
    {
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val width: Int = size.x
        val height: Int = size.y
        image.layoutParams.width = width
        image.layoutParams.height = width * 409 / 720

        Picasso.get().load(enclosure).fit().into(image)
    }
}
