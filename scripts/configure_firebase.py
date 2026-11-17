"""Create ignored Android build settings from a local Firebase client download."""
import argparse
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def extract_config(document: dict, package: str = "com.payscope.app") -> dict[str, str]:
    client = next((item for item in document.get("client", [])
                   if item.get("client_info", {}).get("android_client_info", {}).get("package_name") == package), None)
    if client is None:
        raise ValueError("Firebase configuration has no matching Android package.")
    project = document.get("project_info", {})
    oauth = client.get("oauth_client", []) + client.get("services", {}).get("appinvite_service", {}).get("other_platform_oauth_client", [])
    web = next((item.get("client_id", "") for item in oauth if item.get("client_type") == 3), "")
    keys = client.get("api_key", [])
    values = {
        "PAYSCOPE_FIREBASE_API_KEY": keys[0].get("current_key", "") if keys else "",
        "PAYSCOPE_FIREBASE_APP_ID": client.get("client_info", {}).get("mobilesdk_app_id", ""),
        "PAYSCOPE_FIREBASE_PROJECT_ID": project.get("project_id", ""),
        "PAYSCOPE_FIREBASE_STORAGE_BUCKET": project.get("storage_bucket", ""),
        "PAYSCOPE_FIREBASE_SENDER_ID": str(project.get("project_number", "")),
        "PAYSCOPE_GOOGLE_WEB_CLIENT_ID": web,
    }
    required = ("PAYSCOPE_FIREBASE_API_KEY", "PAYSCOPE_FIREBASE_APP_ID", "PAYSCOPE_FIREBASE_PROJECT_ID", "PAYSCOPE_GOOGLE_WEB_CLIENT_ID")
    missing = [key for key in required if not values[key]]
    if missing:
        raise ValueError("Firebase configuration is missing: " + ", ".join(missing))
    return values


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--google-services", type=Path, default=ROOT / "app/google-services.json")
    args = parser.parse_args()
    values = extract_config(json.loads(args.google_services.read_text(encoding="utf-8")))
    def escaped(value):
        return value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r")
    target = ROOT / "firebase.local.properties"
    target.write_text("# Local Firebase configuration. Never commit this file.\n" +
                      "\n".join(key + "=" + escaped(value) for key, value in values.items()) + "\n", encoding="utf-8")
    print("Created ignored firebase.local.properties; no configuration values were printed.")


if __name__ == "__main__":
    main()
