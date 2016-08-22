package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.oopatterns.SimpleBackedField
import com.github.ericytsang.lib.simulation.Renderer
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.transform.Affine
import javafx.scene.transform.Transform
import java.util.concurrent.CountDownLatch

class CanvasRenderer constructor(val canvas:Canvas,_cellLength:Double):Renderer
{
    /**
     * last transform applied to the [GraphicsContext] of [canvas] in while
     * rendering. this controls where in the simulation the user gets to see.
     */
    val viewTransform:Affine = Transform.affine(1.0,0.0,0.0,1.0,0.0,0.0)
    //.apply {appendTranslation(128.0,128.0)}
    //.apply {appendRotation(-10.0)}

    /**
     * length of a cell used when rendering [Entity] objects.
     */
    val cellLength = object:SimpleBackedField<Double>(0.0)
    {
        override fun setter(proposedValue:Double)
        {
            if (proposedValue <= 0.0)
            {
                throw IllegalArgumentException("value ($proposedValue) cannot be <= 0")
            }
            else
            {
                field = proposedValue
            }
        }
    }

    init
    {
        this.cellLength.set(_cellLength)
    }

    override fun render(renderees:Iterable<*>)
    {
        val releasedOnRenderFinished = CountDownLatch(1)
        Platform.runLater()
        {
            val context = canvas.graphicsContext2D

            // apply all transformations that are common to all entities
            context.transform = Transform.affine(1.0,0.0,0.0,1.0,0.0,0.0).apply()
            {
                appendTranslation(canvas.width/2,canvas.height/2)
                append(viewTransform)
            }

            // render every entity
            renderees
                .filter {it is Renderee}
                .map {it as Renderee}
                .sortedBy {it.renderLayer}
                .forEach()
                {
                    context.save()

                    // apply entity specific transformations, then render the entity
                    context.transform = context.transform.apply()
                    {
                        appendTranslation(cellLength.get()*it.position.x,cellLength.get()*it.position.y)
                        appendRotation(it.direction)
                    }
                    it.render(context,viewTransform.clone(),cellLength.get())

                    context.restore()
                }

            // release latch indicating that rendering has finished
            releasedOnRenderFinished.countDown()
        }
        releasedOnRenderFinished.await()
    }

    interface Renderee
    {
        val direction:Double
        val position:Position
        val renderLayer:Int
        fun render(graphicsContext:GraphicsContext,viewTransform:Affine,cellLength:Double)
    }

    /**
     * where the center of the entity is. The boundary between cells lies on
     * integers.
     */
    data class Position(val x:Double,val y:Double)
}