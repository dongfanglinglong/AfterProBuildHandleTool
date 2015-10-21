package com.telecom.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * 发送项目日志邮件
 *
 * @author xiao
 */
public class MailUtil {

    /**
     * 表示一条svn上传注释
     *
     * @author xiao
     */
    public static class Comment {
        public String content;
        public String author;
        public String time;
        public String code;
        public boolean isImportant;

        @Override
        protected Object clone() throws CloneNotSupportedException {
            Comment cmt = new Comment();
            cmt.content = content;
            cmt.author = author;
            cmt.time = time;
            cmt.code = code;
            cmt.isImportant = isImportant;
            return cmt;
        }

    }

    /**
     * 表示svnlog的内容
     *
     * @author xiao
     */
    public static class SvnLogData {
        // 更新日志的状态
        public static final int NEW_RELEASE = 0;
        public static final int NO_CHANGE = 1;
        public static final int CHANGED = 2;

        public ArrayList<String> apkNames = new ArrayList<String>();
        public ArrayList<String> apkMd5s = new ArrayList<String>();
        public String versionCode;
        public String versionName;
        public String time;
        public String svnReleaseCodes;
        public String svnInfo;
        public int changeType;
        public String lastCode;
        public String nowCode;
        public String svnLog;
        public List<Comment> comments;
        public List<Comment> newCmts;
        public List<Comment> bugCmts;
        public List<Comment> chgCmts;
        public List<Comment> otherCmts;
    }

    private static final String FORM_NAME = "Hudson项目发布提醒";
    private static final String SUBJECT_PATTEN = "[HudsonRelease]已发布项目: %s (版本: %s, 时间: %s)";
    private static final String SEND_INFO_ENV_PATH = "hudsonToolsPath";
    private static final String SEND_INFO_NAME = "send_mail_info.prop";

    private static final String SERVER_PROP = "server";
    private static final String PORT_PROP = "port";
    private static final String USER_PROP = "user";
    private static final String PASSWD_PROP = "passwd";
    private static final String FTP_BASE_PATH = "ftpbasepath";
    private static final String PATH_MATCH = "pathmatch";
    private static final String TO_PROP = "to";

    private static String sServer;
    private static String sPort;
    private static String sUser;
    private static String sPasswd;
    private static String sFtpBasePath;
    private static String sPathMatch;
    private static List<String> sTo;

    /**
     * 读取邮件发送配置信息
     *
     * @return
     */
    private static boolean loadSendInfo() {
        String envPath = System.getenv(SEND_INFO_ENV_PATH);
        String infoFilePath = (envPath == null ? "" : envPath + "/") + SEND_INFO_NAME;
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(infoFilePath));
            sServer = props.getProperty(SERVER_PROP);
            sPort = props.getProperty(PORT_PROP, "25");
            sUser = props.getProperty(USER_PROP);
            sPasswd = props.getProperty(PASSWD_PROP);
            String toStr = props.getProperty(TO_PROP, "");
            String[] commRecipients = toStr.split("\\s");
            String[] recipients = XMLTools.getRecipients();
            sTo = new ArrayList<String>();
            if (recipients != null)
                for (String recipient : recipients)
                    if (recipient != null && !"".equals(recipient))
                        sTo.add(recipient);
            if (commRecipients != null) {
                for (String recipient : commRecipients) {
                    if (recipient != null && !"".equals(recipient)) {
                        sTo.add(recipient);
                    }
                }
            }
            sFtpBasePath = props.getProperty(FTP_BASE_PATH);
            sPathMatch = props.getProperty(PATH_MATCH);
            System.out.println(infoFilePath + " loaded.");
            return true;
        } catch (FileNotFoundException e) {
            // e.printStackTrace();
            System.out.println(infoFilePath + " not found.");
        } catch (IOException e) {
            // e.printStackTrace();
            System.out.println(infoFilePath + " read error.");
        }
        return false;
    }

    /**
     * 发送日志邮件
     *
     * @param project
     * @param version
     * @param date
     * @param log
     */
    public static String sendLogEmail(String project, String version, String date, SvnLogData data, String pkgName,
                                      String outputPath, List<String> apkPaths, String customContentPath) {
        System.out.println("-------------> sendLogEmail() <--------------");
        if (!loadSendInfo()) {
            System.out.println("load send info error, abort send log mail.");
            return null;
        }
        String subject = String.format(SUBJECT_PATTEN, project, version, date);
        System.out.println("mail subject:" + subject);
        String mailContent = formatLog(pkgName, apkPaths, customContentPath, data);
        for (int retry = 0; retry < 3; retry++) {
            try {
                // 配置服务器信息
                Properties props = new Properties();
                props.put("mail.smtp.host", sServer);
                props.put("mail.smtp.port", String.valueOf(sPort));
                props.put("mail.smtp.auth", "true");
                Transport transport = null;
                Session session = Session.getDefaultInstance(props, null);
                transport = session.getTransport("smtp");
                transport.connect(sServer, sUser, sPasswd);
                // 生成邮件
                MimeMessage msg = new MimeMessage(session);
                msg.setSentDate(new Date());
                InternetAddress fromAddress = new InternetAddress(sUser, FORM_NAME, "UTF-8");
                msg.setFrom(fromAddress);
                msg.setSubject(subject, "UTF-8");
                // msg.setText(body, "UTF-8");
                Multipart mainPart = new MimeMultipart("mixed");
                msg.setContent(mainPart);
                // 生成邮件体
                BodyPart html = new MimeBodyPart();
                mainPart.addBodyPart(html);
                html.setContent(mailContent, "text/html; charset=utf-8");
                // 添加附件
                // for (String path : apkPaths) {
                // File apkFile = new File(path);
                // if (apkFile.exists()) {
                // MimeBodyPart attch = new MimeBodyPart();
                // mainPart.addBodyPart(attch);
                // attch.setFileName(apkFile.getName());
                // DataSource ds1 = new FileDataSource(apkFile);
                // DataHandler dh1 = new DataHandler(ds1);
                // attch.setDataHandler(dh1);
                // }
                // }
                // 准备收信人
                int size = sTo.size();
                if (size <= 0) {
                    System.out.println("no recipient, abort send mail.");
                    return mailContent;
                }
                InternetAddress[] toAddresses = new InternetAddress[size];
                System.out.println("add recipients:");
                for (int i = 0; i < size; i++) {
                    toAddresses[i] = new InternetAddress(sTo.get(i));
                    System.out.println(sTo.get(i));
                }
                msg.setRecipients(Message.RecipientType.TO, toAddresses);
                msg.saveChanges();
                // 发送
                System.out.println("log mail prepared. sending...");
                transport.sendMessage(msg, msg.getAllRecipients());
                System.out.println("send log mail success.");
                return mailContent;
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("send log mail retry.");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("send log mail failed.");
        return mailContent;
    }

    /**
     * 通过构建日志生成邮件体
     *
     * @param log
     * @param apkPaths
     * @return
     */
    private static String formatLog(String pkgName, List<String> apkPaths, String customContentPath, SvnLogData data) {
        // 生成 HTML 邮件体
        StringBuffer sb = new StringBuffer();
        sb.append(bold(font("基本信息:", "4", "SimHei", null)));
        sb.append(blockStart());
        for (int i = 0; i < apkPaths.size(); i++) {
            sb.append(bold(font("APK文件名: ", "3", "SimHei", null)));
            sb.append(font(data.apkNames.get(i), "3", null, "blue"));
            sb.append("<br/>");

            sb.append(bold(font("APK文件MD5: ", "3", "SimHei", null)));
            sb.append(font(data.apkMd5s.get(i), "3", null, "blue"));
            sb.append("<br/>");

            String path = apkPaths.get(i);
            System.out.println("ftp outputPath = " + path);
            System.out.println("ftp sPathMatch = " + sPathMatch);
            path = path.substring(path.indexOf(sPathMatch) + sPathMatch.length());
            String ftpaddress = sFtpBasePath + path;
            System.out.println("ftp ftpaddress = " + ftpaddress);

            sb.append(bold(font("FTP地址: ", "3", "SimHei", null)));
            sb.append(a(font(ftpaddress, "3", null, "blue"), ftpaddress));
            sb.append("<br/>");
        }

        sb.append(bold(font("APK包名: ", "3", "SimHei", null)));
        sb.append(font(pkgName, "3", null, "blue"));
        sb.append("<br/>");
        sb.append(bold(font("versionName值: ", "3", "SimHei", null)));
        sb.append(font(data.versionName, "3", null, "blue"));
        sb.append("<br/>");
        sb.append(bold(font("VersionCode值: ", "3", "SimHei", null)));
        sb.append(font(data.versionCode, "3", null, "blue"));
        sb.append("<br/>");

        sb.append(bold(font("SVN地址: ", "3", "SimHei", null)));
        sb.append("<br/>");
        String[] svnPaths = data.svnReleaseCodes.split("\n");
        for (String string : svnPaths) {
            sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
            sb.append(a(font(string, "3", null, "blue"), string));
            sb.append("<br/>");
        }

        sb.append(blockEnd());

        sb.append(bold(font("更新日志:", "4", "SimHei", null)));
        sb.append(blockStart());
        String[] logs = data.svnLog.split("\n");
        for (int i = 0; i < logs.length; i++) {
            sb.append((font(logs[i], "3", null, null)));
            sb.append("<br/>");
        }
        sb.append(blockEnd());
        return sb.toString();
    }

    private static String blockStart() {
        return "<blockquote style=\"margin: 10px 0 10px 40px; border: none; padding: 0px;\">";
    }

    private static String blockEnd() {
        return "</blockquote>";
    }

    private static String a(String content, String href) {
        return "<a href=\"" + href + "\">" + content + "</a>";
    }

    private static String bold(String str) {
        return "<b>" + str + "</b>";
    }

    private static String font(String str, String size, String face, String color) {
        return "<font " + (size == null ? "" : "size=\"" + size + "\" ")
                + (face == null ? "" : "face=\"" + face + "\" ") + (color == null ? "" : "color=\"" + color + "\" ")
                + ">" + str + "</font>";
    }
}