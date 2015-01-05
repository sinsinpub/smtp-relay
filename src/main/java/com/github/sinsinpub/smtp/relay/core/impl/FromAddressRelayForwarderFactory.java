package com.github.sinsinpub.smtp.relay.core.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.mail.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.github.sinsinpub.smtp.relay.context.MailContext;
import com.github.sinsinpub.smtp.relay.core.MailForwardCommand;
import com.github.sinsinpub.smtp.relay.core.MailForwardCommandFactory;
import com.github.sinsinpub.smtp.relay.transport.AdditiveWaitRetryStrategy;
import com.github.sinsinpub.smtp.relay.utils.ConcurrentUtil;

/**
 * Just build simple relay forwarder command by the received from address.
 * 
 * @see SimpleRelayCommand
 * @author sin_sin
 */
@ThreadSafe
@ManagedResource(objectName = FromAddressRelayForwarderFactory.OBJECT_NAME, description = "A controller of building mail forwarder.")
public class FromAddressRelayForwarderFactory implements
        MailForwardCommandFactory, BeanFactoryAware, InitializingBean {

    public static final String OBJECT_NAME = "com.github.sinsinpub.smtp.relay:type=Backend,name=ForwarderFactory";
    public static final String DELIVER_RULE_DELIM = ",";
    public static final String ADDRESS_SESSION_DELIM = ":";
    public static final String DEFAULT_ADDRESS_WILDCARD = "*";

    private final static Logger logger = LoggerFactory.getLogger(FromAddressRelayForwarderFactory.class);

    @GuardedBy("itself")
    protected BeanFactory beanFactory;
    @GuardedBy("getFromAddressSessionMap")
    protected Map<String, Session> fromAddressSessionMap;
    protected String[] fromAddressDeliverRules;
    @GuardedBy("itself")
    protected Session defaultSession;

    protected int retryTimes = AdditiveWaitRetryStrategy.DEFAULT_NUMBER_OF_RETRIES;
    protected long retryStartingWaitMillis = AdditiveWaitRetryStrategy.STARTING_WAIT_TIME;
    protected long retryWaitTimeIncrementMillis = AdditiveWaitRetryStrategy.WAIT_TIME_INCREMENT;
    protected final ConcurrentMap<String, AtomicLong> numForwarded = new ConcurrentHashMap<String, AtomicLong>();
    protected final ConcurrentMap<String, AtomicLong> numForwardingException = new ConcurrentHashMap<String, AtomicLong>();

    /**
     * Default empty constructor.
     */
    public FromAddressRelayForwarderFactory() {
        super();
    }

    public MailForwardCommand newForwardCommand(MailContext mailContext) {
        Validate.notNull(mailContext, "Mail context must not be null");
        Map<String, Session> sessionMap = getFromAddressSessionMap();
        if (sessionMap == null || sessionMap.isEmpty()) {
            throw new IllegalStateException(
                    "From address and session map not configured properly");
        }
        String from = mailContext.getEnvelopeSender();
        String matchedRule = null;
        Session backendSession = null;
        // Full matched address
        if (sessionMap.containsKey(from)) {
            matchedRule = from;
            backendSession = sessionMap.get(from);
        }
        // Try prefix matching
        if (backendSession == null) {
            for (Entry<String, Session> entry : sessionMap.entrySet()) {
                if (from.startsWith(entry.getKey())) {
                    matchedRule = entry.getKey();
                    backendSession = entry.getValue();
                    break;
                }
            }
        }
        // Try to use default one
        if (backendSession == null) {
            matchedRule = DEFAULT_ADDRESS_WILDCARD;
            backendSession = sessionMap.get(DEFAULT_ADDRESS_WILDCARD);
            if (backendSession == null) {
                // Also mark down failed forwarding
                ConcurrentUtil.incrementLong(numForwarded, "null");
                throw new IllegalStateException(
                        "No matched rule to forward mail from " + from);
            }
        }
        logger.info("Building forwarder for {} with session {{}}", from,
                matchedRule + ADDRESS_SESSION_DELIM + backendSession.toString());
        ConcurrentUtil.incrementLong(numForwarded, String.valueOf(matchedRule));
        SimpleRelayCommand command = new SimpleRelayCommand(
        // The mail context to be forwarded
                mailContext,
                // Finally matched session instance
                backendSession,
                // ErrDump not used yet
                null,
                // Customized retry parameters
                new AdditiveWaitRetryStrategy(getRetryTimes(),
                        getRetryStartingWaitMillis(),
                        getRetryWaitTimeIncrementMillis()));
        // For exception counting
        command.setSessionName(matchedRule);
        command.setExceptionCounter(numForwardingException);
        return command;
    }

    /**
     * Update Session map according from address and spring bean name.
     */
    protected synchronized void buildAndUpdateSessionMap() {
        logger.info("Building mail session map with delivering rules: {}",
                getFromAddressDeliverRulesByString());
        if (this.fromAddressSessionMap == null) {
            this.fromAddressSessionMap = new HashMap<String, Session>(
                    this.fromAddressDeliverRules.length);
        }
        if (this.beanFactory == null) {
            logger.info("No rule added as bean factory context not found");
        } else {
            for (String item : getFromAddressDeliverRules()) {
                String from = StringUtils.substringBefore(item,
                        ADDRESS_SESSION_DELIM);
                String sessionBeanName = StringUtils.substringAfter(item,
                        ADDRESS_SESSION_DELIM);
                try {
                    this.fromAddressSessionMap.put(from,
                            this.beanFactory.getBean(sessionBeanName,
                                    Session.class));
                } catch (RuntimeException e) {
                    logger.error(String.format(
                            "Failed on binding session '%s' with '%s': %s",
                            sessionBeanName, from, e.toString()));
                }
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (getFromAddressDeliverRules() != null
                && this.fromAddressSessionMap == null) {
            buildAndUpdateSessionMap();
        }
        if (this.fromAddressSessionMap != null) {
            this.fromAddressSessionMap.put(DEFAULT_ADDRESS_WILDCARD,
                    getDefaultSession());
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public Map<String, Session> getFromAddressSessionMap() {
        return fromAddressSessionMap == null ? null
                : new HashMap<String, Session>(fromAddressSessionMap);
    }

    public void setFromAddressSessionMap(
            Map<String, Session> fromAddressSessionMap) {
        this.fromAddressSessionMap = fromAddressSessionMap;
    }

    @ManagedOperation()
    public void clearFromAddressSessionMap() {
        this.fromAddressSessionMap.clear();
    }

    @ManagedAttribute
    public String[] getFromAddressDeliverRules() {
        return fromAddressDeliverRules;
    }

    @ManagedAttribute
    public void setFromAddressDeliverRules(String[] fromAddressDeliverRules) {
        this.fromAddressDeliverRules = fromAddressDeliverRules;
        if (this.fromAddressDeliverRules != null
                && this.fromAddressDeliverRules.length > 0
                && this.fromAddressSessionMap != null) {
            buildAndUpdateSessionMap();
        }
    }

    @ManagedAttribute
    public String getFromAddressDeliverRulesByString() {
        return StringUtils.join(this.fromAddressDeliverRules,
                DELIVER_RULE_DELIM);
    }

    @ManagedAttribute
    public void setFromAddressDeliverRulesByString(
            String fromAddressDeliverRules) {
        if (StringUtils.isNotBlank(fromAddressDeliverRules)) {
            setFromAddressDeliverRules(StringUtils.split(
                    fromAddressDeliverRules, DELIVER_RULE_DELIM));
        } else {
            setFromAddressDeliverRules(null);
        }
    }

    public Session getDefaultSession() {
        return defaultSession;
    }

    public void setDefaultSession(Session defaultSession) {
        this.defaultSession = defaultSession;
        if (this.fromAddressSessionMap != null) {
            this.fromAddressSessionMap.put(DEFAULT_ADDRESS_WILDCARD,
                    this.defaultSession);
        }
    }

    @ManagedAttribute
    public int getRetryTimes() {
        return retryTimes;
    }

    @ManagedAttribute
    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    @ManagedAttribute
    public long getRetryStartingWaitMillis() {
        return retryStartingWaitMillis;
    }

    @ManagedAttribute
    public void setRetryStartingWaitMillis(long retryStartingWaitMillis) {
        this.retryStartingWaitMillis = retryStartingWaitMillis;
    }

    @ManagedAttribute
    public long getRetryWaitTimeIncrementMillis() {
        return retryWaitTimeIncrementMillis;
    }

    @ManagedAttribute
    public void setRetryWaitTimeIncrementMillis(
            long retryWaitTimeIncrementMillis) {
        this.retryWaitTimeIncrementMillis = retryWaitTimeIncrementMillis;
    }

    @ManagedAttribute
    public Map<String, ?> getNumForwarded() {
        return numForwarded;
    }

    @ManagedAttribute
    public List<String> getNumForwardedOrdered() {
        return ConcurrentUtil.sortEntriesByNumberValues(numForwarded, -1);
    }

    @ManagedAttribute
    public Map<String, ?> getNumForwardingException() {
        return numForwardingException;
    }

    @ManagedAttribute
    public List<String> getNumForwardingExceptionOrdered() {
        return ConcurrentUtil.sortEntriesByNumberValues(numForwardingException,
                -1);
    }

}
