package skadistats.spectre.err;

public class OutOfOrderWrite extends RuntimeException {
    public OutOfOrderWrite(String msg) { super(msg); }
}
