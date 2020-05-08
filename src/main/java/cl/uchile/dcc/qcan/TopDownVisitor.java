package cl.uchile.dcc.qcan;

import org.apache.jena.query.Query;
import org.apache.jena.query.SortCondition;
import org.apache.jena.sparql.algebra.Algebra;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.core.VarExprList;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class TopDownVisitor {
    Op op;
    List<Var> projectedVars;
    List<Var> namedVars = new ArrayList<Var>();

    public TopDownVisitor(Query q) {
        this.op = Algebra.compile(q);
        this.projectedVars = q.getProjectVars();
        this.namedVars.addAll(q.getProjectVars());
        this.op = visit(this.op);
    }

    public TopDownVisitor(Op op, List<Var> project) {
        this.op = op;
        this.projectedVars = project;
        this.namedVars.addAll(project);
        this.op = visit(this.op);
    }

    public Op getOp() {
        return this.op;
    }

    public Op visit(Op op) {
        if (op instanceof Op1) {
            return visit((Op1) op);
        } else if (op instanceof Op2) {
            return visit((Op2) op);
        } else if (op instanceof OpN) {
            return visit((OpN) op);
        } else {
            return op;
        }
    }

    public Op visit(OpN op) {
        List<Op> ops = op.getElements();
        List<Op> newOps = new ArrayList<Op>();
        for (Op o : ops) {
            newOps.add(visit(o));
        }
        if (op instanceof OpSequence) {
            Op ans = OpSequence.create();
            for (Op o : newOps) {
                ((OpSequence) ans).add(o);
            }
            return ans;
        } else if (op instanceof OpDisjunction) {
            Op ans = OpDisjunction.create();
            for (Op o : newOps) {
                ((OpDisjunction) ans).add(o);
            }
            return ans;
        } else {
            return op;
        }
    }

    public Op visit(Op1 op) {
        if (op instanceof OpExtendAssign) {
            return visit((OpExtendAssign) op);
        } else if (op instanceof OpFilter) {
            return visit((OpFilter) op);
        } else if (op instanceof OpGraph) {
            return visit((OpGraph) op);
        } else if (op instanceof OpGroup) {
            return visit((OpGroup) op);
        } else if (op instanceof OpLabel) {
            return visit((OpLabel) op);
        } else if (op instanceof OpProcedure) {
            return visit((OpProcedure) op);
        } else if (op instanceof OpPropFunc) {
            return visit((OpPropFunc) op);
        } else if (op instanceof OpService) {
            return visit((OpService) op);
        } else if (op instanceof OpDistinct) {
            return visit((OpDistinct) op);
        } else if (op instanceof OpReduced) {
            return visit((OpReduced) op);
        } else if (op instanceof OpList) {
            return visit((OpList) op);
        } else if (op instanceof OpProject) {
            return visit((OpProject) op);
        } else if (op instanceof OpSlice) {
            return visit((OpSlice) op);
        } else if (op instanceof OpTopN) {
            return visit((OpTopN) op);
        }
        return op;
    }

    public Op visit(OpProject op) {
        Op subOp = op.getSubOp();
        List<Var> copy = new ArrayList<Var>();
        copy.addAll(namedVars);
        namedVars.addAll(op.getVars());
        Op ans = new OpProject(visit(subOp), op.getVars());
        this.namedVars = copy;
        return ans;
    }

    public Op visit(OpFilter op) {
        Op subOp = op.getSubOp();
        Op ans;
        List<Var> copy = new ArrayList<Var>();
        Set<Var> filterVars = op.getExprs().getVarsMentioned();
        namedVars.addAll(filterVars);
        ans = OpFilter.filter(op.getExprs(), visit(subOp));
        this.namedVars = copy;
        return ans;
    }

    public Op visit(OpGraph op) {
        Op subOp = op.getSubOp();
        List<Var> copy = new ArrayList<Var>();
        copy.addAll(namedVars);
        if (op.getNode().isVariable()) {
            namedVars.add((Var) op.getNode());
        }
        Op ans = new OpGraph(op.getNode(), visit(subOp));
        this.namedVars = copy;
        return ans;
    }

    public Op visit(OpGroup op) {
        Op subOp = op.getSubOp();
        Op ans;
        List<Var> copy = new ArrayList<Var>();
        copy.addAll(namedVars);
        List<Var> groupVars = op.getGroupVars().getVars();
        namedVars.addAll(groupVars);
        ans = new OpGroup(visit(subOp), op.getGroupVars(), op.getAggregators());
        this.namedVars = copy;
        return ans;
    }

    public Op visit(OpSlice op) {
        Op subOp = op.getSubOp();
        Op ans;
        ans = new OpSlice(visit(subOp), op.getStart(), op.getLength());
        return ans;
    }

    public Op visit(OpOrder op) {
        Op subOp = op.getSubOp();
        Op ans;
        List<Var> copy = new ArrayList<Var>();
        copy.addAll(namedVars);
        for (SortCondition lc : op.getConditions()) {
            namedVars.addAll(lc.getExpression().getVarsMentioned());
        }
        ans = new OpOrder(visit(subOp), op.getConditions());
        this.namedVars = copy;
        return ans;
    }

    public Op visit(OpTopN op) {
        Op subOp = op.getSubOp();
        List<Var> copy = new ArrayList<Var>();
        copy.addAll(namedVars);
        for (SortCondition lc : op.getConditions()) {
            namedVars.addAll(lc.getExpression().getVarsMentioned());
        }
        Op ans = new OpTopN(visit(subOp), op.getLimit(), op.getConditions());
        return ans;
    }

    public Op visit(OpDistinct op) {
        Op subOp = op.getSubOp();
        Op ans = new OpDistinct(visit(subOp));
        return ans;
    }

    public Op visit(OpReduced op) {
        Op subOp = op.getSubOp();
        Op ans = OpReduced.create(visit(subOp));
        return ans;
    }

    public Op visit(OpList op) {
        Op subOp = op.getSubOp();
        Op ans = new OpList(visit(subOp));
        return ans;
    }

    public Op visit(OpLabel op) {
        Op subOp = op.getSubOp();
        Op ans = OpLabel.create(op.getObject(), visit(subOp));
        return ans;
    }

    public Op visit(OpPropFunc op) {
        Op subOp = op.getSubOp();
        Op ans = new OpPropFunc(op.getProperty(), op.getSubjectArgs(), op.getObjectArgs(), visit(subOp));
        return ans;
    }

    public Op visit(OpProcedure op) {
        return op;
    }

    public Op visit(OpService op) {
        Op subOp = op.getSubOp();
        Op ans = new OpService(op.getService(), visit(subOp), op.getServiceElement(), op.getSilent());
        return ans;
    }

    public Op visit(OpExtendAssign op) {
        Op subOp = op.getSubOp();
        Op ans;
        List<Var> copy = new ArrayList<Var>();
        copy.addAll(namedVars);
        VarExprList vExprList = op.getVarExprList();
        for (Entry<Var, Expr> entry : vExprList.getExprs().entrySet()) {
            namedVars.add(entry.getKey());
            namedVars.addAll(entry.getValue().getVarsMentioned());
        }
        if (op instanceof OpExtend) {
            ans = OpExtend.create(visit(subOp), vExprList);
        } else {
            ans = OpAssign.create(visit(subOp), vExprList);
        }
        this.namedVars = copy;
        return ans;
    }

    public Op visit(Op2 op) {
        Op leftOp = op.getLeft();
        Op rightOp = op.getRight();
        if (op instanceof OpUnion) {
            return visit((OpUnion) op);
        } else if (op instanceof OpConditional) {
            return new OpConditional(visit(leftOp), visit(rightOp));
        } else if (op instanceof OpDiff) {
            return OpDiff.create(visit(leftOp), visit(rightOp));
        } else if (op instanceof OpLeftJoin) {
            ExprList exprs = ((OpLeftJoin) op).getExprs();
            return OpLeftJoin.create(visit(leftOp), visit(rightOp), exprs);
        } else if (op instanceof OpMinus) {
            return OpMinus.create(visit(leftOp), visit(rightOp));
        } else if (op instanceof OpJoin) {
            return visit((OpJoin) op);
        } else {
            return op;
        }
    }

    public Op visit(OpUnion op) {
        Op ans = op;
        BGPCollapser bc = new BGPCollapser(op, projectedVars, true);
        ans = bc.transform(op, op.getLeft(), op.getRight());
        return ans;
    }

    public Op visit(OpJoin op) {
        Op ans = op;
        if (isCQ(op)) {
            BGPCollapser bc = new BGPCollapser(op, projectedVars, true);
            ans = bc.transform(op, op.getLeft(), op.getRight());
        }
        return ans;
    }

    public boolean isCQ(OpJoin op) {
        Op leftOp = op.getLeft();
        Op rightOp = op.getRight();
        boolean left = true;
        boolean right = true;
        if (leftOp instanceof OpJoin) {
            left = isCQ((OpJoin) leftOp);
        } else left = leftOp instanceof OpTriple || leftOp instanceof OpBGP || leftOp instanceof OpPath;
        if (rightOp instanceof OpJoin) {
            right = isCQ((OpJoin) rightOp);
        } else right = rightOp instanceof OpTriple || rightOp instanceof OpBGP || rightOp instanceof OpPath;
        return left && right;
    }
}
