package com.example.data.model

data class FormulaVariable(
    val symbol: String,
    val meaning: String
)

data class FormulaItem(
    val id: String,
    val title: String,
    val subject: String, // "فیزیک" یا "ریاضی"
    val category: String,
    val gradeLevel: String,
    val formula: String,
    val variables: List<FormulaVariable>,
    val usageTip: String
)
