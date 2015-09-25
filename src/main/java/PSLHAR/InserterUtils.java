package PSLHAR;

/*
 * This file is part of the PSL software.
 * Copyright 2011 University of Maryland
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
//package edu.umd.cs.linqs.action;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.Arrays;
import edu.umd.cs.psl.database.DataStore;
import edu.umd.cs.psl.database.Partition;
import edu.umd.cs.psl.database.loading.Inserter;
import edu.umd.cs.psl.model.predicate.Predicate;
import edu.umd.cs.psl.model.predicate.PredicateFactory;
import edu.umd.cs.psl.model.predicate.StandardPredicate;
import edu.umd.cs.psl.ui.data.file.util.DelimitedObjectConstructor;
import edu.umd.cs.psl.ui.data.file.util.LoadDelimitedData;

/**
 * Utility methods for common data-loading tasks.
 */
public class InserterUtils {
	
	public static int nPartitions = 100000; // made not final
	//private static final Logger log = LoggerFactory.getLogger(InserterUtils.class);
	private static final String delimiter = ",";
	private static String predName = "";
	private static int parameterIndexAsForeignKey;
	//private static int datapointIndex = 0;
	
	public static Inserter[] getMultiPartitionInserters(DataStore data, StandardPredicate pred, Partition[] partitions, int numPartitions, int parameterIndexForeignKey) {
		predName = pred.getName();
		parameterIndexAsForeignKey = parameterIndexForeignKey;
		nPartitions = numPartitions;
		//datapointIndex = 0;
		System.out.println("Populating data for predicate: "+ predName+". NumPartitions: "+nPartitions );//+" sampleIDIndexPos: "+sampleIDIndexPos+"...");
		Inserter[] inserters = new Inserter[numPartitions];
		for (int i = 0; i < nPartitions; i++) {
			//System.out.println("inserting part "+ parts[i]);
			inserters[i] = data.getInserter(pred, partitions[i]);
		}
		return inserters;
	}
	
	public static void loadDelimitedDataMultiPartition(final Inserter[] inserters, String file, String delimiter, boolean insertAllPredicatesInEachDataPointPartition, final String datapointToSampleIDMappingFile) {
		
		LoadDelimitedData.loadTabData(file, new DelimitedObjectConstructor<String>(){
			
			@Override
			public String create(String[] data) {
				int datapointPartitionIndex;
				Integer foreignKey;
				try {
					foreignKey = Integer.valueOf(data[parameterIndexAsForeignKey].trim());
					datapointPartitionIndex = getSampleIDForInt(foreignKey, datapointToSampleIDMappingFile);
					if(datapointPartitionIndex <0 || datapointPartitionIndex >=nPartitions){
						System.out.println( "Error in InserterUtils.loadDelimitedDataMultiPartition, foreign key value: "+foreignKey + " returned index out of partition index set");
						System.exit(-1);
					}
				} catch (NumberFormatException e) {
					throw new AssertionError("Could not parse sequence ID from bounding box ID: " + data[parameterIndexAsForeignKey].trim());
				}
				//System.out.println( "End timestamp "+foreignKey + " belongs to sample (partition) index: " +datapointPartitionIndex);
				Inserter insert = inserters[datapointPartitionIndex];
				insert.insert((Object[])data);
				return null;
			}

			@Override
			public int length() {
				return 0;
			}
			
		}, delimiter);
	}
	
	public static void loadDelimitedDataMultiPartition(final Inserter[] inserters, String file, boolean insertAllPredicatesInEachDataPointPartition, final String datapointToSampleIDMappingFile) {
		loadDelimitedDataMultiPartition(inserters, file, delimiter, insertAllPredicatesInEachDataPointPartition, datapointToSampleIDMappingFile); //LoadDelimitedData.defaultDelimiter, DEFAULT_SEQ_MULT
		//System.out.println("Done!");
	}
	
	public static void loadDelimitedDataTruthMultiPartition(final Inserter[] inserters, final String file, String delimiter, final boolean insertAllPredicatesInEachDataPointPartition, final String datapointToSampleIDMappingFile) {
		LoadDelimitedData.loadTabData(file, new DelimitedObjectConstructor<String>(){

			@Override
			public String create(String[] data) {
				double truth;
				int datapointPartitionIndex = -1;
				String foreignKeyValue = "";
				try {
					if(!insertAllPredicatesInEachDataPointPartition){	
						foreignKeyValue = data[parameterIndexAsForeignKey].trim();						
						if(datapointToSampleIDMappingFile.length() >0){
							//System.out.println(foreignKeyValue+ "Using Dict. "+ datapointToSampleIDMappingFile+" to get partition Id for pred:"+file);
							datapointPartitionIndex = getSampleIDForInt(Integer.valueOf(foreignKeyValue), datapointToSampleIDMappingFile);
							if(datapointPartitionIndex <0 || datapointPartitionIndex >= nPartitions){
								System.out.println( file+"Error in InserterUtils.loadDelimitedDataMultiPartition, foreign key value: "+foreignKeyValue + " returned index out of partition index set");
								System.exit(-1);
							}								
						}							
					}
				} catch (NumberFormatException e) {
					throw new AssertionError("Could not parse sequence ID from bounding box ID: " + data[parameterIndexAsForeignKey].trim());
				}
				
				try {
					truth = Double.parseDouble(data[data.length-1].trim());
					//System.out.println( "truth: "+truth);
				} catch (NumberFormatException e) {
					throw new AssertionError("Could not read truth value for data: " + Arrays.toString(data));
				}
				if (truth<0.0 || truth>1.0)
					throw new AssertionError("Illegal truth value encountered: " + truth);
				
				if(!insertAllPredicatesInEachDataPointPartition){  // INSERT PREDICATE ONLY IN ONE PARTITION, THE CORRESPONDING DATAPOINT EXAMPLE.
					//System.out.println( "Frame "+foreignKeyValue + " belongs to sample (partition) index: " +datapointPartitionIndex);
					Object[] newdata = new Object[data.length-1];
					//System.out.println( "newdata: "+newdata + "  size "+ newdata.length +" data: "+data);
					System.arraycopy(data, 0, newdata, 0, newdata.length);
					//System.out.println( "newdata length: "+newdata.length);
					//System.out.println( file+" Inserting grounded terms into partition index: "+datapointPartitionIndex );
					Inserter insert = inserters[datapointPartitionIndex];
					insert.insertValue(truth, newdata);
				}
				else{	// We insert all predicates in all possible partitions (e.g. hasProperty)
					//System.out.println( "Inserting in all partitions all predicates in: "+file+"\n" );
					for(int inserterIndex =0; inserterIndex< inserters.length; inserterIndex++){
						Inserter insert = inserters[inserterIndex];
						Object[] newdata = new Object[data.length-1];
						//System.out.println( "newdata: "+newdata + "  size "+ newdata.length +" data: "+data);
						System.arraycopy(data, 0, newdata, 0, newdata.length);
						insert.insertValue(truth, newdata);
					}					
				}			
				return null;
			}
			@Override
			public int length() {
				return 0;
			}
			
		}, delimiter);
	}
	
	public static void loadDelimitedDataTruthMultiPartition(final Inserter[] inserters, String file, boolean insertAllPredicatesInEachDataPointPartition, final String datapointToSampleIDMappingFile) {
		loadDelimitedDataTruthMultiPartition(inserters, file, delimiter, insertAllPredicatesInEachDataPointPartition, datapointToSampleIDMappingFile);
		//loadDelimitedDataTruthMultiPartition(inserters, file, LoadDelimitedData.defaultDelimiter, DEFAULT_SEQ_MULT);
		//System.out.println("Done!");
	}
	
	public static int getSampleIDForInt(Integer foreignKeyValue, String datapointToSampleIDMappingFile){
		try {
			if(datapointToSampleIDMappingFile.length() >0){
				HashMap<Integer,Integer> featureSampleIDDict = getFileContentIntoIntCategoriesDictionary(datapointToSampleIDMappingFile);
				//System.out.println(featureSampleIDDict+ "used dict");
				//System.out.println(featureSampleIDDict.keySet().size()+ "Key and value: "+String.valueOf(foreignKeyValue));//+"  "+featureSampleIDDict.get(foreignKeyValue));
				return featureSampleIDDict.get(foreignKeyValue);
			}
			else{
				System.out.println("Error in loadDelimitedDataTruthMultiPartition and getSampleID: the file "+datapointToSampleIDMappingFile+" with the foreign key mapping to the datapoint id is not found!");
				System.exit(-1);
				//System.out.println( "truth: "+truth);
			}
		} catch (NumberFormatException e) {
			//	System.exit(-1);
			throw new AssertionError("Could not read datapoint id from file: " + datapointToSampleIDMappingFile);
		}
		return 0;
	}
	
	public static HashMap<String, Integer> getFileContentIntoStrCategoriesDictionary(String fileName) {
        FileReader file;
        String line = "";
        HashMap<String, Integer> dict = new HashMap<String, Integer>();
        try {
            file = new FileReader(fileName);
            BufferedReader reader = new BufferedReader(file);
            try {
                while ((line = reader.readLine()) != null) {
                    dict.put(line.split(", ")[0].trim(), Integer.valueOf(line.split(", ")[1].trim()));
                    //returnValue += line + "\n";
                }
            } finally {
                reader.close();
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found");
        } catch (IOException e) {
            throw new RuntimeException("IO Error occurred");
        }
        return dict;
    }
	
	public static ArrayList<Integer> getKeyTimestampsForQuery(String fileName, int column) {
        FileReader file;
        String line = "";
        ArrayList<Integer> dict = new ArrayList<Integer>();
        try {
            file = new FileReader(fileName);
            BufferedReader reader = new BufferedReader(file);
            try {
                while ((line = reader.readLine()) != null) {
                    dict.add(Integer.valueOf(line.split(", ")[column].trim()));
                }
            } finally {
                reader.close();
                //System.out.println("Final Arraylist: "+dict+ "  size: "+dict.size());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found");
        } catch (IOException e) {
            throw new RuntimeException("IO Error occurred");
        }
        return dict;
    }
	
	public static HashMap<Integer, Integer> getFileContentIntoIntCategoriesDictionary(String fileName) {
        FileReader file;
        String line = "";
        HashMap<Integer, Integer> dict = new HashMap<Integer, Integer>();
        try {
            file = new FileReader(fileName);
            BufferedReader reader = new BufferedReader(file);
            try {
                while ((line = reader.readLine()) != null) {
                	//System.out.println("Filename "+fileName+" "+line);
                    dict.put(Integer.valueOf(line.split(", ")[0].trim()), Integer.valueOf(line.split(", ")[1].trim()));
                    //returnValue += line + "\n";
                }
            } finally {
                reader.close();
                //System.out.println("Final constructed dictionary: "+dict+ "  size: "+dict.keySet().size());
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found");
        } catch (IOException e) {
            throw new RuntimeException("IO Error occurred");
        }
        return dict;
    }

}