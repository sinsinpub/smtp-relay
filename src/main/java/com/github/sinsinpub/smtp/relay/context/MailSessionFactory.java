package com.github.sinsinpub.smtp.relay.context;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Session;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * The factory class for Java Mail Session, making products by mail properties and
 * authenticator.
 * 
 * @author sin_sin
 */
public class MailSessionFactory extends AbstractFactoryBean<Session> {

    private Properties properties;
    private Authenticator authenticator;

    /**
     * Default empty constructor.
     */
    public MailSessionFactory() {
        super();
    }

    @Override
    protected Session createInstance() throws Exception {
        return Session.getInstance(getProperties(), getAuthenticator());
    }

    @Override
    public Class<?> getObjectType() {
        return Session.class;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

}
