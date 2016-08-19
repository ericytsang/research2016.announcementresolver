package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.oopatterns.StateMachine
import com.github.ericytsang.lib.oopatterns.get
import com.github.ericytsang.lib.oopatterns.set
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.propositionallogic.*
import javafx.scene.paint.Color
import kotlin.concurrent.withLock

class VirtualAgentController(agentId:Double):AgentController(agentId)
{
    override fun connect() = connectivityStateMachine.get().connect()
    override fun disconnect() = connectivityStateMachine.get().disconnect()
    override fun setBeliefState(beliefState:Set<Proposition>) = connectivityStateMachine.get().setBeliefState(beliefState)
    override fun setBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy) = connectivityStateMachine.get().setBeliefRevisionStrategy(beliefRevisionStrategy)
    override fun reviseBeliefState(sentence:Proposition) = connectivityStateMachine.get().reviseBeliefState(sentence)
    override fun setBehaviourDictionary(behaviourDictionary:List<Pair<Proposition,Behaviour>>) = connectivityStateMachine.get().setBehaviourDictionary(behaviourDictionary)
    override fun update(simulation:Simulation<CanvasRenderer.Renderee>) = connectivityStateMachine.get().update(simulation)

    // todo: use getters and setters instead...and make them invoke stuff on the current state in the state machine
    override var bodyColor:Color = Color.YELLOW
    override var direction:Double = 0.0
    override var position:CanvasRenderer.Position = CanvasRenderer.Position(0.0,0.0)

    private val connectivityStateMachine = StateMachine<State>(Disconnected())

    private interface State:StateMachine.BaseState
    {
        fun connect()
        fun disconnect()
        fun setBeliefState(beliefState:Set<Proposition>)
        fun setBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy)
        fun reviseBeliefState(sentence:Proposition)
        fun setBehaviourDictionary(behaviourDictionary:List<Pair<Proposition,Behaviour>>)
        fun update(simulation:Simulation<CanvasRenderer.Renderee>)
    }

    private inner class Disconnected:State
    {
        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun connect() = connectivityStateMachine.stateAccess.withLock {connectivityStateMachine.set(Connected())}
        override fun disconnect() = println("already disconnected")
        override fun setBeliefState(beliefState:Set<Proposition>) = throw IllegalStateException("disconnected")
        override fun setBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy) = throw IllegalStateException("disconnected")
        override fun reviseBeliefState(sentence:Proposition) = throw IllegalStateException("disconnected")
        override fun setBehaviourDictionary(behaviourDictionary:List<Pair<Proposition,Behaviour>>) = throw IllegalStateException("disconnected")
        override fun update(simulation:Simulation<CanvasRenderer.Renderee>) = Unit
    }

    private inner class Connected:State
    {
        private var beliefState:Set<Proposition> = emptySet()
        private var beliefRevisionStrategy:BeliefRevisionStrategy = ComparatorBeliefRevisionStrategy({HammingDistanceComparator(it)})
        private var behaviourDictionary:List<Pair<Proposition,Behaviour>> = emptyList()

        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun connect() = println("already connect")
        override fun disconnect() = connectivityStateMachine.stateAccess.withLock {connectivityStateMachine.set(Disconnected())}

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

        override fun setBehaviourDictionary(behaviourDictionary:List<Pair<Proposition,Behaviour>>)
        {
            this.behaviourDictionary = behaviourDictionary
        }

        private val aiStateMachine = StateMachine<AiState>(Wander())

        override fun update(simulation:Simulation<CanvasRenderer.Renderee>)
        {
            val beliefState = And.make(beliefState) ?: contradiction
            val behaviour = behaviourDictionary.find {beliefState isSubsetOf it.first}?.second
                ?: return /*println("ambiguous behaviour specified: $beliefState, $behaviourDictionary")*/
            when (behaviour)
            {
                is Behaviour.Wander ->
                {
                    aiStateMachine.stateAccess.withLock()
                    {
                        if (!(aiStateMachine.get() is Wander))
                        {
                            aiStateMachine.set(Wander())
                        }
                    }
                }
                is Behaviour.Guard ->
                {
                    aiStateMachine.stateAccess.withLock()
                    {
                        val destinationDirection = when (behaviour.direction)
                        {
                            Behaviour.CardinalDirection.NORTH -> 270.0
                            Behaviour.CardinalDirection.EAST -> 0.0
                            Behaviour.CardinalDirection.SOUTH -> 90.0
                            Behaviour.CardinalDirection.WEST -> 180.0
                        }

                        val currentState = aiStateMachine.get()
                        if (!(currentState is Guard &&
                            currentState.destinationX == behaviour.x.toDouble() &&
                            currentState.destinationY == behaviour.y.toDouble() &&
                            currentState.destinationDirection == destinationDirection))
                        {
                            aiStateMachine.set(Guard(behaviour.x.toDouble(),behaviour.y.toDouble(),destinationDirection))
                        }
                    }
                }
            }
            aiStateMachine.get().update()

        }

        // todo: make agents avoid obstacles

        private inner class Wander:AiState
        {
            private val TURN_MAX_SPEED:Double = 3.0
            private val TURN_THRESHOLD:Double = 3.0

            private val MOVE_MAX_UPDATE_DURATION:Int = 50
            private val MOVE_SPEED:Double = 0.075

            private val stateMachine = StateMachine<AiState>(Turn())

            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update() = stateMachine.get().update()

            private inner class Turn:AiState
            {
                val destinationDirection:Double = (360.0*Math.random())-180.0+direction

                override fun onEnter() = Unit
                override fun onExit() = Unit

                override fun update()
                {
                    when
                    {
                        direction < destinationDirection-TURN_THRESHOLD ->
                        {
                            direction += Math.min(Math.abs(direction-destinationDirection),TURN_MAX_SPEED)
                        }
                        direction > destinationDirection+TURN_THRESHOLD ->
                        {
                            direction -= Math.min(Math.abs(direction-destinationDirection),TURN_MAX_SPEED)
                        }
                        else ->
                        {
                            stateMachine.stateAccess.withLock()
                            {
                                stateMachine.set(MoveForward())
                            }
                        }
                    }
                }
            }

            private inner class MoveForward:AiState
            {
                var remainingSteps:Int = (MOVE_MAX_UPDATE_DURATION*Math.random()).toInt()

                override fun onEnter() = Unit
                override fun onExit() = Unit

                override fun update()
                {
                    if (--remainingSteps > 0)
                    {
                        val deltaX = Math.cos(Math.toRadians(direction))*MOVE_SPEED
                        val deltaY = Math.sin(Math.toRadians(direction))*MOVE_SPEED
                        position = CanvasRenderer.Position(position.x+deltaX,position.y+deltaY)
                    }
                    else
                    {
                        stateMachine.stateAccess.withLock()
                        {
                            stateMachine.set(Turn())
                        }
                    }
                }
            }
        }

        private inner class Guard(val destinationX:Double,val destinationY:Double,val destinationDirection:Double):AiState
        {
            private val MOVE_MAX_SPEED:Double = 0.15
            private val MOVE_THRESHOLD:Double = 0.01

            private val TURN_MAX_SPEED:Double = 5.0
            private val TURN_THRESHOLD:Double = 0.01

            private val stateMachine = StateMachine<AiState>(AlignPosition())

            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update() = stateMachine.get().update()

            private inner class AlignPosition:AiState
            {
                override fun onEnter() = Unit
                override fun onExit() = Unit

                override fun update()
                {
                    val deltaX = when
                    {
                        position.x < destinationX-MOVE_THRESHOLD ->
                        {
                            Math.min(Math.abs(position.x-destinationX),MOVE_MAX_SPEED)
                        }
                        position.x > destinationX+MOVE_THRESHOLD ->
                        {
                            -Math.min(Math.abs(position.x-destinationX),MOVE_MAX_SPEED)
                        }
                        else -> 0.0
                    }
                    val deltaY = when
                    {
                        position.y < destinationY-MOVE_THRESHOLD ->
                        {
                            Math.min(Math.abs(position.y-destinationY),MOVE_MAX_SPEED)
                        }
                        position.y > destinationY+MOVE_THRESHOLD ->
                        {
                            -Math.min(Math.abs(position.y-destinationY),MOVE_MAX_SPEED)
                        }
                        else -> 0.0
                    }
                    position = CanvasRenderer.Position(position.x+deltaX,position.y+deltaY)
                    if (deltaX == 0.0 && deltaY == 0.0)
                    {
                        stateMachine.stateAccess.withLock()
                        {
                            stateMachine.set(AlignDirection())
                        }
                    }
                }
            }

            private inner class AlignDirection:AiState
            {
                override fun onEnter()
                {
                    direction %= 360.0
                }
                override fun onExit() = Unit

                override fun update()
                {
                    when
                    {
                        direction < (destinationDirection%360.0)-TURN_THRESHOLD ->
                        {
                            direction += Math.min(Math.abs(direction-(destinationDirection%360.0)),TURN_MAX_SPEED)
                        }
                        direction > (destinationDirection%360.0)+TURN_THRESHOLD ->
                        {
                            direction -= Math.min(Math.abs(direction-(destinationDirection%360.0)),TURN_MAX_SPEED)
                        }
                        else ->
                        {
                            stateMachine.stateAccess.withLock()
                            {
                                stateMachine.set(AlignPosition())
                            }
                        }
                    }
                }
            }
        }
    }

    private interface AiState:StateMachine.BaseState
    {
        fun update()
    }
}
