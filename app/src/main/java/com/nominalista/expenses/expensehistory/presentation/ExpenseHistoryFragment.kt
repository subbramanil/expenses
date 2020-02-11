package com.nominalista.expenses.expensehistory.presentation

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.CalendarMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import com.kizitonwose.calendarview.ui.MonthHeaderFooterBinder
import com.kizitonwose.calendarview.ui.ViewContainer
import com.kizitonwose.calendarview.utils.next
import com.kizitonwose.calendarview.utils.previous
import com.nominalista.expenses.R
import com.nominalista.expenses.util.extensions.application
import com.nominalista.expenses.util.extensions.plusAssign
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.calendar_day_legend.view.*
import kotlinx.android.synthetic.main.custom_calendar_day.view.*
import kotlinx.android.synthetic.main.fragment_expense_history.*
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.WeekFields
import java.util.*

class ExpenseHistoryFragment : Fragment() {
    private val compositeDisposable = CompositeDisposable()

    private var selectedDate: LocalDate? = null
    private lateinit var model: ExpenseHistoryFragmentModel
    private lateinit var adapter: ExpenseHistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_expense_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindWidgets(view)
        setupActionBar()
        setupRecyclerView()
        setupModel()
        bindModel()
    }

    private fun setupModel() {
        val factory = ExpenseHistoryFragmentModel.Factory(requireContext().application)
        model = ViewModelProviders.of(this, factory).get(ExpenseHistoryFragmentModel::class.java)
    }

    private fun bindModel() {
        compositeDisposable += model.itemModels
                .subscribe { adapter.submitList(it) }
    }

    private fun setupRecyclerView() {
        adapter = ExpenseHistoryAdapter()
        rvExpenseHistory.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        rvExpenseHistory.adapter = adapter
        rvExpenseHistory.addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
    }

    private fun bindWidgets(view: View) {
        val currentMonth = YearMonth.now()
        val daysOfWeek = daysOfWeekFromLocale()
        expenseHistoryCalendar.setup(currentMonth.minusMonths(10), currentMonth.plusMonths(10), daysOfWeek.first())
        expenseHistoryCalendar.scrollToMonth(currentMonth)

        class DayViewContainer(view: View) : ViewContainer(view) {
            lateinit var day: CalendarDay // Will be set when this container is bound.
            val textView = view.exFiveDayText
            val layout = view.exFiveDayLayout
            val flightTopView = view.exFiveDayFlightTop
            val flightBottomView = view.exFiveDayFlightBottom

            init {
                view.setOnClickListener {
                    if (day.owner == DayOwner.THIS_MONTH) {
                        if (selectedDate != day.date) {
                            val oldDate = selectedDate
                            selectedDate = day.date
                            expenseHistoryCalendar.notifyDateChanged(day.date)
                            oldDate?.let { expenseHistoryCalendar.notifyDateChanged(it) }
                        }
                    }
                }
            }
        }

        expenseHistoryCalendar.dayBinder = object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                val textView = container.textView
                val layout = container.layout
                textView.text = day.date.dayOfMonth.toString()

                val flightTopView = container.flightTopView
                val flightBottomView = container.flightBottomView

                flightTopView.background = null
                flightBottomView.background = null

                if (day.owner == DayOwner.THIS_MONTH) {
                    textView.setTextColorRes(R.color.custom_cal_text_grey)
                    layout.setBackgroundResource(if (selectedDate == day.date) R.drawable.custom_cal_selected_bg else 0)
                } else {
                    textView.setTextColorRes(R.color.custom_cal_text_grey)
                    layout.background = null
                }
            }
        }

        class MonthViewContainer(view: View) : ViewContainer(view) {
            val legendLayout = view.legendLayout
        }
        expenseHistoryCalendar.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                // Setup each header day text if we have not done that already.
                if (container.legendLayout.tag == null) {
                    container.legendLayout.tag = month.yearMonth
                    container.legendLayout.children.map { it as TextView }.forEachIndexed { index, tv ->
                        tv.text = daysOfWeek[index].getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                                .toUpperCase(Locale.ENGLISH)
                        tv.setTextColorRes(R.color.custom_cal_text_grey)
                        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    }
                    month.yearMonth
                }
            }
        }

        expenseHistoryCalendar.monthScrollListener = { month ->
            val title = "${monthTitleFormatter.format(month.yearMonth)} ${month.yearMonth.year}"
            exFiveMonthYearText.text = title

            selectedDate?.let {
                // Clear selection if we scroll to a new month.
                selectedDate = null
                expenseHistoryCalendar.notifyDateChanged(it)
//                updateAdapterForDate(null)
            }
        }

        exFiveNextMonthImage.setOnClickListener {
            expenseHistoryCalendar.findFirstVisibleMonth()?.let {
                expenseHistoryCalendar.smoothScrollToMonth(it.yearMonth.next)
            }
        }

        exFivePreviousMonthImage.setOnClickListener {
            expenseHistoryCalendar.findFirstVisibleMonth()?.let {
                expenseHistoryCalendar.smoothScrollToMonth(it.yearMonth.previous)
            }
        }

    }

    private fun setupActionBar() {
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar ?: return
        actionBar.setTitle(R.string.expense_history_title)
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp)
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> backSelected()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun backSelected(): Boolean {
        requireActivity().onBackPressed()
        return true
    }

    fun daysOfWeekFromLocale(): Array<DayOfWeek> {
        val firstDayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        var daysOfWeek = DayOfWeek.values()
        // Order `daysOfWeek` array so that firstDayOfWeek is at index 0.
        if (firstDayOfWeek != DayOfWeek.MONDAY) {
            val rhs = daysOfWeek.sliceArray(firstDayOfWeek.ordinal..daysOfWeek.indices.last)
            val lhs = daysOfWeek.sliceArray(0 until firstDayOfWeek.ordinal)
            daysOfWeek = rhs + lhs
        }
        return daysOfWeek
    }

    internal fun TextView.setTextColorRes(@ColorRes color: Int) = setTextColor(context.getColorCompat(color))
    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")
    internal fun Context.getColorCompat(@ColorRes color: Int) = ContextCompat.getColor(this, color)
}
