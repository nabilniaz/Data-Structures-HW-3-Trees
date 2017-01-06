import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

// Basic model for a spreadsheet.
public class Spreadsheet {
	
	Map<String, Cell> spreadsheetCells;
	DAG dag;
	
	// Construct a new empty spreadsheet
	public Spreadsheet() {
		// new HashMap for cells
		spreadsheetCells = new HashMap<String,Cell>();
		// DAG to store dependencies
		dag = new DAG();
	}

	// Return a string representation of the spreadsheet. This should
	// show a table of the cells ids, values, and contents along with
	// the upstream and downstream links between cells. Ensure that
	// StringBuilder and iterators over various maps are used to
	// efficiently construct the string. The expected format is as
	// follows.
	//
	// ID | Value | Contents
	// -------+--------+---------------
	// A1 | 5.0 | '5'
	// D1 | 4.0 | '=4'
	// C1 | 178.0 | '=22*A1 + 17*D1'
	// B1 | hi | 'hi'
	//
	// Cell Dependencies
	// Upstream Links:
	// C1 : [A1, D1]
	// Downstream Links:
	// A1 : [C1]
	// D1 : [C1]
	//
	public String toString() {
		StringBuilder strToRet = new StringBuilder();
		strToRet.append("    ID |  Value | Contents\n");
		strToRet.append("-------+--------+---------------\n");
		for (Iterator<Map.Entry<String, Cell>> it = spreadsheetCells.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry<String, Cell> entry = it.next();
			String cellID = entry.getKey();
			Cell cell = entry.getValue();
			strToRet.append(String.format("%6s", cellID) + " |" +  
							String.format("%7s", cell.displayString()) + " | '" + cell.contents() + "'\n");
			
		}
		strToRet.append("\nCell Dependencies\n");
		strToRet.append(dag);
		
		return strToRet.toString();
	}

	// Produce a saveable string of the spreadsheet. A reasonable format
	// is each cell id and its contents on a line. You may choose
	// whatever format you like so long as the spreadsheet can be
	// completely recreated using the fromSaveString(s) method.
	public String toSaveString() {
		StringBuilder strToRet = new StringBuilder();
		for (Iterator<Map.Entry<String, Cell>> it = spreadsheetCells.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry<String, Cell> entry = it.next();
			String cellID = entry.getKey();
			Cell cell = entry.getValue();
			strToRet.append(cellID + ":" + cell.contents() + "\n");
		}
		return strToRet.toString();
	}

	// Load a spreadsheet from the given save string. Typical
	// implementations will create an empty spreadsheet and repeatedly
	// read input from the provided string setting cells based on the
	// contents read.
	public static Spreadsheet fromSaveString(String s) {
		// New Sheet to Return
		Spreadsheet newSheet = new Spreadsheet();
		// Scanner to Read the String
		Scanner sc = new Scanner(s);
		// Loop to read all lines
		while(sc.hasNext()) {
			String line = sc.nextLine();
			String[] id_content = line.split(":");
			// Set a Cell with Cell id and it's content
			newSheet.setCell(id_content[0], id_content[1]);
		}
		sc.close();
		return newSheet;
	}

	// Check if a cell ID is well formatted. It must match the regular
	// expression
	//
	// ^[A-Z]+[1-9][0-9]*$
	//
	// to be well formatted. If the ID is not formatted correctly, throw
	// a RuntimeException. The str.matches(..) method is useful for
	// this method.
	public static void verifyIDFormat(String id) {
		if ( ! id.matches("^[A-Z]+[1-9][0-9]*$") ) {
			throw new RuntimeException("Invalid ID Format.");
		}
	}

	// Retrieve a string which should be displayed for the value of the
	// cell with the given ID. Return "" if the specified cell is empty.
	public String getCellDisplayString(String id) {
		Cell cell = spreadsheetCells.get(id);
		if (cell.kind().equals("string")) {
			return cell.contents();
		} else {
			return cell.displayString();
		}
	}

	// Retrieve a string which is the actual contents of the cell with
	// the given ID. Return "" if the specified cell is empty.
	public String getCellContents(String id) {
		Cell cell = spreadsheetCells.get(id);
		if (!cell.contents().equals(""))
			return cell.contents();
		return "";
	}

	// Delete the contents of the cell with the given ID. Update all
	// downstream cells of the change. If specified cell is empty, do
	// nothing.
	public void deleteCell(String id) {
		Cell cell = spreadsheetCells.get(id);
		if (cell.contents().equals(""))
			return;
		
		// Remove the cell from the internal map
		spreadsheetCells.remove(id);
		
		// Remove the cell from the internal DAG
		dag.remove(id);
		
		// notify any downstream cells
		notifyDownstreamOfChange(id);
		
	}

	// Set the given cell with the given contents. If contents is "" or
	// null, delete the cell indicated.
	public void setCell(String id, String contents) {
		// If contents is "" or null, delete the cell indicated.
		if (contents == null || contents.trim().equals("")) {
			deleteCell(id);
		}
		
		// Delete any contents associated with B6 in the map from ids to Cells
//		spreadsheetCells.remove(id);
		// Create a new cell with the contents 
		Cell cell = Cell.make(contents);
		if (cell == null)
			return;
		// Extract the upstream dependencies for the cell
		Set<String> upstreamIDs = cell.getUpstreamIDs();
		// Attempt to add cell to the spreadsheet's DAG with its upstream dependencies 
		try {
			dag.add(id, upstreamIDs);
		} catch (Exception e) {
			throw e;
		}
		
		// Associate in the spreadsheet's map cell with the newly created Cell
		spreadsheetCells.put(id, cell);
		
		// Update the value of that cell passing in the spreadsheet's ID / Cell map
		cell.updateValue(spreadsheetCells);
		
		// Notify any cells that are downstream from cell that its contents have changed 
		notifyDownstreamOfChange(id);
	
	}

	// Notify all downstream cells of a change in the given cell.
	// Recursively notify subsequent cells. Guaranteed to terminate so
	// long as there are no cycles in cell dependencies.
	public void notifyDownstreamOfChange(String id) {
		// Get the DownStream Links
		Set<String> downLinks = dag.getDownstreamLinks(id);
		// Iterate on them and Call updateValue for all of them
		Iterator<String> iterator = downLinks.iterator();
		while (iterator.hasNext()) {
			String cellID = iterator.next();
			Cell cell = spreadsheetCells.get(cellID);
			cell.updateValue(spreadsheetCells);
			// Recursive Call to Down Links of this Cell
			notifyDownstreamOfChange(cellID);
		}
	}

}