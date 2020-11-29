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
        if (count < 5) {
            count++;
            return null;
        }
        count = 0;
        return "success";
    }

    @Test
    public void testWithRealTime_exponentialBackoffRetry() throws Exception {
        final var flux = retryService.retry(this::test, DURATION, 4);
        flux.subscribe(System.out::println);

        Thread.sleep(90_000L);
    }
}
