package com.mg4.control.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import com.mg4.control.R
import com.mg4.control.hardware.MG4Hardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_audio, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Loudness ───────────────────────────────────────────────────────
        val switch = view.findViewById<Switch>(R.id.switch_loudness)
        // Loudness : l'AIDL vendor est BOOLÉEN → 1 = ON, 0 = OFF (et non 2/1).
        switch.setOnCheckedChangeListener { _, checked ->
            if (switch.isPressed) {
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    MG4Hardware.setLoudnessState(if (checked) 1 else 0)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val state = MG4Hardware.getLoudnessState()
            withContext(Dispatchers.Main) {
                if (isAdded && state >= 0) switch.isChecked = (state == 1)
            }
        }

        // ── Volume à l'ouverture d'une porte (v1 : SWI133) ──────────────────
        setupDoorVolume(view)
    }

    private fun setupDoorVolume(view: View) {
        val card = view.findViewById<View>(R.id.door_volume_card)
        if (!MG4Hardware.hasDoorVolumeFeature()) {
            card.visibility = View.GONE
            return
        }

        val prefs = requireContext().getSharedPreferences("mg4_settings", Context.MODE_PRIVATE)
        val toggle = view.findViewById<Switch>(R.id.switch_door_volume)
        val slider = view.findViewById<Slider>(R.id.slider_door_volume)

        val enabled = prefs.getBoolean("door_volume_enabled", false)
        toggle.isChecked = enabled
        slider.isEnabled = enabled

        // Si déjà activé, (re)démarre le watcher à l'ouverture de l'onglet (idempotent) :
        // garantit la connexion Car / les logs MG4_DOOR sans avoir à re-toggler.
        if (enabled) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { MG4Hardware.startDoorVolumeWatcher() }
        }

        // Borne le slider sur le max réel de la voiture (sinon valeur XML par défaut).
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val max = MG4Hardware.getMediaVolumeMax()
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                if (max > 0) slider.valueTo = max.toFloat()
                val level = prefs.getInt("door_volume_level", 0)
                slider.value = level.coerceIn(0, slider.valueTo.toInt()).toFloat()
            }
        }

        toggle.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("door_volume_enabled", checked).apply()
            slider.isEnabled = checked
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                if (checked) MG4Hardware.startDoorVolumeWatcher()
                else MG4Hardware.stopDoorVolumeWatcher()
            }
        }

        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) prefs.edit().putInt("door_volume_level", value.toInt()).apply()
        }
    }
}
