package com.github.ericytsang.research2016.announcementresolver.persist

import com.github.ericytsang.lib.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter

/**
 * Created by surpl on 8/25/2016.
 */
object ObstaclesSaveFileParser
{
    /**
     * the root node of all the json is a [JSONArray] of [JSONObject] elements
     * that are formatted in the way described by [jsonSchema].
     */
    private object jsonSchema
    {
        /**
         * key to [JSONObject] containing coordinates of first end of wall.
         */
        object position1:Json.Schema()
        {
            /**
             * key to [Int] that is x coordinate of first end of wall.
             */
            object x:Json.Schema()

            /**
             * key to [Int] that is y coordinate of first end of wall.
             */
            object y:Json.Schema()
        }

        /**
         * key to [JSONObject] containing coordinates of second end of wall.
         */
        object position2:Json.Schema()
        {
            /**
             * key to [Int] that is x coordinate of second end of wall.
             */
            object x:Json.Schema()

            /**
             * key to [Int] that is y coordinate of second end of wall.
             */
            object y:Json.Schema()
        }
    }

    fun load(file:File):List<Obstacle>
    {
        return file.inputStream()
            // read file into a string
            .use {it.readBytes().let {String(it)}}
            // parse the string as a JSONArray of JSONObjects
            .let {JSONArray(it)}.map {it as JSONObject}
            // convert JSONObjects into objects
            .map {it.toObstacle()}
    }

    fun save(file:File,objects:List<Obstacle>)
    {
        // convert objects into JSON
        val json = objects
            .map {it.toJsonObject()}
            .let {JSONArray(it)}
            .toString(4)

        // save JSON to the file
        file.outputStream()
            .let {PrintWriter(it)}
            .use {it.print(json)}
    }

    data class Obstacle(
        val position1X:Int,
        val position1Y:Int,
        val position2X:Int,
        val position2Y:Int)

    private fun Obstacle.toJsonObject():JSONObject
    {
        return Json.obj()
        {
            "${jsonSchema.position1}" mapsTo Json.obj()
            {
                "${jsonSchema.position1.x}" mapsTo position1X
                "${jsonSchema.position1.y}" mapsTo position1Y
            }
            "${jsonSchema.position2}" mapsTo Json.obj()
            {
                "${jsonSchema.position1.x}" mapsTo position2X
                "${jsonSchema.position1.y}" mapsTo position2Y
            }
        }
    }

    private fun JSONObject.toObstacle():Obstacle
    {
        val position1X = getJSONObject("${jsonSchema.position1}").getInt("${jsonSchema.position1.x}")
        val position1Y = getJSONObject("${jsonSchema.position1}").getInt("${jsonSchema.position1.y}")
        val position2X = getJSONObject("${jsonSchema.position2}").getInt("${jsonSchema.position2.x}")
        val position2Y = getJSONObject("${jsonSchema.position2}").getInt("${jsonSchema.position2.y}")

        return Obstacle(
            position1X,
            position1Y,
            position2X,
            position2Y)
    }
}
