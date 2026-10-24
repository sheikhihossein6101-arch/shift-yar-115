
package com.example.shiftyar115

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Shift(
    val date: String,
    val start: String,
    val end: String,
    val description: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var showSplash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(3000)
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen()
                } else {
                    ShiftYarApp(this@MainActivity)
                }
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D47A1)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.shiftyar
            ),
            contentDescription = "صفحه ورود شیفت‌یار اورژانس ۱۱۵",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftYarApp(context: Context) {
    val preferences = remember {
        context.getSharedPreferences("shift_yar_data", Context.MODE_PRIVATE)
    }

    var baseName by remember {
        mutableStateOf(preferences.getString("base_name", "") ?: "")
    }
    var date by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showContact by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }

    val shifts = remember {
        mutableStateListOf<Shift>().apply {
            val saved = preferences.getString("shifts", "[]") ?: "[]"
            try {
                val array = JSONArray(saved)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        Shift(
                            item.optString("date"),
                            item.optString("start"),
                            item.optString("end"),
                            item.optString("description")
                        )
                    )
                }
            } catch (_: Exception) {
                // داده ذخیره‌شده قابل خواندن نبود.
            }
        }
    }

    fun saveShifts() {
        val array = JSONArray()
        shifts.forEach { shift ->
            array.put(
                JSONObject().apply {
                    put("date", shift.date)
                    put("start", shift.start)
                    put("end", shift.end)
                    put("description", shift.description)
                }
            )
        }

        preferences.edit()
            .putString("shifts", array.toString())
            .putString("base_name", baseName)
            .apply()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("شیفت‌یار اورژانس ۱۱۵") },
                actions = {
                    TextButton(onClick = { showContact = true }) {
                        Text("درباره ما")
                    }
                    TextButton(onClick = { showExit = true }) {
                        Text("خروج")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "برنامه ثبت و مدیریت شیفت",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = baseName,
                onValueChange = {
                    baseName = it
                    preferences.edit()
                        .putString("base_name", it)
                        .apply()
                },
                label = { Text("نام پایگاه") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("تاریخ را به شمسی وارد کنید؛ مثال: ۱۴۰۵/۰۸/۰۲")

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("تاریخ شیفت (شمسی)") },
                placeholder = { Text("۱۴۰۵/۰۸/۰۲") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = startTime,
                onValueChange = { startTime = it },
                label = { Text("ساعت شروع (مثلاً 07:00)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = endTime,
                onValueChange = { endTime = it },
                label = { Text("ساعت پایان (مثلاً 19:00)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات (اختیاری)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (
                        date.isBlank() ||
                        startTime.isBlank() ||
                        endTime.isBlank()
                    ) {
                        Toast.makeText(
                            context,
                            "لطفاً تاریخ، ساعت شروع و پایان را وارد کنید",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        shifts.add(
                            Shift(
                                date.trim(),
                                startTime.trim(),
                                endTime.trim(),
                                description.trim()
                            )
                        )

                        saveShifts()
                        date = ""
                        startTime = ""
                        endTime = ""
                        description = ""

                        Toast.makeText(
                            context,
                            "شیفت ذخیره شد",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ثبت شیفت")
            }

            Button(
                onClick = {
                    exportPdf(context, baseName, shifts)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تهیه خروجی PDF")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "فهرست شیفت‌ها (${shifts.size})",
                style = MaterialTheme.typography.titleLarge
            )

            if (shifts.isEmpty()) {
                Text("هنوز شیفتی ثبت نشده است.")
            }

            shifts.forEachIndexed { index, shift ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text("پایگاه: ${baseName.ifBlank { "ثبت نشده" }}")
                        Text("تاریخ: ${shift.date}")
                        Text("ساعت: ${shift.start} تا ${shift.end}")

                        if (shift.description.isNotBlank()) {
                            Text("توضیحات: ${shift.description}")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    shifts.removeAt(index)
                                    saveShifts()

                                    Toast.makeText(
                                        context,
                                        "شیفت حذف شد",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Text("حذف شیفت")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "طراحی‌شده برای مدیریت شیفت‌های اورژانس پیش‌بیمارستانی",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showContact) {
        AlertDialog(
            onDismissRequest = { showContact = false },
            title = { Text("درباره سازنده") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("حسین شیخی")
                    Text("کارشناس ارشد پرستاری اورژانس")
                    Text("سمنان")
                    Text("وب‌سایت: www.iranianems.ir")
                    Text("ایتا: eitaa.com/iranianems")
                }
            },
            confirmButton = {
                TextButton(onClick = { showContact = false }) {
                    Text("بستن")
                }
            }
        )
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            title = { Text("خروج از برنامه") },
            text = { Text("آیا می‌خواهید از برنامه خارج شوید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExit = false
                        (context as? ComponentActivity)?.finish()
                    }
                ) {
                    Text("بله، خروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExit = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

fun exportPdf(
    context: Context,
    baseName: String,
    shifts: List<Shift>
) {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 40f

    val paint = Paint().apply {
        textSize = 12f
        isAntiAlias = true
    }

    var pageNumber = 1
    var page = document.startPage(
        PdfDocument.PageInfo.Builder(
            pageWidth,
            pageHeight,
            pageNumber
        ).create()
    )

    var canvas = page.canvas
    var y = 50f

    fun writeLine(text: String, size: Float = 12f) {
        if (y > pageHeight - 50f) {
            document.finishPage(page)
            pageNumber++

            page = document.startPage(
                PdfDocument.PageInfo.Builder(
                    pageWidth,
                    pageHeight,
                    pageNumber
                ).create()
            )

            canvas = page.canvas
            y = 50f
        }

        paint.textSize = size
        canvas.drawText(text, margin, y, paint)
        y += size + 12f
    }

    writeLine("ShiftYar 115 - Shift Schedule", 18f)
    writeLine("Base: ${baseName.ifBlank { "Not specified" }}")
    writeLine(
        "Exported: ${
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.US
            ).format(Date())
        }"
    )

    y += 10f

    if (shifts.isEmpty()) {
        writeLine("No shifts have been registered.")
    } else {
        shifts.forEachIndexed { index, shift ->
            writeLine("${index + 1}. Date: ${shift.date}")
            writeLine("Time: ${shift.start} - ${shift.end}")

            if (shift.description.isNotBlank()) {
                writeLine("Notes: ${shift.description}")
            }

            y += 8f
        }
    }

    document.finishPage(page)

    val fileName = "ShiftYar115_${System.currentTimeMillis()}.pdf"

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS
                )
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw IllegalStateException("Could not create PDF file")

            context.contentResolver.openOutputStream(uri).use { output ->
                if (output == null) {
                    throw IllegalStateException("Could not open PDF")
                }
                document.writeTo(output)
            }
        } else {
            throw IllegalStateException("Android version not supported")
        }

        Toast.makeText(
            context,
            "فایل PDF در پوشه Downloads ذخیره شد",
            Toast.LENGTH_LONG
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "ساخت PDF ناموفق بود: ${
                e.localizedMessage ?: "خطای ناشناخته"
            }",
            Toast.LENGTH_LONG
        ).show()
    } finally {
        document.close()
    }
}
