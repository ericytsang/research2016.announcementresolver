package com.github.ericytsang.research2016.announcementresolver

import org.json.JSONArray
import org.json.JSONObject
import com.github.ericytsang.research2016.propositionallogic.And
import com.github.ericytsang.research2016.propositionallogic.AnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.BeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.BruteForceAnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.ComparatorBeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.HammingDistanceComparator
import com.github.ericytsang.research2016.propositionallogic.OrderedAnnouncementResolutionStrategy
import com.github.ericytsang.research2016.propositionallogic.OrderedSetsComparator
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.SatisfiabilityBeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.Variable
import com.github.ericytsang.research2016.propositionallogic.WeightedHammingDistanceComparator
import com.github.ericytsang.research2016.propositionallogic.contradiction
import com.github.ericytsang.research2016.propositionallogic.findAllAnnouncements
import com.github.ericytsang.research2016.propositionallogic.makeFrom
import com.github.ericytsang.research2016.propositionallogic.models
import com.github.ericytsang.research2016.propositionallogic.toDnf
import com.github.ericytsang.research2016.propositionallogic.toParsableString
import javafx.application.Application
import java.io.InputStreamReader

object GuiLauncher
{
    @JvmStatic
    fun main(args:Array<String>)
    {
        Application.launch(Gui::class.java)
    }
}

object jsonSchema
{
    /**
     * key to the [JSONArray] of propositional sentences from inputted [JSONObject].
     */
    object initialK
    {
        override fun toString() = "initialK"
    }

    /**
     * key to the propositional sentence [String] from inputted [JSONObject].
     */
    object targetK
    {
        override fun toString() = "targetK"
    }

    /**
     * key to the [JSONObject] describing a belief revision operator.
     */
    object operator
    {
        override fun toString() = "operator"

        /**
         * key to the [String] that indicates what type of
         * [BeliefRevisionStrategy] the [operator] is describing.
         */
        val name = "name"
        val satisfiability = "satisfiability"
        val hammingDistance = "hamming distance"
        val weightedHammingDistance = "weighted hamming distance"
        val orderedSets = "ordered sets"
        val weights = "weights"
        val sentences = "sentences"
    }
}

object OrderedAnnouncementResolver
{
    @JvmStatic
    fun main(args:Array<String>)
    {
        val input = InputStreamReader(System.`in`,"UTF-8").readText()

        val problemInstances = JSONArray(input).mapIndexedNotNull()
        {
            index,jsonObject ->
            jsonObject as JSONObject
            return@mapIndexedNotNull try
            {
                jsonObject.toProblemInstance()
            }
            catch (ex:Exception)
            {
                println("parsing error of element at index ${index+1}: ${ex.message}")
                null
            }
        }

        val announcement = OrderedAnnouncementResolutionStrategy().resolve(problemInstances)
        if (announcement != null)
        {
            println("announcement: ${announcement.toDnf().toParsableString()}")
            problemInstances.forEach()
            {
                println("initialK: ${(And.make(it.initialBeliefState) ?: contradiction).models}")
                println("targetK:  ${it.targetBeliefState.models}")
                println("resultK:  ${And.make(it.reviseBy(announcement))!!.models}")
                println()
            }
        }
        else
        {
            println("no solution")
        }
    }
}

object BruteForceAnnouncementResolver
{
    @JvmStatic
    fun main(args:Array<String>)
    {
        val input = InputStreamReader(System.`in`,"UTF-8").readText()

        val problemInstances = JSONArray(input).mapIndexedNotNull()
        {
            index,jsonObject ->
            jsonObject as JSONObject
            return@mapIndexedNotNull try
            {
                jsonObject.toProblemInstance()
            }
            catch (ex:Exception)
            {
                println("parsing error of element at index ${index+1}: ${ex.message}")
                null
            }
        }

        val announcement = BruteForceAnnouncementResolutionStrategy().resolve(problemInstances)
        if (announcement != null)
        {
            println("announcement: ${announcement.toDnf().toParsableString()}")
            problemInstances.forEach()
            {
                println("initialK: ${(And.make(it.initialBeliefState) ?: contradiction).models}")
                println("targetK:  ${it.targetBeliefState.models}")
                println("resultK:  ${And.make(it.reviseBy(announcement))!!.models}")
                println()
            }
        }
        else
        {
            println("no solution")
        }
    }
}

object ExhaustiveAnnouncementResolver
{
    @JvmStatic
    fun main(args:Array<String>)
    {
        val input = InputStreamReader(System.`in`,"UTF-8").readText()

        val problemInstances = JSONArray(input).mapIndexedNotNull()
        {
            index,jsonObject ->
            jsonObject as JSONObject
            return@mapIndexedNotNull try
            {
                jsonObject.toProblemInstance()
            }
            catch (ex:Exception)
            {
                println("parsing error of element at index ${index+1}: ${ex.message}")
                null
            }
        }

        val announcement = findAllAnnouncements(problemInstances)
        println("announcement: ${announcement.map {it.toDnf().toParsableString()}}")
        problemInstances.forEach()
        {
            println("initialK: ${(And.make(it.initialBeliefState) ?: contradiction).models}")
            println("targetK:  ${it.targetBeliefState.models}")
            println()
        }
    }
}

fun JSONObject.toProblemInstance():AnnouncementResolutionStrategy.ProblemInstance
{
//    val allVariables = this
//        .getJSONArray("${jsonSchema.initialK}")
//        .plus(this.getString("${jsonSchema.targetK}"))
//        .map {Proposition.makeFrom(it as String)}
//        .flatMap {it.variables}
//        .map {it or it.not}
//        .let {And.make(it) ?: tautology}

    val initialK:Set<Proposition> = this
        .getJSONArray("${jsonSchema.initialK}")
        .map {Proposition.makeFrom(it as String)}
//        .plus(allVariables)
        .toSet()

    val targetK:Proposition = this
        .getString("${jsonSchema.targetK}")
        .let {Proposition.makeFrom(it)/* and allVariables*/}

    val operator:BeliefRevisionStrategy = this
        .getJSONObject("${jsonSchema.operator}")
        .let()
        {
            operatorJson ->
            val type = operatorJson.getString("${jsonSchema.operator.name}")
            return@let when (type)
            {
                jsonSchema.operator.satisfiability -> SatisfiabilityBeliefRevisionStrategy()
                jsonSchema.operator.hammingDistance -> ComparatorBeliefRevisionStrategy({HammingDistanceComparator(it)})
                jsonSchema.operator.weightedHammingDistance ->
                {
                    val weights = operatorJson
                        .getJSONObject(jsonSchema.operator.weights)
                        .let {jsonWeights -> jsonWeights.keySet().associate {Variable.make(it) to jsonWeights.getInt(it)}}
                    ComparatorBeliefRevisionStrategy({WeightedHammingDistanceComparator(it,weights)})
                }
                jsonSchema.operator.orderedSets ->
                {
                    val orderedSets = operatorJson
                        .getJSONArray(jsonSchema.operator.sentences)
                        .map {Proposition.makeFrom(it as String)}
                    ComparatorBeliefRevisionStrategy({OrderedSetsComparator(it,orderedSets)})
                }
                else ->
                {
                    throw IllegalArgumentException("unknown operator type: \"$type\"")
                }
            }
        }

    return AnnouncementResolutionStrategy.ProblemInstance(initialK,targetK,operator)
}

fun AnnouncementResolutionStrategy.ProblemInstance.toJSONObject():JSONObject
{
    val targetK = targetBeliefState
        .let {it.toParsableString()}

    val initialKs = initialBeliefState
        .map {it.toParsableString()}
        .let {JSONArray(it)}

    val beliefRevisionStrategy = beliefRevisionStrategy
    val operator = when (beliefRevisionStrategy)
    {
        is SatisfiabilityBeliefRevisionStrategy ->
        {
            JSONObject().apply()
            {
                put(jsonSchema.operator.name.toString(),jsonSchema.operator.satisfiability)
            }
        }
        is ComparatorBeliefRevisionStrategy ->
        {
            val comparator = beliefRevisionStrategy.situationSorterFactory(emptySet())
            when (comparator)
            {
                is HammingDistanceComparator ->
                {
                    JSONObject().apply()
                    {
                        put(jsonSchema.operator.name.toString(),jsonSchema.operator.hammingDistance)
                    }
                }
                is WeightedHammingDistanceComparator ->
                {
                    JSONObject().apply()
                    {
                        put(jsonSchema.operator.name.toString(),jsonSchema.operator.weightedHammingDistance)
                        val weightsJson = JSONObject()
                        comparator.weights.forEach()
                        {
                            weightsJson.put(it.key.friendly,it.value)
                        }
                        put(jsonSchema.operator.weights,weightsJson)
                    }
                }
                is OrderedSetsComparator ->
                {
                    JSONObject().apply()
                    {
                        put(jsonSchema.operator.name.toString(),jsonSchema.operator.orderedSets)
                        val orderedSetsJson = JSONArray()
                        comparator.orderedSets.forEach()
                        {
                            orderedSetsJson.put(it.toParsableString())
                        }
                        put(jsonSchema.operator.orderedSets,orderedSetsJson)
                    }
                }
                else -> throw IllegalArgumentException("unknown beliefRevisionStrategy: $beliefRevisionStrategy")
            }
        }
        else -> throw IllegalArgumentException("unknown beliefRevisionStrategy: $beliefRevisionStrategy")
    }

    return JSONObject().apply()
    {
        put("${jsonSchema.initialK}",initialKs)
        put("${jsonSchema.targetK}",targetK)
        put("${jsonSchema.operator}",operator)
    }
}
