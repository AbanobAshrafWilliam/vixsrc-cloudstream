<div align="center">

# 🎬 VixSrc for CloudStream

**Movies & TV series in CloudStream, with a full TMDB-powered home page.**

![Platform](https://img.shields.io/badge/platform-CloudStream-blue)
![Type](https://img.shields.io/badge/type-Movies%20%7C%20TV%20Series-orange)
![Language](https://img.shields.io/badge/language-Kotlin-purple)

[English](#english) · [العربية](#العربية)

</div>

---

## English

### ✨ Features

- 🏠 **Rich home page** with categories: Trending, Popular, Top Rated, Now Playing, plus genre rows (Action, Comedy, Horror, Sci-Fi, Drama, Animation, Thriller…), Anime, and Arabic movies & series.
- 🔎 **Search** across movies and TV series.
- 🖼️ **Full details pages**: title, year, poster, backdrop, plot, genres, rating, runtime, trailer, cast, and recommendations.
- 📺 **Series support** with every season and episode listed (with episode thumbnails, descriptions and air dates).
- ▶️ **Stream links** fetched automatically from VixSrc (HLS), with multiple audio / subtitle tracks when the stream provides them.

### 📲 Installation

1. Open **CloudStream**.
2. Go to **Settings → Extensions → Add repository**.
3. Paste this URL:

   ```
   https://raw.githubusercontent.com/AbanobAshrafWilliam/vixsrc-cloudstream/main/repo.json
   ```

4. Open the repository and tap **Download** next to **VixSrc**.
5. Go back to the home screen, pick **VixSrc** as your provider, and enjoy.

### 🛠️ Troubleshooting

| Problem | Try this |
| --- | --- |
| "No plugins found in repository" | Make sure the URL above is pasted exactly, with no extra spaces. Remove the repository and add it again. |
| Home page is empty | Check your internet connection and make sure VixSrc is selected as the provider. |
| "No links found" | The site may be temporarily down or blocked by your ISP. Try again later or use a VPN / different DNS. |
| Extension not updating | Open Settings → Extensions and pull down to refresh, or remove and re-add the repository. |

### 🧩 How it works

VixSrc is an embed service keyed by TMDB IDs and has no browsable catalog. So this extension uses:

- **TMDB** for everything you *browse*: home page, search, posters, cast, seasons and episodes.
- **VixSrc** only for the *stream link*, resolved when you press play.

### 👨‍💻 Build from source

```bash
git clone https://github.com/AbanobAshrafWilliam/vixsrc-cloudstream.git
cd vixsrc-cloudstream
# 1. Put your free TMDB v3 API key in:
#    VixSrc/src/main/kotlin/com/vixsrc/VixSrcProvider.kt  ->  TMDB_API_KEY
# 2. Build
./gradlew make
```

The compiled `VixSrc.cs3` is created in `VixSrc/build/`. Every push to `main` is also built automatically by GitHub Actions and published to the `builds` branch.

### ⚠️ Disclaimer

This repository only contains the code of a CloudStream extension. **It does not host, store, or upload any media.** All content is provided by third-party services (VixSrc). The author has no control over, and is not responsible for, the content those services provide. You are responsible for making sure that your use of this extension complies with the laws of your country.

### 🙏 Credits

- [CloudStream](https://github.com/recloudstream/cloudstream) for the app and extension framework.
- This product uses the [TMDB](https://www.themoviedb.org/) API but is not endorsed or certified by TMDB.

---

## العربية

### ✨ المميزات

- 🏠 **صفحة رئيسية غنية** مقسمة لفئات: الأكثر رواجاً، الأشهر، الأعلى تقييماً، يُعرض الآن، وكمان أقسام حسب النوع (أكشن، كوميدي، رعب، خيال علمي، دراما، أنيميشن، إثارة…)، والأنمي، والأفلام والمسلسلات العربي.
- 🔎 **بحث** في الأفلام والمسلسلات.
- 🖼️ **صفحات تفاصيل كاملة**: الاسم، سنة الإنتاج، البوستر، القصة، النوع، التقييم، المدة، الإعلان، الأبطال، وأعمال مشابهة.
- 📺 **دعم المسلسلات** بكل المواسم والحلقات، مع صور الحلقات ووصفها وتاريخ عرضها.
- ▶️ **روابط المشاهدة** بتتجاب تلقائياً من VixSrc (HLS)، ومعاها مسارات الصوت والترجمة لما تكون متاحة.

### 📲 طريقة التثبيت

1. افتح تطبيق **CloudStream**.
2. روح على **Settings ← Extensions ← Add repository**.
3. الصق الرابط ده:

   ```
   https://raw.githubusercontent.com/AbanobAshrafWilliam/vixsrc-cloudstream/main/repo.json
   ```

4. افتح الـ repository واضغط **Download** جنب **VixSrc**.
5. ارجع للصفحة الرئيسية، واختار **VixSrc** كمصدر، واستمتع.

### 🛠️ حل المشاكل

| المشكلة | الحل |
| --- | --- |
| "No plugins found in repository" | اتأكد إن الرابط متنسخ زي ما هو من غير مسافات زيادة، وامسح الـ repository وضيفه تاني. |
| الصفحة الرئيسية فاضية | اتأكد من الإنترنت ومن إن VixSrc هو المصدر المختار. |
| "No links found" | ممكن الموقع واقع مؤقتاً أو محجوب عند مزود الإنترنت. جرّب بعدين أو استخدم VPN أو DNS مختلف. |
| الـ extension مش بيتحدّث | افتح Settings ← Extensions واسحب لتحت للتحديث، أو امسح الـ repository وضيفه تاني. |

### 🧩 إزاي بيشتغل

موقع VixSrc عبارة عن خدمة embed بتشتغل بأرقام TMDB ومفيهاش كتالوج للتصفح، فالـ extension بتستخدم:

- **TMDB** لكل اللي بتتصفحه: الصفحة الرئيسية، البحث، البوسترات، الأبطال، المواسم والحلقات.
- **VixSrc** للينك المشاهدة بس، وبيتجاب لما تضغط تشغيل.

### ⚠️ إخلاء مسؤولية

الـ repository ده فيه كود extension لتطبيق CloudStream بس. **مفيش أي محتوى مرفوع أو مخزّن هنا.** كل المحتوى بيجي من خدمات خارجية (VixSrc)، وصاحب الإضافة مش مسؤول عن المحتوى اللي بتقدمه الخدمات دي. مسؤوليتك تتأكد إن استخدامك للإضافة متوافق مع قوانين بلدك.

### 🙏 شكر

- [CloudStream](https://github.com/recloudstream/cloudstream) على التطبيق وإطار عمل الإضافات.
- المنتج ده بيستخدم TMDB API لكن مش معتمد أو مُصدّق من TMDB.
