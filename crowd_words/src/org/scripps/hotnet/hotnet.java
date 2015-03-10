package org.scripps.hotnet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

import org.scripps.crowdwords.Annotation;
import org.scripps.crowdwords.Document;
//import org.scripps.crowdwords.TestAggregation;

import banner.types.Sentence;
import bioc.BioCAnnotation;
import bioc.BioCCollection;
import bioc.BioCDocument;
import bioc.BioCPassage;

/**
 * 
 * 
 * @author jbrugg
 */

//Hot net class

public class hotnet{
	
	private List<Annotation> annotations;
	private List<Annotation> unique_annos;
	private List<token> tokens;
	private Double diffusion_rate;
	protected Map<Integer, Double>  count_state;
	private Integer doc_id;

	
	
	public hotnet(){
		annotations = new ArrayList<Annotation>();
		tokens = new ArrayList<token>();
		count_state = new HashMap<Integer, Double>();
	}
	
	public hotnet(Document doc){
		//this method will construct the hot net from a list of annotations from a single doc
		doc_id = doc.getId();
		tokenizeDoc(doc);

		create_start_state(doc);
		
	}
		
	public void set_rate(Double rate){
		diffusion_rate = rate;
	}
	

	
	public void tokenizeDoc(Document doc){
		String text = doc.getText();
		String title = doc.getTitle();
		SimpleTokenizer tokenizer = new SimpleTokenizer();
		
		tokens = tokenizer.getTokens(title+" "+text, doc.getId());

	}
	
	public void create_start_state(Document doc){
		unique_annos = new ArrayList<Annotation>();
		for(Annotation anno: doc.getAnnotations()){
			Annotation simple = new Annotation(anno.getText(), anno.getStart(), anno.getStop(), anno.getDocument_id(), anno.getDocument_section(), "loc");
			if(!unique_annos.contains(simple)){
				unique_annos.add(simple);
			}
		}

		count_state = new HashMap<Integer, Double>();
		SimpleTokenizer izer = new SimpleTokenizer();
		
		for(token toke: tokens){
			Integer start = toke.getStart();
			Double cnt = count_state.get(start);
			if(cnt == null){cnt = 0.0;}
			count_state.put(start, cnt);
		}
		annotations = doc.getAnnotations();
		for(Annotation anno: doc.getAnnotations()){
			Integer start = anno.getStart();
			List<token> tokes = izer.getTokens(anno.getText(), start, anno.getDocument_id());
			/*for(token toke: tokes){
				System.out.println(toke.getText()+ " " + toke.getStart()+ " " + toke.getEnd());
			}*/
			for(token toke: tokes){
				if(tokens.contains(toke)){
					//System.out.println(toke.getText());
					//System.out.println(toke.getStart());
					Double cnt = count_state.get(toke.getStart());
					cnt += 1;
					count_state.put(toke.getStart(), cnt);
				}
			}
		}

		//System.out.println(count_state);
	}
	
	public void step(Integer iters){
		double[] change = new double[tokens.size()];
		
		List<Integer> token_locations = new ArrayList( new TreeSet<Integer>(count_state.keySet()));
		
		HashMap<Integer, Double> copy = new HashMap<Integer, Double>(count_state);
		for(int i = 0; i < iters; i++){
			Integer prev_loc = 0;
			Integer loc_to_update = 0;
			for(int j = 0; j <  token_locations.size(); j++){
				Integer loc = token_locations.get(j);
				Double heat = count_state.get(loc);
				
				//change[j] += heat*diffusion_rate;
				if(j != 0){
					Double prev_heat = count_state.get(token_locations.get(j-1));
					if(prev_heat > 0){  //if statement creates hard border at 0's, no heat bleeds over
						change[j-1] -= heat*diffusion_rate/2;
						change[j] += heat*diffusion_rate/2;
					}
				}
				if(j != (token_locations.size()-1)){
					Double next_heat = count_state.get(token_locations.get(j+1));
					if(next_heat > 0){  //if statement creates hard border at 0's, no heat bleeds over
						change[j+1] -= heat*diffusion_rate/2;
						change[j] += heat*diffusion_rate/2;
					}
				}
				if(j >=2){
					count_state.put(loc_to_update, count_state.get(loc_to_update)-change[j-2]);
				}

				loc_to_update = prev_loc;
				prev_loc = loc;
			}
			Integer j = change.length;
			count_state.put(loc_to_update, count_state.get(loc_to_update)-change[j-2]);
			count_state.put(prev_loc, count_state.get(prev_loc) - change[j-1]);
		}
		
		//next bit for checking that this worked correctly
		
		/*for(token toke: tokens){
			Integer start = toke.getStart();
			System.out.println(toke.getText() + "\t" + toke.getStart()+ "\t" + count_state.get(start)+ "\t"+copy.get(start));
		}*/
	}
	
	//After creating the annotations from the document, this method will instead replace each constructed annotation with the user-marked annotation that is closest to it.
	
	public List<Annotation> convertToSimilarAnno(List<Annotation> annoList){
		List<Annotation> bestAnnos = new ArrayList<Annotation>();
		Annotation bestAnno = null;
		for(Annotation hotnetAnno: annoList){
			Integer min_sep = 1000000000; //does Java have an infinity? probably would look better
			for(Annotation userAnno: unique_annos){
				//System.out.println(userAnno.getStart() + "\t" + userAnno.getStop() + "\t" + userAnno.getText());
				Integer separation = Math.abs(userAnno.getStart() - hotnetAnno.getStart()) + Math.abs(userAnno.getStop() - hotnetAnno.getStop()); //1-Norm separation, could try 2?
				//System.out.println(hotnetAnno.getStart() + "\t" + hotnetAnno.getStop() + "\t" + hotnetAnno.getText());
				if(min_sep > separation){
					bestAnno = userAnno;
					min_sep = separation;
				}
			}
			bestAnnos.add(bestAnno);
		}
		return bestAnnos;
	}
	
	public List<Annotation> convertToAnno(Double threshold){
		SortedSet<Integer> token_locations = new TreeSet<Integer>(count_state.keySet());
		
		List<Annotation> annoList = new ArrayList<Annotation>();
		String annoText = "";
		Integer start = null, stop = null;
		Integer j = 0;
		for(Integer loc: token_locations){
			String tokeText = tokens.get(j).getText();
			if(count_state.get(loc) >= threshold && !tokeText.equals(".") ){//&& !tokeText.equals(",")){
				annoText += tokeText;
				if(start == null){start = loc; stop=loc+annoText.length();}
				else{stop += (1+tokeText.length());}
			}
			else if(annoText.length() >= 1){
				Annotation simple = new Annotation(annoText, start, stop,doc_id, "a", "loc");
				annoList.add(simple);
				start = null;
				annoText = "";
			}	
			j++;
		}
		//final add
		if(annoText.length() >=1){
			//stop = start + annoText.length();
			Annotation simple = new Annotation(annoText, start, stop,doc_id, "a", "loc");
			annoList.add(simple);
		}
		
		return annoList;
	}
	
	//Finds average token heat for each user-marked annotation, chooses Annotations that have an average heat above a threshold
	
	public List<Annotation> convertToAnnosByAverage(Double threshold){
		List<Annotation> annoList = new ArrayList<Annotation>();
		
		SimpleTokenizer izer = new SimpleTokenizer();
		
		for(Annotation anno: unique_annos){
			Integer start = anno.getStart();
			List<token> tokes = izer.getTokens(anno.getText(),  start,anno.getDocument_id());
			
			Double score = 0.0;
			//System.out.println(anno.getText());
			for(token toke: tokes){
				//System.out.println(anno.getText() + "\t" + count_state.get(toke.getStart()) + "\t" + score);
				score += count_state.get(toke.getStart());
			}
			score = score / tokes.size();
			if(score >= threshold){
				annoList.add(anno);
			}
		}
		
		return annoList;
	}
	
	public List<Annotation> getUserAnnosUnique(){
		return this.unique_annos;
	}
	
	public List<token> getTokens(){
		return this.tokens;
	}
}