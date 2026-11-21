# PayScope UI & Configuration Changes — Addendum 03

> **Instructions for Antigravity:** Implement the changes below in the existing PayScope codebase. First inspect the repository structure, framework, existing design system, theme provider, routing, authentication flow, API configuration, and current implementations. Reuse existing components and conventions wherever possible. Do not rebuild unrelated pages or remove working functionality. Do not introduce mock financial data, fake profile information, placeholder API responses, or duplicate configuration. Make the changes responsive for desktop and Android/mobile layouts.

## 1. Apply the selected theme to Landing and Login pages

Make the Landing page and Login page fully theme-aware, just like the authenticated PayScope application.

### Requirements
- When the user selects **Light** theme, the Landing page and Login page must use the Light theme.
- When the user selects **Dark** theme, both pages must use the Dark theme.
- The theme choice must remain consistent while navigating between Landing, Login, authentication, and the main application.
- Persist the user's selected theme using the application's existing theme-preference mechanism. If no persistence mechanism exists, implement one safely so the choice survives page refreshes and navigation.
- Provide an accessible theme switch on the unauthenticated experience, so users can choose a theme before signing in.
- Avoid a flash of the wrong theme during initial page load. Apply the saved theme as early as the existing architecture reasonably allows.
- Ensure text, icons, borders, cards, inputs, buttons, modals, backgrounds, hover states, and focus states all use theme-aware design tokens.
- Do not solve theme issues by applying fixed white or black text to individual components. Use semantic theme variables/tokens so all components respond correctly.

### Acceptance criteria
- Switching to Light mode updates Landing and Login immediately, without requiring a refresh.
- Switching to Dark mode updates Landing and Login immediately, without requiring a refresh.
- The chosen theme remains consistent after navigation and reload.
- No low-contrast or invisible text remains in either theme.

## 2. Replace “Overview” with a personalized welcome heading

On the main/home dashboard, replace the heading **“Overview”** with:

**“Welcome back, {user name}”**

### Requirements
- Use the authenticated user's actual display name from the existing authentication/profile data source.
- Do not use a hardcoded name or a random letter/name.
- Handle missing or unavailable display names gracefully. Use a neutral fallback such as **“Welcome back”** rather than rendering `undefined`, `null`, or an empty punctuation mark.
- Preserve the existing heading hierarchy, spacing, and responsive behavior.
- Ensure the heading text color follows the active theme.

## 3. Highlight the four main feature options in orange and remove the row background block

Update the main feature navigation options:
- **Analysis**
- **Subscriptions**
- **Savings Simulator**
- **Graph Analysis**

### Visual requirements
- Highlight these four feature options using the PayScope orange accent color, following the existing orange used elsewhere in the application.
- Remove the large/shared background container or background strip behind the entire feature-options row. The row should sit cleanly on the page background rather than appearing inside one large colored block.
- Keep each option visually distinct and easy to identify. Apply the orange accent to the appropriate icon, active indicator, or other key visual element without making labels difficult to read.
- Keep labels visible exactly as listed above, including **“Savings Simulator”** and **“Graph Analysis”**.
- Maintain even spacing, alignment, and consistent icon sizing across all four options.
- Make the row responsive: options must remain readable and usable on narrow Android/mobile screens without clipping or overlapping.
- Text and secondary UI colors must still adapt to Light and Dark themes. Orange is an accent, not a replacement for accessible text colors.
- Preserve the existing navigation destinations and click behavior for all four options.

## 4. Move “Monthly Recurring Spend” above the feature options and apply theme-specific colors

Move the **Monthly Recurring Spend** container/card so it appears **above the four highlighted feature options** on the Home dashboard.

### Required order on Home
1. Personalized welcome heading: “Welcome back, {user name}”
2. Monthly Recurring Spend card/container
3. Feature options: Analysis, Subscriptions, Savings Simulator, Graph Analysis
4. The remaining existing Home dashboard content, in its current logical order

### Theme-specific card styling
- **Dark theme:** Use the orange color treatment previously used for this card/container in PayScope. Reuse the existing project color/token where available instead of introducing an unrelated orange shade.
- **Light theme:** Use a cyan color treatment for the card/container.
- Ensure card text, values, icons, borders, and secondary labels maintain sufficient contrast in both themes.
- Keep the existing Monthly Recurring Spend calculation, data source, formatting, and interactions unchanged. This is a layout and styling change, not a request to alter financial calculations.
- Verify that the card does not overlap the feature row or other dashboard sections at mobile, tablet, and desktop breakpoints.

## 5. Add and verify the Android, backend, database, Firebase, and OAuth configuration details

Use the following values as the supplied project configuration reference. Preserve them exactly when documenting or checking the existing configuration. Before changing code or deployment settings, inspect the current implementation and identify where each value is consumed. Do not create duplicate environment variables or replace working configuration without a reason.

### 5.1 Android project SHA key fingerprints

These fingerprints were generated from the local debug keystore at `%USERPROFILE%\.android\debug.keystore`, using alias `androiddebugkey`. They are intended for the Android OAuth configuration in Firebase Console and Google Cloud Console.

- **Android package name:** `com.payscope.app`
- **SHA-1 fingerprint:**

  ```text
  E0:09:D9:D7:1D:E8:68:76:33:C7:09:BE:FD:90:55:07:64:D0:20:FA
  ```

- **SHA-256 fingerprint:**

  ```text
  18:20:64:2A:F2:19:03:9C:15:C9:C7:FC:A6:6A:7A:31:0C:49:81:CB:5A:22:55:A6:82:72:27:B2:EA:D6:19:4F
  ```

### 5.2 Deployed backend details

| Component | Supplied value |
|---|---|
| Live deployed API URL | `https://payscope-api-ice5.onrender.com/` |
| Health check endpoint | `https://payscope-api-ice5.onrender.com/api/v1/health` |
| Readiness check endpoint | `https://payscope-api-ice5.onrender.com/api/v1/ready` |
| Interactive Swagger documentation | `https://payscope-api-ice5.onrender.com/docs` |
| Hosting provider | Render |
| Backend source repository | `https://github.com/vinaybhadane/Payscope-Backend` |
| Primary database | Neon PostgreSQL — AWS `ap-southeast-1` |
| Supplied Neon database identifier/host fragment | `ep-calm-fog-az6aivg7-pooler` |
| Knowledge graph database | Neo4j AuraDB |
| Supplied Neo4j connection URI | `neo4j+s://9d021978.databases.neo4j.io` |
| Supplied Neo4j instance identifier | `9d021978` |
| Firebase project ID | `payscope18` |
| Google OAuth Client ID | `104812680638-ja5lnubukamihfvfivd6mi7fbbjt12tr.apps.googleusercontent.com` |
| Allowed frontend origin 1 | `https://payscope.vercel.app` |
| Allowed frontend origin 2 | `https://pay-scope-lake.vercel.app` |

### 5.3 Configuration and verification tasks

- Inspect the Android application configuration and confirm the application ID/package name is `com.payscope.app`.
- Check that the supplied SHA-1 and SHA-256 fingerprints correspond to the intended local **debug** keystore configuration. Do not assume these debug fingerprints are release-signing fingerprints. Do not overwrite or expose any keystore file.
- Check the Firebase project configuration and Google OAuth setup against the supplied package name, fingerprints, Firebase project ID, and OAuth Client ID.
- Inspect the frontend API base URL configuration and confirm that the application uses the supplied Render API URL in the appropriate deployed environment.
- Check the backend health, readiness, and Swagger routes against the supplied endpoint paths. Report the actual result of any verification performed; do not claim that an endpoint is active or returning HTTP 200 unless it was tested successfully.
- Check the backend CORS configuration and confirm that both supplied Vercel origins are permitted where required. Preserve any other valid origins already needed by development or deployment.
- Inspect Neon PostgreSQL and Neo4j connection configuration and confirm that the application reads connection details from the appropriate server-side environment variables.
- Confirm that Firebase authentication uses the intended Firebase project and that Google sign-in is configured consistently for web and Android, as applicable.
- Keep the Neo4j graph service/configuration logically separate from analytics UI as already requested in the earlier PayScope UI change addenda. Do not reintroduce the graph inside the Analysis page.
- Do not hardcode database passwords, API keys, Firebase private credentials, service-account JSON, OAuth client secrets, or other private credentials in frontend code, source files, committed documentation, or Git. Use the existing environment-variable and secret-management approach.
- The OAuth Client ID, package name, SHA fingerprints, API URLs, and project identifiers listed here are supplied configuration identifiers; they are not substitutes for private credentials.
- Do not invent missing database usernames/passwords, API keys, secrets, or environment-variable values. If a required secret is missing, identify the exact variable name expected by the existing code and report that it must be configured securely.
- Do not print private environment-variable values in logs or in the final implementation report.

## 6. Remove “Import Statement” from the History section

Remove the **“Import Statement”** action/button/entry point from the History section.

### Requirements
- Remove the visible Import Statement option from the History page and its associated History-page UI.
- Preserve the existing imported statement history, statement list, transaction history, statement details, expandable summaries, and other previously requested History functionality.
- Do not delete previously imported statements or their stored data.
- Do not remove statement importing from other parts of the app unless the existing architecture makes it impossible to separate. This request specifically targets the History section.
- Check for empty-state layouts and responsive spacing after the action is removed so there is no awkward blank area.

## 7. Global implementation constraints

- Read the existing project instructions and architecture documentation before making changes (including `AGENTS.md`, `README.md`, relevant files under `docs/`, and environment examples if present).
- Follow the current framework, component structure, state-management approach, routing, authentication flow, and styling conventions.
- Reuse existing theme tokens and orange/cyan palette values wherever possible. Avoid arbitrary one-off colors.
- Preserve existing functionality, calculations, API integrations, authentication behavior, and user data.
- Do not add mock data or hardcoded user information to make the UI look complete.
- Do not make unrelated redesigns or modify pages outside the scope of these requirements unless a shared component change is necessary.
- Ensure all newly changed UI is accessible: readable contrast, visible focus states, keyboard operability, sensible touch targets, and semantic labels.
- Ensure the design works in both Light and Dark themes and across desktop, tablet, and Android/mobile layouts.
- Do not claim a feature is complete merely because the code compiles. Run the available lint, type-check, build, and relevant tests. Test the affected routes and theme states where the project supports it.

## 8. Final verification checklist

Before finishing, verify and report each item:

- [ ] Landing page follows the selected Light/Dark theme.
- [ ] Login page follows the selected Light/Dark theme.
- [ ] Theme preference remains consistent through navigation and reload.
- [ ] “Overview” is replaced with “Welcome back, {user name}” using the authenticated user's real display name and a safe fallback.
- [ ] The four feature options are highlighted with the existing orange accent.
- [ ] The shared background block behind the feature-options row is removed.
- [ ] Monthly Recurring Spend appears above the feature options.
- [ ] Monthly Recurring Spend uses the existing orange treatment in Dark theme and cyan treatment in Light theme.
- [ ] Financial values and all text remain readable in both themes.
- [ ] Android package name and supplied debug SHA fingerprints have been checked against the project configuration.
- [ ] Backend endpoints, CORS origins, Firebase/OAuth settings, Neon configuration, and Neo4j configuration have been inspected; only successfully tested results are reported as verified.
- [ ] “Import Statement” is removed from History without deleting existing imported statements or breaking statement details.
- [ ] Existing navigation, authentication, statement history, calculations, and unrelated features continue to work.
- [ ] Responsive layout, lint, type-check, build, and relevant tests have been checked.

## 9. Final report format

After implementation, provide a concise report with:

1. **Files changed:** List the files created, modified, or removed.
2. **UI changes completed:** State which of Sections 1–4 and 6 were implemented.
3. **Configuration checks:** For each item in Section 5, state `Verified`, `Not verified`, or `Needs user configuration`, with a short explanation. Never expose private secrets.
4. **Theme testing:** Report results for Landing, Login, and Home in both Light and Dark themes.
5. **Build and tests:** Report the actual lint, type-check, build, and test outcomes. Include errors that remain.
6. **Outstanding items:** List any incomplete work, missing environment variables, external console settings that require manual action, or deployment steps.

**Do not fabricate successful tests, endpoint status, configuration checks, or deployment results.**
