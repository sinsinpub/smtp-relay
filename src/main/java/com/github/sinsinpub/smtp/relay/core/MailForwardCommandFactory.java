package com.github.sinsinpub.smtp.relay.core;

import com.github.sinsinpub.smtp.relay.context.MailContext;

/**
 * The controller of building mail forwarders by kinds of contexts.
 * 
 * @author sin_sin
 */
public interface MailForwardCommandFactory {

    /**
     * Create a new mail forwarder command worker by mail context and configured
     * environment.
     * 
     * @param mailContext the mail message context to be forwarded
     * @return new instance of {@code MailForwardCommand}
     */
    MailForwardCommand newForwardCommand(MailContext mailContext);

}
