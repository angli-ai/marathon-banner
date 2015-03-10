/**
 * 
 */
package org.scripps.crowdwords;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import bioc.BioCAnnotation;
import bioc.BioCCollection;
import bioc.BioCDocument;
import bioc.BioCLocation;
import bioc.BioCPassage;
import bioc.io.BioCCollectionReader;
import bioc.io.BioCDocumentWriter;
import bioc.io.BioCFactory;

/**
 * @author jbrugg
 *
 */

// This one's junk, pretty much tests a silly cost-saving method where you first see if two people agree on a set of annotations. If they don't, you look at a 3rd turker. 
// Then accept any annotation that at least 2 agree on is voted into the resulting annotation set.


public class TestSimpleAgreement {

	/**
	 * @param args
	 * @throws XMLStreamException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws XMLStreamException, IOException {	
		String mturkfile = "data/mturk/ncbitrain_e11_bioc.xml";
		//"data/mturk/newpubmed_e12_13_bioc.xml"; 
		String goldfile = "data/ncbi/ncbi_train_bioc.xml";
		//null;
		String k_dir = "data/mturk/ncbitrain_e11_voting/";
		//"data/mturk/newpubmed_e12_13_voting/";//
		
		executeSimpleAgreement(mturkfile, goldfile, k_dir);
	}
	
	public static void executeSimpleAgreement(String mturkfile, String goldfile, String k_dir) throws XMLStreamException, IOException{
		if(goldfile==null){
			System.out.println("No gold standard, just exporting voting results.");
		}else{
			System.out.println("Exporting voting results and evaluating each against gold standard");
		}
		AnnotationComparison ac = new AnnotationComparison();

		//read in the mturk annotations
		BioCCollection mturk_collection = TestAggregation.readBioC(mturkfile);
		//get the full text for export later
		Map<String, Document> id_doc = TestAggregation.convertBioCCollectionToDocMap(mturk_collection);
		//convert to local annotation representation
		List<Annotation> mturk_annos = TestAggregation.convertBioCtoAnnotationList(mturk_collection);

		BioCCollection gold_collection = null;
		List<Annotation> gold_annos = null;
		if(goldfile!=null){
			//load gold standard annotations
			gold_collection = TestAggregation.readBioC(goldfile);
			gold_annos = TestAggregation.convertBioCtoAnnotationList(gold_collection);
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
				System.out.println("n_gold_annos_removed "+n_gold_annos_removed);
				gold_annos = keep_annos;
			}
		}
		//build different k (voting) thresholds for annotations from turkers
		Aggregator agg = new Aggregator();
		Map<Integer, List<Annotation>> tatorList = getAnnoMap(mturk_annos);
		Map<Integer, Double> expList = agg.getAnnotatorExperience(mturk_annos);
		Map<Integer, Double> trustList = agg.getAnnotatorTrust(mturk_annos, gold_annos);
		List<Integer> mostTrusted = getMost3Trust(trustList,expList);
		
		Set<Annotation> firstLook = new HashSet<Annotation>(tatorList.get(mostTrusted.get(0)));
		Set<Annotation> secondLook = new HashSet<Annotation>(tatorList.get(mostTrusted.get(1)));
		Set<Annotation> thirdLook = new HashSet<Annotation>(tatorList.get(mostTrusted.get(2)));
		Set<Annotation> dupSecondLook = new HashSet<Annotation>(tatorList.get(mostTrusted.get(1)));
		
		secondLook.retainAll(firstLook);
		thirdLook.retainAll(firstLook);
		dupSecondLook.retainAll(thirdLook);
		
		thirdLook.addAll(secondLook);
		thirdLook.addAll(dupSecondLook);
		
		List<Annotation> agree = new ArrayList<Annotation>();
		
		for(Annotation anno: thirdLook){
			if(!agree.contains(anno)){
				agree.add(anno);
			}
		}
		
		ComparisonReport report = ac.compareAnnosCorpusLevel(gold_annos, agree, "A = 2");
		System.out.println("Exp=2"+"\t"+report.getRow());
		//Method only runs once in this version
	}
	public static Map<Integer, List<Annotation>> getAnnoMap(List<Annotation> annoList){
		Map<Integer, List<Annotation>> annosByTator = new TreeMap<Integer, List<Annotation>>();
		
		Integer userId;
		for(Annotation anno: annoList){
			userId = anno.getUser_id();
			List<Annotation> tatorAnnos = annosByTator.get(userId);
			if(tatorAnnos == null){
				tatorAnnos = new ArrayList<Annotation>();
				
			}
			Annotation simple = new Annotation(anno.getText(),anno.getStart(),anno.getStop(),anno.getDocument_id(),anno.getDocument_section(),"loc");
			tatorAnnos.add(simple);
			annosByTator.put(userId,tatorAnnos);
		}
		
		return annosByTator;
	}
	public static List<Integer> getMost3Trust(Map<Integer, Double> trustList,Map<Integer, Double> expList){
		Double maxTrust = 0.0, midTrust=0.0, minTrust=0.0, idTrust;
		Integer maxId, midId, minId;
		Map<Double, List<Integer>> tatorTrust  = new TreeMap<Double, List<Integer>>(); //TODO reverse map - then take max 3 and return tator IDs
		for(Integer id: trustList.keySet()){
			idTrust = trustList.get(id)*-1;
			List<Integer> idList = tatorTrust.get(idTrust);
			if(idList == null){
				idList = new ArrayList<Integer>();
			}
			idList.add(id);
			tatorTrust.put(idTrust,idList); //Tator id's mapped to trust scores
		}
		List<Integer> mostTrusted = new ArrayList<Integer>();

		for(Double trust: tatorTrust.keySet()){
			if(mostTrusted.size() > 6){break;}
			mostTrusted.addAll(tatorTrust.get(trust));
		}
		
		//Add a thing to grab most experienced of those most trusted
		
		Map<Double, List<Integer>> tatorExp = new TreeMap<Double, List<Integer>>();
		for(Integer id: mostTrusted){
			Double exp = expList.get(id)*-1;
			List<Integer> idList = tatorExp.get(exp);
			if(idList==null){
				idList = new ArrayList<Integer>();
			}
			idList.add(id);
			tatorExp.put(exp, idList);
		}
		List<Integer> mostExperienced = new ArrayList<Integer>();
		
		for(Double exp: tatorExp.keySet()){
			if(mostExperienced.size() > 3){break;}
			mostExperienced.addAll(tatorExp.get(exp));
		}
		mostExperienced = mostExperienced.subList(0, 3);
		
		return mostExperienced;
	}
}