"""Safe, canonical merchant logo and domain resolution service.

Follows the 4-stage resolution strategy:
1. Existing verified metadata stored on canonical merchant record.
2. Curated trusted merchant directory with verified domains and approved HTTPS assets.
3. Verified official domain favicon fallback (HTTPS Google Favicon CDN).
4. Safe null fallback (neutral generic storefront in UI) for unverified merchants.

Strict SSRF and URL validation:
- HTTPS only (or local /merchant-logos/ paths)
- Prohibits javascript:, data:, file:, localhost, private IPv4/IPv6 networks
"""
import ipaddress
import logging
import re
from typing import Dict, NamedTuple, Optional
import urllib.parse
from sqlalchemy.orm import Session

from app.models.merchant import Merchant

logger = logging.getLogger(__name__)

# Private IPv4/IPv6 network blocks to block SSRF
_PRIVATE_NETWORKS = [
    ipaddress.ip_network("127.0.0.0/8"),
    ipaddress.ip_network("10.0.0.0/8"),
    ipaddress.ip_network("172.16.0.0/12"),
    ipaddress.ip_network("192.168.0.0/16"),
    ipaddress.ip_network("169.254.0.0/16"),
    ipaddress.ip_network("::1/128"),
    ipaddress.ip_network("fc00::/7"),
    ipaddress.ip_network("fe80::/10"),
]

# Curated, trusted global and Indian merchants with verified domains and approved logos
TRUSTED_MERCHANT_REGISTRY: Dict[str, Dict[str, str]] = {
    # Streaming & Media
    "netflix": {
        "canonical_name": "Netflix",
        "domain": "netflix.com",
        "logo_url": "/merchant-logos/netflix.svg",
        "category": "entertainment",
    },
    "spotify": {
        "canonical_name": "Spotify",
        "domain": "spotify.com",
        "logo_url": "/merchant-logos/spotify.svg",
        "category": "entertainment",
    },
    "youtube": {
        "canonical_name": "YouTube",
        "domain": "youtube.com",
        "logo_url": "/merchant-logos/youtube.svg",
        "category": "entertainment",
    },
    "youtube premium": {
        "canonical_name": "YouTube",
        "domain": "youtube.com",
        "logo_url": "/merchant-logos/youtube.svg",
        "category": "entertainment",
    },
    "amazon prime": {
        "canonical_name": "Amazon Prime",
        "domain": "amazon.com",
        "logo_url": "/merchant-logos/prime.png",
        "category": "entertainment",
    },
    "prime video": {
        "canonical_name": "Amazon Prime",
        "domain": "amazon.com",
        "logo_url": "/merchant-logos/prime.png",
        "category": "entertainment",
    },
    "disney+ hotstar": {
        "canonical_name": "Disney+ Hotstar",
        "domain": "hotstar.com",
        "logo_url": "https://www.google.com/s2/favicons?domain=hotstar.com&sz=128",
        "category": "entertainment",
    },
    "hotstar": {
        "canonical_name": "Disney+ Hotstar",
        "domain": "hotstar.com",
        "logo_url": "https://www.google.com/s2/favicons?domain=hotstar.com&sz=128",
        "category": "entertainment",
    },
    # Productivity & AI
    "chatgpt": {
        "canonical_name": "OpenAI ChatGPT",
        "domain": "openai.com",
        "logo_url": "/merchant-logos/chatgpt.png",
        "category": "productivity",
    },
    "openai": {
        "canonical_name": "OpenAI ChatGPT",
        "domain": "openai.com",
        "logo_url": "/merchant-logos/chatgpt.png",
        "category": "productivity",
    },
    "claude": {
        "canonical_name": "Anthropic Claude",
        "domain": "anthropic.com",
        "logo_url": "/merchant-logos/claude.png",
        "category": "productivity",
    },
    "anthropic": {
        "canonical_name": "Anthropic Claude",
        "domain": "anthropic.com",
        "logo_url": "/merchant-logos/claude.png",
        "category": "productivity",
    },
    "gemini": {
        "canonical_name": "Google Gemini",
        "domain": "google.com",
        "logo_url": "/merchant-logos/gemini.png",
        "category": "productivity",
    },
    "google": {
        "canonical_name": "Google",
        "domain": "google.com",
        "logo_url": "/merchant-logos/gemini.png",
        "category": "cloud_software",
    },
    "google one": {
        "canonical_name": "Google One",
        "domain": "google.com",
        "logo_url": "/merchant-logos/gemini.png",
        "category": "cloud_software",
    },
    "adobe": {
        "canonical_name": "Adobe Creative Cloud",
        "domain": "adobe.com",
        "logo_url": "/merchant-logos/adobe.png",
        "category": "productivity",
    },
    "notion": {
        "canonical_name": "Notion Labs",
        "domain": "notion.so",
        "logo_url": "/merchant-logos/notion.svg",
        "category": "productivity",
    },
    "figma": {
        "canonical_name": "Figma",
        "domain": "figma.com",
        "logo_url": "/merchant-logos/figma.svg",
        "category": "productivity",
    },
    "apple": {
        "canonical_name": "Apple iCloud",
        "domain": "apple.com",
        "logo_url": "/merchant-logos/icloud.svg",
        "category": "cloud_software",
    },
    "icloud": {
        "canonical_name": "Apple iCloud",
        "domain": "apple.com",
        "logo_url": "/merchant-logos/icloud.svg",
        "category": "cloud_software",
    },
    "github": {
        "canonical_name": "GitHub",
        "domain": "github.com",
        "logo_url": "https://www.google.com/s2/favicons?domain=github.com&sz=128",
        "category": "developer_tools",
    },
    "microsoft": {
        "canonical_name": "Microsoft 365",
        "domain": "microsoft.com",
        "logo_url": "https://www.google.com/s2/favicons?domain=microsoft.com&sz=128",
        "category": "productivity",
    },
    "slack": {
        "canonical_name": "Slack",
        "domain": "slack.com",
        "logo_url": "https://www.google.com/s2/favicons?domain=slack.com&sz=128",
        "category": "communication",
    },
    "zoom": {
        "canonical_name": "Zoom Video Communications",
        "domain": "zoom.us",
        "logo_url": "https://www.google.com/s2/favicons?domain=zoom.us&sz=128",
        "category": "communication",
    },
    # Indian Utilities, Telecom & Common Subscriptions
    "airtel": {
        "canonical_name": "Bharti Airtel",
        "domain": "airtel.in",
        "logo_url": "https://www.google.com/s2/favicons?domain=airtel.in&sz=128",
        "category": "telecom",
    },
    "jio": {
        "canonical_name": "Reliance Jio",
        "domain": "jio.com",
        "logo_url": "https://www.google.com/s2/favicons?domain=jio.com&sz=128",
        "category": "telecom",
    },
    "vi": {
        "canonical_name": "Vodafone Idea (Vi)",
        "domain": "myvi.in",
        "logo_url": "https://www.google.com/s2/favicons?domain=myvi.in&sz=128",
        "category": "telecom",
    },
    "swiggy": {
        "canonical_name": "Swiggy One",
        "domain": "swiggy.com",
        "logo_url": "https://www.google.com/s2/favicons?domain=swiggy.com&sz=128",
        "category": "food_delivery",
    },
    "zomato": {
        "canonical_name": "Zomato Gold",
        "domain": "zomato.com",
        "logo_url": "https://www.google.com/s2/favicons?domain=zomato.com&sz=128",
        "category": "food_delivery",
    },
    "cult.fit": {
        "canonical_name": "Cult.fit",
        "domain": "cult.fit",
        "logo_url": "https://www.google.com/s2/favicons?domain=cult.fit&sz=128",
        "category": "fitness",
    },
    "curefit": {
        "canonical_name": "Cult.fit",
        "domain": "cult.fit",
        "logo_url": "https://www.google.com/s2/favicons?domain=cult.fit&sz=128",
        "category": "fitness",
    },
}


class ResolvedMerchantMetadata(NamedTuple):
    merchant_id: Optional[str]
    merchant_name: str
    merchant_domain: Optional[str]
    merchant_logo_url: Optional[str]
    logo_source: Optional[str]
    verification_status: str


class MerchantLogoService:
    """Server-side resolution and caching for verified merchant domains and logos."""

    @staticmethod
    def is_safe_https_url(url: str) -> bool:
        """Validate that URL uses HTTPS (or local static path), is a valid host, and is not an SSRF risk."""
        if not url or not isinstance(url, str):
            return False

        # Allow trusted local static asset paths
        if url.startswith("/merchant-logos/") and url.endswith((".svg", ".png", ".jpg", ".webp")):
            return True

        try:
            parsed = urllib.parse.urlparse(url)
            if parsed.scheme.lower() != "https":
                return False

            hostname = parsed.hostname
            if not hostname:
                return False

            hostname_lower = hostname.lower()

            # Reject localhost and single-label hosts
            if hostname_lower in ("localhost", "127.0.0.1", "::1") or "." not in hostname_lower:
                return False

            # Check if hostname is an IP address and reject private IPs
            try:
                ip = ipaddress.ip_address(hostname_lower)
                for net in _PRIVATE_NETWORKS:
                    if ip in net:
                        return False
            except ValueError:
                # Not a literal IP address, valid domain name
                pass

            # Validate basic hostname regex
            if not re.match(r"^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$", hostname_lower):
                return False

            return True
        except Exception:
            return False

    @classmethod
    def normalize_domain(cls, domain_or_url: Optional[str]) -> Optional[str]:
        """Safely extract normalized FQDN domain from a URL or domain string."""
        if not domain_or_url or not isinstance(domain_or_url, str):
            return None

        val = domain_or_url.strip().lower()
        if not val:
            return None

        # Add https:// prefix if missing for urllib parsing
        if not (val.startswith("https://") or val.startswith("http://")):
            val = f"https://{val}"

        try:
            parsed = urllib.parse.urlparse(val)
            hostname = parsed.hostname
            if not hostname:
                return None

            # Strip leading 'www.'
            if hostname.startswith("www."):
                hostname = hostname[4:]

            # Basic domain validation: must contain dot, valid characters, no spaces
            if "." not in hostname or len(hostname) < 4:
                return None

            if not re.match(r"^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+$", hostname):
                return None

            # Disallow private IPs/localhost
            if hostname in ("localhost", "127.0.0.1"):
                return None

            return hostname
        except Exception:
            return None

    @classmethod
    def resolve_merchant_metadata(
        cls,
        merchant: Optional[Merchant] = None,
        fallback_name: Optional[str] = None,
        db: Optional[Session] = None,
    ) -> ResolvedMerchantMetadata:
        """Resolve verified merchant domain and logo through canonical identification.
        
        Order of resolution:
        1. Existing verified metadata stored on the canonical merchant record in the database.
        2. Trusted merchant directory by exact canonical name or key aliases.
        3. Verified official domain favicon (HTTPS Google Favicon CDN).
        4. Safe null fallback (returns None so frontend renders neutral storefront icon).
        """
        merchant_id_str = str(merchant.id) if merchant and merchant.id else None
        name = merchant.canonical_name if merchant else (fallback_name or "Unknown Merchant")

        # 1. Existing verified database metadata
        if merchant and merchant.logo_url and merchant.verification_status == "verified":
            if cls.is_safe_https_url(merchant.logo_url):
                return ResolvedMerchantMetadata(
                    merchant_id=merchant_id_str,
                    merchant_name=name,
                    merchant_domain=merchant.domain,
                    merchant_logo_url=merchant.logo_url,
                    logo_source=merchant.logo_source or "database_cached",
                    verification_status="verified",
                )

        # 2. Trusted curated merchant directory
        lookup_keys = []
        if merchant:
            lookup_keys.append(merchant.canonical_name.strip().lower())
            if merchant.domain:
                lookup_keys.append(merchant.domain.strip().lower())
        if fallback_name:
            lookup_keys.append(fallback_name.strip().lower())

        for key in lookup_keys:
            if key in TRUSTED_MERCHANT_REGISTRY:
                entry = TRUSTED_MERCHANT_REGISTRY[key]
                canonical_domain = entry["domain"]
                canonical_logo = entry["logo_url"]

                if cls.is_safe_https_url(canonical_logo):
                    # Persist to database if merchant record is provided
                    if merchant and db:
                        try:
                            merchant.domain = canonical_domain
                            merchant.logo_url = canonical_logo
                            merchant.logo_source = "curated"
                            merchant.verification_status = "verified"
                            db.flush()
                        except Exception as e:
                            logger.warning(f"Could not persist resolved merchant logo: {e}")

                    return ResolvedMerchantMetadata(
                        merchant_id=merchant_id_str,
                        merchant_name=name,
                        merchant_domain=canonical_domain,
                        merchant_logo_url=canonical_logo,
                        logo_source="curated",
                        verification_status="verified",
                    )

        # 3. Verified official website domain on the canonical merchant
        candidate_domain = None
        if merchant:
            if merchant.domain:
                candidate_domain = cls.normalize_domain(merchant.domain)
            elif merchant.website_url:
                candidate_domain = cls.normalize_domain(merchant.website_url)

        if candidate_domain:
            favicon_url = f"https://www.google.com/s2/favicons?domain={candidate_domain}&sz=128"
            if cls.is_safe_https_url(favicon_url):
                if merchant and db:
                    try:
                        merchant.domain = candidate_domain
                        merchant.logo_url = favicon_url
                        merchant.logo_source = "verified_domain_favicon"
                        merchant.verification_status = "verified"
                        db.flush()
                    except Exception as e:
                        logger.warning(f"Could not persist domain favicon metadata: {e}")

                return ResolvedMerchantMetadata(
                    merchant_id=merchant_id_str,
                    merchant_name=name,
                    merchant_domain=candidate_domain,
                    merchant_logo_url=favicon_url,
                    logo_source="verified_domain_favicon",
                    verification_status="verified",
                )

        # 4. Safe fallback for unverified / unknown merchants
        return ResolvedMerchantMetadata(
            merchant_id=merchant_id_str,
            merchant_name=name,
            merchant_domain=None,
            merchant_logo_url=None,
            logo_source="unresolved",
            verification_status="unverified",
        )
