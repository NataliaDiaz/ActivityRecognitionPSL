package PSLHAR;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map.Entry;

import org.slf4j.Logger
import org.slf4j.LoggerFactory

//import edu.umd.cs.linqs.wiki.ExperimentConfigGenerator
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE





import edu.umd.cs.psl.application.learning.weight.maxlikelihood.MaxPseudoLikelihood
import edu.umd.cs.psl.application.learning.weight.maxmargin.MaxMargin
import edu.umd.cs.psl.application.learning.weight.maxmargin.MaxMargin.LossBalancingType
import edu.umd.cs.psl.application.learning.weight.maxmargin.MaxMargin.NormScalingType
import edu.umd.cs.psl.application.learning.weight.random.HardEMRandOM
import edu.umd.cs.psl.model.Model
import edu.umd.cs.psl.model.atom.GroundAtom
import edu.umd.cs.psl.model.atom.QueryAtom
import edu.umd.cs.psl.database.DatabaseQuery

import com.google.common.collect.Iterables;

import edu.umd.cs.psl.groovy.*;
import edu.umd.cs.psl.core.*
import edu.umd.cs.psl.core.inference.*
import edu.umd.cs.psl.ui.functions.textsimilarity.*;
import PSLHAR.InserterUtils;
import edu.umd.cs.psl.database.loading.Inserter;
import PSLHAR.MulticlassPredictionComparator.Example;
import edu.umd.cs.psl.util.database.Queries;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.StringAttribute
import edu.umd.cs.psl.model.argument.UniqueID;
import edu.umd.cs.psl.model.argument.Variable;
import edu.umd.cs.psl.model.argument.IntegerAttribute
import edu.umd.cs.psl.model.kernel.CompatibilityKernel
import edu.umd.cs.psl.model.parameters.Weight
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
import edu.umd.cs.psl.database.ResultList;
import edu.umd.cs.psl.database.Partition;
import edu.umd.cs.psl.database.DatabasePopulator;
import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.database.loading.Inserter
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore;
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver;
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type;
import edu.umd.cs.psl.evaluation.result.FullInferenceResult
import edu.umd.cs.psl.evaluation.statistics.ConfusionMatrix
import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionComparator
//import edu.umd.cs.psl.evaluation.statistics.MulticlassPredictionComparator
//import edu.umd.cs.psl.evaluation.statistics.MulticlassPredictionStatistics

//import edu.umd.cs.linqs.WeightLearner;

import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionComparator
import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionStatistics
import edu.umd.cs.psl.evaluation.statistics.filter.MaxValueFilter
import edu.umd.cs.psl.evaluation.result.*
import edu.umd.cs.psl.evaluation.statistics.RankingScore
import edu.umd.cs.psl.evaluation.statistics.SimpleRankingComparator  // Shobeir to predict Accuracy with discretePredictionStatistics

//println "This source file is a place holder for the tree of groovy and java sources for your PSL project."
println "PSL for HUMAN ACTIVITY RECOGNITION"


////////////////////////// initial setup ////////////////////////
Logger log = LoggerFactory.getLogger(this.class)

ConfigManager cm = ConfigManager.getManager()
ConfigBundle config = cm.getBundle("PSLHAR");// Needs to be the name of the current package

def outPath = config.getString("outpath", "output/results/");
def computeBaseline = true;

//Default is accept UniqueIDs only as integers.
config.setProperty("rdbmsdatastore.usestringids", false); // to allow categories as UniqueID, instead of only integers as UniqueIDs. A Database can accept only one type of argument UniqueID at a time.

//def defaultPath = System.getProperty("java.io.tmpdir")
String H2DBPath = 'data'+java.io.File.separator+'H2DB' // This setting does not work and takes default
String dbpath = config.getString("dbpath", H2DBPath)  // MAKE SURE THIS DIRECTORY EXISTS!
DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, dbpath, true), config)
println "Working DB path: "+ dbpath + "\n and H2 DB storage: :"+data

/*** EXPERIMENT CONFIG ***/
/**
 * SET UP CONFIGS
 */

//ExperimentConfigGenerator configGenerator = new ExperimentConfigGenerator(dataSet);

/*
 * SET MODEL TYPES
 *
 * Options:
 * "quad" HL-MRF-Q
 * "linear" HL-MRF-L
 * "bool" MRF
 */
modelType = "quad"
sq = (!modelType.equals("linear") ? true : false)
//configGenerator.setModelTypes([modelType]);

/*
 * SET LEARNING ALGORITHMS
 *
 * Options:
 * "MLE" (MaxLikelihoodMPE)
 * "MPLE" (MaxPseudoLikelihood)
 * "MM" (MaxMargin)
 */
//configGenerator.setLearningMethods(["MLE"]);

/* MLE/MPLE options */
//configGenerator.setVotedPerceptronStepCounts([10]); //configGenerator.setVotedPerceptronStepSizes([(double) 5.0]);

//List<ConfigBundle> configs = configGenerator.getConfigs();
PSLModel m = new PSLModel(this, data);
double initialWeight= 10.0;
boolean sq = true;

folds = 4 // number of 
double seedRatio = 0.5 // ratio of observed labels
Random rand = new Random(0) // used to seed observed data
trainTestRatio = 0.5 // ratio of train to test splits (random)
filterRatio = 1.0 // ratio of documents to keep (throw away the rest)
targetSize = 3000 // target size of snowball sampler
explore = 0.001 // prob of random node in snowball sampler

//ACTIVITIES
ArrayList<String> Activities = new ArrayList<String>();
Activities.add("stacking");
Activities.add("unstacking");
Activities.add("arrangingObjects");
Activities.add("microwaving");
Activities.add("medicine");
Activities.add("takeout");
Activities.add("cleaningObjects");
Activities.add("bending");
Activities.add("eatingMeal");
Activities.add("cereal");
//Activities.add("relocatingObject");

GroundTerm preparingCereal = new IntegerAttribute(1111);
GroundTerm takingMedicine = new IntegerAttribute(2222);
GroundTerm stackingObjects = new IntegerAttribute(3333);
GroundTerm unstackingObjects = new IntegerAttribute(4444);
GroundTerm microwaving = new IntegerAttribute(5555);
GroundTerm bending = new IntegerAttribute(6666);
GroundTerm cleaning = new IntegerAttribute(7777);
GroundTerm takeoutFood = new IntegerAttribute(8888);
GroundTerm arrangingObjects = new IntegerAttribute(9999);
GroundTerm eatingMeal = new IntegerAttribute(10000);

ArrayList<GroundTerm> ActivityInts = new ArrayList<GroundTerm>();
ActivityInts.add(stackingObjects);
ActivityInts.add(unstackingObjects);
ActivityInts.add(arrangingObjects);
ActivityInts.add(microwaving);
ActivityInts.add(takingMedicine);
ActivityInts.add(takeoutFood);
ActivityInts.add(cleaning);
ActivityInts.add(bending);
ActivityInts.add(eatingMeal);
ActivityInts.add(preparingCereal);

/*GroundTerm stackingObjects = new StringAttribute("stacking");
GroundTerm unstackingObjects = new StringAttribute("unstacking");
GroundTerm arrangingObjects = new StringAttribute("arrangingObjects");
GroundTerm microwaving = new StringAttribute("microwaving");
GroundTerm takingMedicine = new StringAttribute("medicine");
GroundTerm takeoutFood = new StringAttribute("takeout");
GroundTerm cleaning = new StringAttribute("cleaningObjects");
GroundTerm bending = new StringAttribute("bending");
GroundTerm eatingMeal = new StringAttribute("eatingMeal");
GroundTerm preparingCereal = new StringAttribute("cereal");*/

//GroundTerm relocatingObject = new StringAttribute("relocatingObject");   // TODO SAME FOR OBJ PROPERTIES, needed?


GroundTerm arrangeable = new StringAttribute("arrangeable");
GroundTerm pickable = new StringAttribute("pickable");
GroundTerm stackable = new StringAttribute("stackable");
GroundTerm drinkingKitchenware = new StringAttribute("drinkingKitchenware");
GroundTerm containerKitchenware = new StringAttribute("containerKitchenware");
GroundTerm takeoutFoodObject = new StringAttribute("takeoutFoodObject");
GroundTerm breakfastObject = new StringAttribute("breakfastObject");
GroundTerm cleaningObject = new StringAttribute("cleaningObject");
GroundTerm medicineObject = new StringAttribute("medicineObject");
GroundTerm microwavableObject = new StringAttribute("microwavableObject");
GroundTerm openable = new StringAttribute("openable");

GroundTerm reach = data.getUniqueID(1);
GroundTerm move = data.getUniqueID(2);
GroundTerm place = data.getUniqueID(3);
GroundTerm nulll = data.getUniqueID(4);
GroundTerm open = data.getUniqueID(5);
GroundTerm close = data.getUniqueID(6);
GroundTerm clean = data.getUniqueID(7);
GroundTerm eat = data.getUniqueID(8);
GroundTerm drink = data.getUniqueID(9);
GroundTerm pour = data.getUniqueID(10);

// OBJECT INTERACTIONS (OBSERVED)
GroundTerm book = data.getUniqueID(11);
GroundTerm bowl = data.getUniqueID(22);
GroundTerm box = data.getUniqueID(33);
GroundTerm plate = data.getUniqueID(44);
GroundTerm microwave = data.getUniqueID(55);
GroundTerm cloth = data.getUniqueID(66);
GroundTerm medicineBox = data.getUniqueID(77);
GroundTerm milk = data.getUniqueID(88);
GroundTerm remote = data.getUniqueID(99);
GroundTerm cup = data.getUniqueID(111);
GroundTerm nullObject = data.getUniqueID(110);

////////////////////////// predicate declaration ////////////////////////
// THE FOLLOWING FUNCTIONS CHECK IF THE INSTANT TIMESTAMPS ARE CONSECUTIVE (E.G. T2 = T1+1) FOR DIFFERENT NR OF PARAMETERS [2..7]
int maxFrameDelayAmongSubActivitiesAllowed = 1;
m.add function: "precedes" , implementation: new PrecedesFunction(maxFrameDelayAmongSubActivitiesAllowed); // Compare efficiency of predicate vs function PRECEDES
m.add function: "follows"  , implementation: new FollowsFunction(maxFrameDelayAmongSubActivitiesAllowed); // Parameter n indicates max nr of time units allowed in between timestamps to the function to return true
m.add function: "follows4" , implementation: new FollowsFunction4(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "follows5" , implementation: new FollowsFunction5(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "follows6" , implementation: new FollowsFunction6(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "follows7" , implementation: new FollowsFunction7(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "lessThan" , implementation: new LessThanFunction();
m.add function: "sameObjectType", implementation: new SameObjectTypeFunction();
m.add function: "sameObjectType3", implementation: new SameObjectTypeFunction3();
m.add function: "followsForSameObjectType", implementation: new FollowsForSameObjectTypeFunction(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "conseqPairs8" , implementation: new ConseqPairs8(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "conseqPairs12" , implementation: new ConseqPairs12(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "conseqPairs6" , implementation: new ConseqPairs6(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "conseqPairs14" , implementation: new ConseqPairs14(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "conseqPairs4" , implementation: new ConseqPairs4(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "conseqPairs10" , implementation: new ConseqPairs10(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "conseqPairs26" , implementation: new ConseqPairs26(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "conseqPairs18" , implementation: new ConseqPairs18(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "conseqPairs16" , implementation: new ConseqPairs16(maxFrameDelayAmongSubActivitiesAllowed);
m.add function: "conseqPairs30" , implementation: new ConseqPairs30(maxFrameDelayAmongSubActivitiesAllowed);

/////
// E.g.: 126141638,"cereal","988","moving","50","105","53","2","box","516.5936279296875","661.789306640625","364.231 305.74 114.842"
// ActID, ActName, SubActID, SubActName, SubActStartFrame, SubActEndFrame, FrameNr, ObjectID, ObjectName, DistanceRight, DistanceLeft, Pos3D
// Idact.frame.objId.distLeft.distRight.objName.pos3D Distance is in cm. Touching happens when smaller than max. ~52cm.
// Input event is considered all the time; however, the object is annotated only when the distance to any of the objects is closer than 49cm. Otherwise it is "".

// Object interaction
/*m.add predicate: "hasCoordX", types: [ArgumentType.UniqueID, ArgumentType.Double]
m.add predicate: "hasCoordY", types: [ArgumentType.UniqueID, ArgumentType.Double]
m.add predicate: "hasCoordZ", types: [ArgumentType.UniqueID, ArgumentType.Double]

// Intermediate /heuristic repetitive patterns for moving an object from its original position
//m.add predicate: "performsReachAtPos", types: [ArgumentType.String, ArgumentType.Double, ArgumentType.Double, ArgumentType.Double]
m.add predicate: "onTopOfEachOther", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]
m.add predicate: "onSameHorizPlane", types: [ArgumentType.UniqueID, ArgumentType.UniqueID]*/

///////////////////////////// rules ////////////////////////////////////
/* (O1-O2) means that O1 and O2 are not equal */
// Exclusivity rules
//Mut(L1;L2) ~^ Lbl(E;L1) ) >> ~Lbl(E;L2)
//RMut(R; S) ~^ Rel(E1;E2;R) ) >> ~Rel(E1;E2; S)
 //ADD LATER Mut(A,B) & PerformsTask(T1, T2, A) & Precedes(T3, T2) >> ~PerformsTask(T1, T3, B)

// Prior on disjoint classes
//m.add rule: mutExclusive(performsMakingCereal,BB2), weight: 0.01, squared: sq;*/

///// MODEL 2 WITH LESS PREDICATES //////////////////

m.add predicate: "performsSubActiv", types: [ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID,ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID, ArgumentType.UniqueID]; //m.add predicate: "performsActiv", types: [ArgumentType.String, ArgumentType.UniqueID];
m.add predicate: "performsActiv", types: [ArgumentType.Integer, ArgumentType.UniqueID, ArgumentType.UniqueID];
m.add predicate: "hasObjProperty", types: [ArgumentType.UniqueID, ArgumentType.String]; // Should object be a unique ID?

//m.add predicate: "activity", types: [ArgumentType.UniqueID];
//m.add predicate: "object", types: [ArgumentType.UniqueID];
//m.add predicate: "timestampsTrainFold1", types: [ArgumentType.UniqueID];
m.add predicate: "timestamp", types: [ArgumentType.Integer, ArgumentType.UniqueID, ArgumentType.UniqueID];//[ArgumentType.UniqueID];
//m.add predicate: "mutEx", types: [ArgumentType.UniqueID, ArgumentType.UniqueID];
//m.add predicate: "allTimestamps", types: [ArgumentType.UniqueID];  // All key timestamps we want to query for (when activities actually happen)

// Heuristic rules
//m.add rule : (activity(A) & activity(B) & performsActiv(A,T)) >> ~performsActiv(B,T), constraint:true ; // Since I modelled activity as String, does this equal to...?
m.add rule : (performsActiv(A,T1,TE) & performsActiv(B,T2,TE) & lessThan(T1,T2)) >> ~performsActiv(B,T2,TE), constraint:true ;

//m.add rule: (performsSubActiv(nulll,O,X,Y,Z,T0,T1) & performsSubActiv(reach,O1,X1,Y1,Z1,T2,T3) & performsSubActiv(move,O2,X2,Y2,Z2,T4,T5) & performsSubActiv(place,O3,X3,Y3,Z3,T6,T7) & performsSubActiv(reach,O4,X4,Y4,Z4,T8,T9) & performsSubActiv(move,O5,X5,Y5,Z5,T10,T11) & performsSubActiv(place,O6,X6,Y6,Z6,T12,T13) & performsSubActiv(nulll,O7,X7,Y7,Z7,T14,T15) & hasObjProperty(O1,stackable) & hasObjProperty(O2,stackable) & hasObjProperty(O3,stackable) & hasObjProperty(O4,stackable) & hasObjProperty(O5,stackable) & hasObjProperty(O6,stackable) & hasObjProperty(O7,stackable) & conseqPairs14(T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14)) >> performsActiv(stackingObjects,T0,T14), weight: initialWeight, squared: sq;

m.add rule: (performsSubActiv(nulll,O,X,Y,Z,T0,T1) & performsSubActiv(reach,O1,X1,Y1,Z1,T2,T3) & performsSubActiv(move,O2,X2,Y2,Z2,T4,T5) & performsSubActiv(place,O3,X3,Y3,Z3,T6,T7) & performsSubActiv(reach,O4,X4,Y4,Z4,T8,T9) & performsSubActiv(move,O5,X5,Y5,Z5,T10,T11) & performsSubActiv(place,O6,X6,Y6,Z6,T12,T13) & performsSubActiv(nulll,O7,X7,Y7,Z7,T14,T15) & hasObjProperty(O1,stackable) & hasObjProperty(O2,stackable) & hasObjProperty(O3,stackable) & hasObjProperty(O4,stackable) & hasObjProperty(O5,stackable) & hasObjProperty(O6,stackable) & hasObjProperty(O7,stackable) & conseqPairs14(T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14)) >> performsActiv(unstackingObjects,T0,T14), weight: initialWeight, squared: sq;

m.add rule: (performsSubActiv(nulll,O,X,Y,Z,T0,T1) & performsSubActiv(reach,O,X1,Y1,Z1,T2,T3) & performsSubActiv(move,O,X2,Y2,Z2,T4,T5) & performsSubActiv(place,O,X3,Y3,Z3,T6,T7) & hasObjProperty(O,arrangeable) & conseqPairs6(T1,T2,T3,T4,T5,T6)) >> performsActiv(arrangingObjects,T0,T7), weight: initialWeight, squared: sq;

/*//m.add rule: (performsSubActiv(nulll,O,X,Y,Z,T0,T1) & performsSubActiv(reach,O,X1,Y1,Z1,T2,T3) & performsSubActiv(move,O,X2,Y2,Z2,T4,T5) & conseqPairs4(T1,T2,T3,T4) & hasObjProperty(O,pickable)) >> performsActiv(bending,T0,T5), weight : initialWeight, squared: sq;

m.add rule: (performsSubActiv(move,O,X,Y,Z,T0,T1) & performsSubActiv(place,O,X1,Y1,Z1,T2,T3) & performsSubActiv(reach,O,X2,Y2,Z2,T4,T5) & performsSubActiv(open,O2,X3,Y3,Z3,T6,T7)  & performsSubActiv(reach,O,X4,Y4,Z4,T8,T9) & performsSubActiv(move,O,X5,Y5,Z5,T10,T11) & performsSubActiv(pour,O2,X6,Y6,Z6,T12,T13) & performsSubActiv(move,O,X7,Y7,Z7,T14,T15) & performsSubActiv(place,O,X8,Y8,Z8,T16,T17) & performsSubActiv(reach,O,X9,Y9,Z9,T18,T19) & performsSubActiv(move,O,X10,Y10,Z10,T20,T21) & performsSubActiv(pour,O2,X11,Y11,Z11,T22,T23) & performsSubActiv(move,O,X12,Y12,Z12,T24,T25) & performsSubActiv(place,O,X13,Y13,Z13,T26,T27) & conseqPairs26(T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16,T17,T18,T19,T20,T21,T22,T23,T24,T25,T26) & hasObjProperty(O,breakfastObject) & hasObjProperty(O2,openable)) >> performsActiv(preparingCereal,T0,T7), weight: initialWeight, squared: sq;

m.add rule: (performsSubActiv(reach,O,X,Y,Z,T0,T1) & performsSubActiv(open,medicineBox,X1,Y1,Z1,T2,T3) & performsSubActiv(reach,O,X2,Y2,Z2,T4,T5) & performsSubActiv(move,O,X3,Y3,Z3,T6,T7)  & performsSubActiv(eat,medicineBox,X4,Y4,Z4,T8,T9) & performsSubActiv(reach,O,X5,Y5,Z5,T10,T11) & performsSubActiv(move,O,X6,Y6,Z6,T12,T13) & performsSubActiv(drink,cup,X7,Y7,Z7,T14,T15) & performsSubActiv(move,O,X8,Y8,Z8,T16,T17) & performsSubActiv(place,O,X9,Y9,Z9,T18,T19) & conseqPairs18(T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16,T17,T18) & hasObjProperty(O,medicineObject)) >> performsActiv(takingMedicine,T0,T19), weight: initialWeight, squared: sq;

///
m.add rule: (performsSubActiv(nulll,O,X,Y,Z,T0,T1) & performsSubActiv(reach,O,X1,Y1,Z1,T2,T3) & performsSubActiv(open,microwave,X2,Y2,Z2,T4,T5) & performsSubActiv(reach,O,X3,Y3,Z3,T6,T7)  & performsSubActiv(move,O,X4,Y4,Z4,T8,T9) & performsSubActiv(place,O,X5,Y5,Z5,T10,T11) & performsSubActiv(reach,O,X6,Y6,Z6,T12,T13) & performsSubActiv(close,microwave,X7,Y7,Z7,T14,T15) & performsSubActiv(nulll,O,X8,Y8,Z8,T16,T17) & conseqPairs16(T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16) & hasObjProperty(O,microwavableObject)) >> performsActiv(microwaving,T0,T17), weight: initialWeight, squared: sq;

m.add rule: (performsSubActiv(nulll,O,X,Y,Z,T0,T1) & performsSubActiv(reach,O,X1,Y1,Z1,T2,T3) & performsSubActiv(open,microwave,X2,Y2,Z2,T4,T5) & performsSubActiv(reach,O,X3,Y3,Z3,T6,T7)  & performsSubActiv(move,cloth,X4,Y4,Z4,T8,T9) & performsSubActiv(clean,O,X5,Y5,Z5,T10,T11) & performsSubActiv(move,cloth,X6,Y6,Z6,T12,T13) & performsSubActiv(place,cloth,X7,Y7,Z7,T14,T15) & performsSubActiv(reach,O,X8,Y8,Z8,T16,T17) & performsSubActiv(close,microwave,X9,Y9,Z9,T18,T19) & conseqPairs18(T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16,T17,T18) & hasObjProperty(O,cleaningObject)) >> performsActiv(cleaning,T0,T19), weight: initialWeight, squared: sq;

m.add rule: (performsSubActiv(nulll,O,X,Y,Z,T0,T1) & performsSubActiv(reach,O,X1,Y1,Z1,T2,T3) & performsSubActiv(open,microwave,X2,Y2,Z2,T4,T5) & performsSubActiv(reach,O,X3,Y3,Z3,T6,T7) & performsSubActiv(move,O2,X4,Y4,Z4,T8,T9) & performsSubActiv(place,O2,X5,Y5,Z5,T10,T11) & performsSubActiv(reach,O,X6,Y6,Z6,T12,T13) & performsSubActiv(close,microwave,X7,Y7,Z7,T14,T15)& conseqPairs14(T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14) & hasObjProperty(O,takeoutFoodObject)& hasObjProperty(O2,containerKitchenware)) >> performsActiv(takeoutFood,T0,T15), weight: initialWeight, squared: sq;

m.add rule: (performsSubActiv(reach,cup,X,Y,Z,T0,T1) & performsSubActiv(move,cup,X1,Y1,Z1,T2,T3) & performsSubActiv(eat,cup,X2,Y2,Z2,T4,T5) & performsSubActiv(move,cup,X3,Y3,Z3,T6,T7)  & performsSubActiv(move,cup,X4,Y4,Z4,T8,T9) & performsSubActiv(eat,cup,X5,Y5,Z5,T10,T11) & performsSubActiv(move,cup,X6,Y6,Z6,T12,T13) & performsSubActiv(nulll,cup,X7,Y7,Z7,T14,T15) & performsSubActiv(reach,cup,X8,Y8,Z8,T16,T17) & performsSubActiv(move,cup,X9,Y9,Z9,T18,T19) &
performsSubActiv(drink,cup,X1,Y1,Z1,T20,T21) & performsSubActiv(move,cup,X2,Y2,Z2,T22,T23) & performsSubActiv(move,cup,X3,Y3,Z3,T24,T25)  & performsSubActiv(drink,cup,X4,Y4,Z4,T26,T27) & performsSubActiv(move,cup,X5,Y5,Z5,T28,T29) & performsSubActiv(place,cup,X6,Y6,Z6,T30,T31) & conseqPairs30(T1,T2,T3,T4,T5,T6,T7,T8,T9,T10,T11,T12,T13,T14,T15,T16,T17,T18,T19,T20,T21,T22,T23,T24,T25,T26,T27,T28,T29,T30)) >> performsActiv(eatingMeal,T0,T31), weight: initialWeight, squared: sq;
	 */
// THIS RULE WORKS!:
//m.add rule: (performsSubActiv(reach,O,X,Y,Z,T0,T1) & performsSubActiv(move,O,X1,Y1,Z1,T2,T3) & performsSubActiv(place,O3,X2,Y2,Z2,T4,T5) & performsSubActiv(open,medicineBox,X3,Y3,Z3,T6,T7) & performsSubActiv(eat,medicinebox,X4,Y4,Z4,T8,T9) & performsSubActiv(drink,cup,X5,Y5,Z5,T10,T11) & hasObjProperty(O,"medicationObject") & conseqPairs10(T1,T2,T3,T4,T5,T6,T7,T8,T9,T10)) >> performsActiv(takingMedicine,T0,T11), weight: initialWeight, squared: sq;
// THIS RULE USING 2 TIMES FUNCTION "FOLLOWS" DOES NOT WORK!:
//m.add rule: (performsReach(O, T1) & performsMove(O, T2) & performsPlace(O, T3)  & performsOpen("medcinebox", T4) & performsEat("medcinebox", T5) & performsDrink("cup", T6) & medicineObject(O) & follows(T1,T2,T3) & follows(T4,T5,T6)) >> performsTakingMedicine(T6), weight: initialWeight, squared: sq; //& precedes(T4,T5)

// Advantages: allows for setting mutual exclusivity (disjointness), and functional roles (where the sum of all predicate groundings truth values should not sum over 1),

//PROBLEMATIC RULE GIVING ERROR BECAUSE OF HAVING MORE THAN ONE FUNCTION PREDICATE IN THE BODY: (FIXED WHEN REMOVING FOLLOWS OR SAMEOBJECTTYPE ATOM)
//LATENT VARIABLE RULE: =INTERMEDIATE PREDICATE NON OBSERVED, JUST INFERRED
//m.add rule: (performsSubActiv(reach,O1,T1) & performsSubActiv(move,O2,T2) & performsSubActiv(place,O3,T3) & hasObjProperty(O1,"stackable") & hasObjProperty(O2,"stackable") & hasObjProperty(O3,"stackable") & followsForSameObjectType(T1,T2,T3,O1,O2,O3) ) >> performsActivSE(relocatingObject,O1,T1,T3), weight: initialWeight, squared: sq;  //follows(T1,T2,T3) & sameObjectType3(O1,O2,O3)

//m.add rule: (relocating(t,finalPosX, finalPosY,finalPosZ) and onTop(finalPosX1, finalPosX2, finalPosY1,finalPosY2, finalPosZ1, finalPosZ2) >> performsStackingObjects(t);
//m.add rule: (relocating(t,finalPosX, finalPosY,finalPosZ) and onSamePlane(finalPosX1, finalPosX2, finalPosY1,finalPosY2, finalPosZ1, finalPosZ2) >> performsUnstackingObjects(t);
//m.add rule: (relocating(t,finalPosX, finalPosY,finalPosZ) and onSamePlane(finalPosX1, finalPosX2, finalPosY1,finalPosY2, finalPosZ1, finalPosZ2) >> performsUnstackingObjects(t);
	
// Constraints // Functional constraint on a predicate means that it should sum to 1 for each instantiation of that predicate
//m.add PredicateConstraint.Functional , on : performsActiv;
//m.add PredicateConstraint.Functional , on : performsSubActiv; // Not supported yet

//////////////////////////// data setup ///////////////////////////

/* Loads data */
def fulldataDir = 'data'+java.io.File.separator+'activityRecognition'+java.io.File.separator+'PSLData'+java.io.File.separator;
def minidataDir = 'data'+java.io.File.separator+'activityRecognition'+java.io.File.separator+'PSLDataMini'+java.io.File.separator;
def dataDir = minidataDir;//'data'+java.io.File.separator+'activityRecognition'+java.io.File.separator+'PSLData'+java.io.File.separator;
def primaryKeyToDatapointIDMappingFile = 'data'+java.io.File.separator+'activityRecognition'+java.io.File.separator+'PSLData'+java.io.File.separator+"subactivityEndFrameToDatapointIdMapping.csv";
def keyTimestampsFile = dataDir+java.io.File.separator+"timestamp.csv";
//def trainDir = dataDir+'train'+java.io.File.separator;
//def testDir = dataDir+'test'+java.io.File.separator;

println "Loading data from dataDir: "+dataDir
/*Partition trainObservationsPartition = new Partition(0);
Partition trainPredictionsPartition = new Partition(1);
Partition truthPartition = new Partition(2);
//Partition truthValuesForQueryPartition = new Partition(99);*/

// TARGET PREDICATES TO PREDICT -
Set targetPredicates = [performsActiv] as Set;//, performsActivSE] as Set; //Add mutex and translate
// OBSERVED PREDICATES / KNOWN/ INPUT -
Set observedPredicates = [performsSubActiv, hasObjProperty, timestamp] as Set;//Activities is input manually
//Set knownQueryPredicates = [timestamp] as Set;//sTrainFold1, timestampsTrainFold2, timestampsTrainFold3, timestampsTrainFold4, timestampsTestFold1,timestampsTestFold2, timestampsTestFold3,timestampsTestFold4] as Set;
/*** EXPERIMENT CONFIG ***/

/* get all default weights */
Map<CompatibilityKernel,Weight> initWeights = new HashMap<CompatibilityKernel, Weight>();
for (CompatibilityKernel k : Iterables.filter(m.getKernels(), CompatibilityKernel.class))
	initWeights.put(k, k.getWeight());

	
/*** DATASTORE PARTITIONS ***/

int partitionIndex = 0;
int folds = 4;
int numActivities = countFileLines(dataDir+"performsactiv.csv")
int numSubActivities = countFileLines(dataDir+"performssubactiv.csv")
println "Running experiment for "+numActivities+" activities and "+numSubActivities+" subActivity-object interaction instances"

Partition[][] partitions = new Partition[2][numActivities];
for (int s = 0; s < numActivities; s++) {
	partitions[0][s] = new Partition(partitionIndex++);	// observations
	partitions[1][s] = new Partition(partitionIndex++);	// labels
}
	
//////////////////////////////////////// INSERTING DATA
Inserter[] inserters;

/* Ground truth */
inserters = InserterUtils.getMultiPartitionInserters(data, performsActiv, partitions[1], numActivities, 1);
InserterUtils.loadDelimitedDataTruthMultiPartition(inserters, dataDir+"performsactiv.csv", false, primaryKeyToDatapointIDMappingFile);

/* Observations */
inserters = InserterUtils.getMultiPartitionInserters(data, performsSubActiv, partitions[0], numActivities, 5);
InserterUtils.loadDelimitedDataTruthMultiPartition(inserters, dataDir + "performssubactiv.csv", false, primaryKeyToDatapointIDMappingFile);
inserters = InserterUtils.getMultiPartitionInserters(data, timestamp, partitions[0], numActivities, 1); // true labels, same file as performsActiv
InserterUtils.loadDelimitedDataTruthMultiPartition(inserters, dataDir+"timestamp.csv", false, primaryKeyToDatapointIDMappingFile);
inserters = InserterUtils.getMultiPartitionInserters(data, hasObjProperty, partitions[0], numActivities, -1);
InserterUtils.loadDelimitedDataTruthMultiPartition(inserters, dataDir + "hasobjproperty.csv", true, "");

//inserters = InserterUtils.getMultiPartitionInserters(data, activity, partitions[0], numActivities, 0);
//InserterUtils.loadDelimitedDataMultiPartition(inserters, dataDir + "activity.csv");
//InserterUtils.loadDelimitedDataTruthMultiPartition(inserters, filePfx + "hogframelabel.txt", "\t", 1000);

log.info("Inserting data done!");

///////////////////////////////////////////
/*** GLOBAL DATA FOR DB POPULATION ***/

// Retrieve timestamps when actually each activity in each partition, happens, to use as key atoms to ground in the query
ArrayList<ArrayList<GroundTerm>> candidateEndTimestampsForQuery = new ArrayList<ArrayList<GroundTerm>>();
ArrayList<ArrayList<GroundTerm>> candidateStartTimestampsForQuery = new ArrayList<ArrayList<GroundTerm>>();
ArrayList<ArrayList<GroundTerm[]>> candidatePerformActivityTermsForQuery = new ArrayList<ArrayList<GroundTerm[]>>();

for (int s = 0; s < numActivities; s++) { // to get possible start and end points (those we want to query, when an activity actually has been performed)
	candidateEndTimestampsForQuery.add(new ArrayList<GroundTerm>());
	candidateStartTimestampsForQuery.add(new ArrayList<GroundTerm>());
	candidatePerformActivityTermsForQuery.add(new ArrayList<GroundTerm[]>())
}
Database db = data.getDatabase(new Partition(partitionIndex++), observedPredicates, partitions[0]);

//HashMap<Integer,Integer> queryTimestampsToSampleIDDict = InserterUtils.getFileContentIntoIntCategoriesDictionary(primaryKeyToDatapointIDMappingFile);
ArrayList<Integer> queryStartTimestampsToSampleIDDict = InserterUtils.getKeyTimestampsForQuery(keyTimestampsFile, 1);
ArrayList<Integer> queryEndTimestampsToSampleIDDict = InserterUtils.getKeyTimestampsForQuery(keyTimestampsFile, 2);
println "Start Timestamps: "+queryStartTimestampsToSampleIDDict
println "Size of start timestamps used for grounding: "+queryStartTimestampsToSampleIDDict.size()
println "End Timestamps: "+queryEndTimestampsToSampleIDDict
println "Size of end timestamps used for grounding: "+queryEndTimestampsToSampleIDDict.size()
for(int ts: queryStartTimestampsToSampleIDDict){
	int dataPointIndex = InserterUtils.getSampleIDForInt(ts, primaryKeyToDatapointIDMappingFile);//InserterUtils.getSampleIDForInt(timestamp);
	//int s = Integer.parseInt(terms[0].toString().trim());//-1;//getValue().intValue() - 1;//for integer value
	//println "Adding candidate timestamp: "+ ts+" to activity datapoint index "+dataPointIndex+"  "+data.getUniqueID(ts)+ " "+queryTimestampsToSampleIDDict.get(ts);
	if(dataPointIndex < numActivities){ // If this sample activity is being considered
		candidateStartTimestampsForQuery[dataPointIndex].add(data.getUniqueID(ts));
	}
}
for(int ts: queryEndTimestampsToSampleIDDict){
	int dataPointIndex = InserterUtils.getSampleIDForInt(ts, primaryKeyToDatapointIDMappingFile);//InserterUtils.getSampleIDForInt(timestamp);
	//int s = Integer.parseInt(terms[0].toString().trim());//-1;//getValue().intValue() - 1;//for integer value
	//println "Adding candidate timestamp: "+ ts+" to activity datapoint index "+dataPointIndex+"  "+data.getUniqueID(ts)+ " "+queryTimestampsToSampleIDDict.get(ts);
	if(dataPointIndex < numActivities){ // If this sample activity is being considered
		candidateEndTimestampsForQuery[dataPointIndex].add(data.getUniqueID(ts));
	}
}
/*Set<GroundAtom> endTAtoms = Queries.getAllAtoms(db, timestamp);
for (GroundAtom a : endTAtoms) {
	GroundTerm[] terms = a.getArguments();
	dataPointIndex = InserterUtils.getSampleIDForInt(Integer.valueOf(terms[1].toString().trim()), primaryKeyToDatapointIDMappingFile);
	//int s = Integer.parseInt(terms[0].toString().trim());//-1;//getValue().intValue() - 1;//for integer value
	println "Adding candidate end timestamp: "+ terms[0]+" to activity datapoint index "+dataPointIndex+"  "+terms[1].getClass()+data.getUniqueID(terms[1]);
	candidateEndTimestampsForQuery[dataPointIndex].add(terms[2]);// TODO FIX FOR EFFICIENCY 0]);
	candidateStartTimestampsForQuery[dataPointIndex].add(terms[1]);
	candidatePerformActivityTermsForQuery[dataPointIndex].add(terms);
	}*/

/*Set<GroundAtom> startTAtoms = Queries.getAllAtoms(db, performsSubActiv); // TODO optimize all in 1 loop
//for(int sample =0; sample < numActivities; ++sample){

for (GroundAtom a : startTAtoms) {
	GroundTerm[] terms = a.getArguments();
	dataPointIndex = InserterUtils.getSampleIDForInt(Integer.valueOf(terms[5].toString().trim()), primaryKeyToDatapointIDMappingFile);
	//int s = Integer.parseInt(terms[5].toString().trim())-1;//getValue().intValue() - 1;//for integer value
	//println "Adding candidate start timestamp: "+ terms[5]+" to activity datapoint index "+dataPointIndex;
	//parameter index 1 for performsSubActiv
	candidateStartTimestampsForQuery[dataPointIndex].add(terms[6]);//(data.getUniqueID(Integer.valueOf(terms[6]))); //parameter index 1 for performsSubActiv
}*/

db.close();

//Map<GroundTerm,Integer> activityMap = new HashMap<GroundTerm,Integer>();
Map<GroundTerm,Integer> activityMap = new HashMap<GroundTerm,Integer>();
for (int i = 0; i < ActivityInts.size(); i++)
	activityMap.put(ActivityInts.get(i), i);//StringAttribute(Activities.get(i)), i);
println "ActivityMap "+activityMap

/*** RUN EXPERIMENTS ***/

//Map<String, List<DiscretePredictionStatistics>> discreteStatsPerformsActiv = new HashMap<String, List<DiscretePredictionStatistics>>()
//for (ConfigBundle config : configs)
//discreteStatsPerformsActiv.put(config, new ArrayList<DiscretePredictionStatistics>())

Map<String, List<MulticlassPredictionStatistics>> statsPerformsActiv = new HashMap<String, List<MulticlassPredictionStatistics>>()
//for (ConfigBundle method : configs)
statsPerformsActiv.put(config, new ArrayList<MulticlassPredictionStatistics>())
List<MulticlassPredictionStatistics> stats_baseline = new ArrayList<MulticlassPredictionStatistics>()

for (int fold = 1; fold < folds+1; fold++) {
	 println "Starting experiment fold "+fold
	 // To construct training set: query for all of the atoms from each scene, except for hold-out.
	 List<Partition> trainPartsObs = new ArrayList<Partition>();
	 List<Partition> trainPartsLabels = new ArrayList<Partition>();
	 List<Partition> testPartsObs = new ArrayList<Partition>();
	 List<Partition> testPartsLabels = new ArrayList<Partition>();
	 for (int s = 0; s < numActivities; s++) { // TODO FIX with each timestamp from each fold (regenerate)
		 if ((s+fold) % folds != 0) {
			 println "Fold "+fold+": Adding sample "+s+" to Training partitions"
			 trainPartsObs.add(partitions[0][s]);
			 trainPartsLabels.add(partitions[1][s]);
		 }
		 else {
			 println "Fold "+fold+": Adding sample "+s+" to Test partitions"
			 testPartsObs.add(partitions[0][s]);
			 testPartsLabels.add(partitions[1][s]);
		 }
	 }
	 
	 /*** POPULATE DB ***/
	 println "Populating databases.... fold "+fold
	 log.info("Populating databases ...");
	 Partition write_train = new Partition(partitionIndex++);
	 Partition write_test = new Partition(partitionIndex++);
	 Database trainDB = data.getDatabase(write_train, observedPredicates, (Partition[])trainPartsObs.toArray());
	 Database testDB = data.getDatabase(write_test, observedPredicates, (Partition[])testPartsObs.toArray());
 
	 /* Populate the dataset: populate the unobserved performsActiv predicate so the model knows to what links to provide predictions for.
	. */
	 Variable StartT = new Variable("StartT");
	 Variable EndT = new Variable("EndT")
	 Variable Activity = new Variable("Activity");
	 Map<Variable, Set<GroundTerm>> subs = new HashMap<Variable, Set<GroundTerm>>();
	 subs.put(Activity, activityMap.keySet());
	 // Get all timestamps ground terms
	 Set<GroundTerm> endTimesGTTrain= new HashSet<GroundTerm>();
	 Set<GroundTerm> endTimesGTTest = new HashSet<GroundTerm>();
	 Set<GroundTerm> startTimesGTTrain= new HashSet<GroundTerm>();
	 Set<GroundTerm> startTimesGTTest = new HashSet<GroundTerm>();
	
	 for (int s = 0; s < numActivities; s++) {
		 Set<GroundTerm> curSetEndTime = ((s+fold) % folds != 0) ? endTimesGTTrain : endTimesGTTest;
		 curSetEndTime.addAll(candidateEndTimestampsForQuery[s]);
		 Set<GroundTerm> curSetStartTime = ((s+fold) % folds != 0) ? startTimesGTTrain : startTimesGTTest;
		 //println "Adding groundings to EndT and StartT: "+candidateStartTimestampsForQuery[s].get(0).getClass()
		 curSetStartTime.addAll(candidateStartTimestampsForQuery[s]);
	 }
	 
	 // Training
	 subs.put(StartT, startTimesGTTrain);
	 subs.put(EndT, endTimesGTTrain); // SUBS,generate all data.
	 println "Substitutions for training: "+subs
	 DatabasePopulator dbPop = new DatabasePopulator(trainDB);
	 //dbPop.populate(new QueryAtom(performsActiv, Activity, StartT, EndT), subs);
	 dbPop.populate(performsActiv(Activity, StartT, EndT).getFormula(), subs);
	 // Testing
	 //subs = new HashMap<Variable, Set<GroundTerm>>();
	 subs.put(StartT, startTimesGTTest);
	 subs.put(EndT, endTimesGTTest)
	 println "Substitutions for testing: "+subs
	 dbPop = new DatabasePopulator(testDB);
	 //dbPop.populate(new QueryAtom(performsActiv, Activity, StartT, EndT), subs);  // 2 ways, what is better?
	 dbPop.populate(performsActiv(Activity, StartT, EndT).getFormula(), subs);
	 	 
	 // Populate performsActiv predicate with RVs and commit to DBs
	 for (int s = 0; s < numActivities; s++) {
		 if ((s+fold) % folds != 0) {
			 for (GroundTerm[] terms : candidatePerformActivityTermsForQuery[s]) {
				 //println "Committing RV values for training DB: "+terms+" "+terms[0]+" "+terms[1]+" "+terms[2]+" "+terms[0].getClass()+" "+terms[1].getClass()+" "+terms[2].getClass()
				 GroundAtom rv = trainDB.getAtom(performsActiv, terms);// terms[0], terms[1], terms[2]);//new StringAttribute(terms[0].toString().trim()), data.getUniqueID(Integer.valueOf(terms[1].toString())), data.getUniqueID(Integer.valueOf(terms[2].toString())));
				 if (rv instanceof RandomVariableAtom){
					 //println "Committing RV values for training DB: "+terms+" "+terms[0]+" "+terms[1]+" "+terms[2]+" "+terms[0].getClass()+" "+terms[1].getClass()+" "+terms[2].getClass()
					 rv.setValue(0.0)
					 rv.commitToDB();
					 trainDB.commit((RandomVariableAtom)rv);
				 }
			 }
		 }
		 else {
			 for (GroundTerm[] terms : candidatePerformActivityTermsForQuery[s]) {
				 GroundAtom rv = testDB.getAtom(performsActiv, terms);//data.getUniqueID(terms[0]), data.getUniqueID(terms[1]), data.getUniqueID(terms[2]));
				 if (rv instanceof RandomVariableAtom){
					 //println "Commiting RVs values for testing DB: "+terms+" "+terms[0]+" "+terms[1]+" "+terms[2]
					 rv.setValue(0.0)
					 rv.commitToDB();
					 testDB.commit((RandomVariableAtom)rv);
				 }
			 }
		 }
	 }
	 
	 /* Need to close testDB so that we can use write_te for multiple databases. */
	 testDB.close();
	 
	 /* Label DBs */
	 Database labelDB = data.getDatabase(new Partition(partitionIndex++), targetPredicates, (Partition[])trainPartsLabels.toArray());
	 Database truthDB = data.getDatabase(new Partition(partitionIndex++), targetPredicates, (Partition[])testPartsLabels.toArray());
	 
	 /* Compute baseline accuracy  */
	 if (computeBaseline) {
		 println "Computing baseline..."
		 Partition write_base = new Partition(partitionIndex++);
		 Database baselineDB = data.getDatabase(write_base, observedPredicates, (Partition[])testPartsObs.toArray());
		 atoms = Queries.getAllAtoms(baselineDB, timestamp);
		 for (GroundAtom a : atoms) {
			 GroundTerm[] terms = a.getArguments();
			 RandomVariableAtom rv = (RandomVariableAtom)baselineDB.getAtom(performsActiv, terms);
			 println "Baseline: setting RV truth value to performsActiv: "+rv+ " "+a.getValue()+" for terms: "+terms+" from timestamp: "+a.toString()
			 rv.setValue(a.getValue());
			 rv.commitToDB();
			 baselineDB.commit(rv);
		 }
		 
		 System.out.println( "Comparing with truth baseline... "+inserters.length );
		 def compBaseline = new MulticlassPredictionComparator(baselineDB);
		 compBaseline.setBaseline(truthDB);
		 /*Compare method works this way:
		  *   * @param p A predicate
			  * @param labelMap A map indicating the mapping from the label to an index.
			  * @param labelIndex The index of the label in each example's terms. -> the index of the label in the target predicate's parameters
			  * @return A {@link MulticlassPredictionStatistics}*/
		 def baselineStats = compBaseline.compare(performsActiv, activityMap, 0); // last param: The index of the label in each example's terms.
		 log.info("Log-ACTIVITY ACC: {}", baselineStats.getAccuracy());
		 log.info("Log-ACTIVITY F1:  {}", baselineStats.getF1());
		 println "ACTIVITY ACC: "+ baselineStats.getAccuracy();
		 println "ACTIVITY F1: "+ baselineStats.getF1();
		 ConfusionMatrix baselineConMat = baselineStats.getConfusionMatrix();
		 System.out.println("\n Baseline Confusion Matrix:\n" + baselineConMat);//.toMatlabString()); // produces Java heap space error
		 stats_baseline.add(baselineStats);
		 
		 
		 println "Baseline Inference results with hand-defined weights:"
		 formatter = new DecimalFormat("#.##");
		 for (GroundAtom atom : Queries.getAllAtoms(baselineDB, performsActiv))
			 println "Inference results with hand-defined weights: "+atom.toString() + "\t" + formatter.format(atom.getValue());	 
		 
		 baselineDB.close();
		 data.deletePartition(write_base);
	}
 
	 /*** EXPERIMENT ***/
	 
	 log.info("Starting experiment ...");
	 
	 def configName = config.getString("name", "");
	 println "ConfigName: "+configName
	 
	 /* Weight learning */
	 learningMethod = "MLE"; //s = ["MLE", "MPLE", "MM", "EM", "DualEM"] as Set;
	 config.addProperty("learningmethod", learningMethod);
	 def learningMethod = config.getString("learningmethod", "");
	 
	 log.info("Log- Learning model...: {}", m.toString())
	 System.out.println("Learning model.... " + config.getString("name", "") + "\n");// + m.toString())
	 //learn(m, trainDB, labelDB, config, log)
	 log.info("Learned model {}: \n {}", configName, m.toString())
	 System.out.println("Learned model " + config.getString("name", "") + "\n" + m.toString())
 
	 
	 /* Inference on test set */
	 testDB = data.getDatabase(write_test, observedPredicates, (Partition[])testPartsObs.toArray());
	 Set<GroundAtom> targetAtoms = Queries.getAllAtoms(testDB, performsActiv)
	 for (RandomVariableAtom rv : Iterables.filter(targetAtoms, RandomVariableAtom)){
		 //println "Committing RV values for testDB"
		 rv.setValue(0.0)
		 rv.commitToDB()
		 testDB.commit((RandomVariableAtom)rv); // TODO: not done in activ.recognition example
	 }
		 
	 /*for (int j = airIndexStart; j < airIndexEnd; j++) {
		 UniqueID state = data.getUniqueID(j)
		 //println state
		 RandomVariableAtom atomEV = (RandomVariableAtom) trainDB.getAtom(Air, timeStep, state)
		 atomEV.setValue(0.0)
		 atomEV.commitToDB()
	 }*/
	 log.info("Log- Doing MPEInference..")
	 System.out.println("Doing MPEInference...")
	 MPEInference mpe = new MPEInference(m, testDB, config)
	 FullInferenceResult result = mpe.mpeInference()
	 log.info("Objective: {}", result.getTotalWeightedIncompatibility())
	 println "Objective: "+ result.getTotalWeightedIncompatibility()
	 mpe.close();
	 testDB.close();
 
	 /* Open the prediction DB. */
	 Database predDB = data.getDatabase(write_test, targetPredicates);	 
	 /*
	  * Let's see the results
	  */
	 DecimalFormat formatter = new DecimalFormat("#.##");
	 println "Inference results with learned weights:"
	 for (GroundAtom atom : Queries.getAllAtoms(predDB, performsActiv))
		 println "Inference results with learned weights: Pred DB: "+atom.toString() + "\t" + formatter.format(atom.getValue());
	 
		 
	 /* Evaluate performsActiv target predicate */
	 def comparator = new MulticlassPredictionComparator(predDB);
	 comparator.setBaseline(truthDB);
	 /*def stats = comparator.compare(performsActiv, activityMap, 0); // last param: target argument index within predicate
	 log.info("Log- PerformsActiv ACC: {}", stats.getAccuracy());
	 log.info("Log- PerformsActiv F1:  {}", stats.getF1());
	 println "PerformsActiv ACC: "+ stats.getAccuracy();
	 println "PerformsActiv F1: "+ stats.getF1();
	 ConfusionMatrix conMat = stats.getConfusionMatrix();
	 System.out.println("\nResult Confusion Matrix:\n" + conMat);//.toMatlabString());
	 statsPerformsActiv.get(config).add(stats);*/
	 
	 /* Write confusion matrix to file. */
	 File outFile = new File(outPath);
	 outFile.mkdirs();
	 FileOutputStream fileStr = new FileOutputStream(outPath + configName.replace('.', '_') + "-fold" + fold + ".txt");//matrix");
	 ObjectOutputStream objOutStr = new ObjectOutputStream(fileStr);
	 //objOutStr.writeObject(conMat);
	 
	 /////
	 /*int totalTestExamples = 10;// TOFIX testingKeys.get(fold).size() * Activities.size();
	 System.out.println("totalTestExamples " + totalTestExamples)
	 comparator = new DiscretePredictionComparator(predDB);
	 comparator.setBaseline(truthDB);
	 DiscretePredictionStatistics discreteStats = comparator.compare(performsActiv, totalTestExamples)
	 System.out.println("F1 score " + discreteStats.getF1(DiscretePredictionStatistics.BinaryClass.POSITIVE))
	 //discreteStatsPerformsActiv.get(config).add(fold, discreteStats)
	 /////*/
	 
	 /* Close the prediction DB. */
	 predDB.close();
 
	 /* Close all databases. */
	 trainDB.close();
	 labelDB.close();
	 truthDB.close();
	 
	 /* Empty the write partitions */
	 data.deletePartition(write_train);
	 data.deletePartition(write_test);
 }
 
/*** PRINT RESULTS ***/

/* Only run this block if we're doing all  at once. */
if (computeBaseline) {
	println "PRINTING BASELINE RESULTS..."
	log.info("\n\nBASELINE RESULTS\n");
	double avgF1 = 0.0;
	double avgAcc = 0.0;
	List<ConfusionMatrix> cmats = new ArrayList<ConfusionMatrix>();
	for (int i = 0; i < stats_baseline.size(); i++) {
		avgF1 += stats_baseline.get(i).getF1();
		avgAcc += stats_baseline.get(i).getAccuracy();
		ConfusionMatrix cmat = stats_baseline.get(i).getConfusionMatrix();
		cmats.add(cmat)
	}
	/* Average statistics */
	avgF1 /= stats_baseline.size();
	avgAcc /= stats_baseline.size();
	log.info("\nBaseline\n Avg Acc: {}\n Avg F1:  {}\n", avgAcc, avgF1);
	println "\nBaseline Avg Acc and Avg F1: "+  avgAcc+" "+ avgF1;
	/* Cummulative statistics */
	ConfusionMatrix cumCMat = ConfusionMatrix.aggregate(cmats);
	def cumStats = new MulticlassPredictionStatistics(cumCMat);
	log.info("\nBaseline\n Cum Acc: {}\n Cum F1:  {}\n", cumStats.getAccuracy(), cumStats.getF1());
	log.info("\nCum Recall Matrix:\n{}", cumCMat.getRecallMatrix().toMatlabString(3));
	println "Baseline Cum Acc and Cum F1: "+ cumStats.getAccuracy()+ "  "+ cumStats.getF1();
	println "Cum Recall Matrix: \n"+cumCMat.getRecallMatrix().toMatlabString(3);
	
	/*println "Cum Baseline Inference results with hand-defined weights:"
	formatter = new DecimalFormat("#.##");
	for (GroundAtom atom : Queries.getAllAtoms(baselineDB, performsActiv))
		println "Cum Inference results with hand-defined weights: "+atom.toString() + "\t" + formatter.format(atom.getValue());*/
   
		
	/****************************/
	println "PRINTING ALL  RESULTS..."
	log.info("\n\nRESULTS\n");  //	for (ConfigBundle config : configs) {
	def stats = statsPerformsActiv.get(config);
	avgF1 = 0.0;
	avgAcc = 0.0;
	cmats = new ArrayList<ConfusionMatrix>();
	for (int i = 0; i < stats.size(); i++) {
		avgF1 += stats.get(i).getF1();
		avgAcc += stats.get(i).getAccuracy();
		ConfusionMatrix cmat = stats.get(i).getConfusionMatrix();
		cmats.add(cmat)
	}
	/* Average statistics */
	avgF1 /= stats.size();
	avgAcc /= stats.size();
	configName = "C"
	log.info("\n{}\n Avg Acc: {}\n Avg F1:  {}\n", configName, avgAcc, avgF1);
	println "\n Avg Acc and Avg F1: "+  avgAcc+" "+ avgF1;
	/* Cummulative statistics */
	/*cumCMat = ConfusionMatrix.aggregate(cmats);
	cumStats = new MulticlassPredictionStatistics(cumCMat);
	log.info("\n{}\n Cum Acc: {}\n Cum F1:  {}\n", configName, cumStats.getAccuracy(), cumStats.getF1());
	log.info("\nCum Recall Matrix:\n{}", cumCMat.getRecallMatrix().toMatlabString(3));
	println "Cum Acc and Cum F1: "+ cumStats.getAccuracy()+ "  "+ cumStats.getF1();
	println "Cum Recall Matrix: \n"+cumCMat.getRecallMatrix().toMatlabString(3);*/
}


System.out.println("Learned model " + config.getString("name", "") + "\n" + m.toString())

println "End! :)"
/*def methodStats = discreteStatsPerformsActiv.get(config)
println methodStats
for (int fold = 1; fold < +1; fold++) {
	def stats = methodStats.get(fold)
	def b = DiscretePredictionStatistics.BinaryClass.POSITIVE  // when to use, when answers to a predicate can be only 1/0?
	System.out.println("Method " + learningMethod + ", fold " + fold +", acc " + stats.getAccuracy() +
			", prec " + stats.getPrecision(b) + ", rec " + stats.getRecall(b) +
			", F1 " + stats.getF1(b) + ", correct " + stats.getCorrectAtoms().size() +
			", tp " + stats.tp + ", fp " + stats.fp + ", tn " + stats.tn + ", fn " + stats.fn)
}*/
 
public void learn(Model m, Database db, Database labelsDB, ConfigBundle config, Logger log) {
	
	/* get all default weights */
	Map<CompatibilityKernel,Weight> initWeights = new HashMap<CompatibilityKernel, Weight>();
	for (CompatibilityKernel k : Iterables.filter(m.getKernels(), CompatibilityKernel.class))
		initWeights.put(k, k.getWeight());

	println "Learning weights with method: "+config.getString("learningmethod", "")+"..."
	switch(config.getString("learningmethod", "")) {
		case "MLE":
			MaxLikelihoodMPE mle = new MaxLikelihoodMPE(m, db, labelsDB, config)
			mle.learn()
			println "Weights for MPE learnt!"
			break
		case "MPLE":
			MaxPseudoLikelihood mple = new MaxPseudoLikelihood(m, db, labelsDB, config)
			mple.learn()
			break
		case "MM":
			MaxMargin mm = new MaxMargin(m, db, labelsDB, config)
			mm.learn()
			break
		/*case "EM":
			//HardEM hardEM = new MaxMargin(m, db, labelsDB, config) // Arti's mooc paper
			//hardEM.learn()
			//break
		//case "DualEM":
			DualEM weightLearning = new DualEM(model, distributionDB, truthDB, cb);
			weightLearning.learn();
			weightLearning.close();*/
		default:
			throw new IllegalArgumentException("Unrecognized method.");
		println "Weights learnt!"
	}
}

public int countFileLines(String file) {
	def lines = 0
	new File(file).eachLine {
		lines++
	}
	return lines
}
