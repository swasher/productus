import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.compose.AsyncImagePainter
import androidx.compose.ui.platform.LocalDensity



//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun FullScreenPhotoScreen(navController: NavController, imageUrl: String) {
//    var scale by remember { mutableFloatStateOf(1f) }
//    val state = rememberTransformableState { zoomChange, _, _ ->
//        scale = (scale * zoomChange).coerceIn(1f, 5f)
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Просмотр фото") },
//                navigationIcon = {
//                    IconButton(onClick = { navController.popBackStack() }) {
//                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(padding)
//                .pointerInput(Unit) { detectTransformGestures { _, _, zoom, _ -> scale *= zoom } }
//        ) {
//            Image(
//                painter = rememberAsyncImagePainter(imageUrl),
//                contentDescription = "Полноразмерное фото",
//                modifier = Modifier
//                    .fillMaxSize()
//                    .graphicsLayer(scaleX = scale, scaleY = scale)
//            )
//        }
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPhotoScreen(navController: NavController, imageUrl: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current.density
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp * density // 📌 Получаем ширину экрана в px
    val screenHeight = configuration.screenHeightDp * density // 📌 Высота экрана в px

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f) // ✅ Ограничиваем зум

        val maxX = (screenWidth * (scale - 1)) / 2 // ✅ Динамическое ограничение по X
        val maxY = (screenHeight * (scale - 1)) / 2 // ✅ Динамическое ограничение по Y

        offsetX = (offsetX + panChange.x).coerceIn(-maxX, maxX)
        offsetY = (offsetY + panChange.y).coerceIn(-maxY, maxY)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Просмотр фото") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .transformable(state) // ✅ Добавляем поддержку жестов зума и перемещения
        ) {

            val painter = rememberAsyncImagePainter(
                model = imageUrl,
                onState = { }
            )

            if (painter.state is AsyncImagePainter.State.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Image(
                painter = painter,
                contentDescription = "Полноразмерное фото",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )
        }
    }
}
