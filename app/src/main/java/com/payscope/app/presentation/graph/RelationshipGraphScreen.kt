package com.payscope.app.presentation.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payscope.app.core.theme.*
import com.payscope.app.domain.model.*
import com.payscope.app.data.repository.GraphRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GraphViewModel @Inject constructor(private val repository: GraphRepository) : ViewModel() {
    val data = MutableStateFlow<GraphExplorerData?>(null)
    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    init { loadGraph() }
    fun loadGraph(sync: Boolean = false) {
        if (loading.value) return
        viewModelScope.launch {
            loading.value = true
            message.value = null
            try {
                if (sync) repository.syncGraph().fold(
                    onSuccess = { message.value = "Synced ${it.first} records and ${it.second} relationships." },
                    onFailure = { message.value = it.localizedMessage })
                data.value = repository.getGraphData()
            } catch (e: Exception) { if (e is CancellationException) throw e; message.value = e.localizedMessage ?: "Could not load relationships." }
            finally { loading.value = false }
        }
    }
}

@Composable
fun RelationshipGraphScreen(viewModel: GraphViewModel = hiltViewModel()) {
    val graph by viewModel.data.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val message by viewModel.message.collectAsState()
    var focusId by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf("All payments") }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val data = graph
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 100.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Payment connections", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                Text(if (data?.isConnected == true) "Neo4j | Live workspace projection" else "From saved workspace records",
                    color = if (data?.isConnected == true) SavingsTeal else TextSecondary, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp))
            }
            IconButton(onClick = { viewModel.loadGraph(true) }, enabled = !loading) {
                Icon(Icons.Default.Sync, "Sync Neo4j", tint = SecondaryCyan)
            }
            IconButton(onClick = { viewModel.loadGraph() }, enabled = !loading) {
                Icon(Icons.Default.Refresh, "Refresh relationships", tint = TextSecondary)
            }
        }
        message?.let { Text(it, color = WarningAmber, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp)) }
        if (loading && data == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SecondaryCyan) }
        } else if (data == null || data.nodes.none { it.nodeType == "Commitment" }) {
            Column(Modifier.padding(24.dp)) {
                Icon(Icons.Default.Hub, null, tint = SecondaryCyan, modifier = Modifier.size(34.dp))
                Text("Your payment map starts here", color = TextPrimary, fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp))
                Text("Import a statement and complete its analysis to explore connections between your payments and their evidence.",
                    color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                TextButton(onClick = { viewModel.loadGraph() }) { Text("Retry", color = SecondaryCyan) }
            }
        } else {
            val focus = data.nodes.find { it.id == focusId }
            val payments = data.nodes.filter { it.nodeType == "Commitment" }
            val categories = listOf("All payments") + payments.mapNotNull { it.category }.distinct().sorted()
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { c ->
                    Text(c, color = if (c == category) DeepBackground else TextSecondary, fontSize = 11.sp,
                        modifier = Modifier.clip(CircleShape).background(if (c == category) SecondaryCyan else ElevatedCardSurface)
                            .clickable { category = c; focusId = null }.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
            val root = focus ?: data.nodes.firstOrNull { it.nodeType == "User" }
            val neighbours = if (focus != null) {
                val ids = data.edges.filter { it.source == focus.id || it.target == focus.id }
                    .flatMap { listOf(it.source, it.target) }.toSet() - focus.id
                data.nodes.filter { it.id in ids }.sortedWith(compareBy({ it.nodeType }, { it.name }))
            } else payments.filter { category == "All payments" || it.category == category }
            val nodes = listOfNotNull(root) + neighbours.filter { it.id != root?.id }
            val visibleIds = nodes.map { it.id }.toSet()
            val edges = data.edges.filter { it.source in visibleIds && it.target in visibleIds }
            LaunchedEffect(focusId, category, data) { zoom = 1f; pan = Offset.Zero }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (focus == null) "${payments.size} payments | Tap to explore" else "${neighbours.size} connected records",
                    color = TextMuted, fontSize = 10.sp)
                if (focus != null) TextButton(onClick = { focusId = null }) { Text("All payments", color = SecondaryCyan, fontSize = 11.sp) }
            }
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(CardSurface).border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))) {
                PaymentMap(nodes, edges, root?.id, zoom, pan,
                    onTransform = { centroid, delta, factor ->
                        val newZoom = (zoom * factor).coerceIn(.65f, 2.5f)
                        pan = centroid - (centroid - pan) * (newZoom / zoom) + delta
                        zoom = newZoom
                    }, onSelect = { focusId = it.id })
                Row(Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(CircleShape).background(ElevatedCardSurface)) {
                    IconButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(.65f) }) { Icon(Icons.Default.Remove, "Zoom out", tint = TextPrimary) }
                    IconButton(onClick = { zoom = 1f; pan = Offset.Zero }) { Icon(Icons.Default.CenterFocusStrong, "Reset view", tint = SecondaryCyan) }
                    IconButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(2.5f) }) { Icon(Icons.Default.Add, "Zoom in", tint = TextPrimary) }
                }
            }
            if (focus != null) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(focus.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2)
                        Text(listOfNotNull(focus.nodeType, focus.category, focus.cadence).joinToString(" / "), color = TextSecondary, fontSize = 10.sp)
                    }
                    IconButton(onClick = { focusId = null }) { Icon(Icons.Default.Close, "Close selection", tint = TextSecondary) }
                }
            }
            Text("Pinch to zoom / Drag to move / Select a record for its connections", color = TextMuted,
                fontSize = 9.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun PaymentMap(nodes: List<GraphNodeItem>, edges: List<GraphEdgeItem>, rootId: String?, zoom: Float, pan: Offset,
    onTransform: (Offset, Offset, Float) -> Unit, onSelect: (GraphNodeItem) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current.density
        val width = constraints.maxWidth.toFloat()
        val cardWidth = (width - 44 * density) / 2
        val cardHeight = 55 * density
        val positions = remember(nodes, width) {
            var i = 0
            nodes.associate { n -> n.id to if (n.id == rootId) Offset(width / 2, 46 * density)
                else Offset(if (i % 2 == 0) width * .26f else width * .74f, (145 + (i++ / 2) * 88) * density) }
        }
        Canvas(Modifier.fillMaxSize()
            .pointerInput(nodes) { detectTransformGestures { c, p, z, _ -> onTransform(c, p, z) } }
            .pointerInput(nodes, zoom, pan, width) {
                detectTapGestures { point ->
                    val local = (point - pan) / zoom
                    nodes.firstOrNull { n -> positions[n.id]?.let {
                        kotlin.math.abs(local.x-it.x) <= cardWidth/2 && kotlin.math.abs(local.y-it.y) <= cardHeight/2
                    } == true }?.let(onSelect)
                }
            }) {
            // A quiet grid gives the pan/zoom surface a sense of space.
            for (x in 0..(size.width / (24*density)).toInt()) for (y in 0..(size.height / (24*density)).toInt())
                drawCircle(BorderSubtle.copy(alpha = .55f), density * .65f, Offset(x*24*density, y*24*density))
            withTransform({ translate(pan.x, pan.y); scale(zoom, zoom, Offset.Zero) }) {
                edges.forEach { e ->
                    val a = positions[e.source] ?: return@forEach
                    val b = positions[e.target] ?: return@forEach
                    val path = Path().apply { moveTo(a.x, a.y); cubicTo(a.x, (a.y+b.y)/2, b.x, (a.y+b.y)/2, b.x, b.y) }
                    drawPath(path, SecondaryCyan.copy(alpha = .23f), style = Stroke(1.2f*density))
                }
                nodes.forEach { n ->
                    val p = positions[n.id] ?: return@forEach
                    val color = Color(android.graphics.Color.parseColor(n.colorHex))
                    val topLeft = Offset(p.x-cardWidth/2, p.y-cardHeight/2)
                    drawRoundRect(CardSurface, topLeft, Size(cardWidth, cardHeight), CornerRadius(12*density))
                    drawRoundRect(color.copy(alpha = .5f), topLeft, Size(cardWidth, cardHeight), CornerRadius(12*density), style = Stroke(density))
                    drawCircle(color, 3*density, Offset(topLeft.x+13*density, topLeft.y+14*density))
                    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 9*density; this.color = android.graphics.Color.parseColor(n.colorHex)
                        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                    }
                    drawContext.canvas.nativeCanvas.drawText(n.nodeType.replace("CommitmentInsight", "Insight").uppercase(), topLeft.x+21*density, topLeft.y+17*density, paint)
                    paint.textSize = 12*density; paint.color = android.graphics.Color.parseColor("#F4F7FB")
                    var label = n.name
                    while (paint.measureText(label) > cardWidth-22*density && label.length > 2) label = label.dropLast(2).trimEnd() + "\u2026"
                    drawContext.canvas.nativeCanvas.drawText(label, topLeft.x+11*density, topLeft.y+38*density, paint)
                }
            }
        }
    }
}
