package com.payscope.app.data.repository

import com.payscope.app.data.api.*
import com.payscope.app.data.local.DataStoreManager
import com.payscope.app.domain.model.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphRepository @Inject constructor(private val api: PayScopeApiService, private val store: DataStoreManager) {
    private suspend fun auth() = "Bearer ${store.authTokenFlow.first() ?: error("Please sign in again.")}"
    suspend fun getGraphData(): GraphExplorerData {
        val body = api.getGraphExplorer(auth(), 100).requireBody()
        val nodes = body.nodes.map { n ->
            val d = n.data
            GraphNodeItem(d.id, d.name ?: d.label ?: "Record", d.label ?: d.name.orEmpty(),
                d.nodeType ?: "Record", d.category, d.amount, d.cadence, when (d.nodeType) {
                    "User" -> "#00E0E0"; "Account" -> "#3B82F6"; "Commitment" -> "#FF5500"
                    "Merchant" -> "#A855F7"; "CommitmentInsight" -> "#F59E0B"; else -> "#14B8A6"
                })
        }
        val ids = nodes.map { it.id }.toSet()
        val edges = body.edges.map { e -> GraphEdgeItem(e.data.id, e.data.source, e.data.target,
            e.data.relationshipType.orEmpty(), e.data.label.orEmpty()) }.filter { it.source in ids && it.target in ids }
        return GraphExplorerData(nodes, edges, nodes.size, edges.size, body.source == "neo4j",
            source = body.source ?: "relational", message = body.message)
    }
    suspend fun syncGraph(): Result<Pair<Int, Int>> = try {
        val body = api.syncGraph(auth()).requireBody()
        check(body.status.lowercase() in listOf("completed", "success", "synced")) {
            body.error ?: "Neo4j sync is unavailable. The graph still shows saved database records."
        }
        Result.success(body.nodesSynced to body.relationshipsSynced)
    } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; Result.failure(e) }
}
