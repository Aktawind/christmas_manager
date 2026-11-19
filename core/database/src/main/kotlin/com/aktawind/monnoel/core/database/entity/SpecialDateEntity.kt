package com.aktawind.monnoel.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aktawind.monnoel.core.model.SpecialDate

@Entity(tableName = "special_date")
data class SpecialDateEntity(@PrimaryKey val id: String, val month: Int, val day: Int, val label: String, val emoji: String)

fun SpecialDateEntity.asModel() = SpecialDate(id, month, day, label, emoji)

fun SpecialDate.asEntity() = SpecialDateEntity(id, month, day, label, emoji)
