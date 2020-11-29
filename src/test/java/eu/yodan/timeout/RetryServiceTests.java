package eu.yodan.timeout;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@RunWith(SpringRunner.class)
@ContextConfiguration(classes={RetryService.class})
public class RetryServiceTests {

    final Duration DURATION = Duration.ofSeconds(1);
    final int NUMBER_OF_ATTEMPTS = 10;

    @Autowired
    RetryService retryService;

    private int count = 0;

    public String test()  {
        log.debug("attempt made");
        if (count < 2) {
            count++;
            return null;
        }
        count = 0;
        return "success";
    }

    @Test
    public void test_exponentialBackoff() throws Exception {
        final var flux = retryService.secondRetryThing(this::test, DURATION, NUMBER_OF_ATTEMPTS);
        flux.subscribe(System.out::println);

        Thread.sleep(10_000L);
    }

    @Test
    public void test_concatMonoWithInterval() throws Exception {
        final var start = Mono.just(-100);
        final var rest = Flux.interval(Duration.ofMillis(100));

        final var all  = Flux.concat(start, rest);

        all.subscribe(System.out::println);

        Thread.sleep(10_000L);
    }
}
