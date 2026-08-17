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
import com.mg4.control.automation.ClimateAutomationSettings
import com.mg4.control.hardware.MG4Hardware
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
        inputTemp.setText(prefs.getInt(AutomationSettings.KEY_THRESHOLD, AutomationSettings.DEFAULT_THRESHOLD).toString())
        checkAuto.isChecked = prefs.getBoolean(AutomationSettings.KEY_AUTO_EXECUTE, false)

        // L'interrupteur ne commande QUE l'activation : le parametrage reste consultable
        // automatisation eteinte, c'est le chevron qui le replie.
        switchAuto.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(AutomationSettings.KEY_ENABLED, checked).apply()
        }
        bindExpander(view.findViewById(R.id.btn_automation_expand), rowConfig, expanded = enabled)

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
        bindClimateAutomation(view, prefs)
    }

    // ══════════ Automatisation « Déclenchement A/C via la température » ══════════

    /**
     * Les deux règles (chaud / froid) ont exactement la même structure : on les câble via
     * [bindClimateRule] plutôt que de dupliquer six écouteurs, sinon une correction sur l'une
     * finit tôt ou tard par manquer sur l'autre.
     *
     * Les réglages partagent le fichier de préférences des profils ([AutomationSettings.PREFS])
     * mais pas leur interrupteur : cette automatisation n'est pas une application de profil.
     */
    private fun bindClimateAutomation(view: View, prefs: android.content.SharedPreferences) {
        val card = view.findViewById<View>(R.id.card_ac_automation)
        // Firmware inconnu = aucune voie clim → afficher des réglages sans effet serait trompeur.
        if (!MG4Hardware.hasClimateControl()) {
            card.visibility = View.GONE
            return
        }

        val switchAc = view.findViewById<Switch>(R.id.switch_ac_auto)
        val rowConfig = view.findViewById<View>(R.id.row_ac_auto_config)

        val enabled = prefs.getBoolean(ClimateAutomationSettings.KEY_ENABLED, false)
        switchAc.isChecked = enabled
        switchAc.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(ClimateAutomationSettings.KEY_ENABLED, checked).apply()
        }
        bindExpander(view.findViewById(R.id.btn_ac_auto_expand), rowConfig, expanded = enabled)

        bindClimateRule(
            view, prefs, hot = true,
            checkId = R.id.check_ac_hot, rowId = R.id.row_ac_hot_config,
            thresholdId = R.id.input_ac_hot_threshold, targetId = R.id.input_ac_hot_target,
            fanId = R.id.input_ac_hot_fan,
            defFrontId = R.id.check_ac_hot_def_front, defRearId = R.id.check_ac_hot_def_rear
        )
        bindClimateRule(
            view, prefs, hot = false,
            checkId = R.id.check_ac_cold, rowId = R.id.row_ac_cold_config,
            thresholdId = R.id.input_ac_cold_threshold, targetId = R.id.input_ac_cold_target,
            fanId = R.id.input_ac_cold_fan,
            defFrontId = R.id.check_ac_cold_def_front, defRearId = R.id.check_ac_cold_def_rear
        )
    }

    private fun bindClimateRule(
        view: View,
        prefs: android.content.SharedPreferences,
        hot: Boolean,
        checkId: Int, rowId: Int,
        thresholdId: Int, targetId: Int, fanId: Int,
        defFrontId: Int, defRearId: Int
    ) {
        val check     = view.findViewById<CheckBox>(checkId)
        val row       = view.findViewById<View>(rowId)
        val threshold = view.findViewById<EditText>(thresholdId)
        val target    = view.findViewById<EditText>(targetId)
        val fan       = view.findViewById<EditText>(fanId)
        val defFront  = view.findViewById<CheckBox>(defFrontId)
        val defRear   = view.findViewById<CheckBox>(defRearId)

        val active = prefs.getBoolean(ClimateAutomationSettings.keyOn(hot), false)
        check.isChecked = active
        row.visibility = if (active) View.VISIBLE else View.GONE
        check.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(ClimateAutomationSettings.keyOn(hot), checked).apply()
            row.visibility = if (checked) View.VISIBLE else View.GONE
        }

        val defThreshold = if (hot) ClimateAutomationSettings.DEFAULT_HOT_THRESHOLD
                           else ClimateAutomationSettings.DEFAULT_COLD_THRESHOLD
        val defTarget    = if (hot) ClimateAutomationSettings.DEFAULT_HOT_TARGET
                           else ClimateAutomationSettings.DEFAULT_COLD_TARGET
        threshold.setText(prefs.getInt(ClimateAutomationSettings.keyThreshold(hot), defThreshold).toString())
        target.setText(prefs.getInt(ClimateAutomationSettings.keyTarget(hot), defTarget).toString())
        fan.setText(prefs.getInt(ClimateAutomationSettings.keyFan(hot), ClimateAutomationSettings.DEFAULT_FAN).toString())

        bindIntField(threshold, ClimateAutomationSettings.keyThreshold(hot), prefs) {
            ClimateAutomationSettings.clampThreshold(it, hot)
        }
        bindIntField(target, ClimateAutomationSettings.keyTarget(hot), prefs) {
            ClimateAutomationSettings.clampTarget(it, hot)
        }
        bindIntField(fan, ClimateAutomationSettings.keyFan(hot), prefs) {
            ClimateAutomationSettings.clampFan(it)
        }

        defFront.isChecked = prefs.getBoolean(ClimateAutomationSettings.keyDefFront(hot), false)
        defRear.isChecked  = prefs.getBoolean(ClimateAutomationSettings.keyDefRear(hot), false)
        defFront.setOnCheckedChangeListener { _, c ->
            prefs.edit().putBoolean(ClimateAutomationSettings.keyDefFront(hot), c).apply()
        }
        defRear.setOnCheckedChangeListener { _, c ->
            prefs.edit().putBoolean(ClimateAutomationSettings.keyDefRear(hot), c).apply()
        }
    }

    /**
     * Enregistre un champ numérique à la perte de focus et sur « Terminé », en réécrivant la
     * valeur bornée dans le champ : sans ça l'utilisateur voit 99 alors que 33 a été enregistré.
     * Même motif que le seuil de l'automatisation profil au-dessus.
     */
    private fun bindIntField(
        field: EditText,
        key: String,
        prefs: android.content.SharedPreferences,
        clamp: (Int?) -> Int
    ) {
        fun commit() {
            val clamped = clamp(field.text.toString().toIntOrNull())
            prefs.edit().putInt(key, clamped).apply()
            val txt = clamped.toString()
            if (field.text.toString() != txt) field.setText(txt)
        }
        field.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) commit() }
        field.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) commit()
            false
        }
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

    /**
     * Chevron de depliage d'une carte d'automatisation.
     *
     * Volontairement decorrele de l'interrupteur d'activation : on doit pouvoir consulter et
     * modifier le parametrage sans activer l'automatisation, et inversement la laisser active
     * en repliant la carte. L'etat initial suit quand meme l'activation — une automatisation
     * eteinte s'ouvre repliee, ce qui reproduit le comportement precedent.
     */
    private fun bindExpander(btn: MaterialButton, content: View, expanded: Boolean) {
        var open = expanded
        fun apply() {
            content.visibility = if (open) View.VISIBLE else View.GONE
            btn.text = if (open) "▾" else "▸"   // chevron bas / droite
        }
        apply()
        btn.setOnClickListener { open = !open; apply() }
    }

}
