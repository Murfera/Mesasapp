package br.com.mesasapp.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Datas sao gravadas como **epoch em segundos** (UTC) para que a sobreposicao de
 * janelas seja uma comparacao numerica no SQL. A conversao entra e sai so por aqui.
 */
object Datas {

    private val zona: ZoneId get() = ZoneId.systemDefault()

    private val DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val DATA_CURTA = DateTimeFormatter.ofPattern("dd/MM")
    private val HORA = DateTimeFormatter.ofPattern("HH:mm")

    fun paraEpochSeg(data: LocalDate, hora: LocalTime): Long =
        LocalDateTime.of(data, hora).atZone(zona).toEpochSecond()

    fun paraLocal(epochSeg: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeg), zona)

    fun formatarData(epochSeg: Long): String = paraLocal(epochSeg).format(DATA)
    fun formatarDataCurta(epochSeg: Long): String = paraLocal(epochSeg).format(DATA_CURTA)
    fun formatarHora(epochSeg: Long): String = paraLocal(epochSeg).format(HORA)
    fun formatarDataHora(epochSeg: Long): String =
        "${formatarData(epochSeg)} ${formatarHora(epochSeg)}"

    fun formatar(data: LocalDate): String = data.format(DATA)
    fun formatar(hora: LocalTime): String = hora.format(HORA)

    fun agoraEpochSeg(): Long = Instant.now().epochSecond

    /** Dias que a locacao dura, minimo 1 (meio periodo tambem e uma diaria). */
    fun dias(inicioEpochSeg: Long, fimEpochSeg: Long): Int {
        val inicio = paraLocal(inicioEpochSeg).toLocalDate()
        val fim = paraLocal(fimEpochSeg).toLocalDate()
        val diferenca = ChronoUnit.DAYS.between(inicio, fim).toInt()
        return if (diferenca < 1) 1 else diferenca
    }

    /** Converte um epoch em milissegundos do DatePicker para a data local. */
    fun dataDeMillisUtc(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()

    /** O inverso: o DatePicker do Material espera meia-noite UTC. */
    fun millisUtcDeData(data: LocalDate): Long =
        data.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}
