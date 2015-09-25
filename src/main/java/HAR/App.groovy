package HAR;

//println "This source file is a place holder for the tree of groovy and java sources for your PSL project."
println "PSL for HUMAN ACTIVITY RECOGNITION"

import java.text.DecimalFormat;

import com.google.common.collect.Iterables;

import edu.umd.cs.psl.groovy.*;
import edu.umd.cs.psl.ui.functions.textsimilarity.*;
import edu.umd.cs.psl.ui.loading.InserterUtils;
import edu.umd.cs.psl.util.database.Queries;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.UniqueID;
import edu.umd.cs.psl.model.argument.Variable;
import edu.umd.cs.psl.model.predicate.Predicate;
import edu.umd.cs.psl.model.argument.type.*;
import edu.umd.cs.psl.model.atom.GroundAtom;
import edu.umd.cs.psl.model.atom.RandomVariableAtom;
import edu.umd.cs.psl.model.function.ExternalFunction;
import edu.umd.cs.psl.model.predicate.type.*;
import edu.umd.cs.psl.application.inference.MPEInference;
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE;
import edu.umd.cs.psl.config.*;
import edu.umd.cs.psl.database.DataStore;
import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.database.Partition;
import edu.umd.cs.psl.database.DatabasePopulator;
import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore;
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver;
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type;
//import edu.umd.cs.psl.HAR.FollowsFunction;

////////////////////////// initial setup ////////////////////////
ConfigManager cm = ConfigManager.getManager()
ConfigBundle config = cm.getBundle("ActivityRecognition");

def defaultPath = System.getProperty("java.io.tmpdir")
String dbpath = config.getString("dbpath", defaultPath + File.separator + "ActivityRecognition")
DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, dbpath, true), config)
PSLModel m = new PSLModel(this, data);
double initialWeight= 10.0;
boolean sq = true;
////////////////////////// predicate declaration ////////////////////////

//target predicate
//m.add function: "similarName"  , implementation: new LevenshteinSimilarity(); // Compare efficiency of predicate vs function PRECEDES
//m.add function: "sameObject", implementation: new MyStringSimilarity();//m.add predicate: "timestamp", types: [ArgumentType.UniqueID]
m.add function: "precedes"  , implementation: new PrecedesFunction(1); // Compare efficiency of predicate vs function PRECEDES
m.add function: "follows"  , implementation: new FollowsFunction(1); // Parameter n indicates max nr of time units allowed in between timestamps to the function to return true
m.add function: "follows4", implementation: new FollowsFunction4(1);
m.add function: "follows5"  , implementation: new FollowsFunction5(1); 
m.add function: "follows6", implementation: new FollowsFunction6(1);
m.add function: "follows7"  , implementation: new FollowsFunction7(1); 

/////
// E.g.: 126141638,"cereal","988","moving","50","105","53","2","box","516.5936279296875","661.789306640625","364.231 305.74 114.842"
// ActID, ActName, SubActID, SubActName, SubActStartFrame, SubActEndFrame, FrameNr, ObjectID, ObjectName, DistanceRight, DistanceLeft, Pos3D
// Idact.frame.objId.distLeft.distRight.objName.pos3D Distance is in cm. Touching happens when smaller than max. ~52cm.
// Input event is considered all the time; however, the object is annotated only when the distance to any of the objects is closer than 49cm. Otherwise it is "".

// Objects
// Object types
m.add predicate: "arrangeable", types: [ArgumentType.String]
m.add predicate: "pickable", types: [ArgumentType.String]
m.add predicate: "stackable", types: [ArgumentType.String]  ///
m.add predicate: "breakfastObject", types: [ArgumentType.String]
m.add predicate: "cleaningObject", types: [ArgumentType.String]
m.add predicate: "medicineObject", types: [ArgumentType.String]
m.add predicate: "microwavableObject", types: [ArgumentType.String]
m.add predicate: "takeoutFoodObject", types: [ArgumentType.String]

// Object interaction
m.add predicate: "hasCoordX", types: [ArgumentType.UniqueID, ArgumentType.Double]
m.add predicate: "hasCoordY", types: [ArgumentType.UniqueID, ArgumentType.Double]
m.add predicate: "hasCoordZ", types: [ArgumentType.UniqueID, ArgumentType.Double]
//m.add predicate: "hasSubActivityTimestamp", types: [ArgumentType.UniqueID, ArgumentType.Integer]

//m.add predicate: "hasActivityID", types: [ArgumentType.UniqueID, ArgumentType.Integer]
//m.add predicate: "hasSubActivityID", types: [ArgumentType.UniqueID, ArgumentType.Integer]
/*m.add predicate: "hasObjectID", types: [ArgumentType.UniqueID, ArgumentType.Integer]

m.add predicate: "performsSubActivityAtT", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.Integer]
m.add predicate: "performsActivityAtT", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.Integer]
m.add predicate: "immediatelyFollows",  types: [ArgumentType.UniqueID, ArgumentType.UniqueID]*/
// Sub-activities
m.add predicate: "performsReach", types: [ArgumentType.String, ArgumentType.UniqueID]
m.add predicate: "performsMove", types: [ArgumentType.String, ArgumentType.UniqueID]
m.add predicate: "performsPlace", types: [ArgumentType.String, ArgumentType.UniqueID]
m.add predicate: "performsNull", types: [ArgumentType.String, ArgumentType.UniqueID]
m.add predicate: "performsClose", types: [ArgumentType.String, ArgumentType.UniqueID]
m.add predicate: "performsEat", types: [ArgumentType.String, ArgumentType.UniqueID]
m.add predicate: "performsDrink", types: [ArgumentType.String, ArgumentType.UniqueID]
m.add predicate: "performsPour", types: [ArgumentType.String, ArgumentType.UniqueID]
m.add predicate: "performsClean", types: [ArgumentType.String, ArgumentType.UniqueID]
m.add predicate: "performsOpen", types: [ArgumentType.String, ArgumentType.UniqueID]

// Intermediate /heuristic repetitive patterns for moving an object from its original position
m.add predicate: "performsObjRelocation", types: [ArgumentType.String, ArgumentType.UniqueID] // object, time
m.add predicate: "performsReachAtPos", types: [ArgumentType.String, ArgumentType.Double, ArgumentType.Double, ArgumentType.Double, ]
m.add predicate: "onTopOfEachOther", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]
m.add predicate: "onSameHorizPlane", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]
//m.add predicate: "sameObject", types: [ArgumentType.String, ArgumentType.String] // Needed if we dont use a <> b?
//m.add predicate: "", types: [ArgumentType.String, ArgumentType.String]

//target predicate: Activity(Timestamp)
m.add predicate: "performsPreparingCereal", types: [ArgumentType.UniqueID]
m.add predicate: "performsTakingMedicine", types: [ArgumentType.UniqueID]
m.add predicate: "performsStackingObjects", types: [ArgumentType.UniqueID]
m.add predicate: "performsUnstackingObjects", types: [ArgumentType.UniqueID]
m.add predicate: "performsMicrowaving", types: [ArgumentType.UniqueID]
m.add predicate: "performsBending", types: [ArgumentType.UniqueID]
m.add predicate: "performsCleaning", types: [ArgumentType.UniqueID]
m.add predicate: "performsTakeoutFood", types: [ArgumentType.UniqueID]
m.add predicate: "performsArrangingObjects", types: [ArgumentType.UniqueID]
m.add predicate: "performsEatingMeal", types: [ArgumentType.UniqueID]

/*
 * We also create constants to refer to each object
 */
///////////////////////////// rules ////////////////////////////////////
/* (O1-O2) means that O1 and O2 are not equal */

//m.add rule: (performsReachAtPos(O,X,Y,Z, T1) & performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & follows(T1,T2,T3)) >> performsObjectRelocation(O, P, T3), weight: initialWeight, squared: sq;
//m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & follows(T1,T2,T3) & stackable(O)) >> performsObjRelocation(O, T3), weight: initialWeight, squared: sq;

// Activity rules
m.add rule: (performsReach("cereal", T1) & performsMove("bowl", T2)  & performsPlace(O, T3) & performsPour(O, T4) & follows4(T1,T2,T3,T4) & breakfastObject(O)) >> performsPreparingCereal(T4), weight: initialWeight, squared: sq;
// THIS RULE WORKS!:
m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3)  & performsOpen("medcinebox", T4) & performsEat("medcinebox", T5) & performsDrink("cup", T6) & medicineObject(O) & follows6(T1,T2,T3,T4,T5,T6)) >> performsTakingMedicine(T6), weight: initialWeight, squared: sq; //& precedes(T4,T5)
// THIS RULE USING 2 TIMES FUNCTION "FOLLOWS" DOES NOT WORK!:
//m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3)  & performsOpen("medcinebox", T4) & performsEat("medcinebox", T5) & performsDrink("cup", T6) & medicineObject(O) & follows(T1,T2,T3) & follows(T4,T5,T6)) >> performsTakingMedicine(T6), weight: initialWeight, squared: sq; //& precedes(T4,T5)
//m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsNull(O, T4) &  stackable(O) & precedes(T3, T4)) >> performsStackingObjects(T4), weight: initialWeight, squared: sq; // & (T1-T2)  ADD follows(T1, T2, T3) &
m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsNull(O, T4) &  stackable(O) & follows4(T1,T2,T3,T4)) >> performsStackingObjects(T4), weight: initialWeight, squared: sq; // & (T1-T2)  ADD follows(T1, T2, T3) &
//m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsNull(O, T4) & follows(T2,T3,T4) & stackable(O)) >> performsUnstackingObjects(T4), weight: initialWeight, squared: sq; // add follows(T1, T2, T3) &
m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsOpen(O,T4) & performsClose(O,T5) & follows5(T1,T2,T3,T4,T5) & microwavableObject(O)) >> performsMicrowaving(T5), weight: initialWeight, squared: sq; //follows(T1,T2,T3) & follows(T3,T4,T5)
m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsNull("plate", T3) & follows(T1,T2,T3) & pickable(O)) >> performsBending(T3), weight : initialWeight, squared: sq;
m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsOpen("microwave",T4) & performsClose("microwave",T5) & performsClean(O,T6) & performsNull(O,T7) & follows7(T1,T2,T3,T4,T5,T6,T7) & cleaningObject(O)) >> performsCleaning(T7), weight: initialWeight, squared: sq; //follows(T1,T2,T3) & follows(T3,T4,T5) &
m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsOpen(O,T4) & performsClose(O,T5) & performsNull(O,T6) & follows6(T1,T2,T3,T4,T5,T6) & takeoutFoodObject(O)) >> performsTakeoutFood(T6), weight: initialWeight, squared: sq;
m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsNull(O,T4) & follows4(T1,T2,T3,T4) & arrangeable(O)) >> performsArrangingObjects(T4), weight: initialWeight, squared: sq;
m.add rule: (performsReach("cup", T1) & performsMove("cup", T2) & performsPlace("cup", T3) & performsEat("cup",T4) & performsDrink("cup",T5) & performsNull("cup",T6) & follows6(T1,T2,T3,T4,T5,T6)) >> performsEatingMeal(T6), weight: initialWeight, squared: sq;
 // Exclusivity rules
//Mut(L1;L2) ~^ Lbl(E;L1) ) >> ~Lbl(E;L2)
//RMut(R; S) ~^ Rel(E1;E2;R) ) >> ~Rel(E1;E2; S)

// Heuristic rules
//m.add rule: ((Plate, T1) & performsMove(Plate, T2) & precedes(T1, T2)) >> performsStackingObjects(T2), weight : 8; // & (T1-T2)
//m.add rule: (performsReach(P, T1) & performsMove(P, T2) & performsNull(P, T3) & precedes(T1, T2) & precedes(T2, T3)) >> performsBending(T3), weight : 8;
//m.add rule: (performsReach(Plate, T1) & performsMove(Plate, T2) & precedes(T1, T2)) >> performsStackingObjects(T2), weight : 8; // & (T1-T2)
//m.add rule: (performsReach(P, T1) & performsMove(P, T2) & performsNull(P, T3) & precedes(T1, T2) & precedes(T2, T3)) >> performsBending(T3), weight : 8;
//m.add rule: (performsReach(S, T1) & performsMove(S, T2) & performsPlace(S, T3) & precedes(T1, T2) & precedes(T2, T3) & stackable(S)) >> performsUnstackingObjects(T3), weight : 8;

//////////////////////////// data setup ///////////////////////////

/* Loads data */
def dir = 'data'+java.io.File.separator+'activityRecognition'+java.io.File.separator;
//def dir = 'data'+java.io.File.separator+'activityRecognition'+java.io.File.separator;
def trainDir = dir+'train'+java.io.File.separator;
def testDir = dir+'test'+java.io.File.separator;

//Partition evidencePartition = new Partition(0);
//Partition targetPartition = new Partition(1);

Partition trainObservationsPartition = new Partition(0);
Partition trainPredictionsPartition = new Partition(1);
Partition truthPartition = new Partition(2);

/*Set targetPredicatesClosed = [performsStackingObjects, performsMicrowaving, performsUnstackingObjects, performsTakingMedicine, performsBending, performsEatingMeal, performsCleaning] as Set;
Set targetPredicatesOpen = [performsStackingObjects, performsMicrowaving, performsUnstackingObjects, performsTakingMedicine, performsBending, performsEatingMeal, performsCleaning];
Set givenPredicatesOpen = [performsReach, performsMove, performsPlace, performsEat, performsDrink, performsPour, performsOpen, performsClose, performsClean, performsNull, stackable, pickable, arrangeable, breakfastObject, cleaningObject, medicineObject, microwavableObject, takeoutFoodObject];
Set givenPredicatesClosed = [performsReach, performsMove, performsPlace, performsEat, performsDrink, performsPour, performsOpen, performsClose, performsClean, performsNull, stackable, pickable, arrangeable, breakfastObject, cleaningObject, medicineObject, microwavableObject, takeoutFoodObject] as Set;
*/
// PREDICATES TO PREDICT - WITH THE PATTERN: PERFORMS*ING -
Set targetPredicatesClosed = [performsStackingObjects, performsMicrowaving, performsTakingMedicine, performsBending, performsEatingMeal, performsCleaning] as Set;
Set targetPredicatesOpen = [performsStackingObjects, performsMicrowaving, performsTakingMedicine, performsBending, performsEatingMeal, performsCleaning];
// PREDICATES KNOWN/ INPUT - WITH THE PATTERN: PERFORMS* (THEIR VERB DOES NOT END IN -ING)
Set givenPredicatesOpen = [performsReach, performsMove, performsPlace, performsEat, performsDrink, performsPour, performsOpen, performsClose, performsClean, performsNull, stackable, pickable, arrangeable, breakfastObject, cleaningObject, medicineObject, microwavableObject, takeoutFoodObject];
Set givenPredicatesClosed = [performsReach, performsMove, performsPlace, performsEat, performsDrink, performsPour, performsOpen, performsClose, performsClean, performsNull, stackable, pickable, arrangeable, breakfastObject, cleaningObject, medicineObject, microwavableObject, takeoutFoodObject] as Set;

/*// Used as a way to populate RV from a non-truth partition
Partition observationsBackupPartition = new Partition(86);
Partition predictionsBackupPartition = new Partition(87);
// POPULATING BACK UP DB
for (Predicate p : targetPredicatesOpen){
	println "Inserting data (training target predicate) into backup partition: "+p
	insert = data.getInserter(p, observationsBackupPartition);
	InserterUtils.loadDelimitedData(insert, testDir+p.getName().toLowerCase()+".txt");
}
Database backupDB = data.getDatabase(predictionsBackupPartition, targetPredicatesClosed, observationsBackupPartition);
*/

// POPULATION OF DATABASES
println " INSERTING DATA INTO DB FOR LEARNING WEIGHTS: "
for (Predicate p : givenPredicatesOpen){
	println "Inserting data (training observed predicate): "+p
	insert = data.getInserter(p, trainObservationsPartition);
	InserterUtils.loadDelimitedData(insert, trainDir+p.getName().toLowerCase()+".txt");
}

for (Predicate p : targetPredicatesOpen){
	println "Inserting data (training prediction predicate): "+p
	insert = data.getInserter(p, truthPartition); 
	InserterUtils.loadDelimitedDataTruth(insert, trainDir+p.getName().toLowerCase()+".txt");
}

// DATABASE CREATION
Database trainDB = data.getDatabase(trainPredictionsPartition, givenPredicatesClosed, trainObservationsPartition);

Database truthDB = data.getDatabase(truthPartition, targetPredicatesClosed);

// Populate trainDB target predicates with RV: (takes RV values from filepath with true labels)
for (pred in targetPredicatesOpen)
	populateTargetPredicateRV(data, trainDB, trainDir+pred.getName().toLowerCase()+".txt", targetPredicatesOpen);
//populateTargetPredicateRVfromOrigDB(data, backupDB, trainDB, targetPredicatesClosed);

	
//////////////////////////// weight learning ///////////////////////////
/*
 * We first open a database which contains all the target atoms as observations.
 * We then combine this database with the original database to learn.
 */
println "LEARNING WEIGHTS...";

MaxLikelihoodMPE weightLearning = new MaxLikelihoodMPE(m, trainDB, truthDB, config);
weightLearning.learn();
weightLearning.close();

println "LEARNING WEIGHTS DONE.  ";
println m

/////////////////////////// test setup //////////////////////////////////
println "\n TESTING SET UP:"
Partition testObservationsPartition = new Partition(3);
Partition testPredictionsPartition = new Partition(4);
for (Predicate p : givenPredicatesOpen){
	println "Inserting data (test observed predicate): "+p
	insert = data.getInserter(p, testObservationsPartition);
	InserterUtils.loadDelimitedData(insert, testDir+p.getName().toLowerCase()+".txt");
}

Database testDB = data.getDatabase(testPredictionsPartition, givenPredicatesClosed, testObservationsPartition);

for (pred in targetPredicatesOpen)
	//populateTargetPredicateForLearningWeights(truthDB, testDB, pred , pred);
	//populateDatabaseWithTargetPredicate(data, testDB, trainDir+pred.getName().toLowerCase()+".txt");
	populateTargetPredicateRV(data, testDB, trainDir+pred.getName().toLowerCase()+".txt", targetPredicatesOpen);
//populateTargetPredicateRVfromOrigDB(data, backupDB, testDB, targetPredicatesClosed);


/////////////////////////// test inference //////////////////////////////////
/*
 * Before running inference, we have to add the target atoms to the database.
 * If inference (or learning) attempts to access an atom that is not in the database,
 * it will throw an exception.
 * The below code builds a set of all users, then uses a utility class
 * (DatabasePopulator) to create all possible SamePerson atoms between users of
 * each network.
 */
println "INFERRING...";

MPEInference inference = new MPEInference(m, testDB, config);
inference.mpeInference();
inference.close();

println "INFERENCE DONE\n";
println "Inference results on second test DB with learned weights (for each ground truth timestamp when the activity is known to happen):"


DecimalFormat formatter = new DecimalFormat("#.##");
for (Predicate p : targetPredicatesOpen){
	for (GroundAtom atom : Queries.getAllAtoms(testDB, p)){
		println atom.toString() + "?: " + formatter.format(atom.getValue());
	}
}

/*
 * We close the Databases to flush writes
 */
trainDB.close();
testDB.close();
truthDB.close();
println "END!"

void populateTargetPredicateRVfromOrigDB(DataStore data, Database dbOrig, Database dbTarget, Set<Predicate> predicates){
	/* Collects the activity ending times and uses them to populate target predicates.
	 * 
	 */
	//System.out.println("Populating Database With Target Predicate..."+predToPopulate);
	println "  -PopulateTargetPredicateRVfromOrigDB..."
	Set<GroundTerm> timestamps  = new HashSet<GroundTerm>();
	for( Predicate targetPredicate: predicates){		
		Set<GroundAtom> concepts = Queries.getAllAtoms(dbOrig, targetPredicate);
		for (GroundAtom atom : concepts) {
			//System.out.println("Populating target atom for: "+targetPredicate+" with value: "+atom.getArguments()[0].toString());				
			timestamps.add(atom.getArguments()[0]);
		}
	}
	if(timestamps.size() >0){
		/* Populates manually (as opposed to using DatabasePopulator) */
		//System.out.println("-Populating target predicates with RV (timestamp query values): "+timestamps+"...");
		for( Predicate targetPredicate: predicates){
			for (GroundTerm ts : timestamps) {
				// Populates manually (as opposed to using DatabasePopulator)
				System.out.println("Populating target atom for: "+targetPredicate+" with RV value: "+ts.toString());
				((RandomVariableAtom) dbTarget.getAtom(targetPredicate, ts)).commitToDB();
			}
		}
		System.out.println("...Successful populateTargetPredicateRVfromOrigDB with timestamps: "+timestamps);//+" :"+ performsStackingObjects(Timestamp).getFormula());
	}
}

void populateTargetPredicateRV(DataStore data, Database dbTarget, String origPredPath, Set<Predicate> predicates){
	// Collects the activity ending times. Populates targets	 
	//System.out.println("Populating Database With Target Predicate..."+predToPopulate);
	println "  -PopulateTargetPredicateRV..."
	Set<GroundTerm> timestamps = new HashSet<GroundTerm>();
	File file = new File(origPredPath);//.getText();//'UTF-8'); //String inputFile = new File(filename).getText();
	if (!file.exists()) {
		println "Error in populateDatabaseWithTargetPredicate: File does not exist"
		System.exit(-1);
	}
	else{
		def lines = new File(origPredPath) as String[]
		// Getting timestamps when activities have actually occurred in order to query for them, providing them as grounding
		for(line in lines)
			if(!line.equals("")){
				timestamps.add(data.getUniqueID(Integer.parseInt(line.split("\t")[0])));		
				//println line
			}
		if(timestamps.size() >0){			
			// Populates manually (as opposed to using DatabasePopulator) 
			System.out.println("\n-Populating target predicates with RV (timestamp query values): "+timestamps+"...");
			for( Predicate targetPredicate: predicates){
				System.out.println(" Populating target atoms for: "+targetPredicate);
				for (GroundTerm ts : timestamps) {
					// Populates manually (as opposed to using DatabasePopulator)
					//System.out.println("Populating target atom with value: "+ts.toString());
					((RandomVariableAtom) dbTarget.getAtom(targetPredicate, ts)).commitToDB();
				}
			}						
			System.out.println("...Successful populateDatabaseWithTargetPredicate: "+timestamps);//+" :"+ performsStackingObjects(Timestamp).getFormula());
		}
	}
}	

void populateDatabaseWithTargetPredicate(DataStore data, Database dbTarget, String origPredPath) {
	/* Collects the activity ending times.
	 * Populates targets
	 */
	//System.out.println("Populating Database With Target Predicate..."+predToPopulate);
	Set<GroundTerm> timestamps = new HashSet<GroundTerm>();
	File file = new File(origPredPath);//.getText();//'UTF-8'); //String inputFile = new File(filename).getText();
	if (!file.exists()) {
		println "Error in populateDatabaseWithTargetPredicate: File does not exist"
		System.exit(-1);
	}
	else{
		def lines = new File(origPredPath) as String[]
		// Getting timestamps when activities have actually occurred in order to query for them, providing them as grounding
		for(line in lines)
			if(!line.equals("")){
				timestamps.add(data.getUniqueID(Integer.parseInt(line.split("\t")[0])));		
				//println line
			}
		if(timestamps.size() >0){
			Map<Variable, Set<GroundTerm>> popMap = new HashMap<Variable, Set<GroundTerm>>();
			popMap.put(new Variable("Timestamp"), timestamps)
	
			DatabasePopulator dbPop = new DatabasePopulator(dbTarget);
			//dbPop.populate((performsPreparingCereal(Timestamp)).getFormula(), popMap);
			dbPop.populate((performsStackingObjects(Timestamp)).getFormula(), popMap);
			//dbPop.populate((performsUnstackingObjects(Timestamp)).getFormula(), popMap);
			dbPop.populate((performsTakingMedicine(Timestamp)).getFormula(), popMap);
			//dbPop.populate((performsArrangingObjects(Timestamp)).getFormula(), popMap);
			//dbPop.populate((performsCleaning(Timestamp)).getFormula(), popMap);
			dbPop.populate((performsMicrowaving(Timestamp)).getFormula(), popMap);
			dbPop.populate((performsBending(Timestamp)).getFormula(), popMap);
			//dbPop.populate((performsTakeoutFood(Timestamp)).getFormula(), popMap);
			//dbPop.populate((performsEatingMeal(Timestamp)).getFormula(), popMap);
						
			System.out.println("--Successful populateDatabaseWithTargetPredicate: "+timestamps);//+" :"+ performsStackingObjects(Timestamp).getFormula());
		}
	}
}	

void populateTargetPredicateForLearningWeights(Database dbOrigin, Database dbTarget, Predicate predOrig, Predicate predToPopulate) {
	/* Collects the activity ending times */
	Set<GroundAtom> atoms = Queries.getAllAtoms(dbOrigin, predOrig);
	ArrayList<GroundTerm[]> potentialMatchesInPredOrig = new ArrayList<GroundTerm[]>();
	for (GroundAtom a : atoms) {
		GroundTerm[] terms = a.getArguments();
		potentialMatchesInPredOrig.add(terms);
	}
	int i=0;
	while(i< potentialMatchesInPredOrig.size()){
		for (GroundTerm[] terms : potentialMatchesInPredOrig[i]) {
			GroundAtom rv;
			if(predToPopulate.arity == 3)
				rv = dbTarget.getAtom(predToPopulate, terms[0], terms[1], terms[2]);
			else
				if(predToPopulate.arity == 2)
					rv = dbTarget.getAtom(predToPopulate, terms[0], terms[1]);
				else
					if(predToPopulate.arity == 1)
						rv = dbTarget.getAtom(predToPopulate, terms[0]);
					else{
						System.err.println("Error in populateDatabaseWithNAryPredicate: Arity of predicate is not supported: "+predToPopulate.arity);
						System.exit(-1);
						}
								
			if (rv instanceof RandomVariableAtom){
				dbTarget.commit((RandomVariableAtom)rv);
				System.out.println("Weight learning >> Populating target atom "+rv.toString()+" RV for predicate: "+predToPopulate);
			}
		}
		i++;
	}
	//System.out.println("Successful populateDatabaseWithPredicate");
}