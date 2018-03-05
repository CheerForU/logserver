package cn.com.xxx.logserver.receive;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.ExchangeTypes;

import cn.com.xxx.common.common.CommonUtil;
import cn.com.xxx.logserver.common.PropertyPlaceholder;
import cn.com.xxx.mdp.model.MdpParams;
import cn.com.xxx.mdp.model.Message;
import cn.com.xxx.mdp.model.MessageType;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * 日志接收器
 */
public final class LogReceiver implements Runnable
{

    private static final Logger LOGGER = LoggerFactory.getLogger(LogReceiver.class);

    private static final LogReceiver INSTANCE = new LogReceiver();

    private static final String THREAD_NAME = "LogReceiver";

    private static final String LOGRECORDER_QUEUE = "cn.com.xxx.queue-logrecorder";

    private static final String LOGERCORDER_ROUTEKEY = "logrecorder";

    private static final String EXCHANGE_DIRECT = "cn.com.xxx.direct";

    private final AtomicBoolean bRunning = new AtomicBoolean(false);

    private final AtomicBoolean bInit = new AtomicBoolean(false);

    private final AtomicBoolean bReceiving = new AtomicBoolean(false);

    private ConnResource connResource = null;

    private Map<String, Long> deliveryTagBuffer = new HashMap<String, Long>();

    private LogReceiver()
    {

    }

    public static LogReceiver getInstance()
    {
        return INSTANCE;
    }

    public void start()
    {
        if (bRunning.compareAndSet(false, true))
        {
            bReceiving.set(true);
            LOGGER.info("Starting {}.", THREAD_NAME);

            final Thread thread = new Thread(this, THREAD_NAME);
            thread.start();
        }
        else
        {
            LOGGER.info("{} had beend started.", THREAD_NAME);
        }
    }

    public void stop()
    {
        if (bRunning.compareAndSet(true, false))
        {
            bReceiving.set(false);
            LOGGER.info("Stoping {}.", THREAD_NAME);
            try
            {
                synchronized (this)
                {
                    this.notify();
                }
            }
            catch (final Exception e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }
        else
        {
            LOGGER.info("{} had beend stopped.", THREAD_NAME);
        }
    }

    public void pause()
    {
        if (bRunning.get())
        {
            bReceiving.set(false);
        }
    }

    public void resume()
    {
        if (bRunning.get())
        {
            bReceiving.set(true);
            try
            {
                synchronized (this)
                {
                    this.notify();
                }
            }
            catch (final Exception e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }
    }

    public void reInit()
    {
        if (bRunning.get())
        {
            bInit.set(false);
            try
            {
                synchronized (this)
                {
                    this.notify();
                }
            }
            catch (final Exception e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }
    }

    @Override
    public void run()
    {
        LOGGER.info("{} is started.", THREAD_NAME);

        final PropertyPlaceholder holder = PropertyPlaceholder.getInstance();
        final String brokerUrl = holder.getProperty(MdpParams.BROKER_URL);
        final String brokerUsername = holder.getProperty(MdpParams.BROKER_USERNAME);
        final String brokerPassword = holder.getProperty(MdpParams.BROKER_PASSWORD);
        LOGGER.debug("logrecorder connect params. brokerUrl={}, brokerUsername={}, brokerPassword={}",
                brokerUrl,
                brokerUsername,
                brokerPassword);

        Message message = null;

        while (bRunning.get())
        {
            try
            {
                if (null == connResource)
                {
                    connResource = initConnResource(brokerUrl,
                            brokerUsername,
                            brokerPassword);
                    bInit.set(true);
                }

                final QueueingConsumer consumer = connResource.getConsumer();
                while (bRunning.get() && bInit.get())
                {
                    if (bReceiving.get())
                    {
                        final QueueingConsumer.Delivery delivery = consumer.nextDelivery(300);
                        if (null == delivery)
                        {
                            continue;
                        }
                        final Serializable content = delivery.getBody();
                        final BasicProperties properties = delivery.getProperties();
                        message = MessageConvertor.convert(content, properties);
                        deliveryTagBuffer.put(message.getId(), delivery.getEnvelope()
                                .getDeliveryTag());
                        LogDispatcher.getInstance().dispatch(message);
                    }
                    else
                    {
                        try
                        {
                            synchronized (this)
                            {
                                this.wait();
                            }
                        }
                        catch (final Exception e)
                        {
                            LOGGER.error(e.getLocalizedMessage(), e);
                        }
                    }
                }
            }
            catch (final Exception e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
            finally
            {
                LogDispatcher.getInstance().clearQueue();
                releaseConnResouce(connResource);
                connResource = null;
            }

            // 等待重连
            if (bRunning.get())
            {
                try
                {
                    synchronized (this)
                    {
                        this.wait(3000);
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
            }
        }

        LOGGER.info("{} is stoped.", THREAD_NAME);
    }

    private ConnResource initConnResource(final String brokerUrl,
            final String brokerUsername, final String brokerPassword)
    {
        try
        {
            final ConnectionFactory connfac = new ConnectionFactory();
            connfac.setUri(new URI(brokerUrl));
            connfac.setUsername(brokerUsername);
            connfac.setPassword(brokerPassword);
            connfac.setHandshakeTimeout(30000);

            Connection conn = null;
            try
            {
                conn = connfac.newConnection();
                LOGGER.info("Success to create connection without ssl.");
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to create connection without ssl.");
                LOGGER.error(e.getLocalizedMessage(), e);
                try
                {
                    connfac.useSslProtocol();
                    conn = connfac.newConnection();
                    LOGGER.info("Success to create connection with ssl.");
                }
                catch (final Exception exc)
                {
                    LOGGER.error("Failed to create connection wit ssl.");
                    LOGGER.error(e.getLocalizedMessage(), exc);
                    throw new Exception("Failed to create connection.");
                }
            }

            final Channel channel = conn.createChannel();
            channel.basicQos(10);
            final Map<String, Object> params = new HashMap<String, Object>();
            // 队列超时时间30分钟
            params.put("x-expires", 1800000);
            channel.queueDeclare(LOGRECORDER_QUEUE, true, false, false, params);
            channel.exchangeDeclare(EXCHANGE_DIRECT,
                    ExchangeTypes.DIRECT,
                    true,
                    false,
                    null);
            channel.queueBind(LOGRECORDER_QUEUE, EXCHANGE_DIRECT, LOGERCORDER_ROUTEKEY);
            final QueueingConsumer consumer = new QueueingConsumer(channel);
            channel.basicConsume(LOGRECORDER_QUEUE, false, consumer);
            return new ConnResource(connfac, conn, channel, consumer);
        }
        catch (final Exception e)
        {
            LOGGER.error(e.getLocalizedMessage(), e);
            return null;
        }
    }

    private void releaseConnResouce(final ConnResource connResource)
    {
        if (null == connResource)
        {
            return;
        }

        final Channel channel = connResource.getChannel();
        if (null != channel)
        {
            try
            {
                channel.close();
            }
            catch (final Exception e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }

        final Connection connection = connResource.getConnection();
        if (null != connection)
        {
            try
            {
                connection.close();
            }
            catch (final Exception e)
            {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }
    }

    public void releaseMessage(final Message message, final boolean ack)
    {
        if (null == message || null == message.getId())
        {
            return;
        }
        if (null == connResource || null == connResource.getChannel())
        {
            return;
        }

        final Long deliveryTag = deliveryTagBuffer.remove(message.getId());

        if (null == deliveryTag)
        {
            return;
        }

        try
        {
            if (ack)
            {
                connResource.getChannel().basicAck(deliveryTag, false);
            }
            else
            {
                connResource.getChannel().basicNack(deliveryTag, false, true);
            }
        }
        catch (final IOException e)
        {
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }

    private static class MessageConvertor
    {
        private static String SRC_ID = "srcId";
        private static String MSG_NAME = "msgName";
        private static String DEST = "destination";

        public final static Message convert(final Serializable content,
                final BasicProperties properties)
        {
            if (null != properties && "text/plain".equals(properties.getContentType()))
            {
                final Message message = new Message();
                message.setId(properties.getMessageId());
                message.setCorrelationId(properties.getCorrelationId());
                if (null != properties.getHeaders())
                {
                    final Object srcId = properties.getHeaders().get(SRC_ID);
                    if (null != srcId)
                    {
                        message.setSrcId(srcId.toString());
                    }
                    final Object name = properties.getHeaders().get(MSG_NAME);
                    if (null != name)
                    {
                        message.setName(name.toString());
                    }
                    final Object destId = properties.getHeaders().get(DEST);
                    if (null != destId)
                    {
                        message.setDestId((destId.toString()));
                    }
                }
                message.setReplyTo(properties.getReplyTo());
                if (!CommonUtil.isEmpty(properties.getExpiration()))
                {
                    try
                    {
                        message.setTimeout(Long.parseLong(properties.getExpiration()));
                    }
                    catch (final Exception e)
                    {
                        LOGGER.error(e.getLocalizedMessage(), e);
                        message.setTimeout(0L);
                    }
                }
                message.setType(MessageType.valueOf(properties.getType()));
                try
                {
                    message.setContent(new String((byte[]) content, "utf8"));
                }
                catch (final UnsupportedEncodingException e)
                {
                    LOGGER.error(e.getLocalizedMessage(), e);
                }
                return message;
            }
            return null;
        }
    }
}
