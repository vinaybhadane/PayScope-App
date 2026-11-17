# PayScope

Native Android recurring-payment tracker built with Kotlin/Jetpack Compose and a FastAPI backend. The original navy, orange, cyan and teal palette is preserved.

## APK and complete source

Download the installable APK from [GitHub Releases](https://github.com/vinaybhadane/PayScope-App/releases/latest). The published APK is a debug-signed build connected to `https://payscope-api-ice5.onrender.com/`.

This repository contains the Android app, backend source, Gradle wrapper, tests, logo assets and deployment/build scripts. The running Render backend is also maintained separately in [Payscope-Backend](https://github.com/vinaybhadane/Payscope-Backend).

Before building, download this Android app's Firebase configuration to `app/google-services.json` (ignored by Git), then create the ignored local build settings:

```powershell
python scripts/configure_firebase.py
```

Alternatively, copy `firebase.properties.example` to `firebase.local.properties` and fill in the settings locally. Gradle properties and environment variables with the same names can override that file. The source contains no Firebase API key. A build without Firebase settings supports sample workspaces; Google sign-in requires the configured build.

To build the hosted-backend APK on Windows:

```powershell
.\scripts\build-render-apk.ps1 -ApiUrl 'https://payscope-api-ice5.onrender.com/'
```

The APK is written to `output/android/payscope-render-debug.apk`. On other platforms use `./gradlew :app:assembleDebug -PPAYSCOPE_API_BASE_URL=https://payscope-api-ice5.onrender.com/`.

Firebase client keys are embedded in the compiled Android app by design. They must be restricted to the required Firebase APIs and must never be reused for Gemini. See [security and key configuration](SECURITY.md).

Local credentials, bank statements, signing keys, generated build folders and virtual environments are excluded from source control. Configure `backend/.env` using `backend/.env.example` before running the backend locally. Firebase client identifiers are project-specific; a build on a different computer must have its signing certificate registered with that Firebase project for Google sign-in.

## Working data flow

1. Sign in with Google through Firebase, or open an isolated sample workspace.
2. Select a PDF, CSV, XLSX or JSON statement (maximum 10 MB).
3. Selecting a file starts the import immediately. Loading remains visible while the document is extracted, saved, analyzed and fetched from the database. Structured PDFs use table extraction; scanned or complex PDFs use server-side Gemini document extraction. Ambiguous or incomplete extraction is rejected with an actionable error.
4. Save validated, deduplicated transactions in the signed-in workspace. Gemini confirms subscriptions from at least two observed payment dates and lists single-charge service candidates separately. Their billing cycles and savings remain labelled planning estimates until confirmed.
5. Read saved statement transactions and per-currency debit/credit totals on Overview, even when no subscription recurrence is detected. Analytics includes actual spending, categories and merchant totals from all successful INR debits. Recurring totals include confirmed services; potential services appear in Subs and opt-in savings scenarios. No fabricated chart points, deadlines or success responses are used.
6. Explore payment connections, edit subscriptions, save server-calculated savings scenarios, and track user-reported cancellations through the backend.

If Gemini analysis fails after records were saved, Overview still shows the imported transactions with a retry action. If saving or the final database read fails, the upload screen stays open with an error and Retry import. Completion requires a verified saved import and a fresh dashboard read.

Dashboard totals and savings scenarios aggregate INR commitments. Other currencies retain their original currency in subscription records. Renewal dates are estimates from observed billing history. Cancellation guidance and a user-reported cancellation do not cancel a provider's service automatically.

## Local development

Prerequisites: JDK 21, Android SDK 35, Python 3.11+, and the existing PostgreSQL/Firebase configuration.

Configure backend/.env with DATABASE_URL, TEST_DATABASE_URL (a separate test database), FIREBASE_PROJECT_ID, API_SESSION_SECRET (at least 32 characters), GEMINI_API_KEY and GEMINI_MODEL. Keep credentials on the server. The Android APK contains no Gemini credential.

From the backend directory:

```powershell
uv sync --extra dev
uv run uvicorn app.main:app --host 127.0.0.1 --port 8000
```

From the project root:

```powershell
.\gradlew.bat assembleDebug
adb reverse tcp:8000 tcp:8000
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.payscope.app/com.payscope.app.MainActivity
```

The emulator uses http://10.0.2.2:8000/ and a USB-connected phone uses http://127.0.0.1:8000/ through adb reverse. For a hosted backend, supply its HTTPS URL explicitly:

```powershell
.\gradlew.bat assembleDebug -PPAYSCOPE_API_BASE_URL=https://your-api.example/
```

The API must be running for imports, sample workspaces and saved data. Restart it after changing backend/.env. Firebase Android package, signing fingerprint and Web OAuth client configuration must match the app's Google sign-in settings.

## Graph

The relationship view uses actual workspace records. Its overview groups payments around the workspace; selecting a record focuses its immediate connections. Categories come from the imported data. Pinch/drag, zoom buttons and reset control the view. Neo4j sync reports failures accurately. If its projection is unavailable, the view clearly identifies the relational source rather than fabricating a connected cloud database.

## Email reminders

Profile stores email reminder preferences on the server for verified signed-in email addresses. Delivery requires a configured Brevo sender and an always-running backend email worker. Guests cannot receive email. Report emails and reminders use the existing durable outbox; Android notification posting also checks runtime permission.

## Validation

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
cd backend
uv run python -m unittest discover -s checks -v
uv run pytest -q tests/test_mobile_api.py tests/test_pdf_import.py tests/test_ai_import_review.py tests/test_workspace_auth.py tests/test_import_refresh.py tests/test_dashboard_api.py tests/test_financial_impact_api.py tests/test_graph.py tests/test_commitments_api.py tests/test_cancellations.py tests/test_cadence_classifier.py tests/test_confidence.py
```

Tests migrate only the separately configured mandatelens_test database, using a direct Neon connection for migrations. Normal regression tests mock the Gemini boundary and disable live email delivery.

The optional live smoke test sends a generated scanned statement containing synthetic transactions to Gemini, checks extraction, persisted dashboard totals and graph evidence, then clears its disposable guest workspace:

```powershell
uv run python checks/live_pdf_smoke.py --live
```

Gemini PDF integration follows [Google's document processing documentation](https://ai.google.dev/gemini-api/docs/document-processing) and structured JSON output. A valid server-side provider key and an available configured model are required.

The supplied payscope.png is used unchanged for app branding, the launcher and the splash screen.

## Separate Render deployment

The backend can be hosted independently of this Android project. Follow [the Render deployment guide](backend/DEPLOY_RENDER.md). Run `python scripts/package_render_backend.py` to generate `output/render/payscope-backend.zip` containing only deployable backend source, with local credentials and statements excluded.

After deployment, `scripts/build-render-apk.ps1 -ApiUrl https://YOUR-SERVICE.onrender.com/` checks the hosted API and creates `output/android/payscope-render-debug.apk`. The backend package and Android connection step are separate.
