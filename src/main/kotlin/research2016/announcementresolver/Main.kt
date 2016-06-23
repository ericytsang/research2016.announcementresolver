package research2016.announcementresolver

import org.json.JSONArray
import org.json.JSONObject
import research2016.propositionallogic.And
import research2016.propositionallogic.AnnouncementResolutionStrategy
import research2016.propositionallogic.BeliefRevisionStrategy
import research2016.propositionallogic.ComparatorBeliefRevisionStrategy
import research2016.propositionallogic.HammingDistanceComparator
import research2016.propositionallogic.OrderedSetsComparator
import research2016.propositionallogic.Proposition
import research2016.propositionallogic.SatisfiabilityBeliefRevisionStrategy
import research2016.propositionallogic.SimpleAnnouncementResolutionStrategy
import research2016.propositionallogic.Variable
import research2016.propositionallogic.WeightedHammingDistanceComparator
import research2016.propositionallogic.makeFrom
import research2016.propositionallogic.models
import research2016.propositionallogic.toDnf
import research2016.propositionallogic.toParsableString
import java.io.InputStreamReader

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
        object name
        {
            override fun toString() = "name"
            val satisfiability = "satisfiability"
            val hammindDistance = "hamming distance"
            val weightedHammingDistance = "weighted hamming distance"
            val orderedSets = "ordered sets"
            val weights = "weights"
            val sentences = "sentences"
        }
    }
}

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

    val announcement = SimpleAnnouncementResolutionStrategy().resolve(problemInstances)
    if (announcement != null)
    {
        println("announcement: ${announcement.toDnf().toParsableString()}")
        problemInstances.forEach()
        {
            println("initialK: ${it.initialBeliefState.map {it.toParsableString()}}")
            println("targetK:  ${it.targetBeliefState.models}")
            println("resultK:  ${And.make(it.reviseBy(announcement)).models}")
            println()
        }
    }
    else
    {
        println("no solution")
    }
}

fun JSONObject.toProblemInstance():AnnouncementResolutionStrategy.ProblemInstance
{
    val initialK:Set<Proposition> = this
        .getJSONArray("${jsonSchema.initialK}")
        .map {Proposition.makeFrom(it as String)}
        .toSet()

    val targetK:Proposition = this
        .getString("${jsonSchema.targetK}")
        .let {Proposition.Companion.makeFrom(it)}

    val operator:BeliefRevisionStrategy = this
        .getJSONObject("${jsonSchema.operator}")
        .let()
        {
            operatorJson ->
            val type = operatorJson.getString("${jsonSchema.operator.name}")
            return@let when (type)
            {
                jsonSchema.operator.name.satisfiability -> SatisfiabilityBeliefRevisionStrategy()
                jsonSchema.operator.name.hammindDistance -> ComparatorBeliefRevisionStrategy({HammingDistanceComparator(it)})
                jsonSchema.operator.name.weightedHammingDistance ->
                {
                    val weights = operatorJson
                        .getJSONObject(jsonSchema.operator.name.weights)
                        .let {jsonWeights -> jsonWeights.keySet().associate {Variable.make(it) to jsonWeights.getInt(it)}}
                    ComparatorBeliefRevisionStrategy({WeightedHammingDistanceComparator(it,weights)})
                }
                jsonSchema.operator.name.orderedSets ->
                {
                    val orderedSets = operatorJson
                        .getJSONArray(jsonSchema.operator.name.sentences)
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
