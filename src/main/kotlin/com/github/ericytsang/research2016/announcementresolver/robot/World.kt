package com.github.ericytsang.research2016.announcementresolver.robot

/**
 * Created by surpl on 8/15/2016.
 */
data class Wall(val position1:Point,val position2:Point)
{
    init
    {
        // make sure position1 and position1 are adjacent points.
        if (!(position1 isAdjacentTo position2))
        {
            throw IllegalArgumentException("$position1 and $position2 must be adjacent points.")
        }
    }

    override fun hashCode():Int
    {
        return position1.hashCode()+position2.hashCode()
    }

    override fun equals(other:Any?):Boolean
    {
        return other is Wall
            && ((position1 == other.position1 && position2 == other.position2)
            || (position2 == other.position1 && position1 == other.position2))
    }
}

data class Point(val x:Int,val y:Int)

infix fun Point.isAdjacentTo(that:Point):Boolean
{
    return Math.abs(this.x-that.x)+Math.abs(this.y-that.y) == 1
}
