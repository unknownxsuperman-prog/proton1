# x-bit Proton — KCET Rank & College Predictor

Native Android app (Kotlin, MVVM) for KCET rank prediction and college eligibility.

## Building the APK

### Via GitHub Actions (recommended)
1. Push this project to a GitHub repository
2. Go to **Actions → Build APK → Run workflow**
3. Download `x-bit-proton-debug.apk` or `x-bit-proton-release-unsigned.apk` from the Artifacts section

> **Note:** The CI workflow runs `gradle wrapper` first to regenerate the `gradle-wrapper.jar` binary, then builds. No extra setup needed.

### Locally
Requirements: JDK 17, Android SDK (compileSdk 34)

```bash
# First run — generate the Gradle wrapper jar
gradle wrapper --gradle-version 8.6

chmod +x gradlew
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

## Architecture

- **MVVM**: `ChatViewModel` holds all state; fragments observe LiveData
- **NLP Engine**: Bag-of-words + Jaccard classifier, entity extraction (rank, marks, category, branch)
- **Rank Predictor**: Log-linear interpolation against historical KCET anchor table
- **College Matcher**: Fuzzy branch matching (exact → substring → Jaccard ≥ 0.4)
- **Remote data**: All datasets fetched at runtime from `https://unknownxsuperman-prog.github.io/x-bit-kea/`

## Package structure

```
com.xbit.proton
├── data/model/         Message, Chat, College, TrainingExample, BranchAlias
├── data/repository/    CollegeRepository (OkHttp + Gson, singleton)
├── engine/             KcetRankPredictor, CollegeMatcher, NlpEngine, PdfPriorityParser
├── util/               StorageManager, ThemeManager
└── ui/
    ├── onboarding/     OnboardingActivity
    ├── chat/           ChatFragment, ChatAdapter
    ├── menu/           SideMenuFragment
    ├── cards/          RankPredictionCard, CollegePredictionCard, PriorityListCard, AllCollegesActivity
    ├── widget/         SearchBar
    └── viewmodel/      ChatViewModel
```
