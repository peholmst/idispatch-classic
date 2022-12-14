package net.pkhapps.idispatch.runboard.client;

public class DispatcherClientException extends Exception {

    private final ErrorCode code;

    public DispatcherClientException(ErrorCode code) {
        this.code = code;
    }

    public DispatcherClientException(ErrorCode code, Throwable cause) {
        super(cause);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }

    public enum ErrorCode {

        UNKNOWN_SERVER_ERROR,
        COMMUNICATION_ERROR
    }
}
