package org.scripps.crowdwords;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnnotationComparison {

	public ComparisonReport compareAnnosCorpusLevel(List<Annotation> gold, List<Annotation> test, String exp){

		ComparisonReport report = new ComparisonReport();
		report.setFN_String_Count(new HashMap<String, Integer>());
		report.setFP_String_Count(new HashMap<String, Integer>());
		report.setTP_String_Count(new HashMap<String, Integer>());
		Set<Annotation> g_annos = new HashSet<Annotation>(gold);
		Set<Annotation> t_annos = new HashSet<Annotation>(test);
		//tp
		if(g_annos!=null&&t_annos!=null){
			Set<Annotation> tp_set = new HashSet<Annotation>(g_annos);
			tp_set.retainAll(t_annos);
			report.tp+=tp_set.size();
			if(tp_set!=null&&tp_set.size()>0){
				for(Annotation tp_anno: tp_set){
					Integer c = report.getTP_String_Count().get(tp_anno.getText());
					if(c==null){ c = 0;}
					c++;
					report.getTP_String_Count().put(tp_anno.getText(), c);
				}
			}
		}
		//fp
		if(t_annos!=null){
			Set<Annotation> fp_set = new HashSet<Annotation>(t_annos);
			if(g_annos!=null){
				fp_set.removeAll(g_annos);
			}
			report.fp+=fp_set.size();
			if(fp_set!=null&&fp_set.size()>0){
				for(Annotation fp_anno: fp_set){
					Integer c = report.getFP_String_Count().get(fp_anno.getText());
					if(c==null){ c = 0;}
					c++;
					report.getFP_String_Count().put(fp_anno.getText(), c);
				}
			}
		}
		//fn
		if(g_annos!=null){
			Set<Annotation> fn_set = new HashSet<Annotation>(g_annos);
			if(t_annos!=null){
				fn_set.removeAll(t_annos);
			}
			report.fn+=fn_set.size();
			if(fn_set!=null&&fn_set.size()>0){
				for(Annotation fn_anno: fn_set){
					Integer c = report.getFN_String_Count().get(fn_anno.getText());
					if(c==null){ c = 0;}
					c++;
					report.getFN_String_Count().put(fn_anno.getText(), c);
				}
			}
		}
		return report;
	}


	public List<ComparisonReport> compareAnnosDocumentLevel(List<Annotation> gold, List<Annotation> test){
		List<ComparisonReport> reports = new ArrayList<ComparisonReport>();
		Map<Integer, Set<Annotation>> doc_gold = listtomap(gold);
		Map<Integer, Set<Annotation>> doc_test = listtomap(test);
		//in case one or the other of the sets has no annotations, merge the docids and allow for all false positives or all false negatives..
		Set<Integer> alldocs = new HashSet<Integer>(doc_gold.keySet());
		alldocs.addAll(doc_test.keySet());
		for(Integer docid : alldocs){
			List<Annotation> gold_annos = new ArrayList<Annotation>();
			if(doc_gold.get(docid)!=null){
				gold_annos.addAll(doc_gold.get(docid));
			}
			List<Annotation> test_annos = new ArrayList<Annotation>();
			if(doc_test.get(docid)!=null){
				test_annos = new ArrayList<Annotation>(doc_test.get(docid));	
			}
			ComparisonReport report = compareAnnosCorpusLevel(gold_annos, test_annos, docid+"");
			report.setId(docid+"");
		//	report.text2annotate = getTextByDocId(docid);
			reports.add(report);
		}

		return reports;
	}
	
	public Map<Integer, Set<Annotation>> listtomap(List<Annotation> _annos){
		Map<Integer, Set<Annotation>> doc_annos = new HashMap<Integer, Set<Annotation>>();
		Set<Annotation> annos = new HashSet<Annotation>(_annos);
		//	_annos.removeAll(annos);
		//	System.out.println(annos.size()+" "+_annos.size());
		for(Annotation anno : annos){
			Set<Annotation> vals = doc_annos.get(anno.getDocument_id());
			if(vals==null){
				vals = new HashSet<Annotation>();
			}
			vals.add(anno);
			doc_annos.put(anno.getDocument_id(), vals);
		}
		return doc_annos;
	}

	/**
	 * given a list of annotations return a map from document id to a list of annotations associated with each id
	 * @param _annos
	 * @return
	 */
	public Map<Integer, List<Annotation>> listtomaplist(List<Annotation> _annos){
		Map<Integer, List<Annotation>> doc_annos = new HashMap<Integer, List<Annotation>>();
		List<Annotation> annos = new ArrayList<Annotation>(_annos);
		for(Annotation anno : annos){
			List<Annotation> vals = doc_annos.get(anno.getDocument_id());
			if(vals==null){
				vals = new ArrayList<Annotation>();
			}
			vals.add(anno);
			doc_annos.put(anno.getDocument_id(), vals);
		}
		return doc_annos;
	}
}
