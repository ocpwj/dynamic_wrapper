package com.thomsonreuters.ce.dynamic_wrapper.test;


import java.util.HashMap;
import java.util.Map;

public enum ApplicationStatus {
	RELEASED(1, "RELEASED"),
	INSTALLING(2, "INSTALLING"),
	STARTING(4, "STARTING"),
	STARTED(5, "STARTED"),
	STOPPING(6, "STOPPING"),
	STOPPED(7, "STOPPED"),
	UNINSTALLING(8, "UNINSTALLING"); 
    
    private int id;
    private String name;

    static final Map<Integer, ApplicationStatus> StatusByCode = new HashMap<Integer, ApplicationStatus>();
    static final Map<String, ApplicationStatus> StatusByName = new HashMap<String, ApplicationStatus>();

    static {
        for (ApplicationStatus area : ApplicationStatus.values()) {
        	StatusByCode.put(area.id, area);
        	StatusByName.put(area.name, area);
        }
    }

    private ApplicationStatus(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
 
    public static ApplicationStatus getByCode(int ID) {
        return StatusByCode.get(ID);
    }

    public static ApplicationStatus getByName(String Name) {
        return StatusByName.get(Name);
    }    
}
