package cl.uchile.dcc.qcan.externalfix;


import org.apache.jena.sparql.expr.*;

public class ExprWalker {
    ExprVisitor visitor;

    public ExprWalker(ExprVisitor visitor) {
        this.visitor = visitor;
    }

    public void walk(Expr expr) {
        expr.visit(this.visitor);
    }

    public static void walk(ExprVisitor visitor, Expr expr) {
        if (expr != null) {
            expr.visit(new ExprWalker.WalkerBottomUp(visitor));
        }
    }

    public static class WalkerBottomUp extends ExprWalker.Walker {
        private WalkerBottomUp(ExprVisitor visitor) {
            super(visitor, false);
        }
    }

    public static class WalkerTopDown extends ExprWalker.Walker {
        private WalkerTopDown(ExprVisitor visitor) {
            super(visitor, true);
        }
    }

    static class Walker extends ExprVisitorFunction {
        ExprVisitor visitor;
        boolean topDown;

        private Walker(ExprVisitor visitor, boolean topDown) {
            this.topDown = true;
            this.visitor = visitor;
            this.topDown = topDown;
        }

        protected void visitExprFunction(ExprFunction func) {
            if (this.topDown) {
                func.visit(this.visitor);
            }

            for (int i = 1; i <= func.numArgs(); ++i) {
                Expr expr = func.getArg(i);
                if (expr == null) {
                    NodeValue.nvNothing.visit(this);
                } else {
                    expr.visit(this);
                }
            }

            if (!this.topDown) {
                func.visit(this.visitor);
            }

        }

        public void visit(ExprFunctionOp funcOp) {
            funcOp.visit(this.visitor);
        }

        public void visit(NodeValue nv) {
            nv.visit(this.visitor);
        }

        public void visit(ExprVar v) {
            v.visit(this.visitor);
        }

        public void visit(ExprAggregator eAgg) {
            eAgg.visit(this.visitor);
        }

    }
}