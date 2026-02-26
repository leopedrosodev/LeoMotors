package br.com.tec.tecmotors.presentation.common

import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val dateBrFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.forLanguageTag("pt-BR"))

fun todayBr(): String = LocalDate.now().format(dateBrFormatter)

fun parseDateBrOrIso(input: String): LocalDate? {
    val value = input.trim()
    return runCatching { LocalDate.parse(value, dateBrFormatter) }.getOrNull()
        ?: runCatching { LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
}

fun parseDecimal(input: String): Double? {
    val normalized = input.trim().replace(',', '.')
    return normalized.toDoubleOrNull()
}

fun formatDate(epochDay: Long): String {
    return runCatching {
        LocalDate.ofEpochDay(epochDay).format(dateBrFormatter)
    }.getOrDefault("-")
}

fun formatNumber(value: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"))
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2
    return formatter.format(value)
}

fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value)
}
