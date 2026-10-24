
package com.example.shiftyar115

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val Navy = Color(0xFF123A67)
private val Blue = Color(0xFF1769AA)
private val PaleBlue = Color(0xFFEAF3FC)

data class ShiftEntry(
    val date: String,
    val type: String,
    val first: String,
    val second: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Navy,
                    secondary = Blue,
                    background = Color(0xFFF5F8FC),
                    surface = Color.White
                )
            ) {
                var splash by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(3000)
                    splash = false
                }

                if (splash) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.shiftyar),
                            contentDescription = "شیفت‌یار اورژانس ۱۱۵",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                } else {
                    ShiftYarApp(this@MainActivity)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftYarApp(context: Context) {
    val prefs = remember {
        context.getSharedPreferences("shift_yar_data", Context.MODE_PRIVATE)
    }

    val personnel = remember {
        mutableStateListOf<String>().apply {
            val a = JSONArray(prefs.getString("personnel", "[]") ?: "[]")
            for (i in 0 until a.length()) add(a.optString(i))
        }
    }

    val shifts = remember {
        mutableStateListOf<ShiftEntry>().apply {
            try {
                val a = JSONArray(prefs.getString("shifts_v2", "[]") ?: "[]")
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    add(
                        ShiftEntry(
                            o.optString("date"),
                            o.optString("type"),
                            o.optString("first"),
                            o.optString("second")
                        )
                    )
                }
            } catch (_: Exception) {
                // اگر داده‌ای خراب باشد، برنامه همچنان باز می‌شود.
            }
        }
    }

    var baseName by remember {
        mutableStateOf(prefs.getString("base_name", "") ?: "")
    }
    var selectedPage by remember { mutableStateOf("خانه") }
    var drawerOpen by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }
    var showContact by remember { mutableStateOf(false) }

    var personName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var shiftMode by remember { mutableStateOf("۲۴ ساعته") }
    var shiftType by remember { mutableStateOf("۲۴ ساعته") }
    var firstPerson by remember { mutableStateOf("") }
    var secondPerson by remember { mutableStateOf("") }
    var editIndex by remember { mutableIntStateOf(-1) }

    var month by remember { mutableStateOf("۰۸") }
    var year by remember { mutableStateOf("۱۴۰۵") }

    fun savePersonnel() {
        prefs.edit().putString(
            "personnel",
            JSONArray(personnel.toList()).toString()
        ).apply()
    }

    fun saveShifts() {
        val array = JSONArray()
        shifts.forEach {
            array.put(
                JSONObject().apply {
                    put("date", it.date)
                    put("type", it.type)
                    put("first", it.first)
                    put("second", it.second)
                }
            )
        }
        prefs.edit().putString("shifts_v2", array.toString()).apply()
        prefs.edit().putString("base_name", baseName).apply()
    }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy)
                        .padding(20.dp)
                ) {
                    Text(
                        "✚",
                        color = Color.White,
                        style = MaterialTheme.typography.displayMedium
                    )
                    Text(
                        "شیفت‌یار اورژانس ۱۱۵",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "مدیریت شیفت ماهانه",
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(12.dp))

                listOf(
                    "خانه",
                    "مدیریت شیفت‌ها",
                    "مدیریت پرسنل",
                    "تماس با ما",
                    "خروج"
                ).forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item) },
                        selected = selectedPage == item,
                        onClick = {
                            scope.launch { drawerState.close() }
                            when (item) {
                                "خروج" -> showExit = true
                                "تماس با ما" -> showContact = true
                                else -> selectedPage = item
                            }
                        },
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 3.dp
                        )
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            selectedPage,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        TextButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Text("☰", style = MaterialTheme.typography.headlineSmall)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedPage) {
                    "خانه" -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Navy
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "سامانه مدیریت شیفت",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    "اورژانس پیش‌بیمارستانی ۱۱۵",
                                    color = Color.White
                                )
                                OutlinedTextField(
                                    value = baseName,
                                    onValueChange = {
                                        baseName = it
                                        prefs.edit()
                                            .putString("base_name", it)
                                            .apply()
                                    },
                                    label = { Text("نام پایگاه") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color.White,
                                        unfocusedLabelColor = Color.White,
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.LightGray
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SummaryCard(
                                "تعداد پرسنل",
                                personnel.size.toString(),
                                Modifier.weight(1f)
                            )
                            SummaryCard(
                                "تعداد ردیف شیفت",
                                shifts.size.toString(),
                                Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                selectedPage = "مدیریت شیفت‌ها"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ورود به مدیریت شیفت‌ها")
                        }

                        OutlinedButton(
                            onClick = {
                                selectedPage = "مدیریت پرسنل"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("مدیریت پرسنل")
                        }

                        Text(
                            "برنامه‌ریزی ماهانه با دو نفر در هر شیفت",
                            color = Navy,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    "مدیریت پرسنل" -> {
                        Text(
                            "فهرست پرسنل",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = personName,
                            onValueChange = { personName = it },
                            label = { Text("نام و نام خانوادگی") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                val name = personName.trim()
                                if (name.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "نام پرسنل را وارد کنید",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (personnel.any {
                                        it.equals(name, ignoreCase = true)
                                    }) {
                                    Toast.makeText(
                                        context,
                                        "این نام قبلاً ثبت شده است",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    personnel.add(name)
                                    savePersonnel()
                                    personName = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("افزودن پرسنل")
                        }

                        Text("تعداد پرسنل: ${personnel.size}")

                        personnel.forEach { person ->
                            Card(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(person, modifier = Modifier.weight(1f))
                                    TextButton(
                                        onClick = {
                                            personnel.remove(person)
                                            savePersonnel()
                                            Toast.makeText(
                                                context,
                                                "نام از فهرست حذف شد",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    ) {
                                        Text("حذف")
                                    }
                                }
                            }
                        }
                    }

                    "مدیریت شیفت‌ها" -> {
                        Text(
                            "برنامه ماهانه",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = year,
                            onValueChange = { year = it.filter(Char::isDigit) },
                            label = { Text("سال شمسی") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = month,
                            onValueChange = {
                                month = it.filter(Char::isDigit).take(2)
                            },
                            label = { Text("ماه شمسی (۱ تا ۱۲)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        var modeExpanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = modeExpanded,
                            onExpandedChange = { modeExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = shiftMode,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الگوی شیفت") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = modeExpanded
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )

                            ExposedDropdownMenu(
                                expanded = modeExpanded,
                                onDismissRequest = { modeExpanded = false }
                            ) {
                                listOf("۲۴ ساعته", "۱۲ ساعته").forEach {
                                    DropdownMenuItem(
                                        text = { Text(it) },
                                        onClick = {
                                            shiftMode = it
                                            shiftType = if (it == "۲۴ ساعته") {
                                                "۲۴ ساعته"
                                            } else {
                                                "صبح"
                                            }
                                            modeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("تاریخ شمسی (مثلاً ۱۴۰۵/۰۸/۰۲)") },
                            placeholder = { Text("۱۴۰۵/۰۸/۰۲") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (shiftMode == "۱۲ ساعته") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("صبح", "شب").forEach { type ->
                                    FilterChip(
                                        selected = shiftType == type,
                                        onClick = { shiftType = type },
                                        label = { Text("شیفت $type") }
                                    )
                                }
                            }
                        }

                        if (personnel.size < 2) {
                            Text(
                                "ابتدا در بخش مدیریت پرسنل حداقل دو نفر ثبت کنید.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        PersonDropdown(
                            label = "نفر اول",
                            people = personnel,
                            selected = firstPerson,
                            onSelected = { firstPerson = it }
                        )

                        PersonDropdown(
                            label = "نفر دوم",
                            people = personnel,
                            selected = secondPerson,
                            onSelected = { secondPerson = it }
                        )

                        Button(
                            onClick = {
                                val cleanDate = date.trim()
                                val validMonth = month.toIntOrNull() in 1..12
                                val parts = cleanDate.split("/")
                                val validDate = parts.size == 3 &&
                                    parts[0].toIntOrNull() != null &&
                                    parts[1].toIntOrNull() in 1..12 &&
                                    parts[2].toIntOrNull() in 1..31

                                if (!validMonth || year.toIntOrNull() == null) {
                                    Toast.makeText(
                                        context,
                                        "ماه و سال را درست وارد کنید",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (!validDate) {
                                    Toast.makeText(
                                        context,
                                        "تاریخ را به صورت سال/ماه/روز وارد کنید",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (
                                    parts[0].toIntOrNull() != year.toIntOrNull() ||
                                    parts[1].toIntOrNull() != month.toIntOrNull()
                                ) {
                                    Toast.makeText(
                                        context,
                                        "تاریخ باید با ماه و سال انتخاب‌شده مطابقت داشته باشد",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (
                                    firstPerson.isBlank() ||
                                    secondPerson.isBlank()
                                ) {
                                    Toast.makeText(
                                        context,
                                        "هر دو نفر را انتخاب کنید",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (firstPerson == secondPerson) {
                                    Toast.makeText(
                                        context,
                                        "دو نفر باید متفاوت باشند",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val entry = ShiftEntry(
                                        cleanDate,
                                        if (shiftMode == "۲۴ ساعته") {
                                            "۲۴ ساعته"
                                        } else {
                                            shiftType
                                        },
                                        firstPerson,
                                        secondPerson
                                    )

                                    if (editIndex >= 0 && editIndex < shifts.size) {
                                        shifts[editIndex] = entry
                                        editIndex = -1
                                    } else {
                                        val duplicate = shifts.any {
                                            it.date == entry.date &&
                                                it.type == entry.type
                                        }

                                        if (duplicate) {
                                            Toast.makeText(
                                                context,
                                                "برای این تاریخ و نوع شیفت قبلاً ثبت شده است",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }

                                        shifts.add(entry)
                                    }

                                    saveShifts()
                                    firstPerson = ""
                                    secondPerson = ""
                                    date = ""

                                    Toast.makeText(
                                        context,
                                        "شیفت ذخیره شد",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (editIndex >= 0) "ذخیره ویرایش"
                                else "ثبت شیفت"
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                exportMonthlyCsvLikeText(context, shifts, year, month)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("گزارش متنی ماه")
                        }

                        HorizontalDivider()

                        Text(
                            "شیفت‌های ثبت‌شده",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        val filtered = shifts.filter {
                            val p = it.date.split("/")
                            p.size == 3 &&
                                p[0] == year &&
                                p[1].toIntOrNull() == month.toIntOrNull()
                        }.sortedBy { it.date }

                        if (filtered.isEmpty()) {
                            Text("برای این ماه هنوز شیفتی ثبت نشده است.")
                        }

                        filtered.forEach { entry ->
                            val actualIndex = shifts.indexOf(entry)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = PaleBlue
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "${entry.date}  •  ${weekdayForJalali(entry.date)}",
                                        fontWeight = FontWeight.Bold,
                                        color = Navy
                                    )
                                    Text("نوع شیفت: ${entry.type}")
                                    Text("نفر اول: ${entry.first}")
                                    Text("نفر دوم: ${entry.second}")

                                    Row(
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        TextButton(
                                            onClick = {
                                                editIndex = actualIndex
                                                date = entry.date
                                                firstPerson = entry.first
                                                secondPerson = entry.second
                                                shiftMode =
                                                    if (entry.type == "۲۴ ساعته") {
                                                        "۲۴ ساعته"
                                                    } else {
                                                        "۱۲ ساعته"
                                                    }
                                                shiftType = entry.type
                                            }
                                        ) {
                                            Text("ویرایش")
                                        }
                                        TextButton(
                                            onClick = {
                                                shifts.removeAt(actualIndex)
                                                saveShifts()
                                            }
                                        ) {
                                            Text("حذف")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Text("صفحه انتخاب‌شده در دسترس نیست.")
                    }
                }
            }
        }
    }

    if (showContact) {
        AlertDialog(
            onDismissRequest = { showContact = false },
            title = { Text("تماس با ما") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = Color.DarkGray)
            Text(
                value,
                color = Navy,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDropdown(
    label: String,
    people: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("انتخاب از فهرست پرسنل") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            people.forEach { person ->
                DropdownMenuItem(
                    text = { Text(person) },
                    onClick = {
                        onSelected(person)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun weekdayForJalali(date: String): String {
    return try {
        val p = date.split("/")
        val jy = p[0].toInt()
        val jm = p[1].toInt()
        val jd = p[2].toInt()

        val g = jalaliToGregorian(jy, jm, jd)
        val day = LocalDate.of(g[0], g[1], g[2]).dayOfWeek.value

        when (day) {
            6 -> "شنبه"
            7 -> "یکشنبه"
            1 -> "دوشنبه"
            2 -> "سه‌شنبه"
            3 -> "چهارشنبه"
            4 -> "پنجشنبه"
            5 -> "جمعه"
            else -> ""
        }
    } catch (_: Exception) {
        "روز نامشخص"
    }
}

fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): IntArray {
    var year = jy + 1595
    var days = -355668 + 365 * year +
        (year / 33) * 8 +
        ((year % 33 + 3) / 4) +
        jd

    days += if (jm < 7) {
        (jm - 1) * 31
    } else {
        (jm - 7) * 30 + 186
    }

    var gy = 400 * (days / 146097)
    days %= 146097

    if (days > 36524) {
        gy += 100 * ((--days) / 36524)
        days %= 36524
        if (days >= 365) days++
    }

    gy += 4 * (days / 1461)
    days %= 1461

    if (days > 365) {
        gy += (days - 1) / 365
        days = (days - 1) % 365
    }

    var gd = days + 1
    val leap = gy % 4 == 0 && (gy % 100 != 0 || gy % 400 == 0)
    val monthDays = intArrayOf(
        0, 31, if (leap) 29 else 28, 31, 30, 31, 30,
        31, 31, 30, 31, 30, 31
    )

    var gm = 1
    while (gm <= 12 && gd > monthDays[gm]) {
        gd -= monthDays[gm]
        gm++
    }

    return intArrayOf(gy, gm, gd.toInt())
}

fun exportMonthlyCsvLikeText(
    context: Context,
    shifts: List<ShiftEntry>,
    year: String,
    month: String
) {
    val filtered = shifts.filter {
        val p = it.date.split("/")
        p.size == 3 && p[0] == year &&
            p[1].toIntOrNull() == month.toIntOrNull()
    }.sortedBy { it.date }

    val report = buildString {
        appendLine("گزارش شیفت‌یار اورژانس ۱۱۵")
        appendLine("ماه: $year/$month")
        appendLine("--------------------------------")
        filtered.forEach {
            appendLine("تاریخ: ${it.date} (${weekdayForJalali(it.date)})")
            appendLine("نوع شیفت: ${it.type}")
            appendLine("نفر اول: ${it.first}")
            appendLine("نفر دوم: ${it.second}")
            appendLine("--------------------------------")
        }
    }

    android.app.AlertDialog.Builder(context)
        .setTitle("گزارش ماهانه")
        .setMessage(report)
        .setPositiveButton("بستن", null)
        .show()
}
