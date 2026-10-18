package com.vnengine.renderer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnengine.dsl.Position
import com.vnengine.dsl.SpriteRef
import com.vnengine.engine.DialogueController
import com.vnengine.engine.DialogueDisplay
import com.vnengine.engine.GameEngine
import com.vnengine.engine.SpriteVisual
import com.vnengine.live2d.Live2DRuntime
import com.vnengine.live2d.Live2DVisual
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

private val SceneBackground = Color(0xFF1A1A2E)
private val PanelBackground = Color(0xFF16213E).copy(alpha = 0.95f)
private val AccentColor = Color(0xFFE94560)
private val NamePlateColor = Color(0xFF0F3460)
private const val FACE_ZOOM = 1.55f

@Composable
fun SceneRenderer(engine: GameEngine) {
    val frame by engine.frame.collectAsState()
    val visuals by engine.visuals.collectAsState()
    val finished by engine.finished.collectAsState()

    val controller = remember { DialogueController() }
    var shown by remember { mutableStateOf(0) }

    val finishedNow by rememberUpdatedState(finished)
    val frameNow by rememberUpdatedState(frame)
    val controllerNow by rememberUpdatedState(controller)

    LaunchedEffect(frame.dialogue?.text) {
        val text = frame.dialogue?.text
        if (text != null) controller.setLine(text) else controller.setLine("")
        shown = 0
    }

    LaunchedEffect(Unit) {
        val clock = TimeSource.Monotonic
        var last = clock.markNow()
        while (true) {
            delay(16L)
            val now = clock.markNow()
            val delta = (now - last).inWholeMilliseconds.coerceIn(16L, 100L)
            last = now
            engine.tick(delta)
            if (!controller.complete && controller.text.isNotEmpty()) {
                controller.tick(delta)
                if (shown != controller.shown) shown = controller.shown
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    val f = frameNow
                    when {
                        finishedNow -> engine.restart()
                        f.dialogue != null && !controllerNow.complete -> {
                            controllerNow.completeNow()
                            shown = controllerNow.shown
                        }
                        f.choices.isEmpty() -> engine.advance()
                        else -> Unit
                    }
                }
            }
    ) {
        BackgroundLayer(name = frame.background)

        frame.sprites.sortedBy { it.layer }.forEach { sprite ->
            val visual = visuals.sprites.firstOrNull { it.id == sprite.id }
                ?: SpriteVisual(sprite.id, sprite.position)
            SpriteView(sprite, visual)
        }

        if (visuals.transitionAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = visuals.transitionAlpha.coerceIn(0f, 1f)))
            )
        }

        frame.dialogue?.let { dialogue ->
            DialogueBox(
                dialogue = dialogue,
                shown = shown,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp)
                    .fillMaxWidth(0.92f)
            )
        }

        if (frame.dialogue == null && frame.choices.isNotEmpty()) {
            ChoiceMenu(
                choices = frame.choices,
                onPick = { index -> engine.choose(index) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .fillMaxWidth(0.6f)
            )
        }

        if (visuals.flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(visuals.flashColor).copy(alpha = visuals.flashAlpha.coerceIn(0f, 1f)))
            )
        }

        if (finished) {
            EndScreen()
        }
    }
}

@Composable
private fun BackgroundLayer(name: String?) {
    val bitmap = name?.let { SpriteResources.loadBackground(it) }
    if (bitmap != null) {
        Image(
            painter = BitmapPainter(bitmap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundFor(name))
        )
    }
}

@Composable
private fun EndScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "The End",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "click anywhere to play again",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ChoiceMenu(
    choices: List<String>,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        choices.forEachIndexed { index, label ->
            Button(
                onClick = { onPick(index) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PanelBackground,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun DialogueBox(
    dialogue: DialogueDisplay,
    shown: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (dialogue.speaker != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 16.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(NamePlateColor)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = dialogue.speaker,
                    color = AccentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground)
        ) {
            Text(
                text = dialogue.text.take(shown.coerceIn(0, dialogue.text.length)),
                modifier = Modifier.padding(20.dp),
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun SpriteView(sprite: SpriteRef, visual: SpriteVisual) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (sprite.isLive2D) {
            Live2DModelView(sprite, visual.live2d)
            return@BoxWithConstraints
        }
        val bias = sprite.bias ?: when (sprite.position) {
            Position.Left -> -0.62f
            Position.Center -> 0f
            Position.Right -> 0.62f
        }
        val offX = with(LocalDensity.current) { (bias * constraints.maxWidth / 2f).toDp() }
        val bitmap = if (visual.frameSrc != null) {
            SpriteResources.loadFrame(visual.frameSrc)
        } else {
            SpriteResources.loadStaticSprite(sprite.id)
        }

        if (bitmap != null) {
            Image(
                painter = BitmapPainter(bitmap),
                contentDescription = sprite.id,
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = offX)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        val s = FACE_ZOOM * visual.scale
                        alpha = visual.alpha
                        translationX = visual.xOffset * 4f
                        translationY = visual.yOffset * 4f
                        rotationZ = visual.rotation * 20f
                        scaleX = s
                        scaleY = s
                    },
                contentScale = ContentScale.Fit
            )
        } else {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3460))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(sprite.id, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Live2DModelView(sprite: SpriteRef, live: Live2DVisual?) {
    val renderer = live?.let { Live2DRuntime.renderer(it.modelPath) }
    if (renderer == null) {
        Live2DPreview(sprite, live)
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = if (sprite.anchorTop) Alignment.TopCenter else Alignment.Center
    ) {
        val widthPx = constraints.maxWidth.coerceAtLeast(1)
        val heightPx = if (sprite.anchorTop) (constraints.maxHeight.coerceAtLeast(1) * 2) else constraints.maxHeight.coerceAtLeast(1)
        var frame by remember { mutableStateOf<ImageBitmap?>(null) }
        val parameters = live.parameters
        val partOpacities = live.partOpacities
        val modelId = live.modelId

        LaunchedEffect(parameters, partOpacities, widthPx, heightPx) {
            renderer.setParameterValues(parameters)
            renderer.setPartOpacities(partOpacities)
            frame = renderer.render(widthPx, heightPx)
        }

        frame?.let {
            Image(
                bitmap = it,
                contentDescription = modelId,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (sprite.anchorTop) Modifier.fillMaxHeight(0.5f) else Modifier.fillMaxSize()),
                contentScale = ContentScale.Fit,
                alignment = if (sprite.anchorTop) Alignment.TopCenter else Alignment.Center
            )
        }
    }
}

@Composable
private fun Live2DPreview(sprite: SpriteRef, live: Live2DVisual?) {
    val id = live?.modelId ?: sprite.id
    val motion = live?.motion ?: sprite.motion
    val expression = live?.expression ?: sprite.expression
    val params = live?.parameters.orEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .widthIn(max = 360.dp)
                .aspectRatio(0.62f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("LIVE2D", color = AccentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(id, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("motion: ${motion ?: "---"}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text("expression: ${expression ?: "---"}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                when {
                    live == null -> Text("model unavailable", color = AccentColor, fontSize = 12.sp)
                    !live.supported -> Text(
                        "Cubism Core not installed \u2014 parameter preview",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                params.entries.sortedBy { it.key }.take(8).forEach { (name, value) ->
                    ParameterBar(name, value)
                }
            }
        }
    }
}

@Composable
private fun ParameterBar(name: String, value: Double) {
    val fraction = ((value + 30.0) / 60.0).coerceIn(0.0, 1.0).toFloat()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
            Text(formatParameter(value), color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
        }
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF0F3460))) {
            Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().background(AccentColor))
        }
    }
}

private fun formatParameter(value: Double): String {
    val rounded = (value * 10.0).toLong() / 10.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}

private fun backgroundFor(name: String?): Color = when (name) {
    null, "default" -> SceneBackground
    "classroom_day" -> Color(0xFF2E4053)
    "classroom_dusk" -> Color(0xFF6E2C00)
    "night_sky" -> Color(0xFF0B1026)
    else -> {
        val hex = name.removePrefix("#")
        if (hex.length == 6) {
            try {
                Color(hex.toLong(16))
            } catch (_: Exception) {
                SceneBackground
            }
        } else SceneBackground
    }
}
