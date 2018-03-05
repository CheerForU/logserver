package cn.com.xxx.logserver.common;

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public final class PropertyPlaceholder extends PropertyPlaceholderConfigurer
{

    private static final PropertyPlaceholder INSTANCE = new PropertyPlaceholder();

    private Properties props = null;

    private PropertyPlaceholder()
    {

    }

    public static PropertyPlaceholder getInstance()
    {
        return INSTANCE;
    }

    @Override
    protected void processProperties(
            final ConfigurableListableBeanFactory beanFactoryToProcess,
            final Properties props) throws BeansException
    {
        super.processProperties(beanFactoryToProcess, props);
        this.props = props;
    }

    public String getProperty(final String key)
    {
        if (null == props)
        {
            return null;
        }

        return props.getProperty(key);
    }

}
