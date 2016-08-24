package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.algorithm.AStar
import com.github.ericytsang.lib.algorithm.angle
import com.github.ericytsang.lib.algorithm.angleDifference
import com.github.ericytsang.lib.collections.getRandom
import com.github.ericytsang.lib.concurrent.future
import com.github.ericytsang.lib.oopatterns.StateMachine
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.propositionallogic.*
import javafx.geometry.Point2D
import javafx.scene.paint.Color
import kotlin.concurrent.withLock

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
        private val MOVE_THRESHOLD:Double = 0.1

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

        private val aiStateMachine = StateMachine<AiState>(DoNothing())

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
            aiStateMachine.stateAccess.withLock()
            {
                aiStateMachine.value = when (behaviour)
                {
                    is Behaviour.Wander -> Wander()
                    is Behaviour.Guard -> MoveTo(Simulation.Cell.getElseMake(behaviour.x,behaviour.y),behaviour.direction)
                    null -> DoNothing()
                }
            }
        }

        // todo: add patrol state

        private inner class DoNothing:AiState
        {
            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update() = Unit
        }

        // todo: add parameters to wander so can specify local wander or something

        private inner class Wander:AiState
        {
            private val MAX_MAGNITUDE:Int = 10

            val stateMachine = StateMachine(MoveTo(randomCell(),randomDirection()))

            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update()
            {
                stateMachine.value.update()

                if (stateMachine.value.stateMachine.value is MoveTo.Done)
                {
                    stateMachine.stateAccess.withLock()
                    {
                        stateMachine.value = MoveTo(randomCell(),randomDirection())
                    }
                }
            }

            private fun randomCell():Simulation.Cell
            {
                val x = (position.x+(Math.random()-0.5)*MAX_MAGNITUDE).toInt()
                val y = (position.y+(Math.random()-0.5)*MAX_MAGNITUDE).toInt()
                return Simulation.Cell.getElseMake(x,y)
            }

            private fun randomDirection():Behaviour.CardinalDirection
            {
                return Behaviour.CardinalDirection.values().getRandom()
            }
        }

        /**
         * behaviour that makes the agent move to [targetCell] and face
         * [targetDirection] once it arrives there.
         */
        inner class MoveTo(val targetCell:Simulation.Cell,val targetDirection:Behaviour.CardinalDirection):AiState
        {
            val stateMachine = StateMachine<AiState>(PlanRoute())

            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update() = stateMachine.value.update()

            inner class PlanRoute():AiState
            {
                val MIN_WORK = 1000
                val WORK_MULTIPLIER = 50.0

                val destinationAStarCell = targetCell
                    .let {CellToAStarNodeAdapter(it)}

                val pathPlanningTask = future()
                {
                    Thread.currentThread().priority = Thread.MIN_PRIORITY
                    val startCell = Simulation.Cell.getElseMake(
                        Math.round(position.x).toInt(),
                        Math.round(position.y).toInt())
                        .let {CellToAStarNodeAdapter(it)}
                    val maxIterations = (position.distance(targetCell.x.toDouble(),targetCell.y.toDouble())*WORK_MULTIPLIER)+MIN_WORK
                    AStar.run(startCell,destinationAStarCell,maxIterations.toInt())
                }

                override fun onEnter() = Unit
                override fun onExit() = Unit
                override fun update()
                {
                    // if planning is done, go to the next state
                    if (pathPlanningTask.isDone)
                    {
                        stateMachine.stateAccess.withLock()
                        {
                            val aStarResult = pathPlanningTask.await()
                            val goal = aStarResult.parents.minBy {it.key.estimateTravelCostTo(destinationAStarCell)}?.key!!
                            val path = aStarResult.plotPathTo(goal).map {it.cell}
                            stateMachine.value = FollowRoute(path)
                        }
                    }
                }
            }

            inner class FollowRoute(private var path:List<Simulation.Cell>):AiState
            {
                override fun onEnter() = Unit
                override fun onExit() = Unit
                override fun update()
                {
                    // if there is no destination to move to, move to the next state
                    if (path.isEmpty())
                    {
                        stateMachine.stateAccess.withLock()
                        {
                            stateMachine.value = AlignDirection()
                        }
                        return
                    }

                    // there is a destination to move to...
                    // destination position
                    val destination = path.first().let {Point2D(it.x.toDouble(),it.y.toDouble())}
                    // angle between positive x axis and line from our position to destination
                    val destinationDirection = angle(position.x,position.y,destination.x,destination.y)
                    // how much the agent should change directions to turn towards the destination this update
                    val deltaDirection = angleDifference(direction,destinationDirection).coerceIn(-TURN_MAX_SPEED,TURN_MAX_SPEED)
                    // distance from our position to destination
                    val distanceToDestination = position.distance(destination)
                    // how much the agent should displace to move towards the destination this update
                    val deltaPosition = destination.subtract(position).normalize().multiply(Math.min(distanceToDestination,MOVE_MAX_SPEED))

                    // if we're not facing the destination yet, turn towards it
                    if (Math.abs(deltaDirection) > TURN_THRESHOLD)
                    {
                        direction += deltaDirection
                    }

                    // we're facing the destination, move towards it
                    else
                    {
                        position = position.add(deltaPosition)
                    }

                    // if we have arrived at the destination, remove it from the
                    // path so we can begin moving to the next destination in
                    // the next update
                    if (distanceToDestination < MOVE_THRESHOLD)
                    {
                        path = path.drop(1)
                    }
                }
            }

            inner class AlignDirection():AiState
            {
                override fun onEnter() = Unit
                override fun onExit() = Unit
                override fun update()
                {
                    // turn towards the destination this update
                    val deltaDirection = angleDifference(direction,targetDirection.angle).coerceIn(-TURN_MAX_SPEED,TURN_MAX_SPEED)

                    // if we're not facing the destination yet, turn towards it
                    if (Math.abs(deltaDirection) > TURN_THRESHOLD)
                    {
                        direction += deltaDirection
                    }

                    // we're facing the destination, go to the next state
                    else
                    {
                        stateMachine.stateAccess.withLock()
                        {
                            stateMachine.value = Done()
                        }
                    }
                }
            }

            inner class Done():AiState
            {
                override fun onEnter() = Unit
                override fun onExit() = Unit
                override fun update() = Unit
            }
        }

        /**
         * wraps a [Simulation.Cell] object and provides the [AStar.Node]
         * interface so it can be passed to [AStar.run].
         */
        private inner class CellToAStarNodeAdapter(val cell:Simulation.Cell):AStar.Node<CellToAStarNodeAdapter>
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

    private interface AiState:StateMachine.BaseState
    {
        fun update()
    }
}
