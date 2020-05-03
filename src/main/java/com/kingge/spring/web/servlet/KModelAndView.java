package com.kingge.spring.web.servlet;

import java.util.Map;

/**
 * Created by Tom on 2018/4/22.
 */
public class KModelAndView {

    private String viewName;
    private Map<String,?> model;

    public KModelAndView(String viewName, Map<String, ?> model) {
        this.viewName = viewName;
        this.model = model;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Map<String, ?> getModel() {
        return model;
    }

    public void setModel(Map<String, ?> model) {
        this.model = model;
    }
}
