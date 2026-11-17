# PayScope — Phase 1: Native Android Application Development

You are an expert Android engineer, Kotlin developer, fintech application architect, and premium mobile UI/UX designer.

Build **PayScope**, a fully native Android financial intelligence application using Android Studio SDK.

## IMPORTANT REQUIREMENTS

- Develop a real native Android application.
- Use Kotlin and Jetpack Compose.
- Use Android SDK and Android Gradle Plugin.
- Do NOT use Next.js, React, Flutter, React Native, or WebView for the Android interface.
- The application must compile into an installable `.apk` file.
- Use the uploaded PayScope specifications as the source of truth for product functionality.
- Use the uploaded reference image for UI design inspiration.
- Implement a premium dark version of the reference design.
- Use smooth native animations across every screen.
- Maintain compatibility with the existing FastAPI and Neon PostgreSQL backend.

## 1. Android Project Configuration

Create a complete Android Studio project.

Application Name: PayScope

Package Name: `com.payscope.app`

Language: Kotlin

UI: Jetpack Compose

Minimum SDK: API 26 (Android 8.0)

Target SDK: Latest stable SDK installed in Android Studio.

Architecture: MVVM with Clean Architecture.

Use Gradle Kotlin DSL (`build.gradle.kts`).

Configure:

- Jetpack Compose BOM
- Material 3
- Navigation Compose
- Lifecycle ViewModel
- Kotlin Coroutines and StateFlow
- Hilt Dependency Injection
- Retrofit and OkHttp
- Kotlin Serialization
- Room Database
- DataStore Preferences
- Firebase Authentication
- Android Credential Manager
- Coil image loading

Use stable compatible library versions.

## 2. Dark UI Design

Follow the uploaded reference image's visual structure.

Design a beautiful premium dark fintech experience with large rounded cards, soft gradients, minimal typography, and elegant bottom navigation.

Color Palette:

- Background: `#040711`
- Surface: `#0D1322`
- Elevated Card: `#151D2E`
- Primary Accent: `#FF5500`
- Secondary Accent: `#00E0E0`
- Savings Green: `#14B8A6`
- Primary Text: `#F4F7FB`
- Secondary Text: `#8E9CAE`

Implement a centralized Compose Material 3 theme.

Use rounded corners, subtle borders, custom gradients, and consistent spacing.

Avoid generic Material Design templates.

## 3. Screen 1 — Welcome Screen

Create a premium animated welcome screen.

Show the PayScope logo with an animated entrance.

Main Heading:

"Take Control of Every Payment."

Subtitle:

"Discover hidden subscriptions, track recurring expenses, and unlock smarter savings."

Display animated financial cards inspired by the uploaded image.

Buttons:

1. Get Started
2. Explore Demo Dashboard

Get Started opens the authentication screen.

Explore Demo Dashboard creates a guest session and opens the demo dashboard.

Use smooth fade, slide, and scale animations.

## 4. Screen 2 — Google Authentication

Create a premium authentication screen.

Heading:

"Welcome to PayScope"

Subtitle:

"Your financial clarity starts here."

Buttons:

- Continue with Google
- Try Demo Dashboard

Implement real Firebase Google Sign-In using Android Credential Manager.

Handle authentication success, cancellation, and errors.

Store session information securely.

After successful authentication:

- New users navigate to Bank Statement Upload.
- Existing users with analyzed statements navigate to Dashboard.

Demo users must be isolated from authenticated user data.

Never fake successful authentication.

## 5. Screen 3 — Bank Statement Upload

Create a native Android statement upload screen.

Heading:

"Discover Your Hidden Payments."

Allow selection of:

- PDF
- CSV
- XLSX
- JSON

Use Android Storage Access Framework and Activity Result APIs.

Features:

- Native document picker
- Selected file information
- File size validation
- Remove and replace file
- Animated upload progress
- Upload error handling
- Processing status

Connect to the FastAPI backend using Retrofit.

Implement the upload endpoint integration.

After upload, show animated processing stages:

1. Reading Statement
2. Extracting Transactions
3. Identifying Merchants
4. Detecting Subscriptions
5. Preparing Dashboard

Display real backend processing status, not fabricated progress.

Add a separate sample statement option using synthetic data.

## 6. Screen 4 — Dashboard

Create a beautiful dashboard inspired by the uploaded reference image.

Header:

- PayScope logo
- Welcome message
- Search icon
- Notification icon
- Profile avatar

Financial cards:

- Monthly Recurring Spending
- Yearly Subscription Cost
- Active Subscriptions
- Potential Savings

Use large animated financial values.

Add:

- Animated spending trend chart
- Subscription distribution chart
- Upcoming renewal cards
- Subscription list
- Price increase alerts
- Recent transaction list

Cards must have smooth entrance animations.

Use realistic synthetic data only in demo mode.

Authenticated users without uploaded data must see an informative empty state.

## 7. Native Bottom Navigation

Create a floating, pill-shaped bottom navigation bar.

Tabs:

1. Home
2. Analytics
3. Subscriptions
4. Savings
5. Profile

Use Material 3 icons or custom vector drawables.

The selected tab must have a smoothly animated pill-shaped indicator.

Implement Navigation Compose for screen routing.

Preserve navigation state where appropriate.

## 8. Native Animation System

Use Jetpack Compose animation APIs:

- AnimatedContent
- AnimatedVisibility
- animateFloatAsState
- animateDpAsState
- updateTransition
- Animatable
- SpringSpec
- AnimatedNavHost or supported navigation transitions

Animation requirements:

- Screen transition: 350ms
- Card stagger entrance: 60ms
- Button press: spring scale
- Bottom navigation: sliding selection
- Financial values: animated counting
- Charts: progressive drawing
- Loading: skeleton shimmer
- Upload: animated progress
- Dialogs: scale and fade

Keep animations smooth on mid-range Android devices.

Respect system reduced-motion settings where supported.

## 9. Backend Integration

Connect the native Android app to the existing FastAPI backend.

Use Retrofit with typed API interfaces.

Implement:

- Authentication verification
- Guest session creation
- User profile retrieval
- Statement upload
- Statement metadata retrieval
- Dashboard summary retrieval
- Demo dataset loading

Use Neon PostgreSQL only through backend APIs.

Never connect directly to Neon from the Android APK.

Never embed database passwords or server API secrets inside the application.

## 10. Android Project Structure

Use a modular, maintainable structure.

Suggested packages:

`com.payscope.app`

- `core/theme`
- `core/navigation`
- `core/network`
- `core/database`
- `core/components`
- `data/remote`
- `data/local`
- `data/repository`
- `domain/model`
- `domain/usecase`
- `feature/welcome`
- `feature/auth`
- `feature/upload`
- `feature/dashboard`
- `feature/analytics`
- `feature/subscriptions`
- `feature/savings`
- `feature/profile`

Use ViewModels, repositories, and StateFlow for application state.

## 11. APK Generation

After implementing Phase 1:

1. Sync Gradle dependencies.
2. Compile the Android project.
3. Resolve all compilation errors.
4. Run unit tests.
5. Run Android instrumentation tests where an emulator or device is available.
6. Build a debug APK using:

`./gradlew assembleDebug`

7. Verify the generated APK exists at:

`app/build/outputs/apk/debug/app-debug.apk`

8. If an Android emulator is available, install and launch the APK.
9. Report the build result and any remaining configuration requirements.

Do not claim the APK was tested on a device unless it actually was.

## 12. Phase 1 Acceptance Criteria

The implementation must include:

- Working native Android project
- Premium animated welcome screen
- Get Started navigation
- Google Sign-In integration
- Demo Dashboard mode
- Native bank statement picker
- Statement upload integration
- Animated dashboard
- Functional bottom navigation
- Responsive Android layouts
- FastAPI integration foundation
- Successful debug APK build

Do not create placeholder buttons that do nothing.

If a backend feature is not implemented, show a clearly labeled unavailable or pending state.

## FINAL INSTRUCTIONS

Read all reference materials before coding.

Start by inspecting the Android SDK, installed build tools, Gradle configuration, and existing project files.

Then implement the actual application.

Do not stop after planning.

Write Kotlin code, create Compose screens, configure dependencies, connect available backend endpoints, and build the APK.

Prioritize visual quality, performance, security, and working functionality.

At the end, provide the APK build path, project structure, implemented features, test results, and remaining tasks.

**START DEVELOPING THE NATIVE PAYSCOPE ANDROID APPLICATION NOW.**