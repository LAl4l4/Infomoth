package ReleaseBack.Back.exception;

public class BaseException extends RuntimeException {

    private int code;

    public BaseException(int code, String message) {
        super(message); // 把描述给父类
        this.code = code; // 把 code 存给自己
    }

    public int getCode() {
        return code;
    }
}