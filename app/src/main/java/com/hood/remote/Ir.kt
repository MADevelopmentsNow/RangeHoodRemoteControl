package com.hood.remote

object Ir {
    data class Code(val proto: String, val addr: Int, val cmd: Int)

    val protocols = listOf("NEC", "NEC-EXT", "SAMSUNG", "SONY12", "RC5")

    fun carrier(p: String) = when (p) { "SONY12" -> 40000; "RC5" -> 36000; else -> 38000 }

    private fun cmdMax(p: String) = when (p) { "SONY12" -> 128; "RC5" -> 64; else -> 256 }

    private fun addrList(p: String, full: Boolean): List<Int> = when (p) {
        "NEC" -> if (full) (0..255).toList()
            else listOf(0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0xFF, 0x7F, 0xFE, 0x03, 0x06, 0x0C, 0x18)
        "NEC-EXT" -> {
            val c = listOf(0x0000, 0x00FF, 0xFF00, 0x0001, 0x0100, 0x1010, 0x2020, 0x4040, 0x8080, 0x00F7, 0x08F7, 0x01FE)
            if (full) (c + (0..255)).distinct() else c
        }
        "SAMSUNG" -> if (full) (0..255).toList() else listOf(0x07, 0x0E, 0x00, 0x01, 0x02, 0x04, 0x08, 0x10)
        "SONY12" -> if (full) (0..31).toList() else listOf(1, 0, 2, 3, 4, 5, 8, 16)
        else -> if (full) (0..31).toList() else listOf(0, 1, 2, 5, 16, 17, 20, 21)
    }

    fun build(proto: String, full: Boolean, fixedAddr: Int?): List<Code> {
        val out = ArrayList<Code>()
        for (p in if (proto == "ALL") protocols else listOf(proto)) {
            val addrs = if (fixedAddr != null) listOf(fixedAddr) else addrList(p, full)
            for (a in addrs) for (c in 0 until cmdMax(p)) out.add(Code(p, a, c))
        }
        return out
    }

    private fun MutableList<Int>.pair(m: Int, s: Int) { add(m); add(s) }

    private fun frame(c: Code, toggle: Int): List<Int> {
        val l = ArrayList<Int>()
        when (c.proto) {
            "NEC", "NEC-EXT" -> {
                l.pair(9000, 4500)
                val a = if (c.proto == "NEC") (c.addr and 0xFF) or ((c.addr.inv() and 0xFF) shl 8) else (c.addr and 0xFFFF)
                val d = (c.cmd and 0xFF) or ((c.cmd.inv() and 0xFF) shl 8)
                val v = a or (d shl 16)
                for (i in 0 until 32) l.pair(560, if (((v ushr i) and 1) == 1) 1690 else 560)
                l.add(560)
            }
            "SAMSUNG" -> {
                l.pair(4500, 4500)
                val a = c.addr and 0xFF
                val v = a or (a shl 8) or ((c.cmd and 0xFF) shl 16) or ((c.cmd.inv() and 0xFF) shl 24)
                for (i in 0 until 32) l.pair(560, if (((v ushr i) and 1) == 1) 1690 else 560)
                l.add(560)
            }
            "SONY12" -> {
                l.pair(2400, 600)
                val v = (c.cmd and 0x7F) or ((c.addr and 0x1F) shl 7)
                for (i in 0 until 12) {
                    l.add(if (((v ushr i) and 1) == 1) 1200 else 600)
                    l.add(600)
                }
                l.removeAt(l.size - 1)
            }
            else -> { // RC5
                val field = if (c.cmd < 64) 1 else 0
                val bits = ArrayList<Int>()
                bits.add(1); bits.add(field); bits.add(toggle and 1)
                for (i in 4 downTo 0) bits.add((c.addr ushr i) and 1)
                for (i in 5 downTo 0) bits.add((c.cmd ushr i) and 1)
                val levels = ArrayList<Boolean>() // true = mark
                for (b in bits) { if (b == 1) { levels.add(false); levels.add(true) } else { levels.add(true); levels.add(false) } }
                val runs = ArrayList<Pair<Boolean, Int>>()
                for (lv in levels) {
                    if (runs.isNotEmpty() && runs.last().first == lv) runs[runs.size - 1] = lv to (runs.last().second + 889)
                    else runs.add(lv to 889)
                }
                if (!runs.first().first) runs.removeAt(0)
                if (!runs.last().first) runs.removeAt(runs.size - 1)
                for (r in runs) l.add(r.second)
            }
        }
        return l
    }

    /** Full transmit pattern with repeated frames. */
    fun full(c: Code, toggle: Int): IntArray {
        val reps = if (c.proto == "SONY12") 3 else 1
        val gap = if (c.proto == "SONY12") 20000 else 40000
        val out = ArrayList<Int>()
        repeat(reps) { i ->
            if (i > 0) out.add(gap)
            out.addAll(frame(c, toggle))
        }
        return out.toIntArray()
    }
}
