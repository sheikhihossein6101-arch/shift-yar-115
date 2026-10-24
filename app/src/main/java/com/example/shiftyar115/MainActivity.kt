
package com.example.shiftyar115

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
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
private val Teal = Color(0xFF12A89D)
private val PageBg = Color(0xFFF3F7FB)

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
                    background = PageBg,
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
    var unavailableRules by remember { mutableStateOf(prefs.getString("unavailable_rules", "") ?: "") }
    var requiredRules by remember { mutableStateOf(prefs.getString("required_rules", "") ?: "") }
    var rulePerson by remember { mutableStateOf("") }
    var ruleDays by remember { mutableStateOf(setOf(3, 7, 12)) }
    var ruleType by remember { mutableStateOf("عدم حضور") }
    var ruleMenuExpanded by remember { mutableStateOf(false) }
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
                        // داشبورد مدیریتی جدید
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Navy),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("داشبورد شیفت‌یار", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text("مدیریت هوشمند شیفت‌های اورژانس ۱۱۵", color = Color(0xFFD8E8FA), style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("امروز: ${LocalDate.now().dayOfMonth}", color = Color.White, fontWeight = FontWeight.Medium)
                                    Text("${LocalDate.now().year}/${LocalDate.now().monthValue}", color = Color.White)
                                }
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DashboardMetric("پرسنل", personnel.size.toString(), "👥", Modifier.weight(1f), Blue)
                            DashboardMetric("برنامه‌ها", shifts.size.toString(), "📋", Modifier.weight(1f), Teal)
                        }

                        Text("دسترسی سریع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DashboardAction("برنامه‌ریزی شیفت", "ساخت و ویرایش", Modifier.weight(1f)) { selectedPage = "مدیریت شیفت‌ها" }
                            DashboardAction("مدیریت پرسنل", "افزودن و حذف", Modifier.weight(1f)) { selectedPage = "مدیریت پرسنل" }
                        }

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

                        Text(
                            "نوع شیفت",
                            color = Navy,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("۲۴ ساعته", "صبح", "شب").forEach { type ->
                                FilterChip(
                                    selected = shiftType == type,
                                    onClick = {
                                        shiftType = type
                                        shiftMode = if (type == "۲۴ ساعته") "۲۴ ساعته" else "۱۲ ساعته"
                                    },
                                    label = { Text(type) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it.filter(Char::isDigit).take(2) },
                            label = { Text("شماره روز ماه") },
                            placeholder = { Text("مثلاً ۱۵") },
                            supportingText = { Text("سال و ماه را از بالا انتخاب کنید؛ روز هفته خودکار نمایش داده می‌شود.") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        val previewDate = "${year.trim()}/${month.toIntOrNull()?.toString()?.padStart(2, '0') ?: "01"}/${date.toIntOrNull()?.toString()?.padStart(2, '0') ?: "01"}"
                        if (date.toIntOrNull() in 1..31 && year.toIntOrNull() != null && month.toIntOrNull() in 1..12) {
                            Text(
                                "روز هفته: ${weekdayForJalali(previewDate)}",
                                color = Navy,
                                fontWeight = FontWeight.SemiBold
                            )
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
                                val dayNumber = date.toIntOrNull()
                                val monthNumber = month.toIntOrNull()
                                val yearNumber = year.toIntOrNull()
                                val validMonth = monthNumber in 1..12
                                val maxDay = if (monthNumber in 1..6) 31 else if (monthNumber in 7..11) 30 else 30
                                val validDate = dayNumber != null && dayNumber in 1..maxDay
                                val cleanDate = "${yearNumber ?: 0}/${monthNumber?.toString()?.padStart(2, '0') ?: "00"}/${dayNumber?.toString()?.padStart(2, '0') ?: "00"}"

                                if (!validMonth || yearNumber == null) {
                                    Toast.makeText(
                                        context,
                                        "ماه و سال را درست وارد کنید",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (!validDate) {
                                    Toast.makeText(
                                        context,
                                        "شماره روز معتبر از ۱ تا $maxDay وارد کنید",
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
                                        shiftType,
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

                        HorizontalDivider()
                        Text("برنامه‌ریزی هوشمند", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Navy)
                        Text("نام فرد را انتخاب کن و روزهای موردنظر را لمس کن.", style = MaterialTheme.typography.bodySmall)
                        Box {
                            OutlinedButton(onClick = { ruleMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (rulePerson.isBlank()) "انتخاب از فهرست پرسنل" else rulePerson)
                            }
                            DropdownMenu(expanded = ruleMenuExpanded, onDismissRequest = { ruleMenuExpanded = false }) {
                                personnel.forEach { name ->
                                    DropdownMenuItem(text = { Text(name) }, onClick = { rulePerson = name; ruleMenuExpanded = false })
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterChip(selected = ruleType == "عدم حضور", onClick = { ruleType = "عدم حضور" }, label = { Text("عدم حضور") })
                            FilterChip(selected = ruleType == "حضور اجباری", onClick = { ruleType = "حضور اجباری" }, label = { Text("حضور اجباری") })
                        }
                        Text("انتخاب روزهای ماه", fontWeight = FontWeight.SemiBold)
                        val ruleMaxDay = if ((month.toIntOrNull() ?: 7) <= 6) 31 else 30
                        (1..ruleMaxDay).toList().chunked(7).forEach { rowDays ->
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
                                rowDays.forEach { day ->
                                    FilterChip(
                                        selected = day in ruleDays,
                                        onClick = { ruleDays = if (day in ruleDays) ruleDays - day else ruleDays + day },
                                        label = { Text(day.toString()) }
                                    )
                                }
                            }
                        }
                        Button(onClick = {
                            if (rulePerson.isBlank()) {
                                Toast.makeText(context, "ابتدا نام پرسنل را انتخاب کن", Toast.LENGTH_SHORT).show()
                            } else {
                                fun updateRule(raw: String, name: String, days: Set<Int>): String {
                                    val lines = raw.lines().filterNot { it.substringBefore(":").trim().equals(name, true) && it.contains(":") }.toMutableList()
                                    if (days.isNotEmpty()) lines.add("$name:" + days.sorted().joinToString(","))
                                    return lines.filter { it.isNotBlank() }.joinToString("\n")
                                }
                                if (ruleType == "عدم حضور") {
                                    unavailableRules = updateRule(unavailableRules, rulePerson, ruleDays)
                                    prefs.edit().putString("unavailable_rules", unavailableRules).apply()
                                } else {
                                    requiredRules = updateRule(requiredRules, rulePerson, ruleDays)
                                    prefs.edit().putString("required_rules", requiredRules).apply()
                                }
                                Toast.makeText(context, "محدودیت روزهای $rulePerson ذخیره شد", Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("ذخیره محدودیت انتخاب‌شده") }
                        Text("محدودیت‌های عدم حضور: ${unavailableRules.ifBlank { "ثبت نشده" }}", style = MaterialTheme.typography.bodySmall)
                        Text("حضور اجباری: ${requiredRules.ifBlank { "ثبت نشده" }}", style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = {
                                val yy = year.toIntOrNull()
                                val mm = month.toIntOrNull()
                                if (personnel.size < 2 || yy == null || mm == null || mm !in 1..12) {
                                    Toast.makeText(context, "سال، ماه معتبر و حداقل دو پرسنل لازم است", Toast.LENGTH_LONG).show()
                                } else {
                                    val maxDay = if (mm <= 6) 31 else if (mm <= 11) 30 else 30
                                    fun parseRules(raw: String): Map<String, Set<Int>> = raw.lines().mapNotNull { line ->
                                        val parts = line.split(":", limit = 2)
                                        if (parts.size != 2) null else {
                                            val name = parts[0].trim()
                                            val days = parts[1].split(",", "،", " ").mapNotNull { token ->
                                                token.trim()
                                                    .replace('۰', '0').replace('۱', '1').replace('۲', '2')
                                                    .replace('۳', '3').replace('۴', '4').replace('۵', '5')
                                                    .replace('۶', '6').replace('۷', '7').replace('۸', '8')
                                                    .replace('۹', '9').toIntOrNull()
                                            }.filter { it in 1..maxDay }.toSet()
                                            if (name.isBlank()) null else name to days
                                        }
                                    }.toMap()
                                    val unavailable = parseRules(unavailableRules)
                                    val required = parseRules(requiredRules)
                                    val unknownNames = (unavailable.keys + required.keys).filter { n -> personnel.none { it.equals(n, true) } }
                                    if (unknownNames.isNotEmpty()) {
                                        Toast.makeText(context, "این نام‌ها در فهرست پرسنل نیستند: ${unknownNames.joinToString()}", Toast.LENGTH_LONG).show()
                                    } else {
                                        val generated = mutableListOf<ShiftEntry>()
                                        val counts = personnel.associateWith { 0 }.toMutableMap()
                                        val types = if (shiftType == "۲۴ ساعته") listOf("۲۴ ساعته") else listOf("صبح", "شب")
                                        var impossible = ""
                                        for (day in 1..maxDay) {
                                            val dayText = day.toString().padStart(2, '0')
                                            val dateText = "$yy/${mm.toString().padStart(2, '0')}/$dayText"
                                            val requiredToday = required.filterValues { day in it }.keys.mapNotNull { ruleName -> personnel.firstOrNull { it.equals(ruleName, ignoreCase = true) } }
                                            for (type in types) {
                                                val blocked = unavailable.filterValues { day in it }.keys
                                                val candidates = personnel.filter { person -> blocked.none { it.equals(person, ignoreCase = true) } }.sortedWith(compareBy<String> { counts[it] ?: 0 }.thenBy { it })
                                                val chosen = mutableListOf<String>()
                                                // If a required person has not yet been assigned today, prioritize them.
                                                val mustAssign = requiredToday.filter { it !in chosen && it in candidates }
                                                chosen.addAll(mustAssign.take(2))
                                                for (person in candidates) {
                                                    if (chosen.size >= 2) break
                                                    if (person !in chosen) chosen.add(person)
                                                }
                                                if (chosen.size < 2) {
                                                    impossible = "روز $day شیفت $type: دو نیروی مجاز پیدا نشد"
                                                    break
                                                }
                                                generated.add(ShiftEntry(dateText, type, chosen[0], chosen[1]))
                                                chosen.forEach { counts[it] = (counts[it] ?: 0) + 1 }
                                            }
                                            if (impossible.isNotBlank()) break
                                            val missingRequired = requiredToday.filter { name -> generated.none { it.date == dateText && (it.first == name || it.second == name) } }
                                            if (missingRequired.isNotEmpty()) {
                                                impossible = "روز $day حضور اجباری ${missingRequired.joinToString()} رعایت نشد"
                                                break
                                            }
                                        }
                                        if (impossible.isNotBlank()) {
                                            Toast.makeText(context, "برنامه کامل ساخته نشد: $impossible. محدودیت‌ها را بررسی کنید.", Toast.LENGTH_LONG).show()
                                        } else {
                                            shifts.removeAll { entry -> entry.date.startsWith("$yy/${mm.toString().padStart(2, '0')}/") }
                                            shifts.addAll(generated)
                                            saveShifts()
                                            Toast.makeText(context, "برنامه ماه ساخته شد. تعداد شیفت هر نفر: " + counts.entries.joinToString { "${it.key}: ${it.value}" }, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("ساخت خودکار برنامه ماه") }

                        OutlinedButton(
                            onClick = {
                                exportMonthlyPdf(context, shifts, year, month, baseName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("دریافت خروجی PDF ماهانه")
                        }

                        HorizontalDivider()

                        val filtered = shifts.filter {
                            val p = it.date.split("/")
                            p.size == 3 &&
                                p[0] == year &&
                                p[1].toIntOrNull() == month.toIntOrNull()
                        }.sortedBy { it.date }

                        var calendarExpanded by remember { mutableStateOf(false) }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PaleBlue),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                TextButton(
                                    onClick = { calendarExpanded = !calendarExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "تقویم ماهانه شیفت‌ها",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Navy
                                        )
                                        Text(if (calendarExpanded) "▲" else "▼", color = Navy)
                                    }
                                }
                                if (calendarExpanded) {
                                    MonthlyShiftCalendar(
                                        year = year.toIntOrNull() ?: 1405,
                                        month = month.toIntOrNull() ?: 1,
                                        shifts = filtered,
                                        onSelectDay = { selectedDay -> date = selectedDay.toString() }
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        Text(
                            "جدول کامل شیفت‌های ماه",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Navy
                        )

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
                                                date = entry.date.substringAfterLast("/")
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
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("تماس با ما", color = Navy, fontWeight = FontWeight.Bold)
                    Text("پیشنهادها و پرسش‌های شما به بهتر شدن شیفت‌یار کمک می‌کند.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = PaleBlue), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("حسین شیخی", fontWeight = FontWeight.Bold, color = Navy)
                            Text("کارشناس ارشد پرستاری اورژانس", style = MaterialTheme.typography.bodyMedium)
                            Text("سمنان  •  پشتیبانی شیفت‌یار", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                            Text("وب‌سایت: www.iranianems.ir", color = Blue, style = MaterialTheme.typography.bodyMedium)
                            Text("ایتا: eitaa.com/iranianems", color = Blue, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.iranianems.ir"))
                    try { context.startActivity(intent) } catch (_: Exception) {
                        Toast.makeText(context, "امکان باز کردن وب‌سایت وجود ندارد.", Toast.LENGTH_LONG).show()
                    }
                }) { Text("مشاهده وب‌سایت") }
            },
            dismissButton = { TextButton(onClick = { showContact = false }) { Text("بستن") } }
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
fun MonthlyShiftCalendar(
    year: Int,
    month: Int,
    shifts: List<ShiftEntry>,
    onSelectDay: (Int) -> Unit
) {
    val monthNumber = month.coerceIn(1, 12)
    val maxDay = if (monthNumber <= 6) 31 else if (monthNumber <= 11) 30 else 30
    val firstWeekday = remember(year, monthNumber) {
        val name = weekdayForJalali("$year/${monthNumber.toString().padStart(2, '0')}/01")
        when (name) {
            "شنبه" -> 0
            "یکشنبه" -> 1
            "دوشنبه" -> 2
            "سه‌شنبه" -> 3
            "چهارشنبه" -> 4
            "پنجشنبه" -> 5
            "جمعه" -> 6
            else -> 0
        }
    }
    val byDay = shifts.groupBy {
        it.date.substringAfterLast("/").toIntOrNull() ?: -1
    }
    val totalCells = ((firstWeekday + maxDay + 6) / 7) * 7
    val weekNames = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                weekNames.forEach { dayName ->
                    Box(Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        Text(dayName, color = Navy, fontWeight = FontWeight.Bold)
                    }
                }
            }
            for (row in 0 until totalCells / 7) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val day = row * 7 + col - firstWeekday + 1
                        Box(Modifier.weight(1f).padding(2.dp).heightIn(min = 74.dp)) {
                            if (day in 1..maxDay) {
                                val dayShifts = byDay[day].orEmpty()
                                Card(
                                    onClick = { onSelectDay(day) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (dayShifts.isEmpty()) PaleBlue else Color(0xFFDDF4E8)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(3.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                day.toString(),
                                                color = Navy,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (dayShifts.isNotEmpty()) {
                                                Text(
                                                    dayShifts.first().first,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Navy,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        dayShifts.take(2).forEach { shift ->
                                            Text(
                                                shift.type,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Teal,
                                                maxLines = 1
                                            )
                                            Text(
                                                shift.second,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Blue,
                                                maxLines = 1
                                            )
                                        }
                                        if (dayShifts.size > 2) {
                                            Text("+${dayShifts.size - 2}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMetric(title: String, value: String, symbol: String, modifier: Modifier = Modifier, accent: Color = Blue) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
            Text(symbol, style = MaterialTheme.typography.titleLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF526477))
        }
    }
}

@Composable
fun DashboardAction(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PaleBlue),
        onClick = onClick
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("↗", color = Blue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(title, color = Navy, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = Color(0xFF526477), style = MaterialTheme.typography.bodySmall)
        }
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

fun exportMonthlyPdf(
    context: Context,
    shifts: List<ShiftEntry>,
    year: String,
    month: String,
    baseName: String
) {
    val filtered = shifts.filter {
        val p = it.date.split("/")
        p.size == 3 && p[0].toIntOrNull() == year.toIntOrNull() &&
            p[1].toIntOrNull() == month.toIntOrNull()
    }.sortedBy {
        val p = it.date.split("/")
        p.getOrNull(2)?.toIntOrNull() ?: 0
    }

    if (filtered.isEmpty()) {
        Toast.makeText(context, "برای این ماه شیفتی ثبت نشده است", Toast.LENGTH_LONG).show()
        return
    }

    fun esc(value: String) = value.replace("&", "&amp;")
        .replace("<", "&lt;").replace(">", "&gt;")

    val byDay = filtered.groupBy { it.date.substringAfterLast("/").toIntOrNull() ?: 0 }
    val maxDay = if ((month.toIntOrNull() ?: 1) <= 6) 31 else 30
    val rows = (1..maxDay).joinToString("") { day ->
        val dayEntries = byDay[day].orEmpty()
        val morning = dayEntries.firstOrNull {
            it.type == "صبح" || it.type.lowercase().contains("morning")
        } ?: dayEntries.firstOrNull { it.type == "۲۴ ساعته" || it.type == "24h" }
        val night = dayEntries.firstOrNull {
            it.type == "شب" || it.type.lowercase().contains("night")
        } ?: dayEntries.firstOrNull { it.type == "۲۴ ساعته" || it.type == "24h" }
        val dateText = "$year/${month.toIntOrNull()?.toString()?.padStart(2, '0')}/${
            day.toString().padStart(2, '0')
        }"
        val weekday = weekdayForJalali(dateText)
        val friday = weekday == "جمعه"
        """<tr class="${if (friday) "friday" else if (day % 2 == 0) "even" else ""}">
            <td class="date">$day/${month.toIntOrNull()?.toString()?.padStart(2, '0')}</td>
            <td class="weekday">${esc(weekday)}</td>
            <td>${morning?.let { esc(it.first) } ?: ""}</td>
            <td>${morning?.let { esc(it.second) } ?: ""}</td>
            <td>${night?.let { esc(it.first) } ?: ""}</td>
            <td>${night?.let { esc(it.second) } ?: ""}</td>
        </tr>"""
    }
    val html = """
        <!doctype html>
        <html lang="fa" dir="rtl">
        <head><meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
        @page { size: A4 portrait; margin: 7mm; }
        * { box-sizing: border-box; }
        body { font-family: sans-serif; direction: rtl; color:#111; margin:0; }
        h1 { text-align:right; width:68%; margin-right:0; margin-left:auto; font-size:13px; margin-bottom:5px; color:#111; }
        .sub { text-align:right; width:68%; margin-right:0; margin-left:auto; font-weight:bold; font-size:10px; margin-bottom:7px; }
        table { width:68%; margin-right:0; margin-left:auto; border-collapse:collapse; table-layout:fixed; }
        th, td { border:1px solid #555; text-align:center; vertical-align:middle; overflow-wrap:anywhere; }
        th { padding:4px 1px; font-size:9px; background:#f0f3f6; }
        td { padding:3px 1px; height:20px; font-size:8px; }
        .group { background:#dce8f5; font-size:10px; font-weight:bold; }
        .date { width:11%; font-weight:bold; }
        .weekday { width:16%; }
        .friday td { color:#b00020; font-weight:bold; }
        tr.even td { background:#edf5fb; }
        .blank { background:#fff; }
        .name { font-weight:bold; }
        </style></head>
        <body>
        <h1>برنامه شیفت ماهانه پایگاه ${esc(baseName.ifBlank { "ثبت نشده" })}</h1>
        <div class="sub">اورژانس پیش‌بیمارستانی ۱۱۵ | ماه ${esc(year)}/${esc(month)}</div>
        <table>
          <thead>
            <tr>
              <th rowspan="2" class="date">تاریخ</th>
              <th rowspan="2" class="weekday">روز هفته</th>
              <th colspan="2" class="group">شیفت صبح</th>
              <th colspan="2" class="group">شیفت شب</th>
            </tr>
            <tr>
              <th>نفر اول</th><th>نفر دوم</th>
              <th>نفر اول</th><th>نفر دوم</th>
            </tr>
          </thead>
          <tbody>$rows</tbody>
        </table>
        </body></html>
    """.trimIndent()

    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
    if (printManager == null) {
        Toast.makeText(context, "امکان ساخت PDF در این دستگاه در دسترس نیست", Toast.LENGTH_LONG).show()
        return
    }
    val webView = WebView(context)
    webView.settings.defaultTextEncodingName = "UTF-8"
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String?) {
            val adapter = view.createPrintDocumentAdapter("ShiftYar-${year}-${month}")
            printManager.print(
                "گزارش شیفت‌یار ${year}/${month}",
                adapter,
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
            )
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
}
