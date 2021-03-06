package data;

import java.util.HashSet;

import org.apache.jena.sparql.path.P_Alt;
import org.apache.jena.sparql.path.P_Distinct;
import org.apache.jena.sparql.path.P_FixedLength;
import org.apache.jena.sparql.path.P_Inverse;
import org.apache.jena.sparql.path.P_Link;
import org.apache.jena.sparql.path.P_Mod;
import org.apache.jena.sparql.path.P_Multi;
import org.apache.jena.sparql.path.P_NegPropSet;
import org.apache.jena.sparql.path.P_OneOrMore1;
import org.apache.jena.sparql.path.P_OneOrMoreN;
import org.apache.jena.sparql.path.P_Path0;
import org.apache.jena.sparql.path.P_Path1;
import org.apache.jena.sparql.path.P_Path2;
import org.apache.jena.sparql.path.P_ReverseLink;
import org.apache.jena.sparql.path.P_Seq;
import org.apache.jena.sparql.path.P_Shortest;
import org.apache.jena.sparql.path.P_ZeroOrMore1;
import org.apache.jena.sparql.path.P_ZeroOrMoreN;
import org.apache.jena.sparql.path.P_ZeroOrOne;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathVisitor;
import org.apache.jena.sparql.sse.SSE;

public class PropertyPathFeatureCounter implements PathVisitor {
	
	HashSet<String> features = new HashSet<String>();
	boolean containsStar = false;
	boolean containsNPS = false;
	boolean containsZeroOrOne = false;
	int minLength = 0;
	int maxLength = 0;
	
	public PropertyPathFeatureCounter(Path path) {
		path.visit(this);
		minLength = minLength(path);
		maxLength = maxLength(path);
	}

	@Override
	public void visit(P_Link arg0) {
		
	}

	@Override
	public void visit(P_ReverseLink arg0) {
		
	}

	@Override
	public void visit(P_NegPropSet arg0) {
		features.add("P_NegPropSet");
		containsNPS = true;
	}

	@Override
	public void visit(P_Inverse arg0) {
		arg0.getSubPath().visit(this);
		features.add("P_Inverse");
	}

	@Override
	public void visit(P_Mod arg0) {
		arg0.getSubPath().visit(this);
		features.add("P_Mod");
	}

	@Override
	public void visit(P_FixedLength arg0) {
		arg0.getSubPath().visit(this);
		features.add("P_FixedLength");
	}

	@Override
	public void visit(P_Distinct arg0) {
		arg0.getSubPath().visit(this);
		features.add("P_Distinct");
	}

	@Override
	public void visit(P_Multi arg0) {
		arg0.getSubPath().visit(this);
		features.add("P_Multi");
	}

	@Override
	public void visit(P_Shortest arg0) {
		arg0.getSubPath().visit(this);
		features.add("P_Shortest");
	}

	@Override
	public void visit(P_ZeroOrOne arg0) {
		arg0.getSubPath().visit(this);
		features.add("P_ZeroOrOne");
		containsZeroOrOne = true;
	}

	@Override
	public void visit(P_ZeroOrMore1 arg0) {
		arg0.getSubPath().visit(this);
		features.add("P_ZeroOrMore1");
		containsStar = true;
	}

	@Override
	public void visit(P_ZeroOrMoreN arg0) {
		arg0.getSubPath().visit(this);
		features.add("P_ZeroOrMoreN");
		containsStar = true;
	}

	@Override
	public void visit(P_OneOrMore1 arg0) {
		arg0.getSubPath().visit(this);
		features.add("P_OneOrMore1");
		containsStar = true;
	}

	@Override
	public void visit(P_OneOrMoreN arg0) {
		arg0.getSubPath().visit(this);
		features.add("P_OneOrMoreN");
		containsStar = true;
	}

	@Override
	public void visit(P_Alt arg0) {
		arg0.getLeft().visit(this);
		arg0.getRight().visit(this);
		features.add("P_Alt");
	}

	@Override
	public void visit(P_Seq arg0) {
		arg0.getLeft().visit(this);
		arg0.getRight().visit(this);
		features.add("P_Seq");
	}
	
	public static int minLength(Path path) {
		if (path instanceof P_Path0) {
			return 1;
		}
		else if (path instanceof P_Path1) {
			if (path instanceof P_Inverse) {
				return 1;
			}
			else if (path instanceof P_ZeroOrMore1) {
				return 0;
			}
			else {
				return minLength(((P_Path1) path).getSubPath());
			}
		}
		else if (path instanceof P_Path2) {
			Path left = ((P_Path2) path).getLeft();
			Path right = ((P_Path2) path).getRight();
			if (path instanceof P_Seq) {
				return minLength(left) + minLength(right);
			}
			else if (path instanceof P_Alt) {
				return Math.min(minLength(left), minLength(right));
			}
		}
		return 0;
	}
	
	public int maxLength(Path path) {
		if (path instanceof P_Path0) {
			return 1;
		}
		else if (path instanceof P_Path1) {
			return maxLength(((P_Path1) path).getSubPath());
		}
		else if (path instanceof P_Path2) {
			Path left = ((P_Path2) path).getLeft();
			Path right = ((P_Path2) path).getRight();
			if (path instanceof P_Seq) {
				return maxLength(left) + maxLength(right);
			}
			else if (path instanceof P_Alt) {
				return Math.max(maxLength(left), maxLength(right));
			}
		}
		else {
			return 0;
		}
		return 0;
	}
	
	public static void main(String[] args) {
		Path path2 = SSE.parsePath("(alt <http://xmlns.com/foaf/0.1/p> (reverse <http://xmlns.com/foaf/0.1/q>))");
		PropertyPathFeatureCounter ppfc = new PropertyPathFeatureCounter(path2);
		System.out.println(ppfc.features);
		System.out.println(ppfc.maxLength);
		System.out.println(ppfc.minLength);
	}

}
