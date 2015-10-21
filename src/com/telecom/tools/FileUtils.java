package com.telecom.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;

public class FileUtils {
    /**
     * versionName 最后一位
     */
    private static final String KEY_VERSION_LNAME = "lVerName";
    /**
     * versionName 前三位
     */
    private static final String KEY_VERSION_HNAME = "hVerName";
    /**
     * versionCode
     */
    private static final String KEY_VERSION_CODE = "verCode";
    /**
     * 构建状态
     */
    private static final String KEY_BUILD_STATU = "buildStatu";
    /**
     * svn release code
     */
    private static final String KEY_RELEASE_CODE = "releaseCode";

    /**
     * 读取文件的内容
     *
     * @param fileName
     * @return 字符串形式
     * @author wushuang
     */
    public static String readFile(String fileName) {
        StringBuilder res = new StringBuilder();
        BufferedReader bReader = null;
        try {
            bReader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
            String tempStr = null;
            while ((tempStr = bReader.readLine()) != null) {
                res.append(tempStr + "\n");
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return res.toString();
    }

    /**
     * 删除原文件，保存String到新文件，UTF-8
     *
     * @param pathname
     * @param content
     * @author wushuang
     */
    public static boolean saveFile(String pathname, String content) {
        boolean flag = false;
        File oldFile = new File(pathname);
        if (oldFile.exists()) {
            // 存在
            // 删除原来的
            oldFile.delete();
            // 保存新的
            if (saveStringToFile(pathname, content)) {
                // 保存成功
                flag = true;
            } else {
                // 保存失败
                flag = false;
            }
        }
        return flag;
    }

    /**
     * 保存字符串成文件
     *
     * @param pathname
     * @param content
     * @return
     * @author wushuang
     */
    public static boolean saveStringToFile(String pathname, String content) {
        boolean flag = false;
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pathname), "UTF-8"));
            bufferedWriter.write(content);
            bufferedWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        } finally {
            try {
                bufferedWriter.flush();
                bufferedWriter.close();
                flag = true;
            } catch (IOException e) {
                e.printStackTrace();
                flag = false;
            }
        }
        return flag;
    }

    /**
     * 判断文件夹是否存在
     *
     * @param pathname
     * @return
     * @author wushuang
     */
    public static boolean isFileDirectoryExist(String pathname) {
        try {
            File file = new File(pathname);
            if (file.exists() && file.isDirectory()) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }

    }

    /**
     * 更改构建配置文件信息
     *
     * @param buildStatu           项目构成是成功：true-成功，false-失败
     * @param isVerGowthAotu       版本号是否自增
     * @param defaultConfgFile     项目默认配置文件
     * @param userDefVerConfigFile 用户自定义配置文件
     * @param releaseCode          svn realse code
     */
    public static void changeVerConfigFile(boolean buildStatu, boolean isVerGowthAotu, File defaultConfgFile,
                                           File userDefVerConfigFile, File buildStatuFile, int releaseCode, File outputDirFile) {
        System.out.println("FileUtils --> changeVerConfigFile()");
        Properties psOut = new Properties();
        if (userDefVerConfigFile.exists()) {
            String verNameL = "";
            String verNameH = "";
            String verCode = "";
            FileInputStream fis = null;
            try {
                // 读取配置文件的信息
                fis = new FileInputStream(userDefVerConfigFile);
                Properties ps = new Properties();
                ps.loadFromXML(fis);
                verNameH = ps.getProperty(KEY_VERSION_HNAME, "1.0");
                verNameL = ps.getProperty(KEY_VERSION_LNAME, "0");
                verCode = ps.getProperty(KEY_VERSION_CODE, "1");
                fis.close();
                fis = null;
                ps = null;
            } catch (IOException e) {
                System.out.println(userDefVerConfigFile.getAbsolutePath() + " not found!");
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                    fis = null;
                }
            }

            if (isVerGowthAotu && buildStatu) {
                try {
                    psOut.setProperty(KEY_VERSION_CODE, String.valueOf(Long.parseLong(verCode) + 1));
                    psOut.setProperty(KEY_VERSION_HNAME, verNameH);
                    psOut.setProperty(KEY_VERSION_LNAME, String.valueOf(Integer.parseInt(verNameL) + 1));
                } catch (Exception e) {
                }
            } else {
                psOut.setProperty(KEY_VERSION_CODE, verCode);
                psOut.setProperty(KEY_VERSION_HNAME, verNameH);
                psOut.setProperty(KEY_VERSION_LNAME, verNameL);
            }
        }

        // 保存本次编译前的verCongfig.xml状态
        if (buildStatu && outputDirFile != null) {
            String tmpVerCode = psOut.getProperty(KEY_VERSION_CODE);
            String tmpVerNameH = psOut.getProperty(KEY_VERSION_HNAME);
            String tmpVerNameL = psOut.getProperty(KEY_VERSION_LNAME);
            String tmp = "";
            for (int i = 0; i < tmpVerNameL.length(); i++) {
                if ((tmpVerNameL.charAt(i) + "").matches("^[0-9]{1}$")) {
                    tmp += tmpVerNameL.charAt(i);
                } else
                    break;
            }
            tmpVerNameL = tmp;
            if (isVerGowthAotu) {
                try {
                    tmpVerCode = String.valueOf(Long.parseLong(tmpVerCode) + 1);
                    tmpVerNameL = String.valueOf(Integer.parseInt(tmpVerNameL) + 1);
                } catch (Exception e) {
                }
            }
            Properties tmpProp = new Properties();
            tmpProp.setProperty(KEY_VERSION_CODE, tmpVerCode);
            tmpProp.setProperty(KEY_VERSION_HNAME, tmpVerNameH);
            tmpProp.setProperty(KEY_VERSION_LNAME, tmpVerNameL);
            PrintStream fos = null;
            try {
                fos = new PrintStream(outputDirFile);
                tmpProp.storeToXML(fos, "verConfig");
                fos.flush();
            } catch (IOException e) {
                System.out.println("save userDefVerConfigFile : " + e.getMessage());
            } finally {
                if (null != fos) {
                    fos.close();
                    fos = null;
                }
            }
        }

        // 更新用户自定义配置文件的信息: 改配置文件存放版本信息
        PrintStream pStream = null;
        if (null != userDefVerConfigFile) {
            try {
                pStream = new PrintStream(userDefVerConfigFile);
                psOut.storeToXML(pStream, "verConfig");
                pStream.flush();
            } catch (IOException e) {
                System.out.println("save userDefVerConfigFile : " + e.getMessage());
            } finally {
                if (null != pStream) {
                    pStream.close();
                    pStream = null;
                }
            }
        }

        // 更新项目默认配置文件：该配置文件存放版本信息
        if (null != defaultConfgFile) {
            try {
                pStream = new PrintStream(defaultConfgFile);
                psOut.storeToXML(pStream, "verConfig");
                pStream.flush();
            } catch (IOException e) {
                System.out.println("save defaultConfgFile : " + e.getMessage());
            } finally {
                if (null != pStream) {
                    pStream.close();
                    pStream = null;
                }
            }
        }

        // 更新构建状态文件：该文件保存构建状态，svn release code
        if (null != buildStatuFile) {
            psOut.setProperty(KEY_BUILD_STATU, buildStatu ? "true" : "false");
            // 构建成功，更新releaseCode
            if (buildStatu)
                psOut.setProperty(KEY_RELEASE_CODE, releaseCode + "");
            else {
                String lastReleaseCode = "0";
                FileInputStream fis = null;
                try {
                    // 读取配置文件的信息
                    fis = new FileInputStream(buildStatuFile);
                    Properties ps = new Properties();
                    ps.loadFromXML(fis);
                    lastReleaseCode = ps.getProperty(KEY_RELEASE_CODE, "0");
                    fis.close();
                    fis = null;
                    ps = null;
                } catch (IOException e) {
                    System.out.println(buildStatuFile.getAbsolutePath() + " not found!");
                } finally {
                    if (null != fis) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                        }
                        fis = null;
                    }
                }
                psOut.setProperty(KEY_RELEASE_CODE, lastReleaseCode + "");
            }
            try {
                pStream = new PrintStream(buildStatuFile);
                psOut.storeToXML(pStream, "buildStatu");
                pStream.flush();
            } catch (IOException e) {
                System.out.println("save buildStatuFile : " + e.getMessage());
            } finally {
                if (null != pStream) {
                    pStream.close();
                    pStream = null;
                }
            }
        }

        if (null != psOut) {
            psOut.clear();
            psOut = null;
        }
    }

    /**
     * 获取上次构建svn release code
     *
     * @param verConfigFile
     * @return 0代表未构建
     */
    public static int getLastBuildReleaseCode(File verConfigFile) {
        int code = 0;
        if (null != verConfigFile && verConfigFile.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(verConfigFile);
                Properties ps = new Properties();
                ps.loadFromXML(fis);
                code = Integer.parseInt(ps.getProperty(KEY_RELEASE_CODE, "0"));
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                    }
                    fis = null;
                }
            }
        }
        return code;
    }

    /**
     * 获取最后修改的文件
     *
     * @param files
     * @return
     */
    public static File getLastModifyFile(List<File> files) {
        if (null == files)
            return null;
        File lastModifyDir = null;
        for (int i = 0; i < files.size(); i++) {
            if (lastModifyDir != null && (lastModifyDir.lastModified() > files.get(i).lastModified()))
                continue;
            lastModifyDir = files.get(i);
        }
        return lastModifyDir;
    }

    /**
     * 获取realse code
     *
     * @param filePath
     * @return
     */
    public static int getReleaseCode(String filePath, String svnPath) {
        return getReleaseCode(new File(filePath), svnPath);
    }

    /**
     * 获取realse code
     *
     * @param file
     * @return
     */
    public static int getReleaseCode(File file, String svnPath) {
        BufferedReader buffReader = null;
        int releaseCode = 0;
        try {
            buffReader = new BufferedReader(new FileReader(file));

            String releaseStr;
            while ((releaseStr = buffReader.readLine()) != null) {
                if (releaseStr.startsWith(svnPath)) {
                    releaseCode = Integer.parseInt(releaseStr.substring(releaseStr.lastIndexOf("/") + 1));
                    break;
                }
            }
        } catch (Exception e) {
        } finally {
            try {
                buffReader.close();
            } catch (IOException e) {
            }
            buffReader = null;
        }
        return releaseCode;
    }
}
