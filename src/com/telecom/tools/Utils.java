package com.telecom.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utils {

    /**
     * 执行单挑命令
     *
     * @param command
     */
    public static void execCommand(String command) {
        System.out.println("Exec command : " + command);
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            outLog(process.getInputStream(), 0);
            outLog(process.getErrorStream(), 1);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            if (null != process)
                process.destroy();
        }
    }

    public static String exeCommandWithLog(String command) {
        System.out.println("Exec command : " + command);
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            return outCmdLog(process.getInputStream());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            if (null != process)
                process.destroy();
        }
        return "";
    }

    /**
     * 打印log
     *
     * @param is
     * @param 1  错误日志打印； 0正常日志打印
     */
    public static void outLog(InputStream is, int printFlag) {
        if (null == is)
            return;
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new InputStreamReader(is, "utf-8"));
            String line = "";
            while (null != (line = rd.readLine())) {
                System.out.println(line);
            }
        } catch (Exception e1) {
        } finally {
            try {
                if (null != is)
                    is.close();
                if (null != rd)
                    rd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String outCmdLog(InputStream is) {
        if (null == is)
            return "";
        BufferedReader rd = null;
        StringBuffer sb = new StringBuffer();
        try {
            rd = new BufferedReader(new InputStreamReader(is, "utf-8"));
            String line = "";
            while (null != (line = rd.readLine())) {
                sb.append(line + "\n");
            }
        } catch (Exception e1) {
        } finally {
            try {
                if (null != is)
                    is.close();
                if (null != rd)
                    rd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

}
