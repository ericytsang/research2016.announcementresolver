package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.simulation.Simulation
import javafx.geometry.Point2D
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.transform.Affine
import java.util.LinkedHashSet

data class Wall(val position1:Simulation.Cell,val position2:Simulation.Cell):Simulation.Entity,CanvasRenderer.Renderee,Obstacle
{
    companion object
    {
        val WALL_FILL:Color = Color.BEIGE
        val WALL_FILL_ALPHA:Double = 0.5
        val WALL_STROKE:Color = Color.BEIGE
        val WALL_STROKE_WIDTH:Double = 3.0
        val WALL_STROKE_ALPHA:Double = 1.0
    }

    /**
     * set of all the cells that this wall occupies.
     */
    private val occupiedCells:Set<Simulation.Cell> = run()
    {
        /*
        we figure out which cells are in the set by:

        1. first positioning our cursor at position1
        2. add the cell at the cursor to the set
        3. move the cursor closer to position 2
        4. repeat from step 2 until the cursor reaches position 2
         */

        val start = Point2D(position1.x.toDouble(),position1.y.toDouble())
        val end = Point2D(position2.x.toDouble(),position2.y.toDouble())
        val offset = Point2D(0.0,0.0)
        val numIterations:Int = Math.ceil(start.distance(end)).toInt()
        val step = Point2D((end.x-start.x)/numIterations,(end.y-start.y)/numIterations)
        val result = LinkedHashSet<Simulation.Cell>()

        var cursor = start
        repeat(numIterations+1)
        {
            val cellAtCursor = Simulation.Cell.getElseMake(
                Math.round(cursor.x+offset.x).toInt(),
                Math.round(cursor.y+offset.y).toInt())
            result.add(cellAtCursor)
            cursor = cursor.add(step)
        }

        result
    }

    override fun update(simulation:Simulation)
    {
        simulation.entityToCellsMap[this] = occupiedCells
    }

    override val direction:Double = 0.0

    override val position = Point2D(0.0,0.0)

    override val renderLayer = RenderLayer.WALL.value

    override fun render(graphicsContext:GraphicsContext,viewTransform:Affine,cellLength:Double)
    {
        graphicsContext.fill = WALL_FILL
        graphicsContext.globalAlpha = WALL_FILL_ALPHA
        for (cell in occupiedCells)
        {
            graphicsContext.fillRect((cell.x-0.5)*cellLength,(cell.y-0.5)*cellLength,cellLength,cellLength)
        }

        graphicsContext.stroke = WALL_STROKE
        graphicsContext.lineWidth = WALL_STROKE_WIDTH
        graphicsContext.globalAlpha = WALL_STROKE_ALPHA
        graphicsContext.strokeLine(
            position1.x.toDouble()*cellLength,position1.y.toDouble()*cellLength,
            position2.x.toDouble()*cellLength,position2.y.toDouble()*cellLength)
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
