# Accessibility and localization QA

Verified on the `Pixel_10_Pro` Android emulator on September 10, 2026.

## Completed checks

- English and Hebrew resource catalogs contain the same localized keys; obsolete bilingual sample strings were removed.
- First-run onboarding was visually reviewed in English and Hebrew light mode.
- The Today screen was visually reviewed in English and Hebrew dark mode at 150% system font size.
- Hebrew layout direction, navigation order, alignment, and translated content were verified through screenshots and the Android accessibility hierarchy.
- Interactive checkbox, radio-button, and switch rows expose a single merged target with the appropriate semantic role and a minimum 48 dp target height.
- Expandable section and schedule headers expose merged labels and state to accessibility services.
- Numeric pace, goal, and chazarah-offset fields retain left-to-right entry and use suitable numeric keyboards in either UI language.
- Calendar cells have additional height for large text, and calendar task descriptions are localized.
- Light and dark primary, tertiary, and container color pairs meet WCAG AA contrast for normal text.

## Automated verification

- `testDebugUnitTest` passes.
- `assembleDebug` passes and the APK installs on the emulator.
- Android Lint could not be executed in this environment because its uncached lint artifacts could not be downloaded through the host TLS configuration. Compilation, unit tests, static resource comparison, accessibility-hierarchy inspection, and emulator screenshots were used for this pass.

## Manual release follow-up

- Before a public release, perform a spoken TalkBack traversal on at least one physical phone and test the supported minimum and maximum Android API levels.
