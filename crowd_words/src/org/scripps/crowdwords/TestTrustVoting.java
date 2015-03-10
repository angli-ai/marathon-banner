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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  //Tests trust voting method

public class TestTrustVoting {

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
		
		executeVotingTrust(mturkfile, goldfile, k_dir);
	}
	
	public static void executeVotingTrust(String mturkfile, String goldfile, String k_dir) throws XMLStreamException, IOException{
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
		Integer cnt;
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
		Map<Double,List<Annotation>> trust_annos = agg.getAnnotationsAtAllTrust(mturk_annos,gold_annos);
		for(Double exp : trust_annos.keySet()){
			List<Annotation> annos = trust_annos.get(exp);
			if(goldfile!=null){
				//execute comparison versus gold, report results
				ComparisonReport report = ac.compareAnnosCorpusLevel(gold_annos, annos, "K="+exp);
				System.out.println("Trust="+exp+"\t"+report.getRow());
			}
			//export a BioC version for banner
			//BioCCollection k_collection = convertAnnotationsToBioC(annos, id_doc,"mturk k="+exp,"",1000000);
			//writeBioC(k_collection, k_dir+"_"+exp+".xml");
		}
	}
}