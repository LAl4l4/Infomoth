package ReleaseBack.Back.exception;

public class DataFileNotFoundException extends BaseException {
    public DataFileNotFoundException(String message) {
        super(404, "Data not found: " + message);
    }
}
