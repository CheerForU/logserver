package cn.com.xxx.logserver.filter.oms.syslog;

import java.util.ArrayList;

import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.com.xxx.common.common.CommonUtil;
import cn.com.xxx.common.common.ErrorCode;
import cn.com.xxx.common.file.XMLOper;
import cn.com.xxx.common.msg.MessageConstructor;
import cn.com.xxx.common.msg.ResponseNode;
import cn.com.xxx.common.msg.XmlMessageListener;
import cn.com.xxx.logserver.LogServer;
import cn.com.xxx.logserver.common.ErrorNumber;
import cn.com.xxx.logserver.common.MessageName;
import cn.com.xxx.logserver.filter.oms.model.SysConfig;
import cn.com.xxx.logserver.filter.oms.syslog.model.Syslog;
import cn.com.xxx.mdp.MdpUtil;
import cn.com.xxx.mdp.model.Message;
import cn.com.xxx.oms.syslog.SysLogUtil;

public class SyslogManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SyslogManager.class);

    private static Syslog syslog;

    /**
     * 消息初始化（消息订阅）
     */
    public static void init()
    {
        LogServer.getMdpInst()
                .subscribe(MessageName.MSG_UPDATESYSLOG, new UpdateSyslog());
    }

    /**
     * 消息销毁（消息反订阅）
     */
    public static void destroy()
    {
        LogServer.getMdpInst().unsubscribe(MessageName.MSG_UPDATESYSLOG);
    }

    /**
     * Syslog消息操作类
     */
    private static final class UpdateSyslog extends XmlMessageListener
    {
        // 接收到消息进行handle操作
        @Override
        protected void handle(final Message message, final Document document)
        {
            try
            {
                final String enable = XMLOper.selectSingleValueByXPath(document,
                        "/updateconfig/enable");
                final String protocol = XMLOper.selectSingleValueByXPath(document,
                        "/updateconfig/protocol");
                final String host = XMLOper.selectSingleValueByXPath(document,
                        "/updateconfig/host");
                final String port = XMLOper.selectSingleValueByXPath(document,
                        "/updateconfig/port");
                final String charset = XMLOper.selectSingleValueByXPath(document,
                        "/updateconfig/charset");

                if (CommonUtil.hasEmptyString(enable, protocol, host, port, charset))
                {
                    LOGGER.error("Invalid update syslog config message.");
                    final ResponseNode node = new ResponseNode("updateconfig",
                            ErrorCode.ERRNO_INVALID_PARAMS, "Invalid params.");
                    MdpUtil.reply(message, MessageConstructor.constructMsg(node));
                    return;
                }

                int portNum;
                try
                {
                    portNum = Integer.valueOf(port);
                }
                catch (final NumberFormatException e)
                {
                    LOGGER.error("Invalid update syslog config message.Parse port failed.");
                    final ResponseNode node = new ResponseNode("updateconfig",
                            ErrorCode.ERRNO_INVALID_PARAMS, "Invalid params.");
                    MdpUtil.reply(message, MessageConstructor.constructMsg(node));
                    return;
                }

                syslog = new Syslog(enable, protocol, host, port, charset);

                final ArrayList<SysConfig> list = new ArrayList<SysConfig>();
                list.add(new SysConfig("syslog_enable", enable));
                list.add(new SysConfig("syslog_protocol", protocol));
                list.add(new SysConfig("syslog_host", host));
                list.add(new SysConfig("syslog_port", port));
                list.add(new SysConfig("syslog_charset", charset));

                // 向数据库更新syslog配置
                for (int i = 0; i < list.size(); i++)
                {
                    SyslogDaoImpl.getInstance().updateSyslogConfig(list.get(i));
                }

                // 若禁用syslog，则关闭syslog的过滤并销毁syslog；若启用，则重新初始化加载SysLogUtil
                SysLogUtil.destroy();
                if ("true".equals(enable))
                {
                    SysLogUtil.init(protocol, host, portNum, charset, 8);
                }
                SyslogFilter.getInstance()
                        .setEnable("true".equals(enable) ? true : false);

                final ResponseNode response = new ResponseNode("updateconfig");
                LogServer.getMdpInst().reply(message,
                        MessageConstructor.constructMsg(response));
            }
            catch (final Exception e)
            {
                LOGGER.error("Failed to verify update syslog, syslog={}", syslog);
                LOGGER.error(e.getLocalizedMessage(), e);
                final ResponseNode response = new ResponseNode("updateconfig",
                        ErrorNumber.ERRNO_UPDATE_SYSLOG_FAILURE, e.toString());
                LogServer.getMdpInst().reply(message,
                        MessageConstructor.constructMsg(response));
            }
        }
    }

}
