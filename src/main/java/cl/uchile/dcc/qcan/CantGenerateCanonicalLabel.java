package cl.uchile.dcc.qcan;

public class CantGenerateCanonicalLabel extends RuntimeException {
    public CantGenerateCanonicalLabel(String message) {
        super(message);
    }
}
