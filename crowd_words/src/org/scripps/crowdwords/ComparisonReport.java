package org.scripps.crowdwords;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ComparisonReport {
	private String id; String text2annotate;
	private String config;
	private int k; private int N;
	public float tp; public float fp; public float fn; 
	private float P; private float R; private float F;
	float consistency;
	private Map<String, Integer> FN_String_Count;
	private Map<String, Integer> FP_String_Count;
	private Map<String, Integer> TP_String_Count;
	public void setPRF(){
		setP(tp/(tp+fp));
		if(Float.isNaN(getP())){
			setP(0);
		}
		setR(tp/(tp+fn));
		if(Float.isNaN(getR())){
			setR(0);
		}
		setF(2*getP()*getR()/(getP()+getR()));
		if(Float.isNaN(getF())){
			setF(0);
		}
		consistency = 100*2*tp/(tp+tp+fp+fn);
		if(Float.isNaN(consistency)){
			consistency = 0;
		}
	}
	public String getRow(){
		setPRF();
		String summary = tp+"\t"+fp+"\t"+fn+"\t"+getP()+"\t"+getR()+"\t"+getF()+"\t"+consistency;
		return summary;
	}
	public String getHeader(){
		String header = "TP\tFP\tFN\tprecision\trecall\tF\tconsistency\t"+getConfig();
		return header;
	}
	public String getFNcounts(String delimiter){
		String counts = "";
		List<String> fn_terms = MapFun.sortMapByValue(getFN_String_Count());
		Collections.reverse(fn_terms);
		for(String term : fn_terms){
			counts+=term+", "+getFN_String_Count().get(term)+delimiter;
		}
		return counts;
	}
	public String getFPcounts(String delimiter){
		String counts = "";
		List<String> fp_terms = MapFun.sortMapByValue(getFP_String_Count());
		Collections.reverse(fp_terms);
		for(String term : fp_terms){
			counts+=term+", "+getFP_String_Count().get(term)+delimiter;
		}
		return counts;
	}
	public String getTPcounts(String delimiter){
		String counts = "";
		List<String> tp_terms = MapFun.sortMapByValue(getTP_String_Count());
		Collections.reverse(tp_terms);
		for(String term : tp_terms){
			counts+=term+", "+getTP_String_Count().get(term)+delimiter;
		}
		return counts;
	}

	public void writeReport(String outputdir){
		String outfile = outputdir+"_fn.txt";
		try {
			FileWriter f = new FileWriter(outfile);
			f.write(getFNcounts("\n"));
			f.close();
			outfile = outputdir+"_fp.txt";
			f = new FileWriter(outfile);
			f.write(getFPcounts("\n"));
			f.close();
			outfile = outputdir+"_tp.txt";
			f = new FileWriter(outfile);
			f.write(getTPcounts("\n"));
			f.close();
			outfile = outputdir+"_summary.txt";
			f = new FileWriter(outfile);
			f.write(getHeader()+"\n"+getRow()+"\n");
			f.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeReportList(List<ComparisonReport> reports, String outfile){
		try {
			FileWriter f = new FileWriter(outfile);
			f.write("doc_id\ttext\tTP\tFP\tFN\tprecision\trecall\tF\tconsistency\tfp_map\tfn_map\ttp_map\n");
			for(ComparisonReport r : reports){
				f.write(r.getId()+"\t"+r.text2annotate+"\t"+r.getRow()+"\t"+r.getFPcounts(", ")+"\t"+r.getFNcounts(", ")+"\t"+r.getTPcounts(", ")+"\n");
			}
			f.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public float getF() {
		return F;
	}
	public void setF(float f) {
		F = f;
	}
	public String getConfig() {
		return config;
	}
	public void setConfig(String config) {
		this.config = config;
	}
	public int getN() {
		return N;
	}
	public void setN(int n) {
		N = n;
	}
	public int getK() {
		return k;
	}
	public void setK(int k) {
		this.k = k;
	}
	public float getR() {
		return R;
	}
	public void setR(float r) {
		R = r;
	}
	public float getP() {
		return P;
	}
	public void setP(float p) {
		P = p;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Map<String, Integer> getFN_String_Count() {
		return FN_String_Count;
	}
	public void setFN_String_Count(Map<String, Integer> fN_String_Count) {
		FN_String_Count = fN_String_Count;
	}
	public Map<String, Integer> getFP_String_Count() {
		return FP_String_Count;
	}
	public void setFP_String_Count(Map<String, Integer> fP_String_Count) {
		FP_String_Count = fP_String_Count;
	}
	public Map<String, Integer> getTP_String_Count() {
		return TP_String_Count;
	}
	public void setTP_String_Count(Map<String, Integer> tP_String_Count) {
		TP_String_Count = tP_String_Count;
	}

}
