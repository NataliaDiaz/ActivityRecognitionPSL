/*Set<GroundTerm> performsMoves = new HashSet<GroundTerm>();
Set<GroundTerm> performsPlaces = new HashSet<GroundTerm>();
Set<GroundTerm> performsReaches = new HashSet<GroundTerm>();
Set<GroundTerm> precedess = new HashSet<GroundTerm>();
for (int i = 1; i < 8; i++)
	usersA.add(data.getUniqueID(i));
for (int i = 11; i < 18; i++)
	usersB.add(data.getUniqueID(i));

Map<Variable, Set<GroundTerm>> popMap = new HashMap<Variable, Set<GroundTerm>>();
popMap.put(new Variable("UserA"), usersA)
popMap.put(new Variable("UserB"), usersB)

DatabasePopulator dbPop = new DatabasePopulator(db);
dbPop.populate((SamePerson(UserA, UserB)).getFormula(), popMap);
dbPop.populate((SamePerson(UserB, UserA)).getFormula(), popMap);*/


//Database trueDataDB = data.getDatabase(trueDataPartition, [performsStackingObjects, performsUnstackingObjects] as Set);
Database db = data.getDatabase(targetPartition, [performsReach, performsMove, performsPlace, performsEat, performsDrink, performsPour, performsOpen, performsClose, performsClean, performsNull, stackable, pickable, arrangeable, breakfastObject, cleaningObject, medicineObject, microwavableObject, takeoutFoodObject] as Set, evidencePartition);
//populateTargetPredicate(testDB);

/*
 * Before running inference, we have to add the target atoms to the database.
 * If inference (or learning) attempts to access an atom that is not in the database,
 * it will throw an exception.
 *
 * The below code builds a set of all users, then uses a utility class
 * (DatabasePopulator) to create all possible SamePerson atoms between users of
 * each network.
 */

/*Set<GroundTerm> timestamps = new HashSet<GroundTerm>(); //Set<GroundTerm> usersB = new HashSet<GroundTerm>();
for (int i = 1; i < 8; i++)
	timestamps.add(data.getUniqueID(i));

Map<Variable, Set<GroundTerm>> popMap = new HashMap<Variable, Set<GroundTerm>>();
popMap.put(new Variable("Timestamp"), timestamps) //popMap.put(new Variable("UserB"), usersB)*/

populateActivitiesToRecognize(db);
System.out.println("Populated activities to recognize"); /*DatabasePopulator dbPop = new DatabasePopulator(db);
dbPop.populate((performsPreparingCereal(Timestamp)).getFormula(), popMap); 
dbPop.populate((performsTakingMedicine(Timestamp)).getFormula(), popMap);
dbPop.populate((performsBending(Timestamp)).getFormula(), popMap);
dbPop.populate((performsStackingObjects(Timestamp)).getFormula(), popMap); 
dbPop.populate((performsUnstackingObjects(Timestamp)).getFormula(), popMap);
dbPop.populate((performsArrangingObjects(Timestamp)).getFormula(), popMap);
dbPop.populate((performsCleaning(Timestamp)).getFormula(), popMap); 
dbPop.populate((performsTakeoutFood(Timestamp)).getFormula(), popMap);
dbPop.populate((performsMicrowaving(Timestamp)).getFormula(), popMap);
dbPop.populate((performsEatingMeal(Timestamp)).getFormula(), popMap); */

/*
 * Default model.
 */
println ""
println "Default "
println m

/*
 * Now we can run inference
 */
MPEInference inferenceApp = new MPEInference(m, db, config);
inferenceApp.mpeInference();
inferenceApp.close();

/*
 * Let's see the results
 */
println "Inference results with hand-defined weights -performsStackingObjects:"
DecimalFormat formatter = new DecimalFormat("#.##");
for (Predicate p : [performsStackingObjects, performsMicrowaving]){//performsUnstackingObjects, performsBending, performsTakingMedicine,performsPreparingCereal,performsCleaning,performsTakeoutFood,performsEatingMeal,performsArrangingObjects, performsMicrowaving])
	println "Inference results with hand-defined weights: "+p.toString()
	for (GroundAtom atom : Queries.getAllAtoms(db, p))
		println atom.toString() + "\t" + formatter.format(atom.getValue());
}

/*
 * Next, we want to learn the weights from data. For that, we need to have some
 * evidence data from which we can learn. In our example, that means we need to
 * specify the 'true' alignment, which we now load into another partition.
 */
Partition trueDataPartition = new Partition(2); // These files should be initially created, but empty ?!?!?!?
for (Predicate p : [performsStackingObjects, performsMicrowaving]){//performsUnstackingObjects, performsBending, performsTakingMedicine,performsPreparingCereal,performsCleaning,performsTakeoutFood,performsEatingMeal,performsArrangingObjects, performsMicrowaving])
	insert = data.getInserter(p, trueDataPartition)
	InserterUtils.loadDelimitedData(insert, testDir+p.getName().toLowerCase()+".txt");
}
/*insert = data.getInserter(performsStackingObjects, trueDataPartition)
InserterUtils.loadDelimitedData(insert, testDir + "performsstackingobjects.txt");
insert = data.getInserter(performsUnstackingObjects, trueDataPartition)
InserterUtils.loadDelimitedData(insert, testDir + "performsunstackingobjects.txt");
insert = data.getInserter(performsBending, trueDataPartition)
InserterUtils.loadDelimitedData(insert, testDir + "performsbending.txt");*/

/*
 * Now, we can learn the weights.
 *
 * We first open a database which contains all the target atoms as observations.
 * We then combine this database with the original database to learn.
 */
Database trueDataDB = data.getDatabase(trueDataPartition, [performsStackingObjects, performsMicrowaving]);//performsUnstackingObjects, performsBending, performsTakingMedicine,performsPreparingCereal,performsCleaning,performsTakeoutFood,performsEatingMeal,performsArrangingObjects, performsMicrowaving] as Set);
MaxLikelihoodMPE weightLearning = new MaxLikelihoodMPE(m, db, trueDataDB, config);
weightLearning.learn();
weightLearning.close();

/*
 * Let's have a look at the newly learned weights.
 */
println ""
println "Learned model:"
println m

/*
 * Now, we apply the learned model to a different social network alignment data set.
 * We load the data set as before (into new partitions) and run inference.
 * Finally, we print the results.
 */

/*
 * Loads evidence
 */
Partition evidencePartition2 = new Partition(3);

for (Predicate p : [performsReach, performsMove, performsPlace, performsEat, performsDrink, performsPour, performsOpen, performsClose, performsClean, performsNull, stackable, pickable, arrangeable, breakfastObject, cleaningObject, medicineObject, microwavableObject, takeoutFoodObject])
{
	insert = data.getInserter(p, evidencePartition2);
	InserterUtils.loadDelimitedData(insert, trainDir+p.getName().toLowerCase()+".txt");
}

/*
 * Populates targets
 */
def targetPartition2 = new Partition(4);
Database db2 = data.getDatabase(targetPartition2, [performsReach, performsMove, performsPlace, performsEat, performsDrink, performsPour, performsOpen, performsClose, performsClean, performsNull, stackable, pickable, arrangeable, breakfastObject, cleaningObject, medicineObject, microwavableObject, takeoutFoodObject] as Set, evidencePartition2);


populateActivitiesToRecognize(db2);
/*timestamps.clear();
for (int i = 1; i < 8; i++)
	timestamps.add(data.getUniqueID(i));
dbPop = new DatabasePopulator(db2);
dbPop.populate((performsStackingObjects(Timestamp)).getFormula(), popMap);
dbPop.populate((performsUnstackingObjects(Timestamp)).getFormula(), popMap);
dbPop.populate((performsBending(Timestamp)).getFormula(), popMap);
dbPop.populate((performsTakingMedicine(Timestamp)).getFormula(), popMap);
dbPop.populate((performsPreparingCereal(Timestamp)).getFormula(), popMap);
dbPop.populate((performsCleaning(Timestamp)).getFormula(), popMap);
dbPop.populate((performsTakeoutFood(Timestamp)).getFormula(), popMap);
dbPop.populate((performsEatingMeal(Timestamp)).getFormula(), popMap);
dbPop.populate((performsArrangingObjects(Timestamp)).getFormula(), popMap);
dbPop.populate((performsMicrowaving(Timestamp)).getFormula(), popMap);*/

/*
 * Performs inference
 */
inferenceApp = new MPEInference(m, db2, config);
result = inferenceApp.mpeInference();
inferenceApp.close();

println "Inference results on second db with learned weights:"
for (Predicate p : [performsStackingObjects, performsMicrowaving]){//performsUnstackingObjects, performsBending])
	for (GroundAtom atom : Queries.getAllAtoms(db2, p))
		println atom.toString() + "\t" + formatter.format(atom.getValue());
}

/*
 * We close the Databases to flush writes
 */
db.close();
trueDataDB.close();
db2.close();

/////////**************
/*println "LEARNING WEIGHTS MaxLikelihoodMPE...";

Database trainDB = data.getDatabase(predictions, [performsReach, performsMove, performsPlace, precedes, stackable, pickable, arrangeable, drinkingKitchenware, containerKitchenware] as Set, observations);
//populateTargetPredicate(trainDB);
MaxLikelihoodMPE weightLearning = new MaxLikelihoodMPE(m, trainDB, trueDataDB, config); // USED FOR LEARNING WEIGHTS
weightLearning.learn();
weightLearning.close();
println m;

/////////////////////////// test inference //////////////////////////////////
println "RUNNING INFERENCE:  MPEInference...";
inference = new MPEInference(m, trainDB, config); // FOR INFERENCE
inference.mpeInference();
inference.close();
println m

// MAP (Maximum a posteriori) inference vs MPE inference
//result = model.mapInference(data.getDatabase(write: <outputPartitionID>, read : <evidencePartitionID>));
println "INFERENCE DONE";
/////////////////////////// training setup //////////////////////////////////
//Database truthDB = data.getDatabase(evidencePartition, [stackable] as Set); // 2 PARAMS ONLY WHEN NOT LEARNING WEIGHTS?
*/