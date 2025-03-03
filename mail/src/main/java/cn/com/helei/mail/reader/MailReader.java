package cn.com.helei.mail.reader;

import cn.com.helei.mail.exception.MailReadException;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

public class MailReader {

    private final Properties properties;

    public MailReader(String protocol, String host, String port, String sslEnable) {
        properties = new Properties();
        properties.put("mail.store.protocol", protocol);
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", port);
        properties.put("mail.imap.ssl.enable", sslEnable);
    }

    public MailReader(Properties properties) {
        this.properties = properties;
    }

    public List<Message> stoppableReadMessage(
            String username,
            String password,
            int count,
            Function<Message, Boolean> messageScanner
    ) {
        try {
            // 创建会话
            Session session = Session.getInstance(properties);
            // 连接到邮件存储
            Store store = session.getStore(properties.getProperty("mail.store.protocol"));
            store.connect(username, password);

            // 打开收件箱
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // 获取邮件
            Message[] messages = inbox.getMessages();

            List<Message> messageList = Arrays.asList(messages)
                    .subList(Math.max(0, messages.length - count), messages.length);

            List<Message> list = new ArrayList<>(messageList.size());
            for (int i = messageList.size() - 1; i >= 0; i--) {
                if (!messageScanner.apply(messageList.get(i))) {
                    list.add(messageList.get(i));
                } else {
                    break;
                }
            }

            // 关闭连接
            inbox.close(false);
            store.close();

            return list;
        } catch (Exception e) {
            throw new MailReadException(e);
        }
    }

    public <T> List<T> readMessage(
            String username,
            String password,
            int count,
            Function<Message, T> messageConverter
    ) {
        try {
            // 创建会话
            Session session = Session.getInstance(properties);
            // 连接到邮件存储
            Store store = session.getStore(properties.getProperty("mail.store.protocol"));
            store.connect(username, password);

            // 打开收件箱
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // 获取邮件
            Message[] messages = inbox.getMessages();

            List<Message> messageList = Arrays.asList(messages)
                    .subList(Math.max(0, messages.length - count), messages.length);

            List<T> list = new ArrayList<>(messageList.size());
            for (int i = messageList.size() - 1; i >= 0; i--) {
                list.add(messageConverter.apply(messageList.get(i)));
            }

            // 关闭连接
            inbox.close(false);
            store.close();

            return list;
        } catch (Exception e) {
            throw new MailReadException(e);
        }
    }

    public List<String> readMessageContent(
            String username,
            String password,
            int count
    ) {
        return readMessage(username, password, count, MailReader::getTextFromMessage);
    }


    public static String getTextFromMessage(Message message) {
        try {
            if (message.isMimeType("text/plain")) {
                return message.getContent().toString();
            } else if (message.isMimeType("multipart/*")) {
                MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < mimeMultipart.getCount(); i++) {
                    BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                    result.append(bodyPart.getContent().toString());
                }
                return result.toString();
            }
        } catch (Exception e) {
            throw new MailReadException("邮件内容读取时发生错误", e);
        }

        return "";
    }




}
