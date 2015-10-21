package com.telecom.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import com.telecom.tools.MailUtil.SvnLogData;

/**
 * 项目构建成功后的处理<br>
 * 1 log文件输出<br>
 * 2 apk 文件、混淆mapping等文件输出到releaseApksPath目录<br>
 * 3 相关数据提取<br>
 *
 * @author 张蛟
 */
public class AfterProBuildHandleTool {
    // hudson 构建 workspace 目录
    private String currPath = "";
    // 自定义版本号路径
    private String verConfigPath = "";
    // 版本号是否自增
    private boolean isGouthAuto = true;
    private boolean isLogOpended = false;

    private String versionName = "";
    private String versionCode = "";
    private String packageName = "";
    private String projectName = "";
    private String date = "";
    private ArrayList<String> newAPKNames = new ArrayList<String>();
    private ArrayList<String> apkFileMD5s = new ArrayList<String>();

    // hudson当前构建输出信息目录
    private File currBuildDir = null;
    // apk文件及相关信息的输出目录
    private File outputDir = null;
    // 项目默认配置文件
    private File proDefaultConfgFile = null;
    // 自定义项目配置文件
    private File userDefVerConfigFile = null;
    // 项目构建状态文件
    private File proBuildStatuFile = null;

    // 当前构建代码svn release code
    private int releaseCode = 0;
    // 相关文件输出目录
    private String release_dir;
    // 项目相关版本配置信息
    private final String VER_CONFIG_NAME = "verConfig.xml";
    private final String LAST_VER_CONFIG_NAME = "last_verConfig.xml";
    private final String CUSTOM_MAIL_CONTENT = "custom_mail_content.html";

    public static void main(String[] args) throws Exception {

        System.out.println("=================================================");
        System.out.println("=======> Begin AfterProBuildHandleTool <=========");
        System.out.println("=================================================");

        AfterProBuildHandleTool tool = new AfterProBuildHandleTool();

        boolean initFlag = tool.initArgs(args, tool);

        if (!initFlag) {
            if (!tool.isLogOpended) {
                System.out.println("init Error !!!!");
            }
        } else {
            tool.startPackage();
        }

        System.out.println("===============================================");
        System.out.println("=======> End AfterProBuildHandleTool <=========");
        System.out.println("===============================================");
    }

    private AfterProBuildHandleTool() {
        release_dir = System.getenv("releaseApksPath");
        if (release_dir == null || "".equals(release_dir))
            throw new RuntimeException("releaseApksPath not set");
        if (!release_dir.endsWith("/"))
            release_dir += "/";
        System.out.println("releaseApksPath=" + release_dir);
    }

    /**
     * 参数初始化
     *
     * @param args
     * @param tool
     * @throws Exception
     */
    private boolean initArgs(String[] args, AfterProBuildHandleTool tool) throws Exception {
        System.out.println("-------------> initArgs() <--------------");
        System.out.println("args : " + Arrays.toString(args));
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            if (arg.equalsIgnoreCase("-path")) {
                if (i == args.length - 1) {
                    System.out.println("参数path没有值！");
                    throw new Exception();
                }
                i++;
                tool.currPath = args[i];
                System.out.println("currPath : " + tool.currPath);
            } else if (arg.equalsIgnoreCase("-i")) {
                tool.isGouthAuto = false;
            } else if (arg.equalsIgnoreCase("-log")) {
                tool.isLogOpended = true;
            } else if (arg.equalsIgnoreCase("-verPath")) {
                if (i == args.length - 1) {
                    System.out.println("参数verPath没有值！");
                    return false;
                }
                i++;
                tool.verConfigPath = args[i];
                System.out.println("verConfigPath : " + tool.verConfigPath);
            } else if (arg.equalsIgnoreCase("-pName")) {
                if (i == args.length - 1) {
                    System.out.println("参数verPath没有值！");
                    return false;
                }
                i++;
                projectName = args[i];
                System.out.println("projectName : " + projectName);
            } else if (arg.equalsIgnoreCase("--help")) {
                System.out.println("-------------------------------");
                System.out.println("-path  项目目录：window下为：%cd%, linux下为： ${pwd}");
                System.out.println("-app  日志中包含app相关信息，如appid,appkey等");
                System.out.println("-i  版本号不自增");
                System.out.println("-log  打开日志");
                System.out.println("-verPath path  项目配置文件, eg: -verPath /home/config_5.0.3.xml");
                System.out
                        .println("-fix verNameFix  version_name标识信息, eg: -fix _anzhi ==> version_name: 4.0.5.0_anzhi");
                System.out.println("-------------------------------");
                return false;
            }
        }
        if ("".equals(tool.currPath) || null == tool.currPath) {
            System.out.println("缺少参数-path");
            throw new Exception();
        }

        return true;
    }

    public void startPackage() throws Exception {
        System.out.println("-------------> startPackage() <--------------");
        initParam();
        System.out.println(">>> IsLogOpened : " + isLogOpended);
        File[] newApks = getApkFile();

        if (null == newApks || newApks.length == 0) {
            System.out.println("Do not find the apk file !!!");
            // 修改配置文件状态
            FileUtils.changeVerConfigFile(false, isGouthAuto, proDefaultConfgFile, userDefVerConfigFile,
                    proBuildStatuFile, 0, null);
            return;
        }

        readManifestData();

        // 输出目录
        String outputDirStr = release_dir + projectName + "/" + date + "_" + versionName;
        System.out.println("Output directory path : " + outputDirStr);
        // ftp 目录
        File ftpOutputDir = getFtpDir();
        System.out.println("ftp path : " + ftpOutputDir.getAbsolutePath());
        for (File newApk : newApks) {

            newAPKNames.add(newApk.getName());

            // 获取 apk 的 MD5 校验码
            apkFileMD5s.add(new FileMD5().createFileMD5(newApk));
            System.out.println(newApk.getName() + " : " + apkFileMD5s.get(apkFileMD5s.size() - 1));

            outputDir = new File(outputDirStr);
            outputDir.mkdirs();

            // 將 apk 文件移动到发布目录下
            Utils.execCommand("cp " + newApk.getAbsoluteFile() + " " + outputDir.getAbsoluteFile());

            // 將 apk 文件移动到 ftp 目录下
            Utils.execCommand("cp " + newApk.getAbsoluteFile() + " " + ftpOutputDir.getAbsolutePath());

        }

        outZipProguard();
        appendApkLog();

        outputLog();

        File outputDirVerConfigFile = new File(outputDirStr + "/verConfig_" + versionName + "_" + versionCode + ".xml");
        // 修改配置文件状态
        FileUtils.changeVerConfigFile(true, isGouthAuto, proDefaultConfgFile, userDefVerConfigFile, proBuildStatuFile,
                releaseCode, outputDirVerConfigFile);

        // 保存本次编译配置信息
        Utils.execCommand("cp " + proDefaultConfgFile.getAbsolutePath() + " " + release_dir + projectName + "/"
                + LAST_VER_CONFIG_NAME);
    }

    private void initParam() {
        System.out.println("-------------> initParam() <--------------");
        String proNameTemp = currPath.substring(0, currPath.lastIndexOf("/"));
        // projectName = proNameTemp.substring(proNameTemp.lastIndexOf("/") + 1, proNameTemp.length());
        System.out.println("Project name : " + projectName);
        date = new SimpleDateFormat("yyyyMMdd").format(new Date());

        String projectView = XMLTools.getProjectViewByProjectName(projectName);
        release_dir += (null == projectView ? "" : (projectView + "/"));
        String proVerConfigPath = release_dir + projectName + "/" + VER_CONFIG_NAME;
        proBuildStatuFile = new File(release_dir + projectName + "/buildStatu.xml");
        proDefaultConfgFile = new File(proVerConfigPath);
        if ("".equals(verConfigPath) || proVerConfigPath.equalsIgnoreCase(verConfigPath))
            userDefVerConfigFile = proDefaultConfgFile;
        else
            userDefVerConfigFile = new File(verConfigPath);
        System.out.println("Project version config path: : " + proDefaultConfgFile.getAbsolutePath());
        System.out.println("User-define version config path: : " + userDefVerConfigFile.getAbsolutePath());
    }

    /**
     * 输出APK日志
     */
    private void appendApkLog() {
        System.out.println("-------------> appendApkLog() <--------------");
        FileWriter fWriter = null;
        BufferedWriter bWriter = null;
        try {
            fWriter = new FileWriter(new File(release_dir + projectName + "/" + "ApkPackageLog.txt"), true);
            bWriter = new BufferedWriter(fWriter);
            for (String newAPKName : newAPKNames) {
                bWriter.append(newAPKName + "  \t  " + versionCode);
                bWriter.append("\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != bWriter) {
                try {
                    bWriter.flush();
                    bWriter.close();
                    bWriter = null;
                    fWriter.close();
                    fWriter = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 获取 ant build 的 apk 文件
     */
    private File[] getApkFile() {
        File[] fileList = new File(currPath, "release").listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".apk");
            }
        });
        return fileList;
    }

    /**
     * 获取versionName
     */
    private void readManifestData() throws Exception {
        System.out.println("-------------> readManifestData() <--------------");
        File file = new File(currPath + "/AndroidManifest.xml");
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            org.w3c.dom.Document document = builder.parse(file);
            org.w3c.dom.Element rootElement = document.getDocumentElement();
            versionName = rootElement.getAttribute("android:versionName");
            versionName = versionName.replace("_log", "");
            versionCode = rootElement.getAttribute("android:versionCode");
            packageName = rootElement.getAttribute("package");
            System.out.println("VersionName : " + versionName);
            System.out.println("VersionCode : " + versionCode);
            System.out.println("packageName : " + packageName);
        } catch (ParserConfigurationException e) {
            throw new Exception();
        } catch (SAXException e) {
            throw new Exception();
        } catch (IOException e) {
            throw new Exception();
        }
    }

    /**
     * 组装输出日志
     */
    private void outputLog() throws Exception {
        SvnLogData data = new SvnLogData();

        System.out.println("-------------> outputLog() <--------------");

        for (int i = 0; i < newAPKNames.size(); i++) {
            String newAPKName = newAPKNames.get(i);
            String apkFileMD5 = apkFileMD5s.get(i);
            data.apkNames.add(newAPKName);
            data.apkMd5s.add(apkFileMD5);
        }

        data.versionCode = versionCode;
        data.versionName = versionName;
        data.time = new SimpleDateFormat("yyyy-MM-dd HH:MM:ss").format(new Date());
        data.svnReleaseCodes = FileUtils.readFile(getSVNReverPath());
        getSVNLog(data);

        ArrayList<String> apkPaths = new ArrayList<String>();
        for (String name : newAPKNames) {
            apkPaths.add(outputDir.getAbsolutePath() + "/" + name);
        }
        // 发送邮件
        String mailContent = MailUtil.sendLogEmail(projectName, versionName, date, data, packageName,
                outputDir.getAbsolutePath(), apkPaths, currPath + "/" + CUSTOM_MAIL_CONTENT);
        System.out.println("mailContent：\n" + mailContent);
        if (mailContent != null)
            try {
                File mailFile = new File(outputDir.getAbsoluteFile() + "/" + date + "_" + versionName + "_mail.html");
                mailFile.delete();
                FileUtils.saveStringToFile(mailFile.getAbsolutePath(), mailContent);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    /**
     * 获取ftp目录
     */
    private File getFtpDir() {
        String ftpDir = System.getenv("ftp_physical_address");
        String ftpDirStr = ftpDir + "/" + projectName + "/" + date + "_" + versionName;
        System.out.println("ftp directory path : " + ftpDirStr);
        File ftpOutputDir = new File(ftpDirStr);
        ftpOutputDir.mkdirs();
        return ftpOutputDir;
    }

    /**
     * 获取build的svn release文件
     */
    private String getSVNReverPath() {
        System.out.println("-------------> getSVNReverPath() <--------------");
        String buildPath = new File(currPath).getParentFile().getParentFile().getAbsolutePath() + "/builds";
        File[] filesT = new File(buildPath).listFiles();
        if (null == filesT) {
            System.out.println("Error: No build directory!!!");
            return "";
        }
        List<File> files = Arrays.asList(filesT);
        currBuildDir = FileUtils.getLastModifyFile(files);
        System.out.println("last build path :" + currBuildDir.getAbsolutePath());
        return currBuildDir.getAbsolutePath() + "/revision.txt";
    }

    /**
     * 获取SVN日志 </br> 该方法调用之前须调用方法{@link #getSVNReverPath()} 获取lastBuildDir&lastButoneBuildDir（最后一次build的目录）
     */
    private void getSVNLog(SvnLogData data) {
        System.out.println("-------------> getSVNLog() <--------------");
        StringBuffer sb = new StringBuffer();
        if (null == currBuildDir) {
            System.out.println("Error: No build directory!!!");
            return;
        }
        File workspaceFile = new File(currPath).getAbsoluteFile().getParentFile().getParentFile();
        File jobConfigFile = new File(workspaceFile, "config.xml");

        Document doc;
        String svnPath = null;
        try {
            doc = new SAXReader().read(jobConfigFile);

            org.dom4j.Element element = (Element) doc.selectSingleNode("//locations");
            List<Element> list = element.elements();
            String dirName = new File(currPath).getAbsoluteFile().getName();

            if (list != null) {
                for (Element node : list) {
                    if (dirName.equals(node.selectSingleNode("local").getText())) {
                        svnPath = node.selectSingleNode("remote").getText();
                        break;
                    }
                }
            }
        } catch (DocumentException e) {
            return;
        }

        if (svnPath == null) {
            return;
        }

        releaseCode = FileUtils.getReleaseCode(currBuildDir.getAbsolutePath() + "/revision.txt", svnPath);
        int lastReleaseCode = FileUtils.getLastBuildReleaseCode(proBuildStatuFile);

        System.out.println("current release code : " + releaseCode);
        System.out.println("last release code : " + lastReleaseCode);

        data.lastCode = String.valueOf(lastReleaseCode);
        data.nowCode = String.valueOf(releaseCode);

        String codeRange = "";
        if (0 == lastReleaseCode) {
            data.svnInfo = "本次为第一次构建！！！\n当前 releaseCode:" + releaseCode + "\n";
            data.changeType = SvnLogData.NEW_RELEASE;
            codeRange += releaseCode;
        } else if (releaseCode == lastReleaseCode) {
            data.svnInfo = "本次构建和上次构建没有任何修改！！！\n当前 releaseCode:" + releaseCode + "\n";
            data.changeType = SvnLogData.NO_CHANGE;
            codeRange += releaseCode;
        } else {
            data.svnInfo = "上次构建 releaseCode : " + lastReleaseCode + " , 本次构建 releaseCode :" + releaseCode + "\n";
            data.changeType = SvnLogData.CHANGED;
            codeRange += lastReleaseCode + ":" + releaseCode;
        }
        // 由于 Hudson SVN 插件版本与系统中 SVN 的版本不兼容，需要调用 SVN upgrade 命令升级工作空间。
        Utils.exeCommandWithLog("svn upgrade");
        String log = Utils.exeCommandWithLog("svn log -r " + codeRange
                + " --username chenshijun --password kSDFLKDFr9PGN");
        data.svnLog = log;
        System.out.println("svn log :\n" + log);
    }

    /**
     * 将 Proguard 下的文件输出
     */
    private void outZipProguard() {
        File proguard = new File(outputDir.getAbsoluteFile() + "/proguard_" + versionName + "_" + versionCode + "/");
        proguard.mkdir();
        File[] files = new File(currPath + "/build/outputs/mapping/release").listFiles();
        if (files == null)
            return;
        for (File fileTemp : files) {
            System.out.println("outZipProguard fileName = " + fileTemp.getAbsoluteFile());
            if (fileTemp.isFile()) {
                Utils.execCommand("mv " + fileTemp.getAbsoluteFile() + " " + proguard.getAbsoluteFile());
            }
        }
    }
}
