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

    public <T> Flux<T> retryThing(final Supplier<T> thingToRetry, final int times) {
        final var firstAttempt = Mono.just(0);
        final var restOfAttempts = Flux.interval(Duration.ofSeconds(1))
            .map(i -> i + 1);
        final var allAttempts = Flux.concat(firstAttempt, restOfAttempts)
            .filter(i -> {
                System.out.println(i);
                return this.retryTimes.contains(i);
            })
            .doOnNext(i -> System.out.println("allAttempts attempt number: " + i))
            .take(times)
            .doOnNext(i -> System.out.println("allAttempts attempt number after take: " + i));

        final var madeAttempts = allAttempts
            .map(attempt -> Optional.ofNullable(thingToRetry.get()));
        final var successfulAttempts = madeAttempts
            .filter(Optional::isPresent)
            .map(Optional::get);

        final var timedOut = Flux.interval(MAX_TIMEOUT)
            .flatMap(i -> Mono.<T>error(TimeoutException::new));
        return Flux.merge(successfulAttempts, timedOut)
            .take(1)
            .onErrorStop();
    }

    private class TimeoutException extends RuntimeException {

        TimeoutException() {
            super("timeout reached");
        }
    }
}
