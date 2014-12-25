package com.github.sinsinpub.smtp.relay.core;

import javax.mail.Session;

import com.github.sinsinpub.smtp.relay.context.MailContext;

/**
 * The executable worker command of mail forwarding operation.
 * 
 * @author sin_sin
 */
public interface MailForwardCommand extends Runnable {

    /**
     * @param mail The mail message context to be forwarded
     */
    void setMail(MailContext mail);

    /**
     * @param session The session of back-end mail server used to forward message
     */
    void setMailSession(Session session);

}
