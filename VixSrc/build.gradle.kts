// Use an integer for version numbers (bump it on every release so the app offers an update)
version = 1

cloudstream {
    description = "Movies & TV series from VixSrc with a TMDB-powered home page"
    authors = listOf("Abanob")

    /**
     * Status int as one of the following:
     * 0: Down, 1: Ok, 2: Slow, 3: Beta-only
     **/
    status = 1

    tvTypes = listOf("Movie", "TvSeries")
    language = "en"
    iconUrl = "https://www.google.com/s2/favicons?domain=vixsrc.to&sz=128"
}
