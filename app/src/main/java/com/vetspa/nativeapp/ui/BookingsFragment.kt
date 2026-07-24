package com.vetspa.nativeapp.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.vetspa.nativeapp.R
import com.vetspa.nativeapp.data.api.ApiClient
import com.vetspa.nativeapp.ui.adapter.BookingListAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookingsFragment : Fragment() {

    private var _root: View? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyState: View
    private lateinit var loadingBar: ProgressBar
    private lateinit var emptyIcon: TextView
    private lateinit var emptyTitle: TextView
    private lateinit var emptySubtitle: TextView
    private val adapter = BookingListAdapter { booking ->
        // TODO: mở detail bottom sheet
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_bookings, container, false)
        _root = v

        recyclerView = v.findViewById(R.id.recyclerView)
        swipeRefresh = v.findViewById(R.id.swipeRefresh)
        emptyState = v.findViewById(R.id.emptyState)
        loadingBar = v.findViewById(R.id.loadingBar)
        emptyIcon = v.findViewById(R.id.emptyIcon)
        emptyTitle = v.findViewById(R.id.emptyTitle)
        emptySubtitle = v.findViewById(R.id.emptySubtitle)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadBookings() }
        loadBookings()

        return v
    }

    private fun loadBookings() {
        loadingBar.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.api.getMyBookings()
                withContext(Dispatchers.Main) {
                    loadingBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false

                    if (resp.isSuccessful && resp.body()?.ok == true) {
                        val groups = resp.body()?.bookings
                        if (groups.isNullOrEmpty()) {
                            adapter.submitData(emptyList())
                            showEmpty("📅", "Chưa có lịch đặt", "Đặt lịch ngay để trải nghiệm dịch vụ")
                        } else {
                            adapter.submitData(groups)
                            emptyState.visibility = View.GONE
                            recyclerView.visibility = View.VISIBLE
                        }
                    } else {
                        showEmpty("⚠️", "Không thể tải lịch", "")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                    showEmpty("📡", "Lỗi kết nối", e.localizedMessage ?: "Vui lòng thử lại sau")
                }
            }
        }
    }

    private fun showEmpty(icon: String, title: String, subtitle: String) {
        recyclerView.visibility = View.GONE
        emptyIcon.text = icon
        emptyTitle.text = title
        emptySubtitle.text = subtitle
        emptySubtitle.visibility = if (subtitle.isEmpty()) View.GONE else View.VISIBLE
        emptyState.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _root = null
    }
}
