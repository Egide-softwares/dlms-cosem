
class ObisCode:
    """OBIS (Object Identification System) code representation."""
    
    bytes: list[int]

    def __init__(self, obis_str: str):
        self.bytes = [int(x) for x in obis_str.split('.')]
        if len(self.bytes) != 6:
            raise ValueError("OBIS code must have 6 segments")
        
    def __init__(self, a: int, b: int, c: int, d: int, e: int, f: int):
        self.bytes = [a, b, c, d, e, f]
        
    def __init__(self):
        self.bytes = [0] * 6  # Default to zeros if not provided

    def __str__(self):
        return '.'.join(str(b) for b in self.bytes)