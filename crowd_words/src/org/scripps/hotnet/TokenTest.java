package org.scripps.hotnet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
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



public class TokenTest{
	
	public static void main(String[] args) throws XMLStreamException, IOException{
		
		String mturkfile = "data/mturk/ncbitrain_e11_bioc.xml";
		//"data/mturk/newpubmed_e12_13_bioc.xml"; 
		String goldfile = "data/ncbi/ncbi_train_bioc.xml";
		String k_dir = "data/mturk/ncbitrain_e11_voting/";
		/*File file = new File("output/docs/doc_level.tsv");
		PrintWriter writer = new PrintWriter(file);
		writer.println("DocID \t Rate \t Time \t Threshold \t TP \t FP \t FN \t Precision \t Recall \t FScore \t Accuracy");*/
		PrintWriter writer = null;
		for(double rate = 0.1; rate < .15; rate+=0.05){
			Double junkRate = rate*100;
			Integer fileRate = junkRate.intValue();
			//File file = new File ("output/tokenTests/"+fileRate+".tsv");
			//writer = new PrintWriter(file);
			//writer.println("rate \t time \t threshold \t tp \t fp \t fn \t precision \t recall \t fscore \t accuracy");
			//writer.println("docID \t rate \t time \t threshold \t hot_tp \t hot_fp \t hot_fn \t hot_precision \t hot_recall \t hot_fscore \t hot_accuracy \t k_tp \t k_fp \t k_fn \t k_precision \t k_recall \t k_fscore \t f_accuracy");
			for(double threshold = 7; threshold <= 8; threshold+=1){
				for(int time = 0; time <= 1; time++){
					//File file = new File("output/DocDives/10802668/" + fileRate+".tsv");
					File file = new File("output/tokenTests/"+fileRate + "t" + threshold + ".tsv");
					writer = new PrintWriter(file);
					writer.println("Doc \t Text \t Start \t End");
					//System.out.println(rate + "\t" + time + "\t" + threshold);
					executeHotNet(rate,time,threshold,mturkfile,goldfile, writer);
					writer.close();
				}
			}
			//writer.close();
		}
		//okay, first test tokenization
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
		
		//List<Annotation> gold_annos = new ArrayList<Annotation>();
		List<token> gold_tokens = convertBioCtoTokenList(gold_collection);
		
		//List<Annotation> mturk_annos = convertBioCtoAnnotationList(mturk_collection);

		
		TokenComparison ac = new TokenComparison();
		
		//By Document comparison
		/*for(int i=0; i < new_docList.size(); i++){
			Document doc1 = new_docList.get(i);
			Document gold1 = new_goldList.get(i);
			hotnet hot = new hotnet(doc1);
			hot.set_rate(rate);
			hot.step(time);
			List<Annotation> annoList = hot.convertToAnno(threshold);
			List<Annotation> goldList = gold1.getAnnotations();
			
			hot_annos.addAll(annoList);
			gold_annos.addAll(goldList);
			
			ComparisonReport rep = ac.compareAnnosCorpusLevel(goldList, annoList, "Treshold = 2.0");
			System.out.println(doc1.getId()+"\t"+ gold1.getId()+ "\t" + rep.getRow());
		}*/

		//for(Document doc: new_goldList){
		//writer = new PrintWriter("output/user_corrected/"+"gold10364525.tsv");
		
		List<token> goldTokes = new ArrayList<token>();
		
		List<token> turkTokes = new ArrayList<token>();
		
		Aggregator agg = new Aggregator();

		List<token> origTokes = new ArrayList<token>();
		
		
		//Map<Integer, List<Annotation>> k_map = agg.getAnnotationMapByK(mturk_annos, "loc");

		for(int i = 0; i < new_docList.size(); i++){
			Document doc =  new_docList.get(i); //extra
			//if(doc.getId() != 7523157){continue;}
			Document golden = new_goldList.get(i);
			hotnet hot = new hotnet(doc);
			hot.set_rate(rate);
			
			hotnet gold = new hotnet(golden);
			//for(int j = 0; j <= time; j++){
				/*for(token toke: hot.getTokens()){
					Integer id = toke.getStart();
					writer.println(j + "\t" + toke.getStart() + "\t" + toke.getEnd()+"\t"+toke.getText()+"\t"+hot.count_state.get(id) + "\t" + gold.count_state.get(id));
				}*/
			for(token toke: hot.getTokens()){
				Integer id = toke.getStart();
				if(hot.count_state.get(id) >= threshold){
					origTokes.add(toke);
				}
			}
			hot.step(time);
			for(token toke: hot.getTokens()){
				Integer id = toke.getStart();
				if(hot.count_state.get(id) >= threshold){
					turkTokes.add(toke);
				}
			}
			for(token toke: gold.getTokens()){
				Integer id = toke.getStart();
				if(gold.count_state.get(id) > 0){
					goldTokes.add(toke);
				}
			}
				
				//ComparisonReport rep = ac.compareAnnosCorpusLevel(annoList, golden.getAnnotations(), "garbage");
				//ComparisonReport correctedRep = ac.compareAnnosCorpusLevel(userAnnoList, golden.getAnnotations(), "garbage");
				//System.out.println("Rate = " + rate + "\t Time = " + time + "\t Treshold = " + threshold + "\t" + rep.getRow());
				//System.out.println("Rate = " + rate + "\t Time = " + time + "\t Treshold = " + threshold + "\t" + correctedRep.getRow());
				/*for(Annotation anno: annoList){
					System.out.println(anno.getStart() + "\t" + anno.getStop() + "\t" + anno.getText());
				}
				System.out.println();
				System.out.println("Corrected");
				for(Annotation anno: userAnnoList){
					System.out.println(anno.getStart() + "\t" + anno.getStop() + "\t" + anno.getText());
				}
				System.out.println();
				System.out.println("Golden");
				for(Annotation anno: golden.getAnnotations()){
					System.out.println(anno.getStart() + "\t" + anno.getStop() + "\t" + anno.getText());
				}*/

				//writer = new PrintWriter("output/docs/"+doc.getId()+".tsv");
				//writer.println(doc.getId()+"\t" + rate + "\t" + time + "\t" + threshold + "\t"+rep.getRow());
			/*for(Annotation anno: golden.getAnnotations()){
				writer.println(anno.getStart()+"\t"+anno.getStop()+"\t"+anno.getText());
			}*/
				//List<Annotation> annoList = hot.convertToAnno(threshold);
				//List<Annotation> userAnnoList = hot.convertToSimilarAnno(annoList);
				/*List<Annotation> annoList = hot.convertToAnnosByAverage(threshold);

				hot_annos.addAll(annoList);
				
				goldTester.addAll(golden.getAnnotations());
				
				ComparisonReport rep = ac.compareAnnosCorpusLevel(golden.getAnnotations(), annoList, "k");
				
				List<Annotation> k_annos = agg.getAnnotationsAtK(doc.getAnnotations(), threshold.intValue(), "loc");
				
				if(k_annos != null){
					ComparisonReport k_rep = ac.compareAnnosCorpusLevel(golden.getAnnotations(), k_annos, "k");
					writer.println(doc.getId() + "\t" + rate + "\t" + time + "\t" + threshold + "\t" + rep.getRow() + "\t" + k_rep.getRow());
				}
				else{
					writer.println(doc.getId() + "\t" + rate + "\t" + time + "\t" + threshold + "\t" + rep.getRow() + "\t" + "NA \t NA \t NA \t NA \t NA \t NA \t NA");
				}*/
				

			//}
		}
		ComparisonReport rep = ac.compareAnnosCorpusLevel(goldTokes, turkTokes, "k");
		
		Set<token> origSet = new HashSet<token>(origTokes);
		Set<token> turkSet = new HashSet<token>(turkTokes);
		
		turkSet.removeAll(origSet);
		for(token toke: turkSet){
			writer.println(toke.getDocID() + "\t" + toke.getText() + "\t" + toke.getStart() + "\t" + toke.getEnd());
		}
		//writer.println(rate + "\t" + time + "\t" + threshold + "\t" + rep.getRow());
		//System.out.println(rate + "\t" + time + "\t" + threshold + "\t" + rep.getRow());
		//writer.close();
		
		/*Set<Annotation> hot_set = new HashSet<Annotation>(hot_annos);
		
		for(Integer k: k_map.keySet()){
			List<Annotation> k_annos = k_map.get(k);
			Set<Annotation> k_set = new HashSet<Annotation>(k_annos);
		
			k_set.retainAll(hot_set);
		
			List<Annotation> hot_list = new ArrayList<Annotation>(k_set);
		
			ComparisonReport rep = ac.compareAnnosCorpusLevel(goldTester, hot_list, "k");
			if(writer == null){
				System.out.println(rate + "\t" + time + "\t" + threshold + "\t" + k + " \t" + rep.getRow());
			}
			else{
				writer.println(rate + "\t" + time + "\t" + threshold + "\t" + k + "\t" + rep.getRow());
			}
		}*/
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
	public static List<token> convertBioCtoTokenList(BioCCollection biocCollection){
		List<token> tokens = new ArrayList<token>();
		SimpleTokenizer tokenizer = new SimpleTokenizer();
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
					List<token> tokes = tokenizer.getTokens(bca.getText(), offset, pmid);
					tokens.addAll(tokes);
				}
			}
		}
		return tokens;
	}
	
}