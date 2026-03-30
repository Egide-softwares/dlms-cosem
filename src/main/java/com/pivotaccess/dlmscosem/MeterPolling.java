package com.pivotaccess.dlmscosem;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.pivotaccess.dlmscosem.apdu.AarqApdu;
import com.pivotaccess.dlmscosem.cosem.CosemClientConfig;
import com.pivotaccess.dlmscosem.polling.MeterPollingEngine;
import com.pivotaccess.dlmscosem.polling.MeterReadTask;
import com.pivotaccess.dlmscosem.polling.MeterScheduler;
import com.pivotaccess.dlmscosem.transport.TransportConfig;
import com.pivotaccess.dlmscosem.util.MeterAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class MeterPolling {
    private static final Logger log = LoggerFactory.getLogger(MeterPolling.class);

    private record MeterConfig(
            String meterId,
            String transportId,
            long interval,
            TimeUnit intervalUnit,
            CosemClientConfig clientConfig
    ) {}

    public static void main(String[] args) {
        // Set logging level to INFO
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.toLevel("INFO", Level.DEBUG));

        // Initialize the polling engine
        MeterPollingEngine engine = new MeterPollingEngine();

        // Define the callback for when a reading arrives
        MeterScheduler scheduler = new MeterScheduler(engine, reading -> {
            if (reading.isSuccessful()) {
                log.info("[OK] {}", reading);
                // Ready to be serialized and sent to api or database, etc
            } else {
                log.error("[FAIL] {}", reading);
            }
        });

        // A list of read meter tasks with intervals and transport details
        List<MeterConfig> configurations = List.of(
                new MeterConfig(
                        "METER_01", "TCP_HDLC_127.0.0.1_4060", 20, TimeUnit.SECONDS,
                        CosemClientConfig.builder()
                                .transport(TransportConfig.tcp("localhost", 4060, TransportConfig.FramingMode.HDLC))
                                .serverAddress(MeterAddress.hdlcServer(1))
                                .clientAddress(MeterAddress.hdlcClient(16))
                                .authentication(AarqApdu.AuthenticationLevel.NONE, null)
                                .maxReceivePduSize(1024)
                                .timeoutMs(5_000)
                                .maxRetries(2)
                                .build()
                )
                ,new MeterConfig(
                        "METER_02", "TCP_HDLC_127.0.0.1_4061", 35, TimeUnit.SECONDS,
                        CosemClientConfig.builder()
                                .transport(TransportConfig.tcp("localhost", 4061, TransportConfig.FramingMode.HDLC))
                                .serverAddress(MeterAddress.hdlcServer(1))
                                .clientAddress(MeterAddress.hdlcClient(16))
                                .authentication(AarqApdu.AuthenticationLevel.NONE, null)
                                .maxReceivePduSize(1024)
                                .timeoutMs(5_000)
                                .maxRetries(2)
                                .build()
                )
                ,new MeterConfig(
                        "METER_03", "TCP_WRAPPER_127.0.0.1_4070", 25, TimeUnit.SECONDS,
                        CosemClientConfig.builder()
                                .transport(TransportConfig.tcp("localhost", 4070, TransportConfig.FramingMode.WRAPPER))
                                .serverAddress(MeterAddress.wrapperServer(1))
                                .clientAddress(MeterAddress.wrapperClient(16))
                                .authentication(AarqApdu.AuthenticationLevel.NONE, null)
                                .maxReceivePduSize(1024)
                                .timeoutMs(2_000)
                                .build()
                )
                ,new MeterConfig(
                        "METER_04", "TCP_WRAPPER_127.0.0.1_4071", 30, TimeUnit.SECONDS,
                        CosemClientConfig.builder()
                                .transport(TransportConfig.tcp("localhost", 4071, TransportConfig.FramingMode.WRAPPER))
                                .serverAddress(MeterAddress.wrapperServer(1))
                                .clientAddress(MeterAddress.wrapperClient(16))
                                .authentication(AarqApdu.AuthenticationLevel.NONE, null)
                                .maxReceivePduSize(1024)
                                .timeoutMs(2_000)
                                .build()
                )
        );

        // Schedule all meters for polling
        for  (MeterConfig configuration : configurations) {
            MeterReadTask task = new MeterReadTask(
                    configuration.meterId,
                    configuration.transportId,
                    configuration.clientConfig
            );
            scheduler.schedule(task, configuration.interval, configuration.intervalUnit);
        }
    }
}
