package com.github.ericytsang.lib.algorithm

import java.util.LinkedList

/**
 * Created by surpl on 8/23/2016.
 */
object AStar
{
    interface Node<This:Node<This>>
    {
        /**
         * each key is a neighbour of this node and the mapped value is the cost
         * of traversing to the neighbour from this node.
         */
        val neighbours:Map<This,Double>

        fun estimateTravelCostTo(other:This):Double
    }

    fun <Node:AStar.Node<Node>> run(start:Node,goal:Node,maxIterations:Int):Result<Node>
    {
        // set of evaluated nodes
        val closedSet = mutableSetOf<Node>()

        // set of discovered and unevaluated nodes
        val openSet = mutableSetOf(start)

        // maps nodes to the node that it can be most efficiently reached from
        val parents = mutableMapOf<Node,Node?>(start to null)

        // maps nodes to the total cost it takes to traverse to it
        val costs = mutableMapOf(start to 0.0)

        // maps nodes to the estimated cost it takes to travel to it from the start plus from it to the goal
        val estimatedCosts = mutableMapOf(start to start.estimateTravelCostTo(goal))

        // keep running the algorithm until we exhaust all possibilities
        for (i in 1..maxIterations)
        {
            // get a reference to the node closest to the goal
            val closestNode = openSet.minBy {estimatedCosts[it]!!}

            // if the closest node is the goal or we have exhausted all
            // possibilities, break out of the loop to return a result
            if (closestNode == goal || closestNode == null)
            {
                break
            }

            // evaluate the node
            closedSet.add(closestNode)
            openSet.remove(closestNode)
            for ((neighbour,neighbourCost) in closestNode.neighbours)
            {
                if (Thread.interrupted()) throw InterruptedException()

                // if neighbour is already evaluated, continue
                if (neighbour in closedSet)
                {
                    continue
                }

                // add it to the open set for later evaluation otherwise
                else
                {
                    openSet.add(neighbour)
                }

                // if travel cost to the neighbour from closestNode is faster
                // than any previously known route, then update the path to
                // travel to neighbour from closestNode.
                val pathCost = costs[closestNode]!!+neighbourCost
                if (pathCost < costs[neighbour] ?: Double.MAX_VALUE)
                {
                    parents[neighbour] = closestNode
                    estimatedCosts[neighbour] = pathCost+neighbour.estimateTravelCostTo(goal)
                    costs[neighbour] = pathCost
                }
            }
        }

        return Result(start,parents,costs,estimatedCosts)
    }

    class Result<Node:AStar.Node<Node>>(val source:Node,val parents:Map<Node,Node?>,val costs:Map<Node,Double>,val estimatedCosts:Map<Node,Double>)
    {
        fun plotPathTo(goal:Node):List<Node>
        {
            if (goal !in parents.keys)
            {
                throw IllegalArgumentException("no known parent node for goal: $goal")
            }

            // create the list that will contain the path
            val result = listOf(goal).let {LinkedList(it)}

            // construct the path by inserting the parent of the first node in
            // the list into the first position of the list until we reach the
            // source node
            while (true)
            {
                val parent = parents[result.first] ?: return result
                result.add(0,parent)
            }
        }
    }
}
