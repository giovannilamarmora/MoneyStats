package com.moneystats.MoneyStats.databaseImportExport;

public class DatabaseException extends Exception {

    private Code code;

    /**
     * Constructs a new exception with {@code null} as its detail message. The cause is not
     * initialized, and may subsequently be initialized by a call to {@link #initCause}.
     */
    public DatabaseException(Code code) {
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

    public static enum Code {
        INVALID_DATABASE_COMMAND_DTO,
        NOT_ADMIN_USER,
        ERROR_ON_EXPORT_DATABASE,
        ERROR_ON_IMPORT_DATABASE,
        CONNECTION_FAILED,
        FILE_NOT_FOUND,
    NOT_A_DIRECTORY
  }
}
