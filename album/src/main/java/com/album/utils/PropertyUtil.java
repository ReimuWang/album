package com.album.utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * 读取properties配置文件的工具类
 * 本程序properties配置文件唯一，对外只提供一个get(String)方法
 */
public class PropertyUtil {

    private static Properties PROPERTIES;

    private PropertyUtil() {}

    public static String get(String key) {
        if (null == PropertyUtil.PROPERTIES)
            PropertyUtil.loadProperty();
        return PropertyUtil.PROPERTIES.getProperty(key);
    }

    private static synchronized void loadProperty() {
        try (InputStream inputStream = PropertyUtil.class.getClassLoader().getResourceAsStream("config.properties");) {
            PropertyUtil.PROPERTIES = new Properties();
            PropertyUtil.PROPERTIES.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
}
