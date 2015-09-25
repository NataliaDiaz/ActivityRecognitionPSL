package PSLHAR;//edu.umd.cs.example;

import java.util.ArrayList;

import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.IntegerAttribute;
import edu.umd.cs.psl.model.function.ExternalFunction;

class ConseqPairs18 implements ExternalFunction {

	private ArgumentType[] argTypes = new ArgumentType[]{ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID,ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID};		
	private int maxIncrement;
	private static final ArrayList<Integer> timestamps = new ArrayList<Integer>();
	
	public ConseqPairs18(int maxIncrement) {
		this.maxIncrement = maxIncrement;
		
	}
	
	@Override
	public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
		boolean follows = true; //		argTypes = new ArgumentType[args.length];
		/* Get args */
		try{
			timestamps.clear();
			for (int j = 0; j < args.length; j++) {
				String timestamp = args[j].toString();
				if (! timestamp.equals("")){
					timestamps.add(Integer.parseInt(timestamp));
				}	
			}
			if(timestamps.size()<2){
				System.err.println("Error in Follows function: missing extra input variable ");
				System.exit(-1);
			}
			else{
				int i =1;
				while (i<timestamps.size()){
					if((timestamps.get(i)) != timestamps.get(i-1)+this.maxIncrement)
						return 0.0;
					i = i+2;
				}	
			}
		}
		catch(Exception e){//Catch exception if any
            System.err.println("Error in ConseqPairs18: " + e.getMessage());
        }
		return follows ? 1.0 : 0.0;  // 0 is False; 1 is True
	}

	@Override
	public int getArity() {
		return argTypes.length;
	}

	@Override
	public ArgumentType[] getArgumentTypes() {
		return argTypes;
	}

}