package com.example.alarm

import kotlin.random.Random

data class MathQuestion(
    val expression: String,
    val answer: Int,
    val level: Int
)

object QuestionGenerator {

    /**
     * Generates an arithmetic question based on the hidden session level (1..5+).
     * All answers are strictly integers, formatted with clear spacing: "27 + 48 × 3 = ?"
     */
    fun generateQuestion(level: Int, random: Random = Random.Default): MathQuestion {
        val clampedLevel = level.coerceAtLeast(1)
        return when (clampedLevel) {
            1 -> generateLevel1(random)
            2 -> generateLevel2(random)
            3 -> generateLevel3(random)
            4 -> generateLevel4(random)
            else -> generateLevel5(random)
        }
    }

    // Level 1: Gentle single/two-step arithmetic with clear integer division or 1-digit multiplication
    // Examples: 27 + 48 × 3, 34 + 26 × 2, 72 - 18 ÷ 3, 45 × 4 + 17, 96 ÷ 4 + 38
    private fun generateLevel1(random: Random): MathQuestion {
        return when (random.nextInt(5)) {
            0 -> {
                // a + b × c
                val a = random.nextInt(15, 55)
                val b = random.nextInt(12, 38)
                val c = random.nextInt(2, 5)
                val answer = a + (b * c)
                MathQuestion("$a + $b × $c = ?", answer, 1)
            }
            1 -> {
                // a × b + c
                val a = random.nextInt(12, 35)
                val b = random.nextInt(2, 5)
                val c = random.nextInt(15, 50)
                val answer = (a * b) + c
                MathQuestion("$a × $b + $c = ?", answer, 1)
            }
            2 -> {
                // a - b ÷ c
                val c = random.nextInt(2, 7) // divisor
                val quotient = random.nextInt(4, 16)
                val b = quotient * c
                val a = random.nextInt(b + 20, b + 60)
                val answer = a - quotient
                MathQuestion("$a − $b ÷ $c = ?", answer, 1)
            }
            3 -> {
                // a ÷ b + c
                val b = random.nextInt(2, 6)
                val quotient = random.nextInt(14, 30)
                val a = quotient * b
                val c = random.nextInt(15, 45)
                val answer = quotient + c
                MathQuestion("$a ÷ $b + $c = ?", answer, 1)
            }
            else -> {
                // a + b × c - d
                val a = random.nextInt(20, 45)
                val b = random.nextInt(12, 28)
                val c = random.nextInt(2, 4)
                val d = random.nextInt(10, 25)
                val answer = a + (b * c) - d
                MathQuestion("$a + $b × $c − $d = ?", answer, 1)
            }
        }
    }

    // Level 2: Moderate arithmetic, two-digit by single-digit or slightly larger combinations
    // Example: 34 × 6 + 27, 85 - 14 × 4
    private fun generateLevel2(random: Random): MathQuestion {
        return when (random.nextInt(3)) {
            0 -> {
                val a = random.nextInt(24, 65)
                val b = random.nextInt(5, 9)
                val c = random.nextInt(25, 75)
                val answer = (a * b) + c
                MathQuestion("$a × $b + $c = ?", answer, 2)
            }
            1 -> {
                val a = random.nextInt(15, 45)
                val b = random.nextInt(25, 55)
                val c = random.nextInt(4, 8)
                val answer = a + (b * c)
                MathQuestion("$a + $b × $c = ?", answer, 2)
            }
            else -> {
                val b = random.nextInt(12, 25)
                val c = random.nextInt(3, 6)
                val prod = b * c
                val a = random.nextInt(prod + 30, prod + 90)
                val answer = a - prod
                MathQuestion("$a − $b × $c = ?", answer, 2)
            }
        }
    }

    // Level 3: Two 2-digit factors minus a 3-digit constant or multi-term
    // Example: 84 × 17 − 329
    private fun generateLevel3(random: Random): MathQuestion {
        val a = random.nextInt(40, 90)
        val b = random.nextInt(12, 25)
        val product = a * b
        // Pick c such that answer is positive and clean
        val maxC = (product - 50).coerceAtLeast(100)
        val minC = (product / 3).coerceAtMost(maxC - 20)
        val c = random.nextInt(minC, maxC)
        val answer = product - c
        return MathQuestion("$a × $b − $c = ?", answer, 3)
    }

    // Level 4: Parenthesized double products
    // Example: (47 × 18) + (32 × 7) − 84
    private fun generateLevel4(random: Random): MathQuestion {
        val a = random.nextInt(30, 65)
        val b = random.nextInt(12, 22)
        val c = random.nextInt(20, 45)
        val d = random.nextInt(5, 12)
        val prod1 = a * b
        val prod2 = c * d
        val sub = random.nextInt(50, 150)
        val answer = prod1 + prod2 - sub
        return MathQuestion("($a × $b) + ($c × $d) − $sub = ?", answer, 4)
    }

    // Level 5+: Complex bracketed multi-digit expression
    // Example: (125 × 24) − (87 × 13) + 296
    private fun generateLevel5(random: Random): MathQuestion {
        val a = random.nextInt(100, 160)
        val b = random.nextInt(18, 28)
        val c = random.nextInt(60, 95)
        val d = random.nextInt(11, 19)
        val prod1 = a * b
        val prod2 = c * d
        val add = random.nextInt(150, 450)
        val answer = prod1 - prod2 + add
        return MathQuestion("($a × $b) − ($c × $d) + $add = ?", answer, 5)
    }
}
