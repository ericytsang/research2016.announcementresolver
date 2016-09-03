package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.observe.Change
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.propositionallogic.BeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.Proposition
import javafx.geometry.Point2D
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.transform.Affine

abstract class AgentController:Simulation.Entity,CanvasRenderer.Renderee
{
    /**
     * true if the agent controller is connected to the remote agent; false
     * otherwise.
     */
    abstract val isConnected:Change.Observable<Boolean>

    /**
     * shuts down the controller so it no longer tries to control the remote
     * robot. should be called before removing all references to this object.
     */
    abstract fun shutdown()

    /**
     * sets the belief state of the remote agent to the [beliefState].
     */
    abstract fun uploadBeliefState(beliefState:Set<Proposition>)

    /**
     * sets the belief revision strategy of the remote agent to [beliefRevisionStrategy].
     */
    abstract fun uploadBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy)

    /**
     * sends the behaviour dictionary to the remote agent so it knows how to
     * behave based on its belief state.
     */
    abstract fun uploadBehaviourDictionary(behaviourDictionary:List<Pair<Proposition,Behaviour>>)

    /**
     * sends the sentence to the remote agent to revise its belief state by it
     * to change its behaviour.
     */
    abstract fun uploadSentenceForBeliefRevision(sentence:Proposition)

    /**
     * color used to represent this agent in the simulation.
     */
    abstract var bodyColor:Color

    /**
     * the direction the robot in the simulation.
     */
    abstract override var direction:Double

    /**
     * the position of the robot in the simulation.
     */
    abstract override var position:Point2D

    /**
     * the layer on which the agent should be drawn on in the simulation.
     */
    final override val renderLayer:Int = RenderLayer.AGENT.value

    /**
     * renders the agent in the simulation.
     */
    final override fun render(graphicsContext:GraphicsContext,viewTransform:Affine,cellLength:Double)
    {
        graphicsContext.fill = bodyColor
        graphicsContext.fillOval(-cellLength*.25,-cellLength*.25,cellLength*.5,cellLength*.5)
        graphicsContext.lineWidth = 3.0
        graphicsContext.stroke = bodyColor
        graphicsContext.strokeLine(0.0,0.0,cellLength,0.0)
    }
}
