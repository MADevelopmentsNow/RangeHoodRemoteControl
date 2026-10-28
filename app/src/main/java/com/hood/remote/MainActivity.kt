package com.hood.remote

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.*

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private var ir: ConsumerIrManager? = null
    private var toggle = 0
    private lateinit var root: FrameLayout

    private val keys = listOf(
        "power" to "ON/OFF",
        "light" to "💡 Light",
        "minus" to "− Fan slower",
        "plus" to "+ Fan faster",
        "mode" to "AUTO",
        "timer" to "⏱ Timer"
    )

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        ir = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        root = FrameLayout(this)
        root.setOnApplyWindowInsetsListener { v, ins ->
            val i = ins.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(i.left, i.top, i.right, i.bottom)
            WindowInsets.CONSUMED
        }
        setContentView(root)
        showRemote()
    }

    override fun onPause() { super.onPause(); h.removeCallbacksAndMessages(null) }

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private fun page(): LinearLayout {
        h.removeCallbacksAndMessages(null)
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.setPadding(dp(16), dp(16), dp(16), dp(16))
        val sv = ScrollView(this)
        sv.addView(l)
        root.removeAllViews()
        root.addView(sv)
        return l
    }

    private fun text(l: LinearLayout, s: String, size: Float = 16f): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setPadding(0, dp(6), 0, dp(6))
        l.addView(t)
        return t
    }

    private fun button(l: LinearLayout, s: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = s
        b.isAllCaps = false
        b.setOnClickListener { onClick() }
        l.addView(b, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return b
    }

    private fun load(key: String): Ir.Code? =
        getSharedPreferences("hood", MODE_PRIVATE).getString(key, null)?.split("|")?.let {
            Ir.Code(it[0], it[1].toInt(), it[2].toInt())
        }

    private fun send(c: Ir.Code): Boolean {
        val m = ir
        if (m == null || !m.hasIrEmitter()) { toast("No IR blaster found on this phone"); return false }
        toggle = toggle xor 1
        m.transmit(Ir.carrier(c.proto), Ir.full(c, toggle))
        return true
    }

    private val defaults = mapOf(
        "power" to Ir.Code("NEC", 0, 0x45),
        "light" to Ir.Code("NEC", 0, 0x47),
        "minus" to Ir.Code("NEC", 0, 0x07),
        "plus" to Ir.Code("NEC", 0, 0x09),
        "mode" to Ir.Code("NEC", 0, 0x40)
    )

    private fun codeFor(key: String): Ir.Code? = load(key) ?: defaults[key]
    private fun labelOf(key: String) = keys.first { it.first == key }.second

    private fun roundBtn(key: String, label: String, red: Boolean = false): TextView {
        val t = TextView(this)
        t.text = label
        t.gravity = Gravity.CENTER
        t.textSize = if (label.length > 2) 15f else 30f
        t.setTextColor(if (red) Color.WHITE else Color.BLACK)
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(if (red) 0xFFE53935.toInt() else 0xFFD8D8D8.toInt())
        t.background = g
        t.setOnClickListener {
            val c = codeFor(key)
            if (c != null) { t.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); send(c) }
            else showLearn(key, labelOf(key))
        }
        t.setOnLongClickListener { showLearn(key, labelOf(key)); true }
        return t
    }

    private fun showRemote() {
        val l = page()
        val ok = ir?.hasIrEmitter() == true
        val title = text(l, "Hooda RHR", 22f)
        title.gravity = Gravity.CENTER
        val st = text(l, if (ok) "Point the top edge of the phone at the hood" else "⚠ No IR blaster found", 13f)
        st.gravity = Gravity.CENTER
        val size = dp(84)
        fun cell(v: View?): View {
            val f = FrameLayout(this)
            f.layoutParams = LinearLayout.LayoutParams(0, size + dp(18), 1f)
            if (v != null) f.addView(v, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
            return f
        }
        fun row(a: View?, b: View?, c: View?) {
            val r = LinearLayout(this)
            r.orientation = LinearLayout.HORIZONTAL
            r.addView(cell(a)); r.addView(cell(b)); r.addView(cell(c))
            l.addView(r, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val gap = View(this)
        l.addView(gap, LinearLayout.LayoutParams(1, dp(16)))
        row(roundBtn("power", "ON/OFF", true), null, roundBtn("light", "💡"))
        row(null, roundBtn("mode", "AUTO"), null)
        row(roundBtn("minus", "−"), null, roundBtn("plus", "+"))
        row(null, roundBtn("timer", "⏱"), null)
        val hint = text(l, "Long-press any button to re-learn it.", 13f)
        hint.gravity = Gravity.CENTER
        button(l, "⚙ Settings") { showSettings() }
    }

    private fun showSettings() {
        val l = page()
        text(l, "Settings", 22f)
        text(l, "App icon", 16f)
        val pm = packageManager
        val dark = ComponentName(this, "com.hood.remote.IconDark")
        val light = ComponentName(this, "com.hood.remote.IconLight")
        val isDark = pm.getComponentEnabledSetting(dark) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        fun choose(d: Boolean) {
            pm.setComponentEnabledSetting(if (d) dark else light,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(if (d) light else dark,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            toast("Icon changed. Your home screen may take a few seconds to update.")
            showSettings()
        }
        fun option(resId: Int, name: String, selected: Boolean, d: Boolean) {
            val r = LinearLayout(this)
            r.orientation = LinearLayout.HORIZONTAL
            r.gravity = Gravity.CENTER_VERTICAL
            val iv = ImageView(this)
            iv.setImageResource(resId)
            r.addView(iv, LinearLayout.LayoutParams(dp(80), dp(80)))
            val b = Button(this)
            b.text = (if (selected) "✔ " else "") + name
            b.isAllCaps = false
            b.setOnClickListener { choose(d) }
            r.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            l.addView(r)
        }
        option(R.drawable.ic_fg_dark, "Dark (default)", isDark, true)
        option(R.drawable.ic_fg_light, "White", !isDark, false)
        text(l, "Buttons", 16f)
        button(l, "Reset buttons to default codes") {
            AlertDialog.Builder(this).setMessage("Delete custom saved codes?")
                .setPositiveButton("Delete") { _, _ ->
                    getSharedPreferences("hood", MODE_PRIVATE).edit().clear().apply(); showRemote()
                }.setNegativeButton("Cancel", null).show()
        }
        button(l, "← Back") { showRemote() }
    }

    private fun showLearn(key: String, label: String) {
        val l = page()
        text(l, "Learn: $label", 22f)
        text(l, "Hold the phone's top edge 1-2 m from the hood, facing it. Press Test (or Auto-scan) and watch the hood. " +
                "When it reacts (beep / light / fan), press \"It worked\". If Auto-scan went past it, press Stop, " +
                "then ◀ Prev until it reacts again.", 14f)

        val sp = Spinner(this)
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("ALL") + Ir.protocols)
        val sc = Spinner(this)
        sc.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Quick scan (common addresses)", "Full scan (all addresses)"))
        val ad = EditText(this)
        ad.hint = "Address in hex (optional)"
        ad.setSingleLine()

        val power = if (key != "power") codeFor("power") else null
        if (power != null) {
            sp.setSelection(1 + Ir.protocols.indexOf(power.proto))
            ad.setText("%X".format(power.addr))
            text(l, "Power was found as ${power.proto} address 0x%X, so only commands are scanned.".format(power.addr), 13f)
        }
        text(l, "Protocol", 13f); l.addView(sp)
        text(l, "Scan type", 13f); l.addView(sc)
        text(l, "Address", 13f); l.addView(ad)

        val delayLabel = text(l, "", 13f)
        val sb = SeekBar(this)
        sb.max = 28; sb.progress = 10
        fun delay() = 200 + sb.progress * 50
        delayLabel.text = "Auto-scan delay: ${delay()} ms per code"
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { delayLabel.text = "Auto-scan delay: ${delay()} ms per code" }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        l.addView(sb)

        val status = text(l, "Ready", 18f)

        var list: List<Ir.Code> = emptyList()
        var idx = -1
        var sig = ""
        var auto = false

        fun ensure() {
            val s = "${sp.selectedItemPosition}|${sc.selectedItemPosition}|${ad.text}"
            if (s != sig) {
                sig = s
                val a = ad.text.toString().trim().let { if (it.isEmpty()) null else it.toIntOrNull(16) }
                list = Ir.build(sp.selectedItem as String, sc.selectedItemPosition == 1, a)
                idx = -1
            }
        }
        fun show() {
            status.text = if (idx < 0) "Ready: ${list.size} codes"
            else { val c = list[idx]; "Code ${idx + 1} / ${list.size}\n${c.proto}  addr 0x%X  cmd 0x%X".format(c.addr, c.cmd) }
        }
        fun step(d: Int): Boolean {
            ensure()
            if (list.isEmpty()) { toast("No codes to try"); return false }
            val n = idx + d
            if (n < 0 || n >= list.size) { toast("End of list"); return false }
            idx = n
            val ok = send(list[idx])
            show()
            return ok
        }

        button(l, "▶ Test next code") { step(1) }
        button(l, "◀ Prev code") { step(-1) }
        button(l, "↻ Send this code again") { if (idx >= 0) send(list[idx]) }

        val autoBtn = button(l, "▶▶ Auto-scan") {}
        val run = object : Runnable {
            override fun run() {
                if (!auto) return
                if (step(1)) h.postDelayed(this, delay().toLong()) else { auto = false; autoBtn.text = "▶▶ Auto-scan" }
            }
        }
        autoBtn.setOnClickListener {
            auto = !auto
            autoBtn.text = if (auto) "■ Stop" else "▶▶ Auto-scan"
            if (auto) h.post(run) else h.removeCallbacksAndMessages(null)
        }

        button(l, "✔ It worked — save as $label") {
            if (idx < 0) { toast("Test a code first") } else {
                val c = list[idx]
                getSharedPreferences("hood", MODE_PRIVATE).edit()
                    .putString(key, "${c.proto}|${c.addr}|${c.cmd}").apply()
                toast("Saved")
                showRemote()
            }
        }
        button(l, "← Back") { showRemote() }
    }
}
