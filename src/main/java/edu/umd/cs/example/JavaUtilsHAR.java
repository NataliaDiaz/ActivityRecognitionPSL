package edu.umd.cs.example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.atom.GroundAtom;
import edu.umd.cs.psl.model.predicate.Predicate;
import edu.umd.cs.psl.util.database.Queries;

public class JavaUtilsHAR {

	public static boolean ImmediatelyFollows(Integer time1, Integer time2) {
		if (time1 < time2)
			return true;
		else
			if (time1 > time2)
				return false;
			else
				System.exit(-1);
		return false;
	}
	
	
}
