package com.github.sinsinpub.smtp.relay.core.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import com.github.sinsinpub.smtp.relay.context.MailContext;
import com.github.sinsinpub.smtp.relay.core.MailForwardCommand;
import com.github.sinsinpub.smtp.relay.utils.ObjectUtil;

/**
 * A simple SMTP protocol listener and delivering component.
 * 
 * @author sin_sin
 */
@ThreadSafe
@ManagedResource(objectName = SmtpListener.OBJECT_NAME, description = "A simple SMTP protocol listener and delivering component.")
public class SmtpListener implements SimpleMessageListener, Lifecycle,
        InitializingBean, DisposableBean {

    public static final String OBJECT_NAME = "com.github.sinsinpub.smtp.relay:type=Frontend,name=SmtpListener";
    private final static Logger logger = LoggerFactory.getLogger(SmtpListener.class);

    @GuardedBy("itself")
    protected SMTPServer frontendServer;
    @GuardedBy("itself")
    protected ExecutorService executorService;
    protected InetAddress bindAddress;
    protected int listenPort;
    protected int forwarderCnt;

    protected FromAddressRelayForwarderFactory forwarderFactory;
    protected String myDomain;
    protected String[] allowedTo;
    protected String[] allowedFrom;

    protected final AtomicLong numRejected = new AtomicLong();
    protected final AtomicLong numAccepted = new AtomicLong();
    protected final AtomicLong numForwarded = new AtomicLong();
    protected final AtomicLong numFailed = new AtomicLong();

    private boolean initialized = false;

    /**
     * Default empty constructor.
     */
    public SmtpListener() {
        super();
    }

    public SmtpListener(InetAddress bindAddress, int port, int forwarders,
            FromAddressRelayForwarderFactory forwardFactory, String myDomain,
            String[] allowedFrom, String[] allowedTo, File errDump) {
        this();
        this.bindAddress = bindAddress;
        this.listenPort = port;
        this.forwarderCnt = forwarders;
        this.forwarderFactory = forwardFactory;
        this.myDomain = myDomain;
        this.allowedFrom = allowedFrom;
        this.allowedTo = allowedTo;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initialize();
        if (!isRunning()) {
            start();
        }
    }

    @Override
    public void destroy() throws Exception {
        if (isRunning()) {
            stop();
        }
        finialize();
    }

    public synchronized void initialize() {
        if (!isInitialized()) {
            return;
        }
        this.frontendServer = new SMTPServer(new SimpleMessageListenerAdapter(
                this));
        if (this.bindAddress != null) {
            this.frontendServer.setBindAddress(this.bindAddress);
        }
        this.frontendServer.setPort(this.listenPort);
        int threadCnt = this.forwarderCnt;
        if (threadCnt < 1) {
            threadCnt = Runtime.getRuntime().availableProcessors();
        }
        this.executorService = Executors.newFixedThreadPool(threadCnt);
        this.initialized = true;
    }

    public synchronized void start() {
        isMustInitialized();
        this.frontendServer.start();
    }

    public synchronized void stop() {
        isMustInitialized();
        this.frontendServer.stop();
        this.executorService.shutdownNow();
    }

    public synchronized void finialize() {
        this.frontendServer = null;
        this.executorService = null;
        this.forwarderFactory = null;
    }

    @Override
    public boolean isRunning() {
        return this.initialized && this.frontendServer != null
                && this.frontendServer.isRunning();
    }

    public boolean accept(String from, String recipient) {
        if (allowedFrom != null && allowedFrom.length > 0) {
            if (StringUtils.isBlank(myDomain) || !from.endsWith(myDomain)) {
                boolean okay = false;
                for (String domain : allowedFrom) {
                    if (from.endsWith(domain)) {
                        okay = true;
                        break;
                    }
                }
                if (!okay) {
                    numRejected.getAndIncrement();
                    logger.info("Rejected mail from: {}", from);
                    return false;
                }
            }
        }
        if (allowedTo != null && allowedTo.length > 0) {
            if (StringUtils.isBlank(myDomain) || !recipient.endsWith(myDomain)) {
                boolean okay = false;
                for (String domain : allowedTo) {
                    if (recipient.endsWith(domain)) {
                        okay = true;
                        break;
                    }
                }
                if (!okay) {
                    numRejected.getAndIncrement();
                    logger.info("Rejected mail to: {}", recipient);
                    return false;
                }
            }
        }
        numAccepted.getAndIncrement();
        return true;
    }

    public void deliver(String from, String recipient, InputStream data)
            throws IOException {
        try {
            executeForwarding(from, recipient, data);
        } catch (IOException e) {
            numFailed.getAndIncrement();
            throw e;
        }
    }

    /**
     * Invoked by {@link #deliver(String, String, InputStream)} internally.
     * 
     * @param from
     * @param recipient
     * @param data
     * @throws IOException
     */
    protected void executeForwarding(String from, String recipient,
            InputStream data) throws IOException {
        isMustInitialized();
        logger.info("Forwarding message from " + from + " to " + recipient);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int size = IOUtils.copy(data, baos);
        baos.close();
        if (size == -1) {
            throw new TooMuchDataException("Received mail data larger than 2GB");
        }
        MailContext received = new MailContext(from, recipient,
                baos.toByteArray());
        MailForwardCommand command = null;
        try {
            Validate.notNull(getForwarderFactory(),
                    "Forwarder factory must not be null");
            command = getForwarderFactory().newForwardCommand(received);
        } catch (RuntimeException e) {
            logger.error("Forwarder creating failed on " + e.toString(), e);
            throw new IOException("No mail forwarder available", e);
        }
        try {
            executorService.execute(command);
            numForwarded.getAndIncrement();
        } catch (RejectedExecutionException e) {
            logger.error(
                    "Concurrent forwarder thread insufficent: " + e.toString(),
                    e);
            throw new IOException("Forwarder worker thread count overflow", e);
        }
    }

    /**
     * @return true if this component has not been initialized yet.
     */
    protected boolean isInitialized() {
        if (this.initialized) {
            logger.info("Listener has been initialzed already, operation ignored");
            return false;
        }
        return true;
    }

    /**
     * @throws UnsupportedOperationException if this component has not been
     *             initialized.
     */
    protected void isMustInitialized() throws UnsupportedOperationException {
        if (!this.initialized) {
            throw new UnsupportedOperationException(
                    "Listener has NOT been initialzed yet, operation not allowed");
        }
    }

    @ManagedAttribute
    public String getDisplayableBindAddress() {
        return this.frontendServer == null ? "Not bind"
                : this.frontendServer.getDisplayableLocalSocketAddress();
    }

    @ManagedAttribute
    public InetAddress getBindAddress() {
        return bindAddress;
    }

    @ManagedAttribute
    public int getListenPort() {
        return listenPort;
    }

    @ManagedAttribute
    public int getForwarderCnt() {
        return forwarderCnt;
    }

    public FromAddressRelayForwarderFactory getForwarderFactory() {
        return forwarderFactory;
    }

    public void setForwarderFactory(
            FromAddressRelayForwarderFactory forwarderFactory) {
        this.forwarderFactory = forwarderFactory;
    }

    @ManagedAttribute
    public String getForwarderFactoryType() {
        return ObjectUtil.nullSafeClassCanonicalName(forwarderFactory);
    }

    @ManagedAttribute
    public String getMyDomain() {
        return myDomain;
    }

    @ManagedAttribute
    public String[] getAllowedTo() {
        return allowedTo;
    }

    @ManagedAttribute
    public String[] getAllowedFrom() {
        return allowedFrom;
    }

    public SMTPServer getFrontendServer() {
        return frontendServer;
    }

    @ManagedAttribute
    public void setAllowedFrom(String[] allowedFrom) {
        this.allowedFrom = allowedFrom;
    }

    @ManagedAttribute
    public String getAllowedFromByString() {
        return StringUtils.join(getAllowedFrom(), ",");
    }

    @ManagedAttribute
    public void setAllowedFromByString(String allowedFrom) {
        if (StringUtils.isNotEmpty(allowedFrom)) {
            setAllowedFrom(StringUtils.split(allowedFrom, ","));
        } else {
            setAllowedFrom(null);
        }
    }

    @ManagedAttribute
    public void setAllowedTo(String[] allowedTo) {
        this.allowedTo = allowedTo;
    }

    @ManagedAttribute
    public String getAllowedToByString() {
        return StringUtils.join(getAllowedTo(), ",");
    }

    @ManagedAttribute
    public void setAllowedToByString(String allowedTo) {
        if (StringUtils.isNotEmpty(allowedTo)) {
            setAllowedTo(StringUtils.split(allowedTo, ","));
        } else {
            setAllowedTo(null);
        }
    }

    @ManagedAttribute
    public void setMyDomain(String myDomain) {
        this.myDomain = myDomain;
    }

    public void setBindAddress(InetAddress bindAddress) {
        if (isInitialized()) {
            this.bindAddress = bindAddress;
        }
    }

    public void setBindAddressByString(String bindAddress) {
        if (StringUtils.isNotEmpty(bindAddress)) {
            try {
                setBindAddress(InetAddress.getByName(bindAddress));
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            setBindAddress(null);
        }
    }

    public void setListenPort(int listenPort) {
        if (isInitialized()) {
            this.listenPort = listenPort;
        }
    }

    public void setForwarderCnt(int forwarderCnt) {
        if (isInitialized()) {
            this.forwarderCnt = forwarderCnt;
        }
    }

    @ManagedAttribute
    public Number getNumRejected() {
        return numRejected.get();
    }

    @ManagedAttribute
    public Number getNumAccepted() {
        return numAccepted.get();
    }

    @ManagedAttribute
    public Number getNumForwarded() {
        return numForwarded.get();
    }

    @ManagedAttribute
    public Number getNumFailed() {
        return numFailed.get();
    }

}
