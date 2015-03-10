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
public class TestCrowdAgreement {

	/**
	 * @param args
	 * @throws XMLStreamException 
	 * @throws IOException 
	 */
	
	//Tests getAnnotationsAtAllAgreement method in Aggregator.java
	
	public static void main(String[] args) throws XMLStreamException, IOException {	
		String mturkfile = "data/mturk/ncbitrain_e11_bioc.xml";
		//"data/mturk/newpubmed_e12_13_bioc.xml"; 
		String goldfile = "data/ncbi/ncbi_train_bioc.xml";
		//null;
		String k_dir = "data/mturk/ncbitrain_e11_voting/";
		//"data/mturk/newpubmed_e12_13_voting/";//
		//makeAgreementMatrix(mturkfile,goldfile,true);
		executeCrowdAgreement(mturkfile, goldfile, k_dir);
	}

	public static void executeCrowdAgreement(String mturkfile, String goldfile, String k_dir) throws XMLStreamException, IOException{
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
		Map<Double,List<Annotation>> agreement_annos = agg.getAnnotationsAtAllAgreement(mturk_annos);
		//System.out.println(agreement_annos.size());
		for(Double exp : agreement_annos.keySet()){
			List<Annotation> annos = agreement_annos.get(exp);
			if(goldfile!=null){
				//execute comparison versus gold, report results
				ComparisonReport report = ac.compareAnnosCorpusLevel(gold_annos, annos, "K="+exp);
				System.out.println("Agreement="+exp+"\t"+report.getRow());
			}
			//export a BioC version for banner
			//BioCCollection k_collection = convertAnnotationsToBioC(annos, id_doc,"mturk k="+exp,"",1000000);
			//writeBioC(k_collection, k_dir+"_"+exp+".xml");
		}
	}
	
	//Creates matrix of turker-turker agreements, has option to write out matrix to file
	
	public static double[][] makeAgreementMatrix(String mturkfile, String goldfile, Boolean print) throws XMLStreamException, IOException{
		
		AnnotationComparison ac = new AnnotationComparison();
		BioCCollection mturk_collection = TestAggregation.readBioC(mturkfile);
		
		List<Annotation> turkAnnos = TestAggregation.convertBioCtoAnnotationList(mturk_collection);
		BioCCollection gold_collection = null;
		List<Annotation> goldAnnos = null;
		if(goldfile!=null){
			//load gold standard annotations
			gold_collection = TestAggregation.readBioC(goldfile);
			goldAnnos = TestAggregation.convertBioCtoAnnotationList(gold_collection);
			//filter out annotations from the gold set for docs with no mturk annotations
			boolean common_docs_only = true;
			int n_gold_annos_removed = 0;
			if(common_docs_only){
				List<Annotation> keep_annos = new ArrayList<Annotation>();
				Map<Integer, Set<Annotation>> testdoc_annos = ac.listtomap(turkAnnos);
				Set<Integer> test_ids = testdoc_annos.keySet();
				for(Annotation ganno : goldAnnos){
					if(test_ids.contains(ganno.getDocument_id())){
						keep_annos.add(ganno);
					}
				}
				n_gold_annos_removed = goldAnnos.size()-keep_annos.size();
				System.out.println("n_gold_annos_removed "+n_gold_annos_removed);
				goldAnnos = keep_annos;
			}
		}
		
		Aggregator agg = new Aggregator();
		List<Annotator> tatorList = agg.getAnnotatorList(turkAnnos);
		Map<Integer, Double> tatorTrust = new HashMap<Integer, Double>();
		Map<Integer, Integer> tatorSim = new HashMap<Integer, Integer>(); //count number of other tators can be compared to (gets rid of disjoint doc sets)
		Double trust1, trust2, agreement;
		Integer sim1, sim2, max_sim=0, dimSize;
		agreementOutput tatorAgreement;
		
		
		//Add gold standard data as final "gold" annotator
		if(goldAnnos != null){
			tatorList.add(goldAnnotator(goldAnnos));
		}
		
		dimSize = tatorList.size();

		Integer[][] tatorSimAm = new Integer[dimSize][dimSize]; //Matrix of the number of similar documents between turkers
		double[][] tatorAgreeScore = new double[dimSize][dimSize];
		String header = "";
		for(int i = 0; i<dimSize; i++){
			Annotator tator1 = tatorList.get(i);
			if(i < (dimSize-1)){
				header += tator1.getId()+",";
			}
			for(int j = i+1; j < tatorList.size();j++){
				Annotator tator2 = tatorList.get(j);
				tatorAgreement = agg.findTatorAgreement(tator1,tator2);
				agreement = tatorAgreement.agreement;
				Integer simDocs = tatorAgreement.simDoc;
				if(simDocs > max_sim){max_sim=simDocs;}

				
				tatorAgreeScore[i][j] = tatorAgreement.agreement; //Currently not using scaling by similar documents - tatorSimAm
				tatorAgreeScore[j][i] = tatorAgreement.agreement;
				
				tatorSimAm[i][j] = tatorAgreement.simDoc;
				tatorSimAm[j][i] = tatorAgreement.simDoc;
			}
		}
		header += "GoldStandard";
		if(print){
			File file = new File ("output/agreement_matrix.csv");
			PrintWriter writer = new PrintWriter(file);
			writer.println(header);
			for(int i = 0; i < dimSize; i++){
				String line = "";
				for(int j = 0; j < (dimSize-1); j++){
					line += tatorAgreeScore[i][j] + ",";
				}
				line += tatorAgreeScore[i][dimSize-1];
				writer.println(line);
			}
			writer.close();
			
		}
		return tatorAgreeScore;
	}
	
	public static Annotator goldAnnotator(List<Annotation> goldAnnos){
		Annotator gold = new Annotator(0);
		gold.addAnno(goldAnnos);
		
		return gold;
	}
}