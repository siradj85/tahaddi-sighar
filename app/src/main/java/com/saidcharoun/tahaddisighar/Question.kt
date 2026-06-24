package com.saidcharoun.tahaddisighar

data class Question(
    val emoji: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val age: Int,
    val difficulty: Int
) {
    val category: String get() = categorizeByEmoji(emoji)
}

enum class AgeGroup(val code: Int, val label: String, val emoji: String) {
    YOUNG(1, "مبتدئ", "🌱"),
    OLDER(2, "متوسط", "⭐"),
    NINE_TWELVE(3, "متقدّم", "🔥"),
    FAMILY(4, "خبير", "🧠")
}

fun categorizeByEmoji(emoji: String): String = when {
    emoji.matches(Regex(".*[🐱🐶🐄🐑🐓🦆🐝🦁🐘🐰🐟🐦🐭🐢🐜🦒🐔🐗🐴🐸🐍🦈🐊🦅🐧🐼🐨🦊🐻].*")) -> "حيوانات"
    emoji.matches(Regex(".*[🍎🍌🍊🍓🍇🍉🍅🍫🍪🍕🥕🥦🍩🧀🥛🍼🍽️🍴🥄🍵☕🧃🧊🍚🍝🍜🍛🍣🍤🥟🍦🍰🧁🍭🍬🍫🍿🥜🌰🥗🥙🌮🌯🥪🍳🥞🧇🥓🍔🌭🍟🥨🥖🥐🍞🧀🥚🥣🥡🥢].*")) -> "طعام"
    emoji.matches(Regex(".*[☁️🌿🌙☀️🌧️❄️🌸🌳🌷🌈🌊🔥🌞🌟🌍🌎🌏🌪️🌫️☀️🌤️⛅🌥️☁️🌦️🌧️⛈️🌩️🌨️❄️☃️🏔️🌋🗻🏕️🏖️🏜️🏝️🏞️🌅🌄🌠🎇🎆🌌🌉🌁].*")) -> "طبيعة"
    emoji.matches(Regex(".*[⚪🔺🟦⭐❤️🔵🟢🟡🟠🟣🟤🔴🟩🟧🟨⬛⬜🔶🔷🔸🔹].*")) -> "أشكال وألوان"
    emoji.matches(Regex(".*[👁️👂👃✋🦷🦶👅👀👄💪🦵🧠🫀🫁🦴👐🙌👏🤲🖐️✌️🤞👍👎👊✊🤛🤜👆👇👈👉🤚🖐️✋👌🤌🤏].*")) -> "جسم الإنسان"
    emoji.matches(Regex(".*[🔢➕➖✍️🔤📅🗓️🕐⏰📊📈📉🧮📏📐📖📕📗📘📙📚📓📔📒📃📜📄🧾✏️✒️🖊️🖋️🖌️🖍️✂️🔗📎🖇️].*")) -> "أعداد وتعليم"
    emoji.matches(Regex(".*[🚗🚌✈️🚢🚁🚂🚃🚄🚅🚆🚇🚈🚉🚊🚝🚞🚋🚌🚍🚎🚐🚑🚒🚓🚔🚕🚖🚗🚘🚙🚚🚛🚜🛴🛵🛺🚲🛩️🛫🛬🪂🚀🛸🏍️🛵🛹🛼🚏⛽🅿️🛤️🛣️].*")) -> "وسائل نقل"
    emoji.matches(Regex(".*[🔢].*")) -> "أعداد"
    emoji.contains("🔢") -> "أعداد"
    else -> "متنوع"
}

fun extractCategories(questions: List<Question>): List<String> {
    return questions.map { it.category }.distinct().sorted()
}
