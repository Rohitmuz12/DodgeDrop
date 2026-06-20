# Dodge Drop

A one-tap arcade game for Android: tap to jump over incoming spikes. Endless runner,
gets faster the longer you survive, saves your high score locally.

## What's in this project

- **Kotlin**, no external game engine — built on `SurfaceView` + a dedicated render
  thread (`GameThread`) for smooth 60fps gameplay.
- **No internet/ads/IAP** — fully offline, nothing to configure.
- **Minimal dependencies** — just AndroidX core + AppCompat, so the first Gradle
  sync will be fast.

## How to build & run

1. Install **Android Studio** (free): https://developer.android.com/studio
2. Open Android Studio → **Open** → select this `DodgeDrop` folder (the one
   containing `settings.gradle`).
3. Let Gradle sync (first time may take a few minutes while it downloads the
   Android Gradle Plugin and SDK platform tools).
4. Connect an Android phone via USB with **USB debugging** enabled (Settings →
   About Phone → tap "Build number" 7 times → Developer Options → USB debugging),
   or use the built-in Android Emulator.
5. Click the green ▶ **Run** button. The app installs and launches automatically.

### To get an installable APK file instead

Build menu → **Build Bundle(s) / APK(s)** → **Build APK(s)**. Android Studio will
show a notification with a link to the generated `.apk` in
`app/build/outputs/apk/debug/`. You can copy that file to a phone and install it
directly (you'll need to allow "install from unknown sources").

## How the game works (if you want to tweak it)

| File | Purpose |
|---|---|
| `MainActivity.kt` | Hosts the game fullscreen, keeps screen awake |
| `GameView.kt` | Core game loop: states (ready/playing/game over), spawning, scoring, difficulty curve, drawing |
| `GameThread.kt` | Runs update+draw at a steady ~60fps on a background thread |
| `Player.kt` | The jumping ball — gravity, jump physics, squash/stretch |
| `Obstacle.kt` | The spikes the player must clear |
| `ScoreManager.kt` | Saves high score to `SharedPreferences` (persists between launches) |

### Easy tweaks

- **Jump height / gravity feel** → `Player.kt`, change `gravity` and `jumpVelocity`
- **Starting difficulty / ramp speed** → `GameView.kt`, look for `baseSpeed`,
  `maxSpeed`, and the `speed = ...` / `spawnInterval = ...` lines in `update()`
- **Colors** → each class has its own `Paint` definitions using hex colors, easy
  to find and swap
- **Obstacle shape** → currently a triangle spike in `Obstacle.draw()`, swap for
  any `Canvas` drawing call (rect, circle, custom `Path`)

## Building the APK from your phone (no computer needed)

This project includes a GitHub Actions workflow (`.github/workflows/build.yml`)
that builds the APK in the cloud automatically. Here's how to use it entirely
from your phone's browser:

1. **Create a free GitHub account** at github.com if you don't have one.
2. **Create a new repository**: tap the "+" icon → New repository → name it
   `dodge-drop` → make it Public → Create repository.
3. **Upload the project**: on the repo page, tap "uploading an existing file"
   (or Add file → Upload files). Unzip `DodgeDrop.zip` first on your phone
   (most file manager apps can do this), then select and upload everything
   inside the `DodgeDrop` folder — including the hidden `.github` folder.
   Commit the upload.
4. **Trigger the build**: go to the **Actions** tab in your repo. You should
   see a workflow run start automatically (or tap "Build APK" → "Run workflow"
   if it didn't start on its own).
5. **Wait** for the run to finish (a green checkmark, usually 3-5 minutes).
6. **Download the APK**: tap into the completed run → scroll to **Artifacts**
   → tap `dodge-drop-debug-apk` to download a zip containing `app-debug.apk`.
7. **Install on your phone**: open the downloaded file. You'll need to allow
   "install from unknown sources" the first time — Android will prompt you
   automatically when you tap the APK.

Note: GitHub's mobile web upload doesn't always preserve folder structure
perfectly for deeply nested files. If the upload UI gives you trouble, the
GitHub mobile app (Play Store) has a smoother "upload files" flow, or you can
use a Git client app like Termux + git, or MGit, to push the unzipped folder
as a proper git commit instead of a raw upload.



The "addictive" arcade games (Flappy Bird, Chrome Dino, Crossy Road) all share:
one-button input, instant restart with zero loading, a visible personal high
score, and difficulty that ramps just fast enough to feel earned rather than
unfair. This project leans on all four:

- Tap-anywhere input, nothing else to learn
- Tapping on the game-over screen instantly restarts — no menus in the way
- High score is shown on both the start screen and every death screen
- Speed and spawn rate increase gradually with score, capped so it never
  becomes literally impossible
- Small juice touches (squash/stretch on landing, screen shake + flash on
  death, score count-up animation) make moment-to-moment play feel responsive
