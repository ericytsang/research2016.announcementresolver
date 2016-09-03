package com.github.ericytsang.research2016.announcementresolver

import java.util.NoSuchElementException

/**
 * Created by surpl on 9/3/2016.
 */

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
