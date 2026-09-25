package app.vanta.launcher.domain.model

/** Optional Home tiles. All off by default so the mockup grid is what ships. */
enum class HomeModule { SEARCH, MEDIA, BATTERY, PEOPLE, QUICK_SETTINGS }

/** Quotes shown on the Home / Drawer / Focus quote tiles, cycled. */
val DefaultQuotes: List<String> = listOf(
    "A MORE INTERESTING LIFE IS POSSIBLE.",
    "A DISCIPLINED MIND CREATES A FREER LIFE.",
    "GOOD THINGS TAKE TIME.",
    "SAME PHONE. HIGHER STANDARDS.",
    "DISCIPLINE CREATES FREEDOM.",
    "SAME IDEAS. DIFFERENT REALITY.",
    "A BETTER YOU IS A BRIGHTER TOMORROW.",
    "LESS SCROLLING. MORE DOING."
)
