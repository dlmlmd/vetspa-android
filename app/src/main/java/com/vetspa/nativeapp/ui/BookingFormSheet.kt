package com.vetspa.nativeapp.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.vetspa.nativeapp.R
import com.vetspa.nativeapp.data.api.ApiClient
import com.vetspa.nativeapp.data.api.CreateBookingRequest
import com.vetspa.nativeapp.data.model.Booking
import com.vetspa.nativeapp.data.model.DateGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class BookingFormSheet(
    private val onCreated: () -> Unit = {}
) : BottomSheetDialogFragment() {

    private var selectedDate = Calendar.getInstance()
    private var selectedHour = 9
    private var selectedMinute = 0
    private var selectedStaffId = 0
    private var selectedBedId = 0
    private var selectedPkgId = 0
    private var selectedDuration = 90

    private lateinit var dateBtn: MaterialButton
    private lateinit var startBtn: MaterialButton
    private lateinit var endBtn: MaterialButton
    private lateinit var bedGroup: RadioGroup
    private lateinit var staffGroup: RadioGroup
    private lateinit var staffLoading: TextView
    private lateinit var pkgGroup: RadioGroup
    private lateinit var pkgLoading: TextView
    private lateinit var noteInput: TextView
    private lateinit var errorText: TextView
    private lateinit var submitBtn: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.bottom_sheet_booking_form, container, false)

        dateBtn = v.findViewById(R.id.formDateBtn)
        startBtn = v.findViewById(R.id.formStartBtn)
        endBtn = v.findViewById(R.id.formEndBtn)
        bedGroup = v.findViewById(R.id.formBedGroup)
        staffGroup = v.findViewById(R.id.formStaffGroup)
        staffLoading = v.findViewById(R.id.formStaffLoading)
        pkgGroup = v.findViewById(R.id.formPackageGroup)
        pkgLoading = v.findViewById(R.id.formPkgLoading)
        noteInput = v.findViewById(R.id.formNote)
        errorText = v.findViewById(R.id.formError)
        submitBtn = v.findViewById(R.id.formSubmitBtn)

        updateDateTimeDisplay()

        dateBtn.setOnClickListener { pickDate() }
        startBtn.setOnClickListener { pickTime() }
        submitBtn.setOnClickListener { submitBooking() }

        loadPackages()

        return v
    }

    private fun updateDateTimeDisplay() {
        val dateFmt = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi", "VN"))
        dateBtn.text = dateFmt.format(selectedDate.time)
        startBtn.text = String.format(Locale("vi", "VN"), "%02d:%02d", selectedHour, selectedMinute)

        // Auto end time
        val cal = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }
        cal.add(Calendar.MINUTE, selectedDuration)
        endBtn.text = String.format(Locale("vi", "VN"), "%02d:%02d",
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    private fun pickDate() {
        DatePickerDialog(requireContext(), { _, y, m, d ->
            selectedDate.set(Calendar.YEAR, y)
            selectedDate.set(Calendar.MONTH, m)
            selectedDate.set(Calendar.DAY_OF_MONTH, d)
            updateDateTimeDisplay()
            loadAvailability()
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun pickTime() {
        TimePickerDialog(requireContext(), { _, h, m ->
            selectedHour = h
            selectedMinute = m / 15 * 15
            updateDateTimeDisplay()
            loadAvailability()
        }, selectedHour, selectedMinute, true).show()
    }

    private fun getStartDatetime(): String {
        val cal = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("vi", "VN"))
        return fmt.format(cal.time)
    }

    private fun getEndDatetime(): String {
        val cal = Calendar.getInstance().apply {
            time = selectedDate.time
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
        }
        cal.add(Calendar.MINUTE, selectedDuration)
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("vi", "VN"))
        return fmt.format(cal.time)
    }

    private fun loadAvailability() {
        val start = getStartDatetime()
        val end = getEndDatetime()

        staffLoading.text = "Đang tải..."
        staffGroup.removeAllViews()
        pkgLoading.text = "Đang tải..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val bedsResp = ApiClient.api.getAvailableBeds(start, end)
                val staffResp = ApiClient.api.getAvailableStaff(start, end)

                withContext(Dispatchers.Main) {
                    // Beds
                    val availableBeds = bedsResp.body()?.beds ?: listOf(1, 2, 3, 4)
                    for (i in 0 until bedGroup.childCount) {
                        val rb = bedGroup.getChildAt(i) as? RadioButton ?: continue
                        val bedNum = i + 1
                        rb.isEnabled = bedNum in availableBeds
                        rb.alpha = if (bedNum in availableBeds) 1f else 0.3f
                    }

                    // Staff
                    staffGroup.removeAllViews()
                    val staffList = staffResp.body()?.staff ?: emptyList()
                    if (staffList.isEmpty()) {
                        staffLoading.text = "Không có nhân viên rảnh"
                    } else {
                        staffLoading.text = ""
                        for (s in staffList) {
                            val rb = RadioButton(requireContext()).apply {
                                id = View.generateViewId()
                                text = if (s.isFavorite) "⭐ ${s.name}" else s.name
                                tag = s.id
                                layoutParams = RadioGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, 44.dpToPx()
                                )
                            }
                            staffGroup.addView(rb)
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    staffLoading.text = "Lỗi tải dữ liệu"
                }
            }
        }
    }

    private fun loadPackages() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.api.getMyPackages()
                withContext(Dispatchers.Main) {
                    pkgGroup.removeAllViews()
                    val list = resp.body()?.packages ?: emptyList()
                    if (list.isEmpty()) {
                        pkgLoading.text = "Bạn chưa có gói nào. Hãy mua gói trước."
                    } else {
                        pkgLoading.text = ""
                        var firstId = 0
                        for (p in list) {
                            val rb = RadioButton(requireContext()).apply {
                                id = View.generateViewId()
                                text = "${p.packageName} (còn ${p.sessionsRemaining}/${p.sessions ?: "?"} buổi)"
                                tag = p.packageId
                                layoutParams = RadioGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT, 44.dpToPx()
                                )
                            }
                            pkgGroup.addView(rb)
                            if (firstId == 0) {
                                firstId = p.packageId
                                selectedPkgId = firstId
                                selectedDuration = 90 // fallback, will be updated from API
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    pkgLoading.text = "Lỗi tải gói dịch vụ"
                }
            }
        }
    }

    private fun submitBooking() {
        val start = getStartDatetime()
        val end = getEndDatetime()

        // Get selected bed
        val bedRbId = bedGroup.checkedRadioButtonId
        if (bedRbId == -1) { showError("Vui lòng chọn giường"); return }
        val bedIndex = bedGroup.indexOfChild(bedGroup.findViewById(bedRbId))
        selectedBedId = bedIndex + 1

        // Get selected staff
        val staffRbId = staffGroup.checkedRadioButtonId
        if (staffRbId == -1) { showError("Vui lòng chọn nhân viên"); return }
        val staffRb = staffGroup.findViewById<RadioButton>(staffRbId)
        selectedStaffId = staffRb?.tag as? Int ?: 0
        if (selectedStaffId == 0) { showError("Vui lòng chọn nhân viên"); return }

        // Get selected package
        val pkgRbId = pkgGroup.checkedRadioButtonId
        if (pkgRbId == -1) { showError("Vui lòng chọn gói"); return }
        val pkgRb = pkgGroup.findViewById<RadioButton>(pkgRbId)
        selectedPkgId = pkgRb?.tag as? Int ?: 0
        if (selectedPkgId == 0) { showError("Vui lòng chọn gói"); return }

        val note = noteInput.text?.toString()?.trim() ?: ""

        submitBtn.isEnabled = false
        submitBtn.text = "Đang xử lý..."
        errorText.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.api.createBooking(
                    CreateBookingRequest(
                        staffId = selectedStaffId,
                        bedId = selectedBedId,
                        startTime = start,
                        endTime = end,
                        packageId = selectedPkgId,
                        note = note.ifBlank { null }
                    )
                )
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body()?.ok == true) {
                        dismiss()
                        onCreated()
                    } else {
                        val err = resp.body()?.error ?: "Lỗi máy chủ (${resp.code()})"
                        showError(err)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Lỗi kết nối: ${e.localizedMessage ?: "Thử lại sau"}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    submitBtn.isEnabled = true
                    submitBtn.text = "Xác nhận đặt lịch"
                }
            }
        }
    }

    private fun showError(msg: String) {
        errorText.text = msg
        errorText.visibility = View.VISIBLE
    }
}

private fun Int.dpToPx(): Int {
    return (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
}
