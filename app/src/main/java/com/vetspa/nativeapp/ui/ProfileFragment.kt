package com.vetspa.nativeapp.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.vetspa.nativeapp.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("vetspa_user", Context.MODE_PRIVATE)
        binding.userName.text = prefs.getString("fullname", prefs.getString("username", ""))
        binding.userRole.text = prefs.getString("role", "")?.uppercase() ?: ""
        binding.userUsername.text = prefs.getString("username", "")
        binding.userEmail.text = prefs.getString("email", "—")
        binding.userProfileCode.text = prefs.getString("profile_code", "—")

        binding.logoutBtn.setOnClickListener {
            requireContext().getSharedPreferences("vetspa_user", Context.MODE_PRIVATE).edit().clear().apply()
            requireContext().getSharedPreferences("vetspa_cookies", Context.MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
