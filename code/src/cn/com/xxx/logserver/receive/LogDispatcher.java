package cn.com.xxx.logserver.receive;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.xxx.logserver.common.ConfigParamsHolder;
import cn.com.xxx.logserver.filter.FilterManager;
import cn.com.xxx.mdp.model.Message;

/**
 * 日志分发器
 */
public final class LogDispatcher implements Runnable
{

    private static final Logger LOGGER = LoggerFactory.getLogger(LogDispatcher.class);

    private static final LogDispatcher INSTANCE = new LogDispatcher();

    private static final String THREAD_NAME = "LogDispatcher";

    private final LogReceiver logReceiver = LogReceiver.getInstance();

    private final AtomicBoolean bRunning = new AtomicBoolean(false);

    private final LinkedBlockingQueue<Message> logQueue = new LinkedBlockingQueue<Message>();

    private LogDispatcher()
    {

    }

    public static LogDispatcher getInstance()
    {
        return INSTANCE;
    }

    public void start()
    {
        if (bRunning.compareAndSet(false, true))
        {
            LOGGER.info("Starting {}.", THREAD_NAME);

            final Thread thread = new Thread(this, THREAD_NAME);
            thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler()
                {
                    @Override
                    public void uncaughtException(final Thread t, final Throwable e)
                    {
                        LOGGER.error(e.getLocalizedMessage(), e);
                    }
                });
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
            clearQueue();
            LOGGER.info("Stoping {}.", THREAD_NAME);
        }
        else
        {
            LOGGER.info("{} had beend stopped.", THREAD_NAME);
        }
    }

    public void dispatch(final Message message)
    {
        if (null == message)
        {
            LOGGER.error("message is null.");
            return;
        }

        if (!(message.getContent() instanceof String))
        {
            LOGGER.error("Invalid message type.");
            return;
        }

        if (LOGGER.isInfoEnabled())
        {
            LOGGER.info("Receive log message. message={}", message);
        }

        try
        {
            logQueue.offer(message);
        }
        catch (final Exception e)
        {
            LOGGER.error("Put log message into queue failed. message={}", message);
            LOGGER.error(e.getLocalizedMessage(), e);
        }
    }

    public void clearQueue()
    {
        logQueue.clear();
    }

    @Override
    public void run()
    {
        LOGGER.info("{} is started.", THREAD_NAME);

        Message message = null;
        boolean bFlowControl = false;
        final int capacity = ConfigParamsHolder.getInstance().getLogqueueCapacity();
        final int threshold = Math.round(capacity
                * ConfigParamsHolder.getInstance().getLogqueueFlowcontrolRadio());

        while (bRunning.get())
        {
            // 日志队列流控
            if (logQueue.size() < threshold)
            {
                if (bFlowControl)
                {
                    logReceiver.resume();
                    bFlowControl = false;
                    LOGGER.info("The flow control of log queue is disabled.");
                }
            }
            else if (logQueue.size() >= capacity)
            {
                if (!bFlowControl)
                {
                    logReceiver.pause();
                    bFlowControl = true;
                    LOGGER.info("The flow control of log queue is enabled.");
                }
            }

            // 日志处理
            try
            {
                message = logQueue.poll(300, TimeUnit.MILLISECONDS);
            }
            catch (final InterruptedException e)
            {
                LOGGER.error("{} is interrupted.", THREAD_NAME);
                bRunning.compareAndSet(true, false);
                break;
            }

            if (null == message)
            {
                continue;
            }

            FilterManager.getInstance().dispose(message);
        }

        LOGGER.info("{} is stoped.", THREAD_NAME);
    }
}
