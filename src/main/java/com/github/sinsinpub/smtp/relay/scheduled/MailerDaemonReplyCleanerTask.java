package com.github.sinsinpub.smtp.relay.scheduled;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.mail.BodyPart;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.HeaderTerm;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Constants;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * A simple scheduled task for cleaning auto-replied status report mails from
 * postmaster by facilities of Mail Store (POP3/IMAP protocol).
 * 
 * @author sin_sin
 */
@ThreadSafe
// Notice: if more than one task instance exists in bean factory, only one of them
// will be registered as the MBean. To work around with this, another object name
// strategy needed.
@ManagedResource(objectName = MailerDaemonReplyCleanerTask.OBJECT_NAME, description = "A simple scheduled task for cleaning reply from mailer system.")
public class MailerDaemonReplyCleanerTask implements Runnable {

    public static final String OBJECT_NAME = "com.github.sinsinpub.smtp.relay:type=Scheduler,name=ReplyCleanerTask";
    /** Recognized as status report by kinds of conditions */
    public static final int MATCH_MODE_STRICT = 1;
    /** Recognized as status report just by from address contains MAILER-DAEMON */
    public static final int MATCH_MODE_LOOSE = 2;

    private final static Logger logger = LoggerFactory.getLogger(MailerDaemonReplyCleanerTask.class);

    protected static final String DEFAULT_MAILER_DAEMON_FROM_ADDRESS = "MAILER-DAEMON";
    protected static final String DEFAULT_INBOX_FOLDER_NAME = "INBOX";
    protected static final String DEFAULT_STATUS_REPORT_CONTENT_TYPE_HEADER = "delivery-status;";
    protected static final String DEFAULT_AUTO_SUBMITTED_HEADER = "auto-replied";

    private String sessionName;
    @GuardedBy("itself")
    private Session session;
    private int matchMode = MATCH_MODE_STRICT;
    private boolean loggingDetails = true;

    private AtomicLong numMailDeleted = new AtomicLong();
    private AtomicLong numMailExpunged = new AtomicLong();

    public MailerDaemonReplyCleanerTask() {
        super();
    }

    @Override
    public void run() {
        try {
            doClean();
        } catch (Throwable e) {
            // Do not throw any exception as default ScheduledExecutorService will
            // remove the Runnable who throws an exception.
            logger.error(e.toString(), e);
        }
    }

    /**
     * Connect to the Mail Store and walk through default in-box, expunge those
     * reply messages which made by mailer daemon itself for reporting deliver
     * status.
     */
    private void doClean() {
        Store store = null;
        Folder inbox = null;
        try {
            store = session.getStore();
            store.connect();
            // Only INBOX folder support for this POP3 provider
            // No auto archiving could apply :(
            inbox = store.getDefaultFolder().getFolder(
                    DEFAULT_INBOX_FOLDER_NAME);
            inbox.open(Folder.READ_WRITE);
            HeaderTerm term = new HeaderTerm("Return-Path",
                    DEFAULT_MAILER_DAEMON_FROM_ADDRESS);
            logger.info(
                    "Searching status report messages in folder {} of <{}>...",
                    inbox.getFullName(), getSessionName());
            // Searching may make a long-time data exchange with server than:
            // Message[] messages = inbox.getMessages();
            Message[] messages = inbox.search(term);
            logger.info("Status report messages in total: {}/{}",
                    messages.length, inbox.getMessageCount());
            if (messages.length > 0) {
                int idx = 0;
                for (Message message : messages) {
                    idx += 1;
                    MimeMessage mime = (MimeMessage) message;
                    logger.debug("Checking mail #{}: {}", idx,
                            mime.getMessageID());

                    boolean matched = true;
                    String from = null;
                    String[] returnPath = mime.getHeader("Return-Path");
                    if (returnPath.length >= 1) {
                        from = returnPath[0];
                    } else if (mime.getFrom().length >= 1) {
                        from = mime.getFrom()[0].toString();
                    }
                    if (matched && from != null) {
                        matched &= from.contains(DEFAULT_MAILER_DAEMON_FROM_ADDRESS);
                    } else {
                        // Keep default status as NOT MATCH if no address resolved
                        matched = false;
                    }

                    // Try more conditions if necessary
                    if (getMatchMode() == MATCH_MODE_STRICT) {
                        if (matched) {
                            matched &= mime.getContentType().contains(
                                    DEFAULT_STATUS_REPORT_CONTENT_TYPE_HEADER);
                        }

                        String[] autoFlag = mime.getHeader("Auto-Submitted");
                        if (matched && autoFlag != null && autoFlag.length >= 1) {
                            matched &= DEFAULT_AUTO_SUBMITTED_HEADER.equals(autoFlag[0]);
                        }
                    }

                    if (matched) {
                        mime.setFlag(Flag.DELETED, true);
                        numMailDeleted.getAndIncrement();
                        logger.debug(
                                "Status report mail #{}:{} has been marked as DELETED",
                                idx, mime.getMessageID());
                        if (isLoggingDetails()) {
                            String reason = retrieveReasonMessageLine(mime);
                            if (reason != null) {
                                logger.info(
                                        "Mail {} reported status: {}",
                                        String.format("#%s:%s", idx,
                                                mime.getMessageID()), reason);
                            } else {
                                logger.info(
                                        "Mail #{}:{} has been deleted, but content retrieving failed.",
                                        idx, mime.getMessageID());
                            }
                        }
                    }
                }
                logger.info("Status report messages deleted: {}/{}",
                        inbox.getDeletedMessageCount(), messages.length);
            }
        } catch (MessagingException e) {
            logger.error("Exception on communicating with mailer daemon: " + e,
                    e);
        } finally {
            if (inbox != null) {
                try {
                    int expunged = inbox.getDeletedMessageCount();
                    // To expunges all messages marked as DELETED
                    inbox.close(true);
                    numMailExpunged.getAndAdd(expunged);
                } catch (Exception e) {
                    logger.error(e.toString(), e);
                }
            }
            if (store != null) {
                try {
                    store.close();
                } catch (Exception e) {
                    // Ignore this
                }
            }
        }
    }

    /**
     * Try to lookup the reason message which made by mailer system from the first
     * part of status report contents.
     * 
     * @param message The message of status report
     * @return One line report of the output by mailer daemon, null if no
     *         reasonable found or exception occurs.
     */
    private String retrieveReasonMessageLine(MimeMessage message) {
        String messageId = null;
        try {
            messageId = message.getMessageID();
            if (message.getContent() instanceof MimeMultipart) {
                MimeMultipart multipart = (MimeMultipart) message.getContent();
                if (multipart.getCount() >= 1) {
                    BodyPart firstBodyPart = multipart.getBodyPart(0);
                    String body = String.valueOf(firstBodyPart.getContent());
                    // Here go some stupid methods:
                    // Looking for "Command output"
                    BufferedReader br = new BufferedReader(new StringReader(
                            body));
                    StringWriter sw = new StringWriter();
                    boolean found = false;
                    String line = br.readLine();
                    while (line != null) {
                        if (!found && line.contains("Command output")) {
                            found = true;
                        }
                        if (found && StringUtils.isNotBlank(line)) {
                            if (StringUtils.isNotEmpty(sw.toString())) {
                                sw.append(" ");
                            }
                            sw.append(StringUtils.strip(line));
                        }
                        line = br.readLine();
                    }
                    br.close();
                    // Try "following"
                    if (!found) {
                        br = new BufferedReader(new StringReader(body));
                        line = br.readLine();
                        while (line != null) {
                            if (found && StringUtils.isNotBlank(line)) {
                                if (StringUtils.isNotEmpty(sw.toString())) {
                                    sw.append(" ");
                                }
                                sw.append(StringUtils.strip(line));
                            }
                            if (!found && line.contains("following")) {
                                found = true;
                            }
                            line = br.readLine();
                        }
                        br.close();
                    }
                    sw.close();
                    return found ? sw.toString() : null;
                }
            }
        } catch (Exception e) {
            logger.warn("Retrieving content exception: {} on message {}",
                    e.toString(), messageId);
        }
        return null;
    }

    @ManagedAttribute
    public String getSessionName() {
        return String.valueOf(sessionName);
    }

    @ManagedAttribute
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    @ManagedAttribute
    public int getMatchMode() {
        return matchMode;
    }

    @ManagedAttribute
    public void setMatchMode(int matchMode) {
        this.matchMode = matchMode;
    }

    @ManagedOperation
    public void setMatchModeName(String matchModeName) {
        setMatchMode(new Constants(MailerDaemonReplyCleanerTask.class).asNumber(
                matchModeName)
                .intValue());
    }

    @ManagedAttribute
    public boolean isLoggingDetails() {
        return loggingDetails;
    }

    @ManagedAttribute
    public void setLoggingDetails(boolean loggingDetails) {
        this.loggingDetails = loggingDetails;
    }

    @ManagedAttribute
    public Number getNumMailDeleted() {
        return numMailDeleted.get();
    }

    @ManagedAttribute
    public Number getNumMailExpunged() {
        return numMailExpunged.get();
    }

}
