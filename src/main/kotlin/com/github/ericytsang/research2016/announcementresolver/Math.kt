package com.github.ericytsang.research2016.announcementresolver

import java.util.NoSuchElementException

/**
 * Created by surpl on 8/24/2016.
 */
internal fun angleDifference(angle1:Double,angle2:Double):Double
{
    var result = angle2-angle1
    while (result < -180) result += 360
    while (result > 180) result -= 360
    return result
}

internal fun angle(cx:Double,cy:Double,ex:Double,ey:Double):Double
{
    val dy = ey - cy
    val dx = ex - cx
    return Math.toDegrees(Math.atan2(dy,dx))
}

internal fun <T> List<T>.getRandom():T
{
    if (isEmpty()) throw NoSuchElementException("list is empty.")
    return this[(Math.random()*size).toInt()]
}

internal fun <T> Array<T>.getRandom():T
{
    if (isEmpty()) throw NoSuchElementException("list is empty.")
    return this[(Math.random()*size).toInt()]
}
