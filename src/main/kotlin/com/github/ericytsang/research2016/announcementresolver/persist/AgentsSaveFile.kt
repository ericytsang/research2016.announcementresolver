package com.github.ericytsang.research2016.announcementresolver.persist

import com.github.ericytsang.lib.json.Json
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.announcementresolver.simulation.Behaviour
import com.github.ericytsang.research2016.propositionallogic.AnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.BeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.ComparatorBeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.HammingDistanceComparator
import com.github.ericytsang.research2016.propositionallogic.OrderedSetsComparator
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.SatisfiabilityBeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.Variable
import com.github.ericytsang.research2016.propositionallogic.WeightedHammingDistanceComparator
import com.github.ericytsang.research2016.propositionallogic.makeFrom
import com.github.ericytsang.research2016.propositionallogic.toParsableString
import javafx.scene.paint.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import java.io.File
import java.io.PrintWriter
import java.util.ArrayList

/**
 * Created by surpl on 8/24/2016.
 */
class AgentsSaveFile(val file:File):Closeable
{
    private object jsonSchema
    {
        /**
         * key to the [JSONArray] of propositional sentences from inputted [JSONObject].
         */
        object initialK:Json.Schema()
        /**
         * key to the propositional sentence [String] from inputted [JSONObject].
         */
        object targetK:Json.Schema()
        /**
         * key to the [JSONObject] describing a belief revision operator.
         */
        object operator:Json.Schema()
        {
            /**
             * key to the [String] that indicates what type of
             * [BeliefRevisionStrategy] the [operator] is describing.
             */
            object name:Json.Schema()
            {
                enum class Value
                {
                    SATISFIABILITY,
                    HAMMING_DISTANCE,
                    WEIGHTED_HAMMING_DISTANCE,
                    ORDERED_SETS
                }
            }
            object weights:Json.Schema()
            object sentences:Json.Schema()
        }
        /**
         * key to [JSONObject] describing position of agent.
         */
        object position:Json.Schema()
        {
            /**
             * key to [Int] describing x position of agent.
             */
            object x:Json.Schema()
            /**
             * key to [Int] describing y position of agent.
             */
            object y:Json.Schema()
        }
        /**
         * key to [Int] describing direction of agent.
         */
        object direction:Json.Schema()
        /**
         * key to [JSONObject] describing color of agent.
         */
        object color:Json.Schema()
        {
            object r:Json.Schema()
            object g:Json.Schema()
            object b:Json.Schema()
        }
    }

    val agents = ArrayList<Agent>()

    init
    {
        // initialize members from file data if possible
        if (file.exists())
        {
            agents += file.inputStream()
                // read file into a string
                .use {it.readBytes().let {String(it)}}
                // parse the string as a JSONArray of JSONObjects
                .let {JSONArray(it)}.map {it as JSONObject}
                // convert JSONObjects into agents
                .map {it.toAgent()}
        }
    }

    override fun close()
    {
        // convert agents into JSON
        val json = agents
            .map {it.toJsonObject()}
            .let {JSONArray(it)}
            .toString(4)

        // save JSON to the file
        file.outputStream()
            .let {PrintWriter(it)}
            .use {it.print(json)}
    }

    data class Agent(
        val problemInstance:AnnouncementResolutionStrategy.ProblemInstance,
        val position:Simulation.Cell,
        val direction:Behaviour.CardinalDirection,
        val color:Color)

    private fun Agent.toJsonObject():JSONObject
    {
        return Json.obj()
        {
            "${jsonSchema.initialK}" mapsTo problemInstance.initialBeliefState.map {it.toParsableString()}.let {JSONArray(it)}
            "${jsonSchema.targetK}" mapsTo problemInstance.targetBeliefState.toParsableString()
            "${jsonSchema.operator}" mapsTo problemInstance.beliefRevisionStrategy.toJsonObject()
            "${jsonSchema.position}" mapsTo Json.obj()
            {
                "${jsonSchema.position.x}" mapsTo position.x
                "${jsonSchema.position.y}" mapsTo position.y
            }
            "${jsonSchema.direction}" mapsTo direction.name
            "${jsonSchema.color}" mapsTo Json.obj()
            {
                "${jsonSchema.color.r}" mapsTo color.red
                "${jsonSchema.color.g}" mapsTo color.green
                "${jsonSchema.color.b}" mapsTo color.blue
            }
        }
    }

    private fun JSONObject.toAgent():Agent
    {
        val initialK:Set<Proposition> = getJSONArray("${jsonSchema.initialK}").map {Proposition.makeFrom(it.toString())}.toSet()
        val targetK:Proposition = getString("${jsonSchema.targetK}").let {Proposition.makeFrom(it)}
        val operator:BeliefRevisionStrategy = getJSONObject("${jsonSchema.operator}").toBeliefRevisionStrategy()
        val xPosition:Int = getJSONObject("${jsonSchema.position}").getInt("${jsonSchema.position.x}")
        val yPosition:Int = getJSONObject("${jsonSchema.position}").getInt("${jsonSchema.position.y}")
        val direction:Behaviour.CardinalDirection = getString("${jsonSchema.direction}").let {Behaviour.CardinalDirection.valueOf(it)}
        val color:Color = getJSONObject("${jsonSchema.color}").let()
        {
            val red = it.getDouble("${jsonSchema.color.r}")
            val blue = it.getDouble("${jsonSchema.color.g}")
            val green = it.getDouble("${jsonSchema.color.b}")
            Color.color(red,blue,green)
        }

        return Agent(
            AnnouncementResolutionStrategy.ProblemInstance(initialK,targetK,operator),
            Simulation.Cell.getElseMake(xPosition,yPosition),direction,color)
    }

    private fun JSONObject.toBeliefRevisionStrategy():BeliefRevisionStrategy
    {
        val type = getString("${jsonSchema.operator.name}")
        return when (jsonSchema.operator.name.Value.valueOf(type))
        {
            jsonSchema.operator.name.Value.SATISFIABILITY -> SatisfiabilityBeliefRevisionStrategy()
            jsonSchema.operator.name.Value.HAMMING_DISTANCE -> ComparatorBeliefRevisionStrategy({HammingDistanceComparator(it)})
            jsonSchema.operator.name.Value.WEIGHTED_HAMMING_DISTANCE ->
            {
                val weights = this
                    .getJSONObject("${jsonSchema.operator.weights}")
                    .let {jsonWeights -> jsonWeights.keySet().associate {Variable.fromString(it) to jsonWeights.getInt(it)}}
                ComparatorBeliefRevisionStrategy({WeightedHammingDistanceComparator(it,weights)})
            }
            jsonSchema.operator.name.Value.ORDERED_SETS ->
            {
                val orderedSets = this
                    .getJSONArray("${jsonSchema.operator.sentences}")
                    .map {Proposition.makeFrom(it as String)}
                ComparatorBeliefRevisionStrategy({OrderedSetsComparator(it,orderedSets)})
            }
            else ->
            {
                throw IllegalArgumentException("unknown operator type: \"$type\"")
            }
        }
    }

    private fun BeliefRevisionStrategy.toJsonObject():JSONObject
    {
        val beliefRevisionStrategy = this
        return Json.obj()
        {
            when (beliefRevisionStrategy)
            {
                is SatisfiabilityBeliefRevisionStrategy ->
                {
                    "${jsonSchema.operator.name}" mapsTo jsonSchema.operator.name.Value.SATISFIABILITY.name
                }
                is ComparatorBeliefRevisionStrategy ->
                {
                    val comparator = beliefRevisionStrategy.situationSorterFactory(emptySet())
                    when (comparator)
                    {
                        is HammingDistanceComparator ->
                        {
                            "${jsonSchema.operator.name}" mapsTo jsonSchema.operator.name.Value.HAMMING_DISTANCE.name
                        }
                        is WeightedHammingDistanceComparator ->
                        {
                            "${jsonSchema.operator.name}" mapsTo jsonSchema.operator.name.Value.WEIGHTED_HAMMING_DISTANCE.name
                            "${jsonSchema.operator.weights}" mapsTo Json.obj()
                            {
                                comparator.weights.forEach()
                                {
                                    it.key.friendly mapsTo it.value
                                }
                            }
                        }
                        is OrderedSetsComparator ->
                        {
                            "${jsonSchema.operator.name}" mapsTo jsonSchema.operator.name.Value.ORDERED_SETS.name
                            "${jsonSchema.operator.sentences}" mapsTo comparator.orderedSets
                                .map {it.toParsableString()}
                                .let {JSONArray(it)}
                        }
                    }
                }
            }
        }
    }
}
