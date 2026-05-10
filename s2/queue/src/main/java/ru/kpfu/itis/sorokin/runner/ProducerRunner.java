package ru.kpfu.itis.sorokin.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.kpfu.itis.sorokin.service.FlightTaskProducerService;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Component
public class ProducerRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProducerRunner.class);

    private final FlightTaskProducerService producerService;
    private final String appMode;
    private final int ratePerSecond;

    public ProducerRunner(
            FlightTaskProducerService producerService,
            @Value("${app.mode}") String appMode,
            @Value("${producer.rate-per-second}") int ratePerSecond
    ) {
        this.producerService = producerService;
        this.appMode = appMode;
        this.ratePerSecond = ratePerSecond;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!"producer".equalsIgnoreCase(appMode)) {
            return;
        }

        int safeRate = Math.max(ratePerSecond, 1);
        long delayNanos = TimeUnit.SECONDS.toNanos(1) / safeRate;

        log.info("Producer started. ratePerSecond={}", safeRate);

        long created = 0;

        while (true) {
            producerService.produceOne();
            created++;

            if (created % safeRate == 0) {
                log.info("Produced tasks: {}", created);
            }

            LockSupport.parkNanos(delayNanos);
        }
    }
}
