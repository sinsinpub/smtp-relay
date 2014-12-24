package com.github.sinsinpub.smtp.relay.transport;

import java.util.concurrent.TimeUnit;

import com.github.sinsinpub.smtp.relay.exception.RetryException;

/**
 * Abstract retry strategy controller.
 */
public abstract class RetryStrategy {

    public static final int DEFAULT_NUMBER_OF_RETRIES = 3;
    private int numberOfRetries;
    private int numberOfTriesLeft;

    public RetryStrategy() {
        this(DEFAULT_NUMBER_OF_RETRIES);
    }

    public RetryStrategy(int numberOfRetries) {
        this.numberOfRetries = numberOfRetries;
        this.numberOfTriesLeft = numberOfRetries;
    }

    public int getNumberOfRetries() {
        return numberOfRetries;
    }

    public boolean shouldRetry() {
        return 0 < numberOfTriesLeft;
    }

    public int retriesLeft() {
        return numberOfTriesLeft;
    }

    public String printRetriesCount() {
        return String.format("%s/%s", retriesLeft(), getNumberOfRetries());
    }

    public void tryRetry() throws RetryException {
        numberOfTriesLeft--;
        if (!shouldRetry()) {
            throw new RetryException(String.format("No more retries left (%s)",
                    printRetriesCount()));
        }
        waitUntilNextTry();
    }

    private void waitUntilNextTry() {
        long timeToWait = getTimeToWait();
        try {
            TimeUnit.MILLISECONDS.sleep(timeToWait);
        } catch (InterruptedException ignored) {
        }
    }

    protected abstract long getTimeToWait();

}
