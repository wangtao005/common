package com.wt.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.wt.common.utils.Utils.setFileDownloadHeader;

public class ExcelFileUtils {

    /**
     *      Test entity = new Test();
     *      List<Object> objectList = ExcelFileUtils.fileUpload(file, entity, "text", "static/xml/textXml.xml");
     *      xml实例如下:
     *      <?xml version="1.0" encoding="utf-8" ?>
     * <root>
     *     <part id="text" entityPath="com.mypoi.poi.entity.Test">
     *         <item id="id" name="id" value="ID" type="String" required="false"></item>
     *         <item id="name" name="name" value="姓名" type="String" required="true"></item>
     *         <item id="sex" name="sex" value="性别" type="String" required="false"></item>
     *         <item id="age" name="age" value="年龄" type="Integer" required="true"></item>
     *         <item id="birthday" name="birthday" value="生日" type="Date" required="false"></item>
     *         <item id="sign" name="sign" value="签名" type="String" required="false"></item>
     *     </part>
     * </root>
     * @param file       上传的excel文件
     * @param entity     需要导入的实体对象
     * @param flag       对应xml配置id
     * @param xmlPath    具体xml位置
     * @return
     */
    public static List<Object> fileUpload(MultipartFile file, Object entity, String flag, String xmlPath) {
        try {
            StringBuffer str = new StringBuffer();
            List<Object> strArr = new ArrayList<>(); /* 获得上传文件的文件名*/
            String fileName = file.getOriginalFilename(); /* 获取文件扩展名*/
            String eName = fileName.substring(fileName.lastIndexOf(".") + 1);
            InputStream inputStream = file.getInputStream();
            Workbook workbook = getWorkbook(inputStream, eName); /* 获取工作薄第一张表*/
            Sheet sheet = workbook.getSheetAt(0); /* 获取名称*/
            String sheetName = sheet.getSheetName().trim(); /* 获取第一行*/
            Row row = sheet.getRow(0); /* 获取有效单元格数*/
            int cellNum = row.getPhysicalNumberOfCells(); /* 表头集合*/
            List<String> headList = new ArrayList<>();
            for (int i = 0; i < cellNum; i++) {
                Cell cell = row.getCell(i);
                String val = cell.getStringCellValue();
                headList.add(val);
            }
            Map<String, Field> map = getObjField(entity, flag, xmlPath);
            Map<String, String> required = getRequired(flag, xmlPath);/*是否是必须*/
            String entiyPath = getEntiyPath(flag, xmlPath);
            int rowNum = sheet.getPhysicalNumberOfRows();/*获取有效行数*/
            if (rowNum == 1) {
                str.append("导入了空的文档,请补全再导入;");
            }
            List<Object> objList = new ArrayList<>();
            for (int i = 1; i < rowNum; i++) {
                row = sheet.getRow(i);
                Object data = Class.forName(entiyPath).newInstance();
                for (int j = 0; j < headList.size(); j++) { /* 解析单元格*/
                    Cell cell = row.getCell(j);
                    Field field = map.get(headList.get(j));
                    if (field == null) {
                        continue;
                    }
                    String name = field.getName();
                    if (cell == null) {
                        String s = required.get(name);
                        if ("true".equals(s)) {
                            str.append("第" + i + "行,<" + headList.get(j) + ">为空,请补全再导入;");
                        }
                        continue;
                    }
                    CellType cellType = cell.getCellType();
                    String cellValue = "";
                    if (cellType == CellType.STRING) {
                        cellValue = cell.getStringCellValue();
                        if (!StringUtils.isNotBlank(cellValue)) {
                            str.append("第" + i + "行,<" + headList.get(j) + ">为空,请补全再导入;");
                            continue;
                        }
                    } else if (cellType == CellType.NUMERIC) {
                        if (DateUtil.isCellDateFormatted(cell)) {/*判断是否是日期*/
                            Date date = cell.getDateCellValue();
                            DateFormat formater = new SimpleDateFormat("yyyy-MM-dd");
                            cellValue = formater.format(date);
                        } else {
                            double numericCellValue = cell.getNumericCellValue();
                            DecimalFormat df = new DecimalFormat("0");
                            cellValue = df.format(numericCellValue);
                        }
                    }
                    if (!StringUtils.isNotBlank(cellValue)) {
                        continue;
                    }
                    field.setAccessible(true);
                    if (field.getGenericType().toString().equals("class java.lang.String")) {/* 如果类型是String*/
                        field.set(data, cellValue);
                    } else if (field.getGenericType().toString().equals("class java.lang.Integer")) {/* 如果类型是Integer*/
                        Integer val = Integer.parseInt(cellValue);
                        field.set(data, val);
                    } else if (field.getGenericType().toString().equals("class java.util.Date")) {
                        try {
                            Date date = DateUtil.parseYYYYMMDDDate(cellValue);
                            field.set(data, date);
                        } catch (Exception e) {
                            str.append("第" + i + "行,<" + headList.get(j) + ">日期转换出错,请按正确格式填写;");
                            e.printStackTrace();
                        }
                    } else if (field.getGenericType().toString().equals("class java.lang.Double")) {
                        double val = Double.parseDouble(cellValue);
                        field.set(data, val);
                    } else if (field.getGenericType().toString().equals("class java.lang.Boolean")) {
                        boolean val = Boolean.parseBoolean(cellValue);
                        field.set(data, val);
                    }
                }
                objList.add(data);
            }
            if (str.length() > 0) {
                strArr.add(str);
                return strArr;
            }
            return objList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /*查询字段是否是必填 false 非必填*/
    public static Map<String, String> getRequired(String flag, String xmlPate) {
        Map<String, List<Map<String, String>>> configurationInfo = Dom4jXml.getConfigurationInfo(flag, xmlPate);
        List<Map<String, String>> data = configurationInfo.get("data");
        Map<String, String> requiredMap = new HashMap<String, String>();
        for (int i = 0; i < data.size(); i++) {
            Map<String, String> stringStringMap = data.get(i);
            String name1 = stringStringMap.get("name");
            String required = stringStringMap.get("Required");
            requiredMap.put(name1, required);
        }
        return requiredMap;
    }

    /*获取实体名称全路径*/
    public static String getEntiyPath(String flag, String xmlPath) {
        Map<String, List<Map<String, String>>> configurationInfo = Dom4jXml.getConfigurationInfo(flag, xmlPath);
        List<Map<String, String>> data = configurationInfo.get("entityPath");
        String entityPath = data.get(0).get("entityPath");
        return entityPath;
    }

    /*获取实体属性*/
    public static List<String> getEntiyAttributes(String flag, String xmlPath) {
        Map<String, List<Map<String, String>>> configurationInfo = Dom4jXml.getConfigurationInfo(flag, xmlPath);
        List<Map<String, String>> data = configurationInfo.get("data");
        List<String> list = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            Map<String, String> stringStringMap = data.get(i);
            String value = stringStringMap.get("value");
            list.add(value);
        }
        return list;
    }

    public static Map<String, Field> getObjField(Object entity, String flag, String xmlPath) {
        Map<String, List<Map<String, String>>> configurationInfo = Dom4jXml.getConfigurationInfo(flag, xmlPath);
        List<Map<String, String>> data = configurationInfo.get("data");
        List<Map<String, String>> message = configurationInfo.get("message");
        String msg = message.get(0).get("message");
        if (StringUtils.isNotBlank(msg)) {
        }
        Map<String, String> fileMap = new HashMap<String, String>();
        for (int i = 0; i < data.size(); i++) {
            Map<String, String> stringStringMap = data.get(i);
            String name1 = stringStringMap.get("name");
            String value = stringStringMap.get("value");
            fileMap.put(name1, value);
        }
        Map<String, Field> map = new HashMap<>();    /* 获取该类所有字段信息*/
        Field[] fields = entity.getClass().getDeclaredFields();
        Set<String> set = fileMap.keySet();
        for (String str : set)
            for (Field field : fields) {
                String name = field.getName();
                if (str.equals(name)) {
                    map.put(fileMap.get(name), field);
                }
            }
        return map;
    }

    /* 根据excel文件格式获知excel版本信息 */
    public static Workbook getWorkbook(InputStream fs, String str) {
        Workbook book = null;
        try {
            if ("xls".equals(str)) {/* 2003*/
                book = new HSSFWorkbook(fs);
            } else {/* 2007*/
                book = new XSSFWorkbook(fs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return book;
    }

    /*构建一个excel表格*/
    public static HSSFWorkbook fileDownloadBook(String excelName, String flag, String xmlPath) {

        List<String> arr = getEntiyAttributes(flag, xmlPath);
        HSSFWorkbook wb = new HSSFWorkbook();/*首先创建字体样式*/
        HSSFFont font = wb.createFont();/*创建字体样式*/
        font.setFontName("宋体");/*使用宋体*/
        font.setFontHeightInPoints((short) 10);/*字体大小*/
        font.setBold(true);/* 加粗*/
        HSSFCellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);/*居中*/
        style.setVerticalAlignment(VerticalAlignment.CENTER);/*垂直居中*/
        style.setFont(font);/*设置字体样式*/
        style.setFillForegroundColor((short) 41);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);/*上边框*/
        style.setBorderBottom(BorderStyle.THIN);/*下边框*/
        style.setBorderLeft(BorderStyle.THIN);/*左边框*/
        style.setBorderRight(BorderStyle.THIN);/*右边框*/
        HSSFSheet sheet = wb.createSheet(excelName);
        HSSFRow row = sheet.createRow(0);
        row.setHeight((short) (24 * 20));
        for (int i = 0; i < arr.size(); i++) {
            HSSFCell cell = row.createCell(i);
            cell.setCellValue(arr.get(i));
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, i == 0 ? (12 * 256) : (25 * 256));/*设置第一列的宽度是31个字符宽度*/
        }
        return wb;
    }

    /**
     * 下载模板
     *  ExcelFileUtils.fileDownload(request,response ,"模板下载", "text", "static/xml/textXml.xml");
     * @param request
     * @param response
     * @param fileName 下载文件需要取名
     * @param flag     xml标记id
     * @param xmlPath  xml地址
     */
    public static void fileDownload(HttpServletRequest request, HttpServletResponse response, String fileName, String flag, String xmlPath) {
        String path = request.getSession().getServletContext().getRealPath("") + fileName + ".xls";
        HSSFWorkbook wb = ExcelFileUtils.fileDownloadBook(fileName, flag, xmlPath);
        File file = new File(path);
        try {
            /*生成excel 在服务器*/
            FileOutputStream os = new FileOutputStream(path);
            wb.write(os);
            os.close();
            /*下载excel*/
            response.addHeader("Content-Disposition", "attachment;filename=" + new String(setFileDownloadHeader(request, fileName + ".xls")));
            response.setContentType("application/octet-stream");
            OutputStream out = response.getOutputStream();
            InputStream in = new FileInputStream(file);
            int b;
            while ((b = in.read()) != -1) out.write(b);
            in.close();
            out.close();
            /*删除生成的excel文件*/
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
