package com.vetspa.nativeapp.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.vetspa.nativeapp.R
import com.vetspa.nativeapp.data.model.Booking
import com.vetspa.nativeapp.data.model.DateGroup
import java.text.SimpleDateFormat
import java.util.*

class BookingListAdapter(
    private val onBookingClick: (Booking) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    private val items: MutableList<Any> = mutableListOf()

    data class DateHeader(
        val date: String,
        val displayDate: String,
        val count: Int
    )

    fun submitData(groups: List<DateGroup>) {
        items.clear()
        for (group in groups) {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale("vi", "VN"))
            cal.time = sdf.parse(group.date) ?: Date()

            val dayNames = arrayOf(
                "Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư",
                "Thứ năm", "Thứ sáu", "Thứ bảy"
            )
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
            val dayName = dayNames.getOrElse(dayOfWeek) { "" }

            val parts = group.date.split("-")
            val displayDate = if (parts.size == 3) {
                "$dayName, ${parts[2]}/${parts[1]}/${parts[0]}"
            } else {
                group.date
            }

            items.add(DateHeader(group.date, displayDate, group.items.size))
            items.addAll(group.items)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is DateHeader) TYPE_HEADER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(
                inflater.inflate(R.layout.item_booking_header, parent, false)
            )
        } else {
            ItemViewHolder(
                inflater.inflate(R.layout.item_booking_row, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val header = items[position] as DateHeader
                holder.dateText.text = header.displayDate
                holder.countText.text = "${header.count} lịch"
            }
            is ItemViewHolder -> {
                val booking = items[position] as Booking
                holder.bind(booking, onBookingClick)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // ==================== VIEW HOLDERS ====================

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.dateText)
        val countText: TextView = view.findViewById(R.id.countText)
    }

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val timeStart: TextView = view.findViewById(R.id.timeStart)
        private val timeEnd: TextView = view.findViewById(R.id.timeEnd)
        private val staffName: TextView = view.findViewById(R.id.staffName)
        private val bedText: TextView = view.findViewById(R.id.bedText)
        private val statusBadge: TextView = view.findViewById(R.id.statusBadge)
        private val packageName: TextView = view.findViewById(R.id.packageName)
        private val card: CardView = view.findViewById(R.id.cardView)

        fun bind(booking: Booking, onClick: (Booking) -> Unit) {
            val fmt = SimpleDateFormat("HH:mm", Locale("vi", "VN"))
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("vi", "VN"))
            try {
                val start = sdf.parse(booking.startTime) ?: Date()
                val end = sdf.parse(booking.endTime) ?: Date()
                timeStart.text = fmt.format(start)
                timeEnd.text = fmt.format(end)
            } catch (_: Exception) {
                timeStart.text = booking.startTime.substringAfter(" ").substringBeforeLast(":")
                timeEnd.text = booking.endTime.substringAfter(" ").substringBeforeLast(":")
            }

            staffName.text = booking.staffName ?: "NV #${booking.staffId}"
            bedText.text = "G${booking.bedId}"
            packageName.text = booking.packageName ?: ""

            // Status badge
            when (booking.status) {
                "pending" -> {
                    statusBadge.text = "Chờ XN"
                    statusBadge.setBackgroundResource(android.R.color.holo_orange_light)
                }
                "confirmed" -> {
                    statusBadge.text = "Đã XN"
                    statusBadge.setBackgroundResource(R.color.primary)
                }
                "done" -> {
                    statusBadge.text = "Xong"
                    statusBadge.setBackgroundResource(android.R.color.holo_green_light)
                }
                "cancelled" -> {
                    statusBadge.text = "Đã huỷ"
                    statusBadge.setBackgroundResource(android.R.color.darker_gray)
                }
                else -> {
                    statusBadge.text = booking.status
                    statusBadge.setBackgroundResource(android.R.color.darker_gray)
                }
            }

            card.setOnClickListener { onClick(booking) }
        }
    }
}
