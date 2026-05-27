package ReleaseBack.Back.exception;

public class DataReadException extends BaseException {
    public DataReadException(String message) {
        super(500, "Data read error: " + message);
    }
}
