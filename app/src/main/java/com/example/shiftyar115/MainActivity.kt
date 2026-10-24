
package com.example.shiftyar115

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ShiftYarApp(applicationContext)
            }
        }
    }
}

data class Shift(
    val date: String,
    val start: String,
    val end: String,
    val description: String
)

@Composable
fun ShiftYarApp(context: Context) {
    val prefs = remember {
        context.getSharedPreferences("shift_yar_data", Context.MODE_PRIVATE)
    }

    var baseName by remember {
        mutableStateOf(prefs.getString("base_name", "") ?: "")
    }
    var date by remember { mutableStateOf(today()) }
    var start by remember { mutableStateOf("07:00") }
    var end by remember { mutableStateOf("19:00") }
    var description by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var showContact by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    var shifts by remember {
        mutableStateOf(loadShifts(prefs))
    }

    fun saveShifts() {
        val array = JSONArray()
        shifts.forEach { shift ->
            array.put(JSONObject().apply {
                put("date", shift.date)
                put("start", shift.start)
                put("end", shift.end)
                put("description", shift.description)
            })
        }
        prefs.edit().putString("shifts", array.toString()).apply()
    }

    if (showContact) {
        AlertDialog(
            onDismissRequest = { showContact = false },
            title = { Text("اطلاعات تماس") },
            text = {
                Text(
                    "حسین شیخی\n" +
                    "ارشد پرستاری اورژانس\n" +
                    "سمنان\n\n" +
                    "وب‌سایت: www.iranianems.ir\n" +
                    "ایتا: eitaa.com/iranianems"
                )
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
            text = { Text("برای خروج، دکمهٔ بازگشت گوشی را بزنید.") },
            confirmButton = {
                TextButton(onClick = { showExit = false }) {
                    Text("متوجه شدم")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("شیفت‌یار اورژانس ۱۱۵") },
                actions = {
                    Box {
                        TextButton(onClick = { menuExpanded = true }) {
                            Text("منو ⋮")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("صفحه اصلی") },
                                onClick = {
                                    menuExpanded = false
                                    message = "صفحه اصلی"
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تماس با ما") },
                                onClick = {
                                    menuExpanded = false
                                    showContact = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("خروج") },
                                onClick = {
                                    menuExpanded = false
                                    showExit = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "مدیریت شیفت‌های پایگاه",
                    fontSize = 21.sp
                )
            }

            item {
                OutlinedTextField(
                    value = baseName,
                    onValueChange = {
                        baseName = it
                        prefs.edit().putString("base_name", it).apply()
                    },
                    label = { Text("نام پایگاه اورژانس") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Text("تاریخ شمسی را وارد کنید؛ نمونه: ۱۴۰۵/۰۸/۰۲")
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("تاریخ شیفت") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("ساعت شروع") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("ساعت پایان") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = {
                        if (baseName.isBlank()) {
                            message = "ابتدا نام پایگاه را وارد کنید."
                        } else if (
                            !date.matches(
                                Regex("""[0-9۰-۹]{4}/[0-9۰-۹]{1,2}/[0-9۰-۹]{1,2}""")
                            ) || !validTime(start) || !validTime(end)
                        ) {
                            message = "تاریخ یا ساعت را بررسی کنید."
                        } else {
                            shifts = shifts + Shift(
                                date, start, end, description
                            )
                            saveShifts()
                            message = "شیفت با موفقیت ثبت شد."
                            description = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ثبت شیفت")
                }
            }

            item {
                Text(message)
                HorizontalDivider()
                Text("شیفت‌های ثبت‌شده: ${shifts.size}", fontSize = 18.sp)
            }

            items(shifts.indices.toList().reversed()) { index ->
                val shift = shifts[index]
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("پایگاه: $baseName")
                        Text("تاریخ: ${shift.date}")
                        Text("ساعت: ${shift.start} تا ${shift.end}")
                        if (shift.description.isNotBlank()) {
                            Text("توضیحات: ${shift.description}")
                        }
                        TextButton(
                            onClick = {
                                shifts = shifts.toMutableList().also {
                                    it.removeAt(index)
                                }
                                saveShifts()
                                message = "شیفت حذف شد."
                            }
                        ) {
                            Text("حذف شیفت")
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        message = "اطلاعات شیفت‌ها در حافظهٔ همین گوشی ذخیره می‌شود."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("وضعیت ذخیره‌سازی")
                }
            }
        }
    }
}

fun today(): String {
    val calendar = java.util.Calendar.getInstance()
    val year = calendar.get(java.util.Calendar.YEAR)
    val month = calendar.get(java.util.Calendar.MONTH) + 1
    val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
    return "$year/${month.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}"
}

fun validTime(value: String): Boolean {
    val normalized = value
        .replace('۰', '0').replace('۱', '1')
        .replace('۲', '2').replace('۳', '3')
        .replace('۴', '4').replace('۵', '5')
        .replace('۶', '6').replace('۷', '7')
        .replace('۸', '8').replace('۹', '9')
    if (!normalized.matches(Regex("""\d{2}:\d{2}"""))) return false
    val parts = normalized.split(":")
    return parts[0].toInt() in 0..23 && parts[1].toInt() in 0..59
}

fun loadShifts(
    prefs: android.content.SharedPreferences
): List<Shift> {
    val result = mutableListOf<Shift>()
    val json = prefs.getString("shifts", "[]") ?: "[]"
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            result.add(
                Shift(
                    item.optString("date"),
                    item.optString("start"),
                    item.optString("end"),
                    item.optString("description")
                )
            )
        }
    } catch (_: Exception) {
        // اگر اطلاعات ذخیره‌شده قابل خواندن نباشد، فهرست خالی می‌ماند.
    }
    return result
}

