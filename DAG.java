import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Model a Directed Acyclic Graph (DAG) which allows nodes (vertices)
// to be specified by name as strings and added to the DAG by
// Specifying their upstream dependencies as a set of string IDs.
// Attempting to introduce a cycle causes an exception to be thrown.
public class DAG {
	
	Map<String, Set<String>> upstreamLinksMap;
	Map<String, Set<String>> downstreamLinksMap;
	
	// Construct an empty DAG
	public DAG() {
		upstreamLinksMap = new HashMap<String, Set<String>>();
		downstreamLinksMap = new HashMap<String, Set<String>>();
	}

	// Produce a string representaton of the DAG which shows the
	// upstream and downstream links in the graph. The format should be
	// as follows:
	//
	// Upstream Links:
	// A1 : [E1, F2, C1]
	// C1 : [E1, F2]
	// BB8 : [D1, C1, RU37]
	// RU37 : [E1]
	// Downstream Links:
	// E1 : [A1, C1, RU37]
	// F2 : [A1, C1]
	// D1 : [BB8]
	// RU37 : [BB8]
	// C1 : [A1, BB8]
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("Upstream Links:\n");
		for (String upkey : upstreamLinksMap.keySet()) {
			str.append(String.format("%4s", upkey) + " : " + upstreamLinksMap.get(upkey) + "\n");
		}
		str.append("Downstream Links:\n");
		for (String downkey : downstreamLinksMap.keySet()) {
			str.append(String.format("%4s", downkey) + " : " + downstreamLinksMap.get(downkey) + "\n");
		}
		return str.toString();
	}

	// Return the upstream links associated with the given ID. If there
	// are no links associated with ID, return the empty set.
	//
	// TARGET COMPLEXITY: O(1)
	public Set<String> getUpstreamLinks(String id) {
		return upstreamLinksMap.get(id)==null?new HashSet<String>():upstreamLinksMap.get(id);
	}

	// Return the downstream links associated with the given ID. If
	// there are no links associated with ID, return the empty set.
	//
	// TARGET COMPLEXITY: O(1)
	public Set<String> getDownstreamLinks(String id) {
		return downstreamLinksMap.get(id)==null?new HashSet<String>():downstreamLinksMap.get(id);
	}

	// Class representing a cycle that is detected on adding to the
	// DAG. Raised in checkForCycles(..) and add(..).
	public static class CycleException extends RuntimeException {
		public CycleException(String msg) {
			super(msg);
		}
	}

	// Add a node to the DAG with the provided set of upstream links.
	// Add the new node to the downstream links of each upstream node.
	// If the upstreamIDs argument is either null or empty, remove the
	// node with the given ID.
	//
	// After adding the new node, check whether it has created any
	// cycles through use of the checkForCycles() method. If a cycle is
	// created, revert the DAG back to its original form so it appears
	// there is no change and raise a CycleException with a message
	// showing the cycle that would have resulted from the addition.
	//
	// TARGET RUNTIME COMPLEXITY: O(N + L)
	// MEMORY OVERHEAD: O(P)
	// N : number of nodes in the DAG
	// L : number of upstream links in the DAG
	// P : longest path in the DAG starting from node id
	public void add(String id, Set<String> upstreamIDs) {
		Set<String> removedSet = null;
		// If the upstreamIDs argument is either null or empty, 
		// remove the node with the given ID.
		if (upstreamIDs == null || upstreamIDs.size() == 0) {
			remove(id);
			return;
		}
		
		// Check if an ID already Exists, Remove it
		if (upstreamLinksMap.containsKey(id)) {
			removedSet = upstreamLinksMap.get(id);
			remove(id);
		}
		
		// Add in Upstream Links
		upstreamLinksMap.put(id, upstreamIDs);
		
		// Add in downstream links
		for (String upstreamID : upstreamIDs) {
			if (downstreamLinksMap.containsKey(upstreamID))
			{
				// Add new Entry in Down Stream Set
				downstreamLinksMap.get(upstreamID).add(id);
			} else {
				// Add new Entry in Down Stream Map
				downstreamLinksMap.put(upstreamID, toSet(id));
			}
		}
		
		// If There is any Cycle in the DAG
		List<String> curPath = new ArrayList<String>();
		curPath.add(id);
		if (checkForCycles(upstreamLinksMap, curPath)) {
			// There is a cycle in the DAG
			remove(id);				// Remove the newly added node
			if (removedSet != null)	// If anything removed, Put it back
				add(id, removedSet);
			// Throw the Exception
			throw new CycleException(curPath.toString());
		}
	}
	/**
	 * Helper Method
	 * @param args
	 * @return
	 */
	private static Set<String> toSet(String... args) {
		Set<String> set = new HashSet<String>();
		for (String s : args) {
			set.add(s);
		}
		return set;
	}

	// Determine if there is a cycle in the graph represented in the
	// links map. List curPath is the current path through the graph,
	// the last element of which is the current location in the graph.
	// This method should do a recursive depth-first traversal of the
	// graph visiting each neighbor of the current element. Each
	// neighbor should be checked to see if it equals the first element
	// in curPath in which case there is a cycle.
	//
	// This method should return true if a cycle is found and curPath
	// should be left to contain the cycle that is found. Return false
	// if no cycles exist and leave the contents of curPath as they were
	// originally.
	//
	// The method should be used during add(..) which will initialize
	// curPath to the new node being added and use the upstream links as
	// the links passed in.
	public static boolean checkForCycles(Map<String, Set<String>> links, List<String> curPath) {
		String lastNode = curPath.get(curPath.size() - 1);
		Set<String> neighbours = links.get(lastNode);
		// if NEIGHBORS is empty or null then 
	    // return false as this path has reached a dead end
		if (neighbours == null || neighbours.size() == 0)
			return false;
		
		for (String nid : neighbours) {
			curPath.add(nid);
			// if the first element in PATH equals NID then
			if (curPath.get(0).equals(nid))
				return true;
			if (checkForCycles(links, curPath) == true)
				return true;
			
			// remove the last element from PATH which should be NID
			curPath.remove(curPath.size() - 1);
		}
		
		return false;
	}

	// Remove the given id by eliminating it from the downstream links
	// of other ids and eliminating its upstream links. If the ID has
	// no upstream dependencies, do nothing.
	//
	// TARGET COMPLEXITY: O(L_i)
	// L_i : number of upstream links node id has
	public void remove(String id) {
		// Remove only if upstream exists
		if (upstreamLinksMap.containsKey(id)) {
			upstreamLinksMap.remove(id);
		}
		
		// Remove from downstream links
		for (Iterator<Map.Entry<String, Set<String>>> it = downstreamLinksMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry<String, Set<String>> entry = it.next();
			Set<String> set = entry.getValue();
			// Remove the Value from Set
			Iterator<String> iterator = set.iterator();
			while (iterator.hasNext()) {
				String element = iterator.next();
			    if (element.equals(id)) {
			        iterator.remove();
			    }
			}
			// Check if Set is Empty
			if (set.size() == 0) {
				// Remove the set as well
				it.remove();
			}
		}
	}
	
	

}