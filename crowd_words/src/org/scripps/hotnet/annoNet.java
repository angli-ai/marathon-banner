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
 * @author jbrugg
 * 
 */

//Annotation net class. This finds the 'similarity' between each annotation, in this case being how much character overlap two annotations have. The 'heat', or number of votes, between each pair of annotations
// is then exchanged based off some function of their similarity. Some deprecated objects / functions in this class, they shouldn't interfere with its function.

public class annoNet{
	
	private List<Annotation> annotations;
	private List<Annotation> unique_annos;
	private List<token> tokens;
	private Double diffusion_rate;
	protected Map<Integer, Double>  count_state;
	private Integer doc_id;
	private Double[][] anno_relation;
	private Map<Annotation, Double> anno_counts;

	
	
	public annoNet(){
		annotations = new ArrayList<Annotation>();
		tokens = new ArrayList<token>();
		//count_state = new HashMap<Integer, Double>();
	}
	
	public annoNet(Document doc){
		//this method will construct the hot net from a list of annotations from a single doc
		doc_id = doc.getId();
		tokenizeDoc(doc);
		create_anno_net(doc);
		simple_anno_net_vote(); 		
	}
		
	public void set_rate(Double rate){
		diffusion_rate = rate;
	}
	
	//instead of coverage, uses amount that annotation is contained in annotations it's receiving votes from
	
	public void simple_anno_net_vote_encapsulation(){
		double[] change = new double[unique_annos.size()];
		for(int i = 0; i < unique_annos.size(); i++){
			Annotation anno1 = unique_annos.get(i);
			Double cnt1 = anno_counts.get(anno1);
			for(int j = i+1; j < unique_annos.size(); j++){
				Annotation anno2 = unique_annos.get(j);
				Double cnt2 = anno_counts.get(anno2);
				change[i] += anno_relation[j][i] * cnt2;
				change[j] += anno_relation[i][j] * cnt1;
			}
		}
		for(int i = 0; i < unique_annos.size(); i++){
			Annotation anno1 = unique_annos.get(i);
			anno_counts.put(anno1, anno_counts.get(anno1)+change[i]);
		}
	}
	
	
	//Currently, simply accumulates heat from other sources depending on # of votes from those sources and fraction depending on coverage (see create_anno_net), think of it as a single "step"
	public void simple_anno_net_vote(){
		double[] change = new double[unique_annos.size()];
		for(int i = 0; i < unique_annos.size(); i++){
			Annotation anno1 = unique_annos.get(i);
			Double cnt1 = anno_counts.get(anno1);
			for(int j = i+1; j < unique_annos.size(); j++){
				Annotation anno2 = unique_annos.get(j);
				Double cnt2 = anno_counts.get(anno2);
				change[i] += anno_relation[i][j] * cnt2;
				change[j] += anno_relation[j][i] * cnt1;
			}
		}
		for(int i = 0; i < unique_annos.size(); i++){
			Annotation anno1 = unique_annos.get(i);
			anno_counts.put(anno1, anno_counts.get(anno1)+change[i]);
		}
	}
	
	// Converts annoNet to List of annotations based off of voting threshold. Does this by evaluating the heat of each annotation in an overlapping cluster and choosing one with most heat from that cluster
	// Heavily favors long annotations.
		
	public List<Annotation> annoNetToAnno(Double threshold){
		List<Annotation> best_annos = new ArrayList<Annotation>();
		List<Annotation> cluster = new ArrayList<Annotation>();
		Integer cluster_start= null, cluster_stop = null;
		for(Annotation anno: unique_annos){
			Integer anno_start = anno.getStart();
			Integer anno_stop = anno.getStop();
			if(cluster_start == null){ //beginning case, beginning cluster
				cluster_start = anno_start;
				cluster_stop = anno_stop;
				cluster.add(anno);
				
			}
			else if((anno_stop >= cluster_start && anno_stop <= cluster_stop) || (anno_start >= cluster_start && anno_start <= cluster_stop)){
				cluster_start = Math.min(anno_start, cluster_start); //
				cluster_stop = Math.max(anno_stop,  cluster_stop);
				cluster.add(anno);
			}
			else{ //new annotation isn't in current cluster, pull out best anno from the cluster and seed the new one
				Double max_heat = -1.0;
				Annotation bestAnno = null;
				for(Annotation clusterAnno: cluster){
					if(anno_counts.get(clusterAnno) > max_heat){
						bestAnno = clusterAnno;
						max_heat = anno_counts.get(clusterAnno);
					}
				}
				if(max_heat > threshold){
					best_annos.add(bestAnno);
				}
				cluster_start = anno_start;
				cluster_stop = anno_stop;
				cluster = new ArrayList<Annotation>();
				cluster.add(anno);
			}
		}
		Double max_heat = -1.0;
		Annotation bestAnno = null;
		for(Annotation clusterAnno: cluster){
			//System.out.println(clusterAnno.getStart() + "\t" + clusterAnno.getStop() + "\t" + anno_counts.get(clusterAnno));
			if(anno_counts.get(clusterAnno) > max_heat){
				bestAnno = clusterAnno;
				max_heat = anno_counts.get(clusterAnno);
			}
		}
		//System.out.println(bestAnno.getStart() + "\t" + bestAnno.getStop() + "\t" + anno_counts.get(bestAnno));
		if(max_heat > threshold){
			best_annos.add(bestAnno);
		}
		return best_annos;
	}
	
	//uses coverage model: Anno A receives fraction that Anno B covers
	
	public void create_anno_net(Document doc){
		unique_annos = new ArrayList<Annotation>();
		anno_counts = new HashMap<Annotation, Double>();
		
		List<Annotation> annoList = doc.getAnnotations();
		
		for(Annotation anno: annoList){
			Annotation simple = new Annotation(anno.getText(), anno.getStart(), anno.getStop(), anno.getDocument_id(), anno.getDocument_section(), "loc");
			if(!unique_annos.contains(simple)){
				unique_annos.add(simple);
			}
			Double cnt = anno_counts.get(simple);
			if(cnt == null){cnt=0.0;}
			cnt+=1;
			anno_counts.put(simple,cnt);
		}
		Integer anno1_start, anno2_start, anno1_stop, anno2_stop;
		anno_relation = new Double[unique_annos.size()][unique_annos.size()]; // ij-entry correspond to how much anno i receives from anno j in heat 
		for(int i = 0; i < unique_annos.size(); i++){
			for(int j = i+1; j < unique_annos.size(); j++){
				Annotation anno1 = unique_annos.get(i);
				Annotation anno2 = unique_annos.get(j);
				
				anno1_start = anno1.getStart(); anno1_stop = anno1.getStop();
				anno2_start = anno2.getStart(); anno2_stop = anno2.getStop();
				
				if((anno1_stop <= anno2_start) || (anno2_stop <= anno1_start)){
					anno_relation[i][j] = 0.0;       // no overlap, both get zero
					anno_relation[j][i] = 0.0;
				}
				else if((anno1_stop <= anno2_stop) && (anno1_start >= anno2_start)){
					anno_relation[i][j] = 1.0; //anno1 completely enveloped in 2, gets full heat
					anno_relation[j][i] = (anno1_stop-anno1_start)*1.0/(anno2_stop-anno2_start); //anno2 gets portion that anno1 covers
				}
				else if((anno2_stop <= anno1_stop) && (anno2_start >= anno1_start)){
					anno_relation[j][i] = 1.0; //anno2 completely enveloped in 1, gets full heat
					anno_relation[i][j] = (anno2_stop-anno2_start)*1.0/(anno1_stop-anno1_start); //anno1 gets portion that anno2 covers
				}
				else if((anno1_stop > anno2_start) && (anno1_stop < anno2_stop)){
					Double overlap = 1.0*(anno1_stop - anno2_start);
					anno_relation[i][j] = overlap/(anno1_stop-anno1_start); //partial overlap, each gets fraction that other covers
					anno_relation[j][i] = overlap/(anno2_stop-anno2_start);
				}
				else{
					Double overlap = 1.0*(anno2_stop - anno1_start);
					anno_relation[i][j] = overlap/(anno1_stop-anno1_start);
					anno_relation[j][i] = overlap/(anno2_stop-anno2_start);
				}
			}
		}//ends whole loop
	}
	
	public void tokenizeDoc(Document doc){
		String text = doc.getText();
		String title = doc.getTitle();
		SimpleTokenizer tokenizer = new SimpleTokenizer();
		
		tokens = tokenizer.getTokens(title+" "+text, doc.getId());

	}
	
	
	public List<Annotation> getUserAnnosUnique(){
		return this.unique_annos;
	}
	
	public List<token> getTokens(){
		return this.tokens;
	}
}