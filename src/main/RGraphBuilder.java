package main;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.OpVisitor;
import org.apache.jena.sparql.algebra.Table;
import org.apache.jena.sparql.algebra.op.OpAssign;
import org.apache.jena.sparql.algebra.op.OpBGP;
import org.apache.jena.sparql.algebra.op.OpConditional;
import org.apache.jena.sparql.algebra.op.OpDatasetNames;
import org.apache.jena.sparql.algebra.op.OpDiff;
import org.apache.jena.sparql.algebra.op.OpDisjunction;
import org.apache.jena.sparql.algebra.op.OpDistinct;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.algebra.op.OpFilter;
import org.apache.jena.sparql.algebra.op.OpGraph;
import org.apache.jena.sparql.algebra.op.OpGroup;
import org.apache.jena.sparql.algebra.op.OpJoin;
import org.apache.jena.sparql.algebra.op.OpLabel;
import org.apache.jena.sparql.algebra.op.OpLeftJoin;
import org.apache.jena.sparql.algebra.op.OpList;
import org.apache.jena.sparql.algebra.op.OpMinus;
import org.apache.jena.sparql.algebra.op.OpNull;
import org.apache.jena.sparql.algebra.op.OpOrder;
import org.apache.jena.sparql.algebra.op.OpPath;
import org.apache.jena.sparql.algebra.op.OpProcedure;
import org.apache.jena.sparql.algebra.op.OpProject;
import org.apache.jena.sparql.algebra.op.OpPropFunc;
import org.apache.jena.sparql.algebra.op.OpQuad;
import org.apache.jena.sparql.algebra.op.OpQuadBlock;
import org.apache.jena.sparql.algebra.op.OpQuadPattern;
import org.apache.jena.sparql.algebra.op.OpReduced;
import org.apache.jena.sparql.algebra.op.OpSequence;
import org.apache.jena.sparql.algebra.op.OpService;
import org.apache.jena.sparql.algebra.op.OpSlice;
import org.apache.jena.sparql.algebra.op.OpTable;
import org.apache.jena.sparql.algebra.op.OpTopN;
import org.apache.jena.sparql.algebra.op.OpTriple;
import org.apache.jena.sparql.algebra.op.OpUnion;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprWalker;

/**
 * This class implements Jena's OpVisitor. It recursively builds an r-graph from a query.
 * @author Jaime
 *
 */
public class RGraphBuilder implements OpVisitor {
	
	private Stack<RGraph> graphStack = new Stack<RGraph>();
	private Stack<RGraph> unionStack = new Stack<RGraph>();
	private Stack<RGraph> joinStack = new Stack<RGraph>();
	private Stack<RGraph> optionalStack = new Stack<RGraph>();
	private Stack<RGraph> filterStack = new Stack<RGraph>();
	List<Var> projectionVars;
	Set<Var> totalVars = new HashSet<Var>();
	private List<String> graphURI;
	private List<String> namedGraphURI;
	public int nTriples = 0;
	private int bgpId = 0;
	private int unionId = 0;
	private int optionalId = 0;
	private int filterId = 0;
	private boolean enableFilter = true;
	private boolean enableOptional = true;
	boolean isDistinct = false;
	private boolean containsUnion = false;
	private boolean containsJoin = false;
	private boolean containsOptional = false;
	private boolean containsFilter = false;
	private boolean containsSolutionMods = false;
	private boolean containsNamedGraphs = false;
	
	public RGraphBuilder(){
		
	}
	
	public RGraphBuilder(Query query){
		this.projectionVars = query.getProjectVars();
		graphURI = query.getGraphURIs();
		namedGraphURI = query.getNamedGraphURIs();
	}

	@Override
	public void visit(OpBGP arg0) {
		nTriples += arg0.getPattern().size();
		graphStack.add(new RGraph(arg0.getPattern().getList(), bgpId++));
		for (Triple t : arg0.getPattern().getList()){
			if (t.getSubject().isVariable()){
				totalVars.add((Var) t.getSubject());
			}
			if (t.getPredicate().isVariable()){
				totalVars.add((Var) t.getPredicate());
			}
			if (t.getObject().isVariable()){
				totalVars.add((Var) t.getObject());
			}
		}
	}

	@Override
	public void visit(OpQuadPattern arg0) {
		
	}

	@Override
	public void visit(OpQuadBlock arg0) {
		
	}

	@Override
	public void visit(OpTriple arg0) {
		
	}

	@Override
	public void visit(OpQuad arg0) {
		
	}

	@Override
	public void visit(OpPath arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpTable arg0) {
		System.out.println(arg0.getTable().getVars());
		Table t = arg0.getTable();
		Iterator<Binding> iter = t.rows();
		while (iter.hasNext()) {
			System.out.println(iter.next());
		}
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpNull arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpProcedure arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpPropFunc arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpFilter arg0) {
		containsFilter = true;
		FilterVisitor fv = new FilterVisitor();
		ExprWalker.walk(fv, arg0.getExprs().get(0));
		if (enableFilter){
//			FilterParser fp = new FilterParser(graphStack.peek(), filterId++);
//			fp.parse(arg0.toString().replace("exprlist", "&&").split("\n")[0].substring(8));
			graphStack.peek().filter(fv.getGraph(), filterId++);
			filterStack.add(graphStack.peek());
		}
		else{
			throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		}
	}

	@Override
	public void visit(OpGraph arg0) {
		containsNamedGraphs = true;
		graphStack.peek().graphOp(arg0.getNode());
	}

	@Override
	public void visit(OpService arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpDatasetNames arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpLabel arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpAssign arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpExtend arg0) {
		VarExprList ve = arg0.getVarExprList();
		Map<Var,Expr> map = ve.getExprs();
		for (Map.Entry<Var, Expr> m : map.entrySet()) {
			BindVisitor bv = new BindVisitor(m.getKey());
			ExprWalker.walk(bv, m.getValue());
			graphStack.peek().bind(bv.getGraph(), bgpId++);
		}
		graphStack.peek().print();
	}

	@Override
	public void visit(OpJoin arg0) {
		RGraph e1, e2;
		containsJoin = true;
		if (arg0.getRight() instanceof OpBGP){
			e2 = graphStack.pop();
		}
		else if (arg0.getRight() instanceof OpUnion){
			e2 = unionStack.pop();
		}
		else if (arg0.getRight() instanceof OpLeftJoin){
			if (enableOptional){
				e2 = optionalStack.pop();
			}
			else{
				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getRight().getName());
			}
		}
		else if (arg0.getRight() instanceof OpJoin){
			e2 = joinStack.pop();
		}
		else if (arg0.getRight() instanceof OpFilter){
			if (enableFilter){
				e2 = filterStack.pop();
			}
			else{
				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getRight().getName());
			}
		}
		else{
			e2 = graphStack.pop();
		}
		if (arg0.getLeft() instanceof OpBGP){
			e1 = graphStack.pop();
		}
		else if (arg0.getLeft() instanceof OpUnion){
			e1 = unionStack.pop();
		}
		else if (arg0.getLeft() instanceof OpLeftJoin){
			if (enableOptional){
				e1 = optionalStack.pop();
			}
			else{
				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getLeft().getName());
			}
		}
		else if (arg0.getLeft() instanceof OpJoin){
			e1 = joinStack.pop();
		}
		else if (arg0.getLeft() instanceof OpFilter){
			if (enableFilter){
				e1 = filterStack.pop();
			}
			else{
				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getLeft().getName());
			}
		}
		else{
			e1 = graphStack.pop();
		}
		e1.join(e2,bgpId++);
		joinStack.add(e1);
		graphStack.add(e1);		
	}

	@Override
	public void visit(OpLeftJoin arg0) {
		RGraph e1, e2;
		containsOptional = true;
		if (enableOptional){
			if (arg0.getRight() instanceof OpBGP){
				e2 = new RGraph(((OpBGP)arg0.getRight()).getPattern().getList(), bgpId++);
			}
			else if (arg0.getRight() instanceof OpUnion){
				e2 = unionStack.pop();
			}
			else if (arg0.getRight() instanceof OpLeftJoin){
				e2 = optionalStack.pop();
			}
			else if (arg0.getRight() instanceof OpFilter){
				if (enableFilter){
					e2 = filterStack.pop();
				}
				else{
					throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getRight().getName());
				}
			}
			else{
				e2 = joinStack.pop();
			}
			if (arg0.getLeft() instanceof OpBGP){
				e1 = new RGraph(((OpBGP)arg0.getLeft()).getPattern().getList(), bgpId++);
			}
			else if (arg0.getLeft() instanceof OpUnion){
				e1 = unionStack.pop();
			}
			else if (arg0.getLeft() instanceof OpLeftJoin){
				e1 = optionalStack.pop();
			}
			else if (arg0.getLeft() instanceof OpFilter){
				if (enableFilter){
					e1 = filterStack.pop();
				}
				else{
					throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getLeft().getName());
				}
			}
			else{
				e1 = joinStack.pop();
			}
			e1.optional(e2, optionalId++);
			optionalStack.add(e1);
			graphStack.add(e1);
		}
		else{
			throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		}
		
	}

	@Override
	public void visit(OpUnion arg0) {
		RGraph e1, e2;
		containsUnion = true;
		if (arg0.getLeft() instanceof OpBGP){
			e1 = graphStack.pop();
		}
		else if (arg0.getLeft() instanceof OpUnion){
			e1 = unionStack.pop();
		}
		else if (arg0.getLeft() instanceof OpLeftJoin){
			if (enableOptional){
				e1 = optionalStack.pop();
			}
			else{
				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getLeft().getName());
			}
		}
		else if (arg0.getLeft() instanceof OpJoin){
			e1 = joinStack.pop();
		}
		else if (arg0.getLeft() instanceof OpFilter){
			if (enableFilter){
				e1 = filterStack.pop();
			}
			else{
				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getLeft().getName());
			}
		}
		else{
			e1 = graphStack.pop();
		}
		if (arg0.getRight() instanceof OpBGP){
			e2 = graphStack.pop();
		}
		else if (arg0.getRight() instanceof OpUnion){
			e2 = unionStack.pop();
		}
		else if (arg0.getRight() instanceof OpLeftJoin){
			if (enableOptional){
				e2 = optionalStack.pop();
			}
			else{
				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getRight().getName());
			}
		}
		else if (arg0.getRight() instanceof OpJoin){
			e2 = joinStack.pop();
		}
		else if (arg0.getRight() instanceof OpFilter){
			if (enableFilter){
				e2 = filterStack.pop();
			}
			else{
				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getRight().getName());
			}
		}
		else{
			e2 = graphStack.pop();
		}
		e2.union(e1, unionId++);
		unionStack.add(e2);
		graphStack.add(e2);	
	}

	@Override
	public void visit(OpDiff arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpMinus arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpConditional arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpSequence arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpDisjunction arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpList arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpOrder arg0) {
		containsSolutionMods = true;
		List<SortCondition> cond = arg0.getConditions();
		List<Var> vars = new ArrayList<Var>();
		List<Integer> dir = new ArrayList<Integer>();
		for (SortCondition c : cond){
			if (c.getExpression().isVariable()){
				vars.add(c.getExpression().asVar());
				dir.add(c.getDirection() == -1 ? -1 : 1);
			}
			else{
				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
			}
		}
		if (!graphStack.peek().containsProjection()){
			graphStack.peek().project(projectionVars);
		}
		graphStack.peek().orderBy(vars, dir);
	}

	@Override
	public void visit(OpProject arg0) {
		graphStack.peek().project(arg0.getVars());
	}

	@Override
	public void visit(OpReduced arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpDistinct arg0) {
		isDistinct = true;
		
	}

	@Override
	public void visit(OpSlice arg0) {
		containsSolutionMods = true;
		long offset = arg0.getStart() < 0 ? 0 : arg0.getStart();
		long limit = arg0.getLength();
		if (!graphStack.peek().containsProjection()){
			graphStack.peek().project(projectionVars);
		}
		graphStack.peek().slice((int)offset, (int)limit);
		
	}

	@Override
	public void visit(OpGroup arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpTopN arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}
	
	public void setEnableFilter(boolean b){
		this.enableFilter = b;
	}
	
	public void setEnableOptional(boolean b){
		this.enableOptional = b;
	}
	
	public RGraph getResult(){
		if (!graphStack.peek().containsProjection()){
			graphStack.peek().project(projectionVars);
		}
		if (!this.graphURI.isEmpty()){
			containsNamedGraphs = true;
			graphStack.peek().fromGraph(graphURI);
		}
		if (!this.namedGraphURI.isEmpty()){
			containsNamedGraphs = true;
			graphStack.peek().fromNamedGraph(namedGraphURI);
		}
		return graphStack.peek();
	}
	
	public boolean getContainsUnion(){
		return this.containsUnion;
	}
	
	public boolean getContainsJoin(){
		return this.containsJoin;
	}
	
	public boolean getContainsOptional(){
		return this.containsOptional;
	}
	
	public boolean getContainsFilter(){
		return this.containsFilter;
	}
	
	public boolean getContainsSolutionMods(){
		return this.containsSolutionMods;
	}
	
	public boolean getContainsNamedGraphs(){
		return this.containsNamedGraphs;
	}

}