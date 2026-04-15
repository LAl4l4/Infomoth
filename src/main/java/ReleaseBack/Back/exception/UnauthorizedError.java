package ReleaseBack.Back.exception;

public class UnauthorizedError extends BaseException {
    public UnauthorizedError(String message) {
        super(401, message);
    }
    
}
