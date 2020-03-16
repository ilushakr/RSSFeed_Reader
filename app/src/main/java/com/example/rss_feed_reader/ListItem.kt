package com.example.rss_feed_reader

class ListItem(title: String?, pubDate: String?){

    private var title = title
    private var pubDate = pubDate


    fun getTitle() : String?
    {
        return title
    }

    fun getDate() : String?
    {
        return pubDate
    }

}