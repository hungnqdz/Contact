package com.contact_app.contact.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.contact_app.contact.MainActivity
import com.contact_app.contact.R
import com.contact_app.contact.adapter.CustomSpinnerAdapter
import com.contact_app.contact.base.getTimeRangeLabel
import com.contact_app.contact.databinding.FragmentChartBinding
import com.contact_app.contact.db.ContactDatabaseHelper
import com.contact_app.contact.model.CallLogStats
import com.contact_app.contact.model.ScheduleStats
import com.contact_app.contact.model.StatsType
import com.contact_app.contact.model.TimeRange
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class ChartFragment : Fragment() {
    lateinit var viewBinding: FragmentChartBinding
    lateinit var dbHelper: ContactDatabaseHelper
    private var timeRange: TimeRange? = TimeRange.ALL
    private var listCallLogStats = mutableListOf<CallLogStats>()
    private var listScheduleStats = mutableListOf<ScheduleStats>()
    private var statsType = StatsType.CALL_LOG

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (!::viewBinding.isInitialized) {
            viewBinding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_chart, container, false)
            viewBinding.apply {
                root.isClickable = true
            }
        }
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as MainActivity
        dbHelper = activity.dbHelper
        dbHelper.syncContactsFromDevice()
        dbHelper.syncCallLogsFromDevice()
        viewBinding.apply {
            // Set up spinner
            val spinnerAdapter = CustomSpinnerAdapter(
                requireContext(), R.layout.item_spinner,
                listOf("Hôm nay", "Tuần này", "Tháng này", "Năm nay", "Tất cả")
            )
            dropdownBtn.adapter = spinnerAdapter
            dropdownBtn.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedItem = parent?.getItemAtPosition(position).toString()
                    timeRange = TimeRange.entries.getOrNull(position)
                    tvTime.text = getTimeRangeLabel(timeRange ?: TimeRange.ALL)
                    // Update data based on statsType
                    updateStats()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // No action needed
                }
            }

            // Set up button listeners for toggling tables
            btnCall.setOnClickListener {
                statsType = StatsType.CALL_LOG
                tableCallLog.visibility = View.VISIBLE
                tableSchedule.visibility = View.GONE
                updateStats()
            }

            btnSchedule.setOnClickListener {
                statsType = StatsType.SCHEDULE
                tableCallLog.visibility = View.GONE
                tableSchedule.visibility = View.VISIBLE
                updateStats()
            }

            // Initial update
            updateStats()
        }
    }

    private fun updateStats() {
        when (statsType) {
            StatsType.CALL_LOG -> {
                listCallLogStats.clear()
                listCallLogStats.addAll(
                    dbHelper.getCallLogStatistics(timeRange ?: TimeRange.ALL)
                )
                updatePieChart(listCallLogStats.map {
                    PieEntry(it.totalDuration.toFloat(), it.contactName)
                })
                updateTopCallLogTable()
            }
            StatsType.SCHEDULE -> {
                listScheduleStats.clear()
                listScheduleStats.addAll(
                    dbHelper.getScheduleStatistics(timeRange ?: TimeRange.ALL)
                )
                updatePieChart(listScheduleStats.map {
                    PieEntry(it.scheduleCount.toFloat(), it.contactName)
                })
                updateTopScheduleTable()
            }
        }
    }

    private fun updatePieChart(entries: List<PieEntry>) {
        val filteredEntries = entries.filter { it.value.toInt() != 0 }
        val pieDataSet = PieDataSet(filteredEntries, "")
        pieDataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
        viewBinding.pieChart.data = PieData(pieDataSet)
        pieDataSet.valueTextSize = 12f
        viewBinding.pieChart.animateY(1000)
        viewBinding.pieChart.invalidate()
    }

    private fun updateTopCallLogTable() {
        // Sort contacts by total duration in descending order and take top 3
        val topContacts = listCallLogStats
            .sortedByDescending { it.totalDuration }
            .take(3)

        // Define the TextView IDs and colors
        val textViewIds = listOf(
            Triple(
                viewBinding.tvContactName1,
                viewBinding.tvCallCount1,
                viewBinding.tvTotalDuration1
            ),
            Triple(
                viewBinding.tvContactName2,
                viewBinding.tvCallCount2,
                viewBinding.tvTotalDuration2
            ),
            Triple(
                viewBinding.tvContactName3,
                viewBinding.tvCallCount3,
                viewBinding.tvTotalDuration3
            )
        )

        val colors = listOf(
            "#4CAF50", // Green
            "#FF9800", // Orange
            "#F44336"  // Red
        )

        // Update each row
        textViewIds.forEachIndexed { index, (nameView, countView, durationView) ->
            if (index < topContacts.size) {
                val contact = topContacts[index]
                nameView.text = contact.contactName
                countView.text = contact.callCount.toString()
                durationView.text = formatDuration(contact.totalDuration)
                // Set text color
                nameView.setTextColor(android.graphics.Color.parseColor(colors[index]))
                countView.setTextColor(android.graphics.Color.parseColor(colors[index]))
                durationView.setTextColor(android.graphics.Color.parseColor(colors[index]))
            } else {
                // Clear the row if no contact exists
                nameView.text = ""
                countView.text = ""
                durationView.text = ""
            }
        }
    }

    private fun updateTopScheduleTable() {
        // Sort schedules by count in descending order and take top 3
        val topSchedules = listScheduleStats
            .sortedByDescending { it.scheduleCount }
            .take(3)

        // Define the TextView IDs and colors
        val textViewIds = listOf(
            Pair(viewBinding.tvScheduleName1, viewBinding.tvScheduleCount1),
            Pair(viewBinding.tvScheduleName2, viewBinding.tvScheduleCount2),
            Pair(viewBinding.tvScheduleName3, viewBinding.tvScheduleCount3)
        )

        val colors = listOf(
            "#4CAF50", // Green
            "#FF9800", // Orange
            "#F44336"  // Red
        )

        // Update each row
        textViewIds.forEachIndexed { index, (nameView, countView) ->
            if (index < topSchedules.size) {
                val schedule = topSchedules[index]
                nameView.text = schedule.contactName
                countView.text = schedule.scheduleCount.toString()
                // Set text color
                nameView.setTextColor(android.graphics.Color.parseColor(colors[index]))
                countView.setTextColor(android.graphics.Color.parseColor(colors[index]))
            } else {
                // Clear the row if no schedule exists
                nameView.text = ""
                countView.text = ""
            }
        }
    }

    private fun formatDuration(seconds: Long): String {
        if (seconds < 60) return "${seconds}s"
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return if (remainingSeconds == 0L) "${minutes}m" else "${minutes}m${remainingSeconds}s"
    }
}