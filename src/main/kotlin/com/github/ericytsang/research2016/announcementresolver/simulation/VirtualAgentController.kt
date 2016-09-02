package com.github.ericytsang.research2016.announcementresolver.simulation

import com.github.ericytsang.lib.algorithm.AStar
import com.github.ericytsang.lib.algorithm.angle
import com.github.ericytsang.lib.algorithm.angleDifference
import com.github.ericytsang.lib.collections.getRandom
import com.github.ericytsang.lib.concurrent.future
import com.github.ericytsang.lib.oopatterns.BackedField
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
                is Behaviour.Hide -> Hide()
                is Behaviour.Follow -> Follow(behaviour.agentColor)
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

    private inner class DoNothing:AiState
    {
        override val result:AiState.Status = AiState.Status.SUCCESS
        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun update(simulation:Simulation) = Unit
        override fun hashCode():Int = 0
        override fun equals(other:Any?):Boolean = other is DoNothing
    }

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

    private inner class Hide():AiState
    {
        private val stateMachine = StateMachine<AiState>(PlanRoute())
        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun update(simulation:Simulation)
        {
            stateMachine.stateAccess.withLock()
            {
                stateMachine.value.update(simulation)
                when
                {
                    stateMachine.value is FollowRoute && stateMachine.value.result == AiState.Status.SUCCESS ->
                    {
                        stateMachine.value = DoNothing()
                        result = AiState.Status.SUCCESS
                    }
                }
            }
        }
        override var result:AiState.Status = AiState.Status.PENDING
        override fun hashCode():Int = 0
        override fun equals(other:Any?):Boolean = other is Hide

        inner class PlanRoute():AiState
        {
            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update(simulation:Simulation)
            {
                if (pathPlanningTask.isDone.value)
                {
                    val result = pathPlanningTask.await()
                    val goal = result.parents.maxBy {it.key.adjacentCells.count {it in obstacles}}?.key!!
                    stateMachine.stateAccess.withLock()
                    {
                        stateMachine.value = FollowRoute(result.plotPathTo(goal).map {it.cell})
                    }
                }
            }
            override val result:AiState.Status = AiState.Status.PENDING
            override fun hashCode():Int = 0
            override fun equals(other:Any?):Boolean = false

            val MAX_COST = 100

            val pathPlanningTask = future()
            {
                Thread.currentThread().priority = Thread.MIN_PRIORITY
                val startCell = Simulation.Cell.getElseMake(
                    Math.round(position.x).toInt(),
                    Math.round(position.y).toInt())
                    .let {newAStarNode(it)}
                AStar.run(startCell,MAX_COST)
            }
        }
    }

    private inner class Follow(val agentColor:Color):AiState
    {
        private val stateMachine = StateMachine<AiState>(PickLeader())
        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun update(simulation:Simulation)
        {
            stateMachine.stateAccess.withLock()
            {
                stateMachine.value.update(simulation)
                when
                {
                    stateMachine.value is FollowRoute && stateMachine.value.result == AiState.Status.SUCCESS ->
                    {
                        stateMachine.value = PickLeader()
                    }
                }
            }
        }
        override var result:AiState.Status = AiState.Status.PENDING
        override fun hashCode():Int = 0
        override fun equals(other:Any?):Boolean = other is Hide

        private inner class PickLeader():AiState
        {
            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update(simulation:Simulation)
            {
                val tentativeLeader = simulation.allEntities
                    .filter {it is AgentController && it.bodyColor == agentColor}
                    .map {it as AgentController}
                    .firstOrNull()
                if (tentativeLeader != null)
                {
                    stateMachine.stateAccess.withLock()
                    {
                        stateMachine.value = PlanRoute(tentativeLeader)
                    }
                }
            }
            override val result:AiState.Status = AiState.Status.PENDING
            override fun hashCode():Int = 0
            override fun equals(other:Any?):Boolean = false
        }

        private inner class PlanRoute(var agentController:AgentController):AiState
        {
            val MAX_COST = 100

            val pathPlanningTask = future()
            {
                Thread.currentThread().priority = Thread.MIN_PRIORITY
                val goalCell = Simulation.Cell.getElseMake(
                    Math.round(agentController.position.x).toInt(),
                    Math.round(agentController.position.y).toInt())
                val startCell = Simulation.Cell.getElseMake(
                    Math.round(position.x).toInt(),
                    Math.round(position.y).toInt())
                    .let {newAStarNode(it,goalCell,3)}
                AStar.run(startCell,MAX_COST)
            }

            override fun onEnter() = Unit
            override fun onExit() = Unit
            override fun update(simulation:Simulation)
            {
                if (pathPlanningTask.isDone.value)
                {
                    val result = pathPlanningTask.await()
                    val goal = result.parents
                        .minBy {it.key.estimateRemainingCost()}?.key
                        ?: position
                        .let {Simulation.Cell.getElseMake(Math.round(it.x).toInt(),Math.round(it.y).toInt())}
                        .let {newAStarNode(it,it,0)}
                    stateMachine.stateAccess.withLock()
                    {
                        stateMachine.value = FollowRoute(result.plotPathTo(goal).map {it.cell})
                    }
                }
            }
            override val result:AiState.Status = AiState.Status.PENDING
            override fun hashCode():Int = 0
            override fun equals(other:Any?):Boolean = false
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
        override fun update(simulation:Simulation)
        {
            stateMachine.value.update(simulation)
            stateMachine.stateAccess.withLock()
            {
                when
                {
                    stateMachine.value is FollowRoute && stateMachine.value.result == AiState.Status.SUCCESS ->
                        stateMachine.value = AlignDirection(targetDirection)
                    stateMachine.value is FollowRoute && stateMachine.value.result == AiState.Status.FAIL ->
                        stateMachine.value = PlanRoute()
                    stateMachine.value is AlignDirection && stateMachine.value.result == AiState.Status.SUCCESS ->
                        stateMachine.value = Done()
                }
            }
        }
        override fun hashCode():Int = targetCell.hashCode()+targetDirection.hashCode()
        override fun equals(other:Any?):Boolean
        {
            return other is MoveTo &&
                other.targetCell == targetCell &&
                other.targetDirection == targetDirection
        }

        private inner class PlanRoute():AiState
        {
            val WORK_MULTIPLIER = 50

            val pathPlanningTask = future()
            {
                Thread.currentThread().priority = Thread.MIN_PRIORITY
                val startCell = Simulation.Cell.getElseMake(
                    Math.round(position.x).toInt(),
                    Math.round(position.y).toInt())
                    .let {newAStarNode(it,targetCell,0)}
                val maxCost = position
                    .distance(targetCell.x.toDouble(),targetCell.y.toDouble())
                    .times(WORK_MULTIPLIER)
                    .let {it.toInt()}
                AStar.run(startCell,maxCost)
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
                        val goal = aStarResult.parents.minBy {it.key.estimateRemainingCost()}?.key!!
                        val path = aStarResult.plotPathTo(goal).map {it.cell}
                        stateMachine.value = FollowRoute(path)
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

    private inner class FollowRoute(private var path:List<Simulation.Cell>):AiState
    {
        private var previousDistanceToDestination = Double.MAX_VALUE
        override var result:AiState.Status = AiState.Status.PENDING
        override fun onEnter() = Unit
        override fun onExit() = Unit
        override fun update(simulation:Simulation)
        {
            // if there is no destination to move to, set result to success
            if (path.isEmpty())
            {
                result = AiState.Status.SUCCESS
                return
            }

            // if the path turns out to be obstructed, set result to failure
            if (simulation.cellToEntitiesMap[path.first()]?.any {it is Obstacle} == true)
            {
                result = AiState.Status.FAIL
                return
            }

            // there is a destination to move to...
            // destination position
            val destination = path.first().let {Point2D(it.x.toDouble(),it.y.toDouble())}
            // distance from our position to destination
            val distanceToDestination = position.distance(destination)
            // angle between positive x axis and line from our position to destination
            val destinationDirection = angle(position.x,position.y,destination.x,destination.y)
            // how much the agent should change directions to turn towards the destination this update
            val deltaDirection = if (distanceToDestination < MOVE_THRESHOLD)
            {
                0.0
            }
            else
            {
                angleDifference(direction,destinationDirection).coerceIn(-TURN_MAX_SPEED,TURN_MAX_SPEED)
            }
            // how much the agent should displace to move towards the destination this update
            val deltaPosition = destination.subtract(position).normalize().multiply(Math.min(distanceToDestination,MOVE_MAX_SPEED))

            // if we seem to have deviated from the path, set result to fail
            if (distanceToDestination > previousDistanceToDestination)
            {
                result = AiState.Status.FAIL
                return
            }

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

            // update the distance to the next waypoint in the path
            previousDistanceToDestination = path.firstOrNull()
                ?.let {Point2D(it.x.toDouble(),it.y.toDouble())}
                ?.distance(position) ?: 0.0
        }
        override fun hashCode():Int = 0
        override fun equals(other:Any?):Boolean = false
    }

    private inner class AlignDirection(val targetDirection:Behaviour.CardinalDirection):AiState
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
                result = AiState.Status.SUCCESS
            }
        }
        override fun hashCode():Int = 0
        override fun equals(other:Any?):Boolean = false
    }

    private fun newAStarNode(cell:Simulation.Cell,goal:Simulation.Cell,minDistance:Int):CellToAStarNodeAdapter
    {
        return object:CellToAStarNodeAdapter(cell)
        {
            override val neighbours:Map<CellToAStarNodeAdapter,Int> get()
            {
                return adjacentCells
                    .map {newAStarNode(it,goal,minDistance)}
                    .filter {it.cell !in obstacles}
                    .associate {it to 1}
            }
            override fun estimateRemainingCost():Int
            {
                return Math.abs(cell.x-goal.x)
                    .plus(Math.abs(cell.y-goal.y))
                    .let {Math.max(it-minDistance,0)}
            }
            override fun isSolution():Boolean = estimateRemainingCost() == 0
        }
    }

    private fun newAStarNode(cell:Simulation.Cell):CellToAStarNodeAdapter
    {
        return object:CellToAStarNodeAdapter(cell)
        {
            override val neighbours:Map<CellToAStarNodeAdapter,Int> get()
            {
                return adjacentCells
                    .map {newAStarNode(it)}
                    .filter {it.cell !in obstacles}
                    .associate {it to 1}
            }
            override fun estimateRemainingCost():Int = 0
            override fun isSolution():Boolean = false
        }
    }
}
