package com.github.ericytsang.lib.concurrent

import kotlin.concurrent.thread

fun <R> future(block:()->R) = Future(block)

class Future<out R> internal constructor(val block:()->R)
{
    val workerThread = thread()
    {
        try
        {
            result = block().let {{it}}
        }
        catch (ex:Exception)
        {
            result = {throw ex}
        }
    }

    private var result:()->R = {throw IllegalStateException("uninitialized")}

    val isDone:Boolean get()
    {
        return !workerThread.isAlive
    }

    fun await():R
    {
        workerThread.join()
        return result()
    }
}
