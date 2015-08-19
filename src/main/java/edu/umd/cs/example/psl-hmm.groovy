/* PREDICATES */

m.add predicate: "follows", types: [ArgumentType.UniqueID,ArgumentType.UniqueID];
m.add predicate: "hidState", types: [ArgumentType.UniqueID,ArgumentType.Integer];
m.add predicate: "objState", types: [ArgumentType.UniqueID,ArgumentType.Integer];

/* CONSTANTS */

/* State spaces */
def hidStateSpace = [1,2,3];
def obsStateSpace = [1,2,3,4,5];

/* HMM RULES (full probability tables) */

for (int hs1 : hidStateSpace) {

	/* Transitions between hidden states */
	for (int hs2 : hidStateSpace) {
		/*
		 If time T2 follows T1, and the hidden state at T1 is hs1,
		 then the hidden state at T2 is hs2.
		 */
		m.add rule: follows(T1,T2) hidState(T1,hs1) >> hidState(T2,hs2), weight: 1;
	}
	/* Emissions (observations) */
	for (int os : obsStateSpace) {
		/* If hidden state at T is hs1, then observation at T is os. */
		m.add rule: hidState(T,hs1) >> obsState(T,os), weight: 1;
	}
	
}

/*
 Compact HMM rules, for when learning the full probability tables is infeasible.
 If you uncomment these rules, make sure to comment-out lines 15-31.
 */
// m.add rule: follows(T1,T2) hidState(T1,HS1) >> hidState(T2,HS2), weight: 1;
// m.add rule: hidState(T,HS) >> obsState(T,OS), weight: 1;

/*
 Functional constraints on hidState, obsState mean that soft-truth values for any given T
 should sum to 1.
 A probabilistic interpretation is that there is a distribution over hidden states (or
 observed states) at time T.
 */
m.add PredicateConstraint.Functional, on: hidState;
m.add PredicateConstraint.Functional, on: obsState;
