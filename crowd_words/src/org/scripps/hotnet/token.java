package org.scripps.hotnet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.scripps.crowdwords.Annotation;
import org.scripps.crowdwords.Document;
import org.scripps.crowdwords.TestAggregation;

import bioc.BioCAnnotation;
import bioc.BioCCollection;
import bioc.BioCDocument;
import bioc.BioCPassage;

public class token implements Comparable{
	private String text;
	private Integer start;
	private Integer end;
	private Integer doc_id;
	private String id_sig;
	
	public token(String _text, Integer _start, Integer _end, Integer _doc_id){
		text = _text;
		start = _start;
		end = _end;
		doc_id = _doc_id;
		id_sig = ""+start+doc_id;
	}
	public String getText(){
		return text;
	}
	public Integer getStart(){
		return start;
	}
	public Integer getEnd(){
		return end;
	}
	public Integer getDocID(){
		return doc_id;
	}
	public String getId(){
		return id_sig;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof token))
			return false;
		//TODO note that comparable is flawed, tokens of different docs but same start points will be compared favorably. Should add in doc id
		token rhs = (token) obj;
		return new EqualsBuilder().
				// if deriving: appendSuper(super.equals(obj)).
				append(doc_id, rhs.getDocID()).
				//append(start, rhs.start).
				//append(stop, rhs.stop).
				append(start, rhs.getStart()).
				isEquals();
	}

	public int compareTo(Object obj) {
		if (obj == null)
			return -1;  
		if (obj == this)
			return 0;
		if (!(obj instanceof token))
			return -1;
		token rhs = (token) obj;
		Integer thisstart = new Integer(this.getStart());
		Integer rhsstart = new Integer(rhs.getStart());
		//		     String sortstring = text.toLowerCase();
		//		     String sortstring2 = rhs.text.toLowerCase();
		//		     return(sortstring.compareTo(sortstring2));    
		return(thisstart.compareTo(rhsstart));
	}
	
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