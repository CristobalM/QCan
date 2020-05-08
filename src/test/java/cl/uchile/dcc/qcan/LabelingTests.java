package cl.uchile.dcc.qcan;

import cl.uchile.dcc.blabel.label.GraphColouring;
import org.junit.Test;

import static org.junit.Assert.fail;

public class LabelingTests {
    @Test
    public void LabelTestOne(){
        SingleQuery singleQuery = null;
        try {
            singleQuery = new SingleQuery("SELECT DISTINCT ?a WHERE { { ?a <http://example.org/p> ?b  }   UNION { ?a <http://example.org/q> ?b  }   UNION { ?a <http://example.org/r> ?b  }}");
        } catch (InterruptedException | GraphColouring.HashCollisionException e) {
            e.printStackTrace();
        }

        if(singleQuery == null){
            fail("singleQuery is null");
        }

        try {
            singleQuery.canonicalise(false);
        } catch (InterruptedException | GraphColouring.HashCollisionException e) {
            e.printStackTrace();
        }

        String canonicalQuery = singleQuery.getQuery();

        System.out.println(canonicalQuery);


    }
}
