package com.github.ericytsang.lib.concurrent

import com.github.ericytsang.lib.oopatterns.BackedField
import com.github.ericytsang.lib.oopatterns.Change
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

fun <R> future(block:()->R) = Future(block)

class Future<out R> internal constructor(val block:()->R)
{
    private var result:()->R = {throw IllegalStateException("uninitialized")}

    private val releasedOnResultSet = CountDownLatch(1)

    val exception:Change.Observable<Exception?> get() = _exception
    private val _exception = BackedField<Exception?>(null)

    val status:Change.Observable<Status> get() = _status
    private val _status = BackedField(Status.PENDING)

    val isDone:Change.Observable<Boolean> get() = _isDone
    private val _isDone = BackedField(false)

    val workerThread = thread()
    {
        try
        {
            result = block().let {{it}}
            releasedOnResultSet.countDown()
            _status.value = Status.SUCCESS
        }
        catch (ex:Exception)
        {
            result = {throw ex}
            releasedOnResultSet.countDown()
            _exception.value = ex
            _status.value = Status.FAILURE
        }
        finally
        {
            _isDone.value = true
        }
    }

    fun await():R
    {
        releasedOnResultSet.await()
        return result()
    }

    enum class Status {PENDING, SUCCESS, FAILURE }
}
