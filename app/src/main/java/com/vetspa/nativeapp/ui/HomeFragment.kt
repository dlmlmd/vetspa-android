package com.vetspa.nativeapp.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.vetspa.nativeapp.R
import com.vetspa.nativeapp.data.api.ApiClient
import com.vetspa.nativeapp.data.model.Booking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _root: View? = null

    private lateinit var greetingText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var pkgCountText: TextView
    private lateinit var todayCountText: TextView
    private lateinit var nextBookingCard: MaterialCardView
    private lateinit var nextDate: TextView
    private lateinit var nextTime: TextView
    private lateinit var nextStaff: TextView
    private lateinit var nextStatus: TextView
    private lateinit var nextEmptyText: TextView
    private lateinit var bookNowBtn: MaterialButton
    private lateinit var loading: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_home, container, false)
        _root = v

        greetingText = v.findViewById(R.id.greetingText)
        subtitleText = v.findViewById(R.id.subtitleText)
        pkgCountText = v.findViewById(R.id.pkgCountText)
        todayCountText = v.findViewById(R.id.todayCountText)
        nextBookingCard = v.findViewById(R.id.nextBookingCard)
        nextDate = v.findViewById(R.id.nextDate)
        nextTime = v.findViewById(R.id.nextTime)
        nextStaff = v.findViewById(R.id.nextStaff)
        nextStatus = v.findViewById(R.id.nextStatus)
        nextEmptyText = v.findViewById(R.id.nextEmptyText)
        bookNowBtn = v.findViewById(R.id.bookNowBtn)
        loading = v.findViewById(R.id.homeLoading)

        val prefs = requireContext().getSharedPreferences("vetspa_user", Context.MODE_PRIVATE)
        val name = prefs.getString("fullname", prefs.getString("username", ""))
        greetingText.text = "Xin chào, ${name ?: "bạn"}!"
        subtitleText.text = "Chào mừng bạn đến với VetSpa"

        bookNowBtn.setOnClickListener {
            BookingFormSheet {
                loadDashboard()
            }.show(parentFragmentManager, "booking_form")
        }

        loadDashboard()

        return v
    }

    private fun loadDashboard() {
        loading.visibility = View.VISIBLE
        val today = SimpleDateFormat("yyyy-MM-dd", Locale("vi", "VN")).format(Date())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pkgResp = ApiClient.api.getMyPackages()
                val bookResp = ApiClient.api.getMyBookings()

                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE

                    // Package count
                    val pkgList = pkgResp.body()?.packages ?: emptyList()
                    val totalSessions = pkgList.sumOf { it.sessionsRemaining ?: 0 }
                    pkgCountText.text = totalSessions.toString()

                    // Today's bookings
                    val groups = bookResp.body()?.bookings ?: emptyList()
                    val todayGroup = groups.find { it.date == today }
                    val todayCount = todayGroup?.items?.size ?: 0
                    todayCountText.text = todayCount.toString()

                    // Next upcoming booking
                    val allItems = groups.flatMap { g -> g.items.map { it to g.date } }
                        .sortedBy { (b, _) -> b.startTime }
                        .filter { (b, d) -> d >= today && b.status != "cancelled" }

                    val next = allItems.firstOrNull()
                    if (next != null) {
                        val (booking, date) = next
                        bindNextBooking(booking, date)
                    } else {
                        nextBookingCard.visibility = View.GONE
                        nextEmptyText.visibility = View.VISIBLE
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                    pkgCountText.text = "—"
                    todayCountText.text = "—"
                }
            }
        }
    }

    private fun bindNextBooking(b: Booking, date: String) {
        val fmt = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("vi", "VN"))
        val dayFmt = SimpleDateFormat("EEEE, dd/MM", Locale("vi", "VN"))

        try {
            val start = sdf.parse(b.startTime) ?: Date()
            val day = sdf.parse("$date 00:00:00") ?: Date()
            nextDate.text = dayFmt.format(day)
            nextTime.text = fmt.format(start)
        } catch (_: Exception) {
            nextDate.text = date
            nextTime.text = b.startTime.substringAfter(" ").substringBeforeLast(":")
        }

        nextStaff.text = b.staffName ?: ""
        nextStatus.text = when (b.status) {
            "pending" -> "⏳ Chờ XN"
            "confirmed" -> "✅ Đã XN"
            "done" -> "✔️ Xong"
            else -> b.status
        }

        nextBookingCard.visibility = View.VISIBLE
        nextEmptyText.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _root = null
    }
}
