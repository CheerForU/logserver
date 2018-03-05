package cn.com.xxx.logserver.receive;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

/**
 * 连接资源
 */
public final class ConnResource
{

    private ConnectionFactory connFactory;

    private Connection connection;

    private Channel channel;

    private QueueingConsumer consumer;

    public ConnResource(final ConnectionFactory connFactory, final Connection connection,
            final Channel channel, final QueueingConsumer consumer)
    {
        super();
        this.connFactory = connFactory;
        this.connection = connection;
        this.channel = channel;
        this.consumer = consumer;
    }

    public ConnectionFactory getConnFactory()
    {
        return connFactory;
    }

    public void setConnFactory(final ConnectionFactory connFactory)
    {
        this.connFactory = connFactory;
    }

    public Connection getConnection()
    {
        return connection;
    }

    public void setConnection(final Connection connection)
    {
        this.connection = connection;
    }

    public Channel getChannel()
    {
        return channel;
    }

    public void setChannel(final Channel channel)
    {
        this.channel = channel;
    }

    public QueueingConsumer getConsumer()
    {
        return consumer;
    }

    public void setConsumer(final QueueingConsumer consumer)
    {
        this.consumer = consumer;
    }

}
