import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.HashMap;

// Represent elements of a binary abstract syntax tree for basic
// spreadsheet formulas like '=A1 + -5.23 *(2+3+A4) / ZD11'.
//
// This class does not require modification.
public class FNode {
  // Type of token at this node. May be one of the following values:
  //   TokenType.Plus
  //   TokenType.Minus
  //   TokenType.Multiply
  //   TokenType.Divide
  //   TokenType.Negate
  //   TokenType.CellID
  //   TokenType.Number
  public TokenType type;

  // Raw data for this node. May be a number, operator, or an id for
  // another cell.
  public String data;

  // Left and right branch of the tree. One or the other may be null
  // if syntax dictates a null child. Notably, for unary negation the
  // left child is the subtree that is negated and the right tree is
  // empty. Examine the implementation of FormulaVisitorImpl method
  // for details.
  public FNode left, right;

  // Construct a node with the given data.
  public FNode(TokenType type, String data, FNode left, FNode right){
    this.type=type;
    this.data=data;
    this.left=left;
    this.right=right;
  }

  // Constructor a node with the given data
  public FNode(TokenType type, FNode left, FNode right){
    this.type=type;
    this.data=type.typeString;
    this.left=left;
    this.right=right;
  }

  // Create a fancy-ish string version of this node. Enters a
  // recursive version of the method
  public String toString(){
    StringBuilder sb = new StringBuilder();
    fancyToString(this, 0, sb);
    return sb.toString();
  }

  // Controls the indentation of the 
  private static int indentOffset = 2;

  // Recursive helper method to traverse the tree and produce a
  // semi-readable string version of the tree.
  private static void fancyToString(FNode node, int indent, StringBuilder sb){
    if(node == null){
      return;
    }
    for(int i=0; i<indent; i++){
      sb.append(' ');
    }
    sb.append(node.data);
    sb.append('\n');
    fancyToString(node.left,  indent+indentOffset, sb);
    fancyToString(node.right, indent+indentOffset, sb);
    return;
  }


  // Class to cause immediate exception percolation on encountering an
  // error in the input of the formula grammar.
  // 
  // Adapted from The Definitive ANTLR 4 Reference, 2nd Edition" section 9.2
  public static class FailOnErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line, int charPos,
                            String msg,
                            RecognitionException e)
    {

      String errmsg = "";
      if(recognizer instanceof Parser){
        java.util.List<String> stack = ((Parser)recognizer).getRuleInvocationStack();
        java.util.Collections.reverse(stack);
        errmsg = stack.toString();
      }
      else if(recognizer instanceof Lexer){
        errmsg = ((Lexer)recognizer).getAllTokens().toString();
      }
      else{
        throw new RuntimeException("WTF^M?");
      }
      String errMsg = String.format("Parse Error:\nRule Stack: %s\nLine %d:%d at %s",
                                    errmsg,line,charPos,offendingSymbol,msg);
      throw new RuntimeException(errMsg);
    }

  }

  // Construct a tree based on the provided formula string. Primary
  // means to construct trees.
  public static FNode parseFormulaString(String formulaStr){
    ANTLRInputStream input = new ANTLRInputStream(formulaStr);
    FormulaLexer lexer = new FormulaLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    FormulaParser parser = new FormulaParser(tokens);

    lexer.removeErrorListeners();                      // remove ConsoleErrorListener
    lexer.addErrorListener(new FailOnErrorListener()); // add failure listener

    parser.removeErrorListeners();                      // remove ConsoleErrorListener
    parser.addErrorListener(new FailOnErrorListener()); // add failure listener

    ParseTree tree = parser.input();
    FNode root = (new FormulaVisitorImpl()).visit(tree);
    return root;
  }    

  // Main method to test construction. Attempts to parse the formula
  // given as the first command line argument and print out its contents as a parsed tree.
  //
  // Example:
  // > java -cp .:formula.jar FNode '=A1 + -5.23 *(2+3+A4) / ZD11'
  public static void main(String[] args) throws Exception {
    if(args.length < 1){
      System.out.println("usage:   java -jar formula.jar 'formula to interpret'");
      System.out.println("Example: java -jar formula.jar '=A1 + -5.23 *(2+3+A4) / ZD11'");
      return;
    }
    FNode root = parseFormulaString(args[0]);
    String rootString = root.toString();
    System.out.println(rootString);
  }


  // Class which visits an ANTLR parse tree for the Formula grammar and
  // builds a tree of FNodes.  Basic usage is as follows such as in a
  // main() method:
  //
  // ANTLRInputStream input = new ANTLRInputStream(formulaStr);
  // FormulaLexer lexer = new FormulaLexer(input);
  // CommonTokenStream tokens = new CommonTokenStream(lexer);
  // FormulaParser parser = new FormulaParser(tokens);
  // ParseTree tree = parser.input();
  // FNode root = (new FormulaVisitorImpl()).visit(tree);
  //
  public static class FormulaVisitorImpl extends FormulaBaseVisitor<FNode>{
    // This series of methods is used to walk the actual parse tree of
    // formula text and produce a binary abstract syntax tree. Each
    // method overrides a method in FormulaBaseVisitor which is called
    // on visiting various parts of the parse tree.
    @Override
    public FNode visitAll(FormulaParser.AllContext ctx) {
      return visit(ctx.plusOrMinus());
    }

    @Override
    public FNode visitPlus(FormulaParser.PlusContext ctx) {
      return new FNode(TokenType.Plus,
                       visit(ctx.plusOrMinus()),visit(ctx.multOrDiv()));
    }
    
    @Override
    public FNode visitMinus(FormulaParser.MinusContext ctx) {
      return new FNode(TokenType.Minus,
                       visit(ctx.plusOrMinus()),visit(ctx.multOrDiv()));
    }

    @Override
    public FNode visitMultiply(FormulaParser.MultiplyContext ctx) {
      return new FNode(TokenType.Multiply,
                       visit(ctx.multOrDiv()),visit(ctx.negate()));
    }

    @Override
    public FNode visitDivide(FormulaParser.DivideContext ctx) {
      return new FNode(TokenType.Divide,
                       visit(ctx.multOrDiv()),visit(ctx.negate()));
    }

    @Override
    public FNode visitNegation(FormulaParser.NegationContext ctx) {
      return new FNode(TokenType.Negate,
                       visit(ctx.negate()),null);
    }

    @Override
    public FNode visitCellID(FormulaParser.CellIDContext ctx) {
      return new FNode(TokenType.CellID,ctx.CELLID().getText(),
                       null,null);
    }

    @Override
    public FNode visitNumber(FormulaParser.NumberContext ctx) {
      return new FNode(TokenType.Number,ctx.NUMBER().getText(),
                       null,null);
    }

    @Override
    public FNode visitBraces(FormulaParser.BracesContext ctx) {
      return visit(ctx.plusOrMinus()); 
    }
  }
  

}
