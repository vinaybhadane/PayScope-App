"""Public guest session creation; authenticated workspace identity."""
from fastapi import APIRouter, Depends, Request, Response
from sqlalchemy.orm import Session
from app.core.auth import SESSION_COOKIE, bearer, check_cookie_origin, create_guest_token, require_identity
from app.db.session import get_db
from app.repositories.user_repository import UserRepository, resolve_workspace_user_id

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post("/guest")
def guest_session(request: Request, response: Response):
    check_cookie_origin(request)
    response.headers["Cache-Control"] = "no-store"
    session = create_guest_token()
    token = session.get("access_token")
    set_session_cookie(response, token, session["expires_in"])
    return session


def set_session_cookie(response: Response, token: str, max_age: int = 86400):
    response.set_cookie(SESSION_COOKIE, token, httponly=True, secure=True,
                        samesite="none", max_age=max_age, path="/")
    response.headers["Cache-Control"] = "no-store"


@router.post("/session")
def exchange_session(response: Response, credentials=Depends(bearer)):
    from fastapi import HTTPException
    import jwt
    import time
    if not credentials:
        raise HTTPException(401, "A verified Bearer token is required to establish a session.")
    # require_identity has already verified the signature and expiry.
    expires = jwt.decode(credentials.credentials, options={"verify_signature": False})["exp"]
    set_session_cookie(response, credentials.credentials, max(1, min(86400, int(expires - time.time()))))
    return {"authenticated": True}


@router.post("/logout")
def logout(response: Response):
    response.delete_cookie(SESSION_COOKIE, path="/", secure=True, httponly=True, samesite="none")
    response.headers["Cache-Control"] = "no-store"
    return {"authenticated": False}


@router.get("/me", dependencies=[Depends(require_identity)])
def workspace_identity(db: Session = Depends(get_db)):
    user = UserRepository.get_or_create_demo_user(db)
    return {"id": str(user.id), "display_name": user.display_name, "is_demo": user.is_demo}
