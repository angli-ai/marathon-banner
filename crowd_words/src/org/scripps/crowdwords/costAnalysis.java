/**
 * 
 */
package org.scripps.crowdwords;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import bioc.*;
/**
 * @author jbrugg
**/

public class costAnalysis{
	
	public static void main(String[] args) throws XMLStreamException, IOException{
		//Set up and run cost tests, also aggregate here?
		
		String mturkfile = "data/mturk/ncbitrain_e11_bioc.xml";

		String goldfile = "data/ncbi/ncbi_train_bioc.xml";

		String cost_dir = "output/costAnalysisByDoc/";
		
		Integer nTests = 3;
		
		Integer maxCost = 15*539;
		Integer costStep = 539;
		
		Integer minTestTators = 3;
		Integer maxTestTators = 7;
		
		double maxTestAcceptance = 1.0;
		double acceptanceStep = 0.1;
		
		double maxTestRejection = 1.0;
		double rejectionStep = 0.1;
		
		
		for(Integer cost = 539; cost <=maxCost; cost += costStep){
			PrintWriter writer = new PrintWriter(cost_dir+cost+".tsv");
			writer.println("MaxCost \t MinTurkers \t MinAcceptance \t MaxRejection \t pid \t AverageCost \t AverageF");
			for(Integer minTators = minTestTators; minTators <= maxTestTators; minTators++){
				for(double minimumAcceptance = 0.0; minimumAcceptance <= maxTestAcceptance; minimumAcceptance += acceptanceStep){
					for(double maximumRejection = 0.0; maximumRejection <= minimumAcceptance-0.1; maximumRejection += rejectionStep){
						Float avgCost = 0.0f;
						Float avgF = 0.0f;
						
						Map<String, Float> costMap = new HashMap<String, Float>();
						Map<String, Float> fMap = new HashMap<String, Float>();

	
						System.out.println(cost + "\t" + minTators + "\t" +  minimumAcceptance + "\t" + maximumRejection);
						
						for(int iter = 0; iter < nTests; iter++){
							List<costOutput> docTests = runDocumentCostTest(mturkfile,goldfile,cost,minTators,minimumAcceptance,maximumRejection);
							
							for(costOutput indivTest: docTests){
								String id = indivTest.docId;
								
								float docCost = indivTest.cost;
								float docF = indivTest.fmeasure;
								
								avgCost = costMap.get(id);
								avgF = fMap.get(id);
								
								if(avgCost == null){avgCost = 0.0f;}
								if(avgF == null){avgF = 0.0f;}
								
								avgCost += docCost / nTests;
								avgF += docF / nTests;
								
								costMap.put(id,avgCost);
								fMap.put(id,avgF);
							}
						}
						
						//avgCost = avgCost / nTests;
						//avgF = avgF / nTests;
						for(String id: costMap.keySet()){
							avgCost = costMap.get(id);
							avgF = fMap.get(id);
							
							writer.println(cost+ "\t" + minTators + "\t" + minimumAcceptance + "\t" + maximumRejection + "\t" + id + "\t" + avgCost + "\t" + avgF);
						}
						//System.out.println(cost+ "\t" + minTators + "\t" + minimumAcceptance + "\t" + maximumRejection + "\t" + avgCost + "\t" + avgF);
					}
				}
			}			
			writer.close();
		}
		
	}
	public static costOutput runCorpusCostTest(String mturkfile, String goldfile, Integer maxCost, Integer minTators, Double acceptLevel, Double rejectLevel) throws XMLStreamException, IOException{
		
		AnnotationComparison ac = new AnnotationComparison();

		
		//read in the mturk annotations
		BioCCollection mturk_collection = TestAggregation.readBioC(mturkfile);
		//get the full text for export later
		Map<String, Document> id_doc = TestAggregation.convertBioCCollectionToDocMap(mturk_collection);
		
		//convert to local annotation representation
		List<Annotation> mturk_annos = TestAggregation.convertBioCtoAnnotationList(mturk_collection);

		BioCCollection gold_collection = null;
		List<Annotation> gold_annos = null;
		Map<String, Document> id_gold = null;
		if(goldfile!=null){
			//load gold standard annotations
			gold_collection = TestAggregation.readBioC(goldfile);
			gold_annos = TestAggregation.convertBioCtoAnnotationList(gold_collection);
			id_gold = TestAggregation.convertBioCCollectionToDocMap(gold_collection);
			//filter out annotations from the gold set for docs with no mturk annotations
			boolean common_docs_only = true;
			int n_gold_annos_removed = 0;
			if(common_docs_only){
				List<Annotation> keep_annos = new ArrayList<Annotation>();
				Map<Integer, Set<Annotation>> testdoc_annos = ac.listtomap(mturk_annos);
				Set<Integer> test_ids = testdoc_annos.keySet();
				for(Annotation ganno : gold_annos ){
					if(test_ids.contains(ganno.getDocument_id())){
						keep_annos.add(ganno);
					}
				}
				n_gold_annos_removed = gold_annos.size()-keep_annos.size();
				gold_annos = keep_annos;
			}
		}
		
		//apparatus for testing with single document
		
		/*
		List<Annotation> doc_gold = new ArrayList<Annotation>();
		for(Annotation gold: gold_annos){
			if(gold.getDocument_id() == 10802668){
				doc_gold.add(gold);
			}
		}
		 */
		
		List<String> tatorEvents = new ArrayList<String>();
		
		Map<String, List<Annotation>> annosByTator = new TreeMap<String, List<Annotation>>();
		for(Annotation anno: mturk_annos){
			String id = anno.getUser_id() + "" +anno.getDocument_id();
			List<Annotation> turkList = annosByTator.get(id);
			if(turkList == null){turkList = new ArrayList<Annotation>(); tatorEvents.add(id);}
			turkList.add(anno);
			annosByTator.put(id, turkList);
		}
		
		
		Collections.shuffle(tatorEvents);
		
		//Runs through randomized annotations and individually adds them to documents
		Integer incurredCost = 0;
		for(String id: tatorEvents){
			List<Annotation> turkAnnos = annosByTator.get(id);
			Integer doc_id = turkAnnos.get(0).getDocument_id();
			if(incurredCost > maxCost){break;}
			//if(doc_id != 10802668){continue;}  //only look at one doc
			Document doc = id_doc.get(doc_id+"");
			if(doc.completion){continue;}  //if document is done, skip
			incurredCost++;
			doc.addAnnotations(turkAnnos);
			doc.addTators(turkAnnos.get(0).getUser_id());
			doc = updateDoc(doc, minTators,acceptLevel, rejectLevel);
			id_doc.put(doc_id+"",doc);
			//System.out.println(doc.getAnnotations().size() + "\t" + doc.getTators().size() + "\t" + doc.completion);
			/*for(Annotation ann: doc.getAnnotations()){
				System.out.println(ann.getStart() + "\t" + ann.getStop() + "\t" + ann.getText());
			}*/
		}
		
		
		//Now need to compile list of accepted annotations (check if complete, if not need to manually assess)
		
		List<Annotation> voted_annos = new ArrayList<Annotation>();
		
		for(String doc_id: id_doc.keySet()){
			Document doc = id_doc.get(doc_id);
			//if(!doc_id.equals("10802668")){continue;}  //only look at one doc
			if(doc.completion){
				voted_annos.addAll(doc.getAnnotations());
			}
			else{
				List<Annotation> doc_annos = assessDoc(doc,acceptLevel,rejectLevel);
				voted_annos.addAll(doc_annos);
			}
		}
		//Now find f-score for our voted_annos
		ComparisonReport rep = ac.compareAnnosCorpusLevel(gold_annos, voted_annos,"");
		
		rep.getRow();
		
		costOutput testOutput = new costOutput(incurredCost, rep.consistency/100);
		return testOutput;
	}
	
	public static List<costOutput> runDocumentCostTest(String mturkfile, String goldfile, Integer maxCost, Integer minTators, Double acceptLevel, Double rejectLevel) throws XMLStreamException, IOException{
		
		AnnotationComparison ac = new AnnotationComparison();

		
		//read in the mturk annotations
		BioCCollection mturk_collection = TestAggregation.readBioC(mturkfile);
		//get the full text for export later
		Map<String, Document> id_doc = TestAggregation.convertBioCCollectionToDocMap(mturk_collection);
		
		//convert to local annotation representation
		List<Annotation> mturk_annos = TestAggregation.convertBioCtoAnnotationList(mturk_collection);

		BioCCollection gold_collection = null;
		List<Annotation> gold_annos = null;
		Map<String, Document> id_gold = null;
		if(goldfile!=null){
			//load gold standard annotations
			gold_collection = TestAggregation.readBioC(goldfile);
			gold_annos = TestAggregation.convertBioCtoAnnotationList(gold_collection);
			id_gold = TestAggregation.convertBioCCollectionToDocMap(gold_collection);
			//filter out annotations from the gold set for docs with no mturk annotations
			boolean common_docs_only = true;
			int n_gold_annos_removed = 0;
			if(common_docs_only){
				List<Annotation> keep_annos = new ArrayList<Annotation>();
				Map<Integer, Set<Annotation>> testdoc_annos = ac.listtomap(mturk_annos);
				Set<Integer> test_ids = testdoc_annos.keySet();
				for(Annotation ganno : gold_annos ){
					if(test_ids.contains(ganno.getDocument_id())){
						keep_annos.add(ganno);
					}
				}
				n_gold_annos_removed = gold_annos.size()-keep_annos.size();
				gold_annos = keep_annos;
			}
		}
		
		//apparatus for testing with single document
		
		/*
		List<Annotation> doc_gold = new ArrayList<Annotation>();
		for(Annotation gold: gold_annos){
			if(gold.getDocument_id() == 10802668){
				doc_gold.add(gold);
			}
		}
		 */
		
		List<String> tatorEvents = new ArrayList<String>();
		
		Map<String, List<Annotation>> annosByTator = new TreeMap<String, List<Annotation>>();
		for(Annotation anno: mturk_annos){
			String id = anno.getUser_id() + "_" +anno.getDocument_id();
			List<Annotation> turkList = annosByTator.get(id);
			if(turkList == null){turkList = new ArrayList<Annotation>(); tatorEvents.add(id);}
			turkList.add(anno);
			annosByTator.put(id, turkList);
		}
		
		
		Collections.shuffle(tatorEvents);
		
		//Runs through randomized annotations and individually adds them to documents
		Integer incurredCost = 0;
		
		Map<String, Integer> turkersOnDoc = new HashMap<String, Integer>();
		
		for(String id: tatorEvents){
			List<Annotation> turkAnnos = annosByTator.get(id);
			Integer doc_id = turkAnnos.get(0).getDocument_id();
			if(incurredCost > maxCost){break;}
			//if(doc_id != 10802668){continue;}  //only look at one doc
			Document doc = id_doc.get(doc_id+"");
			if(doc.completion){continue;}  //if document is done, skip
			
			Integer numTurkers = turkersOnDoc.get(doc_id+"");
			if(numTurkers==null){numTurkers=0;}
			numTurkers++;
			turkersOnDoc.put(doc_id+"", numTurkers);
			
			incurredCost++;
			doc.addAnnotations(turkAnnos);
			doc.addTators(turkAnnos.get(0).getUser_id());
			doc = updateDoc(doc, minTators,acceptLevel, rejectLevel);
			id_doc.put(doc_id+"",doc);
			//System.out.println(doc.getAnnotations().size() + "\t" + doc.getTators().size() + "\t" + doc.completion);
			/*for(Annotation ann: doc.getAnnotations()){
				System.out.println(ann.getStart() + "\t" + ann.getStop() + "\t" + ann.getText());
			}*/
		}
		
		
		//Now need to compile list of accepted annotations (check if complete, if not need to manually assess)
		
		List<Annotation> voted_annos = new ArrayList<Annotation>();
		
		for(String doc_id: id_doc.keySet()){
			Document doc = id_doc.get(doc_id);
			//if(!doc_id.equals("10802668")){continue;}  //only look at one doc
			if(doc.completion){
				voted_annos.addAll(doc.getAnnotations());
			}
			else{
				List<Annotation> doc_annos = assessDoc(doc,acceptLevel,rejectLevel);
				voted_annos.addAll(doc_annos);
			}
		}
		//Now find f-score for our voted_annos
		List<ComparisonReport> reps = ac.compareAnnosDocumentLevel(gold_annos, voted_annos);
		
		List<costOutput> testOutput = new ArrayList<costOutput>();
		
		for(ComparisonReport rep: reps){
			rep.getRow();
			String id = rep.getId();
			Integer cost = turkersOnDoc.get(id);
			if(cost == null){cost = 0;}
			costOutput docOutput = new costOutput(cost, rep.consistency/100,rep.getId());
			testOutput.add(docOutput);
		}
		return testOutput;
	}
	
	public static Document updateDoc(Document doc, Integer minTators, Double acceptLevel, Double rejectLevel){
		//First step is to create counts
		Map<Annotation, Double> countMap = new HashMap<Annotation, Double>();
		for(Annotation anno: doc.getAnnotations()){
			Annotation simple = new Annotation(anno.getText(), anno.getStart(), anno.getStop(), anno.getDocument_id(), anno.getDocument_section(), "loc");
			
			Double cnt = countMap.get(simple);
			if(cnt == null){cnt = 0.0;}
			cnt += 1.0/doc.getTators().size();
			countMap.put(simple, cnt);
		}
		
		Boolean done = true;
		List<Annotation> votedAnnos = new ArrayList<Annotation>();
		for(Annotation anno: countMap.keySet()){
			Double cnt = countMap.get(anno);
			if(cnt > acceptLevel){votedAnnos.add(anno);}
			else if(cnt <= acceptLevel && cnt >= rejectLevel){
				done = false;
				break;
			}
		}
		if(done && doc.getTators().size() >= minTators){
			doc.rewriteAnnos(votedAnnos);
			doc.markComplete();
		}
		
		return doc;
	}
	
	public static List<Annotation> assessDoc(Document doc, Double acceptLevel, Double rejectLevel){
		//System.out.println("assessing...");
		Map<Annotation, Double> countMap = new HashMap<Annotation, Double>();
		for(Annotation anno: doc.getAnnotations()){
			//System.out.println(anno.getStart() + "\t " + anno.getStop() + "\t" + anno.getDocument_id()+ "\t" + anno.getText());
			Annotation simple = new Annotation(anno.getText(), anno.getStart(), anno.getStop(), anno.getDocument_id(), anno.getDocument_section(), "loc");
			Double cnt = countMap.get(simple);
			if(cnt == null){cnt = 0.0;}
			cnt += 1.0/doc.getTators().size();
			countMap.put(simple, cnt);
		}
		
		//In this case we'll just pull all Annotations above acceptance level, can adjust later
		List<Annotation> acceptDocs = new ArrayList<Annotation>();
		for(Annotation anno: countMap.keySet()){
			Double cnt = countMap.get(anno);
			//System.out.println(cnt + "\t" + anno.getText() + "\t" + anno.getStart() + "\t" + anno.getStop());
			if(cnt > acceptLevel){acceptDocs.add(anno);}
		}
		
		return acceptDocs;
	}
	
}
