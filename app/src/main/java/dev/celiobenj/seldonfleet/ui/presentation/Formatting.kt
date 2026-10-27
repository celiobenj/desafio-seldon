package dev.celiobenj.seldonfleet.ui.presentation

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// Formatação de números e datas para exibição, em português (vírgula decimal).
// Kotlin puro: testável na JVM, sem Context.

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

private val LAST_SEEN_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm 'UTC'").withZone(ZoneOffset.UTC)

/** Número com [decimals] casas e vírgula decimal, sem separador de milhar (ex.: "1585,7"). */
fun formatDecimal(value: Double, decimals: Int = 1): String = String.format(PT_BR, "%.${decimals}f", value)

/** Limiar inteiro sem casas ("12"); fracionário com 1 casa ("7,5"). */
fun formatThreshold(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else formatDecimal(value)

/** Último contato em UTC ("20/10/2026 02:58 UTC"), ou "—" se ausente ou inválido no arquivo. */
fun formatLastSeen(instant: Instant?): String = instant?.let { LAST_SEEN_FORMAT.format(it) } ?: "—"
