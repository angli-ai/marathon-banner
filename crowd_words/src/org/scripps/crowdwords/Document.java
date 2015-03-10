package org.scripps.crowdwords;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Document implements Comparable{
	private int id;
	private String text;
	private String title;
	private String source;
	private List<Annotation> annotations;
	private Set<Integer> annotators;
	public Boolean completion;
	
	public Document(){
		annotations = new ArrayList<Annotation>();
		annotators = new HashSet<Integer>();
		completion = false;
	}
	
	//These methods handle annotators of this document
	
	public void addTators(Integer tator){
		annotators.add(tator);
	}
	
	public void addTators(List<Integer> tators){
		annotators.addAll(tators);
	}
	
	public Set<Integer> getTators(){
		return annotators;
	}
	
	public void markComplete(){
		completion = true;
	}
	
	public void markIncomplete(){
		completion = false;
	}
	
	
	public void addAnnotations(Annotation anno){
		annotations.add(anno);
	}
	
	public void addAnnotations(List<Annotation> annoList){
		annotations.addAll(annoList);
	}
	
	public List<Annotation> getAnnotations(){
		return annotations;
	}
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	
	//completely rewrites the Annotations in a given document
	
	public void rewriteAnnos(List<Annotation> annoList){
		annotations = annoList;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Document))
			return false;

		Document rhs = (Document) obj;
		return new EqualsBuilder().
				// if deriving: appendSuper(super.equals(obj)).
				//append(document_id, rhs.document_id).
				//append(start, rhs.start).
				//append(stop, rhs.stop).
				append(getId(), rhs.getId()).
				isEquals();
	}

	@Override
	public int compareTo(Object obj) {
		if (obj == null)
			return -1;  
		if (obj == this)
			return 0;
		if (!(obj instanceof Document))
			return -1;
		Document rhs = (Document) obj;
		Integer thisstart = new Integer(this.getId());
		Integer rhsstart = new Integer(rhs.getId());
		//		     String sortstring = text.toLowerCase();
		//		     String sortstring2 = rhs.text.toLowerCase();
		//		     return(sortstring.compareTo(sortstring2));    
		return(thisstart.compareTo(rhsstart));
	}
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
				// if deriving: appendSuper(super.hashCode()).
				//append(document_id).
				//append(start).
				//append(stop).
				append(getId()).
				toHashCode();
	}
	
}
