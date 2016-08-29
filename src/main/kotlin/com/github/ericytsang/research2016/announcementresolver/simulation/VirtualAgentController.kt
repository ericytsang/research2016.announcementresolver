package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.algorithm.AStar
import com.github.ericytsang.lib.algorithm.angle
import com.github.ericytsang.lib.algorithm.angleDifference
import com.github.ericytsang.lib.collections.getRandom
import com.github.ericytsang.lib.concurrent.future
import com.github.ericytsang.lib.oopatterns.BackedField
import com.github.ericytsang.lib.oopatterns.Change
import com.github.ericytsang.lib.oopatterns.StateMachine
import com.github.ericytsang.lib.simulation.Simulation
import com.github.ericytsang.research2016.propositionallogic.*
import javafx.geometry.Point2D
import javafx.scene.paint.Color
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class VirtualAgentController:AgentController()
{
    companion object
    {
        private const val MOVE_MAX_SPEED:Double = 0.075
        private const val MOVE_THRESHOLD:Double = 0.1
        private const val TURN_MAX_SPEED:Double = 5.0
        private const val TURN_THRESHOLD:Double = 0.01
    }

    override val isConnected = BackedField(true)

    override var bodyColor:Color = Color.YELLOW

    override var direction:Double = 0.0

    override var position:Point2D = Point2D(0.0,0.0)

    override fun shutdown() = Unit

    override fun uploadBeliefState(beliefState:Set<Proposition>)
    {
        if (this.beliefState != beliefState)
        {
            this.beliefState = beliefState
        }
    }

    override fun uploadBeliefRevisionStrategy(beliefRevisionStrategy:BeliefRevisionStrategy)
    {
        if (this.beliefRevisionStrategy != beliefRevisionStrategy)
        {
            this.beliefRevisionStrategy = beliefRevisionStrategy
        }
    }

    override fun uploadBehaviourDictionary(behaviourDictionary:List<Pair<Proposition,Behaviour>>)
    {
        if (this.behaviourDictionary != behaviourDictionary)
        {
            this.behaviourDictionary = behaviourDictionary
        }
    }

    override fun uploadObstacles(obstacles:Set<Simulation.Cell>)
    {
        if (this.obstacles != obstacles)
        {
            this.obstacles = obstacles
        }
    }

    override fun uploadSentenceForBeliefRevision(sentence:Proposition)
    {
        beliefState = beliefRevisionStrategy.revise(beliefState,sentence)
    }

    override fun update(simulation:Simulation)
    {
        aiStateMachine.value.update(simulation)
    }

    private var beliefState:Set<Proposition> = emptySet()
        set(value)
        {
            field = value
            refreshBehaviour()
        }

    private var behaviourDictionary:List<Pair<Proposition,Behaviour>> = emptyList()
        set(value)
        {
            field = value
            refreshBehaviour()
        }

    private var obstacles:Set<Simulation.Cell> = emptySet()

    private var beliefRevisionStrategy:BeliefRevisionStrategy = ComparatorBeliefRevisionStrategy({HammingDistanceComparator(it)})

    private fun refreshBehaviour()
    {
        val beliefState = And.make(beliefState) ?: contradiction
        val behaviour = behaviourDictionary.find {beliefState isSubsetOf it.first}?.second
        aiStateMachine.stateAccess.withLock()
        {
            val tentativeValue = when (behaviour)
            {
                is Behaviour.DoNothing -> DoNothing()
                is Behaviour.Wander -> Wander()
                is Behaviour.Guard -> MoveTo(Simulation.Cell.getElseMake(behaviour.x,behaviour.y),behaviour.direction)
                is Behaviour.Patrol -> Patrol(behaviour.waypoints)
                null -> DoNothing()
            }
            if (tentativeValue != aiStateMachine.value)
            {
                aiStateMachine.value = tentativeValue
            }
        }
    }

    private val aiStateMachine = StateMachine<AiState>(DoNothing())

    private interface AiState:StateMachine.BaseState
    {
        fun update(simulation:Simulation)
        val result:Status
        override fun hashCode():Int
        override fun equals(other:Any?):Boolean

        enum class Status
        {PENDING,SUCCESS,FAIL}
    }

    // todo: add patrol state

    private inner class DoNothing:AiState
    {
        override val result:AiState.Status = AiState.Status.SUCCESS
        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun update(simulation:Simulation) = Unit
        override fun hashCode():Int = 0
        override fun equals(other:Any?):Boolean = other is DoNothing
    }

    // todo: add parameters to wander so can specify local wander or something

    private inner class Wander:AiState
    {
        private val MAX_MAGNITUDE:Int = 10

        val stateMachine = StateMachine(MoveTo(randomCell(),randomDirection()))

        override val result:AiState.Status = AiState.Status.SUCCESS
        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun update(simulation:Simulation)
        {
            stateMachine.value.update(simulation)

            if (stateMachine.value.result == AiState.Status.SUCCESS)
            {
                stateMachine.stateAccess.withLock()
                {
                    stateMachine.value = MoveTo(randomCell(),randomDirection())
                }
            }
        }
        override fun hashCode():Int = 0
        override fun equals(other:Any?):Boolean = other is Wander

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

    private inner class Patrol(val waypoints:List<Behaviour.Guard>):AiState
    {
        val MILLIS_WAIT_AT_EACH_WAYPOINT:Long = 1000

        val stateMachine = StateMachine<AiState>(Wait(0))

        override val result:AiState.Status = AiState.Status.SUCCESS
        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun update(simulation:Simulation)
        {
            stateMachine.value.update(simulation)

            if (stateMachine.value.result == AiState.Status.SUCCESS)
            {
                stateMachine.stateAccess.withLock()
                {
                    when (stateMachine.value)
                    {
                        is Wait ->
                        {
                            val nextWaypoint = getNextWaypoint()
                            stateMachine.value = MoveTo(
                                Simulation.Cell.getElseMake(nextWaypoint.x,nextWaypoint.y),
                                nextWaypoint.direction)
                        }
                        is MoveTo ->
                        {
                            stateMachine.value = Wait(MILLIS_WAIT_AT_EACH_WAYPOINT)
                        }
                    }
                }
            }
        }
        override fun hashCode():Int = waypoints.hashCode()
        override fun equals(other:Any?):Boolean
        {
            return other is Patrol &&
                other.waypoints == waypoints
        }

        private var waypointIterator = emptyList<Behaviour.Guard>().iterator()

        private fun getNextWaypoint():Behaviour.Guard
        {
            if (!waypointIterator.hasNext())
            {
                waypointIterator = waypoints.iterator()
            }
            return waypointIterator.next()
        }
    }

    private inner class Wait(val millisToWait:Long):AiState
    {
        override var result:AiState.Status = AiState.Status.PENDING
            private set
        override fun onEnter()
        {
            thread()
            {
                Thread.sleep(millisToWait)
                result = AiState.Status.SUCCESS
            }
        }
        override fun onExit() = Unit
        override fun update(simulation:Simulation) = Unit
        override fun hashCode():Int = millisToWait.toInt()
        override fun equals(other:Any?):Boolean
        {
            return other is Wait &&
                other.millisToWait == millisToWait
        }
    }

    /**
     * behaviour that makes the agent move to [targetCell] and face
     * [targetDirection] once it arrives there.
     */
    private inner class MoveTo(val targetCell:Simulation.Cell,val targetDirection:Behaviour.CardinalDirection):AiState
    {
        val stateMachine = StateMachine<AiState>(PlanRoute())

        override var result:AiState.Status = AiState.Status.PENDING
            private set
        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun update(simulation:Simulation) = stateMachine.value.update(simulation)
        override fun hashCode():Int = targetCell.hashCode()+targetDirection.hashCode()
        override fun equals(other:Any?):Boolean
        {
            return other is MoveTo &&
                other.targetCell == targetCell &&
                other.targetDirection == targetDirection
        }

        private inner class PlanRoute():AiState
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

            override var result:AiState.Status = AiState.Status.PENDING
            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update(simulation:Simulation)
            {
                // if planning is done, go to the next state
                if (pathPlanningTask.isDone.value)
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
            override fun hashCode():Int = 0
            override fun equals(other:Any?):Boolean = false
        }

        private inner class FollowRoute(private var path:List<Simulation.Cell>):AiState
        {
            override var result:AiState.Status = AiState.Status.PENDING
            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update(simulation:Simulation)
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

                // if the path turns out to be obstructed, replan the path
                if (simulation.cellToEntitiesMap[path.first()]?.any {it is Obstacle} == true)
                {
                    stateMachine.stateAccess.withLock()
                    {
                        stateMachine.value = PlanRoute()
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
            override fun hashCode():Int = 0
            override fun equals(other:Any?):Boolean = false
        }

        private inner class AlignDirection():AiState
        {
            override var result:AiState.Status = AiState.Status.PENDING
            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update(simulation:Simulation)
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
            override fun hashCode():Int = 0
            override fun equals(other:Any?):Boolean = false
        }

        private inner class Done():AiState
        {
            override var result:AiState.Status = AiState.Status.PENDING
            override fun onEnter()
            {
                this@MoveTo.result = AiState.Status.SUCCESS
            }
            override fun onExit() = Unit
            override fun update(simulation:Simulation) = Unit
            override fun hashCode():Int = 0
            override fun equals(other:Any?):Boolean = false
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
