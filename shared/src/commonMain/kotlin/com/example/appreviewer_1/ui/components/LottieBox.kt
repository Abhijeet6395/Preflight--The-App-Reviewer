package com.example.appreviewer_1.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import appreviewer1.shared.generated.resources.Res
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Renders a Lottie animation from composeResources/files via Compottie (KMP). */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun LottieBox(
    path: String,
    modifier: Modifier = Modifier,
    iterations: Int = Compottie.IterateForever,
    contentDescription: String? = null,
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(Res.readBytes(path).decodeToString())
    }
    Image(
        painter = rememberLottiePainter(
            composition = composition,
            iterations = iterations,
        ),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
