package com.privacynudge.data

/**
 * List of sensitive domain patterns that may indicate privacy-relevant network activity.
 * These are categorized for easier understanding in nudge notifications.
 */
object SensitiveDomains {

    data class DomainPattern(
        val pattern: String,
        val category: String,
        val description: String
    )

    /**
     * Map domain patterns to their categories and descriptions.
     */
    val patterns = listOf(
        // Location Services
        DomainPattern("maps.google.com", "Location", "Google Maps service"),
        DomainPattern("maps.googleapis.com", "Location", "Google Maps API"),
        DomainPattern("www.googleapis.com/geolocation", "Location", "Google Geolocation API"),
        DomainPattern("location.services.mozilla.com", "Location", "Mozilla Location Services"),
        DomainPattern("api.mapbox.com", "Location", "Mapbox mapping"),
        DomainPattern("tiles.mapbox.com", "Location", "Mapbox tiles"),
        DomainPattern("api.openstreetmap.org", "Location", "OpenStreetMap"),
        DomainPattern("nominatim.openstreetmap.org", "Location", "OSM geocoding"),
        DomainPattern("ipinfo.io", "Location", "IP geolocation"),
        DomainPattern("ip-api.com", "Location", "IP geolocation"),
        DomainPattern("geoip", "Location", "GeoIP service"),

        // Analytics & Tracking
        DomainPattern("google-analytics.com", "Analytics", "Google Analytics"),
        DomainPattern("analytics.google.com", "Analytics", "Google Analytics"),
        DomainPattern("firebase.google.com", "Analytics", "Firebase"),
        DomainPattern("firebaselogging", "Analytics", "Firebase logging"),
        DomainPattern("app-measurement.com", "Analytics", "App measurement"),
        DomainPattern("amplitude.com", "Analytics", "Amplitude analytics"),
        DomainPattern("mixpanel.com", "Analytics", "Mixpanel"),
        DomainPattern("segment.io", "Analytics", "Segment"),
        DomainPattern("segment.com", "Analytics", "Segment"),
        DomainPattern("branch.io", "Analytics", "Branch"),
        DomainPattern("adjust.com", "Analytics", "Adjust"),
        DomainPattern("appsflyer.com", "Analytics", "AppsFlyer"),
        DomainPattern("flurry.com", "Analytics", "Flurry"),

        // Advertising
        DomainPattern("doubleclick.net", "Advertising", "DoubleClick ads"),
        DomainPattern("googlesyndication.com", "Advertising", "Google ads"),
        DomainPattern("googleadservices.com", "Advertising", "Google ad services"),
        DomainPattern("admob.com", "Advertising", "AdMob"),
        DomainPattern("facebook.com/tr", "Advertising", "Facebook tracking"),
        DomainPattern("graph.facebook.com", "Advertising", "Facebook API"),
        DomainPattern("ads.twitter.com", "Advertising", "Twitter ads"),
        DomainPattern("ads.linkedin.com", "Advertising", "LinkedIn ads"),
        DomainPattern("adsserver", "Advertising", "Ad server"),
        DomainPattern("adservice", "Advertising", "Ad service"),

        // Social / Auth
        DomainPattern("login.facebook.com", "Social", "Facebook login"),
        DomainPattern("accounts.google.com", "Auth", "Google accounts"),
        DomainPattern("oauth", "Auth", "OAuth authentication"),
    )

    /**
     * Check if a domain matches any sensitive pattern.
     * Returns the matching DomainPattern or null if no match.
     */
    fun matchDomain(domain: String): DomainPattern? {
        val lowerDomain = domain.lowercase()
        return patterns.find { pattern ->
            lowerDomain.contains(pattern.pattern.lowercase())
        }
    }

    /**
     * Check if a domain is sensitive (quick boolean check).
     */
    fun isSensitive(domain: String): Boolean = matchDomain(domain) != null

    /**
     * Get the category for a domain, or "Unknown" if not matched.
     */
    fun getCategory(domain: String): String = matchDomain(domain)?.category ?: "Unknown"
}
