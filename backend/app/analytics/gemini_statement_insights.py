"""Ask Gemini to explain server-calculated facts without sending statement text."""
import json
import logging
import re

import requests
from pydantic import BaseModel, Field, ValidationError

from app.core.ai_security import text_risks
from app.core.config import settings

logger = logging.getLogger(__name__)


class Highlight(BaseModel):
    fact_id: int
    explanation: str = Field(min_length=12, max_length=250)


class Highlights(BaseModel):
    highlights: list[Highlight] = Field(min_length=1, max_length=6)


class GeminiUnavailable(Exception):
    pass


def comparison_facts(comparison):
    """Use response array indexes as references. Send no filenames, names or IDs."""
    facts = []
    for index, total in enumerate(comparison["totals"]):
        for field in ("inflow", "outflow", "net"):
            facts.append({"kind": field, "index": index, "currency": total["currency"],
                          "previous": total["previous"][field], "current": total["current"][field],
                          "delta": total["delta"][field]})
    for index, change in enumerate(comparison["changes"]):
        facts.append({"kind": change["kind"], "index": index, "currency": change["currency"],
                      "previous": change["previous_amount"], "current": change["current_amount"]})
    for index, flow in enumerate(comparison["new_flows"][:12]):
        facts.append({"kind": f"new_{flow['direction']}", "index": index,
                      "currency": flow["currency"], "current": flow["amount"]})
    return facts[:80]


def explain_comparison(comparison):
    if not settings.GEMINI_API_KEY:
        raise GeminiUnavailable("Gemini is unavailable until the API key is configured on the server.")
    facts = comparison_facts(comparison)
    if not facts:
        raise ValueError("Select two statements with completed payments to get a Gemini explanation.")
    model = settings.GEMINI_MODEL.removeprefix("models/")
    if not re.fullmatch(r"[A-Za-z0-9._-]+", model):
        raise GeminiUnavailable("The server's Gemini model setting is invalid.")
    try:
        response = requests.post(
            f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent",
            headers={"x-goog-api-key": settings.GEMINI_API_KEY},
            json={
                "systemInstruction": {"parts": [{"text": (
                    "Explain the most useful facts in this bank statement comparison in plain English. "
                    "Choose up to six fact IDs supplied in the input. Give each one short sentence, "
                    "under 24 words. The interface will supply the names and exact amounts. Never include "
                    "digits, currency symbols, names, percentages, or facts that were not supplied. "
                    "A price change is a difference in observed charges. Payment not seen only means no "
                    "matching payment appeared in the selected statement; it is not evidence of cancellation. "
                    "Closure recorded means user reported, while closed means documented. New credit/debit "
                    "means first seen in this comparison, not necessarily the first ever. Do not speculate "
                    "about financial health, service usage, motives, or why a change occurred. If periods "
                    "overlap, do not describe the change as month over month. Return JSON only."
                )}]},
                "contents": [{"parts": [{"text": json.dumps({
                    "overlapping_dates": comparison["overlapping_dates"],
                    "facts": [{"fact_id": i, **{k: v for k, v in f.items() if k != "index"}}
                              for i, f in enumerate(facts)],
                })}]}],
                "generationConfig": {
                    "responseMimeType": "application/json", "responseJsonSchema": Highlights.model_json_schema(),
                    "temperature": 0.2, "maxOutputTokens": 1800,
                },
            },
            timeout=(5, 22),
        )
        response.raise_for_status()
        candidate = response.json()["candidates"][0]
        if candidate.get("finishReason") != "STOP":
            raise ValueError("Incomplete Gemini output")
        payload = "".join(part.get("text", "") for part in candidate["content"]["parts"] if not part.get("thought"))
        answer = Highlights.model_validate_json(payload)
    except (requests.RequestException, ValidationError, ValueError, KeyError, IndexError, TypeError) as exc:
        status = exc.response.status_code if isinstance(exc, requests.HTTPError) and exc.response is not None else None
        logger.warning("Gemini statement explanation failed: %s status=%s", type(exc).__name__, status)
        if status == 429:
            raise GeminiUnavailable("Gemini has reached its current quota. Try again later.") from exc
        if status in {401, 403}:
            raise GeminiUnavailable("Gemini rejected the server credentials or project access.") from exc
        if status == 404:
            raise GeminiUnavailable("The configured Gemini model is unavailable for this project.") from exc
        raise GeminiUnavailable("Gemini could not explain this comparison. Try again in a moment.") from exc

    seen = set()
    highlights = []
    for item in answer.highlights:
        explanation = item.explanation.strip()
        if (not 0 <= item.fact_id < len(facts) or item.fact_id in seen or len(explanation) < 12
                or re.search(r"[\d$%\u20b9\u20ac\u00a3]", explanation) or text_risks(explanation)):
            continue
        fact = facts[item.fact_id]
        if fact["kind"] == "payment_not_seen" and re.search(r"\b(?:closed|cancelled|canceled|ended|stopped)\b", explanation, re.I):
            continue
        highlights.append({"kind": fact["kind"], "index": fact["index"], "explanation": explanation})
        seen.add(item.fact_id)
    if not highlights:
        raise GeminiUnavailable("Gemini's response did not match the statement evidence. Try again.")
    return {"provider": "Gemini", "model": model, "highlights": highlights}
