package com.telecom.tools;

import java.io.File;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;

public class FileMD5 {
    /**
     * @param file
     * @return
     * @throws Exception
     */
    public String createFileMD5(File file) throws Exception {
        String md5 = "";
        if (file.exists()) {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            FileInputStream in = new FileInputStream(file);
            FileChannel fileChannel = in.getChannel();
            MappedByteBuffer byteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            messageDigest.update(byteBuffer);
            fileChannel.close();
            in.close();
            byte data[] = messageDigest.digest();
            md5 = byteArrayToHexString(data);
        }
        return md5;
    }

    private String byteArrayToHexString(byte[] data) {
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char arr[] = new char[16 * 2];
        int k = 0;
        for (int i = 0; i < 16; i++) {
            byte b = data[i];
            arr[k++] = hexDigits[b >>> 4 & 0xf];
            arr[k++] = hexDigits[b & 0xf];
        }
        return new String(arr);
    }
}
