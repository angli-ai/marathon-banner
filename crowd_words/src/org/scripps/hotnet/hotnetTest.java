	package org.scripps.hotnet;
	
	import java.io.File;
	import java.io.IOException;
	import java.io.PrintWriter;
	import java.util.ArrayList;
	import java.util.HashMap;
	import java.util.HashSet;
	import java.util.List;
	import java.util.Map;
	import java.util.Set;
	
	import javax.xml.stream.XMLStreamException;
	
	import org.scripps.crowdwords.Annotation;
	import org.scripps.crowdwords.AnnotationComparison;
	import org.scripps.crowdwords.ComparisonReport;
	import org.scripps.crowdwords.Document;
	import org.scripps.crowdwords.TestAggregation;
	import org.scripps.crowdwords.Aggregator;
	
	import banner.types.Sentence;
	import banner.types.Token;
	import bioc.BioCAnnotation;
	import bioc.BioCCollection;
	import bioc.BioCDocument;
	import bioc.BioCPassage;
	
	
	
	public class hotnetTest{
		
		public static void main(String[] args) throws XMLStreamException, IOException{
			
			String mturkfile = "data/mturk/ncbitrain_e11_bioc.xml";
	
			String goldfile = "data/ncbi/ncbi_train_bioc.xml";
			String k_dir = "data/mturk/ncbitrain_e11_voting/";
	
			PrintWriter writer = null; //Placeholder for writer. This can be changed up to print out whatever info you want from the hotnet testing.
			
			for(double rate = 0.0; rate < .25; rate+=0.05){
				//Double junkRate = rate*100;
				//Integer fileRate = junkRate.intValue();
				for(Double threshold = 0.0; threshold <= 10; threshold+=1){
					for(int time = 0; time <= 6; time++){
						
						System.out.println(rate + "\t" + time + "\t" + threshold);
						executeHotNet(rate,time,threshold,mturkfile,goldfile, writer);
					}
				}
			}
		}
		
		public static void executeHotNet(Double rate, Integer time, Double threshold, String mturkfile, String goldfile,PrintWriter writer) throws XMLStreamException, IOException{
			
			BioCCollection mturk_collection = TestAggregation.readBioC(mturkfile);
			
			List<Document> doc_list = convertBioCCollectionToDocMap(mturk_collection);
			
			//List<Annotation> mturk_annos = TestAggregation.convertBioCtoAnnotationList(mturk_collection);
			
			BioCCollection gold_collection = TestAggregation.readBioC(goldfile);
			
			List<Document> gold_list = convertBioCCollectionToDocMap(gold_collection);
			
			Set<Document> gold_set = new HashSet<Document>(gold_list);
			Set<Document> doc_set = new HashSet<Document>(doc_list);
			
			//I know, this is gross. Bear with it 
			
			doc_set.retainAll(gold_set);
			List<Document> new_docList = new ArrayList<Document>(doc_set);
			List<Document> new_goldList = new ArrayList<Document>(gold_set);
			
			List<Annotation> hot_annos = new ArrayList<Annotation>();
			
			List<Annotation> gold_annos = convertBioCtoAnnotationList(gold_collection);
			
			List<Annotation> mturk_annos = convertBioCtoAnnotationList(mturk_collection);
	
			
			AnnotationComparison ac = new AnnotationComparison();
			
			
			List<Annotation> goldTester = new ArrayList<Annotation>();
			
			Aggregator agg = new Aggregator();
	
			SimpleTokenizer izer = new SimpleTokenizer();
	
			
	
			for(int i = 0; i < new_docList.size(); i++){
				Document doc =  new_docList.get(i); //extra
				Document golden = new_goldList.get(i);
				hotnet hot = new hotnet(doc);
				hot.set_rate(rate);
				
				hotnet gold = new hotnet(golden);
				
	
					
					//General use time step
					hot.step(time);
					List<Annotation> annoList = hot.convertToAnnosByAverage(threshold);
					
					hot_annos.addAll(annoList);
			}
			
			
			
			
				ComparisonReport rep = ac.compareAnnosCorpusLevel(goldTester, hot_annos, "");
				if(writer == null){
					System.out.println(rate + "\t" + time + "\t" + threshold +" \t" + rep.getRow());
				}
				else{
					writer.println(rate + "\t" + time + "\t" + threshold + "\t"+ rep.getRow());
				
			}
		}
		
		public static List<Document> convertBioCCollectionToDocMap(BioCCollection biocCollection){
			
			List<Document> docList = new ArrayList<Document>();
				
			for(BioCDocument biodoc : biocCollection.getDocuments()){
				Integer pmid = Integer.parseInt(biodoc.getID());
				Document doc = new Document();
				doc.setId(pmid);
				//			String n_annotators = doc.getInfon("n_annotators");
				//			String annotator_ids = doc.getInfon("annotators");
				for(BioCPassage passage : biodoc.getPassages()){
					String type = passage.getInfon("type");
					if(type.equals("title")){
						type = "t";
						doc.setTitle(passage.getText());
					}else if(type.equals("abstract")){
						type = "a";
						doc.setText(passage.getText());
					}
					for(BioCAnnotation bca : passage.getAnnotations()){
						//assumes that we have one location per annotation
						//will work until we get to relations.
						int offset = bca.getLocations().get(0).getOffset();
						if(type=="a"){offset+=1;}
						Annotation anno = new Annotation(bca.getText(), offset, offset+bca.getText().length(), pmid, type, "loc");
						String annotatorId = bca.getInfon("annotator_id");
						anno.setUser_id(Integer.parseInt(annotatorId));
						anno.setId(Integer.parseInt(bca.getID()));
						doc.addAnnotations(anno);
					}
				}
				docList.add(doc);
			}
			return docList;
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
						if(type=="a"){offset+=1;}
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
		
	}