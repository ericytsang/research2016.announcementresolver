package com.github.ericytsang.research2016.announcementresolver

/**
 * Created by surpl on 8/19/2016.
 */
class NamedValue<out Value>(val name:String,val value:Value)
{
    override fun toString():String = name
}
