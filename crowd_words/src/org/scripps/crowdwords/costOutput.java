/**
 * 
 */
package org.scripps.crowdwords;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import bioc.*;
/**
 * @author jbrugg
**/

public class costOutput{
	
	int cost;
	float fmeasure;
	String docId;
	
	public costOutput(int _cost, float _fmeasure){
		cost = _cost;
		fmeasure = _fmeasure;
		docId = "";
	}
	
	public costOutput(int _cost, float _fmeasure, String _docId){
		cost = _cost; 
		fmeasure = _fmeasure;
		docId = _docId;
	}
}