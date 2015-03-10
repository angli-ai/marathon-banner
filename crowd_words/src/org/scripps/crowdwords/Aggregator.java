/**
 * 
 */
package org.scripps.crowdwords;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author bgood
 *
 */
public class Aggregator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	public Map<Integer, List<Annotation>> getAnnotationMapByK(List<Annotation> all_annos, String identity_criteria){
		Map<Annotation, Integer> anno_k = new HashMap<Annotation, Integer>();
		Map<Annotation, List<String>> anno_users = new HashMap<Annotation, List<String>>();
		int max_k = 0;
		for(Annotation anno : all_annos){
			List<String> users = anno_users.get(anno);
			if(users==null){
				users = new ArrayList<String>();
			}
			users.add(anno.getUser_id()+"");
			anno.setAnnotators(users);
			anno.setUser_id(0); //avoid confusion later as this annotation comes from multiple folks
			int c = users.size();
			anno_k.put(anno, c);
			anno_users.put(anno, users);
			if(max_k<c){max_k=c;}
		}
		Map<Integer, List<Annotation>> k_annos = new TreeMap<Integer, List<Annotation>>();
		for(Annotation anno : anno_k.keySet()){
			int k = anno_k.get(anno);
			//List<Annotation> annos = k_annos.get(k);
			//if(annos==null){ annos = new ArrayList<Annotation>();}
			//annos.add(anno);
			//k_annos.put(1*k,  annos);
			for(int _k=1;  _k<=k; _k++){
				List<Annotation> annos = k_annos.get(_k);
				if(annos==null){ annos = new ArrayList<Annotation>();}
				annos.add(anno);
				k_annos.put(_k, annos);
			}
		}
		return k_annos;
	}
	
	//Gives map of Turker experience based off of raw number of annotations they have marked, log-scaled
	
	public Map<Integer, Double> getAnnotatorExperience(List<Annotation> annoList){
		Map<Integer, Double> expList = new HashMap<Integer, Double>();
		Double maxExp = 0.0;
		for(Annotation anno : annoList){
			Integer userId = anno.getUser_id();
			Double annoExp = expList.get(userId);
			if(annoExp == null){
				annoExp = 1.0;
			}
			else{
				annoExp++;
			}
			if(annoExp > maxExp){
				maxExp = annoExp;
			}
			expList.put(userId,annoExp);
		}
		for(Integer annoID: expList.keySet()){
			expList.put(annoID, Math.log10(expList.get(annoID))/Math.log10(maxExp)); //From Zhai, et al. 2013
		}
		
		return expList;
	}
	
	//Get "Trust" Score for each turker based off f-score performance for each turker against gold-standard documents
	
	public Map<Integer, Double> getAnnotatorTrust(List<Annotation> annoList, List<Annotation> goldList){
		Double prec, reca, fscore;
		Map<Integer, Double> trustList = new HashMap<Integer, Double>();
		//Double maxTrust = 0.0;
		Map<Integer, List<Annotation>> annosByid = new TreeMap<Integer, List<Annotation>>();
		for(Annotation anno: annoList){
			Integer id = anno.getUser_id();
			Annotation simple = new Annotation(anno.getText(),anno.getStart(),anno.getStop(),anno.getDocument_id(),anno.getDocument_section(),"loc");
			List<Annotation> tatorList = annosByid.get(id);
			if(tatorList == null){
				tatorList = new ArrayList<Annotation>();
			}
			Integer docID = anno.getDocument_id();
			if(docID == 10364520 || docID == 8198128 || docID==7759075 || docID==3464560){
				tatorList.add(simple);
			}
			annosByid.put(id, tatorList);
		}
		List<Annotation> trustGold = new ArrayList<Annotation>();
		for(Annotation anno: goldList){
			Integer docID = anno.getDocument_id();
			if(docID == 10364520 || docID == 8198128 || docID==7759075 || docID==3464560){
				Annotation simple = new Annotation(anno.getText(),anno.getStart(),anno.getStop(),anno.getDocument_id(),anno.getDocument_section(),"loc");
				trustGold.add(simple);
			}
		}		
		for(Integer id: annosByid.keySet()){
			Set<Annotation> gold = new HashSet<Annotation>(trustGold);
			List<Annotation> tatorList = annosByid.get(id);
			Set<Annotation> tator = new HashSet<Annotation>(tatorList);
			//tp
			gold.retainAll(tator);
			Integer tp = gold.size();
			
			//fp
			gold = new HashSet<Annotation>(trustGold);
			tator.retainAll(gold);
			Integer fp = tator.size();
			
			//fn
			tator = new HashSet<Annotation>(tatorList);
			gold.removeAll(tator);
			Integer fn = gold.size();
			
			prec = (tp*1.0)/(tp+fp);
			reca = (tp*1.0)/(tp+fn);
			fscore = 2*prec*reca/(prec+reca);
			
			trustList.put(id,reca);
		}
		return trustList;
	}

	//Add up total user experience for each annotation, aka add up the 'experience score' for each turker that marks a specific annotation. Then performs 'voting' for annotations with minimum total user experience scores
	
	public Map<Double,List<Annotation>> getAnnotationAtAllExp(List<Annotation> annoList){
		Map<Integer, Double> expList = getAnnotatorExperience(annoList);
		//List<Annotation> expAnno = new ArrayList<Annotation>();
		Map<Annotation, Double> annoExp = new HashMap<Annotation, Double>();
		Double max_exp = 0.0;
		for(Annotation anno: annoList){
			Annotation simple = new Annotation(anno.getText(),anno.getStart(),anno.getStop(),anno.getDocument_id(),anno.getDocument_section(),"loc");
			Double exp = annoExp.get(simple);
			Double userExp = expList.get(anno.getUser_id());
			if(exp==null){exp=0.0;}
			exp+=userExp;
			annoExp.put(simple, exp);
			if(exp>max_exp){max_exp=exp;}
		}
		Double exp_step = max_exp/135;
		Map<Double,List<Annotation>> annosByExp = new TreeMap<Double,List<Annotation>>();
		for(Annotation anno: annoExp.keySet()){
			double exp = annoExp.get(anno);
			for(Double _exp = exp_step; _exp<=exp; _exp+=exp_step){
				List<Annotation> annos = annosByExp.get(_exp);
				if(annos==null){
					annos = new ArrayList<Annotation>();
				}
					annos.add(anno);
					annosByExp.put(_exp,annos);
			}
		}
		
		return annosByExp;
	}
	
	//Add up total user trust for each annotation, aka add up the 'trust score' for each turker that marks a specific annotation. Then performs 'voting' for annotations with minimum total user trust scores
	
	public Map<Double, List<Annotation>> getAnnotationsAtAllTrust(List<Annotation> annoList, List<Annotation> goldList){
		Map<Integer, Double> trustList = getAnnotatorTrust(annoList,goldList);
		Map<Annotation, Double> annoTrust = new HashMap<Annotation, Double>();
		Double max_trust = 0.0;
		for(Annotation anno: annoList){
			Annotation simple = new Annotation(anno.getText(),anno.getStart(),anno.getStop(),anno.getDocument_id(),anno.getDocument_section(),"loc");
			Double trust = annoTrust.get(simple);
			Double userTrust = trustList.get(anno.getUser_id());
			if(trust==null){trust=0.0;}
			trust+=userTrust;
			annoTrust.put(simple, trust);
			if(trust>max_trust){max_trust=trust;}
		}
		Double trust_step = max_trust/135;
		Map<Double, List<Annotation>> annosByTrust = new TreeMap<Double, List<Annotation>>();
		for(Annotation anno: annoTrust.keySet()){
			double trust = annoTrust.get(anno);
			for(Double _trust = trust_step; _trust<=trust; _trust+=trust_step){
				List<Annotation> annos = annosByTrust.get(_trust);
				if(annos==null){
					annos = new ArrayList<Annotation>();
				}
					annos.add(anno);
					annosByTrust.put(_trust,annos);
			}
		}
		return annosByTrust;
	}
	
	//Performs similar voting procedure as getAnnotationAtAllExp, but returns only those with a minimum of (expLvl * Max_Exp_for_any_annotation)/135
	
	public List<Annotation> getAnnotationAtExp(List<Annotation> annoList, int expLvl){
		Map<Integer, Double> expList = getAnnotatorExperience(annoList);
		Map<Annotation, Double> annoExp = new HashMap<Annotation, Double>();
		Double max_exp = 0.0;
		for(Annotation anno: annoList){
			Annotation simple = new Annotation(anno.getText(),anno.getStart(),anno.getStop(),anno.getDocument_id(),anno.getDocument_section(),"loc");
			Double exp = annoExp.get(simple);
			Double userExp = expList.get(anno.getUser_id());
			if(exp==null){exp=0.0;}
			exp+=userExp;
			annoExp.put(simple, exp);
			if(exp>max_exp){max_exp=exp;}
		}
		
		Double exp_step = max_exp/135;
		Map<Double,List<Annotation>> annosByExp = new TreeMap<Double,List<Annotation>>();
		for(Annotation anno: annoExp.keySet()){
			double exp = annoExp.get(anno);
			for(Double _exp = exp_step; _exp<exp; _exp+=exp_step){
				List<Annotation> annos = annosByExp.get(_exp);
				if(annos==null){
					annos = new ArrayList<Annotation>();
					annos.add(anno);
					annosByExp.put(_exp,annos);
				}
			}
		}
		
		//System.out.println(expList);
		return annosByExp.get(expLvl*exp_step);
	}
	
	public List<Annotation> getAnnotationsAtK(List<Annotation> all_annos, int kval, String identity_criteria){
		Map<Annotation, Integer> anno_k = new HashMap<Annotation, Integer>();
		int max_k = 0;
		for(Annotation anno : all_annos){
			Annotation simple = new Annotation(anno.getText(), anno.getStart(), anno.getStop(), anno.getDocument_id(), anno.getDocument_section(), identity_criteria);
			Integer c = anno_k.get(simple);
			if(c==null){ c = 0;}
			c++;
			anno_k.put(simple, c);
			if(max_k<c){max_k=c;}
		}
		Map<Integer, List<Annotation>> k_annos = new TreeMap<Integer, List<Annotation>>();
		for(Annotation anno : anno_k.keySet()){
			int k = anno_k.get(anno);
			for(int _k=1;  _k<=k; _k++){
				List<Annotation> annos = k_annos.get(_k);
				if(annos==null){ annos = new ArrayList<Annotation>();}
				annos.add(anno);
				k_annos.put(_k, annos);
			}
		}
		return k_annos.get(kval);
	}
	
	//returns list of turker ids
	
	public List<Annotator> getAnnotatorList(List<Annotation> annoList){
		List<Annotator> tatorList = new ArrayList<Annotator>();
		Map<Integer, List<Annotation>> annosByTator = new TreeMap<Integer, List<Annotation>>();
		
		Integer userId;
		for(Annotation anno: annoList){
			userId = anno.getUser_id();
			List<Annotation> tatorAnnos = annosByTator.get(userId);
			if(tatorAnnos == null){
				tatorAnnos = new ArrayList<Annotation>();
				
				Annotator tator = new Annotator(userId);
				tatorList.add(tator);
			}
			tatorAnnos.add(anno);
			annosByTator.put(userId,tatorAnnos);
		}
		
		for(Annotator tator: tatorList){
			userId = tator.getId();
			tator.addAnno(annosByTator.get(userId));
		}
		
		return tatorList;
	}
	
	//Create agreement score for annotations by adding up each individual turker agreement scores, then return map of annotations by minimum agreement
	
	public Map<Double, List<Annotation>> getAnnotationsAtAllAgreement(List<Annotation> annoList){
		Map<Double, List<Annotation>> annosByAgreement = new TreeMap<Double, List<Annotation>>();
		Double agreement;
		
		List<Annotator> tatorList = getAnnotatorList(annoList);
		
		Map<Integer, Double> agreeList = getAgreement(tatorList);
		
		Map<Annotation, Double> annoAgree = new HashMap<Annotation, Double>();
		Double maxAgreement = 0.0;
		for(Annotation anno: annoList){
			Annotation simple = new Annotation(anno.getText(),anno.getStart(),anno.getStop(),anno.getDocument_id(),anno.getDocument_section(),"loc");
			agreement = annoAgree.get(simple);
			if(agreement == null){
				agreement = 0.0;
			
				for(Annotator tator: tatorList){
					if(tator.getDocs().contains(anno.getDocument_id())){
						if(tator.getAnnos().contains(simple)){
							agreement += agreeList.get(tator.user_id);
						} else{
							agreement -= agreeList.get(tator.user_id);
						}
					}
				}
			}
			//agreement += agreeList.get(anno.getUser_id());
			annoAgree.put(simple,agreement);
			if(agreement > maxAgreement){maxAgreement=agreement;}
		}
		Double agreeStep = maxAgreement/(135);
		
		for(Annotation anno: annoList){
			Annotation simple = new Annotation(anno.getText(),anno.getStart(),anno.getStop(),anno.getDocument_id(),anno.getDocument_section(),"loc");
			agreement = annoAgree.get(simple);
			for(Double _agree = -1*maxAgreement; (_agree < agreement) && (_agree<maxAgreement); _agree += agreeStep){
				List<Annotation> stepList = annosByAgreement.get(_agree);
				if(stepList == null){
					stepList = new ArrayList<Annotation>();
				}
				stepList.add(anno);
				annosByAgreement.put(_agree,stepList);
			}
		}
		
		return annosByAgreement;
	}
	
	// Creates turker "agreement" map. Essentially adds up number of times a turker agrees with another turker who marked the same document. The total agreement is then log-scaled by the maximum number of agreements one 
	// turker has to the whole rest of the group. 
	
	public Map<Integer, Double> getAgreement(List<Annotator> tatorList){
		Map<Integer, Double> tatorTrust = new HashMap<Integer, Double>();
		Map<Integer, Integer> tatorSim = new HashMap<Integer, Integer>(); //count number of other tators can be compared to (gets rid of disjoint doc sets)
		Double trust1, trust2;
		Integer sim1, sim2, max_sim=0;
		agreementOutput tatorAgreement;
		Integer[][] tatorSimAm = new Integer[tatorList.size()][tatorList.size()];
		double[][] tatorAgreeScore = new double[tatorList.size()][tatorList.size()];
		Double agreement;
		for(int i = 0; i<tatorList.size(); i++){
			Annotator tator1 = tatorList.get(i);
			trust1 = tatorTrust.get(tator1.getId());
			sim1 = tatorSim.get(tator1.getId());
			if(trust1 == null){trust1=0.0; sim1 = 0;}
			for(int j = i+1; j < tatorList.size();j++){
				Annotator tator2 = tatorList.get(j);
				trust2 = tatorTrust.get(tator2.getId());
				sim2 = tatorSim.get(tator2.getId());
				if(trust2==null){trust2=0.0; sim2 = 0;}
				tatorAgreement = findTatorAgreement(tator1,tator2);
				agreement = tatorAgreement.agreement;
				Integer simDocs = tatorAgreement.simDoc;
				if(simDocs > max_sim){max_sim=simDocs;}
				if(agreement >= 0.0){
					trust1 += agreement;
					trust2 += agreement;
					sim1++;
					sim2++;
				}
				//tatorTrust.put(tator1.getId(), trust1);
				//tatorTrust.put(tator2.getId(), trust2);
				tatorAgreeScore[i][j] = tatorAgreement.agreement;
				tatorAgreeScore[j][i] = tatorAgreement.agreement;
				tatorSimAm[i][j] = tatorAgreement.simDoc;
				tatorSimAm[j][i] = tatorAgreement.simDoc;
				//tatorSim.put(tator1.getId(),sim1);
				//tatorSim.put(tator2.getId(),sim2);
			}
		}
		for(int i = 0; i < tatorList.size(); i++){
			Integer id1 = tatorList.get(i).getId();
			trust1 = tatorTrust.get(id1);
			sim1 = tatorSim.get(id1);
			if(trust1 == null){
				trust1 = 0.0;
				sim1 = 0;
			}
			for(int j = i+1; j < tatorList.size(); j++){
				Integer id2 = tatorList.get(j).getId();
				trust2 = tatorTrust.get(id2);
				sim2 = tatorSim.get(id2);
				if(trust2 == null){
					trust2 = 0.0;
					sim2 = 0;
				}
				Integer simDocs = tatorSimAm[i][j];
				agreement = tatorAgreeScore[i][j];
				if(simDocs > 0){
					trust1 += agreement * (Math.log(simDocs)/Math.log(max_sim)); //Log scale by maximum number of similarities any two turkers have, might be incorrect to do this hear instead of a total log scaling at the end
					trust2 += agreement * (Math.log(simDocs)/Math.log(max_sim));
					sim1++;
					sim2++;
				}
				tatorTrust.put(id1,trust1);
				tatorTrust.put(id2,trust2);
				tatorSim.put(id1, sim1);
				tatorSim.put(id2, sim2);
			}
		}
		
		//scale by number of total similar documents each person has, so get average agreement per document
		
		for(Integer id: tatorTrust.keySet()){
			tatorTrust.put(id, tatorTrust.get(id)/tatorSim.get(id));
		}
		return tatorTrust;
	}
	
	//Finds agreement between two turkers, aka number of times they mark the exact same annotation, scaled by number of unique marked annotations (Tanimoto coeff.)
	
	public agreementOutput findTatorAgreement(Annotator tator1, Annotator tator2){
		List<Annotation> dummy1 = new ArrayList<Annotation>();
		List<Annotation> dummy2 = new ArrayList<Annotation>();
		
		List<Integer> docs1 = tator1.getDocs();
		List<Integer> docs2 = tator2.getDocs();
		
		Set<Integer> docSet1 = new HashSet<Integer>(docs1);
		Set<Integer> docSet2 = new HashSet<Integer>(docs2);
		docSet2.retainAll(docSet1);
		
		if(docSet2.size() < 1){
			return new agreementOutput(-1.0,docSet2.size());
		}
		
		//Filter out only docs that are common between both tators
		for(Annotation anno: tator1.getAnnos()){
			if(docSet2.contains(anno.getDocument_id())){
				dummy1.add(anno);
			}
		}
		
		for(Annotation anno: tator2.getAnnos()){
			if(docSet2.contains(anno.getDocument_id())){
				dummy2.add(anno);
			}
		}
		
		Set<Annotation> annos1 = new HashSet<Annotation>(dummy1);
		Set<Annotation> annos2 = new HashSet<Annotation>(dummy2);
		
		annos1.retainAll(annos2);
		Double agreement = annos1.size()*1.0;
		
		annos1 = new HashSet<Annotation>(dummy1);
		
		annos1.addAll(annos2);
		Double total = annos1.size()*1.0;
		return new agreementOutput(agreement/total,docSet2.size());
	}
}

//Simple output class for the agreement score procedure

class agreementOutput{
	Double agreement;
	Integer simDoc;
	
	public agreementOutput(Double _agreement, Integer _simDoc){
		this.agreement = _agreement;
		this.simDoc = _simDoc;
	}
}
