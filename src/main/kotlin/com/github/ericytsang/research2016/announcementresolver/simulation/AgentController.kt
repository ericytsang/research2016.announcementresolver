package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.propositionallogic.BeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.Variable
import javafx.geometry.Point2D
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.transform.Affine

abstract class AgentController(val agentId:Double):Simulation.Entity,CanvasRenderer.Renderee
{
    /**
     * try to connect this controller to the remote agent. this could be trying
     * to connect to a physical robot, or connecting to a virtual robot.
     */
    abstract fun connect()

    /**
     * disconnect from the remote agent.
     */
    abstract fun disconnect()

    /**
     * sets the belief state of the remote agent to the [beliefState].
     */
    abstract fun setBeliefState(beliefState:Set<Proposition>)

    /**
     * sets the belief revision strategy of the remote agent to [beliefRevisionStrategy].
     */
    abstract fun setBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy)

    /**
     * sends the behaviour dictionary to the remote agent so it knows how to
     * behave based on its belief state.
     */
    abstract fun setBehaviourDictionary(behaviourDictionary:List<Pair<Proposition,Behaviour>>)

    /**
     * sends information about the location of obstacles to the remote agent so
     * it knows where they are so it can do better path finding.
     */
    abstract fun setObstacles(obstacles:Set<Simulation.Cell>)

    /**
     * sends the sentence to the remote agent to revise its belief state by it
     * changing its behaviour.
     */
    abstract fun reviseBeliefState(sentence:Proposition)

    /**
     * color used to represent this agent in the [CanvasRenderer].
     */
    abstract var bodyColor:Color

    abstract override var direction:Double

    abstract override var position:Point2D

    final override val renderLayer:Int = RenderLayer.AGENT.value

    final override fun render(graphicsContext:GraphicsContext,viewTransform:Affine,cellLength:Double)
    {
        graphicsContext.fill = bodyColor
        graphicsContext.fillOval(-cellLength*.25,-cellLength*.25,cellLength*.5,cellLength*.5)
        graphicsContext.stroke = bodyColor
        graphicsContext.strokeLine(0.0,0.0,cellLength,0.0)
    }
}
