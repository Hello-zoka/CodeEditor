// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
//import kotlinx.coroutines.DefaultExecutor.delay
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets


//@Composable
//fun LineNumberedTextField(
//    value: String,
//    onValueChange: (String) -> Unit,
//    modifier: Modifier = Modifier,
//    lineNumberWidth: Dp = 32.dp
//) {
//    // Split the text value into lines
//    val lines = value.lines()
//
//    // Create a lazy column with the line numbers
//    LazyColumn(
//        modifier = modifier.fillMaxHeight(0.6.toFloat())
//    ) {
//        items(lines.size) { index ->
//            Box(
//                modifier = Modifier
//                    .heightIn(min = 24.dp)
//                    .widthIn(min = 24.dp)
//                    .padding(end = 8.dp),
//                contentAlignment = Alignment.CenterEnd
//            ) {
//                Text(
//                    text = "${index + 1}",
//                    style = MaterialTheme.typography.body2,
//                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
//                )
//            }
//        }
//    }
//
//    // Create the text field with a modifier to shift the text to the right
//    TextField(
//        value = value,
//        onValueChange = onValueChange,
//        modifier = modifier
//            .fillMaxHeight(0.6.toFloat())
//    )
//}


@Composable
@Preview
fun App() {
    val outputFile = File("output.txt")
    val errorsFile = File("errors.txt")

    var code by remember { mutableStateOf("") }
    var output by remember { mutableStateOf(outputFile.readText()) }
    val output_scroll = rememberScrollState(0)
    val output_scroll_v = rememberScrollState(0)
    var errors by remember { mutableStateOf(errorsFile.readText()) }
    val errors_scroll = rememberScrollState(0)
    val errors_scroll_v = rememberScrollState(0)
    val kotlinScriptFile = File("src/script_files/script.kts")
    val process =
        ProcessBuilder("bash", "kotlinc", "-script", kotlinScriptFile.absolutePath).redirectOutput(
            outputFile
        ).redirectError(errorsFile)
    var cur_proces: Process? by remember { mutableStateOf(null) }
    var exitCode: Int? by remember { mutableStateOf(null) }
    var runningStatus : String? by remember { mutableStateOf(null) }
    var curProcessThread: Thread? = null
    LaunchedEffect(outputFile) {
        while (true) {
            val fileContents = outputFile.readText()
            output = fileContents
            delay(100) // Wait for 0.1 second before checking the file again
        }
    }

    LaunchedEffect(errorsFile) {
        while (true) {
            val fileContents = errorsFile.readText()
            errors = fileContents
            delay(100) // Wait for 0.1 second before checking the file again
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxHeight(0.9.toFloat())) {
            Column(modifier = Modifier.fillMaxWidth(0.6.toFloat()).padding(20.dp)) {
                TopAppBar(modifier = Modifier.fillMaxHeight(0.1.toFloat())) {
                    Text("Code:")
                }
                TextField(
                    value = code, onValueChange = { code = it }, Modifier
                        .fillMaxSize()
                        .border(BorderStroke(1.dp, Color.Black)),
                    label = {}
                )
            }
            Column(modifier = Modifier.padding(20.dp)) {
                Column(modifier = Modifier.fillMaxHeight(0.5.toFloat())) {
                    TopAppBar(modifier = Modifier.fillMaxHeight(0.1.toFloat())) {
                        Text(
                            text = "Output",
                        )
                    }
                    Text(
                        text = output,
                        modifier = Modifier.fillMaxSize()
                            .horizontalScroll(output_scroll).verticalScroll(output_scroll_v)
                            .border(BorderStroke(1.dp, Color.Black))
                    )
                }
                Column(modifier = Modifier.fillMaxHeight()) {
                    TopAppBar(modifier = Modifier.fillMaxHeight(0.1.toFloat())) {
                        Text(
                            text = "Errors",
                        )
                    }
                    Text(
                        text = errors,
                        modifier = Modifier.fillMaxHeight().fillMaxWidth()
                            .horizontalScroll(errors_scroll).verticalScroll(errors_scroll_v)
                            .border(BorderStroke(1.dp, Color.Black))
                    )

                }


            }

        }

        BottomAppBar(modifier = Modifier.fillMaxHeight()) {
            Row {
                Text(text = "Run")

                Button(onClick = {
                    curProcessThread?.interrupt()
                    runningStatus = null
                    cur_proces?.destroy()
                    curProcessThread = Thread {
                        runningStatus = "Running"
                        val writer = PrintWriter(kotlinScriptFile);
                        writer.print(code);
                        writer.close()
                        cur_proces = process.start()
                        exitCode = cur_proces!!.waitFor()
                        println(exitCode)
                        runningStatus = null
                    }
                    curProcessThread?.start()
                }, content = { Text(text = "RUN") })

                Button(onClick = {
                    curProcessThread?.interrupt()
                    cur_proces?.destroy()
                    runningStatus = null
                }, content = { Text(text = "STOP") })
            }
            Text(
                text = runningStatus ?: ""
            )
            Text(
                text = if (exitCode == null) {
                    ""
                } else {
                    "Last return code is " + exitCode
                }
            )
        }

    }

}

fun main() = application {
    Window(title = "RUN", onCloseRequest = ::exitApplication) {
        App()
    }
}
