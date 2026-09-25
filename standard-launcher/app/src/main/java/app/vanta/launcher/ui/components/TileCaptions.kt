package app.vanta.launcher.ui.components

/** Default mono captions under app tiles, in the voice of the mockups. Users can override per app. */
object TileCaptions {

    private val byPackage: Map<String, String> = mapOf(
        "com.whatsapp" to "MESSAGES / CALLS / COMMUNITY",
        "org.telegram.messenger" to "CHATS / CHANNELS / BOTS",
        "com.google.android.youtube" to "WATCH",
        "com.spotify.music" to "MUSIC",
        "com.android.chrome" to "BROWSE FURTHER",
        "com.brave.browser" to "BROWSE FURTHER",
        "com.duckduckgo.mobile.android" to "BROWSE PRIVATELY",
        "org.mozilla.firefox" to "BROWSE FURTHER",
        "com.reddit.frontpage" to "DISCOVER",
        "com.twitter.android" to "REAL TIME",
        "com.instagram.android" to "MOMENTS",
        "com.oplus.camera" to "CAPTURE EVERYTHING",
        "com.android.camera" to "CAPTURE EVERYTHING",
        "com.android.camera2" to "CAPTURE EVERYTHING",
        "com.google.android.GoogleCamera" to "CAPTURE EVERYTHING",
        "com.oneplus.gallery" to "PHOTOS / VIDEOS / MEMORIES",
        "com.coloros.gallery3d" to "PHOTOS / VIDEOS / MEMORIES",
        "com.google.android.apps.photos" to "PHOTOS / VIDEOS / MEMORIES",
        "com.android.gallery" to "PHOTOS / VIDEOS / MEMORIES",
        "com.google.android.keep" to "IDEAS / LISTS",
        "com.simplemobiletools.notes" to "IDEAS / LISTS",
        "com.android.settings" to "CONTROL",
        "com.google.android.apps.nbu.files" to "ORGANISE",
        "com.google.android.documentsui" to "ORGANISE",
        "com.android.documentsui" to "ORGANISE",
        "com.google.android.apps.maps" to "EXPLORE",
        "com.oneplus.calculator" to "TOOLS",
        "com.google.android.calculator" to "TOOLS",
        "com.oneplus.deskclock" to "TIME",
        "com.google.android.deskclock" to "TIME",
        "com.google.android.contacts" to "PEOPLE",
        "com.google.android.dialer" to "CALLS",
        "com.google.android.apps.messaging" to "TEXTS",
        "com.google.android.gm" to "MAIL",
        "ch.protonmail.android" to "MAIL",
        "com.Slack" to "WORK",
        "com.openai.chatgpt" to "CREATE / EXPLORE",
        "com.anthropic.claude" to "CREATE / EXPLORE",
        "com.google.android.apps.bard" to "CREATE / EXPLORE",
        "com.netflix.mediaclient" to "WATCH",
        "com.amazon.mShop.android.shopping" to "SHOP",
        "com.google.android.calendar" to "PLANS"
    )

    private val byKeyword: List<Pair<Regex, String>> = listOf(
        Regex("mail") to "MAIL",
        Regex("note|keep") to "IDEAS / LISTS",
        Regex("photo|gallery") to "PHOTOS / MEMORIES",
        Regex("music|spotify|audio|podcast") to "MUSIC",
        Regex("bank|pay|wallet|upi") to "MONEY",
        Regex("map|navigat") to "EXPLORE",
        Regex("browser|chrome|brave|firefox|duck|edge|opera") to "BROWSE",
        Regex("camera") to "CAPTURE EVERYTHING",
        Regex("message|chat|telegram|signal|whatsapp|messenger|discord") to "MESSAGES",
        Regex("calc") to "TOOLS",
        Regex("file|drive|storage") to "ORGANISE",
        Regex("clock|alarm|timer") to "TIME",
        Regex("setting") to "CONTROL",
        Regex("game|play") to "PLAY",
        Regex("video|tube|netflix|prime|stream|tv") to "WATCH",
        Regex("book|read|kindle|pocket") to "READ",
        Regex("shop|amazon|flipkart|store|market") to "SHOP",
        Regex("food|swiggy|zomato|eat") to "EAT",
        Regex("uber|ola|ride|cab|transit|rail") to "GO",
        Regex("weather") to "FORECAST",
        Regex("calendar|agenda") to "PLANS",
        Regex("health|fit|run|step|sleep") to "MOVE",
        Regex("gpt|claude|gemini|copilot|\\bai\\b|assistant") to "CREATE / EXPLORE",
        Regex("term|shell|code|git|dev") to "BUILD",
        Regex("vpn|auth|password|secure|proton") to "PROTECT",
        Regex("contact|people|phone|dial|call") to "CALLS"
    )

    fun defaultFor(packageName: String, label: String): String {
        byPackage[packageName]?.let { return it }
        val haystack = (label + " " + packageName).lowercase()
        byKeyword.firstOrNull { (re, _) -> re.containsMatchIn(haystack) }?.let { return it.second }
        return "OPEN"
    }
}
