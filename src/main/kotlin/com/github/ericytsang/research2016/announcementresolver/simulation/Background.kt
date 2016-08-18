package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.simulation.Simulation
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.transform.Affine
import javafx.scene.transform.Transform

/**
 * Created by surpl on 8/16/2016.
 */
class Background:Simulation.Entity<CanvasRenderer.Renderee>,CanvasRenderer.Renderee
{
    companion object
    {
        val COLOR_BACKGROUND:Color = Color.BLACK
        val COLOR_GRID:Color = Color.WHITE
        val ALPHA_GRID:Double = 0.25
        val ALPHA_GRID_AXIS:Double = 0.5
    }

    override fun update(simulation:Simulation<CanvasRenderer.Renderee>) = Unit

    override val direction = 0.0
    override var position = CanvasRenderer.Position(0.0,0.0)
    override val renderLayer = RenderLayer.GROUND.value
    override fun render(graphicsContext:GraphicsContext,viewTransform:Affine,cellLength:Double)
    {
        val canvas = graphicsContext.canvas

        // cover background with background color
        graphicsContext.save()
        graphicsContext.transform = Transform.affine(1.0,0.0,0.0,1.0,0.0,0.0)
        graphicsContext.fill = COLOR_BACKGROUND
        graphicsContext.fillRect(0.0,0.0,canvas.width,canvas.height)
        graphicsContext.restore()

        // draw grid lines...

        // todo: make number of cells to draw customizable
        val left = canvas.width.toInt()/cellLength.toInt()*-cellLength
        val top = canvas.height.toInt()/cellLength.toInt()*-cellLength
        val right = canvas.width.toInt()/cellLength.toInt()*cellLength
        val bottom = canvas.height.toInt()/cellLength.toInt()*cellLength

        graphicsContext.stroke = COLOR_GRID
        graphicsContext.globalAlpha = ALPHA_GRID

        // draw horizontal grid lines
        var y = top
        while (y <= bottom)
        {
            if (y == 0.0)
            {
                graphicsContext.save()
                graphicsContext.globalAlpha = ALPHA_GRID_AXIS
                graphicsContext.strokeLine(left,y,right,y)
                graphicsContext.restore()
            }
            else
            {
                graphicsContext.strokeLine(left,y,right,y)
            }
            y += cellLength
        }

        // draw vertical grid lines
        var x = left
        while (x <= right)
        {
            if (x == 0.0)
            {
                graphicsContext.save()
                graphicsContext.globalAlpha = ALPHA_GRID_AXIS
                graphicsContext.strokeLine(x,top,x,bottom)
                graphicsContext.restore()
            }
            else
            {
                graphicsContext.strokeLine(x,top,x,bottom)
            }
            x += cellLength
        }
    }
}
