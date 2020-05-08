package cl.uchile.dcc.qcan;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.*;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.path.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PathWalker implements PathVisitor {
	
	Graph graph = GraphFactory.createPlainGraph();
	Node epsilon = NodeFactory.createURI("http://example.org/epsilon");
	Stack<Node> nodeStack = new Stack<Node>();
	Stack<Triple> tripleStack = new Stack<Triple>();
	Set<Node> predicates = new HashSet<Node>();
	Map<Pair<Node,Node>, Set<Pair<Node,Integer>>> delta = new HashMap<Pair<Node,Node>, Set<Pair<Node,Integer>>>();

	final Logger logger = LoggerFactory.getLogger(PathWalker.class);
	
	@Override
	public void visit(P_Link arg0) {
		Node n = NodeFactory.createBlankNode();
		Node b = NodeFactory.createBlankNode();
		Triple t = Triple.create(n, arg0.getNode(), b);
		tripleStack.add(t);
		nodeStack.add(b);
		graph.add(t);
		predicates.add(arg0.getNode());
	}

	@Override
	public void visit(P_ReverseLink arg0) {
		Node n = NodeFactory.createBlankNode();
		Node b = NodeFactory.createBlankNode();
		Node p = NodeFactory.createLiteral("^"+arg0.getNode().toString());
		Triple t = Triple.create(n, p, b);
		tripleStack.add(t);
		nodeStack.add(b);
		graph.add(t);
		predicates.add(p);
	}

	@Override
	public void visit(P_NegPropSet arg0) {
		List<P_Path0> list = arg0.getNodes();
		for (P_Path0 p : list ) {
			p.visit(this);
		}
	}

	@Override
	public void visit(P_Inverse arg0) {
		if (arg0.getSubPath() instanceof P_Link) {
			Node n = NodeFactory.createBlankNode();
			Node b = NodeFactory.createBlankNode();
			Node p = NodeFactory.createLiteral("^"+((P_Path0) arg0.getSubPath()).getNode().toString());
			Triple t = Triple.create(n, p, b);
			tripleStack.add(t);
			nodeStack.add(b);
			graph.add(t);
			predicates.add(p);
		}
		else {
			arg0.getSubPath().visit(this);	
		}
	}

	@Override
	public void visit(P_Mod arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_FixedLength arg0) {
		arg0.getSubPath().visit(this);
		
	}

	@Override
	public void visit(P_Distinct arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_Multi arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_Shortest arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_ZeroOrOne arg0) {
		arg0.getSubPath().visit(this);
		Node n = NodeFactory.createBlankNode();
		Node f = NodeFactory.createBlankNode();
		Triple t = Triple.create(n, epsilon, f);
		graph.add(Triple.create(n, epsilon, tripleStack.pop().getSubject()));
		graph.add(Triple.create(nodeStack.pop(), epsilon, f));
		graph.add(t);
		tripleStack.add(t);
		nodeStack.add(f);
	}

	@Override
	public void visit(P_ZeroOrMore1 arg0) {
		arg0.getSubPath().visit(this);
		Node n = NodeFactory.createBlankNode();
		Node f = NodeFactory.createBlankNode();
		Triple t = Triple.create(n, epsilon, f);
		Node t1 = tripleStack.pop().getSubject();
		Node finalState = nodeStack.pop();
		graph.add(Triple.create(n, epsilon, t1));
		graph.add(Triple.create(finalState, epsilon, f));
		graph.add(Triple.create(finalState, epsilon, t1));
		graph.add(t);
		tripleStack.add(t);
		nodeStack.add(f);
	}

	@Override
	public void visit(P_ZeroOrMoreN arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_OneOrMore1 arg0) {
		arg0.getSubPath().visit(this);
		Node n = NodeFactory.createBlankNode();
		Node f = NodeFactory.createBlankNode();
		Node t1 = tripleStack.pop().getSubject();
		Triple t = Triple.create(n, epsilon, t1);
		Node finalState = nodeStack.pop();
		graph.add(Triple.create(finalState, epsilon, f));
		graph.add(Triple.create(finalState, epsilon, t1));
		graph.add(t);
		tripleStack.add(t);
		nodeStack.add(f);
	}

	@Override
	public void visit(P_OneOrMoreN arg0) {
		arg0.getSubPath().visit(this);
	}

	@Override
	public void visit(P_Alt arg0) {
		Node n = NodeFactory.createBlankNode();
		Node f = NodeFactory.createBlankNode();
		arg0.getLeft().visit(this);
		Triple t1 = Triple.create(n, epsilon, tripleStack.pop().getSubject());
		Node finalState1 = nodeStack.pop();
		graph.add(t1);
		graph.add(Triple.create(finalState1, epsilon, f));
		arg0.getRight().visit(this);
		Triple t2 = Triple.create(n, epsilon, tripleStack.pop().getSubject());
		Node finalState2 = nodeStack.pop();
		graph.add(t2);
		graph.add(Triple.create(finalState2, epsilon, f));
		tripleStack.add(t1);
		nodeStack.add(f);
	}

	@Override
	public void visit(P_Seq arg0) {
		arg0.getLeft().visit(this);
		Node finalState = nodeStack.pop();
		Triple t1 = tripleStack.pop();
		arg0.getRight().visit(this);
		Triple t2 = tripleStack.pop();
		graph.add(Triple.create(finalState, epsilon, t2.getSubject()));
		tripleStack.add(t1);
	}
	
	public void print() {
		if(!logger.isDebugEnabled()) return;
		ExtendedIterator<Triple> et = GraphUtil.findAll(graph);
		while (et.hasNext()) {
			logger.debug(String.valueOf(et.next()));
		}
		logger.debug("Start: "+tripleStack.peek().getSubject());
		logger.debug("End: "+nodeStack.peek());
	}
	
	public Node getStartState() {
		if (!tripleStack.empty()) {
			return tripleStack.peek().getSubject();
		}
		else {
			return null;
		}
	}
	
	public Node getEndState() {
		if (!nodeStack.empty()) {
			return nodeStack.peek();
		}
		else {
			return null;
		}
	}

}
