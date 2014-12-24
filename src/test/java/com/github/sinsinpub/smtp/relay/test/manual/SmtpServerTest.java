package com.github.sinsinpub.smtp.relay.test.manual;

import java.util.Properties;

import com.github.sinsinpub.smtp.relay.context.MailSessionFactory;
import com.github.sinsinpub.smtp.relay.core.impl.FromAddressRelayForwarderFactory;
import com.github.sinsinpub.smtp.relay.core.impl.SmtpListener;

public class SmtpServerTest {

    public static void main(String[] args) throws Exception {

        int port = 2025;
        int threads = 1;

        Properties props = System.getProperties();
        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.port", "25");
        props.setProperty("mail.smtp.auth", "false");
        MailSessionFactory sessionFactory = new MailSessionFactory();
        sessionFactory.setProperties(props);
        sessionFactory.afterPropertiesSet();

        FromAddressRelayForwarderFactory forwarderFactory = new FromAddressRelayForwarderFactory();
        forwarderFactory.setFromAddressDeliverRulesByString("*:defaultSmtpPlaintext");
        forwarderFactory.setDefaultSession(sessionFactory.getObject());
        forwarderFactory.afterPropertiesSet();

        SmtpListener listener = new SmtpListener();
        listener.setListenPort(port);
        listener.setForwarderCnt(threads);
        listener.setForwarderFactory(forwarderFactory);
        listener.initialize();
        listener.start();
    }

}
