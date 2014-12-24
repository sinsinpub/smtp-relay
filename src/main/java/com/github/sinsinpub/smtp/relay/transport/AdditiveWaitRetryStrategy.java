package com.github.sinsinpub.smtp.relay.transport;

/**
 * Do each retry after additive wait time.
 */
public class AdditiveWaitRetryStrategy extends RetryStrategy {

    public static final long STARTING_WAIT_TIME = 3000;
    public static final long WAIT_TIME_INCREMENT = 5000;

    private long currentTimeToWait;
    private long waitTimeIncrement;

    public AdditiveWaitRetryStrategy() {
        this(DEFAULT_NUMBER_OF_RETRIES, STARTING_WAIT_TIME, WAIT_TIME_INCREMENT);
    }

    public AdditiveWaitRetryStrategy(int numberOfRetries,
            long startingWaitTime, long waitTimeIncrement) {
        super(numberOfRetries);
        currentTimeToWait = startingWaitTime;
        this.waitTimeIncrement = waitTimeIncrement;
    }

    protected long getTimeToWait() {
        long returnValue = currentTimeToWait;
        currentTimeToWait += waitTimeIncrement;
        return returnValue;
    }

}
