package com.wt.common.utils;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wt.common.utils.Utils.setFileDownloadHeader;

/**
 * 关于文件处理的类
 */
public class FileUtils {

    private static String fileUploadPath;

    @Value("${file_upload_path}")
    public  void setFileUploadPath(String fileUploadPath) {
        FileUtils.fileUploadPath = fileUploadPath;
    }

    /**
     * 实现文件上传
     * 需要创建application.properties文件,添加file_upload_path=D:/easyexp/upload/上传文件保存位置
     */
    public static Map<String, Object> upload(MultipartFile file, HttpServletRequest request) {
        String fileName = file.getOriginalFilename();
        int lastIndexOf = fileName.lastIndexOf(".");
        String fileType = fileName.substring(lastIndexOf + 1);
        double fileSize = Math.round(file.getSize() / 1024);// kb
        Map<String, Object> map = new HashMap<String, Object>();
        if (file.isEmpty()) {
            map.put("status", false);
            map.put("msg", "上传文件为空");
            map.put("path", "");
            map.put("fileType", "");
            map.put("fileSize", "");
            map.put("fileName", "");
            return map;
        }
        String separator = File.separator;
        Calendar cal = Calendar.getInstance();
        String dstr = "/" + cal.get(Calendar.YEAR) + (cal.get(Calendar.MONTH) + 1) + cal.get(Calendar.DATE);
        File dir = new File(fileUploadPath + dstr);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        fileName = fileName.substring(fileName.lastIndexOf(separator) + 1);
//        if (fileName.lastIndexOf(".") == -1) {
//            map.put("status", false);
//            map.put("msg", "上传文件为空");
//            map.put("path", "");
//            map.put("fileType", "");
//            map.put("fileSize", "");
//            map.put("fileName", "");
//            return map;
//        }
        String realPath2 = fileUploadPath + dstr + File.separator + fileName;
        File dest = new File(realPath2);
        File parent = dest.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        try {
            try (RandomAccessFile randomFile = new RandomAccessFile(realPath2, "rw")) {// 防止断点续传
                randomFile.seek(randomFile.length());
                randomFile.write(file.getBytes());
                randomFile.close();
            }
            map.put("status", true);
            map.put("msg", "上传文件成功");
            map.put("fileType", fileType);
            map.put("fileSize", fileSize);
            map.put("path", dstr + "/" + fileName);
            map.put("fileName", fileName);
            return map;
        } catch (IOException e) {
            map.put("status", false);
            map.put("msg", e.getMessage());
            map.put("path", "");
            map.put("fileType", "");
            map.put("fileSize", "");
            map.put("fileName", fileName);
        }
        throw new RuntimeException("upload error");
    }

    /**
     * 下载文件
     * @param request
     * @param response
     * @param name
     * @param url
     * @throws IOException
     */
    public void downloadByUrl(HttpServletRequest request, HttpServletResponse response, String name, String url) throws IOException {

        url = new String(new Base64().decode(url));
        name = new String(new Base64().decode(name));
        long length = 0L;
        //判断是本地路径还是网上路径
        String regEx ="[a-zA-z]+://[^\\s]*";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(url);
        boolean rs = matcher.matches();
        InputStream inputStream = null;
        if(rs) {
            URL obj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)");
            inputStream = conn.getInputStream();
            length = conn.getContentLengthLong();
        }else {//本地文件
            inputStream = new FileInputStream(url);
            length = inputStream.available();
        }

        // 清空response
        response.reset();
        // 设置response的Header
        response.addHeader("Content-Disposition","attachment;filename=" + setFileDownloadHeader(request, name));
        response.addHeader("Content-Length", "" + length);
        OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
        response.setContentType("application/octet-stream");
        int len = 0;
        byte[] b = new byte[1024*100];
        while((len=inputStream.read(b))!=-1) {
            toClient.write(b,0,len);
        }
        //释放inputstream
        try {inputStream.close();}catch(Exception e) {}
        toClient.flush();
        toClient.close();
    }
}
