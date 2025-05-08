package com.example.sisig

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.sisig.data.AllOrder
import com.example.sisig.data.AppDatabase
import com.example.sisig.data.Notification
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class ReportFragment : Fragment() {
    private lateinit var db: AppDatabase
    private lateinit var dailySalesTextView: TextView
    private lateinit var monthlySalesTextView: TextView
    private lateinit var yearlySalesTextView: TextView
    private lateinit var salesChart: LineChart
    private var lastMilestone: Int = 0
    private val PREFS_NAME = "ReportPrefs"
    private val KEY_LAST_MILESTONE = "lastMilestone"

    companion object {
        fun newInstance(): ReportFragment {
            return ReportFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_report, container, false)

        dailySalesTextView = view.findViewById(R.id.daily_sales_amount)
        monthlySalesTextView = view.findViewById(R.id.monthly_sales_amount)
        yearlySalesTextView = view.findViewById(R.id.yearly_sales_amount)
        salesChart = view.findViewById(R.id.salesChart)

        db = AppDatabase.getInstance(requireContext())

        // Load lastMilestone from SharedPreferences
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        lastMilestone = prefs.getInt(KEY_LAST_MILESTONE, 0)

        val generateReportButton = view.findViewById<Button>(R.id.generateReportButton)
        generateReportButton.setOnClickListener {
            showDatePicker()
        }

        setupChart()

        viewLifecycleOwner.lifecycleScope.launch {
            db.allOrderDao.getAllOrders().collect { orders ->
                val total = orders.sumOf { it.totalAmount }
                checkMilestone(total)
            }
        }

        val calendar = Calendar.getInstance()
        generateReport(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        return view
    }

    private fun setupChart() {
        salesChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return value.toInt().toString()
                    }
                }
            }

            axisLeft.apply {
                setDrawGridLines(true)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "₱${value.toInt()}"
                    }
                }
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun updateChart(orders: List<AllOrder>) {
        val entries = orders.mapIndexed { index, order ->
            Entry(index.toFloat(), order.totalAmount.toFloat())
        }

        val dataSet = LineDataSet(entries, "Sales").apply {
            color = Color.RED
            setCircleColor(Color.RED)
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
        }

        salesChart.data = LineData(dataSet)
        salesChart.invalidate()
    }

    private fun checkMilestone(totalSales: Double) {
        val milestone = (totalSales / 2000).toInt() * 2000
        if (milestone > lastMilestone && milestone >= 2000) {
            lastMilestone = milestone
            // Save lastMilestone to SharedPreferences
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_LAST_MILESTONE, lastMilestone).apply()
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                // Check if notification already exists to avoid duplicates
                val existingNotifications = db.notificationDao.getAllNotificationsSync()
                if (!existingNotifications.any { it.message == "Milestone achieved: You earned ₱$milestone!" }) {
                    val notificationId = db.notificationDao.insert(
                        Notification(message = "Milestone achieved: You earned ₱$milestone!")
                    )
                    withContext(Dispatchers.Main) {
                        Log.d("ReportActivity", "Inserted milestone notification with ID: $notificationId")
                    }
                }
            }
        }
    }

    private fun generateReport(year: Int, month: Int, day: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            db.allOrderDao.getAllOrders().collect { orders ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month - 1, day)
                }

                val dailyOrders = orders.filter { order ->
                    val orderDate = parseOrderDate(order)
                    orderDate != null && isSameDay(orderDate, selectedDate)
                }

                val monthlyOrders = orders.filter { order ->
                    val orderDate = parseOrderDate(order)
                    orderDate != null && isSameMonth(orderDate, selectedDate)
                }

                val yearlyOrders = orders.filter { order ->
                    val orderDate = parseOrderDate(order)
                    orderDate != null && isSameYear(orderDate, selectedDate)
                }

                val dailyTotal = dailyOrders.sumOf { it.totalAmount }
                val monthlyTotal = monthlyOrders.sumOf { it.totalAmount }
                val yearlyTotal = yearlyOrders.sumOf { it.totalAmount }

                checkMilestone(yearlyTotal)

                dailySalesTextView.text = String.format("₱%.2f", dailyTotal)
                monthlySalesTextView.text = String.format("₱%.2f", monthlyTotal)
                yearlySalesTextView.text = String.format("₱%.2f", yearlyTotal)

                updateChart(dailyOrders)
            }
        }
    }

    private fun parseOrderDate(order: AllOrder): Calendar? {
        return try {
            val calendar = Calendar.getInstance()
            calendar.time = order.date
            calendar
        } catch (e: Exception) {
            null
        }
    }

    private fun isSameDay(date1: Calendar, date2: Calendar): Boolean {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH) &&
                date1.get(Calendar.DAY_OF_MONTH) == date2.get(Calendar.DAY_OF_MONTH)
    }

    private fun isSameMonth(date1: Calendar, date2: Calendar): Boolean {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH)
    }

    private fun isSameYear(date1: Calendar, date2: Calendar): Boolean {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                generateReport(year, month + 1, day)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.maxDate = calendar.timeInMillis
        datePicker.show()
    }
}