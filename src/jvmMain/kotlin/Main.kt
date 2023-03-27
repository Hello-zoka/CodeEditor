import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import java.io.File
import java.io.PrintWriter

/**
 * Class for working with code text
 */
class CodeTransformer {
    private val keyWords =
        listOf("for", "while", "do", "if", "else", "when", "new", "throw", "var", "val", "fun")
    private val separators = listOf(',', ';', '(', ')', '{', '}')

    private fun checkIsSeparator(letter: Char): Boolean {
        return separators.contains(letter) || letter.isWhitespace()
    }

    /**
     * Highlighting keyWords ands double quotes in text.
     * It can easily be extended to highlight single quotes, comments, etc
     * @param codeText Source code, which should be highlighted
     * @return TransformedText of highlighted code
     */
    fun highlightCode(codeText: AnnotatedString): TransformedText {
        val anString = AnnotatedString.Builder().run {
            append(codeText)
            var prevQuote = -1
            for (currentPosition in codeText.indices) {
                if (codeText[currentPosition] == '"') {
                    prevQuote = if (prevQuote != -1) {
                        addStyle(SpanStyle(color = Color.Yellow), prevQuote, currentPosition + 1)
                        -1
                    } else {
                        currentPosition
                    }
                }
                if (prevQuote != -1) { // we are inside string, no other highlight
                    continue
                }
                if (currentPosition != codeText.length - 1 && !checkIsSeparator(codeText[currentPosition + 1])) {
                    // checking if current symbol is end of word
                    continue
                }
                for (keyWord in keyWords) {
                    val wordBeginPos = currentPosition - keyWord.length + 1
                    if (wordBeginPos < 0 || (wordBeginPos > 0 && !checkIsSeparator(codeText[wordBeginPos - 1]))) {
                        // checking if word fits and there are a separator before it
                        continue
                    }
                    if (codeText.substring(wordBeginPos, currentPosition + 1) == keyWord) {
                        addStyle(SpanStyle(color = Color.Blue), wordBeginPos, currentPosition + 1)
                    }
                }
            }
            toAnnotatedString()
        }
        return TransformedText(anString, OffsetMapping.Identity)
    }
}


@Composable
@Preview
fun App() {
    val outputFile = File("src/script_files/output.txt")
    val errorsFile = File("src/script_files/errors.txt")
    val kotlinScriptFile = File("src/script_files/script.kts")

    val codeTransformer = CodeTransformer()
    var codeText by remember { mutableStateOf("") }

    var output by remember { mutableStateOf(outputFile.readText()) }
    val outputScrollHorizontal = rememberScrollState(0)
    val outputScrollVertical = rememberScrollState(0)
    var errors by remember { mutableStateOf(errorsFile.readText()) }
    val errorsScrollHorizontal = rememberScrollState(0)
    val errorsScrollVertical = rememberScrollState(0)

    var exitCode: Int? by remember { mutableStateOf(null) }
    var runningStatus: String by remember { mutableStateOf("") }
    val process =
        ProcessBuilder("bash", "kotlinc", "-script", kotlinScriptFile.absolutePath)
            .redirectOutput(outputFile)
            .redirectError(errorsFile)
    var scriptThread: Thread? = null
    var scriptProcess: Process? = null


    LaunchedEffect(outputFile) {// updating data from output file
        while (true) {
            val fileContents = outputFile.readText()
            output = fileContents
            delay(100) // Wait for 0.1 second before checking the file again
        }
    }

    LaunchedEffect(errorsFile) { // updating date from error file
        while (true) {
            val fileContents = errorsFile.readText()
            errors = fileContents
            delay(100) // Wait for 0.1 second before checking the file again
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxHeight(0.9.toFloat())) {
            // Code editor column
            Column(modifier = Modifier.fillMaxWidth(0.6.toFloat()).padding(20.dp)) {
                // TopBar with info
                TopAppBar(modifier = Modifier.fillMaxHeight(0.1.toFloat())) {
                    Text("Code:")
                }
                // Code editor
                TextField(
                    value = codeText, onValueChange = { codeText = it }, Modifier
                        .fillMaxSize()
                        .border(BorderStroke(1.dp, Color.Black)),
                    label = {},
                    visualTransformation = { codeTransformer.highlightCode(it) }
                )

            }
            // Right column with Output and Error
            Column(modifier = Modifier.padding(20.dp)) {
                // Output
                Column(modifier = Modifier.fillMaxHeight(0.5.toFloat())) {
                    TopAppBar(modifier = Modifier.fillMaxHeight(0.1.toFloat())) {
                        Text(
                            text = "Output",
                        )
                    }
                    Text(
                        text = output,
                        modifier = Modifier.fillMaxSize()
                            .horizontalScroll(outputScrollHorizontal)
                            .verticalScroll(outputScrollVertical)
                            .border(BorderStroke(1.dp, Color.Black))
                    )
                }
                // Errors
                Column(modifier = Modifier.fillMaxHeight()) {
                    TopAppBar(modifier = Modifier.fillMaxHeight(0.1.toFloat())) {
                        Text(
                            text = "Errors",
                        )
                    }
                    Text(
                        text = errors,
                        modifier = Modifier.fillMaxHeight().fillMaxWidth()
                            .horizontalScroll(errorsScrollHorizontal)
                            .verticalScroll(errorsScrollVertical)
                            .border(BorderStroke(1.dp, Color.Black))
                    )
                }
            }

        }
        // Bottom bar with buttons and info about current running state
        BottomAppBar(modifier = Modifier.fillMaxHeight()) {
            // Buttons
            Row(modifier = Modifier.fillMaxWidth(0.2.toFloat())) {
                // RUN button
                Button(onClick = {
                    scriptThread?.interrupt()
                    scriptProcess?.destroy()
                    scriptThread = Thread {
                        runningStatus = "Running"
                        val writer = PrintWriter(kotlinScriptFile)
                        writer.print(codeText)
                        writer.close()
                        try {
                            scriptProcess = process.start()
                            exitCode = scriptProcess!!.waitFor()
                            runningStatus = "Process finished"
                        } catch (e: InterruptedException) {
                            println("Script was interrupted")
                        }
                    }
                    scriptThread?.start()
                }, content = { Text(text = "RUN") })
                // STOP button
                Button(onClick = {
                    scriptThread?.interrupt()
                    scriptProcess?.destroy()
                    if (runningStatus == "Running") {
                        runningStatus = "Process Interrupted"
                    }
                }, content = { Text(text = "STOP") })
            }
            // Info about script state
            Row(modifier = Modifier.fillMaxSize()) {
                // Running status
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(0.5.toFloat()).fillMaxHeight()
                ) {
                    Text(
                        text = runningStatus
                    )
                }
                // Last return code
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = if (exitCode == null) {
                            ""
                        } else {
                            "Last return code is $exitCode"
                        }
                    )
                }
            }
        }
    }
}

// Main just running out application
fun main() = application {
    Window(title = "RUN", onCloseRequest = ::exitApplication) {
        App()
    }
}
