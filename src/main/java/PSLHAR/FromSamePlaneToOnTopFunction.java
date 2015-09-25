package PSLHAR;//edu.umd.cs.example;

import java.util.ArrayList;

import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.IntegerAttribute;
import edu.umd.cs.psl.model.function.ExternalFunction;

class FromSamePlaneToOnTopFunction implements ExternalFunction {

	private ArgumentType[] argTypes = new ArgumentType[]{ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID};		
	private int maxIncrement;
	private static final ArrayList<Integer> timestamps = new ArrayList<Integer>();
	
	public FromSamePlaneToOnTopFunction(int maxIncrement) {
		this.maxIncrement = maxIncrement;
		
	}
	
	@Override
	public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
		boolean passedToBeOnTop = false; //		argTypes = new ArgumentType[args.length];
		/* Get args */
		try{
			timestamps.clear();
			for (int j = 0; j < args.length; j = j+3) {
				String x = args[j].toString();
				String y = args[j+1].toString();
				String z = args[j+2].toString();
				if (! x.equals(""))
					Xpos.add(Integer.parseInt(x));
				if(!y.equals("")
					Ypos.add(Integer.parseInt(y));
				if(!z.equals("")
					Zpos.add(Integer.parseInt(z));					
			}
			if(Xpos.size()<2){
				System.err.println("Error in function: missing extra input variable ");
				System.exit(-1);
			}
			else{
				if(objectsOnTopOfEachOther(pos1, pos2) && objectsInStackedPosition.size()>1){ // SEARCHING MIN 3 OBJECTS OF SAME TYPE ARE DETECTED WITH "STACKED" POSITIONS
                    //System.out.println("__StackingMov__ detected in activ "+activityToMatch);
                    Dictionary o2 = new Hashtable();
                    o2.put("object", object2);
                    o2.put("pos", new float[]{pos2[0], pos2[1], pos2[2]});
                    o2.put("objectID", object2ID);
                    objectsInStackedPosition.add(o2);
                    if(activityToMatch.equals(STACKING))
                        return true;
                }
                else{   // CHECKING FOR UNSTACKED OBJECTS
                    if(objectsOnSameSurface(pos1, pos2) && objectsInUnstackedPosition.size()<1){//state.equals("stacked")){ //
                        Dictionary o1 = new Hashtable();
                        o1.put("object", object1);
                        o1.put("pos", new float[]{pos1[0], pos1[1], pos1[2]});
                        o1.put("objectID", object1ID);
                        objectsInUnstackedPosition.add(o1); // if different type than the previous object?
                        Dictionary o2 = new Hashtable();
                        o2.put("object", object2);
                        o2.put("pos", new float[]{pos2[0], pos2[1], pos2[2]});
                        o2.put("objectID", object2ID);
                        objectsInUnstackedPosition.add(o2);
                    }
                    else{ // TODO: IS CONSIDERING STATES OF GROUPS OF 3 OBJECTS OF SAME TYPE NEEDED?
                        if(objectsOnSameSurface(pos1, pos2) && objectsInUnstackedPosition.size()>1)	
			}
		}
		catch(Exception e){//Catch exception if any
            System.err.println("Error in FollowsFunction: " + e.getMessage());
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