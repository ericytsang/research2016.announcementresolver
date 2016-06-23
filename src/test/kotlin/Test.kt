import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import research2016.announcementresolver.toProblemInstance
import research2016.propositionallogic.SimpleAnnouncementResolutionStrategy
import research2016.propositionallogic.toParsableString

/**
 * Created by surpl on 6/23/2016.
 */
class Test
{
    @Test
    fun test1()
    {
        baseTest(
"""
[
    {
        "initialK" : [
            "patrol"
        ],
        "targetK" : "patrol",
        "operator" : {
            "name" : "satisfiability"
        },
    },
    {
        "initialK" : [
            "-breach",
            "breach xor patrol",
        ],
        "targetK" : "-patrol",
        "operator" : {
            "name" : "satisfiability"
        },
    },
]
"""
        )
    }

    fun baseTest(input:String)
    {
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
                println("parsing error at line ${index+1}: ${ex.message}")
                null
            }
        }

        println(SimpleAnnouncementResolutionStrategy().resolve(problemInstances)?.toParsableString() ?: "no solution")
    }
}
