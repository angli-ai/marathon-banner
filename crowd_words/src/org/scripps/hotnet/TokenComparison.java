package org.scripps.hotnet;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scripps.crowdwords.Annotation;
import org.scripps.crowdwords.ComparisonReport;


//Compares sets of tokens, only has corpus level to date
//Based off of AnnotationComparison, works with same logic / algo

public class TokenComparison {
	
	
	
	public ComparisonReport compareAnnosCorpusLevel(List<token> gold, List<token> test, String exp){

		ComparisonReport report = new ComparisonReport();
		report.setFN_String_Count(new HashMap<String, Integer>());
		report.setFP_String_Count(new HashMap<String, Integer>());
		report.setTP_String_Count(new HashMap<String, Integer>());
		Set<token> g_annos = new HashSet<token>(gold);
		Set<token> t_annos = new HashSet<token>(test);
		//tp
		if(g_annos!=null&&t_annos!=null){
			Set<token> tp_set = new HashSet<token>(g_annos);
			tp_set.retainAll(t_annos);
			report.tp+=tp_set.size();
			if(tp_set!=null&&tp_set.size()>0){
				for(token tp_anno: tp_set){
					Integer c = report.getTP_String_Count().get(tp_anno.getText());
					if(c==null){ c = 0;}
					c++;
					report.getTP_String_Count().put(tp_anno.getText(), c);
				}
			}
		}
		//fp
		if(t_annos!=null){
			Set<token> fp_set = new HashSet<token>(t_annos);
			if(g_annos!=null){
				fp_set.removeAll(g_annos);
			}
			report.fp+=fp_set.size();
			if(fp_set!=null&&fp_set.size()>0){
				for(token fp_anno: fp_set){
					Integer c = report.getFP_String_Count().get(fp_anno.getText());
					if(c==null){ c = 0;}
					c++;
					report.getFP_String_Count().put(fp_anno.getText(), c);
				}
			}
		}
		//fn
		if(g_annos!=null){
			Set<token> fn_set = new HashSet<token>(g_annos);
			if(t_annos!=null){
				fn_set.removeAll(t_annos);
			}
			report.fn+=fn_set.size();
			if(fn_set!=null&&fn_set.size()>0){
				for(token fn_anno: fn_set){
					Integer c = report.getFN_String_Count().get(fn_anno.getText());
					if(c==null){ c = 0;}
					c++;
					report.getFN_String_Count().put(fn_anno.getText(), c);
				}
			}
		}
		return report;
	}
	
}