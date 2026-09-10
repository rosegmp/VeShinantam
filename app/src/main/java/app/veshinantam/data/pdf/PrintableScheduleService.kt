package app.veshinantam.data.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import app.veshinantam.R
import app.veshinantam.data.local.ScheduleDao
import app.veshinantam.data.local.TodayTaskRow
import app.veshinantam.domain.model.TaskType
import app.veshinantam.localization.AppLanguage
import app.veshinantam.localization.AppLocale
import app.veshinantam.localization.LanguageSettings
import app.veshinantam.localization.PrimaryCalendar
import app.veshinantam.localization.SefarimDisplay
import app.veshinantam.localization.SefarimLanguage
import app.veshinantam.ui.calendar.HebrewDateFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

data class PrintableScheduleSummary(val dayCount: Int, val taskCount: Int, val pageCount: Int)

class PrintableScheduleService(
    private val context: Context,
    private val dao: ScheduleDao,
) {
    suspend fun write(uri: Uri, startDate: LocalDate, dayCount: Int): PrintableScheduleSummary {
        require(dayCount in ALLOWED_DAY_COUNTS)
        val endDate = PrintableScheduleRules.endDate(startDate, dayCount)
        val tasks = dao.getPrintableTasks(startDate, endDate)
        val settings = LanguageSettings(context)
        val language = settings.read()
        val localizedContext = AppLocale.wrap(context, language)
        val renderer = SchedulePdfRenderer(
            context = localizedContext,
            language = language,
            sefarimLanguage = settings.readSefarimLanguage(),
            primaryCalendar = settings.readPrimaryCalendar(),
        )
        val document = renderer.render(startDate, endDate, tasks)
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use(document::writeTo)
                ?: error("The selected PDF file could not be opened.")
        } finally {
            document.close()
        }
        return PrintableScheduleSummary(dayCount, tasks.size, renderer.pageCount)
    }

    companion object {
        val ALLOWED_DAY_COUNTS = listOf(7, 14, 30, 60, 90)
    }
}

internal class SchedulePdfRenderer(
    private val context: Context,
    private val language: AppLanguage,
    private val sefarimLanguage: SefarimLanguage,
    private val primaryCalendar: PrimaryCalendar,
) {
    private val isRtl = language == AppLanguage.HEBREW
    private val locale = Locale.forLanguageTag(language.languageTag)
    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)
    private val document = PdfDocument()
    private var pageNumber = 0
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var y = TOP_MARGIN
    var pageCount: Int = 0
        private set

    fun render(startDate: LocalDate, endDate: LocalDate, tasks: List<TodayTaskRow>): PdfDocument {
        startPage(startDate, endDate)
        if (tasks.isEmpty()) {
            drawText(context.getString(R.string.print_no_tasks), BODY_SIZE, MUTED, false, CONTENT_WIDTH)
        } else {
            tasks.groupBy { it.plannedDate }.forEach { (date, dayTasks) ->
                ensureSpace(DATE_HEADER_HEIGHT + measureTaskHeight(dayTasks.first()))
                drawDateHeader(date)
                dayTasks.forEach(::drawTask)
            }
        }
        finishPage()
        return document
    }

    private fun startPage(startDate: LocalDate, endDate: LocalDate) {
        pageNumber++
        pageCount = pageNumber
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        y = TOP_MARGIN
        val title = context.getString(R.string.print_schedule_title)
        drawText(title, TITLE_SIZE, NAVY, true, CONTENT_WIDTH)
        val range = context.getString(R.string.print_date_range, displayDate(startDate), displayDate(endDate))
        drawText(range, SMALL_SIZE, MUTED, false, CONTENT_WIDTH)
        canvas.drawLine(LEFT_MARGIN, y + 5f, PAGE_WIDTH - RIGHT_MARGIN, y + 5f, Paint().apply {
            color = GOLD
            strokeWidth = 2f
        })
        y += 20f
    }

    private fun finishPage() {
        val footer = context.getString(R.string.print_page_number, pageNumber)
        val paint = textPaint(SMALL_SIZE, MUTED, false)
        val width = paint.measureText(footer)
        canvas.drawText(footer, (PAGE_WIDTH - width) / 2f, PAGE_HEIGHT - 22f, paint)
        document.finishPage(page)
    }

    private fun ensureSpace(height: Float) {
        if (y + height <= PAGE_HEIGHT - BOTTOM_MARGIN) return
        finishPage()
        pageNumber++
        pageCount = pageNumber
        page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        canvas = page.canvas
        canvas.drawColor(Color.WHITE)
        y = TOP_MARGIN
        drawText(context.getString(R.string.print_schedule_title), 16f, NAVY, true, CONTENT_WIDTH)
        y += 5f
    }

    private fun drawDateHeader(date: LocalDate) {
        val paint = Paint().apply { color = PALE_GOLD }
        canvas.drawRoundRect(LEFT_MARGIN, y, PAGE_WIDTH - RIGHT_MARGIN, y + DATE_HEADER_HEIGHT - 6f, 6f, 6f, paint)
        y += 8f
        drawText(displayDate(date), 12f, NAVY, true, CONTENT_WIDTH - 16f, horizontalInset = 8f)
        y += 4f
    }

    private fun drawTask(task: TodayTaskRow) {
        val layout = taskLayout(task)
        val rowHeight = maxOf(ROW_HEIGHT, layout.height + 12f)
        ensureSpace(rowHeight)
        val boxLeft = if (isRtl) PAGE_WIDTH - RIGHT_MARGIN - CHECKBOX_SIZE else LEFT_MARGIN
        canvas.drawRect(boxLeft, y + 9f, boxLeft + CHECKBOX_SIZE, y + 9f + CHECKBOX_SIZE, Paint().apply {
            color = NAVY
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
        })
        val textLeft = if (isRtl) LEFT_MARGIN else LEFT_MARGIN + CHECKBOX_SIZE + 10f
        canvas.save()
        canvas.translate(textLeft, y + 3f)
        layout.draw(canvas)
        canvas.restore()
        y += rowHeight
        canvas.drawLine(LEFT_MARGIN, y - 3f, PAGE_WIDTH - RIGHT_MARGIN, y - 3f, Paint().apply {
            color = DIVIDER
            strokeWidth = 0.6f
        })
    }

    private fun measureTaskHeight(task: TodayTaskRow): Float = maxOf(ROW_HEIGHT, taskLayout(task).height + 12f)

    private fun taskLayout(task: TodayTaskRow): StaticLayout {
        val scheduleName = PrintableScheduleRules.scheduleName(task, language)
        val type = context.getString(if (task.type == TaskType.LEARNING) R.string.new_learning else R.string.chazarah)
        val reference = SefarimDisplay.label(task.labelEnglish, task.labelHebrew, sefarimLanguage, language)
        val text = "$scheduleName - $type\n$reference"
        return StaticLayout.Builder.obtain(text, 0, text.length, textPaint(10.5f, TEXT, false), (CONTENT_WIDTH - CHECKBOX_SIZE - 10f).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
            .setIncludePad(false)
            .setLineSpacing(1f, 1f)
            .build()
    }

    private fun displayDate(date: LocalDate): String {
        val gregorian = date.format(dateFormatter)
        val hebrew = HebrewDateFormatter.format(date, locale)
        return when (primaryCalendar) {
            PrimaryCalendar.GREGORIAN -> "$gregorian - $hebrew"
            PrimaryCalendar.HEBREW -> "$hebrew - $gregorian"
        }
    }

    private fun drawText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean,
        width: Float,
        horizontalInset: Float = 0f,
        advance: Boolean = true,
    ) {
        val layout = StaticLayout.Builder.obtain(value, 0, value.length, textPaint(size, color, bold), width.toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(if (isRtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.LTR)
            .setIncludePad(false)
            .setLineSpacing(1f, 1f)
            .build()
        canvas.save()
        canvas.translate(LEFT_MARGIN + horizontalInset, y)
        layout.draw(canvas)
        canvas.restore()
        if (advance) y += layout.height + 5f
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val LEFT_MARGIN = 42f
        const val RIGHT_MARGIN = 42f
        const val TOP_MARGIN = 38f
        const val BOTTOM_MARGIN = 45f
        const val CONTENT_WIDTH = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN
        const val TITLE_SIZE = 22f
        const val BODY_SIZE = 11f
        const val SMALL_SIZE = 9f
        const val DATE_HEADER_HEIGHT = 30f
        const val ROW_HEIGHT = 48f
        const val CHECKBOX_SIZE = 15f
        val NAVY = Color.rgb(21, 59, 91)
        val GOLD = Color.rgb(201, 153, 46)
        val PALE_GOLD = Color.rgb(249, 241, 219)
        val TEXT = Color.rgb(35, 38, 42)
        val MUTED = Color.rgb(92, 98, 105)
        val DIVIDER = Color.rgb(220, 222, 224)
    }
}

internal object PrintableScheduleRules {
    fun endDate(startDate: LocalDate, dayCount: Int): LocalDate {
        require(dayCount > 0)
        return startDate.plusDays(dayCount.toLong() - 1)
    }

    fun scheduleName(task: TodayTaskRow, language: AppLanguage): String =
        if (language == AppLanguage.HEBREW) {
            task.scheduleNameHebrew.ifBlank { task.scheduleNameEnglish }
        } else {
            task.scheduleNameEnglish.ifBlank { task.scheduleNameHebrew }
        }
}
