package com.github.sinsinpub.smtp.relay.exception;

/**
 * Thrown when no retry times left.
 * 
 * @author sin_sin
 */
public class RetryException extends Exception {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public RetryException(String message) {
        super(message);
    }

}
