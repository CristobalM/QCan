package cl.uchile.dcc.qcan.generate;

import cl.uchile.dcc.blabel.label.GraphColouring.HashCollisionException;
import cl.uchile.dcc.qcan.RGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Var;

import java.io.*;
import java.util.*;


public class Generator {
    protected BufferedReader br;
    protected HashMap<Integer, Node> hm = new HashMap<Integer, Node>();
    protected List<Triple> triples = new ArrayList<Triple>();
    protected Random rng = new Random();
    protected Node predicate;
    protected List<Var> vars = new ArrayList<Var>();

    public Generator(File f) throws FileNotFoundException {
        br = new BufferedReader(new FileReader(f));
        predicate = NodeFactory.createURI("p");
    }

    public void generateTriples() throws IOException {
        String s = br.readLine();
        while ((s = br.readLine()) != null) {
            String[] coords = s.split(" ");
            int r = Math.abs(rng.nextInt((int) Math.pow(2, 20)));
            int v0 = new Integer(coords[1]);
            int v1 = new Integer(coords[2]);
            if (!hm.containsKey(v0)) {
                hm.put(v0, NodeFactory.createVariable("v" + (v0 + r)));
                vars.add(Var.alloc("v" + (v0 + r)));
            }
            r = Math.abs(rng.nextInt((int) Math.pow(2, 20)));
            if (!hm.containsKey(v1)) {
                hm.put(v1, NodeFactory.createVariable("v" + (v1 + r)));
                vars.add(Var.alloc("v" + (v1 + r)));
            }
            triples.add(Triple.create(hm.get(v0), predicate, hm.get(v1)));
        }
        br.close();
    }

    public RGraph generateGraph() {
        RGraph ans = new RGraph(this.triples, this.vars);
        ans.project(vars.subList(0, 1));
        ans.setDistinctNode(true);
        ans.turnDistinctOn();
        return ans;
    }

    public void printStats() throws InterruptedException, HashCollisionException, IOException {
        int initialNodes, finalNodes, initialVars, finalVars, initialTriples, finalTriples;
        generateTriples();
        RGraph e = generateGraph();
        String output = "";
        initialNodes = e.getNumberOfNodes();
        initialVars = e.getNumberOfVars();
        initialTriples = e.getNumberOfTriples();
        long t = System.nanoTime();
        RGraph a = e.getCanonicalForm();
        t = System.nanoTime() - t;
        finalNodes = a.getNumberOfNodes();
        finalVars = a.getNumberOfVars();
        finalTriples = a.getNumberOfTriples();
        output += t + "\t";
        output += initialNodes + "\t";
        output += finalNodes + "\t";
        output += initialVars + "\t";
        output += finalVars + "\t";
        output += initialTriples + "\t";
        output += finalTriples;
        System.out.println(output);
    }

    static File fromResource(String resourcePath) {
        ClassLoader classLoader = Generator.class.getClassLoader();
        return new File(Objects.requireNonNull(classLoader.getResource(resourcePath)).getFile());
    }

    public static void main(String[] args) throws IOException, InterruptedException, HashCollisionException {
        Generator g = new Generator(fromResource("eval/lattice/lattice-4"));
        g.printStats();
    }

}
