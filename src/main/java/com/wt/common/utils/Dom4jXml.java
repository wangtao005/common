package com.wt.common.utils;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dom4jXml {
    public static  Map<String,List<Map<String, String>>> getConfigurationInfo(String flag,String xmlPath) {

        Map<String,List<Map<String, String>>> returnMmap = new HashMap<>();
        List<Map<String, String>> message = new ArrayList<>();
        List<Map<String, String>> entityPathList = new ArrayList<>();
        try {
            String str = "";
            String entityPath = "";
            List<Map<String, String>> attrLists = new ArrayList<>();
//            File inputFile = new File(xmlPath);
//            Document document = reader.read(inputFile);
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(xmlPath);
            SAXReader reader = new SAXReader();
            Document document = reader.read(is);
            Element root = document.getRootElement();
            /*获取root下的所有节点*/
            List<Element> elements = root.elements();
            /*遍历节点,获取需要的提取数据的大节点,根据id*/
            for (int i = 0; i < elements.size(); i++) {
                Element element = elements.get(i);
                List<Attribute> attributes = element.attributes();
                /*获取该节点上的属性*/
                for (Attribute attribute : attributes) {
                    String name = attribute.getName();
                    if ( "id".equals(name)) {
                        String value = attribute.getValue();
                        if (value.equals(flag)) {
                            /*获取指定名称下的所有节点*/
                            List<Element> list = element.elements();
                            if(list.size()==0){
                                str += "标记<"+flag+">在xml<"+xmlPath+">中下没有子节点,请按照要求添加";
                                System.out.println("标记<"+flag+">在xml<"+xmlPath+">中下没有子节点,请按照要求添加");
                            }
                            for (Element listElement : list) {
                                List<Attribute> attrList = listElement.attributes();
                                Map<String, String> map = new HashMap<>();
                                for (Attribute attr : attrList) map.put(attr.getName(), attr.getValue());
                                attrLists.add(map);
                            }
                        }else{
                            str += "标记<"+flag+">在xml<"+xmlPath+">中未发现";
                            System.out.println("标记<"+flag+">在xml<"+xmlPath+">中未发现");
                        }
                    }else if("entityPath".equals(name)){
                        entityPath = attribute.getValue();
                    }
                }
            }
            if (attrLists.size() == 0) {
                return null;
            }
            Map<String, String> mesmap = new HashMap<>();
            Map<String, String> entityPathMap = new HashMap<>();
            entityPathMap.put("entityPath", entityPath);
            entityPathList.add(entityPathMap);
            mesmap.put("message", str);
            message.add(mesmap);
            returnMmap .put("data", attrLists);
            returnMmap .put("message", message);
            returnMmap .put("entityPath", entityPathList);
            return  returnMmap;
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return null;
    }
}
