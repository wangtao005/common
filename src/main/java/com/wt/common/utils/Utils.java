package com.wt.common.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class Utils {
    /**
     * 获取ip @param request @return
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 获取ip地址对应实际地址
     * @param content 请求的参数   格式为：name=xxx&pwd=xxx <p> 服务器端请求编码。如GBK,UTF-8等
     * @return
     */
    public static String getAddresses(String ip) {
        try {/* 这里调用pconline的接口*/
            String urlStr = "http://ip.taobao.com/outGetIpInfo";
            String content = "ip=" + ip + "&accessKey=alibaba-inc";/* 从http://whois.pconline.com.cn取得IP所在的省市区信息*/
            String returnStr = getResult(urlStr, content, "UTF-8");
            if (returnStr != null) {/* 处理返回的省市区信息*/
                System.out.println("IP=====" + returnStr);
                String[] temp = returnStr.split(",");
                if (temp.length < 3) {
                    return "0";
                }
                JSONObject jsonObject = JSON.parseObject(returnStr);
                JSONObject data = (JSONObject) jsonObject.get("data");
                String country = data.get("country") != null ? data.get("country").toString() : "";
                String region = data.get("region") != null ? data.get("region").toString() : "";
                String city = data.get("city") != null ? data.get("city").toString() : "";
                String isp = data.get("isp") != null ? data.get("isp").toString() : "";
                String strRentun = country + " " + region + " " + city + "(" + isp + ")";
                System.out.println(strRentun);
                return strRentun;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * @param urlStr 请求的地址 @param content  请求的参数 格式为：name=xxx&pwd=xxx @param encoding 服务器端请求编码。如GBK,UTF-8等 @return
     */
    private static String getResult(String urlStr, String content, String encoding) {
        URL url = null;
        HttpURLConnection connection = null;
        try {
            url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();  /* 新建连接实例*/
            connection.setConnectTimeout(2000);                     /* 设置连接超时时间，单位毫秒*/
            connection.setReadTimeout(2000);                        /* 设置读取数据超时时间，单位毫秒*/
            connection.setDoOutput(true);                           /* 是否打开输出流 true|false*/
            connection.setDoInput(true);                            /* 是否打开输入流true|false*/
            connection.setRequestMethod("POST");                    /* 提交方法POST|GET*/
            connection.setUseCaches(false);                         /* 是否缓存true|false                    connection.connect();                                   // 打开连接端口*/
            DataOutputStream out = new DataOutputStream(connection.getOutputStream());/* 打开输出流往对端服务器写数据*/
            out.writeBytes(content);                                /* 写数据,也就是提交你的表单 name=xxx&pwd=xxx*/
            out.flush();                                            /* 刷新*/
            out.close();                                            /* 关闭输出流*/
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));/* 往对端写完数据对端服务器返回数据 ,以BufferedReader流来读取*/
            StringBuffer buffer = new StringBuffer();
            String line = "";
            while ((line = reader.readLine()) != null) buffer.append(line);
            reader.close();
            return buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();                            /* 关闭连接*/
            }
        }
        return null;
    }

    /**
     * 获取请求来自
     * android : 所有android设备
     * mac os : iphone ipad
     * windows phone:Nokia等windows系统的手机
     */
    public static String mobileOrPC(HttpServletRequest request) {
        String requestHeader = request.getHeader("User-Agent");
        if (requestHeader == null) {
            return "";
        }
        requestHeader = requestHeader.toLowerCase();
        String str = "";
        if (requestHeader.indexOf("mac os") > 0) {
            str = str + "苹果";
        } else if (requestHeader.indexOf("android") > 0) {
            str = str + "安卓";
        } else if (requestHeader.indexOf("windows") > 0) {
            str = str + "微软";
        }
        if(requestHeader.indexOf("android") < 0){
            if (requestHeader.indexOf("mobile") > 0) {/*移动端*/
                str = str +"移动";
            } else {
                str = str +"PC";
            }
        }
        if(requestHeader.indexOf("mac os") < 0) {
            if (requestHeader.indexOf("wechat") > 0) {
                str = str + "微信";
            } else {
                str = str + "浏览器";
            }
        }

        return str + "端";
    }

    /**
     * 设置文件名称编码
     * @param request
     * @param fileName
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String setFileDownloadHeader(HttpServletRequest request, String fileName) throws UnsupportedEncodingException {
        final String agent = request.getHeader("USER-AGENT");
        String filename = fileName;
        if (agent.contains("MSIE")) {
            // IE浏览器
            filename = URLEncoder.encode(filename, "utf-8");
            filename = filename.replace("+", " ");
        } else if (agent.contains("Firefox")) {
            // 火狐浏览器
            filename = new String(fileName.getBytes(), "ISO8859-1");
        } else if (agent.contains("Chrome")) {
            // google浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        } else {
            // 其它浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        }
        return filename;
    }

}
