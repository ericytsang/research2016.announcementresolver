package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.oopatterns.StateMachine
import com.github.ericytsang.lib.oopatterns.get
import com.github.ericytsang.lib.oopatterns.set
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.propositionallogic.And
import com.github.ericytsang.research2016.propositionallogic.BeliefRevisionStrategy
import com.github.ericytsang.research2016.propositionallogic.Proposition
import com.github.ericytsang.research2016.propositionallogic.Variable
import javafx.scene.paint.Color
import kotlin.concurrent.withLock

class VirtualAgentController:AgentController()
{
    override fun connect() = stateMachine.get().connect()
    override fun disconnect() = stateMachine.get().disconnect()
    override fun setBeliefState(beliefState:Set<Proposition>) = stateMachine.get().setBeliefState(beliefState)
    override fun setBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy) = stateMachine.get().setBeliefRevisionStrategy(beliefRevisionStrategy)
    override fun reviseBeliefState(sentence:Proposition) = stateMachine.get().reviseBeliefState(sentence)
    override fun setBehaviourDictionary(dictionary:Map<Variable,Behaviour>) = stateMachine.get().setBehaviourDictionary(dictionary)
    override fun update(simulation:Simulation<CanvasRenderer.Renderee>) = stateMachine.get().update(simulation)

    // todo: use getters and setters instead...and make them invoke stuff on the current state in the state machine
    override var bodyColor:Color = Color.YELLOW
    override var direction:Double = 0.0
    override var position:CanvasRenderer.Position = CanvasRenderer.Position(0.0,0.0)

    private val stateMachine = StateMachine<State>(Disconnected())

    private interface State:StateMachine.BaseState
    {
        fun connect()
        fun disconnect()
        fun setBeliefState(beliefState:Set<Proposition>)
        fun setBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy)
        fun reviseBeliefState(sentence:Proposition)
        fun setBehaviourDictionary(behaviourDictionary:Map<Variable,Behaviour>)
        fun update(simulation:Simulation<CanvasRenderer.Renderee>)
    }

    private inner class Disconnected:State
    {
        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun connect() = stateMachine.stateAccess.withLock {stateMachine.set(Connected())}
        override fun disconnect() = println("already disconnected")
        override fun setBeliefState(beliefState:Set<Proposition>) = throw IllegalStateException("disconnected")
        override fun setBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy) = throw IllegalStateException("disconnected")
        override fun reviseBeliefState(sentence:Proposition) = throw IllegalStateException("disconnected")
        override fun setBehaviourDictionary(behaviourDictionary:Map<Variable,Behaviour>) = throw IllegalStateException("disconnected")
        override fun update(simulation:Simulation<CanvasRenderer.Renderee>) = Unit
    }

    private inner class Connected:State
    {
        private lateinit var beliefState:Set<Proposition>
        private lateinit var beliefRevisionStrategy:BeliefRevisionStrategy
        private lateinit var behaviourDictionary:Map<Variable,Behaviour>

        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun connect() = println("already connect")
        override fun disconnect() = stateMachine.stateAccess.withLock {stateMachine.set(Disconnected())}

        override fun setBeliefState(beliefState:Set<Proposition>)
        {
            this.beliefState = beliefState
        }

        override fun setBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy)
        {
            this.beliefRevisionStrategy = beliefRevisionStrategy
        }

        override fun reviseBeliefState(sentence:Proposition)
        {
            beliefState = beliefRevisionStrategy.revise(beliefState,sentence)
        }

        override fun setBehaviourDictionary(behaviourDictionary:Map<Variable,Behaviour>)
        {
            this.behaviourDictionary = behaviourDictionary
        }

        override fun update(simulation:Simulation<CanvasRenderer.Renderee>)
        {
            val models = And.make(beliefState)?.models ?: emptySet()
            val variables = models.singleOrNull()?.let {it.entries.filter {it.value}.map {it.key}}?.toSet() ?: emptySet()
            val behaviour = variables.mapNotNull {behaviourDictionary[it]}.singleOrNull() ?: return/* println("ambiguous behaviour specified: $variables")*/
            when (behaviour)
            {
                is Behaviour.Wander -> direction++
                is Behaviour.Guard ->
                {
                    val speed = 0.1
                    val threshold = 0.25
                    val deltaX = when
                    {
                        position.x < behaviour.x-threshold ->
                        {
                            speed
                        }
                        position.x > behaviour.x+threshold ->
                        {
                            -speed
                        }
                        else -> 0.0
                    }
                    val deltaY = when
                    {
                        position.y < behaviour.y-threshold ->
                        {
                            speed
                        }
                        position.y > behaviour.y+threshold ->
                        {
                            -speed
                        }
                        else -> 0.0
                    }
                    position = CanvasRenderer.Position(position.x+deltaX,position.y+deltaY)
                }
            }
        }
    }
}
