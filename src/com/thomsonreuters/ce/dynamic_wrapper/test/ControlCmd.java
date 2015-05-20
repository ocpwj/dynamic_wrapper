package com.thomsonreuters.ce.dynamic_wrapper.test;


import java.util.HashMap;
import java.util.Map;

public enum ControlCmd {
    INSTALL(1, "INSTALL"),
    START(2, "START"),
    STOP(3, "STOP"), // TODO verify the sign of the region
    UNINSTALL(4, "UNINSTALL"); 
    
    private int id;
    private String name;

    static final Map<Integer, ControlCmd> CmdByCode = new HashMap<Integer, ControlCmd>();

    static {
        for (ControlCmd area : ControlCmd.values()) {
        	CmdByCode.put(area.id, area);
        }
    }

    private ControlCmd(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
 
    public static ControlCmd getByCode(int ID) {
        return CmdByCode.get(ID);
    }


}
