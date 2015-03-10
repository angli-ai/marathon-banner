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

    class Mention {
        int ID, offset, len;
        public boolean overlaps(Mention mention2)
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
            final Mention other = (Mention)obj;
            if (ID!=other.ID)
                return false;
            if (offset!=other.offset)
                return false;
            if (len!=other.len)
                return false;
            return true;
        }

        public int compareTo(Mention mention2)
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

    public double doExec() throws Exception {

        // load linking file
        int id_set[] = new int[5000];
        {
            BufferedReader br = new BufferedReader(new FileReader("example_data_link.csv"));
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
        List<Mention> gtfMentions = new ArrayList<Mention>();
        // load gtf file
        {
            BufferedReader br = new BufferedReader(new FileReader("example_gtf.csv"));
            while (true)
            {
                String s = br.readLine();
                if (s==null) break;
                String[] items = s.split(",");
                if (items.length>2)
                {
                    Mention m = new Mention();
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

        // get answer
        BannerAnnotate ann = new BannerAnnotate();
        int[] answer = ann.annotate();
        if ( (answer.length%3) != 0 ) {
            System.err.println("ERROR: return size not a multiple of 3.");
            System.exit(1);
        }
        List<Mention> answerMentions = new ArrayList<Mention>();
        for (int i=0;i<answer.length;i+=3) {
            Mention m = new Mention();
            m.ID = answer[i];
            if (m.ID<0 || m.ID>3000) {
                System.err.println("ERROR: ID out of range.");
                System.exit(1);
            }
            int st = id_set[m.ID];
            if (st==seed) {
                m.offset = answer[i+1];
                m.len = answer[i+2];
                answerMentions.add(m);
            }
        }

        // score
        int tp = 0;
        int fp = 0;
        int fn = 0;
        Set<Mention> mentionsNotFound = new HashSet<Mention>(gtfMentions);
        for (Mention mFound : answerMentions) {
            boolean found = false;
            if (mentionsNotFound.contains(mFound)) {
                mentionsNotFound.remove(mFound);
                found = true;
                tp++;
            } else if (gtfMentions.contains(mFound)) {
                found = true;
                for (Mention mentionRequired : new HashSet<Mention>(mentionsNotFound)) {
                    if (mFound.overlaps(mentionRequired)) {
                        mentionsNotFound.remove(mentionRequired);
                        tp++;
                    }
                }
            }
            if (!found) {
                fp++;
            }
        }
        for (Mention mentionNotFound : mentionsNotFound) {
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

    public static void main(String[] args) throws Exception {

       for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-seed")) {
                seed = Long.parseLong(args[++i]);
            } else {
                System.out.println("WARNING: unknown argument " + args[i] + ".");
            }
        }

        BannerAnnotatorVis vis = new BannerAnnotatorVis();
        try {
            double score = vis.doExec();
            System.out.println("Score  = " + score);
        } catch (Exception e) {
            System.out.println("FAILURE: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
