package com.github.ericytsang.lib.json

import org.json.JSONObject
import java.util.LinkedHashMap

class Json private constructor()
{
    companion object
    {
        fun obj(block:MapBuilder<String,Any>.()->Unit):JSONObject
        {
            val map = MapBuilder<String,Any>()
                .apply(block)
                .build()

            return JSONObject().apply()
            {
                for((key,value) in map)
                {
                    put(key,value)
                }
            }
        }
    }

    abstract class Schema
    {
        override fun toString():String
        {
            return javaClass.simpleName
        }
    }

    class MapBuilder<K,V> internal constructor()
    {
        val map = LinkedHashMap<K,V>()

        infix fun K.mapsTo(any:V):Unit
        {
            map += this to any
        }

        fun build():Map<K,V>
        {
            return map
        }
    }
}
