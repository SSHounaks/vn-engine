package com.vnengine.harness

import com.vnengine.dsl.SceneBuilder
import com.vnengine.dsl.SceneScript
import com.vnengine.engine.GameVars

/**
 * Builds and validates the scene graph of a [SceneScript]: which scenes exist,
 * their outgoing edges (unconditional `next` and choice targets), dangling
 * targets, scenes unreachable from "start", and auto-advance (`next`) cycles
 * that could loop forever without yielding control to the player.
 */
object SceneGraph {
    enum class Kind { Next, Choice }
    data class Edge(val from: String, val to: String, val kind: Kind)

    data class Graph(
        val scenes: Set<String>,
        val edges: List<Edge>,
        val dangling: Set<String>,
        val unreachable: Set<String>,
        val autoCycles: List<List<String>>
    ) {
        fun isHealthy(): Boolean = dangling.isEmpty() && autoCycles.isEmpty()

        fun outgoing(name: String): Set<String> =
            edges.filter { it.from == name }.map { it.to }.toSet()

        fun describe(): String = buildString {
            if (dangling.isNotEmpty()) appendLine("Dangling targets: $dangling")
            if (unreachable.isNotEmpty()) appendLine("Unreachable scenes: $unreachable")
            if (autoCycles.isNotEmpty()) appendLine("Auto-advance cycles: $autoCycles")
            if (isHealthy()) append("Scene graph healthy (${scenes.size} scenes, ${edges.size} edges)")
        }
    }

    fun analyze(script: SceneScript): Graph {
        val names = script.scenes.keys
        val edges = mutableListOf<Edge>()
        val nextEdges = mutableListOf<Pair<String, String>>()

        script.scenes.forEach { (name, block) ->
            val builder = SceneBuilder()
            builder.vars = GameVars()
            runCatching { block(builder) }
            builder.nextSceneName?.let {
                edges += Edge(name, it, Kind.Next)
                nextEdges += name to it
            }
            builder.optionGroup?.options?.forEach { edges += Edge(name, it.target, Kind.Choice) }
        }

        val dangling = edges.map { it.to }.filter { it !in names }.toSet()
        val reachable = breadthFirst("start", edges)
        val unreachable = names.filter { it !in reachable }.toSet()

        return Graph(names, edges, dangling, unreachable, findAutoCycles(nextEdges))
    }

    private fun breadthFirst(start: String, edges: List<Edge>): Set<String> {
        val adjacency = edges.groupBy { it.from }.mapValues { entry -> entry.value.map { it.to } }
        val seen = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue += start
        seen += start
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            adjacency[node]?.forEach { if (seen.add(it)) queue += it }
        }
        return seen
    }

    private fun findAutoCycles(nextEdges: List<Pair<String, String>>): List<List<String>> {
        val adjacency = nextEdges.groupBy({ it.first }, { it.second })
        val cycles = mutableListOf<List<String>>()
        val colour = mutableMapOf<String, Int>()
        val stack = mutableListOf<String>()

        fun visit(node: String) {
            colour[node] = 1
            stack += node
            adjacency[node]?.forEach { next ->
                when (colour[next]) {
                    1 -> cycles += stack.subList(stack.indexOf(next), stack.size).toList()
                    0, null -> visit(next)
                }
            }
            stack.removeAt(stack.size - 1)
            colour[node] = 2
        }

        adjacency.keys.forEach { if (colour[it] == null) visit(it) }
        return cycles
    }
}
