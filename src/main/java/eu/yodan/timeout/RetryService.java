package eu.yodan.timeout;

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

    private static final Duration MAX_TIMEOUT = Duration.ofSeconds(60);

    public <T> Flux<T> retryWithBackoff(
        final Supplier<T> thingToRetry,
        Duration timeout,
        int numberOfAttempts) {

        return durationsWithBackoffAppliedToFlux(timeout, numberOfAttempts)
            .map((i) -> callableToResultOrEmptyOptionalOnError(thingToRetry))
            .filter(Optional::isPresent)
            .map(Optional::get);
    }

    private Flux<Duration> durationsWithBackoffAppliedToFlux(Duration timeout, int numberOfAttempts) {
        final var allDurationsSpacedInTime = this.exponentialBackoffDurations(timeout, numberOfAttempts)
            .concatMap(RetryService::singleItemIntervalFluxOfDuration);

        final var allAttempts = Flux.concat(
            allDurationsSpacedInTime.take(numberOfAttempts),
            Mono.error(RetryLimitException::new)
        );

        return Flux.merge(allAttempts, maxTimeout())
            .checkpoint("exponential backoff durations with intervals")
            .onErrorStop();
    }

    private Flux<Duration> exponentialBackoffDurations(final Duration timeout, int numberOfAttempts) {
        final var initialDuration = Mono.just(Duration.ZERO);
        final var retryDurations = Flux.range(0, numberOfAttempts + 10)
            .map(i -> {
                final long multiplier = (long) Math.pow(2, i);
                return timeout.multipliedBy(multiplier);
            });
        return Flux.concat(
            initialDuration,
            retryDurations)
            .checkpoint("exponential backoff durations");
    }

    private Flux<Duration> maxTimeout() {
        return Flux.interval(MAX_TIMEOUT)
            .map(i -> {
                throw new TimeoutException();
            });
    }

    private class RetryLimitException extends RuntimeException {
        RetryLimitException() {
            super("retry limit reached");
        }
    }

    private class TimeoutException extends RuntimeException {
        TimeoutException() {
            super("timeout reached");
        }
    }

    private static Flux<Duration> singleItemIntervalFluxOfDuration(Duration duration) {
        return Flux.interval(duration)
            .take(1)
            .map(i -> duration);
    }

    private static <T> Optional<T> callableToResultOrEmptyOptionalOnError(Supplier<T> callable) {
        try {
            return Optional.of(callable.get());
        } catch (Throwable t) {
            return Optional.empty();
        }
    }
}
