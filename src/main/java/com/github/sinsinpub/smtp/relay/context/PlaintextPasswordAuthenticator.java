package com.github.sinsinpub.smtp.relay.context;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * Simple password authenticator in plain-text. Protect your secrets by yourself.
 * 
 * @author sin_sin
 */
public class PlaintextPasswordAuthenticator extends Authenticator {

    private String username;
    private String password;

    public PlaintextPasswordAuthenticator() {
        super();
    }

    public PlaintextPasswordAuthenticator(String username, String password) {
        this();
        setUsername(username);
        setPassword(password);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
    }

}
