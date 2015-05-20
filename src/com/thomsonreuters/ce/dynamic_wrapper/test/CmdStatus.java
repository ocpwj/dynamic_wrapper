package com.thomsonreuters.ce.dynamic_wrapper.test;


import java.util.HashMap;
import java.util.Map;

public enum CmdStatus {
    WAITING(1, "WAITING"),
    RUNNING(2, "RUNNING"),
    FINISHED(3, "FINISHED"),
    FAILED(4, "FAILED");
    
    private int id;
    private String name;

    static final Map<Integer, CmdStatus> StatusByCode = new HashMap<Integer, CmdStatus>();

    static {
        for (CmdStatus area : CmdStatus.values()) {
        	StatusByCode.put(area.id, area);
        }
    }

    private CmdStatus(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
 
    public static CmdStatus getByCode(int ID) {
        return StatusByCode.get(ID);
    }
}
