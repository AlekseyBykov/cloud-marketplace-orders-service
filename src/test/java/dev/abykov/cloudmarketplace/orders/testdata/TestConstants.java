package dev.abykov.cloudmarketplace.orders.testdata;

import java.time.Duration;

public class TestConstants {

    public static final int DELAY_MILLIS = 1500;
    public static final int RETRY_COUNT = 3;
    public static final double RETRY_JITTER = 0.75;
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(1);
    public static final Duration RETRY_BACKOFF = Duration.ofMillis(10);
}
