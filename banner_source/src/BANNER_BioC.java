import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;

import dragon.nlp.tool.Tagger;
import dragon.nlp.tool.lemmatiser.EngLemmatiser;

import banner.eval.BANNER;
import banner.postprocessing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.dictionary.DictionaryTagger;
import banner.tokenization.Tokenizer;
import banner.types.Mention;
import banner.types.Sentence;
import banner.util.SentenceBreaker;

import bioc.BioCAnnotation;
import bioc.BioCCollection;
import bioc.BioCDocument;
import bioc.BioCPassage;
import bioc.io.BioCDocumentWriter;
import bioc.io.BioCFactory;
import bioc.io.woodstox.ConnectorWoodstox;

// ---------------- TopCoder submission generation ----------------
import bioc.BioCLocation;
import java.io.PrintWriter;
import java.io.*;
// ----------------------------------------------------------------

import banner.util.BannerAnnotatorVis;

public class BANNER_BioC {

	private SentenceBreaker breaker;
	private CRFTagger tagger;
	private Tokenizer tokenizer;
	private PostProcessor postProcessor;

	public static void main(String[] args) throws IOException, XMLStreamException, ConfigurationException {
		if (args.length != 3) {
			usage();
			return;
		}

		String configFilename = args[0];
		BANNER_BioC bannerBioC = new BANNER_BioC(configFilename);

		String in = args[1];
		File inFile = new File(in);
		String out = args[2];
		File outFile = new File(out);

		if (inFile.isDirectory()) {
			if (!outFile.isDirectory()) {
				usage();
				throw new IllegalArgumentException();
			}
			if (!in.endsWith("/"))
				in = in + "/";
			if (!out.endsWith("/"))
				out = in + "/";
			File[] listOfFiles = (new File(in)).listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".xml")) {
					String reportFilename = in + listOfFiles[i].getName();
					System.out.println("Processing file " + reportFilename);
					String annotationFilename = out + listOfFiles[i].getName();
					bannerBioC.processFile(reportFilename, annotationFilename);
				}
			}
		} else {
			if (outFile.isDirectory()) {
				usage();
				throw new IllegalArgumentException();
			}
			bannerBioC.processFile(in, out);
		}
	}

	private static void usage() {
		System.out.println("Usage:");
		System.out.println("\tBANNER_BioC configurationFilename inputFilename outputFilename");
		System.out.println("OR");
		System.out.println("\tBANNER_BioC configurationFilename inputDirectory outputDirectory");
	}

	public BANNER_BioC(String configFilename) throws IOException, ConfigurationException {
		long start = System.currentTimeMillis();
		HierarchicalConfiguration config = new XMLConfiguration(configFilename);
		tokenizer = BANNER.getTokenizer(config);
		DictionaryTagger dictionary = BANNER.getDictionary(config);
		EngLemmatiser lemmatiser = BANNER.getLemmatiser(config);
		Tagger posTagger = BANNER.getPosTagger(config);
		postProcessor = BANNER.getPostProcessor(config);
		HierarchicalConfiguration localConfig = config.configurationAt(BANNER.class.getPackage().getName());
		String modelFilename = localConfig.getString("modelFilename");
		System.out.println("Model: " + modelFilename);
		tagger = CRFTagger.load(new File(modelFilename), lemmatiser, posTagger, dictionary);
		System.out.println("Loaded: " + (System.currentTimeMillis() - start));
		breaker = new SentenceBreaker();
	}

	private void processFile(String inXML, String outXML) throws IOException, XMLStreamException {
		ConnectorWoodstox connector = new ConnectorWoodstox();
		BioCCollection collection = connector.startRead(new InputStreamReader(new FileInputStream(inXML), "UTF-8"));
		String parser = BioCFactory.WOODSTOX;
		BioCFactory factory = BioCFactory.newFactory(parser);
		BioCDocumentWriter writer = factory.createBioCDocumentWriter(new OutputStreamWriter(new FileOutputStream(outXML), "UTF-8"));
		writer.writeCollectionInfo(collection);
		
		// ---------------- TopCoder submission generation ----------------
		PrintWriter submission = new PrintWriter(new FileWriter("BannerAnnotate.java"));
		submission.println("public class BannerAnnotate {");
		int numArray = 1;
		int numItems = 0;
        submission.println("static void init0() {");
        submission.println("a0 = new int[] {");
		// ----------------------------------------------------------------

        List<BannerAnnotatorVis.EvalMention> mentions = new ArrayList<BannerAnnotatorVis.EvalMention>();
        HashMap<Integer, BioCDocument> docmap = new HashMap<Integer, BioCDocument>();
		while (connector.hasNext()) {
			BioCDocument document = connector.next();
			String documentId = document.getID();
			System.out.println("ID=" + documentId);
            docmap.put(Integer.parseInt(documentId), document);
			for (BioCPassage passage : document.getPassages()) {
				processPassage(documentId, passage);
			}
			writer.writeDocument(document);
//			System.out.println();

		// ---------------- TopCoder submission generation ----------------
			for (BioCPassage passage : document.getPassages()) {
	            for (BioCAnnotation annotation : passage.getAnnotations()) {
	            
	                String str = document.getID();
                    int sz = 0;
	                for (BioCLocation loc : annotation.getLocations()) {
                        BannerAnnotatorVis.EvalMention mention = new BannerAnnotatorVis.EvalMention();
                        int passageoffset = passage.getOffset();
                        mention.ID = Integer.parseInt(document.getID());
                        mention.offset = loc.getOffset();
                        mention.len = loc.getLength();
//                        System.out.println(passage.getText());
//                        System.out.println(mention.ID + ", " + mention.offset + ", " + mention.len);
//                        mention.text = passage.getText().substring(mention.offset - passageoffset, mention.offset - passageoffset + mention.len);
//                        System.out.println("annotation: " + mention.text);
//                        mention.passage = passage.getText();

//                        if (mention.text.equals("VHL"))
//                            continue;

                        ++ sz;
                        mentions.add(mention);
                        str += "," + loc.getOffset() + "," + loc.getLength() + ",";
	                }
	                if (sz>0) {
		                submission.println(str);
		                numItems++;
		                if ((numItems%1000)==0) {
		                    submission.println("};");
		                    submission.println("}");
                            submission.println("static void init"+numArray+"() {");
                            submission.println("a"+numArray+" = new int[] {");
		                    numArray++;
		                }
		            }
	            }				    
			}
		// ----------------------------------------------------------------

		}
		writer.close();
		
		// ---------------- TopCoder submission generation ----------------
		submission.println("};");
		submission.println("}");
		for (int i=0;i<numArray;i++) 
		{
    		submission.println("static int[] a"+i+";");
		}
		submission.println("static {");
		for (int i=0;i<numArray;i++) 
		{
    		submission.println("init"+i+"();");
		}
		submission.println("}");
  		submission.println("int[] annotate() {");
        submission.println("int[] ans = new int["+(numItems*3)+"];");
        submission.println("int idx = 0;");
        for (int i=0;i<numArray;i++) {
            submission.println("for (int i:a"+i+") ans[idx++] = i;");
        }
  		submission.println("return ans; }}");
		submission.flush();
        submission.close();
		// ----------------------------------------------------------------

        // do analysis
        BannerAnnotatorVis tester = new BannerAnnotatorVis();
        try {
            tester.setup();
        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
        tester.forward(mentions);
        for (BannerAnnotatorVis.EvalMention m : tester.mentionsNotFound) {
            // false negative
            System.out.println("[false negative] passage:");
            BioCDocument doc = docmap.get(m.ID);
            BioCPassage pas = null;
            for (BioCPassage passage : doc.getPassages()) {
                if (passage.getOffset() <= m.offset
                    && m.offset - passage.getOffset() + m.len <= passage.getText().length()) {
                    pas = passage;
                    break;
                }
            }
            System.out.println(pas.getText());
            System.out.println("[false negative] mention: (" + m.ID + ", " + m.offset + ", " + m.len + ")");
            System.out.println(pas.getText().substring(m.offset - pas.getOffset(), m.offset - pas.getOffset() + m.len));
        }

        for (BannerAnnotatorVis.EvalMention m : tester.mentionsFalsePos) {
            // false positive
            System.out.println("[false positive] passage:");

			BioCDocument doc = docmap.get(m.ID);
			BioCPassage pas = null;
			for (BioCPassage passage : doc.getPassages()) {
				if (passage.getOffset() <= m.offset
						&& m.offset - passage.getOffset() + m.len <= passage.getText().length()) {
					pas = passage;
					break;
				}
			}
			System.out.println(pas.getText());
			System.out.println("[false positive] mention: (" + m.ID + ", " + m.offset + ", " + m.len + ")");
			System.out.println(pas.getText().substring(m.offset - pas.getOffset(), m.offset - pas.getOffset() + m.len));

//            System.out.println(m.passage);
//            System.out.println("[false positive] mention: (" + m.ID + ", " + m.offset + ", " + m.len + ")");
//            System.out.println(m.text);
        }
	}

	private void processPassage(String documentId, BioCPassage passage) {
		// Figure out the correct next annotation ID to use
		int nextId = 0;
		for (BioCAnnotation annotation : passage.getAnnotations()) {
			String annotationIdString = annotation.getID();
			if (annotationIdString.matches("[0-9]+")) {
				int annotationId = Integer.parseInt(annotationIdString);
				if (annotationId > nextId)
					nextId = annotationId;
			}
		}

		// Process the passage text
//		System.out.println("Text=" + passage.getText());
		breaker.setText(passage.getText());
		int offset = passage.getOffset();
		List<String> sentences = breaker.getSentences();
		for (int i = 0; i < sentences.size(); i++) {
			String sentenceText = sentences.get(i);
			String sentenceId = Integer.toString(i);
			if (sentenceId.length() < 2)
				sentenceId = "0" + sentenceId;
			sentenceId = documentId + "-" + sentenceId;
			Sentence sentence = new Sentence(sentenceId, documentId, sentenceText);
			sentence = BANNER.process(tagger, tokenizer, postProcessor, sentence);
			for (Mention mention : sentence.getMentions()) {
				BioCAnnotation annotation = new BioCAnnotation();
				nextId++;
				annotation.setID(Integer.toString(nextId));
				String entityType = mention.getEntityType().getText();
				if (entityType.matches("[A-Z]+")) {
					entityType = entityType.toLowerCase();
					String first = entityType.substring(0, 1);
					entityType = entityType.replaceFirst(first, first.toUpperCase());
				}
				annotation.setInfons(Collections.singletonMap("type", entityType));
				String mentionText = mention.getText();
				annotation.setLocation(offset + mention.getStartChar(), mentionText.length());
				annotation.setText(mentionText);
				passage.addAnnotation(annotation);
			}
			offset += sentenceText.length();
		}
	}
}
