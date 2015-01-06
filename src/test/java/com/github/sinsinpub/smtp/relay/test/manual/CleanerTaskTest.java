package com.github.sinsinpub.smtp.relay.test.manual;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.sinsinpub.smtp.relay.context.MailSessionFactory;
import com.github.sinsinpub.smtp.relay.scheduled.MailerDaemonReplyCleanerTask;

public class CleanerTaskTest {

    private final static String userAddr = "user@domain.net";
    private static MailSessionFactory factory;

    @BeforeClass
    public static void setUp() throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "pop3");
        props.put("mail.pop3.host", "pop3.domain.net");
        props.put("mail.pop3.port", "110");
        factory = new MailSessionFactory();
        factory.setProperties(props);
        factory.afterPropertiesSet();
    }

    @Test
    public void testTaskRunning() throws Exception {
        MailerDaemonReplyCleanerTask task = new MailerDaemonReplyCleanerTask();
        task.setSession(factory.getObject());
        task.setSessionName(userAddr);
        task.run();
    }

}
