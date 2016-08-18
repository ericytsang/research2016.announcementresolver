package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.simulation.Simulation
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.transform.Affine

data class Wall(val position1:Simulation.Cell,val position2:Simulation.Cell):Simulation.Entity<CanvasRenderer.Renderee>,CanvasRenderer.Renderee
{
    companion object
    {
        val COLOR_WALL:Color = Color.WHITE
    }

    init
    {
        // make sure position1 and position1 are adjacent points.
        if (!(position1 isAdjacentTo position2))
        {
            throw IllegalArgumentException("$position1 and $position2 must be adjacent points.")
        }
    }

    override fun update(simulation:Simulation<CanvasRenderer.Renderee>) = Unit

    override val direction:Double get()
    {
        return if (position1.x == position2.x)
        {
            0.0
        }
        else
        {
            90.0
        }
    }

    override val position = CanvasRenderer.Position(
        ((position1.x.toDouble()+position2.x.toDouble())/2)+0.5,
        ((position1.y.toDouble()+position2.y.toDouble())/2)+0.5)

    override val renderLayer = RenderLayer.WALL.value

    override fun render(graphicsContext:GraphicsContext,viewTransform:Affine,cellLength:Double)
    {
        graphicsContext.stroke = COLOR_WALL
        graphicsContext.strokeLine(cellLength*-0.5,0.0,cellLength*0.5,0.0)
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
