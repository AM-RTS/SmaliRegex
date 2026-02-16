# SmaliRegex

An Android app that converts raw Smali instructions into regex-escaped patterns for use in bytecode search/matching tools.

## Features

- **Smali to Regex conversion** — Escapes regex-sensitive characters and replaces registers (`v0`, `p1`, etc.) with `([pv]\d+)` patterns
- **Advanced whitespace mode** — Collapses leading spaces into `\s{N}` and newlines into `\n{N}` for single-line regex output
- **Include branches** — Converts branch labels (`:cond_XX`, `:goto_XX`) into `:[a-zA-Z_]\w*` patterns
- **Exclude debug info** — Filters out `.line`, `.local`, `.prologue`, `.source`, `.param` directives before conversion
- **Auto-convert on paste** — Detects paste actions in the input field and automatically converts
- **Auto-convert on option change** — Toggling any checkbox re-runs conversion instantly
- **Import / Export** — Import `.smali` files and export regex output to `.txt`
- **Copy Regex** — One-tap copy to clipboard
- **Light / Dark theme toggle** — Manual theme switcher in the top bar
- **MIUI-safe** — Disables Android forced dark mode to ensure consistent theming

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with `StateFlow`
- **Min SDK**: 24
- **Target SDK**: 35
- **No third-party libraries** — Only official AndroidX / Jetpack dependencies

## Project Structure

```
app/src/main/java/com/amrts/regexsmali/
├── MainActivity.kt              # Compose UI (screens and components)
├── SmaliConverterViewModel.kt   # ViewModel with reactive state
├── SmaliRegexConverter.kt       # Pure Kotlin conversion logic
└── ui/theme/
    ├── Color.kt                 # Material 3 color scheme (Android Green seed)
    ├── Theme.kt                 # Light / Dark theme definitions
    └── Type.kt                  # Typography
```

## Building

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (signed)
./gradlew assembleRelease
```

The release APK is minified with R8 and resource-shrunk (~1 MB).

## License

MIT
