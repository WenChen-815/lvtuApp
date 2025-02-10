package com.zhoujh.lvtu.utils;

public class HuanXinUtils {
    /**
     * 构建环信ID
     * 去除其中的“-”，并每四位字符为一组进行组内逆序
     * @param uuid 用户的 UUID
     * @return 构建得到的 环信ID
     */
    public static String createHXId(String uuid) {
        // 去除字符串中的 -
        String uuidWithoutDash = uuid.replace("-", "");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < uuidWithoutDash.length(); i += 4) {
            String group = uuidWithoutDash.substring(i, Math.min(i + 4, uuidWithoutDash.length()));
            // 对每一组进行逆序
            StringBuilder reversedGroup = new StringBuilder(group).reverse();
            result.append(reversedGroup);
        }
        return result.toString();
    }
}
