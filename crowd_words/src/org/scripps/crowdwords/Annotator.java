package org.scripps.crowdwords;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class Annotator implements Comparable{

	int user_id;
	List<String> ip;
	int n_fp;
	int n_tp;
	int exp;
	private String identity_sig;
	private List<Integer> docs;
	private List<Annotation> annos;
	
	
	public Annotator(int user_id){
		this.user_id = user_id;
		this.exp = 0;
		this.setIdentity_sig(Integer.toString(user_id));
		this.annos = new ArrayList<Annotation>();
		this.docs = new ArrayList<Integer>();
	}
	
	@Override
	public int compareTo(Object obj) {
		if (obj == null)
			return -1;
		if (obj == this)
			return 0;
		if (!(obj instanceof Annotator))
			return -1;
		Annotator rhs = (Annotator) obj;
		Integer thisstart = new Integer(this.user_id);
		Integer rhsstart = new Integer(rhs.user_id);
		//		     String sortstring = text.toLowerCase();
		//		     String sortstring2 = rhs.text.toLowerCase();
		//		     return(sortstring.compareTo(sortstring2));    
		return(thisstart.compareTo(rhsstart));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Annotator))
			return false;

		Annotator rhs = (Annotator) obj;
		return new EqualsBuilder().
				// if deriving: appendSuper(super.equals(obj)).
				//append(document_id, rhs.document_id).
				//append(start, rhs.start).
				//append(stop, rhs.stop).
				append(getIdentity_sig(), rhs.getIdentity_sig()).
				isEquals();
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
	
	public String getIdentity_sig(){
		return this.identity_sig;
	}
	public void setIdentity_sig(String sig){
		this.identity_sig = sig;
	}
	
	public void addAnno(Annotation anno){
		this.annos.add(anno);
	}
	public List<Annotation> getAnnos(){
		return this.annos;
	}
	
	public void addAnno(List<Annotation> annoList){
		this.annos.addAll(annoList);
		for(Annotation anno: annoList){
			Integer docID = anno.getDocument_id();
			this.addDoc(docID);
		}
	}
	
	public List<Integer> getDocs(){
		return this.docs;
	}
	
	public void addDoc(Integer doc){
		if(!this.docs.contains(doc)){
			this.docs.add(doc);
		}
	}
	
	public Boolean containsDoc(Integer doc){
		return this.docs.contains(doc);
	}
	
	public Integer getId(){
		return this.user_id;
	}
}
