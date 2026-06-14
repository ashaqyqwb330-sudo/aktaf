package com.example.database

import kotlin.random.Random

object QuizGenerator {

    fun generateMCQ(text: String, quizId: Long, count: Int = 5): List<QuestionEntity> {
        val sentences = text.split(".", "؟", "،", "\n")
            .map { it.trim() }
            .filter { it.length > 35 && it.contains(" ") }
        val questions = mutableListOf<QuestionEntity>()

        var sentenceIndex = 0
        var attempts = 0
        while (questions.size < count && sentenceIndex < sentences.size && attempts < 100) {
            attempts++
            val sentence = sentences[sentenceIndex]
            sentenceIndex++

            val words = sentence.split(" ")
                .map { it.replace(Regex("[.\\n\\r,،؟?!]"), "").trim() }
                .filter { it.length > 3 && !it.contains("____") }

            if (words.size < 4) continue

            val correctWord = words.random()
            // Find distinct distractors from the text or other words
            val otherWords = text.split(Regex("\\s+"))
                .map { it.replace(Regex("[.\\n\\r,،؟?!]"), "").trim() }
                .filter { it.length > 3 && it != correctWord && !it.contains("____") && it.matches(Regex("^[\\u0600-\\u06FF]+$")) }
                .distinct()

            val distFiltered = otherWords.shuffled().take(3)
            if (distFiltered.size < 3) continue

            // Randomize position of correct answer
            val slots = (distFiltered + correctWord).shuffled()
            val opA = slots.getOrElse(0) { "غير ذلك" }
            val opB = slots.getOrElse(1) { "غير ذلك" }
            val opC = slots.getOrElse(2) { "غير ذلك" }
            val opD = slots.getOrElse(3) { "غير ذلك" }

            questions.add(
                QuestionEntity(
                    quizId = quizId,
                    question = "أكمل العبارة التالية: " + sentence.replace(correctWord, "______"),
                    optionA = opA,
                    optionB = opB,
                    optionC = opC,
                    optionD = opD,
                    correctAnswer = correctWord,
                    type = "mcq"
                )
            )
        }

        // Fallback generator if text is too short or doesn't yield enough MCQ questions
        val fallbackTopics = listOf("الوعي والجهاد", "نور الإيمان", "بصائر الهدي", "العزة الإيمانية", "العدالة والتقوى")
        while (questions.size < count) {
            val correctWord = fallbackTopics.random()
            val distractors = fallbackTopics.filter { it != correctWord }.shuffled().take(3)
            val slots = (distractors + correctWord).shuffled()
            questions.add(
                QuestionEntity(
                    quizId = quizId,
                    question = "من الركائز الأساسية للهوية والتثقيف القرآني هي ______ ونور الاستقامة.",
                    optionA = slots[0],
                    optionB = slots[1],
                    optionC = slots[2],
                    optionD = slots[3],
                    correctAnswer = correctWord,
                    type = "mcq"
                )
            )
        }

        return questions.take(count)
    }

    fun generateTrueFalse(text: String, quizId: Long, count: Int = 5): List<QuestionEntity> {
        val sentences = text.split(".", "؟", "،", "\n")
            .map { it.trim() }
            .filter { it.length > 25 && it.contains(" ") }
        val questions = mutableListOf<QuestionEntity>()

        var sentenceIndex = 0
        var attempts = 0
        while (questions.size < count && sentenceIndex < sentences.size && attempts < 100) {
            attempts++
            val sentence = sentences[sentenceIndex]
            sentenceIndex++

            val isTrue = Random.nextBoolean()
            val finalQuestion: String
            val correctAnswer: String

            if (isTrue) {
                finalQuestion = sentence
                correctAnswer = "صح"
            } else {
                // Introduce negation or change context slightly
                val words = sentence.split(" ").toMutableList()
                if (words.size > 4) {
                    val changeIndex = words.size / 2
                    val wordToChange = words[changeIndex]
                    if (!wordToChange.startsWith("لا")) {
                        words[changeIndex] = "لا " + wordToChange
                    } else {
                        words[changeIndex] = wordToChange.substring(2)
                    }
                    finalQuestion = words.joinToString(" ")
                    correctAnswer = "خطأ"
                } else {
                    continue
                }
            }

            questions.add(
                QuestionEntity(
                    quizId = quizId,
                    question = "هل هذه العبارة صحيحة أم خاطئة؟\n\n\"$finalQuestion\"",
                    optionA = "صح",
                    optionB = "خطأ",
                    correctAnswer = correctAnswer,
                    type = "truefalse"
                )
            )
        }

        // Fallback for True/False
        val fallbackPairs = listOf(
            "الصبر والعمل الصالح هما درع المؤمن في مواجهة الصعاب." to "صح",
            "الوعي بالثقافات المغلوطة ليس ضرورياً لبناء المجتمع الإسلامي." to "خطأ",
            "الثقة بالله والتوكل عليه هما مصدر القوة المعنوية لرجال الله." to "صح",
            "التمسك بالقرآن الكريم يغني عن كل الثقاقات المنحرفة والمضللة." to "صح",
            "يتحقق النصر والتأييد الإلهي بالأماني دون الحاجة للأخذ بالأسباب." to "خطأ"
        )
        var fallbackIndex = 0
        while (questions.size < count) {
            val pair = fallbackPairs[fallbackIndex % fallbackPairs.size]
            fallbackIndex++
            questions.add(
                QuestionEntity(
                    quizId = quizId,
                    question = "هل هذه العبارة صحيحة أم خاطئة؟\n\n\"${pair.first}\"",
                    optionA = "صح",
                    optionB = "خطأ",
                    correctAnswer = pair.second,
                    type = "truefalse"
                )
            )
        }

        return questions.take(count)
    }

    fun generateFillBlank(text: String, quizId: Long, count: Int = 5): List<QuestionEntity> {
        val sentences = text.split(".", "؟", "،", "\n")
            .map { it.trim() }
            .filter { it.length > 30 && it.contains(" ") }
        val questions = mutableListOf<QuestionEntity>()

        var sentenceIndex = 0
        var attempts = 0
        while (questions.size < count && sentenceIndex < sentences.size && attempts < 100) {
            attempts++
            val sentence = sentences[sentenceIndex]
            sentenceIndex++

            val words = sentence.split(" ")
                .map { it.replace(Regex("[.\\n\\r,،؟?!]"), "").trim() }
                .filter { it.length > 4 }

            if (words.size < 4) continue
            val blankWord = words[words.size / 2]

            questions.add(
                QuestionEntity(
                    quizId = quizId,
                    question = "أكمل الفراغ بالكلمة المناسبة:\n\n" + sentence.replace(blankWord, "______"),
                    optionA = "",
                    optionB = "",
                    optionC = "",
                    optionD = "",
                    correctAnswer = blankWord,
                    type = "fillblank"
                )
            )
        }

        // Fallback for fill blanks
        val fallbackBlanks = listOf(
            "إن الله يعطي الحكمة من يشاء من عباده المخلصين." to "الحكمة",
            "التمسك بالهوية الإيمانية هو طوق النجاة من كيد الأعداء." to "الهوية",
            "اليقين بوعد الله يثمر الصبر والجهاد في سبيله." to "اليقين",
            "المسيرة القرآنية هي مسار الهدى والبصيرة للأمة." to "المسيرة",
            "الجهاد بالمال والنفس ذروة سنام الإسلام ومصدر العزة." to "الجهاد"
        )
        var fallbackIndex = 0
        while (questions.size < count) {
            val pair = fallbackBlanks[fallbackIndex % fallbackBlanks.size]
            fallbackIndex++
            questions.add(
                QuestionEntity(
                    quizId = quizId,
                    question = "أكمل الفراغ بالكلمة المناسبة:\n\n" + pair.first.replace(pair.second, "______"),
                    optionA = "",
                    optionB = "",
                    optionC = "",
                    optionD = "",
                    correctAnswer = pair.second,
                    type = "fillblank"
                )
            )
        }

        return questions.take(count)
    }

    fun generateCrossword(text: String): List<CrosswordEntry> {
        val words = text.split(Regex("\\s+"))
            .map { it.replace(Regex("[.\\n\\r,،؟?!]"), "").trim() }
            .filter { it.length in 3..6 && it.matches(Regex("^[\\u0600-\\u06FF]+$")) }
            .distinct()
            .take(10)

        val defaultEntries = listOf(
            CrosswordEntry("يقين", "الثقة التامة والاعتقاد الراسخ بالشيء", "أفقي", 1, 1),
            CrosswordEntry("جهاد", "بذل الجهد دفاعاً عن الحق ونصرة له", "رأسي", 2, 2),
            CrosswordEntry("بصيرة", "نور العقل والفطرة الذي يميز الحق والضلال", "أفقي", 3, 1),
            CrosswordEntry("صبر", "تحمل الصعاب والمشاق في سبيل نيل رضا الله", "رأسي", 4, 3),
            CrosswordEntry("هدى", "الدلالة بلطف إلى ما فيه الخير والصلاح", "أفقي", 5, 2)
        )

        if (words.size < 5) return defaultEntries

        return words.mapIndexed { index, word ->
            val dir = if (index % 2 == 0) "أفقي" else "رأسي"
            val clue = when (word) {
                "صبر" -> "تحمل الصعاب والمشاق في سبيل نيل رضا الله"
                "يقين" -> "الثقة التامة والاعتقاد الراسخ"
                "قرآن" -> "كتاب الله المنزل على نبيه محمد صلى الله عليه وسلم"
                "تقوى" -> "الوقاية من عذاب الله بفعل أوامره واجتناب نواهيه"
                "إيمان" -> "التصديق بالقلب والإقرار باللسان وعمل الجوارح"
                "جهاد" -> "بذل القدرة والطاقة في نصرة دين الله"
                else -> "كلمة من السليقة والدروس التوعوية للدرس (${word.length} أحرف)"
            }
            CrosswordEntry(
                word = word,
                clue = clue,
                direction = dir,
                startRow = index + 1,
                startCol = (index % 3) + 1
            )
        }
    }

    data class CrosswordEntry(
        val word: String,
        val clue: String,
        val direction: String,
        val startRow: Int,
        val startCol: Int
    )
}
