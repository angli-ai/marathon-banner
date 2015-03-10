package org.scripps.crowdwords;
import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;



public class Annotation implements Comparable{
	private int id; private int kind; private String type; private String text; private int start; private int stop; private Timestamp created;
	private int user_id; private int document_id; private int section_id; int concept_id; private String user_agent; private String player_ip;
	private int experiment; private String document_section; private int k; private String gm_compare; private List<String> annotators;
	private String identity_sig;


	public static String getHeaderForOutputTable(){
		String h = "";
		h = "text\tstart\tstop\tdocument_id\tsection_id\tdocument_section\tgm_compare\tk\tannotators\t";
		return h;
	}

	public String getStringForOutputTable(){
		String out = "";

		out+=getText()+"\t"+getStart()+"\t"+getStop()+"\t"+getDocument_id()+"\t"+getSection_id()+"\t"+getDocument_section()+"\t"+getGm_compare()+"\t"+getK()+"\tu:";
		if(getAnnotators()!=null){
			for(String u : getAnnotators()){
				out+=u+",";
			}
		}
		return out.substring(0,out.length()-1);
	}

	public Annotation(String text, int start, int stop, int document_id, String document_section, String identity_criteria) {
		super();
		this.setText(text);
		this.setStart(start);
		this.setStop(stop);
		this.setDocument_id(document_id);
		this.setDocument_section(document_section);
		this.setIdentity_sig(this.getDocument_id()+"_");//+this.document_section+"_";
		if(identity_criteria.equals("loc")){
			this.setIdentity_sig(this.getIdentity_sig() + this.getStart()+"_"+this.getStop());
		}else if(identity_criteria.equals("string")){
			this.setIdentity_sig(this.getIdentity_sig() + this.getText());
		}else{
			System.out.println("identity not set to loc or string");
			System.exit(0);
		}

	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
				// if deriving: appendSuper(super.hashCode()).
				//append(document_id).
				//append(start).
				//append(stop).
				append(getIdentity_sig()).
				toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Annotation))
			return false;

		Annotation rhs = (Annotation) obj;
		return new EqualsBuilder().
				// if deriving: appendSuper(super.equals(obj)).
				//append(document_id, rhs.document_id).
				//append(start, rhs.start).
				//append(stop, rhs.stop).
				append(getIdentity_sig(), rhs.getIdentity_sig()).
				isEquals();
	}

	@Override
	public int compareTo(Object obj) {
		if (obj == null)
			return -1;  
		if (obj == this)
			return 0;
		if (!(obj instanceof Annotation))
			return -1;
		Annotation rhs = (Annotation) obj;
		Integer thisstart = new Integer(this.getStart());
		Integer rhsstart = new Integer(rhs.getStart());
		//		     String sortstring = text.toLowerCase();
		//		     String sortstring2 = rhs.text.toLowerCase();
		//		     return(sortstring.compareTo(sortstring2));    
		return(thisstart.compareTo(rhsstart));
	}

	public String getDocument_section() {
		return document_section;
	}

	public void setDocument_section(String document_section) {
		this.document_section = document_section;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getStop() {
		return stop;
	}

	public void setStop(int stop) {
		this.stop = stop;
	}

	public int getUser_id() {
		return user_id;
	}

	public void setUser_id(int user_id) {
		this.user_id = user_id;
	}

	public int getDocument_id() {
		return document_id;
	}

	public void setDocument_id(int document_id) {
		this.document_id = document_id;
	}

	public String getIdentity_sig() {
		return identity_sig;
	}

	public void setIdentity_sig(String identity_sig) {
		this.identity_sig = identity_sig;
	}

	public List<String> getAnnotators() {
		return annotators;
	}

	public void setAnnotators(List<String> annotators) {
		this.annotators = annotators;
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}

	public String getGm_compare() {
		return gm_compare;
	}

	public void setGm_compare(String gm_compare) {
		this.gm_compare = gm_compare;
	}

	public Timestamp getCreated() {
		return created;
	}

	public void setCreated(Timestamp created) {
		this.created = created;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getKind() {
		return kind;
	}

	public void setKind(int kind) {
		this.kind = kind;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getSection_id() {
		return section_id;
	}

	public void setSection_id(int section_id) {
		this.section_id = section_id;
	}

	public String getUser_agent() {
		return user_agent;
	}

	public void setUser_agent(String user_agent) {
		this.user_agent = user_agent;
	}

	public String getPlayer_ip() {
		return player_ip;
	}

	public void setPlayer_ip(String player_ip) {
		this.player_ip = player_ip;
	}

	public int getExperiment() {
		return experiment;
	}

	public void setExperiment(int experiment) {
		this.experiment = experiment;
	}
}