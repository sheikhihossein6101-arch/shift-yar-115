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

    val rows = filtered.joinToString("") { entry ->
        """<tr>
            <td>${esc(entry.date)}<br/><span>${esc(weekdayForJalali(entry.date))}</span></td>
            <td>${esc(if (entry.type == "۲۴ ساعته" || entry.type == "24h") "۲۴ ساعته" else if (entry.type == "صبح" || entry.type.lowercase().contains("morning")) "صبح" else if (entry.type == "شب" || entry.type.lowercase().contains("night")) "شب" else entry.type)}</td>
            <td>${esc(entry.first)}</td>
            <td>${esc(entry.second)}</td>
        </tr>"""
    }
    val html = """
        <!doctype html>
        <html lang="fa" dir="rtl">
        <head><meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
        @page { size: A4 landscape; margin: 14mm; }
        body { font-family: sans-serif; direction: rtl; color: #18324d; }
        h1 { text-align:center; color:#123A67; font-size:22px; }
        .sub { text-align:center; margin-bottom:20px; }
        table { width:100%; border-collapse:collapse; }
        th { background:#123A67; color:white; padding:10px; }
        td { border:1px solid #b8c8d8; padding:9px; text-align:center; }
        tr:nth-child(even) { background:#edf5fb; }
        span { color:#087f78; font-weight:bold; }
        </style></head>
        <body>
        <h1>گزارش ماهانه شیفت‌یار اورژانس ۱۱۵</h1>
        <div class="sub">پایگاه: ${esc(baseName.ifBlank { "ثبت نشده" })} | ماه: ${esc(year)}/${esc(month)}</div>
        <table><thead><tr><th>تاریخ و روز هفته</th><th>نوع شیفت</th><th>نفر اول</th><th>نفر دوم</th></tr></thead>
        <tbody>$rows</tbody></table>
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
