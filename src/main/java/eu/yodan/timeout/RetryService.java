package eu.yodan.timeout;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
public class RetryService {

    private final static int MAX_ATTEMPTS = 5;
    private static final Duration MAX_TIMEOUT = Duration.ofSeconds(4);

    private ImmutableSet<Long> retryTimes = ImmutableSet.of(0L,1L,2L,4L,8L,16L,32L);

    public <T> Flux<T> secondRetryThing(
        final Supplier<T> thingToRetry,
        Duration timeout,
        int numberOfAttempts) {

        final Flux<T> successfulAttempts = exponentialBackoffDurationsWithIntervals(timeout, numberOfAttempts)
            .map((i) -> callCallableAndReturnOptional(thingToRetry))
            .filter(Optional::isPresent)
            .map(Optional::get);

        final Flux<T> maxTimeout = Flux.interval(MAX_TIMEOUT)
            .map(i -> { throw new TimeoutException(); });

        return Flux.merge(successfulAttempts, maxTimeout)
            .take(1)
            .onErrorStop();
    }

    private <T> Optional<T> callCallableAndReturnOptional(Supplier<T> callable) {
        try {
            final T result = callable.get();
            return Optional.<T>ofNullable(result);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    private Flux<Duration> exponentialBackoffDurationsWithIntervals(Duration timeout, int numberOfAttempts) {
        return this.exponentialBackoffDurations(timeout, numberOfAttempts)
            .concatMap(duration -> Flux.interval(duration).take(1).map(i -> duration))
            .take(numberOfAttempts);
    }

    private Flux<Duration> exponentialBackoffDurations(final Duration instanceTimeout, int numberOfAttempts) {
        final var retries = Flux.range(0, numberOfAttempts)
            .map(i -> {
                final long multiplier = (long) Math.pow(2, i);
                return instanceTimeout.multipliedBy(multiplier);
            });
        return Flux.concat(
            Mono.just(Duration.ZERO),
            retries)
            .take(numberOfAttempts)
            .checkpoint("exponential backoff durations");
    }

    private class TimeoutException extends RuntimeException {

        TimeoutException() {
            super("timeout reached");
        }
    }
}
