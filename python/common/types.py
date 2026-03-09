from enum import Enum
from python.common.obis import ObisCode


class DlmsTag(Enum):
    Null = 0
    Boolean = 3
    DoubleLong = 5        # int32
    DoubleLongUnsigned = 6 # uint32
    OctetString = 9
    VisibleString = 10
    Integer = 15          # int8
    Long = 16             # int16
    Unsigned = 17         # uint8
    LongUnsigned = 18      # uint16

class DlmsValue:
    """Represents a DLMS/COSEM value"""

    def __init__(self, value: None | int | str | bytes | ObisCode):
        self.value = value

    def __repr__(self):
        return f"DlmsValue(value={self.value})"
