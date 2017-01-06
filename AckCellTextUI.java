import java.util.*;
import java.io.*;


public class AckCellTextUI{

  public static void displaySheet(Spreadsheet sheet){
    // System.out.println("SHEET");
    // System.out.println("-----");
    System.out.println();
    System.out.println(sheet.toString());
    // System.out.println("Downstream Map");
    // System.out.println(sheet.getDownstreamMap());
    // System.out.println("Upstream Map");
    // System.out.println(sheet.getUpstreamMap());

  }

  private static String slurp(String fname) throws Exception{
    return new Scanner(new File(fname), "UTF-8").useDelimiter("\\Z").next();
  }

  public static void echo(String s){
    System.out.println(s);
  }


  public static void main(String args[]){
    Spreadsheet sheet = new Spreadsheet();

    System.out.println("AckCell Spreadsheet v0.1");

    System.out.println("Enter commands as follows");
    System.out.println("-------------------------");
    System.out.println("set id contents :  Set cell id to given contents");
    System.out.println("delete id       :  Delete contents cell with given id");
    System.out.println("save filename   :  Save the current sheet to named file");
    System.out.println("load filename   :  Discard the current sheet and load from the named file");
    System.out.println("quit            :  Quit program");
    System.out.println();

    String command = "";
    Scanner input = new Scanner(System.in);
    while(!command.equals("quit")){
      displaySheet(sheet);

      System.out.print("> ");
      try{
        command = input.next();
      }
      catch(NoSuchElementException e){
        command = "quit";
      }
      if(command.equals("quit")){
        echo(command);
        System.out.println("Quitting...");
      }
      else if(command.equals("set")){
        String id = input.next();
        String contents = input.nextLine().trim();
        try{
          echo(String.format("%s %s %s",command,id,contents));
          sheet.setCell(id,contents);
        }
        catch(Exception e){
          System.out.printf("Could not set cell %s to %s:\n%s\n",
                            id,contents,e.getMessage());
        }
      }
      else if(command.equals("delete")){
        String id = input.next();
        sheet.deleteCell(id);
      }
      else if(command.equals("save")){
        String filename = input.nextLine().trim();
        echo(String.format("%s %s",command,filename));
        System.out.printf("Saving sheet to '%s' filename... ",filename);
        try{
          PrintWriter out = new PrintWriter(new File(filename));
          String saveString = sheet.toSaveString();
          out.print(saveString);
          out.close();
          System.out.printf("done.\n");
        }
        catch(Exception e){
          System.out.printf("\nCould not save sheet: %s\n",e.getMessage());
        }
      }
      else if(command.equals("load")){
        String filename = input.nextLine().trim();
        echo(String.format("%s %s",command,filename));
        System.out.printf("Loading sheet to '%s' filename... ",filename);
        try{
          String saveString = slurp(filename);
          sheet = Spreadsheet.fromSaveString(saveString);
          System.out.printf("done.\n");
        }
        catch(Exception e){
          System.out.printf("\nCould not load sheet: %s\n",e.getMessage());
        }
      }
      else{
        echo(String.format("%s",command));
        System.out.printf("Unrecognized command '%s'\n",command);
      }
    }
    System.out.println("Bye!");
  }
}
      
    
