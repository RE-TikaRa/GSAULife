package com.tika.gsaulife.academic.model

data class GradeStats(
    val avgScore: Double,
    val avgGradePoint: Double,
    val totalCredit: Double,
    val scoredCredit: Double,
    val courseCount: Int,
) {
    companion object {
        fun of(grades: List<Grade>): GradeStats {
            var scoreWeighted = 0.0
            var scoredCredit = 0.0
            var gradePointWeighted = 0.0
            var gradePointCredit = 0.0
            var totalCredit = 0.0
            for (grade in grades) {
                totalCredit += grade.credit
                grade.numericScore?.let {
                    scoreWeighted += it * grade.credit
                    scoredCredit += grade.credit
                }
                grade.gradePoint?.let {
                    gradePointWeighted += it * grade.credit
                    gradePointCredit += grade.credit
                }
            }
            return GradeStats(
                avgScore = if (scoredCredit > 0) scoreWeighted / scoredCredit else 0.0,
                avgGradePoint =
                    if (gradePointCredit > 0) gradePointWeighted / gradePointCredit else 0.0,
                totalCredit = totalCredit,
                scoredCredit = scoredCredit,
                courseCount = grades.size,
            )
        }
    }
}

data class TermGrades(val term: String, val grades: List<Grade>) {
    val stats: GradeStats = GradeStats.of(grades)
}

fun groupByTerm(grades: List<Grade>): List<TermGrades> {
    val grouped = LinkedHashMap<String, MutableList<Grade>>()
    for (grade in grades) grouped.getOrPut(grade.term) { mutableListOf() }.add(grade)
    return grouped.keys.sortedDescending().map { TermGrades(it, grouped.getValue(it)) }
}
