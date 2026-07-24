package com.mg4.control.ui

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.mg4.control.R
import com.mg4.control.automation.AutomationSettings
import com.mg4.control.model.DrivingProfile
import com.mg4.control.profile.ProfileManager

class AutomationFragment : Fragment() {

    private var profiles: List<DrivingProfile> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_automation, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext().getSharedPreferences(AutomationSettings.PREFS, Context.MODE_PRIVATE)

        val switchAuto = view.findViewById<Switch>(R.id.switch_automation)
        val rowConfig  = view.findViewById<View>(R.id.row_automation_config)
        val inputTemp  = view.findViewById<EditText>(R.id.input_automation_temp)
        val spinner    = view.findViewById<Spinner>(R.id.spinner_automation_profile)
        val checkAuto  = view.findViewById<CheckBox>(R.id.check_auto_execute)

        val enabled = prefs.getBoolean(AutomationSettings.KEY_ENABLED, false)
        switchAuto.isChecked = enabled
        rowConfig.visibility = if (enabled) View.VISIBLE else View.GONE
        inputTemp.setText(prefs.getInt(AutomationSettings.KEY_THRESHOLD, AutomationSettings.DEFAULT_THRESHOLD).toString())
        checkAuto.isChecked = prefs.getBoolean(AutomationSettings.KEY_AUTO_EXECUTE, false)

        switchAuto.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(AutomationSettings.KEY_ENABLED, checked).apply()
            rowConfig.visibility = if (checked) View.VISIBLE else View.GONE
        }

        fun commitTemp() {
            val clamped = AutomationSettings.clampTemp(inputTemp.text.toString().toIntOrNull())
            prefs.edit().putInt(AutomationSettings.KEY_THRESHOLD, clamped).apply()
            val txt = clamped.toString()
            if (inputTemp.text.toString() != txt) inputTemp.setText(txt)
        }
        inputTemp.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) commitTemp() }
        inputTemp.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) commitTemp()
            false
        }

        checkAuto.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(AutomationSettings.KEY_AUTO_EXECUTE, checked).apply()
        }

        // ── Sens du déclenchement (inférieure / supérieure au seuil) ─────────
        val btnDirBelow = view.findViewById<MaterialButton>(R.id.btn_dir_below)
        val btnDirAbove = view.findViewById<MaterialButton>(R.id.btn_dir_above)
        val accentDim   = requireContext().getColor(R.color.dash_accent_dim)
        val inactive    = requireContext().getColor(R.color.dash_btn)

        fun highlightDirection(dir: AutomationSettings.Direction) {
            btnDirBelow.backgroundTintList = ColorStateList.valueOf(
                if (dir == AutomationSettings.Direction.BELOW) accentDim else inactive)
            btnDirAbove.backgroundTintList = ColorStateList.valueOf(
                if (dir == AutomationSettings.Direction.ABOVE) accentDim else inactive)
        }
        highlightDirection(AutomationSettings.readDirection(requireContext()))

        fun setDirection(dir: AutomationSettings.Direction) {
            prefs.edit().putString(AutomationSettings.KEY_DIRECTION, dir.name).apply()
            highlightDirection(dir)
        }
        btnDirBelow.setOnClickListener { setDirection(AutomationSettings.Direction.BELOW) }
        btnDirAbove.setOnClickListener { setDirection(AutomationSettings.Direction.ABOVE) }

        setupSpinner(spinner, prefs)
    }

    override fun onResume() {
        super.onResume()
        // Les profils peuvent avoir changé dans l'onglet Profils → on recharge la liste.
        view?.findViewById<Spinner>(R.id.spinner_automation_profile)?.let { sp ->
            val prefs = requireContext().getSharedPreferences(AutomationSettings.PREFS, Context.MODE_PRIVATE)
            setupSpinner(sp, prefs)
        }
    }

    private fun setupSpinner(spinner: Spinner, prefs: android.content.SharedPreferences) {
        profiles = ProfileManager(requireContext()).getAll()
        val labels = if (profiles.isEmpty()) listOf(getString(R.string.automation_no_profile))
                     else profiles.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.isEnabled = profiles.isNotEmpty()

        // Positionne sur le profil déjà configuré.
        val savedId = prefs.getString(AutomationSettings.KEY_PROFILE_ID, "") ?: ""
        val idx = profiles.indexOfFirst { it.id == savedId }
        if (idx >= 0) spinner.setSelection(idx)

        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (profiles.isEmpty()) return
                prefs.edit().putString(AutomationSettings.KEY_PROFILE_ID, profiles[position].id).apply()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }
}
