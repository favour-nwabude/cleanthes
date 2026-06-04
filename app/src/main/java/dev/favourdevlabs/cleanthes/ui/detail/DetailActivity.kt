package dev.favourdevlabs.cleanthes.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import dev.favourdevlabs.cleanthes.security.TOTPGenerator
import dev.favourdevlabs.cleanthes.ui.addedit.AddEditActivity
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import java.util.concurrent.Executors

class DetailActivity : AuthenticatedActivity() {

    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
    }

    private lateinit var tvToolbarTitle: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnEdit: Button
    private lateinit var tvCategory: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvPassword: TextView
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnCopyUsername: ImageButton
    private lateinit var btnCopyPassword: ImageButton
    private lateinit var labelWebsite: TextView
    private lateinit var tvWebsite: TextView
    private lateinit var labelNotes: TextView
    private lateinit var tvNotes: TextView
    private lateinit var tvFavorite: TextView
    private lateinit var labelTotp: TextView
    private lateinit var totpRow: View
    private lateinit var tvTotpCode: TextView
    private lateinit var btnCopyTotp: ImageButton
    private lateinit var progressBarTotp: ProgressBar

    private var totpHandler: Handler? = null
    private var totpRunnable: Runnable? = null

    private lateinit var repository: VaultRepository
    private var plainPassword = ""
    private var passwordVisible = false
    private var entryId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        repository = VaultRepository.getInstance(this)
        bindViews()
        attachListeners()
        entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1)
        loadEntry(entryId)
    }

    override fun onResume() {
        super.onResume()
        if (entryId != -1L) loadEntry(entryId)
    }

    override fun onPause() {
        super.onPause()
        stopTotpUpdater()
    }

    private fun bindViews() {
        tvToolbarTitle = findViewById(R.id.detail_tv_toolbar_title)
        btnBack = findViewById(R.id.detail_btn_back)
        btnEdit = findViewById(R.id.detail_btn_edit)
        tvCategory = findViewById(R.id.detail_tv_category)
        tvUsername = findViewById(R.id.detail_tv_username)
        tvPassword = findViewById(R.id.detail_tv_password)
        btnTogglePassword = findViewById(R.id.detail_btn_toggle_password)
        btnCopyUsername = findViewById(R.id.detail_btn_copy_username)
        btnCopyPassword = findViewById(R.id.detail_btn_copy_password)
        labelWebsite = findViewById(R.id.detail_label_website)
        tvWebsite = findViewById(R.id.detail_tv_website)
        labelNotes = findViewById(R.id.detail_label_notes)
        tvNotes = findViewById(R.id.detail_tv_notes)
        tvFavorite = findViewById(R.id.detail_tv_favorite)
        labelTotp = findViewById(R.id.detail_label_totp)
        totpRow = findViewById(R.id.detail_totp_row)
        tvTotpCode = findViewById(R.id.detail_tv_totp_code)
        btnCopyTotp = findViewById(R.id.detail_btn_copy_totp)
        progressBarTotp = findViewById(R.id.detail_totp_progress)

        btnEdit.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.citadel_gold))
    }

    private fun attachListeners() {
        btnBack.setOnClickListener { finish() }
        btnEdit.setOnClickListener {
            startActivity(Intent(this, AddEditActivity::class.java).apply {
                putExtra(AddEditActivity.EXTRA_ENTRY_ID, entryId)
            })
        }
        btnTogglePassword.setOnClickListener {
            passwordVisible = !passwordVisible
            tvPassword.text = if (passwordVisible) plainPassword else "••••••••••••"
            btnTogglePassword.setImageResource(
                if (passwordVisible) R.drawable.ic_eye_on else R.drawable.ic_eye_off
            )
        }
        btnCopyUsername.setOnClickListener { copyToClipboard("username", tvUsername.text.toString()) }
        btnCopyPassword.setOnClickListener { copyToClipboard("password", plainPassword) }
        btnCopyTotp.setOnClickListener {
            copyToClipboard("totp", tvTotpCode.text.toString().replace(" ", ""))
        }
    }

    private fun loadEntry(id: Long) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val key = SessionManager.getSessionKey() ?: run { runOnUiThread { finish() }; return@execute }
                val entry = repository.getEntryById(id, key)
                runOnUiThread {
                    if (entry != null) populateUI(entry) else finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Error loading entry", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun populateUI(entry: VaultEntry) {
        passwordVisible = false
        plainPassword = entry.encryptedPassword

        tvToolbarTitle.text = entry.title
        tvCategory.text = entry.category
        tvUsername.text = entry.username
        tvPassword.text = "••••••••••••"
        btnTogglePassword.setImageResource(R.drawable.ic_eye_off)

        val hasWebsite = !entry.website.isNullOrEmpty()
        setVisible(hasWebsite, labelWebsite, tvWebsite)
        if (hasWebsite) tvWebsite.text = entry.website

        val hasNotes = !entry.notes.isNullOrEmpty()
        setVisible(hasNotes, labelNotes, tvNotes)
        if (hasNotes) tvNotes.text = entry.notes

        tvFavorite.visibility = if (entry.isFavorite) View.VISIBLE else View.GONE

        val hasTOTP = entry.hasTOTP()
        setVisible(hasTOTP, labelTotp, totpRow, progressBarTotp)

        if (hasTOTP) startTotpUpdater(entry) else stopTotpUpdater()
    }

    private fun startTotpUpdater(entry: VaultEntry) {
        stopTotpUpdater()
        totpHandler = Handler(Looper.getMainLooper())
        totpRunnable = object : Runnable {
            override fun run() {
                try {
                    val secret = entry.totpSecret ?: return
                    val code = TOTPGenerator.generate(secret,
                        entry.totpDigits,
                        entry.totpPeriod,
                        entry.totpAlgorithm
                    )
                    val display = if (code.length == 6)
                        "${code.substring(0, 3)} ${code.substring(3)}" else code
                    val secsLeft = TOTPGenerator.getSecondsRemaining(entry.totpPeriod)
                    tvTotpCode.text = display
                    progressBarTotp.max = entry.totpPeriod
                    progressBarTotp.progress = secsLeft
                } catch (e: Exception) {
                    tvTotpCode.text = "ERR"
                }
                totpHandler?.postDelayed(this, 1000)
            }
        }
        totpHandler?.post(totpRunnable!!)
    }

    private fun stopTotpUpdater() {
        totpRunnable?.let { totpHandler?.removeCallbacks(it) }
        totpHandler = null
        totpRunnable = null
    }

    private fun copyToClipboard(label: String, value: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun setVisible(visible: Boolean, vararg views: View) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        views.forEach { it.visibility = visibility }
    }
}
