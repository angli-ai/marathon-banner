package org.scripps.crowdwords;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunking;

import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.MedlineSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

import com.aliasi.util.Files;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/** Use SentenceModel to find sentence boundaries in text */
public class SentenceSplitter {

	static final TokenizerFactory TOKENIZER_FACTORY = IndoEuropeanTokenizerFactory.INSTANCE;
	static final SentenceModel SENTENCE_MODEL  = new MedlineSentenceModel(); //IndoEuropeanSentenceModel(); 
	static final SentenceChunker SENTENCE_CHUNKER = new SentenceChunker(TOKENIZER_FACTORY,SENTENCE_MODEL);

//	Pattern refPattern1;
//	Pattern refPattern2;
	Pattern refPattern;
	Pattern citeTemplate;
	Pattern header;
	Pattern references;
	Pattern trustPattern;

	public static void main(String[] args) throws IOException {
		String text = "I am a sentence.  So am I (said alfred).  I love numbers like 3.07 because they are great.";
		test(text);
	}
	
	public static void test (String text){	
		SentenceSplitter s = new SentenceSplitter();
		List<Sentence> chunks = s.splitSentences(text);
		int i = 1;
		for (Sentence sentence : chunks) {
			System.out.println("SENTENCE "+(i++)+":");
			System.out.println(sentence.getStartIndex()+"-"+sentence.getStopIndex()+" "+sentence.getNextStartIndex()+" ---- "+sentence.getPrettyText());
		}
	}

	/***
 */
	
	public static void splitWords(String text){
		
	}

	public List<Sentence> splitSentences(String text){
		Chunking chunking = SENTENCE_CHUNKER.chunk(text.toCharArray(),0,text.length());
		Set<Chunk> sentences = chunking.chunkSet();
		if (sentences.size() < 1) {
			return null;
		}
		List<Sentence> s = new ArrayList<Sentence>();
		String slice = chunking.charSequence().toString();
		Sentence previous = null;
		for (Iterator<Chunk> it = sentences.iterator(); it.hasNext(); ) {
			Chunk sentence = it.next();
			int start = sentence.start();
			int end = sentence.end();
			String t = slice.substring(start,end);

			Sentence sent = new Sentence();
			sent.setStartIndex(start); 
			sent.setStopIndex(end);
			sent.setText(text.substring(start,end));
			sent.setText(t);
			
			if(previous!=null){
				previous.setNextStartIndex(start);
			}
			if(it.hasNext()==false){
				sent.setNextStartIndex(-1);
			}
			if(keepSentence(t)){
				s.add(sent);
			}
			previous = sent;

		}
		return s;
	}

	public boolean keepSentence(String sentence){
		String test = new String(sentence);
		if(test!=null){
			test = Sentence.makePrettyText(test);
			if(test.length()>10){
				return true;
			}
		}
		return false;
	}

}