package cl.uchile.dcc.qcan;

import cl.uchile.dcc.qcan.data.PropertyPathFeatureCounter;
import cl.uchile.dcc.qcan.externalfix.ExprWalker;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.*;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.algebra.optimize.TransformExtendCombine;
import org.apache.jena.sparql.algebra.optimize.TransformMergeBGPs;
import org.apache.jena.sparql.algebra.optimize.TransformPathFlatternStd;
import org.apache.jena.sparql.algebra.optimize.TransformSimplify;
import org.apache.jena.sparql.core.TriplePath;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.E_LogicalAnd;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprAggregator;
import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This class implements Jena's OpVisitor. It recursively builds an r-graph from a query.
 * @author Jaime
 *
 */
public class RGraphBuilder implements OpVisitor {
	
	private Stack<RGraph> graphStack = new Stack<>();
	private Stack<RGraph> unionStack = new Stack<>();
	private Stack<RGraph> joinStack = new Stack<>();
	private Stack<RGraph> optionalStack = new Stack<>();
	private Stack<RGraph> filterStack = new Stack<>();
	List<Var> projectionVars;
	Set<Var> totalVars = new HashSet<Var>();
	private List<String> graphURI = Collections.emptyList();
	private List<String> namedGraphURI = Collections.emptyList();
	public int nTriples = 0;
	private boolean enableFilter = true;
	private boolean enableOptional = true;
	boolean isDistinct = false;
	private boolean containsUnion = false;
	private boolean containsJoin = false;
	private boolean containsOptional = false;
	private boolean containsFilter = false;
	private boolean containsSolutionMods = false;
	private boolean containsNamedGraphs = false;
	private boolean containsPaths = false;

	final Logger logger = LoggerFactory.getLogger(RGraphBuilder.class);
	
	public RGraphBuilder(){
		
	}

	public RGraphBuilder(Query query){
		this.projectionVars = query.getProjectVars();
		graphURI = query.getGraphURIs();
		namedGraphURI = query.getNamedGraphURIs();
		Op op = Algebra.compile(query);
		op = UCQTransformation(op);
		this.setEnableFilter(true);
		this.setEnableOptional(true);
		OpWalker.walk(op, this);
	}

	@Override
	public void visit(OpBGP arg0) {
		nTriples += arg0.getPattern().size();
		graphStack.add(new RGraph(arg0.getPattern().getList()));
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
		nTriples += 1;
		graphStack.add(new RGraph(Collections.singletonList(arg0.getTriple())));
		Triple t = arg0.getTriple();
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

	@Override
	public void visit(OpQuad arg0) {
		
	}

	@Override
	public void visit(OpPath arg0) {
		TriplePath tp = arg0.getTriplePath();
		PathTransform pt = new PathTransform();
		Path path = tp.getPath();
		path = pt.visit(path);
		Op o = pt.getResult(tp);
		if (o instanceof OpJoin) {
			OpPath left = (OpPath) ((OpJoin) o).getLeft();
			OpPath right = (OpPath) ((OpJoin) o).getRight();
			TriplePath leftTP = left.getTriplePath();
			TriplePath rightTP = right.getTriplePath();
			PGraph pLeft = new PGraph(leftTP);
			PGraph pRight = new PGraph(rightTP);
			RGraph leftG = new RGraph(leftTP.getSubject(), leftTP.getObject(), pLeft);
			RGraph rightG = new RGraph(rightTP.getSubject(), rightTP.getObject(), pRight);
//			RGraph leftG = new RGraph(leftTP.getSubject(), leftTP.getObject(), leftTP.getPath());
//			RGraph rightG = new RGraph(rightTP.getSubject(), rightTP.getObject(), rightTP.getPath());
			leftG.join(rightG);
			graphStack.add(leftG);
		}
		else {
			TriplePath tp0 = ((OpPath) o).getTriplePath();
			PGraph p = new PGraph(tp0);
			RGraph rg = new RGraph(tp0.getSubject(), tp0.getObject(), p);
//			RGraph rg = new RGraph(tp0.getSubject(), tp0.getObject(), tp0.getPath());
			graphStack.add(rg);		
		}
		this.containsPaths = true;
	}

	@Override
	public void visit(OpTable arg0) {
		Table t = arg0.getTable();
		RGraph table = RGraph.table(t);
		graphStack.add(table);		
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
		List<Expr> exprs = arg0.getExprs().getList();
		Expr expr = exprs.get(0);
		if (exprs.size() > 1) {
			for (Expr e : exprs.subList(1, exprs.size())){
				expr = new E_LogicalAnd(expr, e);
			}
		}
		ExprWalker.walk(fv, expr);
		if (enableFilter){
			graphStack.peek().filter(fv.getGraph());
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
		logger.debug(String.valueOf(arg0.getServiceElement()));
		graphStack.peek().service(arg0.getService(), arg0.getSilent());	
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
			graphStack.peek().bind(bv.getGraph());
		}
	}

	@Override
	public void visit(OpJoin arg0) {
		RGraph e1, e2;
		containsJoin = true;
		if (arg0.getRight() instanceof OpBGP){
			e2 = graphStack.pop();
		}
//		else if (arg0.getRight() instanceof OpUnion){
//			e2 = unionStack.pop();
//		}
//		else if (arg0.getRight() instanceof OpLeftJoin){
//			if (enableOptional){
//				e2 = optionalStack.pop();
//			}
//			else{
//				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getRight().getName());
//			}
//		}
//		else if (arg0.getRight() instanceof OpJoin){
//			e2 = joinStack.pop();
//		}
//		else if (arg0.getRight() instanceof OpFilter){
//			if (enableFilter){
//				e2 = filterStack.pop();
//			}
//			else{
//				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getRight().getName());
//			}
//		}
		else{
			e2 = graphStack.pop();
		}
		if (arg0.getLeft() instanceof OpBGP){
			e1 = graphStack.pop();
		}
//		else if (arg0.getLeft() instanceof OpUnion){
//			e1 = unionStack.pop();
//		}
//		else if (arg0.getLeft() instanceof OpLeftJoin){
//			if (enableOptional){
//				e1 = optionalStack.pop();
//			}
//			else{
//				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getLeft().getName());
//			}
//		}
//		else if (arg0.getLeft() instanceof OpJoin){
//			e1 = joinStack.pop();
//		}
//		else if (arg0.getLeft() instanceof OpFilter){
//			if (enableFilter){
//				e1 = filterStack.pop();
//			}
//			else{
//				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getLeft().getName());
//			}
//		}
		else{
			e1 = graphStack.pop();
		}
		e1.join(e2);
		joinStack.add(e1);
		graphStack.add(e1);		
	}

	@Override
	public void visit(OpLeftJoin arg0) {
		RGraph e1, e2;
		containsOptional = true;
		if (enableOptional){
			if (arg0.getRight() instanceof OpBGP){
				e2 = new RGraph(((OpBGP)arg0.getRight()).getPattern().getList());
			}
//			else if (arg0.getRight() instanceof OpUnion){
//				e2 = unionStack.pop();
//			}
//			else if (arg0.getRight() instanceof OpLeftJoin){
//				e2 = optionalStack.pop();
//			}
//			else if (arg0.getRight() instanceof OpFilter){
//				if (enableFilter){
//					e2 = filterStack.pop();
//				}
//				else{
//					throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getRight().getName());
//				}
//			}
			else{
				e2 = graphStack.pop();
			}
			if (arg0.getLeft() instanceof OpBGP){
				e1 = new RGraph(((OpBGP)arg0.getLeft()).getPattern().getList());
			}
//			else if (arg0.getLeft() instanceof OpUnion){
//				e1 = unionStack.pop();
//			}
//			else if (arg0.getLeft() instanceof OpLeftJoin){
//				e1 = optionalStack.pop();
//			}
//			else if (arg0.getLeft() instanceof OpFilter){
//				if (enableFilter){
//					e1 = filterStack.pop();
//				}
//				else{
//					throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getLeft().getName());
//				}
//			}
			else{
				e1 = graphStack.pop();
			}
			e1.optional(e2);
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
//		else if (arg0.getLeft() instanceof OpUnion){
//			e1 = graphStack.pop();
//		}
//		else if (arg0.getLeft() instanceof OpLeftJoin){
//			if (enableOptional){
//				e1 = optionalStack.pop();
//			}
//			else{
//				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getLeft().getName());
//			}
//		}
//		else if (arg0.getLeft() instanceof OpJoin){
//			e1 = joinStack.pop();
//		}
//		else if (arg0.getLeft() instanceof OpFilter){
//			if (enableFilter){
//				e1 = filterStack.pop();
//			}
//			else{
//				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getLeft().getName());
//			}
//		}
		else{
			e1 = graphStack.pop();
		}
		if (arg0.getRight() instanceof OpBGP){
			e2 = graphStack.pop();
		}
//		else if (arg0.getRight() instanceof OpUnion){
//			e2 = graphStack.pop();
//		}
//		else if (arg0.getRight() instanceof OpLeftJoin){
//			if (enableOptional){
//				e2 = optionalStack.pop();
//			}
//			else{
//				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getRight().getName());
//			}
//		}
//		else if (arg0.getRight() instanceof OpJoin){
//			e2 = joinStack.pop();
//		}
//		else if (arg0.getRight() instanceof OpFilter){
//			if (enableFilter){
//				e2 = filterStack.pop();
//			}
//			else{
//				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getRight().getName());
//			}
//		}
		else{
			e2 = graphStack.pop();
		}
		e2.union(e1);
		unionStack.add(e2);
		graphStack.add(e2);	
	}

	@Override
	public void visit(OpDiff arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpMinus arg0) {
		RGraph e1, e2;
		if (arg0.getRight() instanceof OpBGP){
			e2 = new RGraph(((OpBGP)arg0.getRight()).getPattern().getList());
		}
//		else if (arg0.getRight() instanceof OpUnion){
//			e2 = unionStack.pop();
//		}
//		else if (arg0.getRight() instanceof OpLeftJoin){
//			e2 = optionalStack.pop();
//		}
//		else if (arg0.getRight() instanceof OpFilter){
//			if (enableFilter){
//				e2 = filterStack.pop();
//			}
//			else{
//				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getRight().getName());
//			}
//		}
		else{
			e2 = graphStack.pop();
		}
		if (arg0.getLeft() instanceof OpBGP){
			e1 = new RGraph(((OpBGP)arg0.getLeft()).getPattern().getList());
		}
//		else if (arg0.getLeft() instanceof OpUnion){
//			e1 = unionStack.pop();
//		}
//		else if (arg0.getLeft() instanceof OpLeftJoin){
//			e1 = optionalStack.pop();
//		}
//		else if (arg0.getLeft() instanceof OpFilter){
//			if (enableFilter){
//				e1 = filterStack.pop();
//			}
//			else{
//				throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getLeft().getName());
//			}
//		}
		else{
			e1 = graphStack.pop();
		}
		e1.minus(e2);
		graphStack.add(e1);
		
	}

	@Override
	public void visit(OpConditional arg0) {
		throw new UnsupportedOperationException("Unsupported SPARQL feature: "+arg0.getName());
		
	}

	@Override
	public void visit(OpSequence arg0) {
		List<Op> ops = arg0.getElements();
		RGraph r = graphStack.pop();
		for (int i = 1; i < ops.size(); i++) {
			r.join(graphStack.pop());
		}
		graphStack.add(r);	
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
		RGraph r = RGraph.group(arg0);
		List<ExprAggregator> agg = arg0.getAggregators();
		List<RGraph> rGraphs = new ArrayList<RGraph>();
		for (ExprAggregator a : agg) {
			FilterVisitor fv = new FilterVisitor();
			ExprWalker.walk(fv, a);
			rGraphs.add(fv.getGraph());
		}
		RGraph r0 = graphStack.peek();
		r0.aggregation(r,rGraphs);
		r0.groupBy(r);	
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
			if (projectionVars != null) {
				graphStack.peek().project(projectionVars);
			}	
		}
		if (!this.graphURI.isEmpty()){
			containsNamedGraphs = true;
			graphStack.peek().fromGraph(graphURI);
		}
		if (!this.namedGraphURI.isEmpty()){
			containsNamedGraphs = true;
			graphStack.peek().fromNamedGraph(namedGraphURI);
		}
//		if (groupByGraph != null) {
//			graphStack.peek().groupBy(groupByGraph);
//		}
		graphStack.peek().containsPaths = this.containsPaths;
		graphStack.peek().print();
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
	
	public boolean getContainsPaths() {
		return this.containsPaths;
	}
	
	public Op transitiveClosure(Op op) {
		if (op instanceof OpBGP) {
			
		}
		else if (op instanceof OpSequence) {
			List<Op> existingOps = new ArrayList<Op>();
			List<Op> newOps = new ArrayList<Op>();
			List<HashSet<Node>> partitions = new ArrayList<HashSet<Node>>();
			Map<Node,Set<Path>> partitionsPaths = new HashMap<Node,Set<Path>>();
			for (Op o : ((OpSequence) op).getElements()) {
				existingOps.add(o);
				if (o instanceof OpPath) {
					TriplePath tp = ((OpPath) o).getTriplePath();
					Set<Path> paths = new HashSet<Path>();
					Node sub = tp.getSubject();
					Node obj = tp.getObject();
					if (partitionsPaths.containsKey(sub)) {
						paths.addAll(partitionsPaths.get(sub));
						partitionsPaths.put(sub, paths);
					}
					else {
						partitionsPaths.put(sub, paths);
					}
					if (PropertyPathFeatureCounter.minLength(tp.getPath()) == 0) {
						logger.debug(String.valueOf(lengthZeroPaths(tp.getPath())));
						if (sub.isVariable() && obj.isVariable()) {
							paths.addAll(lengthZeroPaths(tp.getPath()));
							partitionsPaths.put(sub, paths);
							if (partitions.isEmpty()) {
								HashSet<Node> newPart = new HashSet<Node>();
								newPart.add(sub);
								newPart.add(obj);
								partitions.add(newPart);
							}
							else {
								for (HashSet<Node> part : partitions) {
									if (part.contains(sub)) {
										part.add(obj);
										break;
									}
									else if (part.contains(obj)) {
										part.add(sub);
										break;
									}
									else {
										HashSet<Node> newPart = new HashSet<Node>();
										newPart.add(sub);
										newPart.add(obj);
										partitions.add(newPart);
									}
								}
							}
						}
					}
				}
				else if (o instanceof OpBGP) {

				}
				else if (o instanceof OpTriple) {

				}
			}
			logger.debug(String.valueOf(partitions));
			for (HashSet<Node> partition : partitions) {
				for (Node sub : partition) {
					for (Node obj : partition) {
						if (sub.equals(obj)) {
							continue;
						}
						for (Path path : partitionsPaths.get(sub)) {
							OpPath opPath1 = new OpPath(new TriplePath(sub, path, obj));
							OpPath opPath2 = new OpPath(new TriplePath(obj, path, sub));
							if (!existingOps.contains(opPath1)){
								newOps.add(opPath1);
							}
							if (!existingOps.contains(opPath2)) {
								newOps.add(opPath2);
							}
						}
					}
				}
			}
			logger.debug(String.valueOf(newOps));
		}
		else if (op instanceof OpUnion) {
			
		}
		else {
			if (op instanceof Op1) {
				return transitiveClosure(((Op1) op).getSubOp());
			}
			return op;
		}
		return op;
	}
	
	public List<Path> lengthZeroPaths(Path path){
		ArrayList<Path> ans = new ArrayList<Path>();
		int length = PropertyPathFeatureCounter.minLength(path);
		if (length == 0) {
			if (path instanceof P_ZeroOrMore1 || path instanceof P_Seq) {
				ans.add(path);
			}
			else if (path instanceof P_Alt) {
				List<Path> left = lengthZeroPaths(((P_Alt) path).getLeft());
				List<Path> right = lengthZeroPaths(((P_Alt) path).getRight());
				ans.addAll(left);
				ans.addAll(right);
			}
			return ans;
		}
		else {
			return ans;
		}
	}
	
	public Op uC2RPQCollapse(Op op) {
		Op op1;
		Op op2 = op;
		do {
			op1 = op2;
			TopDownVisitor tdv = new TopDownVisitor(op1, this.projectionVars);
			op2 = tdv.getOp();
		}
		while (!op1.equals(op2));
		return op1;
	}
	
	public Op UCQNormalisation(Op op) {
		Op op1;
		Op op2 = op;
		do {
			op1 = op2;
			op2 = Transformer.transform(new UCQVisitor(), op1);
		}
		while (!op1.equals(op2));
		return op2;
	}
	
	public Op UCQTransformation(Op op){
		Op opResult = Transformer.transform(new UCQVisitor(), op);
		opResult = UCQNormalisation(opResult);
		opResult = uC2RPQCollapse(opResult);
		opResult = Transformer.transform(new TransformPathFlatternStd(), opResult);
		opResult = Transformer.transform(new TransformSimplify(), opResult);
		opResult = Transformer.transform(new TransformMergeBGPs(), opResult);
		opResult = Transformer.transform(new FilterTransform(), opResult);
		opResult = Transformer.transform(new TransformExtendCombine(), opResult);
		opResult = Transformer.transform(new BGPSort(), opResult);
		return opResult;
	}

}
