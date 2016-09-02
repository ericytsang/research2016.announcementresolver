package com.github.ericytsang.research2016.announcementresolver.persist

import com.github.ericytsang.lib.json.Json
import com.github.ericytsang.research2016.announcementresolver.simulation.Behaviour
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.makeFrom
import com.github.ericytsang.research2016.propositionallogic.toParsableString
import javafx.scene.paint.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter

/**
 * Created by surpl on 8/25/2016.
 */
object DefinitionsSaveFileParser
{
    /**
     * the root node of all the json is a [JSONArray] of [JSONObject] elements
     * that are formatted in the way described by [jsonSchema].
     */
    private object jsonSchema
    {
        /**
         * key to [String] containing parseable [Proposition] string.
         */
        object proposition:Json.Schema()

        /**
         * key to [JSONObject] containing information about a [Behaviour].
         */
        object behaviour:Json.Schema()
        {
            /**
             * key to [String] that describes which behaviour this is.
             */
            object name:Json.Schema()
            {
                enum class Value
                {
                    DO_NOTHING,
                    GUARD,
                    PATROL,
                    WANDER,
                    HIDE,
                    FOLLOW,
                    PROTECT,
                }
            }

            /**
             * exclusive to [jsonSchema.proposition.behaviour.name.Value.GUARD].
             * key to [JSONObject] describing position of agent.
             */
            object follow:Json.Schema()
            {
                /**
                 * color of the agent to follow.
                 */
                object agentColor:Json.Schema()
                {
                    /**
                     * key to [Double] describing red component of the [agentColor].
                     */
                    object r:Json.Schema()

                    /**
                     * key to [Double] describing green component of the [agentColor].
                     */
                    object g:Json.Schema()

                    /**
                     * key to [Double] describing blue component of the [agentColor].
                     */
                    object b:Json.Schema()
                }
            }

            /**
             * exclusive to [jsonSchema.proposition.behaviour.name.Value.GUARD].
             * key to [JSONObject] describing position of agent.
             */
            object protect:Json.Schema()
            {
                /**
                 * color of the agent to follow.
                 */
                object agentColor:Json.Schema()
                {
                    /**
                     * key to [Double] describing red component of the [agentColor].
                     */
                    object r:Json.Schema()

                    /**
                     * key to [Double] describing green component of the [agentColor].
                     */
                    object g:Json.Schema()

                    /**
                     * key to [Double] describing blue component of the [agentColor].
                     */
                    object b:Json.Schema()
                }
            }

            /**
             * exclusive to [jsonSchema.proposition.behaviour.name.Value.GUARD].
             * key to [JSONObject] describing position of agent.
             */
            object guard:Json.Schema()
            {
                /**
                 * key to [Int] describing x position of agent.
                 */
                object x:Json.Schema()

                /**
                 * key to [Int] describing y position of agent.
                 */
                object y:Json.Schema()

                /**
                 * key to [Int] describing direction of agent.
                 */
                object direction:Json.Schema()
            }

            /**
             * exclusive to [jsonSchema.proposition.behaviour.name.Value.PATROL].
             *
             * key to [JSONArray] containing [JSONObject] elements each of which
             * describe a waypoint to visit while on patrol.
             */
            object waypoints:Json.Schema()
            {
                /**
                 * key to [Int] describing x position of agent.
                 */
                object x:Json.Schema()

                /**
                 * key to [Int] describing y position of agent.
                 */
                object y:Json.Schema()

                /**
                 * key to [Int] describing direction of agent.
                 */
                object direction:Json.Schema()
            }
        }
    }

    fun load(file:File):List<BehaviourEntry>
    {
        return file.inputStream()
            // read file into a string
            .use {it.readBytes().let {String(it)}}
            // parse the string as a JSONArray of JSONObjects
            .let {JSONArray(it)}.map {it as JSONObject}
            // convert JSONObjects into objects
            .map {it.toBehaviourEntry()}
    }

    fun save(file:File,objects:List<BehaviourEntry>)
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

    data class BehaviourEntry(
        val proposition:Proposition,
        val behaviour:Behaviour)

    private fun BehaviourEntry.toJsonObject():JSONObject
    {
        return Json.obj()
        {
            "${jsonSchema.proposition}" mapsTo proposition.toParsableString()
            "${jsonSchema.behaviour}" mapsTo behaviour.toJsonObject()
        }
    }

    private fun JSONObject.toBehaviourEntry():BehaviourEntry
    {
        return BehaviourEntry(
            getString("${jsonSchema.proposition}").let {Proposition.makeFrom(it)},
            getJSONObject("${jsonSchema.behaviour}").toBehaviour())
    }

    private fun Behaviour.toJsonObject():JSONObject
    {
        return when(this)
        {
            is Behaviour.DoNothing -> Json.obj()
            {
                "${jsonSchema.behaviour.name}" mapsTo jsonSchema.behaviour.name.Value.DO_NOTHING.name
            }
            is Behaviour.Wander -> Json.obj()
            {
                "${jsonSchema.behaviour.name}" mapsTo jsonSchema.behaviour.name.Value.WANDER.name
            }
            is Behaviour.Hide -> Json.obj()
            {
                "${jsonSchema.behaviour.name}" mapsTo jsonSchema.behaviour.name.Value.HIDE.name
            }
            is Behaviour.Follow -> Json.obj()
            {
                "${jsonSchema.behaviour.name}" mapsTo jsonSchema.behaviour.name.Value.FOLLOW.name
                "${jsonSchema.behaviour.follow}" mapsTo Json.obj()
                {
                    "${jsonSchema.behaviour.follow.agentColor}" mapsTo Json.obj()
                    {
                        "${jsonSchema.behaviour.follow.agentColor.r}" mapsTo agentColor.red
                        "${jsonSchema.behaviour.follow.agentColor.g}" mapsTo agentColor.blue
                        "${jsonSchema.behaviour.follow.agentColor.b}" mapsTo agentColor.green
                    }
                }
            }
            is Behaviour.Protect -> Json.obj()
            {
                "${jsonSchema.behaviour.name}" mapsTo jsonSchema.behaviour.name.Value.PROTECT.name
                "${jsonSchema.behaviour.protect}" mapsTo Json.obj()
                {
                    "${jsonSchema.behaviour.protect.agentColor}" mapsTo Json.obj()
                    {
                        "${jsonSchema.behaviour.protect.agentColor.r}" mapsTo agentColor.red
                        "${jsonSchema.behaviour.protect.agentColor.g}" mapsTo agentColor.blue
                        "${jsonSchema.behaviour.protect.agentColor.b}" mapsTo agentColor.green
                    }
                }
            }
            is Behaviour.Guard -> Json.obj()
            {
                "${jsonSchema.behaviour.name}" mapsTo jsonSchema.behaviour.name.Value.GUARD.name
                "${jsonSchema.behaviour.guard}" mapsTo Json.obj()
                {
                    "${jsonSchema.behaviour.guard.x}" mapsTo x
                    "${jsonSchema.behaviour.guard.y}" mapsTo y
                    "${jsonSchema.behaviour.guard.direction}" mapsTo direction.name
                }
            }
            is Behaviour.Patrol -> Json.obj()
            {
                "${jsonSchema.behaviour.name}" mapsTo jsonSchema.behaviour.name.Value.PATROL.name
                "${jsonSchema.behaviour.waypoints}" mapsTo waypoints.map()
                {
                    Json.obj()
                    {
                        "${jsonSchema.behaviour.guard.x}" mapsTo it.x
                        "${jsonSchema.behaviour.guard.y}" mapsTo it.y
                        "${jsonSchema.behaviour.guard.direction}" mapsTo it.direction.name
                    }
                }.let {JSONArray(it)}
            }
        }
    }

    private fun JSONObject.toBehaviour():Behaviour
    {
        return when(jsonSchema.behaviour.name.Value.valueOf(getString("${jsonSchema.behaviour.name}")))
        {
            jsonSchema.behaviour.name.Value.WANDER -> Behaviour.Wander()
            jsonSchema.behaviour.name.Value.DO_NOTHING -> Behaviour.DoNothing()
            jsonSchema.behaviour.name.Value.HIDE -> Behaviour.Hide()
            jsonSchema.behaviour.name.Value.FOLLOW ->
            {
                val red = getJSONObject("${jsonSchema.behaviour.follow}")
                    .getJSONObject("${jsonSchema.behaviour.follow.agentColor}")
                    .getDouble("${jsonSchema.behaviour.follow.agentColor.r}")
                val green = getJSONObject("${jsonSchema.behaviour.follow}")
                    .getJSONObject("${jsonSchema.behaviour.follow.agentColor}")
                    .getDouble("${jsonSchema.behaviour.follow.agentColor.g}")
                val blue = getJSONObject("${jsonSchema.behaviour.follow}")
                    .getJSONObject("${jsonSchema.behaviour.follow.agentColor}")
                    .getDouble("${jsonSchema.behaviour.follow.agentColor.b}")
                Behaviour.Follow(Color.color(red,blue,green))
            }
            jsonSchema.behaviour.name.Value.PROTECT ->
            {
                val red = getJSONObject("${jsonSchema.behaviour.protect}")
                    .getJSONObject("${jsonSchema.behaviour.protect.agentColor}")
                    .getDouble("${jsonSchema.behaviour.protect.agentColor.r}")
                val green = getJSONObject("${jsonSchema.behaviour.protect}")
                    .getJSONObject("${jsonSchema.behaviour.protect.agentColor}")
                    .getDouble("${jsonSchema.behaviour.protect.agentColor.g}")
                val blue = getJSONObject("${jsonSchema.behaviour.protect}")
                    .getJSONObject("${jsonSchema.behaviour.protect.agentColor}")
                    .getDouble("${jsonSchema.behaviour.protect.agentColor.b}")
                Behaviour.Protect(Color.color(red,blue,green))
            }
            jsonSchema.behaviour.name.Value.PATROL ->
            {
                val waypoints = getJSONArray("${jsonSchema.behaviour.waypoints}").map()
                {
                    it as JSONObject
                    val x = it.getInt("${jsonSchema.behaviour.waypoints.x}")
                    val y = it.getInt("${jsonSchema.behaviour.waypoints.y}")
                    val direction = it
                        .getString("${jsonSchema.behaviour.waypoints.direction}")
                        .let {Behaviour.CardinalDirection.valueOf(it)}
                    Behaviour.Guard(x,y,direction)
                }
                Behaviour.Patrol(waypoints)
            }
            jsonSchema.behaviour.name.Value.GUARD ->
            {
                val x = getJSONObject("${jsonSchema.behaviour.guard}")
                    .getInt("${jsonSchema.behaviour.guard.x}")
                val y = getJSONObject("${jsonSchema.behaviour.guard}")
                    .getInt("${jsonSchema.behaviour.guard.y}")
                val direction = getJSONObject("${jsonSchema.behaviour.guard}")
                    .getString("${jsonSchema.behaviour.guard.direction}")
                    .let {Behaviour.CardinalDirection.valueOf(it)}
                Behaviour.Guard(x,y,direction)
            }
        }
    }
}
