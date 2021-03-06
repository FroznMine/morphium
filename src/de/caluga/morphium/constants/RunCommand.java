package de.caluga.morphium.constants;

public final class RunCommand {

    public enum Response {
        primary,
        secondary,
        arbiterOnly,
        ismaster,
        hosts,
        setName
    }

    public enum Command {
        local
    }

    public enum ErrorCode {
        UNABLE_TO_CONNECT("20");

        private String code;

        ErrorCode(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
