package com.telecom.tools;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

public class XMLTools {

    /***
     * 根据项目名获取项目所在的view名
     *
     * @param projectName
     * @return
     */
    public static String getProjectViewByProjectName(String projectName) {
        Document doc = null;
        try {
            doc = new SAXReader().read("/usr/share/tomcat6/.hudson/config.xml");
            Element rootEle = doc.getRootElement();
            Element viewEle = rootEle.element("views");
            Iterator<Element> listViewIt = viewEle.elementIterator("listView");
            while (listViewIt.hasNext()) {
                Element listViewEle = listViewIt.next();
                String viewName = listViewEle.elementText("name");
                Iterator<Element> projectNamesIt = listViewEle.element("jobNames").elementIterator("string");
                while (projectNamesIt.hasNext()) {
                    if (projectName.equals(projectNamesIt.next().getStringValue()))
                        return viewName;
                }
            }
        } catch (DocumentException e) {
            System.out.println("preAntInitTool --> XMLTools --> getProjectViewByProjectName() : DocumentException");
        } catch (Exception e) {
            System.out.println("preAntInitTool --> XMLTools --> getProjectViewByProjectName() : Exception");
        } finally {
            doc = null;
        }
        return null;
    }

    public static String[] getRecipients() {
        try {
            File file = new File("").getAbsoluteFile().getParentFile().getParentFile();
            Document doc = new SAXReader().read(new File(file, "config.xml").getAbsolutePath());
            Node n = doc.selectSingleNode("//recipients");
            return n.getText().split("\\s");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getVerCode4(String verCode4) {
        String verCode4Temp = "";
        for (int i = 0; i < verCode4.length(); i++) {
            if ((verCode4.charAt(i) + "").matches("^[0-9]{1}$")) {
                verCode4Temp += verCode4.charAt(i);
            } else
                break;
        }
        return verCode4Temp;
    }

    public static void main(String[] args) {
        System.out.println(getVerCode4(""));
        System.out.println(getVerCode4("123"));
        System.out.println(getVerCode4("12d3"));
        System.out.println(getVerCode4("123d"));
        System.out.println(getVerCode4("d123"));
        // System.out.println(getProjectViewByProjectName("OTT_TV_LTYX"));
    }
}
