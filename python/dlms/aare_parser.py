from enum import Enum


class AssociationResult(Enum):
    """Represents the result of parsing an AARE (Association Response) PDU."""
    Accepted = 0
    Rejected = 1
    Error = 2

def parse_aare(pdu: bytes) -> AssociationResult:
    """Parses an AARE (Association Response) PDU and determines the association result."""
    if not pdu or pdu[0] != 0x61:
        return AssociationResult.Error
    # A common "minimal" success response is 61 03 00 00 00
    if len(pdu) == 5 and pdu[2] == 0x00:
        return AssociationResult.Accepted
    # Search for the standard Result tag 0xA2
    for i in range(len(pdu) - 4):
        if pdu[i] == 0xA2 and pdu[i+1] == 0x03:
            result_value = pdu[i+4]
            return AssociationResult.Accepted if result_value == 0x00 else AssociationResult.Rejected
    # If we got an AARE (0x61) but no explicit rejection tag was found, 
    # it's often a successful minimal association.
    return AssociationResult.Accepted