package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.algorithm.AStar
import com.github.ericytsang.lib.concurrent.future
import com.github.ericytsang.lib.oopatterns.StateMachine
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.propositionallogic.*
import javafx.geometry.Point2D
import javafx.scene.paint.Color
import kotlin.concurrent.withLock

// todo: add pathfinding
class VirtualAgentController(agentId:Double):AgentController(agentId)
{
    override fun connect() = connectivityStateMachine.value.connect()
    override fun disconnect() = connectivityStateMachine.value.disconnect()
    override fun setBeliefState(beliefState:Set<Proposition>) = connectivityStateMachine.value.uploadBeliefState(beliefState)
    override fun setBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy) = connectivityStateMachine.value.uploadBeliefRevisionStrategy(beliefRevisionStrategy)
    override fun setBehaviourDictionary(behaviourDictionary:List<Pair<Proposition,Behaviour>>) = connectivityStateMachine.value.uploadBehaviourDictionary(behaviourDictionary)
    override fun setObstacles(obstacles:Set<Simulation.Cell>) = connectivityStateMachine.value.uploadObstacles(obstacles)
    override fun reviseBeliefState(sentence:Proposition) = connectivityStateMachine.value.reviseBeliefState(sentence)
    override fun update(simulation:Simulation) = connectivityStateMachine.value.update(simulation)

    override var bodyColor:Color = Color.YELLOW

    // todo: use getters and setters instead...and make them invoke stuff on the current state in the state machine
    override var direction:Double
        get() = connectivityStateMachine.value.direction
        set(value)
        {
            connectivityStateMachine.value.direction = value
        }
    override var position:Point2D
        get() = connectivityStateMachine.value.position
        set(value)
        {
            connectivityStateMachine.value.position = value
        }

    private val connectivityStateMachine = StateMachine<State>(Disconnected())

    private interface State:StateMachine.BaseState
    {
        fun connect()
        fun disconnect()
        fun uploadBeliefState(beliefState:Set<Proposition>)
        fun uploadBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy)
        fun uploadBehaviourDictionary(behaviourDictionary:List<Pair<Proposition,Behaviour>>)
        fun uploadObstacles(obstacles:Set<Simulation.Cell>)
        fun reviseBeliefState(sentence:Proposition)
        fun update(simulation:Simulation)
        var direction:Double
        var position:Point2D
    }

    private inner class Disconnected:State
    {
        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun connect() = connectivityStateMachine.stateAccess.withLock {connectivityStateMachine.value = Connected()}
        override fun disconnect() = println("already disconnected")
        override fun uploadBeliefState(beliefState:Set<Proposition>) = throw IllegalStateException("disconnected")
        override fun uploadBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy) = throw IllegalStateException("disconnected")
        override fun uploadBehaviourDictionary(behaviourDictionary:List<Pair<Proposition,Behaviour>>) = throw IllegalStateException("disconnected")
        override fun uploadObstacles(obstacles:Set<Simulation.Cell>) = throw IllegalStateException("disconnected")
        override fun reviseBeliefState(sentence:Proposition) = throw IllegalStateException("disconnected")
        override fun update(simulation:Simulation) = Unit
        override var direction:Double
            get() = 0.0
            set(value) = throw IllegalStateException("disconnected")
        override var position:Point2D
            get() = Point2D(0.0,0.0)
            set(value) = throw IllegalStateException("disconnected")
    }

    private inner class Connected:State
    {
        private val MOVE_MAX_SPEED:Double = 0.075
        private val MOVE_THRESHOLD:Double = 0.01

        private val TURN_MAX_SPEED:Double = 5.0
        private val TURN_THRESHOLD:Double = 0.01

        private var beliefState:Set<Proposition> = emptySet()

            set(value)
            {
                field = value
                refreshBehaviour()
            }

        private var beliefRevisionStrategy:BeliefRevisionStrategy = ComparatorBeliefRevisionStrategy({HammingDistanceComparator(it)})
        private var behaviourDictionary:List<Pair<Proposition,Behaviour>> = emptyList()

            set(value)
            {
                field = value
                refreshBehaviour()
            }

        private var obstacles:Set<Simulation.Cell> = emptySet()

            set(value)
            {
                field = value
                refreshBehaviour()
            }

        private val aiStateMachine = StateMachine<AiState>(Wander())

        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun connect() = println("already connect")

        override fun disconnect() = connectivityStateMachine.stateAccess.withLock {connectivityStateMachine.value = Disconnected()}

        override fun uploadBeliefState(beliefState:Set<Proposition>)
        {
            this.beliefState = beliefState
        }

        override fun uploadBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy)
        {
            this.beliefRevisionStrategy = beliefRevisionStrategy
        }

        override fun uploadBehaviourDictionary(behaviourDictionary:List<Pair<Proposition,Behaviour>>)
        {
            this.behaviourDictionary = behaviourDictionary
        }

        override fun uploadObstacles(obstacles:Set<Simulation.Cell>)
        {
            this.obstacles = obstacles
        }

        override var direction:Double = 0.0

        override var position:Point2D = Point2D(0.0,0.0)

        override fun reviseBeliefState(sentence:Proposition)
        {
            beliefState = beliefRevisionStrategy.revise(beliefState,sentence)
        }

        override fun update(simulation:Simulation)
        {
            aiStateMachine.value.update()
        }

        private fun refreshBehaviour()
        {
            val beliefState = And.make(beliefState) ?: contradiction
            val behaviour = behaviourDictionary.find {beliefState isSubsetOf it.first}?.second
                ?: return println("ambiguous behaviour specified: $beliefState, $behaviourDictionary")
            println("behaviour specified: $behaviour, $behaviourDictionary")
            aiStateMachine.stateAccess.withLock()
            {
                aiStateMachine.value = when (behaviour)
                {
                    is Behaviour.Wander -> Wander()
                    is Behaviour.Guard -> Guard(behaviour.x.toDouble(),behaviour.y.toDouble(),behaviour.direction.angle)
                }
            }
        }

        // todo: make agents avoid obstacles

        private inner class Wander:AiState
        {
            private val MOVE_MAX_UPDATE_DURATION:Int = 50

            private val stateMachine = StateMachine<AiState>(Turn())

            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update() = stateMachine.value.update()

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
                                stateMachine.value = MoveForward()
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
                        val deltaX = Math.cos(Math.toRadians(direction))*MOVE_MAX_SPEED
                        val deltaY = Math.sin(Math.toRadians(direction))*MOVE_MAX_SPEED
                        position = Point2D(position.x+deltaX,position.y+deltaY)
                    }
                    else
                    {
                        stateMachine.stateAccess.withLock()
                        {
                            stateMachine.value = Turn()
                        }
                    }
                }
            }
        }

        private inner class Guard(val destinationX:Double,val destinationY:Double,val destinationDirection:Double):AiState
        {
            private val stateMachine = StateMachine<AiState>(PlanRoute())

            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update() = stateMachine.value.update()

            private inner class PlanRoute():AiState
            {
                val route = future()
                {
                    Thread.currentThread().priority = Thread.MIN_PRIORITY
                    val destinationCell = Simulation.Cell.getElseMake(
                        Math.round(destinationX).toInt(),
                        Math.round(destinationY).toInt())
                        .let {CellToAStarNodeAdapter(it)}
                    val startCell = Simulation.Cell.getElseMake(
                        Math.round(position.x).toInt(),
                        Math.round(position.y).toInt())
                        .let {CellToAStarNodeAdapter(it)}
                    AStar.run(startCell,destinationCell)
                }

                override fun onEnter() = Unit
                override fun onExit() = Unit

                // todo: if it is impossible to get to the destination, just get really close to it instead
                override fun update()
                {
                    // if the route planning isn't done yet, come back later
                    if (!route.isDone) return

                    // else it is done. if a path was found, go to the next state
                    val result = route.await()
                    if (result != null)
                    {
                        stateMachine.stateAccess.withLock()
                        {
                            stateMachine.value = FollowRoute(result.map {it.cell})
                        }
                    }

                    // try to find the path again otherwise
                    else
                    {
                        stateMachine.stateAccess.withLock()
                        {
                            stateMachine.value = PlanRoute()
                        }
                    }
                }

                inner class CellToAStarNodeAdapter(val cell:Simulation.Cell):AStar.Node<CellToAStarNodeAdapter>
                {
                    override val neighbours:Map<CellToAStarNodeAdapter,Double> get()
                    {
                        return listOf(
                            CellToAStarNodeAdapter(Simulation.Cell.getElseMake(cell.x+1,cell.y)),
                            CellToAStarNodeAdapter(Simulation.Cell.getElseMake(cell.x-1,cell.y)),
                            CellToAStarNodeAdapter(Simulation.Cell.getElseMake(cell.x,cell.y+1)),
                            CellToAStarNodeAdapter(Simulation.Cell.getElseMake(cell.x,cell.y-1)))
                            .filter {it.cell !in obstacles}
                            .associate {it to 1.0}
                    }

                    override fun estimateTravelCostTo(other:CellToAStarNodeAdapter):Double
                    {
                        return (Math.abs(cell.x-other.cell.x)+Math.abs(cell.y-other.cell.y)).toDouble()
                    }

                    override fun equals(other:Any?):Boolean = other is CellToAStarNodeAdapter && other.cell == cell

                    override fun hashCode():Int = cell.hashCode()

                    override fun toString():String = cell.toString()
                }
            }

            private inner class FollowRoute(path:List<Simulation.Cell>):AiState
            {
                var path:List<Simulation.Cell> = path
                    private set

                override fun onEnter() = Unit
                override fun onExit() = Unit

                override fun update()
                {
                    // todo also make the agent face the direction it's moving too!
                    val destination = path.first()
                        .let {Point2D(it.x.toDouble(),it.y.toDouble())}
                    val remainingDistance = position.distance(destination)
                    val delta = destination.subtract(position).normalize()
                        .multiply(Math.min(remainingDistance,MOVE_MAX_SPEED))
                    position = position.add(delta)

                    if (remainingDistance < MOVE_THRESHOLD)
                    {
                        path = path.drop(1)
                    }

                    if (path.isEmpty())
                    {
                        stateMachine.stateAccess.withLock()
                        {
                            stateMachine.value = AlignDirection()
                        }
                    }
                }
            }

            private inner class AlignDirection():AiState
            {
                override fun onEnter() = Unit
                override fun onExit() = Unit
                override fun update() = Unit //todo
            }
        }
    }

    private interface AiState:StateMachine.BaseState
    {
        fun update()
    }
}
