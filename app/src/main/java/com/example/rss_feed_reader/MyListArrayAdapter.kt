package com.example.rss_feed_reader

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.rss_feed_reader.ListItem
import java.util.concurrent.TimeoutException

class MyListArrayAdapter(private var myContext: Context, var resources : Int, var items : List<ListItem>) : ArrayAdapter<ListItem>(myContext, resources, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val layoutInflater : LayoutInflater = LayoutInflater.from(myContext)
        val view : View = layoutInflater.inflate(resources, null)

        var title : TextView = view.findViewById(R.id.text_title)
        var date : TextView = view.findViewById(R.id.text_date)

        var myItem : ListItem = items[position]
        title.text = myItem.getTitle()
        date.text = myItem.getDate()
        return view
    }

}