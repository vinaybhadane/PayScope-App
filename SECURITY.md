# Credentials and client configuration

Do not commit `.env`, `firebase.local.properties`, downloaded `google-services*.json`, signing keys, service-account JSON, or access tokens. The repository ignores these files. Build configuration is documented in `firebase.properties.example` and `backend/.env.example`.

Enable the included history-scanning pre-push hook with `git config core.hooksPath .githooks`. Run `python scripts/check_public_source.py` to check the current branch manually. The scanner reports file paths and credential categories, never matched values.

Firebase Android client keys identify an app/project and are embedded in the compiled APK. Moving them out of Git does not make them secret inside an APK. Restrict them to the Firebase APIs required by the app, review application restrictions, and register the appropriate Android package/signing certificates. Never enable Generative Language API on a publicly accessible Firebase client key. Use a separate server-only Gemini key in Render.

Google documents these requirements at https://firebase.google.com/docs/projects/api-keys.

If a key is flagged or exposed, check its restrictions in the owning Google Cloud project. Replace or revoke unrestricted or compromised keys, update the ignored local configuration, rebuild the APK, and verify sign-in before removing the old key. Do not mark a GitHub alert as revoked until revocation is confirmed.

Removing a key from the current branch does not revoke it. Old commits may remain in cached GitHub views, forks, or existing clones after a history rewrite. See https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository.
