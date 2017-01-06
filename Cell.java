import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// Spreadsheet Cells can be one of three different kinds:
// - Formulas always start with the = sign.  If the 0th character in
//   contents is a '=', use the method
//     FNode root = FNode.parseFormulaString(contents);
//   to create a formula tree of FNodes for later use.
// - Numbers can be parsed as doubles using the
//   Double.parseDouble(contents) method.  
// - Strings are anything else aside from Formulas and Numbers and
//   store only the contents given.
//
// Cells are largely immutable: once the contents of a cell are set,
// they do not change except to reflect movement of upstream dependent
// cells in a formula.  The value of a formula cell may change after
// if values change in its dependent cells. To make changes in a
// spreadsheet, one will typically create a new Cell with different
// contents using the method
//   newCell = Cell.make(contents);
//
// This class may contain nested static subclasses to implement the
// Different kinds of cells.
public class Cell {
	private String cellContents;
	private String cellKind;
	private boolean isError;
	private double numberValue;
	private FNode treeRoot;
	
	public Cell(String cellContents, String cellKind, boolean isError) {
		this.cellContents = cellContents;
		this.cellKind = cellKind;
		this.isError = isError;
		if (this.cellKind.equals("number")) {
			this.numberValue = Double.parseDouble(cellContents);
		} else if (this.cellKind.equals("formula")) {
			treeRoot = FNode.parseFormulaString(cellContents);
		}
	}

	// Factory method to create cells with the given contents linked to
	// the given spreadsheet. The method is static so that one invokes
	// it with:
	//
	// Cell c = Cell.make("=A21*2");
	//
	// The return value may be a subclass of Cell which is not possible
	// with constructors. Call trim() on the contents string to remove
	// whitespace at the beginning and end. If the contents is null or
	// empty, return null. If contents is not valid, a RuntimeException
	// is generated; this may happen if the contents is a formula that
	// cannot be parsed by FNode.parseFormulaString(contents) which
	// raises RuntimeExceptions for invalid syntax such as "=1 ++ 2"
	//
	// If the cell is a formula, it is not possible to evaluate its
	// formula during Cell.make() as other references to other cells
	// cannot be resolved. The formula can only be reliably evaluated
	// after a call to cell.updateValue(cellMap) is made later. Until
	// that time the cell should be in the ERROR state with
	// cell.isError() == true and displayString() == "ERROR" and
	// cell.numberValue() == null.
	public static Cell make(String contents) {
		// Return null for null or empty string
		if (contents == null || contents.trim().equals("")) 
			return null;
		
		try {
			Double.parseDouble(contents.trim());
			// Return a new numbered Cell
			return new Cell(contents.trim(), "number", false);
			
		} catch (Exception e) {
			if (contents.trim().startsWith("=")) {
				// Formula
				return new Cell(contents.trim(), "formula", true);
			} else {
				// string
				return new Cell(contents.trim(), "string", false);
			}
		}
	}

	// Return the kind of the cell which is one of "string", "number",
	// or "formula".
	public String kind() {
		return cellKind;
	}

	// Returns whether the cell is currently in an error state. Cells
	// with kind() "string" and "number" are never in error. Formula
	// cells are in error and show ERROR if their formula involves cells
	// which are blank or have kind "string" and therefore cannot be
	// used to calculate the value of the cell.
	public boolean isError() {
		return isError;
	}

	// Produce a string to display the contents of the cell. For kind()
	// "string", this method returns the original contents of the
	// cell. For kind "number", show the numeric value of the cell with
	// 1 decimal point of accuracy. For formula cells which are in
	// error, return the string "ERROR". Formula cells which are not in
	// error return a string of their numeric value with 1 decimal digit
	// of accuracy which is easiest to produce with the String.format()
	// method.
	//
	// Target Complexity: O(1)
	// Avoid repeated formula evaluation by traversing the formula tree
	// only in updateValue()
	public String displayString() {
		if (kind().equals("string")) {
			// string
			return cellContents;
		} else if (kind().equals("number")) {
			// number
			return String.format("%.1f", numberValue);
		} else {
			// Formula
			if (isError())
				return "ERROR";
			else 
				return String.format("%.1f", numberValue);
		}
	}

	// Return the numeric value of this cell. If the cell is kind
	// "number", this is the double value of its contents. For kind
	// "formula", it is the evaluated value of the formula. For kind
	// "string" return null.
	//
	// Target Complexity: O(1)
	// Avoid repeated formula evaluation by traversing the formula tree
	// only in updateValue()
	public Double numberValue() {
		if (kind().equals("string")) {
			// string
			return null;
		} else if (kind().equals("formula")) {
			if (isError())
				return null;
		}
		return numberValue;
	}

	// Return the raw contents of the cell. For kind() "number" and
	// "string", this is the original contents entered into the cell.
	// For kind() "formula", this is the text of the formula.
	//
	// Target Complexity: O(1)
	public String contents() {
		return cellContents;
	}

	// Update the value of the cell value. If the cell is not a formula
	// (string and number), do nothing. Formulas should re-evaluate the
	// stored formula tree to determine a numeric value. This method
	// may be called when the cell is initially created to give it a
	// numeric value in which case an empty map should be used.
	// Whenever an upstream cell changes value, the housing spreadsheet
	// will call this method to recompute the numeric value to reflect
	// the change. This method should not raise any exceptions if there
	// are problems evaluating the formula due to other unusable cells.
	// It should set the state of this cell to be in error so that a
	// call to isError() returns true. If the cell formula is
	// successfully evaluated, isError() should return false.
	//
	// Target Complexity:
	// O(1) for "number" and "string" cells
	// O(T) for "formula" nodes where T is the number of nodes in the
	// formula tree
	public void updateValue(Map<String, Cell> cellMap) {
		if (kind().equals("string") || kind().equals("number")) {
			return;
		}
		try {
			// Evaluate Formula
			numberValue = evalFormulaTree(treeRoot, cellMap);
			// No Error
			isError = false;
		} catch (Exception e) {
			// Error in Formula
			isError = true;
		}
	}

	// A simple class to reflect problems evaluating a formula tree.
	public static class EvalFormulaException extends RuntimeException {
		public EvalFormulaException(String msg){
			super(msg);
		}
	}

	// Recursively evaluate the formula tree rooted at the given
	// node. Return the computed value. Use the given map to retrieve
	// the number value of cells which appear in the formula. If any
	// cell ID in the formula is unusable (blank, error, string), this
	// method raises an EvalFormulaException.
	//
	// This method is public and static to allow for testing independent
	// of any individual cell but should be used in the
	// updateValue() method to allow individual cells to compute
	// their formula values.
	//
	// Inspect the FNode and TokenType classes to gain insight into what
	// information is available in FNodes to inspect during the
	// post-order traversal for evaluation.
	//
	// Target Complexity: O(T)
	// T: the number of nodes in the formula tree
	public static Double evalFormulaTree(FNode node, Map<String, Cell> cellMap)
	{
		// Recursive post Order to Evaluate the Formula
		return postOrderTraversal(node, cellMap);
	}
	
	/**
	 * Post Order Traversal to Evaluate the Formula
	 * @param node
	 * @param cellMap
	 * @return
	 */
	private static Double postOrderTraversal(FNode node, Map<String, Cell> cellMap) {
		if (node != null) {
			Double leftValue = postOrderTraversal(node.left, cellMap);
			Double rightValue = postOrderTraversal(node.right, cellMap);
			if (node.type == TokenType.Number)
				return Double.parseDouble(node.data);
			else if (node.type == TokenType.CellID) {
				Cell cell = cellMap.get(node.data);
				// Check if there is any error.
				if (cell == null || cell.isError() || cell.contents().equals("") || cell.kind().equals("string")) {
					throw new EvalFormulaException("Error in Formula Evaluation.");
				}
				// Otherwise return Double value
				return cell.numberValue();
			}
			else if (node.type == TokenType.Plus || node.type == TokenType.Minus 
					|| node.type == TokenType.Multiply || node.type == TokenType.Divide) {
				switch(node.type) {
				case Plus:
					return leftValue + rightValue;
				case Divide:
					return leftValue / rightValue;
				case Minus:
					return leftValue - rightValue;
				case Multiply:
					return leftValue * rightValue;
				default:
					break;
				}
			}
			else if (node.type == TokenType.Negate) {
				// Return the Negated value
				return (leftValue * -1);
			}
		}
		return 0.0;
	}

	// Return a set of upstream cells from this cell. Cells of kind
	// "string" and "number" return an empty set. Formula cells are
	// dependent on the contents of any cell whose ID appears in the
	// formula and returns all such ids in a set. For formula cells,
	// this method should call a recursive helper method to traverse the
	// formula tree and accumulate a set of ids in the formula tree.
	//
	// Target Complexity: O(T)
	// T: the number of nodes in the formula tree
	public Set<String> getUpstreamIDs(){
		Set<String> setToReturn = new HashSet<String>();
		if (kind().equals("string") || kind().equals("number")) {
			return setToReturn;
		}
		// Traverse and get IDs
		getUpstreamIDs(treeRoot, setToReturn);
		return setToReturn;
	}
	
	/**
	 * Helper function
	 * @param node
	 * @param idsSet
	 */
	private void getUpstreamIDs(FNode node, Set<String> idsSet) {
		if (node != null) {
			getUpstreamIDs(node.left, idsSet);
			if (node.type == TokenType.CellID)
				idsSet.add(node.data);
			getUpstreamIDs(node.right, idsSet);
		}
	}

}