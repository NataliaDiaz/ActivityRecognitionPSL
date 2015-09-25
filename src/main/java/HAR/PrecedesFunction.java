package HAR;//edu.umd.cs.example;

import java.util.ArrayList;

import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.IntegerAttribute;
import edu.umd.cs.psl.model.function.ExternalFunction;

class PrecedesFunction implements ExternalFunction {

	private ArgumentType[] argTypes = new ArgumentType[]{ArgumentType.UniqueID, ArgumentType.UniqueID};//.Integer,ArgumentType.Integer,	// x-coord
		/*ArgumentType.Integer,ArgumentType.Integer,	// y-coord
		ArgumentType.Integer,ArgumentType.Integer,	// width
		};*/
	private int maxIncrement;
	private static final ArrayList<Integer> timestamps = new ArrayList<Integer>();
	//private static final ArrayList<ArgumentType> argumentTypes = new ArrayList<ArgumentType>();
	//private ArgumentType[] argTypes ;
	
	public PrecedesFunction(int maxIncrement) {
		this.maxIncrement = maxIncrement;
		
	}
	/*public FollowsFunction() {
		FollowsFunction(1);
	}	
	//public FollowsFunction(GroundTerm... args) {
		
	//}*/
	
	@Override
	public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
		boolean follows = true; //		argTypes = new ArgumentType[args.length];
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
				System.err.println("Error in Precedes function: missing extra input variable ");
				System.exit(-1);
			}
			else{
				int i =1;			
				while (i<timestamps.size()){
					if((timestamps.get(i)) != (timestamps.get(i-1)+this.maxIncrement))
						return 0.0;  // False, timestamps do not precede each other with a difference of maxIncrement
					i++;
				}	
				//System.out.println( follows);
				//System.out.println(timestamps);
			}
		}
		catch(Exception e){//Catch exception if any
            System.err.println("Error in PrecedesFunction: " + e.getMessage());
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
		/*ArgumentType[] argTypes = new ArgumentType[argumentTypes.size()];
		int i=0;
		for(ArgumentType e: argumentTypes){
			argTypes[i] = e;
			i++;
		}*/
		return argTypes;// (ArgumentType[]) argumentTypes.toArray();
	}

}