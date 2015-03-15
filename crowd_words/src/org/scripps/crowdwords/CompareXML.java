package org.scripps.crowdwords;

/**
 * Created by ang on 3/14/15.
 */
/**
 *
 */

import java.io.*;
import java.util.*;

import javax.xml.stream.XMLStreamException;

import bioc.BioCAnnotation;
import bioc.BioCCollection;
import bioc.BioCDocument;
import bioc.BioCLocation;
import bioc.BioCPassage;
import bioc.io.BioCCollectionReader;
import bioc.io.BioCDocumentWriter;
import bioc.io.BioCFactory;

/**
 * @author bgood
 *
 */
public class CompareXML {

    /**
     * @param args
     * @throws javax.xml.stream.XMLStreamException
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws XMLStreamException, IOException {
        //
        String resultfile = "/Users/ang/workspace/topcoder-banner/marathon-banner/banner_source/out-ncbi.xml";
        String goldfile = "data/ncbi/ncbi_train_bioc.xml";
//        String goldfile = "data/mturk/newpubmed_e12_13_bioc.xml";

//        resultfile = "/Users/ang/workspace/topcoder-banner/marathon-banner/banner_source/out-combo-K1.xml";
//        goldfile = "/Users/ang/workspace/topcoder-banner/marathon-banner/crowd_words/data/mturk/newpubmed_e12_13_bioc/combo-K1.xml";
        //null;

        String filename = "data/combo-k1-fp.txt";
        filename = "data/ncbi-fp.txt";

        Set<String> fp = compareResults(resultfile, goldfile);

        write(fp, filename);

        resultfile = "/Users/ang/workspace/topcoder-banner/marathon-banner/banner_source/out.xml";
//        resultfile = "/Users/ang/workspace/topcoder-banner/marathon-banner/banner_source/out-combo-K1.xml";
        BioCCollection rescollection = TestAggregation.readBioC(resultfile);
        List<Annotation> annos = convertBioCtoAnnotationListRes(rescollection);
        int count = 0;
        for (Annotation anno : annos) {
            if (fp.contains(anno.getText())) {
                ++ count;
            }
        }
        System.out.println("possible improve = " + count);
    }

    public static List<Annotation> convertBioCtoAnnotationListRes(BioCCollection biocCollection){
        List<Annotation> annos = new ArrayList<Annotation>();
        for(BioCDocument doc : biocCollection.getDocuments()){
            Integer pmid = Integer.parseInt(doc.getID());
            //			String n_annotators = doc.getInfon("n_annotators");
            //			String annotator_ids = doc.getInfon("annotators");
            for(BioCPassage passage : doc.getPassages()){
                String type = passage.getInfon("type");
                if(type.equals("title")){
                    type = "t";
                }else if(type.equals("abstract")){
                    type = "a";
                }
                for(BioCAnnotation bca : passage.getAnnotations()){
                    //assumes that we have one location per annotation
                    //will work until we get to relations.
                    int offset = bca.getLocations().get(0).getOffset();
                    Annotation anno = new Annotation(bca.getText(), offset, offset+bca.getText().length(), pmid, type, "loc");
                    anno.setUser_id(0);
                    anno.setId(0);
                    annos.add(anno);
                }
            }
        }
        return annos;
    }

    public static void write(Set<String> data, String filename) {
        try {
            FileWriter fr = new FileWriter(filename);
            BufferedWriter br = new BufferedWriter(fr);
            PrintWriter out = new PrintWriter(br);
            for (String str : data) {
                out.write(str);
                out.write("\n");
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Set<String> compareResults(String mturkfile, String goldfile) throws XMLStreamException, IOException{
        if(goldfile==null){
            System.out.println("No gold standard, just exporting voting results.");
        }else{
            System.out.println("Exporting voting results and evaluating each against gold standard");
        }
        AnnotationComparison ac = new AnnotationComparison();

        //read in the mturk annotations
        BioCCollection mturk_collection = TestAggregation.readBioC(mturkfile);
        //get the full text for export later
        Map<String, Document> id_doc = TestAggregation.convertBioCCollectionToDocMap(mturk_collection);
        //convert to local annotation representation
        List<Annotation> mturk_annos = convertBioCtoAnnotationListRes(mturk_collection);

        BioCCollection gold_collection = null;
        List<Annotation> gold_annos = null;
        Integer cnt;
        if(goldfile!=null){
            //load gold standard annotations
            gold_collection = TestAggregation.readBioC(goldfile);
            gold_annos = convertBioCtoAnnotationListRes(gold_collection);
            //filter out annotations from the gold set for docs with no mturk annotations
            boolean common_docs_only = true;
            int n_gold_annos_removed = 0;
            if(common_docs_only){
                List<Annotation> keep_annos = new ArrayList<Annotation>();
                Map<Integer, Set<Annotation>> testdoc_annos = ac.listtomap(mturk_annos);
                Set<Integer> test_ids = testdoc_annos.keySet();
                for(Annotation ganno : gold_annos ){
                    if(test_ids.contains(ganno.getDocument_id())){
                        keep_annos.add(ganno);
                    }
                }
                n_gold_annos_removed = gold_annos.size()-keep_annos.size();
                System.out.println("n_gold_annos_removed "+n_gold_annos_removed);
                gold_annos = keep_annos;
            }
        }
        if (goldfile != null) {
            //execute comparison versus gold, report results
            ComparisonReport report = ac.compareAnnosCorpusLevel(gold_annos, mturk_annos, "compare");
            System.out.println(report.getHeader());
            System.out.println(report.getRow());
            System.out.println("num = " + report.getFP_String_Count().size());
            System.out.println(report.getFP_String_Count().toString());
            Set<String> falsepos = new HashSet<String>();
            Map<String, Integer> map = report.getFP_String_Count();
            for (String key : map.keySet()) {
                if (map.get(key) > 1)
                    falsepos.add(key);
            }
            for (Annotation anno: gold_annos) {
                falsepos.remove(anno.getText());
            }
            System.out.println("size = " + falsepos.size());
            System.out.println(falsepos);
            return falsepos;
        }

        return null;

    }

}
