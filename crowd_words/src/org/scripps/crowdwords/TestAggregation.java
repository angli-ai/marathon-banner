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
 * @author bgood
 *
 */
public class TestAggregation {

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
		executeVotingExperiment(mturkfile, goldfile, k_dir);
	}

	public static void executeVotingExperiment(String mturkfile, String goldfile, String k_dir) throws XMLStreamException, IOException{
		if(goldfile==null){
			System.out.println("No gold standard, just exporting voting results.");
		}else{
			System.out.println("Exporting voting results and evaluating each against gold standard");
		}
		AnnotationComparison ac = new AnnotationComparison();

		//read in the mturk annotations
		BioCCollection mturk_collection = readBioC(mturkfile);
		//get the full text for export later
		Map<String, Document> id_doc = convertBioCCollectionToDocMap(mturk_collection);
		//convert to local annotation representation
		List<Annotation> mturk_annos = convertBioCtoAnnotationList(mturk_collection);

		BioCCollection gold_collection = null;
		List<Annotation> gold_annos = null;
		if(goldfile!=null){
			//load gold standard annotations
			gold_collection = readBioC(goldfile);
			gold_annos = convertBioCtoAnnotationList(gold_collection);
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
		Map<Integer, List<Annotation>> k_annos = agg.getAnnotationMapByK(mturk_annos, "loc");
		for(Integer k=1; k < 16; k++){// : k_annos.keySet()){
			List<Annotation> annos = k_annos.get(k);
			if(goldfile!=null){
				//execute comparison versus gold, report results
				ComparisonReport report = ac.compareAnnosCorpusLevel(gold_annos, annos, "K="+k);
				System.out.println("K="+k+"\t"+report.getRow());
			}
			//export a BioC version for banner
			BioCCollection k_collection = convertAnnotationsToBioC(annos, id_doc,"mturk k="+k,"",1000000);
			//writeBioC(k_collection, k_dir+"_"+k+".xml");
		}
	}
	
	public static BioCCollection readBioC(String file) throws FileNotFoundException, XMLStreamException{
		BioCFactory factory = BioCFactory.newFactory(BioCFactory.WOODSTOX);
		BioCCollectionReader reader =
				factory.createBioCCollectionReader(new FileReader(file));    
		BioCCollection collection = reader.readCollection();
		return collection;
	}

	public static void writeBioC(BioCCollection collection, String file) throws XMLStreamException, IOException{
		OutputStream out = new FileOutputStream(file);
		BioCFactory factory = BioCFactory.newFactory(BioCFactory.STANDARD);
		BioCDocumentWriter writer =
				factory.createBioCDocumentWriter(new OutputStreamWriter(out));
		writer.writeCollectionInfo(collection);
		for ( BioCDocument document : collection ) {
			writer.writeDocument(document);
		}
		writer.close();
	}

	public static List<Annotation> convertBioCtoAnnotationList(BioCCollection biocCollection){
		List<Annotation> annos = new ArrayList<Annotation>();
		for(BioCDocument doc : biocCollection.getDocuments()){
			Integer pmid = Integer.parseInt(doc.getID());
			//			String n_annotators = doc.getInfon("n_annotators");
			//			String annotator_ids = doc.getInfon("annotators");
			for(BioCPassage passage : doc.getPassages()){
				String type = passage.getInfon("type");
				if(type.equals("title")){
					type = "t";
				}else if(type.equals("abstract")){
					type = "a";
				}
				for(BioCAnnotation bca : passage.getAnnotations()){
					//assumes that we have one location per annotation
					//will work until we get to relations.
					int offset = bca.getLocations().get(0).getOffset();
					Annotation anno = new Annotation(bca.getText(), offset, offset+bca.getText().length(), pmid, type, "loc");
					String annotatorId = bca.getInfon("annotator_id");
					anno.setUser_id(Integer.parseInt(annotatorId));
					anno.setId(Integer.parseInt(bca.getID()));
					annos.add(anno);
				}
			}
		}
		return annos;
	}

	public static BioCCollection convertAnnotationsToBioC(List<Annotation> annos, Map<String, Document> docid_doc, String source, String date, int limit) throws IOException{
		//create the BioC collection
		BioCCollection biocCollection = new BioCCollection();
		biocCollection.setSource(source);
		biocCollection.setDate(date);

		SentenceSplitter splitter = new SentenceSplitter();
		//this turns the list of annotations into a map keyed by pmid
		AnnotationComparison annocompare = new AnnotationComparison();
		Map<Integer, List<Annotation>> docid_annotations = annocompare.listtomaplist(annos);
		int fine = 0; int ndocs = 0;
		for(Integer docid : docid_annotations.keySet()){
			ndocs++;
			Document doc = docid_doc.get(docid+"");
			//create bioc doc
			BioCDocument biocDoc = new BioCDocument();
			biocDoc.setID(docid+"");
			//make two passages per doc - one for the title and one for the abstract
			BioCPassage title_passage = new BioCPassage();
			title_passage.setOffset(0);
			title_passage.setText(doc.getTitle()+" "); //TODO will need to make some checks and adjustments to make sure that the pffsets of the annotations are exactly right
			title_passage.putInfon("type", "title");
			BioCPassage abstract_passage = new BioCPassage();
			abstract_passage.setOffset(doc.getTitle().length()+1);
			abstract_passage.setText(doc.getText());
			abstract_passage.putInfon("type", "abstract");
			//get the annotations for this document
			List<Annotation> dannos = docid_annotations.get(docid);
			//put them in order
			Collections.sort(dannos);
			//create the BioC versions			
			Set<String> workers = new HashSet<String>();
			for(Annotation anno : dannos){		
				List<String> annospecific_workers = null;
				if(anno.getUser_id()==0){
					annospecific_workers = anno.getAnnotators();
					if(annospecific_workers!=null){
						for(String w : annospecific_workers){
							workers.add(w);
						}
					}
				}else{
					workers.add(anno.getUser_id()+"");
				}
				int stop = anno.getStop(); int start = anno.getStart();
				String kind = "a";

				if(anno.getDocument_section().equals("t")){
					kind = "t";
				}else if(anno.getDocument_section().equals("a")){
					start++;
				}				
				//check if valid
				String tmp = title_passage.getText();
				//uncomment to make the offsets relative to the passage, not the document
				//if(kind.equals("a")){
				//	tmp = abstract_passage.getText();
				//start = start - title_passage.getText().length();
				//}
				//comment this if you want the offsets relative to the passage
				tmp = tmp+abstract_passage.getText();

				//set the annotation start and stops
				stop = start+anno.getText().length();

				//don't export if not valid
				if(start>-1&&!anno.getText().equals(tmp.substring(start, stop))){
					System.out.println("Boundary error, skipping: "+anno.getText()+"\t"+tmp.substring(start, stop));
				}else if(start==stop){
					System.out.println("Empty annotation, skipping");
				}else{//all ok
					fine++;
					//create the bioc annotation
					BioCAnnotation biocAnno = new BioCAnnotation();
					biocAnno.setID(anno.getId()+"");
					biocAnno.putInfon("type", "Disease");
					BioCLocation loc = new BioCLocation();
					loc.setLength(stop-start);
					loc.setOffset(start);					
					biocAnno.addLocation(loc);
					biocAnno.setText(anno.getText()); 
					//if multiple annotator..
					if(anno.getUser_id()==0){
						if(annospecific_workers!=null){
							biocAnno.putInfon("annotator_ids", annospecific_workers.toString());
							biocAnno.putInfon("n_annotators", annospecific_workers.size()+"");
						}
					}else{
						biocAnno.putInfon("annotator_id",anno.getUser_id()+"");
					}
					//add the annotation to the passage
					if(kind.equals("t")){
						title_passage.addAnnotation(biocAnno);
					}else{	
						abstract_passage.addAnnotation(biocAnno);
					}
				}

			}
			biocDoc.putInfon("n_annotators",workers.size()+"");
			biocDoc.putInfon("annotator_ids", workers.toString());

			//add these passages to the doc
			biocDoc.addPassage(title_passage);
			biocDoc.addPassage(abstract_passage);
			//add the doc to the collection
			biocCollection.addDocument(biocDoc);
			if(ndocs>limit){
				break;
			}
		}
		return biocCollection;
	}

	public static Map<String, Document> convertBioCCollectionToDocMap(BioCCollection biocCollection){
		Map<String, Document> id_doc = new HashMap<String, Document>();
		for(BioCDocument biodoc : biocCollection.getDocuments()){
			Integer pmid = Integer.parseInt(biodoc.getID());
			Document doc = new Document();
			doc.setId(pmid);
			for(BioCPassage passage : biodoc.getPassages()){
				String type = passage.getInfon("type");
				if(type.equals("abstract")){
					doc.setText(passage.getText());
				}else if(type.equals("title")){
					doc.setTitle(passage.getText());
				}
			}
			id_doc.put(pmid+"", doc);
		}	
		return id_doc;
	}

}
