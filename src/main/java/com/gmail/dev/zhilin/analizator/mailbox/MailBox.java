package com.gmail.dev.zhilin.analizator.mailbox;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeUtility;
import javax.mail.search.AndTerm;
import javax.mail.search.DateTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.gmail.dev.zhilin.analizator.util.DateUtil;

@Component
public class MailBox {

    @Value("${mailbox.username}")
    private String username;
    @Value("${mailbox.password}")
    private String password;
    @Value("${mailbox.host}")
    private String host;
    @Value("${mailbox.port}")
    private String port;
    @Value("${folderpath.tempfiles}")
    private String tempFolderPath;
    @Value("${mailbox.sender}")
    private String sender;
    @Value("${mailbox.vdksubject}")
    private String vdkSubject;
    @Value("${mailbox.msksubject}")
    private String mskSubject;
    @Value("${mailbox.vdkfilename}")
    private String vdkFileName;
    @Value("${mailbox.mskfilename}")
    private String mskFileName;
    
    private Properties initializeProps() {
        Properties props = new Properties();

        props.put("mail.imap.host", host);
        props.put("mail.imap.port", port);
        props.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.imap.socketFactory.fallback", "false");
        props.put("mail.imap.socketFactory.port", port);
        props.put("mail.imap.auth", "true");
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.protocols", "TLSv1.2");

        return props;
    }

    public Optional<File> retrieveVdkPriceList(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date from = null;
        Date to = null;
        Optional<File> file = null;

        try {
            // TODO: need to change time before deploying, the server has another time zone - MSK
            from = isoFormat.parse(formatter.format(date.minusDays(1)) + "T18:00:00");
            to = isoFormat.parse(formatter.format(date) + "T08:59:59");
            file = findVdkPriceList(from, to);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (file == null)
            return Optional.empty();

        return file;
    }
    
    public Optional<File> retrieveMskPriceList(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date from = null;
        Date to = null;
        Optional<File> file = null;
        
        try {
            // TODO: need to change time before deploying, the server has another time zone - MSK
            from = isoFormat.parse(formatter.format(date) + "T01:00:00");
            to = isoFormat.parse(formatter.format(date) + "T15:59:59");
            file = findMskPriceList(from, to);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (file == null)
            return Optional.empty();
        
        return file;
    }

    private Optional<File> findVdkPriceList(Date from, Date to) {
        Properties props = initializeProps();
        Session session = Session.getDefaultInstance(props);
        Store store;
        Folder folderInbox;
        File file = null;
        
        try {
            store = session.getStore("imap");
            store.connect(username, password);

            folderInbox = store.getFolder("INBOX");
            folderInbox.open(Folder.READ_ONLY);

            SearchTerm searchCondition = new AndTerm(new SearchTerm[] { new FromStringTerm(sender),
                    new SubjectTerm(vdkSubject), new ReceivedDateTerm(DateTerm.GT, DateUtil.minusDays(from, 1)),
                    new ReceivedDateTerm(DateTerm.LT, DateUtil.plusDays(to, 1)), });

            Message[] messages = folderInbox.search(searchCondition);

            for (Message message : messages) {
                if (message.getReceivedDate().after(from) && message.getReceivedDate().before(to)
                        && message.getContentType().contains("multipart")) {
                    Multipart multipart = (Multipart) message.getContent();
                    int numberOfParts = multipart.getCount();

                    for (int partCount = 0; partCount < numberOfParts; partCount++) {
                        MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(partCount);

                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                            String fileName = MimeUtility.decodeText(part.getFileName());

                            if (fileName.equals(vdkFileName)) {
                                file = File.createTempFile("vdk", ".xlsx", new File(tempFolderPath));
                                part.saveFile(file);
                                break;
                            }
                        }
                    }
                }
            }

            folderInbox.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (file == null)
            return Optional.empty();

        return Optional.of(file);
    }

    private Optional<File> findMskPriceList(Date from, Date to) {
        Properties props = initializeProps();
        Session session = Session.getDefaultInstance(props);
        Store store;
        Folder folderInbox;
        File file = null;
        
        try {
            store = session.getStore("imap");
            store.connect(username, password);
            
            folderInbox = store.getFolder("INBOX");
            folderInbox.open(Folder.READ_ONLY);
            
            SearchTerm searchCondition = new AndTerm(new SearchTerm[] { new FromStringTerm(sender),
                    new SubjectTerm(mskSubject), new ReceivedDateTerm(DateTerm.GT, DateUtil.minusDays(from, 2)),
                    new ReceivedDateTerm(DateTerm.LT, DateUtil.plusDays(to, 2)), });
            
            Message[] messages = folderInbox.search(searchCondition);
            
            for (Message message : messages) {
                if (message.getReceivedDate().after(from) && message.getReceivedDate().before(to)
                        && message.getContentType().contains("multipart")) {
                    Multipart multipart = (Multipart) message.getContent();
                    int numberOfParts = multipart.getCount();
                    
                    for (int partCount = 0; partCount < numberOfParts; partCount++) {
                        MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(partCount);
                        
                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
                            String fileName = MimeUtility.decodeText(part.getFileName());
                            if (fileName.equals(mskFileName)) {
                                file = File.createTempFile("msk", ".xlsx", new File(tempFolderPath));
                                part.saveFile(file);
                                break;
                            }
                        }
                    }
                }
            }
            
            folderInbox.close(false);
            store.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (file == null)
            return Optional.empty();
        
        return Optional.of(file);
    }
    
}
