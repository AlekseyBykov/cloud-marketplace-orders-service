package dev.abykov.cloudmarketplace.orders.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for external integrations used by Orders Service.
 *
 * <p>Currently contains settings for communication with Menu Service.</p>
 *
 * <p>All values are bound from application.yml using the "external" prefix.</p>
 *
 * <p>This configuration defines:
 * <ul>
 *     <li>Base URL and endpoint path for Menu Service</li>
 *     <li>Timeout for outbound HTTP calls</li>
 *     <li>Retry policy (count, backoff, jitter)</li>
 * </ul>
 *
 * <p>These parameters directly affect resilience and latency of the system.</p>
 */
@Data
@ConfigurationProperties(prefix = "external")
public class OrderServiceProps {

    /**
     * Base URL of the Menu Service.
     * <p>
     * Example: http://localhost:9091
     */
    private final String menuServiceUrl;

    /**
     * Endpoint path for fetching menu item info (price + availability).
     * <p>
     * Combined with base URL to form full request URI.
     */
    private final String menuInfoPath;

    /**
     * Maximum time to wait for a response from Menu Service per request attempt.
     * <p>
     * If exceeded, a {@link java.util.concurrent.TimeoutException} is thrown.
     */
    private final Duration defaultTimeout;

    /**
     * Initial delay between retry attempts.
     * <p>
     * Used as a base value for exponential backoff strategy.
     */
    private final Duration retryBackoff;

    /**
     * Maximum number of retry attempts for transient failures.
     * <p>
     * Applies only to retryable errors (5xx responses, timeouts).
     */
    private final int retryCount;

    /**
     * Randomization factor applied to retry delays (jitter).
     * <p>
     * Helps to avoid synchronized retries across multiple service instances
     * (prevents thundering herd problem).
     */
    private final double retryJitter;
}
