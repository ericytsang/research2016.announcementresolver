package com.github.ericytsang.lib.json

import org.junit.Test

/**
 * Created by surpl on 8/24/2016.
 */
class JsonTest
{
    object jsonSchema
    {
        object operator:Json.Schema()
        {
            object hammingDistance:Json.Schema()
        }
    }

    @Test
    fun jsonObjectTest()
    {
        val json = Json.obj {
            "hey" mapsTo 5
            "yo" mapsTo 5.5
            "goodbye" mapsTo "eyyy"
            "seeya" mapsTo Json.obj {
                "nestedObject" mapsTo "some string"
            }
        }

        println(json.toString(4))
    }

    @Test
    fun jsonSchemaTest()
    {
        assert(jsonSchema.operator.hammingDistance.toString() == "hammingDistance")
    }
}
