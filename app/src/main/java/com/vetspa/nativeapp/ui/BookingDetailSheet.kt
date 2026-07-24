package com.vetspa.nativeapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.vetspa.nativeapp.R
import com.vetspa.nativeapp.data.api.ApiClient
import com.vetspa.nativeapp.data.model.Booking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BookingDetailSheet(
    private val bookingId: Int,
    private val onCancelled: () -> Unit = {}
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.bottom_sheet_booking_detail, container, false)
        loadDetail(v)
        return v
    }

    private fun loadDetail(v: View) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.api.getBookingDetail(bookingId)
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body()?.ok == true) {
                        val b = resp.body()?.booking
                        if (b != null) bindDetail(v, b)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun bindDetail(v: View, b: Booking) {
        val fmt = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("vi", "VN"))
        try {
            val start = sdf.parse(b.startTime) ?: Date()
            val end = sdf.parse(b.endTime) ?: Date()
            val dayFmt = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN"))
            v.findViewById<TextView>(R.id.detailTime).text =
                "${dayFmt.format(start)}\n${fmt.format(start)} → ${fmt.format(end)}"
        } catch (_: Exception) {
            v.findViewById<TextView>(R.id.detailTime).text = b.startTime
        }

        v.findViewById<TextView>(R.id.detailStaff).text = b.staffName ?: "NV #${b.staffId}"
        v.findViewById<TextView>(R.id.detailBed).text = "Giường ${b.bedId}"
        v.findViewById<TextView>(R.id.detailPackage).text = b.packageName ?: "—"
        v.findViewById<TextView>(R.id.detailStatus).text = statusText(b.status)

        if (!b.note.isNullOrBlank()) {
            v.findViewById<TextView>(R.id.detailNote).text = b.note
            v.findViewById<View>(R.id.detailNoteRow).visibility = View.VISIBLE
        }

        val cancelBtn = v.findViewById<MaterialButton>(R.id.detailCancelBtn)
        if (b.status == "pending") {
            cancelBtn.visibility = View.VISIBLE
            cancelBtn.setOnClickListener {
                cancelBtn.isEnabled = false
                cancelBtn.text = "Đang huỷ..."
                cancelBooking(b.id)
            }
        }
    }

    private fun cancelBooking(bid: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.api.cancelBooking(
                    com.vetspa.nativeapp.data.api.CancelBookingRequest(bid)
                )
                withContext(Dispatchers.Main) {
                    dismiss()
                    onCancelled()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    dismiss()
                }
            }
        }
    }

    private fun statusText(s: String): String = when (s) {
        "pending" -> "⏳ Chờ xác nhận"
        "confirmed" -> "✅ Đã xác nhận"
        "done" -> "✔️ Hoàn tất"
        "cancelled" -> "❌ Đã huỷ"
        else -> s
    }
}
