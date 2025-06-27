package com.cdcs.screens

import java.time.LocalDate
import java.time.LocalTime

data class Meeting(
    val title: String,
    val date: LocalDate,
    val time: LocalTime
)