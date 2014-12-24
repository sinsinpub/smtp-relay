package com.github.sinsinpub.smtp.relay.core;

import javax.mail.Session;

import com.github.sinsinpub.smtp.relay.context.MailContext;

/**
 * The executable worker command of mail forwarding operation.
 * 
 * @author sin_sin
 */
public interface MailForwardCommand extends Runnable {

    void setMail(MailContext mail);

    void setMailSession(Session session);

}
