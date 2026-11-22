"""Neutralize formulas in every user-influenced spreadsheet string cell."""
def sanitize_spreadsheet_field(value):
    if not isinstance(value, str):
        return value
    # Also handle whitespace/control padding used to disguise formula prefixes.
    if value.startswith(("=", "+", "-", "@", "\t", "\r", "\n")) or value.lstrip().startswith(("=", "+", "-", "@")):
        return "'" + value
    return value
