package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.simulation.Simulation

infix fun Simulation.Cell.isAdjacentTo(that:Simulation.Cell):Boolean
{
    return Math.abs(this.x-that.x)+Math.abs(this.y-that.y) == 1
}
