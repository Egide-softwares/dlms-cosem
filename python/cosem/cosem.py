from python.common.obis import ObisCode
from python.common.types import DlmsValue
from typing import Optional


class CosemObject:
    """Base class for COSEM objects."""

    class_id: int
    obis_code: ObisCode

    def __init__(self, class_id: int, obis_code: ObisCode):
        self.class_id = class_id
        self.obis_code = obis_code

class CosemRegister(CosemObject):
    """COSEM Register object."""

    value: DlmsValue
    scaler_unit: DlmsValue

    def __init__(self, obis_code: ObisCode):
        super().__init__(3, obis_code)
        self.value = DlmsValue(None)
        self.scaler_unit = DlmsValue(None)  # Usually a structure containing scaler and unit    
