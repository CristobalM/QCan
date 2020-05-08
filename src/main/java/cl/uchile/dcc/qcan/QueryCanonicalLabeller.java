package cl.uchile.dcc.qcan;

import cl.uchile.dcc.blabel.label.GraphColouring;

public class QueryCanonicalLabeller {
    public static String labelQuery(String query) {
        SingleQuery singleQuery;
        try {
            singleQuery = new SingleQuery(query);
            singleQuery.canonicalise();
        } catch (InterruptedException | GraphColouring.HashCollisionException e) {
            throw new CantGenerateCanonicalLabel(e.toString());
        }

        return singleQuery.getCanonicalLabel();
    }
}
