package com.github.ericytsang.research2016.announcementresolver.simulation

sealed class Behaviour
{
    class Wander():Behaviour()

    class DoNothing():Behaviour()

    class Guard(val x:Int,val y:Int,val direction:CardinalDirection):Behaviour()
    {
        override fun toString():String
        {
            return "Guard(x=$x, y=$y, direction=$direction)"
        }
    }

    class Patrol(val waypoints:List<Guard>):Behaviour()
    {
        init
        {
            if (waypoints.size < 2)
            {
                throw IllegalArgumentException("please specify at least 2 waypoints for this behaviour")
            }
        }

        override fun toString():String
        {
            return "Patrol(waypoints=$waypoints)"
        }
    }

    class Hide():Behaviour()

    override fun toString():String = "${javaClass.simpleName}()"

    enum class CardinalDirection(val friendly:String,val angle:Double)
    {
        NORTH("North",270.0), EAST("East",0.0), SOUTH("South",90.0), WEST("West",180.0)
    }
}
