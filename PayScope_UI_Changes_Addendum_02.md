# PayScope UI Changes – Implementation Prompt (Addendum 02)

> **Purpose:** Implement the six additional UI/UX changes below in the existing PayScope application. Treat the attached PhonePe home-screen screenshot as a visual reference for the compact, icon-led quick-action layout (icons grouped evenly with their labels directly underneath). Adapt the visual treatment to PayScope's own design system and both supported themes; do not copy PhonePe branding, colors, names, or product-specific UI.
>
> **Instruction precedence:** These are the latest requirements and supersede conflicting earlier instructions. In particular, the Home-page profile avatar must now be at the **top-left**, not the top-right.

## Instructions for the coding agent

Before changing code, inspect the existing project structure, `AGENTS.md`, `README.md`, current design tokens/theme provider, authentication setup, routing, Home page, Analysis page, Profile page, and Neo4j/graph implementation. Identify the actual components and data flow responsible for each requested change. Reuse the existing architecture and components wherever practical.

Implement the requirements in the existing codebase. Do not create mock user data, fake Google profile pictures, placeholder graph data, or duplicate pages. Preserve existing financial calculations, statement data, authentication, subscriptions, savings functionality, and all unrelated working features. Do not make broad redesigns outside the scope of these requirements.

---

## 1. Make every text color theme-aware

**Problem:** In Light Mode, some areas still render white text. In particular, the PayScope AI Assistant and Neo4j graph labels/text become difficult or impossible to read. Similar fixed-color issues may exist in other components.

**Required changes:**

- Ensure text, headings, descriptions, labels, placeholders, helper text, buttons, tooltips, menus, cards, dialogs, and disabled states use semantic theme tokens rather than hard-coded foreground colors.
- PayScope AI Assistant must have readable foreground, background, border, and message colors in both Light and Dark Mode. Do not leave white text on a light background.
- Neo4j Graph Analysis must also respond to the active theme. Update node labels, relationship/edge labels, legends, tooltips, controls, loading states, empty states, and graph panels. Avoid white labels on a light canvas or dark labels on a dark canvas.
- Use the application's existing theme provider/design tokens if available. If necessary, extend the shared token system (for example, `--color-text-primary`, `--color-text-secondary`, `--color-surface`, `--color-border`, and graph-specific tokens) and consume those tokens consistently.
- Avoid solving this with isolated one-off overrides. Do not detect the theme separately in every component when the shared theme context or CSS variables can provide the correct values.
- Preserve the current visual hierarchy and ensure sufficient contrast and visible focus states in both themes.

**Acceptance criteria:**
- Switching between Light and Dark Mode updates all affected text and surfaces immediately and consistently.
- PayScope AI Assistant is fully readable in both themes.
- Neo4j graph nodes, edges, labels, legends, and controls are fully readable in both themes.
- No important text is rendered using a fixed white/black color that becomes invisible in the opposite theme.

## 2. Update the Home-page header: remove PayScope logo and move profile avatar to top-left

**Required changes:**

- On the Home page header, remove the PayScope logo/wordmark currently displayed there.
- Move the user's profile avatar from the top-right corner to the **top-left corner** of the Home page header.
- Do not display the PayScope logo in the header as a replacement for the avatar. Keep the remaining header controls and spacing balanced and responsive.
- Scope this removal to the Home-page header. Do not unnecessarily remove PayScope branding from other pages or application surfaces where it is still required.

**Acceptance criteria:**
- The Home page has no PayScope logo in its header.
- The profile avatar appears at the top-left of the Home page.
- The header remains aligned and usable on mobile, tablet, and desktop widths.

## 3. Add four icon-led quick-access options to the Home page

Use the attached screenshot as the layout reference: a clean row/grid of evenly spaced icons, with each option's text label directly below its icon. Recreate this interaction pattern using PayScope's design system.

Display these four options on the Home page:

1. **Analysis**
2. **Subscriptions**
3. **Savings Simulator**
4. **Graph Analysis**

**Visual requirements:**

- Give each option a clear, recognizable icon that matches its purpose. Reuse the project's current icon library and styling conventions; do not use emoji as the primary interface icons.
- Keep all four options visually consistent in icon size, hit area, spacing, alignment, and typography.
- Place the text label directly underneath each icon and show the exact labels listed above.
- Use a compact, icon-first arrangement similar to the reference screenshot. On wider screens, keep the four options aligned in one row when space allows. On narrow screens, use a responsive layout that avoids clipped labels, horizontal overflow, or tiny tap targets (a two-by-two grid is acceptable when needed).
- Make each option a real navigation action connected to the existing page/route for that feature. Do not create duplicate pages or dead/inert controls.
- Provide visible hover, focus, pressed, and active states that work in both themes.

**Navigation mapping:**

- **Analysis** → existing Analysis page.
- **Subscriptions** → existing Subscriptions page.
- **Savings Simulator** → existing Savings Simulator feature/page.
- **Graph Analysis** → the dedicated Neo4j/graph experience described in Requirement 6.

**Acceptance criteria:**
- All four options appear on the Home page with the exact labels.
- Each icon and label is aligned consistently and remains legible in both themes.
- Each option navigates to the correct existing feature.

## 4. Remove the Imported Statements block from the Profile page

**Required changes:**

- Remove the Imported Statements block/section from the Profile page UI.
- Remove any now-unnecessary Profile-page-only layout, button, or empty-state elements that exist solely for this block.
- Do not delete imported statement records, storage, APIs, statement processing, or statement-history functionality.
- Do not remove statement history from any separate History area or other page where it is intentionally used. This requirement is only about removing the block from the Profile page.

**Acceptance criteria:**
- The Profile page no longer displays an Imported Statements block.
- Existing imported statements remain available through the app's intended statement/history flow.
- No unrelated Profile features are broken or removed.

## 5. Show the user's real Google profile picture instead of a random letter

**Problem:** The UI currently shows a random letter/avatar character instead of the user's Google account profile picture.

**Required changes:**

- Inspect the current authentication provider and user/session object. Use the authenticated Google account's real profile image URL (for example, the provider's `photoURL`, `picture`, or equivalent field already returned by the project's auth integration).
- Bind the profile avatar to the currently authenticated user's profile image. Do not hard-code an image URL, generate a random letter, or use another user's image.
- Reuse the same authenticated avatar wherever the app displays the user's profile identity, including the Home-page top-left avatar and Profile page identity area if present.
- Handle missing, invalid, or still-loading image URLs gracefully. Use a neutral user/avatar icon as the fallback; do not fall back to a random initial/letter.
- Preserve the image's aspect ratio and crop it neatly into the existing avatar shape. Add appropriate alternative text/accessibility labeling.
- Do not request additional Google permissions or change the authentication flow just to display the existing profile picture.

**Acceptance criteria:**
- A signed-in Google user sees their own Google profile picture.
- The picture is not hard-coded and changes correctly when a different user signs in.
- If the provider does not supply a photo, a neutral avatar placeholder appears without broken-image UI or random letters.

## 6. Remove Neo4j Graph from Analysis and make Graph Analysis a separate Home feature

**Required changes:**

- Remove the Neo4j graph/visualization section, embedded graph panel, or graph widget from the Analysis page.
- Keep the Analysis page focused on its existing financial analysis functionality. Do not remove unrelated analysis metrics, charts, insights, or calculations.
- Keep the Neo4j integration and graph functionality intact, but expose it as a separate, dedicated **Graph Analysis** feature accessible from the Home-page quick-access option in Requirement 3.
- If a dedicated Graph Analysis route/page already exists, reuse it. Otherwise, create a dedicated page using the existing Neo4j integration and current app layout conventions; do not duplicate the graph implementation.
- Ensure the dedicated Graph Analysis page can load, render, and handle loading, empty, error, and successful data states. Preserve existing connection/configuration behavior and do not fabricate graph data.
- Apply the shared Light/Dark theme colors to the complete graph experience as described in Requirement 1.

**Acceptance criteria:**
- The Analysis page no longer contains the Neo4j graph.
- The Home page has a separate Graph Analysis quick-access option.
- Selecting Graph Analysis opens the dedicated Neo4j graph experience.
- Existing graph functionality and real data integration continue to work.

---

## Implementation quality and validation requirements

After implementing the changes:

1. Run the project's available linting, type-checking, and build commands. Fix errors introduced by these changes. Do not claim a command passed unless it was actually run successfully.
2. Test the Home page, Analysis page, Profile page, and Graph Analysis page in both Light and Dark Mode.
3. Verify the header/avatar placement and quick-access layout at mobile, tablet, and desktop widths.
4. Test navigation for all four Home-page options.
5. Test the Google profile image with a valid image URL, a missing URL, and an image-loading failure.
6. Confirm the Profile page no longer contains Imported Statements while existing statement records and statement history remain intact.
7. Confirm Analysis still displays its non-graph financial analysis features and Graph Analysis still loads the actual Neo4j data.
8. Check browser console and server logs for new errors, missing keys, failed requests, or theme-related rendering issues.

## Final report expected from the coding agent

When finished, provide:

- A concise summary of the changes implemented.
- The exact files/components modified and any new files/routes created.
- The routes used for Analysis, Subscriptions, Savings Simulator, and Graph Analysis.
- The authentication field used for the Google profile picture and the fallback behavior.
- The theme tokens or shared theme mechanism used to make all text and graph labels theme-aware.
- The lint, type-check, build, and manual validation results. Clearly identify anything that could not be tested.
- Any remaining issues or assumptions. Do not report incomplete work as completed.

**Important:** Implement these six changes in the existing PayScope codebase, follow the attached screenshot's icon-and-label layout as a visual reference, and preserve all unrelated existing functionality.
