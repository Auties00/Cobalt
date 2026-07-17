package com.github.auties00.cobalt.media.transcode.text.link;

import com.github.auties00.cobalt.telemetry.log.Log;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebExport;
import com.github.auties00.cobalt.meta.annotation.WhatsAppWebModule;
import com.github.auties00.cobalt.meta.model.WhatsAppAdaptation;

import java.lang.System.Logger.Level;
import java.net.IDN;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects HTTP, HTTPS, mailto, IRC, and FTP links inside a free-form text body.
 *
 * <p>Drives both the link-preview pipeline (detecting the first URL in an outgoing body)
 * and the receive pipeline (flagging messages that carry a link). A successful match is
 * validated against {@link #TLD}, has Punycode IDN labels decoded, has trailing
 * punctuation balanced against opening brackets and quotes, and is given a synthesised
 * scheme when the user typed a bare host. Each {@link Match} preserves the literal matched
 * substring, the canonical URL, the post-match offset, the resolved scheme, and the parsed
 * host, port, path, query, and fragment so consumers dispatch off the components without
 * re-parsing.
 *
 * @implNote
 * This implementation transcribes the JS composite regular expression and its nine capture
 * groups verbatim, then re-validates the final TLD label in Java (against {@link #TLD})
 * rather than embedding the TLD alternation into the regex so the TLD list lives in a
 * single {@link Set}.
 */
@WhatsAppWebModule(moduleName = "WALinkify")
@WhatsAppWebModule(moduleName = "WAWebLinkify")
@WhatsAppWebModule(moduleName = "WATopLevelDomains")
public final class Linkify {
    /** The logger for {@link Linkify}. */
    private static final System.Logger LOGGER = Log.get(Linkify.class);

    /**
     * Holds the atomic character class shared by host labels, paths, queries, and anchors.
     *
     * <p>Accepts an ASCII word character, any non-whitespace non-ASCII character outside a
     * small set of formatting punctuation, or a percent-encoded byte.
     */
    private static final String CHAR_CLASS = "\\w|[^\\s\\u0000-\\u007F\\u00AB\\u00BB\\u2018\\u2019\\u201C\\u201D]|%[0-9a-f][0-9a-f]";

    /**
     * Holds the suffix matching either a letter-only TLD or a Punycode IDN label.
     *
     * <p>Combines the letter-only TLD shape with the {@code xn--...} prefix shape so both
     * ASCII and IDN TLDs match.
     */
    private static final String TLD_SUFFIX = "[a-z]{2,}|xn--(?:" + CHAR_CLASS + ")+";

    /**
     * Holds the single host-label fragment.
     *
     * <p>Matches a letter or digit run that may contain dashes but cannot start or end with
     * one.
     */
    private static final String HOST_LABEL = "(?:" + CHAR_CLASS + ")|(?:" + CHAR_CLASS + ")(?:" + CHAR_CLASS + "|-)*(?:" + CHAR_CLASS + ")";

    /**
     * Holds the full host pattern.
     *
     * <p>Matches one or more labels followed by a final TLD label.
     */
    private static final String HOST = "(?!_)(?:(?:" + HOST_LABEL + ")\\.)+(" + TLD_SUFFIX + ")(?!\\." + HOST_LABEL + ")";

    /**
     * Holds the optional port suffix.
     *
     * <p>Matches a colon followed by one to five digits.
     */
    private static final String PORT = ":\\d{1,5}";

    /**
     * Holds the trailing-punctuation set that may appear at the end of a URL but is trimmed
     * because it belongs to the surrounding sentence.
     */
    private static final String TRAILING_PUNCT = "@!.?,(\\[{<\\u00AB\\u2018\\u201C:";

    /**
     * Holds the path-character class.
     *
     * <p>Matches a {@link #CHAR_CLASS} character or any non-whitespace non-percent
     * character.
     */
    private static final String PATH_CHAR = "(?:" + CHAR_CLASS + "|[^\\s%])";

    /**
     * Holds the path component, a slash followed by lazy {@link #PATH_CHAR}s.
     */
    private static final String PATH = "/" + PATH_CHAR + "*?";

    /**
     * Holds the negative lookahead that terminates the URL match at sentence boundaries.
     */
    private static final String STOP_LOOKAHEAD = "[" + TRAILING_PUNCT + "]*(?!" + PATH_CHAR + "|#)";

    /**
     * Holds the query component.
     */
    private static final String QUERY = "\\?(?!" + STOP_LOOKAHEAD + ")" + PATH_CHAR + "*?";

    /**
     * Holds the anchor (fragment) component.
     */
    private static final String ANCHOR = "#" + PATH_CHAR + "*?";

    /**
     * Holds the email local-part character class, the RFC 5321 set of characters legal in
     * an unquoted local part.
     */
    private static final String EMAIL_LOCAL_CHAR = "0-9a-z!#$%&'*+/=?^_`{|}~\\-";

    /**
     * Holds the email local-part fragment.
     */
    private static final String EMAIL_LOCAL = "\\b\\w[" + EMAIL_LOCAL_CHAR + "]*(?:\\.[" + EMAIL_LOCAL_CHAR + "]+)*";

    /**
     * Holds the pre-context that must immediately precede the URL.
     *
     * <p>Anchors the match so the URL never starts mid-identifier.
     */
    private static final String PRE_CONTEXT = "^|\\W\\.|[^/\\w.]|_";

    /**
     * Holds the composite URL pattern source with nine capture groups.
     *
     * <p>Assembled at field-init time so the {@link Pattern#compile(String, int)} call is
     * paid once.
     */
    private static final String COMPOSITE = "(" + PRE_CONTEXT + ")"
            + "((?:http|https)://|mailto:)?"
            + "(" + EMAIL_LOCAL + "@)?"
            + "(" + HOST + ")"
            + "(?:(?!" + HOST_LABEL + ")|(?=_))"
            + "(?:(?=[^:/?#])|(" + PORT + ")?"
            + "(" + PATH + ")?"
            + "(" + QUERY + ")?"
            + "(" + ANCHOR + ")?"
            + "(?=" + STOP_LOOKAHEAD + "))";

    /**
     * Holds the capture-group index for the pre-context, the leading character or boundary
     * the URL match is anchored against.
     */
    private static final int GROUP_PRE_CONTEXT = 1;

    /**
     * Holds the capture-group index for the explicit scheme, which carries either
     * {@code http://}, {@code https://}, or {@code mailto:} when one is typed.
     */
    private static final int GROUP_SCHEME = 2;

    /**
     * Holds the capture-group index for the email local part, non-null only on
     * mailto-shaped matches.
     */
    private static final int GROUP_EMAIL_LOCAL = 3;

    /**
     * Holds the capture-group index for the entire host fragment.
     */
    private static final int GROUP_HOST = 4;

    /**
     * Holds the capture-group index for the TLD label inside the host.
     *
     * <p>The TLD is re-validated against {@link #TLD} in
     * {@link #build(Matcher, String, boolean)}.
     */
    private static final int GROUP_TLD = 5;

    /**
     * Holds the capture-group index for the port suffix, including the leading colon.
     */
    private static final int GROUP_PORT = 6;

    /**
     * Holds the capture-group index for the path.
     */
    private static final int GROUP_PATH = 7;

    /**
     * Holds the capture-group index for the query, including the leading question mark.
     */
    private static final int GROUP_QUERY = 8;

    /**
     * Holds the capture-group index for the anchor, including the leading hash.
     */
    private static final int GROUP_ANCHOR = 9;

    /**
     * Maps each closing bracket or quote to its opening counterpart for the
     * bracket-balancing truncation pass.
     *
     * <p>A trailing bracket or quote is trimmed only when it was not opened earlier in the
     * URL.
     */
    private static final Map<Integer, Integer> CLOSING_TO_OPENING = Map.ofEntries(
            Map.entry((int) '"', (int) '"'),
            Map.entry((int) ')', (int) '('),
            Map.entry((int) '>', (int) '<'),
            Map.entry((int) ']', (int) '['),
            Map.entry((int) '}', (int) '{'),
            Map.entry(0x00BB, 0x00AB),
            Map.entry(0x2019, 0x2018),
            Map.entry(0x201D, 0x201C)
    );

    /**
     * Maps each opening bracket or quote to its closing counterpart for the
     * bracket-balancing truncation pass.
     *
     * <p>An opening bracket pushes its expected closer onto the truncation pass's pending
     * stack.
     */
    private static final Map<Integer, Integer> OPENING_TO_CLOSING = Map.ofEntries(
            Map.entry((int) '"', (int) '"'),
            Map.entry((int) '(', (int) ')'),
            Map.entry((int) '<', (int) '>'),
            Map.entry((int) '[', (int) ']'),
            Map.entry((int) '{', (int) '}'),
            Map.entry(0x00AB, 0x00BB),
            Map.entry(0x2018, 0x2019),
            Map.entry(0x201C, 0x201D)
    );

    /**
     * Holds the compiled composite pattern.
     *
     * <p>Compiled case-insensitive so a URL typed in mixed case still matches.
     */
    private static final Pattern PATTERN = Pattern.compile(COMPOSITE, Pattern.CASE_INSENSITIVE);

    /**
     * Holds the fast-path TLD presence check.
     *
     * <p>Consulted first to short-circuit a full match attempt when no candidate TLD
     * appears in the body.
     */
    private static final Pattern TLD_GUARD = Pattern.compile("\\.(?:" + TLD_SUFFIX + ")", Pattern.CASE_INSENSITIVE);

    /**
     * Holds the snapshot of top-level domain labels eligible for URL detection and
     * link-preview generation.
     *
     * <p>Consulted by {@link #build(Matcher, String, boolean)} after the composite regex
     * matches; a host whose final label is absent from this set is rejected. Includes the
     * ASCII gTLDs and ccTLDs, the IANA-registered IDN gTLDs, and Cyrillic, Arabic, and
     * Indic country TLDs.
     *
     * @implNote
     * This implementation lower-cases and Unicode-normalises every entry so the check can
     * run directly without re-case folding.
     */
    @WhatsAppWebExport(moduleName = "WATopLevelDomains", exports = "TLD",
            adaptation = WhatsAppAdaptation.DIRECT)
    private static final Set<String> TLD = Set.of(
            "aaa", "abb", "abbott", "abogado", "abudhabi", "ac", "academy", "accountant", "accountants", "ad",
            "adult", "ae", "aero", "af", "afl", "africa", "ag", "agency", "ai", "aig",
            "airforce", "al", "alsace", "am", "amazon", "amex", "amsterdam", "android", "ao", "apartments",
            "app", "apple", "ar", "arab", "archi", "army", "arpa", "art", "as", "asia",
            "associates", "at", "au", "auction", "audi", "audio", "auspost", "auto", "autos", "aw",
            "aws", "ax", "az", "ba", "baby", "band", "bank", "bar", "barcelona", "barclaycard",
            "barclays", "bargains", "basketball", "bauhaus", "bayern", "bb", "bbva", "bd", "be", "beauty",
            "beer", "berlin", "best", "bet", "bf", "bg", "bh", "bi", "bible", "bid",
            "bike", "bingo", "bio", "biz", "bj", "black", "blackfriday", "blog", "blue", "bm",
            "bmw", "bn", "bnpparibas", "bo", "boats", "bond", "boo", "boston", "bot", "boutique",
            "box", "br", "bradesco", "broker", "brother", "brussels", "bs", "bt", "build", "builders",
            "business", "buzz", "bw", "by", "bz", "bzh", "ca", "cab", "cafe", "cam",
            "camera", "camp", "canon", "capetown", "capital", "car", "cards", "care", "career", "careers",
            "cars", "casa", "cash", "casino", "cat", "catering", "cba", "cc", "cd", "center",
            "ceo", "cern", "cf", "cfd", "cg", "ch", "charity", "chase", "chat", "cheap",
            "christmas", "chrome", "church", "ci", "citic", "city", "ck", "cl", "claims", "cleaning",
            "click", "clinic", "clothing", "cloud", "club", "clubmed", "cm", "cn", "co", "coach",
            "codes", "coffee", "college", "cologne", "com", "community", "company", "computer", "condos", "construction",
            "consulting", "contact", "contractors", "cooking", "cool", "coop", "corsica", "country", "coupons", "courses",
            "cpa", "cr", "credit", "cricket", "crs", "cu", "cuisinella", "cv", "cw", "cx",
            "cy", "cymru", "cyou", "cz", "dad", "dance", "date", "dating", "day", "de",
            "dealer", "deals", "delivery", "deloitte", "democrat", "dental", "desi", "design", "dev", "dhl",
            "diamonds", "diet", "digital", "direct", "directory", "discount", "diy", "dj", "dk", "dm",
            "do", "doctor", "dog", "domains", "download", "durban", "dvag", "dz", "earth", "ec",
            "eco", "edeka", "edu", "education", "ee", "eg", "email", "energy", "engineer", "engineering",
            "enterprises", "epson", "equipment", "es", "esq", "estate", "et", "eu", "eus", "events",
            "exchange", "expert", "exposed", "express", "extraspace", "fail", "faith", "family", "fan", "fans",
            "farm", "fashion", "feedback", "fi", "film", "finance", "financial", "fish", "fishing", "fit",
            "fitness", "fj", "fk", "flights", "flir", "florist", "flowers", "fm", "fo", "foo",
            "food", "football", "forex", "forsale", "forum", "foundation", "fox", "fr", "frl", "fujitsu",
            "fun", "fund", "furniture", "futbol", "fyi", "ga", "gal", "gallery", "game", "games",
            "garden", "gay", "gd", "gdn", "ge", "gent", "gf", "gg", "gh", "gi",
            "gift", "gifts", "gives", "giving", "gl", "glass", "gle", "global", "globo", "gm",
            "gmbh", "gn", "godaddy", "gold", "golf", "goog", "google", "gop", "gov", "gp",
            "gq", "gr", "graphics", "gratis", "green", "group", "gs", "gt", "guide", "guru",
            "gw", "gy", "hair", "hamburg", "haus", "health", "healthcare", "help", "hermes", "hiphop",
            "hk", "hm", "hn", "hockey", "holdings", "holiday", "homes", "honda", "horse", "host",
            "hosting", "house", "how", "hr", "ht", "hu", "ice", "icu", "id", "ie",
            "ikano", "il", "im", "immo", "immobilien", "in", "inc", "industries", "info", "ing",
            "ink", "institute", "insurance", "insure", "int", "international", "investments", "io", "iq", "ir",
            "irish", "is", "ismaili", "ist", "istanbul", "it", "itau", "itv", "java", "jcb",
            "je", "jetzt", "jewelry", "jio", "jm", "jnj", "jo", "jobs", "joburg", "jp",
            "kaufen", "ke", "kg", "kh", "ki", "kids", "kim", "kitchen", "kiwi", "kn",
            "koeln", "komatsu", "kp", "kpmg", "kr", "krd", "kred", "kw", "ky", "kyoto",
            "kz", "la", "land", "landrover", "lat", "law", "lawyer", "lb", "lc", "leclerc",
            "legal", "lgbt", "li", "lidl", "life", "lighting", "lilly", "limited", "limo", "link",
            "live", "lk", "llc", "loan", "loans", "local", "lol", "london", "love", "ls",
            "lt", "ltd", "ltda", "lu", "lundbeck", "luxe", "luxury", "lv", "ly", "ma",
            "madrid", "makeup", "man", "management", "mango", "market", "marketing", "markets", "mba", "mc",
            "md", "me", "media", "meet", "melbourne", "meme", "memorial", "men", "menu", "mg",
            "mh", "miami", "microsoft", "mil", "mk", "ml", "mm", "mn", "mo", "mobi",
            "moda", "moe", "mom", "monash", "money", "monster", "mortgage", "moscow", "motorcycles", "mov",
            "movie", "mp", "mq", "mr", "ms", "mt", "mu", "museum", "music", "mv",
            "mw", "mx", "my", "mz", "na", "nab", "nagoya", "name", "navy", "nc",
            "ne", "net", "network", "neustar", "new", "news", "next", "nexus", "nf", "ng",
            "ngo", "ni", "nico", "nike", "ninja", "nl", "no", "now", "np", "nr",
            "nrw", "ntt", "nu", "nyc", "nz", "observer", "okinawa", "om", "one", "ong",
            "onion", "onl", "online", "ooo", "orange", "org", "organic", "ovh", "pa", "page",
            "panasonic", "paris", "partners", "parts", "party", "pe", "pet", "pf", "pg", "ph",
            "pharmacy", "phd", "photo", "photography", "photos", "pics", "pictet", "pictures", "pink", "pioneer",
            "pizza", "pk", "pl", "place", "plumbing", "plus", "pm", "pn", "poker", "politie",
            "porn", "post", "pr", "press", "pro", "productions", "prof", "promo", "properties", "property",
            "ps", "pt", "pub", "pw", "py", "qa", "qpon", "quebec", "quest", "racing",
            "radio", "re", "realestate", "realtor", "recipes", "red", "rehab", "reisen", "ren", "rent",
            "rentals", "repair", "report", "republican", "rest", "restaurant", "review", "reviews", "rio", "rip",
            "ro", "rocks", "rodeo", "rs", "rsvp", "ru", "rugby", "ruhr", "run", "rw",
            "ryukyu", "sa", "saarland", "sale", "salon", "sandvik", "sanofi", "sap", "sarl", "saxo",
            "sb", "sbi", "sbs", "sc", "scb", "schmidt", "school", "schule", "schwarz", "science",
            "scot", "sd", "se", "seat", "security", "select", "sener", "services", "sex", "sexy",
            "sg", "sh", "sharp", "shell", "shiksha", "shoes", "shop", "shopping", "show", "si",
            "singles", "site", "sk", "ski", "skin", "sky", "sl", "sm", "sn", "sncf",
            "so", "soccer", "social", "software", "solar", "solutions", "sony", "soy", "space", "sport",
            "sr", "srl", "ss", "st", "statebank", "statefarm", "stockholm", "storage", "store", "stream",
            "studio", "study", "style", "su", "sucks", "supplies", "supply", "support", "surf", "surgery",
            "suzuki", "sv", "swiss", "sx", "sy", "sydney", "systems", "sz", "taipei", "tatamotors",
            "tatar", "tattoo", "tax", "taxi", "tc", "td", "team", "tech", "technology", "tel",
            "tennis", "teva", "tf", "tg", "th", "theater", "tickets", "tienda", "tips", "tirol",
            "tj", "tk", "tl", "tm", "tn", "to", "today", "tokyo", "tools", "top",
            "toshiba", "total", "tours", "town", "toyota", "toys", "tr", "trade", "trading", "training",
            "travel", "tt", "tube", "tui", "tv", "tw", "tz", "ua", "ug", "uk",
            "university", "uno", "uol", "us", "uy", "uz", "va", "vacations", "vanguard", "vc",
            "ve", "vegas", "ventures", "vet", "vg", "vi", "video", "vin", "vip", "vision",
            "vivo", "vlaanderen", "vn", "vodka", "vote", "voto", "voyage", "vu", "wales", "wang",
            "watch", "webcam", "weber", "website", "wedding", "weir", "wf", "wien", "wiki", "williamhill",
            "win", "wine", "woodside", "work", "works", "world", "ws", "wtf", "xin", "xyz",
            "yachts", "yandex", "ye", "yoga", "yokohama", "youtube", "yt", "za", "zappos", "zara",
            "zip", "zm", "zone", "zw",
            "бел", "дети", "москва", "онлайн", "рус", "рф", "укр",
            "भारत", "ভাৰত", "ભારત", "ଭାରତ", "セール", "中国",
            "公司", "我爱你", "移动", "网址", "网站", "网络",
            "닷넷", "닷컴", "한국"
    );

    /**
     * Prevents instantiation of this utility class.
     *
     * @throws AssertionError always
     */
    private Linkify() {
        throw new AssertionError();
    }

    /**
     * Returns every URL detected in {@code text}, in order of appearance.
     *
     * <p>Returns the empty list for a {@code null} {@code text} and when no candidate TLD
     * is present. When {@code requireExplicitScheme} is {@code true}, only matches carrying
     * an explicit {@code http(s)://} scheme are kept. Each candidate is materialised by
     * {@link #build(Matcher, String, boolean)}, which may reject it; rejected candidates do
     * not appear in the result.
     *
     * @implNote
     * This implementation runs the {@link #TLD_GUARD} fast path first and returns the empty
     * list when no candidate TLD is present in the body; the JS regex would otherwise run
     * on every call.
     *
     * @param text                  the text to scan
     * @param requireExplicitScheme whether to keep only matches that carry an explicit
     *                              {@code http(s)://} scheme
     * @return the detected URLs
     */
    @WhatsAppWebExport(moduleName = "WALinkify", exports = "findLinks",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static List<Match> findLinks(String text, boolean requireExplicitScheme) {
        if (text == null || !TLD_GUARD.matcher(text).find()) {
            return List.of();
        }
        var out = new ArrayList<Match>();
        var matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            var match = build(matcher, text, requireExplicitScheme);
            if (match != null) {
                out.add(match);
            }
        }
        if (Log.TRACE) LOGGER.log(Level.TRACE, "linkify scan complete, matches={0}", out.size());
        return out;
    }

    /**
     * Returns the first URL detected in {@code text}, when one is present.
     *
     * <p>Returns empty for a {@code null} {@code text}, when no candidate TLD is present,
     * and when the regex finds no match. Only the first regex match is considered; when
     * {@link #build(Matcher, String, boolean)} rejects that candidate the result is empty
     * without attempting subsequent matches. When {@code requireExplicitScheme} is
     * {@code true}, a match without an explicit {@code http} / {@code https} /
     * {@code mailto} scheme is rejected.
     *
     * @param text                  the text to scan
     * @param requireExplicitScheme whether to require an explicit {@code http} /
     *                              {@code https} / {@code mailto} scheme
     * @return the first match, or empty when no URL is detected
     */
    @WhatsAppWebExport(moduleName = "WALinkify", exports = "findLink",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Optional<Match> findLink(String text, boolean requireExplicitScheme) {
        if (text == null || !TLD_GUARD.matcher(text).find()) {
            return Optional.empty();
        }
        var matcher = PATTERN.matcher(text);
        if (!matcher.find()) {
            return Optional.empty();
        }
        var result = build(matcher, text, requireExplicitScheme);
        if (Log.TRACE) LOGGER.log(Level.TRACE, "linkify find complete, matched={0}", result != null);
        return Optional.ofNullable(result);
    }

    /**
     * Returns whether {@code text} contains at least one explicit-scheme HTTP / HTTPS link.
     *
     * <p>Delegates to {@link #findLink(String, boolean)} with the explicit-scheme
     * requirement set, so a bare-host or mailto match does not satisfy it. Used by the
     * persistence pipeline to set the message's has-link bit without parsing the body
     * twice.
     *
     * @param text the text to scan
     * @return {@code true} when at least one explicit-scheme HTTP / HTTPS link is detected
     */
    @WhatsAppWebExport(moduleName = "WAWebLinkify", exports = "hasHttpLink",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static boolean hasHttpLink(String text) {
        return findLink(text, true).isPresent();
    }

    /**
     * Returns the match describing {@code text} when the entire input is a single,
     * well-formed {@code mailto:} address.
     *
     * <p>The candidate must cover {@code text} in its entirety, resolve to the
     * {@code mailto:} scheme, expose a non-empty local part, and carry neither a query
     * string nor a fragment. Any other match shape yields empty, as does a {@code null}
     * {@code text}.
     *
     * @param text the candidate email address
     * @return the match when {@code text} is a complete mailto address, otherwise empty
     */
    @WhatsAppWebExport(moduleName = "WALinkify", exports = "validateEmail",
            adaptation = WhatsAppAdaptation.DIRECT)
    public static Optional<Match> validateEmail(String text) {
        if (text == null) {
            return Optional.empty();
        }
        var match = findLink(text, false).orElse(null);
        if (match == null) {
            return Optional.empty();
        }
        if (!match.url().equals(text)) {
            return Optional.empty();
        }
        if (!"mailto:".equals(match.scheme())) {
            return Optional.empty();
        }
        if (match.username() == null || match.username().isEmpty()) {
            return Optional.empty();
        }
        if (match.params() != null && !match.params().isEmpty()) {
            return Optional.empty();
        }
        if (match.anchor() != null && !match.anchor().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(match);
    }

    /**
     * Builds a single {@link Match} from the current matcher state.
     *
     * <p>Validates the TLD against {@link #TLD} (decoding Punycode IDN hosts first),
     * validates the port range, balances trailing punctuation against opening brackets and
     * quotes, and synthesises an implicit scheme when none was typed. Returns {@code null}
     * when the candidate fails any validation step, so the enclosing
     * {@link #findLink(String, boolean)} or {@link #findLinks(String, boolean)} loop can
     * either bail or continue.
     *
     * @implNote
     * This implementation re-validates the TLD in Java rather than embedding the
     * {@link #TLD} alternation into {@link #COMPOSITE} so the TLD list lives in a single
     * {@link Set}. The bracket-balancing pass mirrors the JS quirk where out-of-bounds
     * string reads are treated as undefined, including the trailing-underscore probe on the
     * character preceding the match.
     *
     * @param matcher               the regex matcher positioned on a successful match
     * @param input                 the original input text
     * @param requireExplicitScheme whether to drop matches without an explicit scheme
     * @return the materialised {@link Match}, or {@code null} when the candidate is invalid
     */
    private static Match build(Matcher matcher, String input, boolean requireExplicitScheme) {
        var preContext = matcher.group(GROUP_PRE_CONTEXT);
        if (preContext == null) {
            return null;
        }
        var preLength = preContext.length();
        var fullMatch = matcher.group(0);
        var matchStart = matcher.start();
        if ("_".equals(preContext) && matchStart != 1) {
            if (matchStart == 0 || !Character.isWhitespace(input.charAt(matchStart - 1))) {
                return null;
            }
        }
        var tld = matcher.group(GROUP_TLD);
        if (tld != null && tld.startsWith("xn--")) {
            try {
                var unicode = IDN.toUnicode(tld);
                if (!TLD.contains(unicode.toLowerCase())) {
                    return null;
                }
            } catch (IllegalArgumentException malformed) {
                if (Log.DEBUG) LOGGER.log(Level.DEBUG, "linkify punycode tld decode failed", malformed);
                return null;
            }
        } else if (tld != null && !TLD.contains(tld.toLowerCase())) {
            return null;
        }
        var portGroup = matcher.group(GROUP_PORT);
        if (portGroup != null && portGroup.length() > 1) {
            var port = Integer.parseInt(portGroup.substring(1));
            if (portGroup.charAt(1) == '0' || port <= 0 || port >= 65536) {
                return null;
            }
        }
        var components = new String[]{
                matcher.group(GROUP_SCHEME),
                matcher.group(GROUP_EMAIL_LOCAL),
                matcher.group(GROUP_HOST),
                matcher.group(GROUP_PORT),
                matcher.group(GROUP_PATH),
                matcher.group(GROUP_QUERY),
                matcher.group(GROUP_ANCHOR)
        };
        var lastComponentGroup = 0;
        if (components[6] != null) {
            lastComponentGroup = GROUP_ANCHOR;
        } else if (components[5] != null) {
            lastComponentGroup = GROUP_QUERY;
        } else if (components[4] != null) {
            lastComponentGroup = GROUP_PATH;
        }
        var a = fullMatch;
        if (lastComponentGroup != 0) {
            var lastComponent = components[lastComponentGroup - GROUP_SCHEME];
            var probe = matchStart + preLength - 1;
            if (lastComponent.endsWith("_") && probe >= 0 && probe < a.length() && a.charAt(probe) == '_') {
                a = a.substring(0, a.length() - 1);
                lastComponent = lastComponent.substring(0, lastComponent.length() - 1);
                components[lastComponentGroup - GROUP_SCHEME] = lastComponent;
            }
            var pending = new ArrayDeque<Integer>();
            var expectedCloser = 0;
            var lastValid = 0;
            for (var h = 0; h < lastComponent.length(); h++) {
                var codePoint = (int) lastComponent.charAt(h);
                if (codePoint == expectedCloser) {
                    expectedCloser = pending.isEmpty() ? 0 : pending.pop();
                    if (expectedCloser == 0) {
                        lastValid = h;
                    }
                } else if (OPENING_TO_CLOSING.containsKey(codePoint)) {
                    if (expectedCloser != 0) {
                        pending.push(expectedCloser);
                    }
                    expectedCloser = OPENING_TO_CLOSING.get(codePoint);
                } else if (!CLOSING_TO_OPENING.containsKey(codePoint) || expectedCloser == 0) {
                    lastValid = h;
                }
            }
            if (lastValid != lastComponent.length() - 1) {
                if (lastComponentGroup == GROUP_QUERY && expectedCloser != 0) {
                    a = a.substring(preLength);
                } else {
                    var rebuilt = new StringBuilder();
                    for (var g = GROUP_SCHEME; g < lastComponentGroup; g++) {
                        if (g == GROUP_TLD) {
                            continue;
                        }
                        var part = components[g - GROUP_SCHEME];
                        if (part != null && !part.isEmpty()) {
                            rebuilt.append(part);
                        }
                    }
                    rebuilt.append(lastComponent, 0, lastValid + 1);
                    a = rebuilt.toString();
                    matcher.region(matchStart + preLength + a.length(), input.length());
                }
            } else {
                a = a.substring(preLength);
            }
        } else {
            a = a.substring(preLength);
        }
        var url = a;
        var scheme = components[0];
        var hasExplicitHttp = scheme != null && (scheme.equalsIgnoreCase("http://") || scheme.equalsIgnoreCase("https://"));
        if (requireExplicitScheme && !hasExplicitHttp) {
            return null;
        }
        var href = url;
        if (scheme == null) {
            var lower = url.toLowerCase();
            if (lower.startsWith("irc.")) {
                scheme = "irc://";
            } else if (lower.startsWith("ftp.")) {
                scheme = "ftp://";
            } else if (components[1] != null) {
                scheme = "mailto:";
            } else {
                scheme = "http://";
            }
            href = scheme + url;
        } else {
            scheme = scheme.toLowerCase();
        }
        var index = matchStart + preLength + url.length();
        return new Match(
                href,
                url,
                index,
                input,
                scheme,
                components[1],
                components[2],
                components[3],
                components[4],
                components[5],
                components[6],
                hasExplicitHttp
        );
    }

    /**
     * Represents one detected URL match.
     *
     * <p>Carries the canonical URL, the literal matched substring, the post-match offset,
     * the resolved scheme, and the parsed host, port, path, query, and fragment so every
     * preview branch and the message-links pipeline can dispatch off the components without
     * re-parsing.
     *
     * @param href     the canonical URL with an explicit scheme
     * @param url      the literal substring as it appeared in the body
     * @param index    the offset of the first character past the match in the source body
     * @param input    the original input text
     * @param scheme   the resolved scheme (always lower-case; synthesised when the user did
     *                 not type one)
     * @param username the email local part with the trailing {@code @}, or {@code null} for
     *                 non-mailto URLs
     * @param domain   the host portion of the URL
     * @param port     the port suffix including the leading colon, or {@code null} when no
     *                 port was provided
     * @param path     the path component, or {@code null}
     * @param params   the query component, or {@code null}
     * @param anchor   the fragment component, or {@code null}
     * @param isHttp   whether the user typed an explicit {@code http(s)://} scheme
     */
    public record Match(
            String href,
            String url,
            int index,
            String input,
            String scheme,
            String username,
            String domain,
            String port,
            String path,
            String params,
            String anchor,
            boolean isHttp
    ) {
    }
}
