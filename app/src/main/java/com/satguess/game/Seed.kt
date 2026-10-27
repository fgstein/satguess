package com.satguess.game

import kotlin.random.Random

enum class Mode { MOVE, NO_MOVE }

/**
 * Seed d'une partie (specs §16) : 40 bits = 1 bit mode + 7 bits version de la liste + 32 bits aléatoires,
 * affichés en 8 caractères base32 Crockford groupés `XXXX-XXXX`.
 */
data class Seed(val mode: Mode, val listVersion: Int, val value: Long) {
    init {
        require(listVersion in 0..MAX_VERSION) { "version hors limites : $listVersion" }
        require(value in 0..0xFFFF_FFFFL) { "valeur hors limites : $value" }
    }

    fun encode(): String {
        var bits = (if (mode == Mode.NO_MOVE) 1L else 0L) shl 39 or (listVersion.toLong() shl 32) or value
        val chars = CharArray(8)
        for (i in 7 downTo 0) {
            chars[i] = ALPHABET[(bits and 31).toInt()]
            bits = bits ushr 5
        }
        return String(chars, 0, 4) + "-" + String(chars, 4, 4)
    }

    override fun toString() = encode()

    companion object {
        const val MAX_VERSION = 127
        private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

        fun random(mode: Mode, listVersion: Int, random: Random = Random.Default) =
            Seed(mode, listVersion, random.nextLong(0, 0x1_0000_0000L))

        /** Décode une seed saisie par l'utilisateur ; null si elle est invalide. */
        fun parse(input: String): Seed? {
            val cleaned = input.uppercase()
                .filterNot { it == '-' || it.isWhitespace() }
                .map {
                    when (it) {
                        'O' -> '0'
                        'I', 'L' -> '1'
                        else -> it
                    }
                }
            if (cleaned.size != 8) return null
            var bits = 0L
            for (c in cleaned) {
                val index = ALPHABET.indexOf(c)
                if (index < 0) return null
                bits = bits shl 5 or index.toLong()
            }
            val mode = if (bits ushr 39 and 1L == 1L) Mode.NO_MOVE else Mode.MOVE
            val version = (bits ushr 32 and 0x7F).toInt()
            return Seed(mode, version, bits and 0xFFFF_FFFFL)
        }
    }
}

/** Générateur SplitMix64 : déterministe et identique sur toutes les plateformes. */
class SplitMix64(private var state: Long) {
    fun nextLong(): Long {
        state += -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L // 0x94D049BB133111EB
        return z xor (z ushr 31)
    }

    /** Entier dans [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        return ((nextLong() ushr 1) % bound).toInt()
    }
}

/**
 * Tire [count] éléments selon la seed : mélange de Fisher-Yates partiel sur la liste triée par id.
 * Le tri est fait ici pour que le résultat ne dépende pas de l'ordre du fichier.
 */
fun <T> pickForSeed(items: List<T>, seed: Seed, count: Int, id: (T) -> String): List<T> {
    val pool = items.sortedBy(id).toMutableList()
    val rng = SplitMix64(seed.value)
    val n = minOf(count, pool.size)
    for (i in 0 until n) {
        val j = i + rng.nextInt(pool.size - i)
        pool[i] = pool[j].also { pool[j] = pool[i] }
    }
    return pool.subList(0, n).toList()
}
