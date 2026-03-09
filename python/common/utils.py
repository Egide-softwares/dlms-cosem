
def print_hex(label: str, data: bytes) -> None:
    """Prints the given data in hexadecimal format with a label."""
    print(f"{label}: {data.hex()}")