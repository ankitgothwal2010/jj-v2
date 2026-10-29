/*
 * Copyright ©️ 2026 Ankit Gothwal.
 * All rights reserved, subject to applicable third-party license terms.
 *
 * Project: JARVIS AI
 * Developer: Ankit Gothwal
 * Application ID: com.ankit.jarvis
 */
package com.ankit.jarvis

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ankit.jarvis.databinding.ActivityAboutBinding

/**
 * Official Creator, Ownership, Copyright, and Non-Affiliation Screen for JARVIS AI.
 * Created and developed by Ankit Gothwal.
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackFromAbout.setOnClickListener {
            finish()
        }
    }
}
