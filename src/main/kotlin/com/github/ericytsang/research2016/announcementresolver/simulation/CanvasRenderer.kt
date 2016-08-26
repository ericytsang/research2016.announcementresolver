package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.oopatterns.BackedField
import com.github.ericytsang.lib.simulation.Renderer
import javafx.application.Platform
import javafx.geometry.Point2D
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
    val cellLength = object:BackedField<Double>(0.0)
    {
        override fun FieldAccess<Double>.setter(proposedValue:Double)
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
        this.cellLength.value = _cellLength
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
                        appendTranslation(cellLength.value*it.position.x,cellLength.value*it.position.y)
                        appendRotation(it.direction)
                    }
                    it.render(context,viewTransform.clone(),cellLength.value)

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
        val position:Point2D
        val renderLayer:Int
        fun render(graphicsContext:GraphicsContext,viewTransform:Affine,cellLength:Double)
    }
}