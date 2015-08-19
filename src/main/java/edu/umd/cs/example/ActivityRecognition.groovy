/*
 * This file is part of the PSL software.
 * Copyright 2011-2013 University of Maryland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.umd.cs.example;

import java.text.DecimalFormat;

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
import edu.umd.cs.example.JavaUtilsHAR;
import edu.umd.cs.example.FollowsFunction;

////////////////////////// initial setup ////////////////////////
ConfigManager cm = ConfigManager.getManager()
ConfigBundle config = cm.getBundle("ActivityRecognition");//ontology-alignment")

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
//m.add predicate: "drinkingKitchenware", types: [ArgumentType.String]
//m.add predicate: "containerKitchenware", types: [ArgumentType.String]
//m.add predicate: "mealObject", types: [ArgumentType.String]
//m.add predicate: "object", types: [ArgumentType.UniqueID]
//m.add predicate: "plate", types: [ArgumentType.UniqueID]

// Object interaction
//m.add predicate: "performs"  , types: [ArgumentType.UniqueID, ArgumentType.String, ArgumentType.UniqueID]
//m.add predicate: "precedes"  , types: [ArgumentType.UniqueID, ArgumentType.UniqueID]
//m.add predicate: "precedes"  , types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]
//m.add predicate: "involvesObjectAtPos"  , types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]
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
m.add predicate: "performsTranslation", types: [ArgumentType.String, ArgumentType.UniqueID]
	
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
/*GroundTerm cloth = data.getUniqueID(80);
GroundTerm medicineBox = data.getUniqueID(81);
GroundTerm microwave = data.getUniqueID(82);//"microwave"); 
GroundTerm milk = data.getUniqueID(83);
GroundTerm book = data.getUniqueID(84);
GroundTerm cup = data.getUniqueID(85);
GroundTerm plate = data.getUniqueID(86);
GroundTerm remote = data.getUniqueID(87);
GroundTerm bowl = data.getUniqueID(88);
GroundTerm box = data.getUniqueID(89);
GroundTerm nullObject = data.getUniqueID(90);*/
//Activities in constants?

/////// Predicate constraints
//m.add PredicateConstraint.Functional, on: precedes;
//m.add PredicateConstraint.InverseFunctional, on: precedes;
//m.add PredicateConstraint.PartialFunctional, on : involvesSubActivity;
//m.add PredicateConstraint.PartialFunctional, on : performsActivityAtT;

//m.add function: "immediatelyFollows",  implementation: new ImmediatelyFollows()
//m.add function: "repeatsSubsequenceMinTimes", name1  : Entity,    name2   : Entity	, implementation: new RepeatsSubsequenceMinTimes(integer i)
//m.add function: "objectsGetStacked"  , name1  : Text,    name2   : Text	, implementation: new ObjectsGetStacked()
//m.add function: "objectsGetUnstacked", name1  : Entity,    name2   : Entity	, implementation: new ObjectsGetUnstacked()
//m.add PredicateConstraint.Functional, on: immediatelyFollows
//m.add Predicateconstraint.InverseFunctional, on: immediatelyFollows

///////////////////////////// rules ////////////////////////////////////
/* (O1-O2) means that O1 and O2 are not equal */
// & precedes(M, P) & performs(P, Place, Cup)

//m.add rule: plate(O) >> stackable(O) & containerKitchenware(O), weight : 8;
//m.add rule: (object("box")) >> stackable("box"), weight : 8; //
//m.add rule: (object(X) & X.is("box")) >> stackable(X), weight : 8;
//m.add rule: cup(O) >> pourable(O), weight : 8;


//m.add rule: (performsReach(Cereal, T1) & performsMove(Bowl, T2) & precedes(T1, T2)) >> performsPreparingCereal(T2), weight : 8;
//a b and c
//m.add rule : (performsReach(P, T1) & performsMove(P, T2) & performsPlace(P, T3) & precedes(T1, T2) & precedes(T2, T3) & P.is("plate")) >> performsStackingObjects(T3), weight : 8;
//m.add rule : (performsReach("plate", T1) & performsMove("plate", T2) & performsPlace("plate", T3) & precedes(T1, T2) & precedes(T2, T3)) >> performsStackingObjects(T3), weight : 8;
//m.add rule : (performsReach(P, T1) & performsMove(P, T2) & performsPlace(P, T3) & precedes(T1, T2) & precedes(T2, T3) & plate(P)) >> performsStackingObjects(T3), weight : 8;
//m.add rule : (performsReach("plate", T1) & performsMove("plate", T2) & performsPlace("plate", T3) & precedes(T1, T2) & precedes(T2, T3)) >> performsStackingObjects(T3), weight : 8;

// Activity rules
//m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsPour(O, T4) & follows(T1, T2, T3) & precedes(T3,T4) & breakfastObject(O)) >> performsPreparingCereal(T4), weight : 8;
//m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsOpen(MedicineBox, T4) & performsEat(MedicineBox, T5) & performsDrink(Cup, T6) & medicineObject(O) & follows(T1,T2, T3) & follows(T3,T4,T5) & precedes(T5,T6)) >> performsTakingMedicine(T6), weight : 8;

//RULE 1: WORKS
m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsNull(O, T4) &  stackable(O) & precedes(T3, T4)) >> performsStackingObjects(T4), weight: initialWeight, squared: sq; // & (T1-T2)  ADD follows(T1, T2, T3) &
//RULE 2: DOES NOT WORK!!
m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsOpen(O,T4) & performsClose(O,T5) & precedes(T1,T2) & microwavableObject(O)) >> performsMicrowaving(T5), weight: initialWeight, squared: sq; //follows(T1,T2,T3) & follows(T3,T4,T5)
//RULE 3: WORKS
m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsNull(O, T4) & precedes(T3, T4) & stackable(O)) >> performsUnstackingObjects(T4), weight: initialWeight, squared: sq; // add follows(T1, T2, T3) & 

//m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsNull(O, T3)  & follows(T1, T2, T3) & pickable(O)) >> performsBending(T3), weight : 8;
//m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsOpen("microwave",T4) & performsClose("microwave",T5) & performsClean(O,T6) & performsNull(O,T7) & follows(T1,T2,T3) & follows(T3,T4,T5) & follows(T5,T6,T7) & cleaningObject(O)) >> performsCleaning(T7), weight : 8;
//m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsOpen(O,T4) & performsClose(O,T5) & performsNull(O,T6) & follows(T1,T2,T3) & follows(T3,T4,T5) & precedes(T5, T6) & takeoutFoodObject(O)) >> performsTakeoutFood(T6), weight : 8;
//m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3) & performsNull(O,T4) & follows(T1,T2,T3) & precedes(T3,T4) & arrangeable(O)) >> performsArrangingObjects(T4), weight : 8;
//m.add rule: (performsReach("cup", T1) & performsMove("cup", T2) & performsPlace("cup", T3) & performsEat("cup",T4) & performsDrink("cup",T5) & performsNull("cup",T6) & follows(T1,T2,T3) & follows(T3,T4,T5) & precedes(T5,T6)) >> performsEatingMeal(T6), weight : 8;
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

Partition trainObservations = new Partition(0);
Partition trainPredictions = new Partition(1);
Partition truth = new Partition(2);

Set targetPredicates = [performsStackingObjects, performsMicrowaving, performsUnstackingObjects] as Set;

for (Predicate p : [performsReach, performsMove, performsPlace, performsEat, performsDrink, performsPour, performsOpen, performsClose, performsClean, performsNull, stackable, pickable, arrangeable, breakfastObject, cleaningObject, medicineObject, microwavableObject, takeoutFoodObject])
{
	println "Populating training predicate: "+p
	insert = data.getInserter(p, trainObservations);
	InserterUtils.loadDelimitedData(insert, trainDir+p.getName().toLowerCase()+".txt");
}

for (Predicate p : targetPredicates){//performsUnstackingObjects, performsBending, performsTakingMedicine,performsPreparingCereal,performsCleaning,performsTakeoutFood,performsEatingMeal,performsArrangingObjects, performsMicrowaving])
	println "Populating training truth predicate: "+p
	insert = data.getInserter(p, truth);
	InserterUtils.loadDelimitedDataTruth(insert, trainDir+p.getName().toLowerCase()+".txt");
}

Database truthDB = data.getDatabase(truth, targetPredicates);
//populateTargetPredicatesToPredict(truthDB)

Database trainDB = data.getDatabase(trainPredictions, [performsReach, performsMove, performsPlace, performsEat, performsDrink, performsPour, performsOpen, performsClose, performsClean, performsNull, stackable, pickable, arrangeable, breakfastObject, cleaningObject, medicineObject, microwavableObject, takeoutFoodObject] as Set, trainObservations);
//populateTargetPredicatesToPredict(trainDB);
populateDatabaseWithPredicates(truthDB, trainDB, targetPredicates, 0 );
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

Partition testObservations = new Partition(3);
Partition testPredictions = new Partition(4);
for (Predicate p : [performsReach, performsMove, performsPlace, performsEat, performsDrink, performsPour, performsOpen, performsClose, performsClean, performsNull, stackable, pickable, arrangeable, breakfastObject, cleaningObject, medicineObject, microwavableObject, takeoutFoodObject]){
	insert = data.getInserter(p, testObservations);
	InserterUtils.loadDelimitedData(insert, testDir+p.getName().toLowerCase()+".txt");
}

Database testDB = data.getDatabase(testPredictions, [performsReach, performsMove, performsPlace, performsEat, performsDrink, performsPour, performsOpen, performsClose, performsClean, performsNull, stackable, pickable, arrangeable, breakfastObject, cleaningObject, medicineObject, microwavableObject, takeoutFoodObject] as Set, testObservations);
populateDatabaseWithPredicates(truthDB, testDB, targetPredicates, 0 );


/////////////////////////// test inference //////////////////////////////////
/*
 * Before running inference, we have to add the target atoms to the database.
 * If inference (or learning) attempts to access an atom that is not in the database,
 * it will throw an exception.
 *
 * The below code builds a set of all users, then uses a utility class
 * (DatabasePopulator) to create all possible SamePerson atoms between users of
 * each network.
 */
println "INFERRING...";

MPEInference inference = new MPEInference(m, testDB, config);
inference.mpeInference();
inference.close();

println "INFERENCE DONE";
println "Inference results on second db with learned weights:"
DecimalFormat formatter = new DecimalFormat("#.##");
for (Predicate p : targetPredicates){//performsUnstackingObjects, performsBending])
	for (GroundAtom atom : Queries.getAllAtoms(testDB, p))
		println atom.toString() + ": " + formatter.format(atom.getValue());
}
println "END!"



/**
 * Populates all the similar atoms between the concepts of two ontologies using
 * the fromOntology predicate.
 *
 * @param db  The database to populate. It should contain the fromOntology atoms
 */
void populateSimilar(Database db) {
	/* Collects the ontology concepts */
	Set<GroundAtom> concepts = Queries.getAllAtoms(db, fromOntology);
	Set<GroundTerm> o1 = new HashSet<GroundTerm>();
	Set<GroundTerm> o2 = new HashSet<GroundTerm>();
	for (GroundAtom atom : concepts) {
		if (atom.getArguments()[1].toString().equals("o1"))
			o1.add(atom.getArguments()[0]);
		else
			o2.add(atom.getArguments()[0]);
	}
	
	/* Populates manually (as opposed to using DatabasePopulator) */
	for (GroundTerm o1Concept : o1) {
		for (GroundTerm o2Concept : o2) {
			((RandomVariableAtom) db.getAtom(similar, o1Concept, o2Concept)).commitToDB();
			((RandomVariableAtom) db.getAtom(similar, o2Concept, o1Concept)).commitToDB();
		}
	}
}

void populateDatabaseWithPredicates(Database dbOrigin, Database dbTarget, Set predicates, int argumentIndex) {
	/* Collects the activity ending times */
	Set<GroundTerm> groundTerms = new HashSet<GroundTerm>();
	for( Predicate targetPredicate: predicates){//performsUnstackingObjects, performsBending, performsTakingMedicine,performsPreparingCereal,performsCleaning,performsTakeoutFood,performsEatingMeal,performsArrangingObjects, performsMicrowaving]){
		//System.out.println("Populating target predicate: "+targetPredicate);
		Set<GroundAtom> atoms = Queries.getAllAtoms(dbOrigin, targetPredicate);
		for (GroundAtom atom : atoms) {
			groundTerms.add(atom.getArguments()[argumentIndex]);
			//System.out.println("!! Populating target atom: "+atom.toString()+": "+atom.getArguments()[argumentIndex]);
		}
	}
	for( Predicate targetPredicate: predicates){//performsUnstackingObjects, performsBending, performsTakingMedicine,performsPreparingCereal,performsCleaning,performsTakeoutFood,performsEatingMeal,performsArrangingObjects, performsMicrowaving]){
		System.out.println("Populating target atoms for: "+targetPredicate);
		for (GroundTerm t : groundTerms) {
			/* Populates manually (as opposed to using DatabasePopulator) */
			System.out.println("!! Populating target atom with value: "+t.toString());
			((RandomVariableAtom) dbTarget.getAtom(targetPredicate, t)).commitToDB();
		}
	}	
	System.out.println("Successful populateDatabaseWithPredicates");
}
