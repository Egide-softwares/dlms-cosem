package com.pivotaccess.dlmscosem.polling;

import com.pivotaccess.dlmscosem.cosem.CosemClient;
import com.pivotaccess.dlmscosem.cosem.CosemClientConfig;
import com.pivotaccess.dlmscosem.cosem.ObisCode;
import com.pivotaccess.dlmscosem.cosem.objects.ClockObject;
import com.pivotaccess.dlmscosem.cosem.objects.CosemDataObject;
import com.pivotaccess.dlmscosem.cosem.objects.RegisterObject;
import com.pivotaccess.dlmscosem.polling.data.MeterReading;
import com.pivotaccess.dlmscosem.transport.TransportConfig;

import java.util.concurrent.Callable;

public class MeterReadTask implements Callable<MeterReading> {
    private final String meterId;
    private final CosemClientConfig config;
    private final String transportId;

    /**
     * @param transportId A unique identifier for the port (e.g., "/dev/ttyUSB0" or "TCP_IP")
     */
    public MeterReadTask(String meterId, String transportId, CosemClientConfig config) {
        this.meterId = meterId;
        this.config = config;
        this.transportId = transportId;
    }

    public String getMeterId() { return meterId; }
    public String getTransportId() { return transportId; }

    /**
     * Determines if this task can run in parallel with others. For example, tasks using different TCP connections or serial ports can run concurrently.
     * Tasks that share the same connection (e.g., same serial port) should return false to ensure sequential execution.
     */
    public boolean isParallel() {
        return config.getTransportConfig().getType() == TransportConfig.Type.TCP;
    }

    @Override
    public MeterReading call() {
        MeterReading reading = new MeterReading(meterId, transportId);
        try (CosemClient client = new CosemClient(config)) {
            client.open();

            try {
                CosemDataObject serialNumber = client.getData(ObisCode.SERIAL_NUMBER);
                reading.setSerialNumber(serialNumber);
            } catch (Exception ignored) {
                reading.setSerialNumber(null);
            }

            //try {
            //    CosemDataObject logicalDeviceName = client.getData(ObisCode.of("0.0.42.0.0.255"));
            //    reading.setLogicalDeviceName(logicalDeviceName);
            //} catch (Exception ignored) {
            //    reading.setLogicalDeviceName(null);
            //}

            try {
                CosemDataObject invocationCounter = client.getData(ObisCode.of("0.0.43.1.2.255"));
                reading.setInvocationCounter(invocationCounter);
            } catch (Exception ignored) {
                reading.setInvocationCounter(null);
            }

            try {
                ClockObject clock = client.getClock();
                reading.setClock(clock);
            } catch (Exception ignored) {
                reading.setClock(null);
            }

            try {
                RegisterObject activeEnergyImport = client.getRegister(ObisCode.ACTIVE_ENERGY_IMPORT);
                reading.setActiveEnergyImport(activeEnergyImport);
            } catch (Exception ignored) {
                reading.setActiveEnergyImport(null);
            }

            try {
                RegisterObject activeEnergyExport = client.getRegister(ObisCode.ACTIVE_ENERGY_EXPORT);
                reading.setActiveEnergyExport(activeEnergyExport);
            } catch (Exception ignored) {
                reading.setActiveEnergyExport(null);
            }

            try {
                RegisterObject voltageL1 = client.getRegister(ObisCode.VOLTAGE_L1);
                reading.setVoltageL1(voltageL1);
            } catch (Exception ignored) {
                reading.setVoltageL1(null);
            }

            try {
                RegisterObject voltageL2 = client.getRegister(ObisCode.VOLTAGE_L2);
                reading.setVoltageL2(voltageL2);
            } catch (Exception ignored) {
                reading.setVoltageL2(null);
            }

            try {
                RegisterObject voltageL3 = client.getRegister(ObisCode.VOLTAGE_L3);
                reading.setVoltageL3(voltageL3);
            } catch (Exception ignored) {
                reading.setVoltageL3(null);
            }

            reading.setSuccessful(true);
        } catch (Exception e) {
            reading.setErrorMsg(e.getMessage());
            reading.setSuccessful(false);
        }
        return reading;
    }
}