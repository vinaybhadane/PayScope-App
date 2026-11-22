"""Import coordination service handling preview and persistence with merchant resolution."""
import hashlib
import logging
import uuid
from datetime import datetime, timezone
from decimal import Decimal
from typing import Any, Dict, List, Optional, Set
from sqlalchemy import select, text
from sqlalchemy.orm import Session
from app.ingestion.column_mapper import ColumnMapper
from app.ingestion.csv_parser import CSVParser
from app.ingestion.pdf_parser import PDFParser
from app.ingestion.file_validator import FileValidator
from app.ingestion.json_parser import JSONParser
from app.ingestion.schema_normalizer import SchemaNormalizer
from app.ingestion.xlsx_parser import XLSXParser
from app.ingestion.image_parser import ImageStatementParser
from app.matching.config import DEFAULT_MATCHING_CONFIG
from app.matching.resolver import MerchantResolver
from app.matching.review_service import ReviewService
from app.models.import_record import ImportRecord
from app.models.payment_notification import PaymentNotification
from app.models.transaction import Transaction
from app.models.statement_duplicate import StatementDuplicate
from app.models.account import Account
from app.repositories.import_repository import ImportRepository
from app.analytics.autodebit_keyword_service import analyze_transaction_descriptor

logger = logging.getLogger(__name__)


class ImportService:
    """Coordinates parsing, preview, schema validation, and database persistence."""

    @staticmethod
    def _parse_content(filename: str, content: bytes) -> tuple[str, List[str], List[Dict[str, Any]]]:
        """Parse content based on file extension."""
        lower = filename.lower()
        if lower.endswith(".csv"):
            headers, rows = CSVParser.parse(content)
            return "csv", headers, rows
        elif lower.endswith(".pdf"):
            headers, rows = PDFParser.parse(content)
            return "pdf", headers, rows
        elif lower.endswith((".xlsx", ".xls")):
            headers, rows = XLSXParser.parse(content)
            return "xlsx", headers, rows
        elif lower.endswith(".json"):
            headers, rows = JSONParser.parse(content)
            return "json", headers, rows
        elif any(lower.endswith(ext) for ext in [".png", ".jpg", ".jpeg", ".webp"]):
            headers, rows = ImageStatementParser.parse(content, filename)
            return "image", headers, rows
        else:
            raise ValueError(f"Unsupported file format for '{filename}'. Allowed: CSV, XLSX, JSON, PDF, PNG, JPG, JPEG, WEBP.")

    @classmethod
    def preview(
        cls,
        db: Session,
        user_id: uuid.UUID,
        filename: str,
        content: bytes,
        import_type: str = "statement",  # "statement" or "notifications"
        synthetic_confirmed: bool = True,
        column_overrides: Optional[Dict[str, str]] = None,
    ) -> Dict[str, Any]:
        """Generate unpersisted import preview with validation report and duplicate check."""
        # 1. Pre-flight file validation
        val_res = FileValidator.validate_upload(filename, content, synthetic_confirmed)
        if not val_res.is_valid:
            return {
                "success": False,
                "error": val_res.error_message,
                "file_info": {"filename": filename, "size_bytes": len(content)},
            }

        # 2. Parse file
        try:
            file_type, headers, rows = cls._parse_content(filename, content)
        except Exception as e:
            return {
                "success": False,
                "error": f"Failed to parse {filename}: {str(e)}",
                "file_info": {"filename": filename, "size_bytes": len(content)},
            }

        if not rows:
            return {
                "success": False,
                "error": f"File '{filename}' contains no readable data rows.",
                "file_info": {"filename": filename, "size_bytes": len(content)},
            }

        # 3. Handle by import type
        if import_type == "statement":
            detected_mapping = ColumnMapper.auto_detect(headers)
            final_mapping = ColumnMapper.apply_overrides(detected_mapping, column_overrides)
            mapping_errors = ColumnMapper.validate_mapping(final_mapping, headers)

            if mapping_errors:
                return {
                    "success": False,
                    "error": "Column mapping incomplete: " + "; ".join(mapping_errors),
                    "file_info": {"filename": filename, "size_bytes": len(content), "type": file_type},
                    "headers": headers,
                    "mapping": final_mapping,
                    "available_targets": ColumnMapper.TARGET_COLUMNS,
                    "import_type": "statement", "total_rows": len(rows),
                    "valid_count": 0, "duplicate_count": 0, "invalid_count": 0,
                    "errors": [], "preview_rows": [],
                }

            norm_result = SchemaNormalizer.normalize_transactions(rows, final_mapping)

            # Check existing duplicate external IDs in database
            candidate_ext_ids = [
                r["external_synthetic_id"] for r in norm_result.valid_records if r.get("external_synthetic_id")
            ]
            existing_ids: Set[str] = set()
            if candidate_ext_ids:
                try:
                    stmt = select(Transaction.external_synthetic_id).where(
                        Transaction.user_id == user_id,
                        Transaction.external_synthetic_id.in_(candidate_ext_ids),
                    )
                    existing_ids = set(db.scalars(stmt).all())
                except Exception as exc:
                    logger.warning("Could not query duplicate transaction IDs from DB: %s", exc)

            # Count duplicates vs new
            duplicate_count = sum(1 for r in norm_result.valid_records if r.get("external_synthetic_id") in existing_ids)
            # Preview must also count duplicate IDs within this upload.
            seen_preview = set(existing_ids)
            duplicate_count = 0
            for record in norm_result.valid_records:
                key = record.get("external_synthetic_id")
                duplicate_count += int(key in seen_preview)
                if key:
                    seen_preview.add(key)
            net_valid_count = len(norm_result.valid_records) - duplicate_count

            # Pre-load resolver for diagnostic preview
            try:
                resolver = MerchantResolver.from_db(db)
            except Exception as exc:
                logger.warning("Could not instantiate MerchantResolver from DB: %s", exc)
                resolver = MerchantResolver()

            # Build preview rows (first 15)
            preview_rows = []
            preview_seen = set(existing_ids)
            for r in norm_result.valid_records[:15]:
                is_dup = r.get("external_synthetic_id") in preview_seen
                preview_seen.add(r.get("external_synthetic_id"))
                res = resolver.resolve(r["raw_descriptor"])
                preview_rows.append({
                    "date": r["transaction_date"].strftime("%Y-%m-%d"),
                    "descriptor": r["raw_descriptor"],
                    "amount": float(r["amount"]),
                    "currency": r["currency"],
                    "direction": r["direction"],
                    "channel": r["payment_channel"],
                    "reference": r["payment_reference"],
                    "external_id": r["external_synthetic_id"],
                    "is_duplicate": is_dup,
                    "suggested_merchant": res.merchant_name,
                    "match_status": res.status,
                    "match_confidence": res.confidence_band,
                })

            return {
                "success": True,
                "import_type": "statement",
                "file_info": {
                    "filename": filename,
                    "size_bytes": len(content),
                    "file_type": file_type,
                    "checksum_sha256": val_res.checksum_sha256,
                },
                "headers": headers,
                "mapping": final_mapping,
                "available_targets": ColumnMapper.TARGET_COLUMNS,
                "total_rows": norm_result.total_rows,
                "valid_count": net_valid_count,
                "duplicate_count": duplicate_count,
                "invalid_count": len({err.row for err in norm_result.errors}),
                "quarantined_count": len(norm_result.quarantined_records),
                "errors": [err.to_dict() for err in norm_result.errors[:50]],
                "preview_rows": preview_rows,
            }

        else:  # notifications
            norm_result = SchemaNormalizer.normalize_notifications(rows)

            candidate_ext_ids = [
                r["external_synthetic_id"] for r in norm_result.valid_records if r.get("external_synthetic_id")
            ]
            existing_ids: Set[str] = set()
            if candidate_ext_ids:
                try:
                    stmt = select(PaymentNotification.external_synthetic_id).where(
                        PaymentNotification.user_id == user_id,
                        PaymentNotification.external_synthetic_id.in_(candidate_ext_ids),
                    )
                    existing_ids = set(db.scalars(stmt).all())
                except Exception as exc:
                    logger.warning("Could not query duplicate notification IDs from DB: %s", exc)

            duplicate_count = sum(1 for r in norm_result.valid_records if r.get("external_synthetic_id") in existing_ids)
            net_valid_count = len(norm_result.valid_records) - duplicate_count

            preview_rows = []
            for r in norm_result.valid_records[:15]:
                is_dup = r.get("external_synthetic_id") in existing_ids
                preview_rows.append({
                    "timestamp": r["event_timestamp"].isoformat(),
                    "event_type": r["event_type"],
                    "merchant_hint": r["merchant_hint"],
                    "amount": float(r["amount"]) if r["amount"] is not None else None,
                    "currency": r["currency"],
                    "mechanism": r["payment_mechanism"],
                    "mandate_ref": r["mandate_external_ref"],
                    "message": r["message_text"],
                    "external_id": r["external_synthetic_id"],
                    "is_duplicate": is_dup,
                })

            return {
                "success": True,
                "import_type": "notifications",
                "file_info": {
                    "filename": filename,
                    "size_bytes": len(content),
                    "file_type": file_type,
                    "checksum_sha256": val_res.checksum_sha256,
                },
                "headers": headers,
                "total_rows": norm_result.total_rows,
                "valid_count": net_valid_count,
                "duplicate_count": duplicate_count,
                "invalid_count": len({err.row for err in norm_result.errors}),
                "errors": [err.to_dict() for err in norm_result.errors[:50]],
                "preview_rows": preview_rows,
            }

    @classmethod
    def execute_import(
        cls,
        db: Session,
        user_id: uuid.UUID,
        filename: str,
        content: bytes,
        import_type: str = "statement",
        synthetic_confirmed: bool = True,
        column_overrides: Optional[Dict[str, str]] = None,
        account_id: Optional[uuid.UUID] = None,
    ) -> Dict[str, Any]:
        """Validate, persist records, automatically resolve merchants, and return summary."""
        # 1. Validation
        val_res = FileValidator.validate_upload(filename, content, synthetic_confirmed)
        if not val_res.is_valid:
            raise ValueError(val_res.error_message)

        # 2. Parse
        file_type, headers, rows = cls._parse_content(filename, content)
        if not rows:
            raise ValueError(f"File '{filename}' contains no readable data rows.")

        # Keep a source account so imported payment evidence has connected paths.
        if import_type == "statement" and account_id is None:
            account = db.scalar(select(Account).where(
                Account.user_id == user_id, Account.label == "Imported statements"
            ).order_by(Account.created_at).limit(1))
            if account is None:
                account = Account(user_id=user_id, label="Imported statements", is_synthetic=synthetic_confirmed)
                db.add(account)
                db.flush()
            account_id = account.id

        # 3. Create ImportRecord in DB
        storage_key = f"imports/{user_id}/{uuid.uuid4()}"
        import_record = ImportRepository.create(
            db=db,
            user_id=user_id,
            account_id=account_id,
            filename_display=filename,
            storage_key=storage_key,
            file_type=file_type,
            source_label="bank_statement" if import_type == "statement" else "payment_notifications",
            status="processing",
            row_count=len(rows),
            synthetic_confirmed=synthetic_confirmed,
        )

        accepted_count = 0
        duplicate_count = 0
        rejected_count = 0
        errors_list: List[Dict[str, Any]] = []

        try:
            db.execute(text("SELECT pg_advisory_xact_lock(hashtextextended(:key, 0))"),
                       {"key": "import:" + str(user_id)})
            if import_type == "statement":
                detected_mapping = ColumnMapper.auto_detect(headers)
                final_mapping = ColumnMapper.apply_overrides(detected_mapping, column_overrides)
                mapping_errors = ColumnMapper.validate_mapping(final_mapping, headers)
                if mapping_errors:
                    raise ValueError("Column mapping incomplete: " + "; ".join(mapping_errors))

                norm_result = SchemaNormalizer.normalize_transactions(rows, final_mapping)
                rejected_count = len({err.row for err in norm_result.errors})
                errors_list = [e.to_dict() for e in norm_result.errors]
                from app.models.manual_review_item import ManualReviewItem
                errors_by_row = {}
                quarantined_rows = {item["row"] for item in norm_result.quarantined_records}
                for error in norm_result.errors:
                    if error.row not in quarantined_rows:
                        errors_by_row.setdefault(error.row, []).append(error.to_dict())
                for row_number, row_errors in errors_by_row.items():
                    source = next((row for row in rows if row.get("_row_number") == row_number), {})
                    db.add(ManualReviewItem(user_id=user_id, item_type="statement_parse",
                        raw_descriptor=str(source.get(final_mapping.get("raw_descriptor")) or "Unparsed statement row")[:1000],
                        normalised_descriptor="", reason_code="invalid_statement_row", status="open",
                        candidate_payload={"import_id": str(import_record.id), "row": row_number, "errors": row_errors, "source": source},
                        explanation="; ".join(error["reason"] for error in row_errors)))
                for quarantined in norm_result.quarantined_records:
                    db.add(ManualReviewItem(
                        user_id=user_id, item_type="ocr_security" if "_ocr_security" in quarantined["source"] else "input_security",
                        raw_descriptor=quarantined["descriptor"], normalised_descriptor=quarantined["descriptor"],
                        candidate_payload={"import_id": str(import_record.id), **quarantined},
                        reason_code="security_quarantine", status="open",
                        explanation="Record excluded from automated processing: " + ", ".join(quarantined["reasons"]),
                    ))

                # Check duplicates
                candidate_ext_ids = [
                    r["external_synthetic_id"] for r in norm_result.valid_records if r.get("external_synthetic_id")
                ]
                existing_transactions = {}
                if candidate_ext_ids:
                    stmt = select(Transaction.external_synthetic_id, Transaction.id).where(
                        Transaction.user_id == user_id,
                        Transaction.external_synthetic_id.in_(candidate_ext_ids),
                    )
                    existing_transactions = dict(db.execute(stmt).tuples().all())
                existing_ids = set(existing_transactions)
                seen_duplicates = set()

                # Load resolver for automatic merchant matching
                resolver = MerchantResolver.from_db(db)
                now = datetime.now(timezone.utc)
                review_items_to_queue = []

                # Persist non-duplicate records
                new_records = []
                for r in norm_result.valid_records:
                    payment_key = r.get("external_synthetic_id")
                    if payment_key in existing_ids:
                        duplicate_count += 1
                        related_id = existing_transactions.get(payment_key)
                        if related_id and related_id not in seen_duplicates:
                            db.add(StatementDuplicate(import_id=import_record.id, transaction_id=related_id))
                            seen_duplicates.add(related_id)
                        continue

                    # Automatic merchant resolution
                    resolution = resolver.resolve(r["raw_descriptor"])

                    merchant_id = resolution.merchant_id
                    if resolution.status in ("MATCHED_HIGH", "MATCHED_MEDIUM"):
                        m_status = "matched"
                    elif resolution.status == "NEEDS_REVIEW":
                        m_status = "needs_review"
                    else:
                        m_status = "unmatched"
                        merchant_id = None

                    score_dec = Decimal(str(round(resolution.score, 2))) if resolution.score else None

                    # Auto-debit keyword intelligence analysis
                    autodebit_analysis = analyze_transaction_descriptor(r["raw_descriptor"])
                    resolved_channel = r.get("payment_channel") or "unknown"
                    resolved_status = r["transaction_status"]

                    txn = Transaction(
                        user_id=user_id,
                        account_id=account_id,
                        import_id=import_record.id,
                        external_synthetic_id=r.get("external_synthetic_id"),
                        transaction_date=r["transaction_date"],
                        raw_descriptor=r["raw_descriptor"],
                        normalised_descriptor=resolution.normalised_descriptor or r["normalised_descriptor"],
                        amount=r["amount"],
                        currency=r["currency"],
                        direction=r["direction"],
                        transaction_status=resolved_status,
                        payment_channel=resolved_channel,
                        payment_reference=r["payment_reference"],
                        source_row_number=r["source_row_number"],
                        raw_source_snapshot=r["raw_source_snapshot"],
                        is_synthetic=synthetic_confirmed,
                        # Merchant resolution fields
                        merchant_id=merchant_id,
                        merchant_match_status=m_status,
                        merchant_match_method=resolution.match_method,
                        merchant_match_score=score_dec,
                        merchant_match_confidence=resolution.confidence_band,
                        merchant_match_reason=resolution.explanation,
                        matched_descriptor_alias_id=resolution.matched_alias_id,
                        matcher_version=DEFAULT_MATCHING_CONFIG.matcher_version,
                        matched_at=now,
                    )
                    new_records.append(txn)
                    if r.get("external_synthetic_id"):
                        existing_ids.add(r["external_synthetic_id"])

                    if resolution.status in ("NEEDS_REVIEW", "UNRESOLVED"):
                        review_items_to_queue.append((txn, resolution))

                if new_records:
                    db.add_all(new_records)
                    db.flush()  # Flush so txn.id is populated

                # Add review items for transactions needing review
                for txn, resolution in review_items_to_queue:
                    ReviewService.queue_for_review(db, user_id, txn, resolution)

                accepted_count = len(new_records)

            else:  # notifications
                norm_result = SchemaNormalizer.normalize_notifications(rows)
                rejected_count = len({err.row for err in norm_result.errors})
                errors_list = [e.to_dict() for e in norm_result.errors]

                candidate_ext_ids = [
                    r["external_synthetic_id"] for r in norm_result.valid_records if r.get("external_synthetic_id")
                ]
                existing_ids: Set[str] = set()
                if candidate_ext_ids:
                    stmt = select(PaymentNotification.external_synthetic_id).where(
                        PaymentNotification.user_id == user_id,
                        PaymentNotification.external_synthetic_id.in_(candidate_ext_ids),
                    )
                    existing_ids = set(db.scalars(stmt).all())

                new_records = []
                for r in norm_result.valid_records:
                    if r.get("external_synthetic_id") in existing_ids:
                        duplicate_count += 1
                        continue

                    notif = PaymentNotification(
                        user_id=user_id,
                        import_id=import_record.id,
                        external_synthetic_id=r["external_synthetic_id"],
                        event_timestamp=r["event_timestamp"],
                        event_type=r["event_type"],
                        merchant_hint=r["merchant_hint"],
                        amount=r["amount"],
                        currency=r["currency"],
                        payment_mechanism=r["payment_mechanism"],
                        mandate_external_ref=r["mandate_external_ref"],
                        message_text=r["message_text"],
                        raw_payload=r["raw_payload"],
                        is_synthetic=synthetic_confirmed,
                    )
                    new_records.append(notif)
                    if r.get("external_synthetic_id"):
                        existing_ids.add(r["external_synthetic_id"])

                if new_records:
                    db.add_all(new_records)
                accepted_count = len(new_records)

            # Determine final status
            if accepted_count > 0 and rejected_count == 0 and duplicate_count == 0:
                final_status = "completed"
            elif accepted_count > 0:
                final_status = "partially_completed"
            elif duplicate_count > 0 and accepted_count == 0 and rejected_count == 0:
                final_status = "completed"  # All were already imported duplicates
            else:
                final_status = "failed"

            err_summary = None
            if rejected_count > 0:
                err_summary = f"{rejected_count} row(s) failed validation."

            ImportRepository.update_status(
                db=db,
                import_record=import_record,
                status=final_status,
                accepted_count=accepted_count,
                rejected_count=rejected_count,
                duplicate_count=duplicate_count,
                error_summary=err_summary,
            )

            return {
                "success": True,
                "import_id": str(import_record.id),
                "status": final_status,
                "filename": filename,
                "total_rows": len(rows),
                "accepted_count": accepted_count,
                "duplicate_count": duplicate_count,
                "rejected_count": rejected_count,
                "errors": errors_list[:20],
            }

        except Exception as e:
            ImportRepository.update_status(
                db=db,
                import_record=import_record,
                status="failed",
                error_summary=str(e),
            )
            raise e
