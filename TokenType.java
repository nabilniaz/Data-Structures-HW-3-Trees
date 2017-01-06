// Types of tokens that can appear in a formula 
public enum TokenType {
	Plus("+"), 
	Minus("-"), 
	Multiply("*"), 
	Divide("/"), 
	Negate("negate"), 
	CellID("CellID"), 
	Number("Number");

	// String representation of the token
	public String typeString;

	TokenType(String s) {
		this.typeString = s;
	}
}
