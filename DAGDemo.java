import java.util.*;
import java.io.*;

public class DAGDemo {
	public static Set<String> toSet(String... args) {
		Set<String> set = new HashSet<String>();
		for (String s : args) {
			set.add(s);
		}
		return set;
	}

	public static void main(String args[]) {
		PrintStream o = System.out;

		o.println("DEMO:");
		o.println("=====");

		DAG dag = new DAG();
		o.println(dag);
		dag.add("A1",toSet("B1"));
		o.println(dag);
		
		dag.add("A1", toSet("B1", "C1", "D1"));
		o.println(dag);
		dag.add("B1", toSet("C1", "D1"));
		o.println(dag);
		dag.add("C1", toSet("E1", "F2"));
		o.println(dag);

		dag.remove("F2");
		o.println(dag);

		dag.remove("A1");
		o.println(dag);

		dag.add("A1", toSet("E1", "F2", "C1"));
		o.println(dag);

		try {
			dag.add("F2", toSet("B1", "E2"));
		} catch (Exception e) {
			o.println("Cycle detected: " + e.getMessage());
		}
		o.println(dag);

	}
}
