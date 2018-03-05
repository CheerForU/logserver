package cn.com.xxx.logserver.filter.model;

import java.sql.SQLException;

import com.ibatis.sqlmap.client.extensions.ParameterSetter;
import com.ibatis.sqlmap.client.extensions.ResultGetter;
import com.ibatis.sqlmap.client.extensions.TypeHandlerCallback;

/**
 * 将数据库中的整形值转换LogLevel枚举值
 * @author zjr
 *
 */
public class LogLevelTypeHandler implements TypeHandlerCallback
{

    @Override
    public Object getResult(final ResultGetter getter) throws SQLException
    {
        LogLevel result = null;
        if (!getter.wasNull() && getter.getObject() != null)
        {
            for (final LogLevel level : LogLevel.values())
            {
                if (level.getValue() == (Integer) getter.getObject())
                {
                    result = level;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public void setParameter(final ParameterSetter arg0, final Object arg1)
            throws SQLException
    {
    }

    @Override
    public Object valueOf(final String arg0)
    {
        return null;
    }

}
