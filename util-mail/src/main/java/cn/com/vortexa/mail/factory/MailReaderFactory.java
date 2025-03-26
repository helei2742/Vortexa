package cn.com.vortexa.mail.factory;

import cn.com.vortexa.mail.constants.MailProtocolType;
import cn.com.vortexa.mail.reader.MailReader;


public class MailReaderFactory {

    public static MailReader getMailReader(MailProtocolType protocol, String host, String port, boolean useSSL) {
        return new MailReader(protocol.name(), host, port, String.valueOf(useSSL));
    }

}
