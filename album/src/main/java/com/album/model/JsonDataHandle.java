package com.album.model;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import com.album.utils.PropertyUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class JsonDataHandle {

    private Map<String, JSONObject> map = null;

    public JsonDataHandle() {
        this.map = new TreeMap<String, JSONObject>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o2.compareTo(o1);
            }
        });
    }

    public void add(String name) {
        // 解析原始名称
        String[] array1 = name.split("\\.");
        String[] array2 = array1[0].split("_");
        String dateStr = array2[0];
        String hmsStr = array2[1];
        String subjectStr = array2[2];
        String descStr = array2[3];
        // 获得本图片所属天的结构体(若没有则创建)
        if (!this.map.containsKey(dateStr)) {
            JSONObject arrValue = new JSONObject();
            arrValue.put("text", new JSONArray());
            arrValue.put("type", new JSONArray());
            arrValue.put("link", new JSONArray());
            arrValue.put("year", Integer.parseInt(dateStr.substring(0, 4)));
            arrValue.put("month", Integer.parseInt(dateStr.substring(4, 6)));
            arrValue.put("day", Integer.parseInt(dateStr.substring(6, 8)));
            JSONObject arr = new JSONObject();
            arr.put("arr", arrValue);
            this.map.put(dateStr, arr);
        }
        JSONObject day = this.map.get(dateStr);
        day.getJSONObject("arr").getJSONArray("text").add(this.getHMS(hmsStr) +
                                 ("0".equals(subjectStr) ? "" : "[" + subjectStr + "]") +
                                 ("0".equals(descStr) ? "" : descStr));
        day.getJSONObject("arr").getJSONArray("type").add("image");
        day.getJSONObject("arr").getJSONArray("link").add(name);
    }

    private String getHMS(String hmsStr) {
        String hour = hmsStr.substring(0, 2);
        String minute = hmsStr.substring(2, 4);
        String second = hmsStr.substring(4, 6);
        String nullValue = "99";
        if (nullValue.equals(hour)) return "";
        StringBuilder result = new StringBuilder();
        result.append("[").append(hour).append("时");
        if (nullValue.equals(minute)) return result.append("]").toString();
        result.append(minute).append("分");
        if (nullValue.equals(second)) return result.append("]").toString();
        result.append(second).append("秒]");
        return result.toString();
    }

    public void createJson() throws IOException {
        JSONObject root = new JSONObject();
        JSONArray rootArr = new JSONArray();
        for (Map.Entry<String, JSONObject> entry : this.map.entrySet())
            rootArr.add(entry.getValue());
        root.put("list", rootArr);
        try (FileWriter writer = new FileWriter(PropertyUtil.get("local.json"), true)) {
            writer.write(root.toJSONString());
            writer.flush();
        }
    }
}
