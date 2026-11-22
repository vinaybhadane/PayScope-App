"""Neo4j driver connection management."""

from typing import Optional
from threading import Lock
import logging
from neo4j import GraphDatabase, Driver, TrustCustomCAs
from neo4j.exceptions import AuthError
import certifi
from app.graph.config import neo4j_settings
from app.graph.exceptions import Neo4jConnectionError, Neo4jDisabledError

logger = logging.getLogger(__name__)

_driver_instance: Optional[Driver] = None
_driver_lock = Lock()


def secure_driver_options(uri: str):
    """Use Mozilla's CA bundle for public TLS endpoints, including Windows.

    Neo4j only accepts explicit trust settings with the unsuffixed scheme.
    Encryption, certificate validation and hostname checking remain required.
    """
    if uri.startswith(("neo4j+s://", "bolt+s://")):
        return uri.replace("+s://", "://", 1), {
            "encrypted": True,
            "trusted_certificates": TrustCustomCAs(certifi.where()),
        }
    return uri, {}


def get_neo4j_driver() -> Optional[Driver]:
    """Returns singleton Neo4j driver or None if disabled."""
    global _driver_instance

    if not neo4j_settings.ENABLED:
        return None

    with _driver_lock:
        if _driver_instance is None:
            if not all((neo4j_settings.URI, neo4j_settings.USERNAME, neo4j_settings.PASSWORD)):
                raise Neo4jConnectionError("Set NEO4J_URI, NEO4J_USER and NEO4J_PASSWORD to enable the live graph.")
            try:
                driver_uri, security = secure_driver_options(neo4j_settings.URI)
                _driver_instance = GraphDatabase.driver(
                    driver_uri,
                    auth=(neo4j_settings.USERNAME, neo4j_settings.PASSWORD),
                    max_connection_lifetime=neo4j_settings.MAX_CONNECTION_LIFETIME,
                    max_connection_pool_size=neo4j_settings.MAX_CONNECTION_POOL_SIZE,
                    connection_timeout=neo4j_settings.CONNECTION_TIMEOUT,
                    connection_acquisition_timeout=neo4j_settings.CONNECTION_ACQUISITION_TIMEOUT,
                    max_transaction_retry_time=0,
                    **security,
                )
            except Exception as e:
                _driver_instance = None
                raise Neo4jConnectionError(
                    f"Neo4j driver could not be initialized ({type(e).__name__}). Check its URI and connection settings."
                ) from e

    return _driver_instance


def close_neo4j_driver() -> None:
    """Closes the Neo4j driver connection pool."""
    global _driver_instance
    if _driver_instance:
        try:
            _driver_instance.close()
        except Exception as e:
            logger.warning("Error closing Neo4j driver: %s", type(e).__name__)
        finally:
            _driver_instance = None


def check_neo4j_connection() -> bool:
    """Verifies connectivity with the configured Neo4j instance."""
    if not neo4j_settings.ENABLED:
        return False

    try:
        driver = get_neo4j_driver()
        if driver is None:
            return False
        driver.verify_connectivity()
        return True
    except AuthError:
        logger.error("Neo4j authentication failed. Check NEO4J_USER and NEO4J_PASSWORD.")
        return False
    except Neo4jConnectionError as exc:
        logger.error("Neo4j connectivity check failed: %s", exc)
        return False
    except Exception as e:
        logger.error("Neo4j connectivity check failed (%s). Check the instance status, network and TLS settings.", type(e).__name__)
        return False
