"""Verified Firebase identities and isolated, expiring guest sessions."""
import hashlib
import time
import uuid
from contextvars import ContextVar
from dataclasses import dataclass
from typing import Optional

import jwt
import requests
from cachecontrol import CacheControl
from fastapi import Depends, HTTPException, Request
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from google.auth.transport.requests import Request as GoogleRequest
from google.oauth2.id_token import verify_firebase_token
from starlette.concurrency import run_in_threadpool

from app.core.config import settings
from app.db.session import get_db
from sqlalchemy.orm import Session


@dataclass(frozen=True)
class Identity:
    workspace_key: str
    display_name: str
    is_demo: bool = False
    email: Optional[str] = None


current_identity: ContextVar[Optional[Identity]] = ContextVar("identity", default=None)
bearer = HTTPBearer(auto_error=False)
certificate_request = GoogleRequest(session=CacheControl(requests.Session()))
SESSION_COOKIE = "payscope_token"
DEPLOYED_WEB_ORIGINS = (
    "https://payscope-web.onrender.com",
    "https://pay-scope-lake.vercel.app",
    "https://payscope.vercel.app",
)


def public_endpoint(request: Request) -> bool:
    path = request.url.path.rstrip("/")
    prefix = settings.API_V1_STR
    return (
        path in {"", "/"}
        or (request.method == "POST" and path == f"{prefix}/auth/guest")
        or (request.method in {"GET", "HEAD"} and path in {f"{prefix}/health", f"{prefix}/ready", f"{prefix}/demo/sample-files"})
        or (request.method in {"GET", "HEAD"} and path.startswith(f"{prefix}/demo/sample-files/"))
    )


def allowed_browser_origins() -> list[str]:
    configured = list(settings.BACKEND_CORS_ORIGINS)
    if settings.ENVIRONMENT.lower() == "production":
        # Keep the existing PayScope deployments, without trusting shared hosting suffixes.
        configured.extend(DEPLOYED_WEB_ORIGINS)
    return list(dict.fromkeys(configured))


def is_allowed_origin(origin: Optional[str]) -> bool:
    return bool(origin and origin in allowed_browser_origins())


def check_cookie_origin(request: Request) -> None:
    """Reject unauthorized cross-origin mutations while permitting authorized frontends."""
    if request.method in {"GET", "HEAD", "OPTIONS"}:
        return
    origin = request.headers.get("origin")
    if origin and not is_allowed_origin(origin):
        raise HTTPException(403, "Request origin is not allowed.")
    # Scripted cookie writes must carry an explicit Origin. Non-browser API clients
    # should use Bearer auth; a cookie alone must not bypass this CSRF check.
    if request.cookies.get(SESSION_COOKIE) and not origin:
        raise HTTPException(403, "Cookie-authenticated writes require an Origin header.")


def verify_token(token: str) -> Identity:
    try:
        # The issuer only chooses a verifier; both branches verify the signature.
        issuer = jwt.decode(token, options={"verify_signature": False}).get("iss")
        if issuer == "payscope-guest":
            if not settings.ALLOW_GUEST_SESSIONS or len(settings.API_SESSION_SECRET) < 32:
                raise ValueError("Guest sessions disabled")
            claims = jwt.decode(token, settings.API_SESSION_SECRET, algorithms=["HS256"],
                                issuer="payscope-guest", audience="payscope-api",
                                options={"require": ["exp", "iat", "sub"]})
            guest_id = uuid.UUID(claims["sub"])
            return Identity(f"guest:{guest_id}", "Demo User", True)
        if not settings.FIREBASE_PROJECT_ID:
            raise ValueError("Firebase project not configured")
        claims = verify_firebase_token(token, certificate_request, audience=settings.FIREBASE_PROJECT_ID)
        if not claims.get("sub") or claims.get("iss") != f"https://securetoken.google.com/{settings.FIREBASE_PROJECT_ID}":
            raise ValueError("Invalid identity")
        key = hashlib.sha256(claims["sub"].encode()).hexdigest()
        email = claims.get("email") if claims.get("email_verified") else None
        return Identity(f"firebase:{key}", str(claims.get("name", "PayScope User"))[:255], email=email)
    except Exception as exc:
        raise HTTPException(401, "Your session is invalid or expired. Please sign in again.",
                            headers={"WWW-Authenticate": "Bearer"}) from exc


async def require_identity(request: Request, credentials: Optional[HTTPAuthorizationCredentials] = Depends(bearer), db: Session = Depends(get_db)):
    if public_endpoint(request):
        yield None
        return
    token_value = credentials.credentials if credentials else request.cookies.get(SESSION_COOKIE)
    if not token_value:
        raise HTTPException(401, "Sign in to access your workspace.", headers={"WWW-Authenticate": "Bearer"})
    if not credentials:
        check_cookie_origin(request)
    identity = await run_in_threadpool(verify_token, token_value)
    token = current_identity.set(identity)
    try:
        from app.repositories.user_repository import UserRepository
        user = await run_in_threadpool(UserRepository.get_or_create_demo_user, db)
        requested = request.query_params.get("user_id")
        if requested and requested != str(user.id):
            raise HTTPException(403, "You cannot access another workspace.")
        yield identity
    finally:
        current_identity.reset(token)


def create_guest_token() -> dict:
    if not settings.ALLOW_GUEST_SESSIONS or len(settings.API_SESSION_SECRET) < 32:
        raise HTTPException(503, "Guest sessions are not configured.")
    guest_id = str(uuid.uuid4())
    now = int(time.time())
    token = jwt.encode({"sub": guest_id, "iss": "payscope-guest", "aud": "payscope-api",
                        "iat": now, "exp": now + 86400}, settings.API_SESSION_SECRET, algorithm="HS256")
    return {"access_token": token, "token_type": "bearer", "expires_in": 86400, "uid": guest_id}
