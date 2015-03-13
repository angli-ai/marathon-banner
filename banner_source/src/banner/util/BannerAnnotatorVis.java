package banner.util;

import edu.stanford.nlp.parser.metrics.Eval;

import java.io.*;
import java.util.Arrays;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.SecureRandom;

public class BannerAnnotatorVis {

    public static long seed = 4;

    static public class EvalMention {
        public int ID, offset, len;
//        public String text, passage;
        public boolean overlaps(EvalMention mention2)
        {
            return ID==mention2.ID && offset+len > mention2.offset && offset < mention2.offset+mention2.len;
        }
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final EvalMention other = (EvalMention)obj;
            if (ID!=other.ID)
                return false;
            if (offset!=other.offset)
                return false;
            if (len!=other.len)
                return false;
            return true;
        }

        public int compareTo(EvalMention mention2)
        {
            Integer compare = offset - mention2.offset;
            if (compare != 0)
                return compare;
            compare = offset+len - (mention2.offset+mention2.len);
            if (compare != 0)
                return compare;
            return ID - (mention2.ID);
        }
    }

    public List<EvalMention> gtfMentions  = new ArrayList<EvalMention>();
    public int id_set[] = new int[5000];

    public void setup() throws  Exception {
        // load linking file
        {
            BufferedReader br = new BufferedReader(new FileReader("submission/example_data_link.csv"));
            while (true)
            {
                String s = br.readLine();
                if (s==null) break;
                String[] items = s.split(",");
                if (items.length>1)
                {
                    int ID = Integer.parseInt(items[0]);
                    int st = Integer.parseInt(items[1]);
                    id_set[ID] = st;
                }
            }
            br.close();
        }

        gtfMentions.clear();
        // load gtf file
        {
            BufferedReader br = new BufferedReader(new FileReader("submission/example_gtf.csv"));
            while (true)
            {
                String s = br.readLine();
                if (s==null) break;
                String[] items = s.split(",");
                if (items.length>2)
                {
                    EvalMention m = new EvalMention();
                    m.ID = Integer.parseInt(items[0]);
                    int st = id_set[m.ID];
                    if (st==seed) {
                        m.offset = Integer.parseInt(items[1]);
                        m.len = Integer.parseInt(items[2]);
                        gtfMentions.add(m);
                    }
                }
            }
            br.close();
        }
    }

    public Set<EvalMention> mentionsNotFound;
    public Set<EvalMention> mentionsFalsePos;
    public double forward(List<EvalMention> inputMentions) {
        List<EvalMention> answerMentions = new ArrayList<EvalMention>();
        for (EvalMention m : inputMentions) {
            if (m.ID<0 || m.ID>3000) {
                System.err.println("ERROR: ID out of range.");
                System.exit(1);
            }
            int st = id_set[m.ID];
            if (st==seed) {
                answerMentions.add(m);
            }
        }

        // score
        int tp = 0;
        int fp = 0;
        int fn = 0;
        mentionsFalsePos = new HashSet<EvalMention>();
        mentionsNotFound = new HashSet<EvalMention>(gtfMentions);
        for (EvalMention mFound : answerMentions) {
            boolean found = false;
            if (mentionsNotFound.contains(mFound)) {
                mentionsNotFound.remove(mFound);
                found = true;
                tp++;
            } else if (gtfMentions.contains(mFound)) {
                found = true;
                for (EvalMention mentionRequired : new HashSet<EvalMention>(mentionsNotFound)) {
                    if (mFound.overlaps(mentionRequired)) {
                        mentionsNotFound.remove(mentionRequired);
                        tp++;
                    }
                }
            }
            if (!found) {
                mentionsFalsePos.add(mFound);
                fp++;
            }
        }
        for (EvalMention mentionNotFound : mentionsNotFound) {
            fn++;
        }
        System.out.println("tp = " + tp);
        System.out.println("fp = " + fp);
        System.out.println("fn = " + fn);
        double precision = (double) tp / (tp + fp);
        double recall = (double) tp / (tp + fn);
        System.out.println("precision = " + precision);
        System.out.println("recall = " + recall);
        double fmeasure = 0;
        if (precision + recall > 1e-10) {
            fmeasure = 2.0 * precision * recall / (precision + recall);
        }
        double score = 1000000.0 * fmeasure;

        return score;
    }

}
