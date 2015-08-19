package edu.umd.cs.example;

import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.function.ExternalFunction;

class RepeatsSubsequenceMinTimes  implements ExternalFunction {
		
		@Override
		public int getArity() {
			return 2;
		}
		
		@Override
		public ArgumentType[] getArgumentTypes() {
			return new ArgumentType[] {ArgumentType.String, ArgumentType.String};
		}
		
		@Override
		public double getValue(ReadOnlyDatabase db, GroundTerm... args) {
			return args[0].toString() == (args[1].toString()+1) ? 1.0 : 0.0;
		}
}