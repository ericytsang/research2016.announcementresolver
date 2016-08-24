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

    fun <Node:AStar.Node<Node>> run(start:Node,goal:Node):List<Node>?
    {
        // set of evaluated nodes
        val closedSet = mutableSetOf<Node>()

        // set of discovered and unevaluated nodes
        val openSet = mutableSetOf(start)

        // maps nodes to the node that it can be most efficiently reached from
        val parents = mutableMapOf<Node,Node>()

        // maps nodes to the total cost it takes to traverse to it
        val costs = mutableMapOf(start to 0.0)

        // maps nodes to the estimated cost it takes to travel to it from the start plus from it to the goal
        val estimatedTotalCosts = mutableMapOf(start to start.estimateTravelCostTo(goal))

        // keep running the algorithm until we exhaust all possibilities
        while (openSet.isNotEmpty())
        {
            // get a reference to the node closest to the goal
            val closestNode = openSet.minBy {estimatedTotalCosts[it]!!}!!

            // if the closest node is the goal, return the path to the goal
            if (closestNode == goal)
            {
                val result = LinkedList<Node>()
                result.add(closestNode)

                while (true)
                {
                    val parent = parents[result.first] ?: return result
                    result.add(0,parent)
                }
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
                    estimatedTotalCosts[neighbour] = pathCost+neighbour.estimateTravelCostTo(goal)
                    costs[neighbour] = pathCost
                }
            }
        }

        return null
    }
}
