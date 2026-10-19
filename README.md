# VixSrc – CloudStream extension

1. Put your free TMDB v3 API key in `VixSrc/src/main/kotlin/com/vixsrc/VixSrcProvider.kt` (`TMDB_API_KEY`).
2. Replace `USERNAME/REPO` in `repo.json` with your GitHub username and repository name.
3. Push to `main`/`master`; GitHub Actions builds `VixSrc.cs3` + `plugins.json` into the `builds` branch.
4. In CloudStream: Settings → Extensions → Add repository →
   `https://raw.githubusercontent.com/USERNAME/REPO/main/repo.json`
