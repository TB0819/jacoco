package org.jacoco.core.tools;

import java.util.List;

public class Utils {

    public static boolean isAllAnalyzer(String methodName, String className, String desc, List<String> changeMethods) {
        String method = className + methodName;
        return changeMethods.contains(method);
    }
}
