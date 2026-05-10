package ru.kpfu.itis.sorokin.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kpfu.itis.sorokin.repository.FlightEventRepository;
import ru.kpfu.itis.sorokin.repository.FlightRepository;
import ru.kpfu.itis.sorokin.repository.TaskRepository;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class FlightTaskProducerService {

    private final FlightRepository flightRepository;
    private final FlightEventRepository flightEventRepository;
    private final TaskRepository taskRepository;

    public FlightTaskProducerService(
            FlightRepository flightRepository,
            FlightEventRepository flightEventRepository,
            TaskRepository taskRepository
    ) {
        this.flightRepository = flightRepository;
        this.flightEventRepository = flightEventRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public long produceOne() {
        long flightId = flightRepository.findAnyFlightId();

        int priority = generatePriority();

        String eventType = priority == 100
                ? "FLIGHT_DELAYED_CRITICAL"
                : "FLIGHT_STATUS_UPDATED";

        long flightEventId = flightEventRepository.create(flightId, eventType);

        return taskRepository.createFlightNotificationTask(
                flightId,
                flightEventId,
                eventType,
                priority
        );
    }

    private int generatePriority() {
        return ThreadLocalRandom.current().nextInt(100) < 20 ? 100 : 0;
    }
}
