package com.sample.app;

import java.util.ArrayList;
import java.util.List;

public class ClassInfo {
    public String className;
    public String file;
    public List<FieldInfo> fields = new ArrayList<>();

    public ClassInfo(String className, String file) {
        this.className = className;
        this.file = file;
    }
}
