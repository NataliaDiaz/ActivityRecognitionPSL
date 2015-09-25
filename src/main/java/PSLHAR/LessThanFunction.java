package PSLHAR;//edu.umd.cs.example;

import java.util.ArrayList;

import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.IntegerAttribute;
import edu.umd.cs.psl.model.function.ExternalFunction;

class LessThanFunction implements ExternalFunction {

	private ArgumentType[] argTypes = new ArgumentType[]{ArgumentType.UniqueID, ArgumentType.UniqueID};
	private static final ArrayList<Integer> timestamps = new ArrayList<Integer>();
	//private static final ArrayList<ArgumentType> argumentTypes = new ArrayList<ArgumentType>();
	//private ArgumentType[] argTypes ;
	
	public LessThanFunction() {	}
		
	@Override
	public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
		boolean follows = false; //		argTypes = new ArgumentType[args.length];
		/* Get args */
		try{
			timestamps.clear();
			for (int j = 0; j < args.length; j++) {
				String timestamp = args[j].toString();
				if (! timestamp.equals("")){
					timestamps.add(Integer.parseInt(timestamp));//((IntegerAttribute) args[j]).getValue().intValue());
				}				
				//System.out.println("Adding element to precedesFunction: "+((IntegerAttribute) args[j]).getValue().toString());
				//argumentTypes.add(ArgumentType.Integer);
				//argTypes[j] = ArgumentType.Integer;///* = new ArgumentType[]{args.length};/* = new ArgumentType[]
			}
			if(timestamps.size()<2){
				System.err.println("Error in LessThan function: missing extra input variable ");
				System.exit(-1);
			}
			else{
				if(timestamps.get(0) < timestamps.get(1))
					return 1.0;
				else
					return 0.0;  // False
			}
		}
		catch(Exception e){//Catch exception if any
            System.err.println("Error in LessThanFunction: " + e.getMessage());
        }
		//System.out.println("Precedes evaluates to: "+follows);
		return follows ? 1.0 : 0.0;  // 0 is False; 1 is True 
	}

	@Override
	public int getArity() {
		return argTypes.length;//argumentTypes.size();
	}

	@Override
	public ArgumentType[] getArgumentTypes() {
		return argTypes;// (ArgumentType[]) argumentTypes.toArray();
	}

}