import java.io.*;  
import java.util.*;

public class Fin_Assembler{  
     static HashMap<String,String> opcodes=new HashMap<String,String>();	//opcode table
     static HashMap<String,Integer> l_table=new HashMap<String,Integer>();	//label table
     static HashMap<String,Integer> s_table=new HashMap<String,Integer>();	//symbol table
     static HashMap<String,ArrayList<String>> m_table=new HashMap<String,ArrayList<String>>();	//macro table
     static HashMap<Integer,String> P1Out = new HashMap<Integer,String>();	//Map to be read by pass 2 
     static int locCount;	//location counter
     static int lnum;
     static boolean claUsed=false;
     static boolean stpUsed=false;
     static boolean errInLabel=false;


     //Function to convert statement to machine code
     static String translate(String line, String[] words, String s){
          if(opcodes.containsKey(words[0])){	//if valid opcode
          		if(!words[0].equals("END")){
                    s+=opcodes.get(words[0]);
               	}
          		if (words[0].equals("CLA")){
          			s+="00000000";
          			claUsed=true;
          		}
          		if (words[0].equals("STP")){
          			s+="00000000";
          			stpUsed=true;
          		}
               if(words[0].indexOf("BR")<0 && !words[0].equals("STP") && !words[0].equals("CLA")){
                    int num;
                    try{
                         if(!Character.isDigit(words[1].charAt(0))) num = s_table.get(words[1]);
                         else num = Integer.valueOf(words[1]);
                         s+=String.format("%8s", Integer.toBinaryString(num)).replace(' ', '0');
                    }catch(NullPointerException e){
                    }catch(ArrayIndexOutOfBoundsException e){
                         System.out.println("ERROR(In Macro): The operation "+words[0]+" requires more operands than provided.");
                         System.exit(0);
                    }              
               }
               if(words[0].indexOf("BR")>=0){
                    s+=String.format("%8s", Integer.toBinaryString(Integer.valueOf(l_table.get(words[1])))).replace(' ', '0');
               }
          }
          return s;
     }

     //function to check for errors in the 1st pass
     static boolean ErrorCheck(String line, String[] word, boolean mdef, boolean err, int lnum){
           if(opcodes.containsKey(word[0])){
               if (line.indexOf("BR")>=0){  //If a branch instruction, add label to l_table
                    try{
                         if(!l_table.containsKey(word[1])){
                              l_table.put(word[1],-1);
                         }
                    } catch(ArrayIndexOutOfBoundsException e){
                         err = true;
                         System.out.println("ERROR(line "+lnum+"): Instruction incomplete, branch instruction does not specify a label.");
                    }
               }
               else if(word[0].equals("DIV")){
                    if(!s_table.containsKey("R1") || !s_table.containsKey("R2")){
                         s_table.put("R1",-2);
                         s_table.put("R2",-2);
                    }
               }
               else if(!word[0].equals("CLA") && !word[0].equals("STP")){ 
                    try{
                         if(!mdef && !Character.isDigit(word[1].charAt(0)) && !s_table.containsKey(word[1])){
                              s_table.put(word[1],-2);
                         }
                    }catch(ArrayIndexOutOfBoundsException e){
                         err = true;
                         System.out.println("ERROR(line "+lnum+"): The operation "+word[0]+" requires more operands than provided.");
                    }
               }
               if((line.indexOf("CLA")>=0 && line.indexOf("CLA")<2 )|| (line.indexOf("STP")>=0) && line.indexOf("STP")<2){
                    if(word.length>1){
                         err = true;
                         System.out.println("ERROR(line "+lnum+"): Too many operands for the given command "+word[0]);
                    }
               }
               else if(word.length>2){           
                    err = true;
                    System.out.println("ERROR(line "+lnum+"): Too many operands for the given command "+word[0]);
               }
          }

          if(!opcodes.containsKey(word[0]) && !m_table.containsKey(word[0])){
               err = true;
               System.out.println("ERROR (Line "+lnum+"): Command not found- "+word[0]);
          }
          return err;
     }

     public static void main(String args[]){    
          opcodes.put("CLA","0000");
          opcodes.put("LAC","0001");
          opcodes.put("SAC","0010");
          opcodes.put("ADD","0011");
          opcodes.put("SUB","0100");
          opcodes.put("BRZ","0101");
          opcodes.put("BRN","0110");
          opcodes.put("BRP","0111");
          opcodes.put("INP","1000");
          opcodes.put("DSP","1001");
          opcodes.put("MUL","1010");
          opcodes.put("DIV","1011");
          opcodes.put("STP","1100");
          boolean err=false;

          /////////////////////////////////////////////////////// PASS ONE ////////////////////////////////////////////////////////
          locCount=0;
          lnum=1;
          try{
               BufferedReader reader=new BufferedReader(new FileReader("input.txt"));    
               String line=reader.readLine();
               String[] w=line.split("\\s");
               ArrayList<String> l = new ArrayList<String>(Arrays.asList(w));
               l.removeIf(n->(n.equals(" ") || n.equals("")));
               w = l.toArray(new String[0]);
               if(w[0].equals("START")){
                    try{
                         locCount = Integer.valueOf(w[1]); 
                    }catch(ArrayIndexOutOfBoundsException e){
                         err=true;
                         System.out.println("ERROR(line "+lnum+"): Command START requires a parameter");
                    }catch(NumberFormatException e){
                         err=true;
                         System.out.println("ERROR(line "+lnum+"): Command START takes only integer arguments");
                    }
                    if(w.length>2){
                         err = true;
                         System.out.println("ERROR(line "+lnum+"): Too many parameters for the command START");
                    }
                    lnum++;
                    line = reader.readLine();
               }
               boolean mdef = false;
               ArrayList<String> macdef = new ArrayList<String>();
               while(line != null && !line.equals("END")) {
                    if (line.indexOf("//")>=0){		//ignore comments in line
                         line=line.substring(0,line.indexOf("//"));
                    }      
                    
                    if (line.indexOf("MACRO")>=0){		//If macro definition, add to m_table   
                         m_table.put(line.substring(0,line.indexOf("MACRO")-1),macdef);
                         macdef.add(line.substring(line.indexOf("MACRO")+6,line.length()));
                         mdef=true;
                         line = reader.readLine();
                         lnum++;
                         continue;
                    }
                    if(line.equals("MEND")){
                         mdef=false;
                         line = reader.readLine();
                         macdef = new ArrayList<String>();
                         lnum++;
                         continue;
                    }
                    if(mdef==true){
                         String[] word=line.split("\\s");
                         ArrayList<String> list = new ArrayList<String>(Arrays.asList(word));
                         list.removeIf(n->(n.equals(" ") || n.equals("")));
                         word = list.toArray(new String[0]);
                         if(word.length==0){ //If empty line
                              line = reader.readLine();
                              lnum++;
                              continue;
                         }
                         macdef.add(line);
                         err = ErrorCheck(line, word, mdef, err, lnum);
                         line = reader.readLine();
                         lnum++;
                         continue;
                    }

                    if (line.indexOf(":")>=0){  //If label, save address to l_table
                         String lb = line.substring(0,line.indexOf(":"));
                         lb = lb.replaceAll(" ","");
                         if(l_table.containsKey(lb) && l_table.get(lb)!=-1){
                              err = true;
                              System.out.println("ERROR(line "+lnum+"): Label over-writing is not allowed, "+lb+" is being defined more than once.");
                         }
                         else l_table.put(lb,locCount);
                         line = line.substring(line.indexOf(":")+1);
                         if(line==null)locCount++;
                    }

                    String[] word=line.split("\\s");
                    ArrayList<String> list = new ArrayList<String>(Arrays.asList(word));
			   	list.removeIf(n->(n.equals(" ") || n.equals("")));
			   	word = list.toArray(new String[0]);

                    if(word.length==0){ //If empty line
                         line = reader.readLine();
                         lnum++;
                         continue;
                    }
                    
                    if(m_table.containsKey(word[0])){		//If macro call substitute along with actual parameters
                       	String[] m_arr = m_table.get(word[0]).get(0).split("\\s");
                       	list = new ArrayList<String>(Arrays.asList(m_arr));
			  			list.removeIf(n->(n.equals(" ") || n.equals("")));
			   			m_arr = list.toArray(new String[0]);
			   		for(int i=1;i<word.length;i++){
			   			if(!Character.isDigit(word[i].charAt(0)) && !s_table.containsKey(word[i])){
                            s_table.put(word[i],-2);
			   			}
			   		}
                        if (m_arr.length!=word.length-1){ 
                              err = true;
                         	System.out.println("ERROR(line "+lnum+"): "+word[0]+ " requires "+m_arr.length+" parameter(s),"+(word.length-1)+" were given.");
                        }
                        else{
                         	  String[] vpar= m_table.get(word[0]).get(0).split("\\s");
                         	  list = new ArrayList<String>(Arrays.asList(vpar));
			  				  list.removeIf(n->(n.equals(" ") || n.equals("")));
			   				  vpar = list.toArray(new String[0]);
                         	  boolean usesp = false;
                         	  for(int j=1;j<m_table.get(word[0]).size();j++){
                         	  	usesp = false;
                         	  	String oline = m_table.get(word[0]).get(j);
                         	  	for(int i=0;i<vpar.length;i++){
                         	  		if(oline.indexOf(vpar[i])>=0){
                         	  			line = oline.replaceAll(" "+vpar[i]," "+word[i+1]);
                         	  			usesp = true;
                         	  		}
                         	  	}
                         	  	if(!usesp){
                         	  		String[] wo=oline.split("\\s");
                         	  		ArrayList<String> li = new ArrayList<String>(Arrays.asList(wo));
			  				  		li.removeIf(n->(n.equals(" ") || n.equals("")));
			   				  		wo = li.toArray(new String[0]);
			   				  		if(opcodes.containsKey(wo[0]) && !wo[0].equals("CLA") && !wo[0].equals("STP")){
			   				  			s_table.put(wo[1],-2);
			   				  		}
                         	  		line = oline;
                         	  	}
                         	     if(j<m_table.get(word[0]).size()-1){
                         	  		P1Out.put(locCount, line);
                         	  		locCount+=1;
                         	   }
                         	}
                        }
                    }

                    err = ErrorCheck(line, word, mdef, err, lnum);

                    P1Out.put(locCount, line);              
                    locCount+=1;
                    lnum++;
                    line=reader.readLine();
               } 
               reader.close();
          }
          catch(FileNotFoundException f){
               System.out.println("Input code file not found. Please check the name and try again.");
               System.exit(0);
          }
          catch(IOException e){
               e.printStackTrace();
          }
          ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
          //Pass One Outputs
          System.out.println("Label Table:");
          l_table.entrySet().forEach(entry->{
          	if (entry.getValue()==-1){
          		System.out.println("ERROR: LABEL "+entry.getKey()+" used but not defined.");
          		errInLabel=true;
          	}
          	else{
          		System.out.println(entry.getKey()+"---->"+entry.getValue());}
          	});
          err=err||errInLabel;
          System.out.println("Symbol Table:");
          s_table.entrySet().forEach(entry->{
          	if(entry.getValue()==-2){
          		s_table.put(entry.getKey(),locCount++);
          	}
          	System.out.println(entry.getKey()+"---->"+entry.getValue());
          });

          System.out.println("Macro table:");
          m_table.entrySet().forEach(entry->{
               System.out.println(entry.getKey()+"---->");
               for(int i=0;i<entry.getValue().size();i++){
                    System.out.println(entry.getValue().get(i));
               }
          }); 
          if(err) {
          	System.out.println("Output file not created.");
	          System.exit(0);
          }

                    if (P1Out.size()==0){
          	System.out.println("ERROR: No code found.");
          	System.out.println("Output file not created.");
          	System.exit(0);
          }
          System.out.println("Program as stored in memory:");
          P1Out.entrySet().forEach(entry->{
               System.out.println(entry.getKey()+"->"+entry.getValue());
          });

          /////////////////////////////////////////////////////// PASS TWO ///////////////////////////////////////////////////////
          try{
               BufferedWriter bw = new BufferedWriter(new FileWriter("output.txt")); 
               for(HashMap.Entry<Integer, String> entry : P1Out.entrySet()){  
	                   String line = entry.getValue();
	                   boolean mend=true;
	                    if (line.indexOf("//")>=0){
	                         line=line.substring(0,line.indexOf("//"));
	                    }
	                    String[] words=line.split("\\s");
	                    List<String> list = new ArrayList<String>(Arrays.asList(words));
			   		list.removeIf(n->(n.equals(" ") || n.equals("")));
			   		words = list.toArray(new String[0]);
	                    String s="";
	                    
	                    s=translate(line,words,s);

	                    bw.write(s+'\n');
               }    
               bw.close();
          }
          catch(IOException e){
               e.printStackTrace();
          } 
          if (claUsed==false){
          	System.out.println("WARNING: CLA not used, the program may contain an error");
          }if (stpUsed==false){
          	System.out.println("WARNING: STP not used, the program may contain an error");
          }
          ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }
}