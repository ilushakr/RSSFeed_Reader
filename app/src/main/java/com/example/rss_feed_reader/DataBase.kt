package com.example.rss_feed_reader

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.Cursor.FIELD_TYPE_BLOB
import android.database.Cursor.FIELD_TYPE_STRING
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.getStringOrNull

class DataBase(context: Context) : SQLiteOpenHelper(context,
    DB_NAME, null,
    DB_VERSION
) {

    companion object {
        private const val DB_VERSION = 1
        private const val DB_NAME = "MyDB"

        private const val TABLE_NAME = "items"
        private const val COL_TITLE = "title"
        private const val COL_PUBDATE = "pubDate"
        private const val COL_CATEGORY = "category"
        private const val COL_ENCLOSURE = "enclosure"
        private const val COL_FULLTESXT = "fullText"
        private const val COL_IMG = "image"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTable = "CREATE TABLE " + TABLE_NAME + " (" + COL_TITLE + " TITLE," +
                COL_PUBDATE + " PUBDATE," +
                COL_CATEGORY + " CATEGORY," +
                COL_ENCLOSURE + " ENCLOSURE," +
                COL_FULLTESXT + " FULTEXT," +
                COL_IMG + " BLOB)"
        db!!.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }


    fun insertData(title : String, pubDate : String, category : String, enclosure : String, fullText : String)
    {
        var db = this.writableDatabase
        var contentValues = ContentValues()
        contentValues.put(COL_TITLE, title)
        contentValues.put(COL_PUBDATE, pubDate?.removeRange(pubDate!!.length - 9, pubDate!!.length)?.removeRange(0, 5))
        contentValues.put(COL_CATEGORY, category)
        contentValues.put(COL_ENCLOSURE, enclosure)
        contentValues.put(COL_FULLTESXT, fullText?.replace(Regex("\\n{2,}\\s*")){"\n"})

        db.insert(TABLE_NAME, null, contentValues)
    }

    //Sort titles by category
    fun getTitlesByCategory(category: String): List<ListItem>
    {
        var db = this.readableDatabase
        var list = mutableListOf<ListItem>()
        var cursor : Cursor
        if(category == "Все категории")
        {
            cursor = db.query(
                TABLE_NAME, arrayOf(
                    COL_TITLE,
                    COL_PUBDATE
                ), null, null,
                null,
                null,
                "$COL_PUBDATE DESC",
                null
            )
        }

        else {
            cursor = db.query(
                TABLE_NAME, arrayOf(
                    COL_TITLE,
                    COL_PUBDATE
                ), "$COL_CATEGORY=?", arrayOf(category),
                null,
                null,
                "$COL_PUBDATE DESC",
                null
            )
        }
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    ListItem(
                        cursor.getString(cursor.getColumnIndex(COL_TITLE)),
                        cursor.getString(cursor.getColumnIndex(COL_PUBDATE))
                    )
                )
            } while (cursor.moveToNext())
            cursor.close()
        }
        return list
    }

    private fun isEmpty() : Boolean
    {
        var db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_NAME"
        var cursor: Cursor = db.rawQuery(query, null)
        return cursor.count == 0
        cursor.close()
    }

    //Return last by date title
    fun getFirstTitle() : String
    {
        var db = this.readableDatabase
        val query = "SELECT $COL_TITLE FROM $TABLE_NAME ORDER BY $COL_PUBDATE DESC LIMIT 1"
        var cursor: Cursor = db.rawQuery(query, null)
        return if(isEmpty()) "EmptyDataBase"
        else {
            cursor.moveToFirst()
            var first = cursor.getString(cursor.getColumnIndex(COL_TITLE))
            cursor.close()
            first
        }
    }

    fun getCategoryList() : List<String>
    {
        var db = this.readableDatabase
        var list = mutableListOf<String>()
        list.add("Все категории")
        val query = "SELECT DISTINCT $COL_CATEGORY FROM $TABLE_NAME"
        val cursor = db.rawQuery(query, null)
        if(cursor != null)
        {
            if(cursor.moveToFirst())
            {
                do {
                    if(cursor.getType(cursor.getColumnIndex(COL_CATEGORY)) == FIELD_TYPE_STRING) {
                        list.add(cursor.getString(cursor.getColumnIndex(COL_CATEGORY)))
                    }
                }while (cursor.moveToNext())
            }
        }
        cursor.close()
        return list
    }

    fun getEnclosureList() : List<String>
    {
        var db = this.readableDatabase
        var list = mutableListOf<String>()
        val query = "SELECT $COL_ENCLOSURE FROM $TABLE_NAME ORDER BY $COL_PUBDATE DESC"
        val cursor = db.rawQuery(query, null)
        if(cursor.moveToFirst())
        {
            do {
                if(cursor.getType(cursor.getColumnIndex(COL_ENCLOSURE)) == FIELD_TYPE_STRING) {
                    list.add(cursor.getString(cursor.getColumnIndex(COL_ENCLOSURE)))
                }
            }while (cursor.moveToNext())

        }
        cursor.close()
        return list
    }

    fun getTitleList() : List<String>
    {
        var db = this.readableDatabase
        var list = mutableListOf<String>()
        val query = "SELECT $COL_TITLE FROM $TABLE_NAME ORDER BY $COL_PUBDATE DESC"
        val cursor = db.rawQuery(query, null)
        if(cursor.moveToFirst())
        {
            do {
                list.add(cursor.getString(cursor.getColumnIndex(COL_TITLE)))
            }while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getImg(title: String) : ByteArray?
    {
        var db = this.readableDatabase
        val cursor = db.query(
            TABLE_NAME, arrayOf(
                COL_IMG
            ), "$COL_TITLE=?", arrayOf(title),
            null,
            null,
            null,
            null
        )
        var img : ByteArray? = null
        if(cursor != null)
        {
            if (cursor.moveToFirst()) {
                do {
                    img = cursor.getBlob(cursor.getColumnIndex(COL_IMG))
                } while (cursor.moveToNext())
                cursor.close()
            }
        }
        return img

    }

    fun getListImg() : List<ByteArray?>
    {
        var db = this.readableDatabase
        var list = mutableListOf<ByteArray?>()
        val query = "SELECT $COL_IMG FROM $TABLE_NAME"
        val cursor = db.rawQuery(query, null)
        if(cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    list.add(cursor.getBlob(cursor.getColumnIndex(COL_IMG)))
                } while (cursor.moveToNext())
            }
        }
        cursor.close()
        return list
    }

    fun getText(title : String) : String
    {
        var db = this.readableDatabase
        lateinit var text : String
        lateinit var date : String
        val cursor = db.query(
            TABLE_NAME, arrayOf(
                COL_PUBDATE,
                COL_FULLTESXT
            ), "$COL_TITLE=?", arrayOf(title),
            null,
            null,
            null,
            null
        )
        if (cursor.moveToFirst()) {
            do {
                date = cursor.getString(cursor.getColumnIndex(COL_PUBDATE))
                text = cursor.getString(cursor.getColumnIndex(COL_FULLTESXT))
            } while (cursor.moveToNext())
            cursor.close()
        }
        return date + "\n" + text

    }

    fun setImage(title : String, img : ByteArray)
    {
        var db = this.writableDatabase
        var contentValues = ContentValues()
        contentValues.put(COL_IMG, img)
        db.update(TABLE_NAME, contentValues, "$COL_TITLE = ?", arrayOf(title))
    }

    fun getEnclosureByTitle(title : String) : String
    {
        var db = this.readableDatabase
        lateinit var enclosure : String
        val cursor = db.query(
            TABLE_NAME, arrayOf(
                COL_ENCLOSURE
            ), "$COL_TITLE=?", arrayOf(title),
            null,
            null,
            null,
            null
        )
        if(cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    enclosure = cursor.getString(cursor.getColumnIndex(COL_ENCLOSURE))
                } while (cursor.moveToNext())
            }
        }
        cursor.close()
        return enclosure
    }

    fun deleteAll()
    {
        var db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
        db.close()
    }

}