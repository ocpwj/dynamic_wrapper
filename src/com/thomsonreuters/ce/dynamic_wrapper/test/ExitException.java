package com.thomsonreuters.ce.dynamic_wrapper.test;

public class ExitException extends SecurityException {
    public final int status;
    public ExitException(int status) 
    {
        super("Stopped inside System.exit() attempt.");
        this.status = status;
    }
}
