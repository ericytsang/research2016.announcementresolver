package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.algorithm.AStar
import com.github.ericytsang.lib.simulation.Simulation

/**
 * wraps a [Simulation.Cell] object and provides the [AStar.Node]
 * interface so it can be passed to [AStar.run].
 */
abstract class CellToAStarNodeAdapter(val cell:Simulation.Cell):AStar.Node<CellToAStarNodeAdapter>
{
    val adjacentCells:Set<Simulation.Cell> get() = setOf(
        Simulation.Cell.getElseMake(cell.x+1,cell.y),
        Simulation.Cell.getElseMake(cell.x-1,cell.y),
        Simulation.Cell.getElseMake(cell.x,cell.y+1),
        Simulation.Cell.getElseMake(cell.x,cell.y-1))

    override fun equals(other:Any?):Boolean = other is CellToAStarNodeAdapter && other.cell == cell

    override fun hashCode():Int = cell.hashCode()

    override fun toString():String = cell.toString()
}
