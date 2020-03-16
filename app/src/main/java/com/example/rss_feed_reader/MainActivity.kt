package com.example.rss_feed_reader

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.view.get
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.my_list.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.channels.Channel
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.*
import java.lang.Exception
import java.net.URL
import java.util.*
import java.util.stream.LongStream

class MainActivity : AppCompatActivity() {

    private val category = "Все категории"

    var db = DataBase(this)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var actionBar = supportActionBar
        actionBar?.title = "Vesti.ru"

        db.writableDatabase

        //Check if should continue with selected category or first start
        if(intent.getStringExtra("back") != null) {

            var category: String = intent.getStringExtra("back")

            var adapterSpinner = ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, db.getCategoryList())
            spinner.adapter = adapterSpinner
            spinner.setSelection(db.getCategoryList().indexOf(category))

            var myAdapter = MyListArrayAdapter(
                this@MainActivity,
                R.layout.my_list,
                db.getTitlesByCategory(category)
            )
            listView.adapter = myAdapter

        }
        else {
            swipeLayout.isRefreshing = true
            CoroutineScope(IO).launch{
                getURL()
            }
        }


        swipeLayout.setOnRefreshListener {
            onRefresh()
        }

        //Sort by category
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {  }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                view as TextView
                db.readableDatabase
                var myAdapter = MyListArrayAdapter(
                    this@MainActivity,
                    R.layout.my_list,
                    db.getTitlesByCategory(view.text.toString())
                )
                listView.adapter = myAdapter
            }
        }

        //Not to update if not at the beginning of list
        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}
            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                swipeLayout.isEnabled = firstVisibleItem == 0
            }
        })

        //Go to intent with selected title
        listView.setOnItemClickListener { _, view, _, _ ->
            startActivity(Intent(this, ItemActivity::class.java).apply { putExtra("item", spinner.selectedItem.toString() +  "___" + view.text_title.text.toString()) })
        }
    }



    private fun onRefresh()
    {
        swipeLayout.isRefreshing = true
        CoroutineScope(IO).launch{
            getURL()
        }
    }

    private suspend fun getURL()
    {
        readRSSFeed("https://www.vesti.ru/vesti.rss")

        setListView()

        setSpinner()

        enclosureToImg()
    }

    //Go through RSS feed until the last title from DB or until end. Put data from every item to DB
    private fun readRSSFeed(urlAddress: String?)
    {
        try{
            val rssUrl = URL("https://www.vesti.ru/vesti.rss")
            var input = BufferedReader(InputStreamReader(rssUrl.openStream(), "UTF8"))
            var sc = Scanner(input)
            var line = ""
            var isStop = false
            var lastItem = db.getFirstTitle()
            var fileInput: FileOutputStream
            while (sc.hasNextLine())
            {
                if(sc.nextLine().also{line = it}.contains("<item>"))
                {
                    fileInput = openFileOutput("item.txt", Context.MODE_PRIVATE)
                    fileInput.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n").toByteArray())
                    while (!line.contains("</item>"))
                    {
                        if(line.contains(lastItem)){isStop = true; fileInput = openFileOutput("item.txt", Context.MODE_PRIVATE); fileInput.write(("").toByteArray()); fileInput.close(); break}
                        fileInput.write((line + "\n").toByteArray())
                        line = sc.nextLine()
                    }
                    if(isStop) break
                    fileInput.write(("</item>").toByteArray())
                    fileInput.close()
                    strToLV()
                    fileInput = openFileOutput("item.txt", Context.MODE_PRIVATE)
                    fileInput.write(("").toByteArray())
                    fileInput.close()
                }

            }
            input.close()
        } catch (e : Exception) {CoroutineScope(Main).launch { swipeLayout.isRefreshing = false; Toast.makeText(this@MainActivity, "Failed",Toast.LENGTH_LONG).show() }}
    }

    private suspend fun setListView()
    {
        var adapter = MyListArrayAdapter(
            this@MainActivity,
            R.layout.my_list,
            db.getTitlesByCategory(category)
        )
        withContext(Main) {
            listView.adapter = adapter
            swipeLayout.isRefreshing = false
        }
    }

    private suspend fun setSpinner()
    {
        var adapterSpinner = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, db.getCategoryList())
        withContext(Main)
        {
            spinner.adapter = adapterSpinner
        }
    }

    //Parse of XML doc from RSSFeed to insert to DB
    private fun strToLV()
    {
        var fileInputStream = openFileInput("item.txt")
        var inputStreamReader = InputStreamReader(fileInputStream)
        var factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        var xpp = factory.newPullParser()
        xpp.setInput(StringReader(inputStreamReader.readText()))
        var eventType = xpp.eventType
        var title = ""
        var pubDate = ""
        var category = ""
        var enclosure = ""
        var fullText = ""
        while (eventType != XmlPullParser.END_DOCUMENT)
        {
            if(xpp.name == "item" && eventType == XmlPullParser.START_TAG)
            {

                xpp.next()
                while (xpp.name != "item")
                {
                    when(xpp.name)
                    {
                        "title" -> {title = xpp.nextText()}
                        "pubDate" -> {pubDate = xpp.nextText()}
                        "category" -> {category = xpp.nextText()}
                        "enclosure" -> {enclosure = xpp.getAttributeValue(null, "url")}
                        "yandex:full-text" -> {fullText = xpp.nextText()}
                    }
                    xpp.next()
                }
                db.insertData(title, pubDate, category, enclosure, fullText)
            }
            eventType = xpp.next()
        }
    }

    //Convert img from URL to ByteArray to insert in DB. Make asynchronously every 10 items with function "toByteArray"
    private suspend fun enclosureToImg()
    {
        var listEnclosure = db.getEnclosureList()
        var listTitle = db.getTitleList()
        var count = db.getListImg().count { it == null }


        for(x in 1..count/10)
        {
            val job = CoroutineScope(IO).launch {
                toByteArray(listEnclosure ,listTitle, (x - 1) * 10, 10)
            }
            job.join()
        }

        val job = CoroutineScope(IO).launch {
            toByteArray(listEnclosure, listTitle, count/10, count - count/10)
        }
        job.join()

    }

    private suspend fun toByteArray(listEnclosure : List<String>, listTitle : List<String>, x : Int, times : Int)
    {
        withContext(IO)
        {
            repeat(times){i ->
                launch {
                    if(db.getImg(listTitle[i + x]) == null) {
                        var bitMap = Picasso.get().load(listEnclosure[i + x]).get()
                        var stream = ByteArrayOutputStream()
                        bitMap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        var imgByteArray = stream.toByteArray()
                        db.setImage(listTitle[i + x], imgByteArray)
                    }
                }
            }
        }
    }
}
