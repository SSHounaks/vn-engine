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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vnengine.dsl.Position
import com.vnengine.dsl.SpriteRef
import com.vnengine.engine.DialogueDisplay
import com.vnengine.engine.GameEngine
import com.vnengine.engine.SpriteVisual
import kotlinx.coroutines.delay

private val SceneBackground = Color(0xFF1A1A2E)
private val PanelBackground = Color(0xFF16213E).copy(alpha = 0.95f)
private val AccentColor = Color(0xFFE94560)
private val NamePlateColor = Color(0xFF0F3460)
private const val FACE_ZOOM = 1.55f

@Composable
fun SceneRenderer(engine: GameEngine) {
    val frame by engine.frame.collectAsState()
    val visuals by engine.visuals.collectAsState()

    var forceCompleteLine by remember(frame.dialogue) { mutableStateOf(false) }
    var typingProgress by remember(frame.dialogue) { mutableStateOf(0 to 0) }
    val finished by engine.finished.collectAsState()

    val finishedNow by rememberUpdatedState(finished)
    val dialogueNow by rememberUpdatedState(frame.dialogue)
    val choicesNow by rememberUpdatedState(frame.choices.size)
    val progressNow by rememberUpdatedState(typingProgress)
    val doForceComplete by rememberUpdatedState({ forceCompleteLine = true })

    LaunchedEffect(Unit) {
        while (true) {
            engine.tick(16L)
            delay(16L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    val activeLine = dialogueNow != null
                    val partiallyTyped = progressNow.first < progressNow.second
                    println("[TAP] finished=$finishedNow activeLine=$activeLine shown=${progressNow.first}/${progressNow.second} choices=$choicesNow force=$forceCompleteLine")
                    if (finishedNow) {
                        println("[TAP] -> restart")
                        engine.restart()
                    } else if (activeLine && partiallyTyped) {
                        println("[TAP] -> force-complete")
                        doForceComplete()
                    } else if (choicesNow == 0) {
                        println("[TAP] -> advance")
                        engine.advance()
                    } else {
                        println("[TAP] -> ignored (choice menu)")
                    }
                }
            }
    ) {
        BackgroundLayer(name = frame.background)

        frame.sprites.forEach { sprite ->
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
                forceComplete = forceCompleteLine,
                onProgress = { shown, total -> typingProgress = shown to total },
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
    forceComplete: Boolean,
    onProgress: (Int, Int) -> Unit,
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
            TypewriterText(
                text = dialogue.text,
                forceComplete = forceComplete,
                onProgress = onProgress,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}

@Composable
private fun TypewriterText(
    text: String,
    forceComplete: Boolean,
    onProgress: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var shown by remember(text) { mutableStateOf(0) }

    LaunchedEffect(text, forceComplete) {
        println("[TYPE] effect start force=$forceComplete shown=$shown len=${text.length}")
        if (forceComplete) {
            shown = text.length
            onProgress(text.length, text.length)
            println("[TYPE] -> forced full ${shown}/${text.length}")
        } else {
            onProgress(shown, text.length)
            while (shown < text.length) {
                delay(55)
                shown++
                onProgress(shown, text.length)
            }
            shown = text.length
            onProgress(text.length, text.length)
            println("[TYPE] -> finished naturally ${shown}/${text.length}")
        }
    }

    Text(
        text = text.substring(0, shown),
        modifier = modifier,
        color = Color.White,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
}

@Composable
private fun SpriteView(sprite: SpriteRef, visual: SpriteVisual) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
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
