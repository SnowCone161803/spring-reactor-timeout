package eu.yodan.timeout;

import org.junit.Test;
import reactor.core.publisher.Flux;

public class PlayingWithReactorFluxTests {

    @Test
    public void test_generator() throws Exception {
        final var flux = Flux.generate(() -> 0,
            (state, sink) -> {
                final var newState = state + 1;
                sink.next(newState);
                return newState;
        });

        flux.take(10).subscribe(System.out::println);
    }
}
