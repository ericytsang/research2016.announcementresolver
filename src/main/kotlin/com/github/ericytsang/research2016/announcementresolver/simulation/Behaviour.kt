package com.github.ericytsang.research2016.announcementresolver.simulation

sealed class Behaviour
{
    class Wander():Behaviour()
    {
        override fun toString():String = "${javaClass.simpleName}()"
    }

    class Guard(val x:Int,val y:Int,val direction:CardinalDirection):Behaviour()
    {
        override fun toString():String
        {
            return "Guard(x=$x, y=$y, direction=$direction)"
        }
    }

    enum class CardinalDirection {NORTH, EAST, SOUTH, WEST }
}
