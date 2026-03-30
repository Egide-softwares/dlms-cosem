package com.pivotaccess.dlmscosem.polling.data;

import com.pivotaccess.dlmscosem.cosem.objects.ClockObject;
import com.pivotaccess.dlmscosem.cosem.objects.CosemDataObject;
import com.pivotaccess.dlmscosem.cosem.objects.RegisterObject;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class MeterReading {
    String meterId;
    String transportId;
    Instant timestamp;

    CosemDataObject serialNumber;
    CosemDataObject logicalDeviceName;
    CosemDataObject invocationCounter;
    ClockObject clock;
    RegisterObject activeEnergyImport;
    RegisterObject activeEnergyExport;
    RegisterObject voltageL1;
    RegisterObject voltageL2;
    RegisterObject voltageL3;

    String errorMsg;
    boolean isSuccessful;

    public MeterReading(String meterId, String transportId) {
        this.meterId = meterId;
        this.transportId = transportId;
        this.timestamp = Instant.now();
    }

    public String getMeterId() { return meterId; }
    public void setMeterId(String meterId) { this.meterId = meterId; }

    public String getTransportId() { return transportId; }
    public void setTransportId(String transportId) { this.transportId = transportId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public CosemDataObject getSerialNumber() { return serialNumber; }
    public void setSerialNumber(CosemDataObject serialNumber) { this.serialNumber = serialNumber; }

    public CosemDataObject getLogicalDeviceName() { return logicalDeviceName; }
    public void setLogicalDeviceName(CosemDataObject logicalDeviceName) { this.logicalDeviceName = logicalDeviceName; }

    public CosemDataObject getInvocationCounter() { return invocationCounter; }
    public void setInvocationCounter(CosemDataObject invocationCounter) { this.invocationCounter = invocationCounter; }

    public ClockObject getClock() { return clock; }
    public void setClock(ClockObject clock) { this.clock = clock; }

    public RegisterObject getActiveEnergyImport() { return activeEnergyImport; }
    public void setActiveEnergyImport(RegisterObject activeEnergyImport) { this.activeEnergyImport = activeEnergyImport; }

    public RegisterObject getActiveEnergyExport() { return activeEnergyExport; }
    public void setActiveEnergyExport(RegisterObject activeEnergyExport) { this.activeEnergyExport = activeEnergyExport; }

    public RegisterObject getVoltageL1() { return voltageL1; }
    public void setVoltageL1(RegisterObject voltageL1) { this.voltageL1 = voltageL1; }

    public RegisterObject getVoltageL2() { return voltageL2; }
    public void setVoltageL2(RegisterObject voltageL2) { this.voltageL2 = voltageL2; }

    public RegisterObject getVoltageL3() { return voltageL3; }
    public void setVoltageL3(RegisterObject voltageL3) { this.voltageL3 = voltageL3; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public boolean isSuccessful() { return isSuccessful; }
    public void setSuccessful(boolean successful) { isSuccessful = successful; }

    @Override
    public String toString() {
        if (!isSuccessful) return "FAILED [" + meterId + "] [" + transportId + "]: " + errorMsg;
        return "MeterReading(" +
                "meterId='" + meterId + '\'' +
                ", transportId='" + transportId + '\'' +
                ", timestamp=" + timestamp +
                ", serialNumber=" + (serialNumber != null ? serialNumber.getValue().getOctetStringAsTextOrNull(StandardCharsets.UTF_8) : "NA") +
                ", logicalDeviceName=" + (logicalDeviceName != null ? logicalDeviceName.getValue().getOctetStringAsTextOrNull(StandardCharsets.UTF_8) : "NA") +
                ", invocationCounter=" + (invocationCounter != null ? invocationCounter.getValue().getLongOrNull() : "NA") +
                ", clock=" + (clock != null ? clock.getTime().format(DateTimeFormatter.ISO_DATE_TIME) : "NA") +
                ", activeEnergyImport=" + (activeEnergyImport != null ? activeEnergyImport.getPhysicalValueOrNull() + " " + activeEnergyImport.getUnitSymbol() : "NA") +
                ", activeEnergyExport=" + (activeEnergyExport != null ? activeEnergyExport.getPhysicalValueOrNull() + " " + activeEnergyExport.getUnitSymbol() : "NA") +
                ", voltageL1=" + (voltageL1 != null ? voltageL1.getPhysicalValueOrNull() + " " + voltageL1.getUnitSymbol() : "NA") +
                ", voltageL2=" + (voltageL2 != null ? voltageL2.getPhysicalValueOrNull() + " " + voltageL2.getUnitSymbol() : "NA") +
                ", voltageL3=" + (voltageL3 != null ? voltageL3.getPhysicalValueOrNull() + " " + voltageL3.getUnitSymbol() : "NA") +
                ')';
    }
}
