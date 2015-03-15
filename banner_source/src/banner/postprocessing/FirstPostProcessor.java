package banner.postprocessing;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.*;

import banner.tokenization.SimpleTokenizer;
import banner.tokenization.Tokenizer;
import banner.types.EntityType;
import banner.types.Mention;
import banner.types.Sentence;
import banner.types.Token;

/**
 * Created by ang on 3/14/15.
 */
public class FirstPostProcessor implements  PostProcessor {
    /**
     * Creates a new instance of {@link ParenthesisPostProcessor}
     */
    public FirstPostProcessor()
    {
        // Empty
        extractAbbrev = new ExtractAbbrev();
    }

    private Set<String> blacklist = new HashSet<String>();

    public static boolean isStart(String text)
    {
        if (text.equals("("))
            return true;
        if (text.equals("["))
            return true;
        if (text.equals("{"))
            return true;
        return false;
    }

    public static boolean isEnd(String text)
    {
        if (text.equals(")"))
            return true;
        if (text.equals("]"))
            return true;
        if (text.equals("}"))
            return true;
        return false;
    }


    private ExtractAbbrev extractAbbrev;

    // remove abbreviations that has no mentions immediately before them. and remove them for the rest of the paragraph.
    public void postProcess(Sentence sentence)
    {
        String sid = sentence.getSentenceId();
        if (sid.substring(sid.length() - 2, sid.length()).equals("00")) {
            blacklist.clear();;
//            System.out.println("clear blacklist");
        }
//        System.out.println("doing remove abbrev post process");
        List<Mention> mentions = new ArrayList<Mention>(sentence.getMentions());
        List<Token> tokens = sentence.getTokens();
//        Set<ExtractAbbrev.AbbreviationPair> abbreviationPairs = extractAbbrev.extractAbbrPairs(sentence.getText());
//        System.out.println(abbreviationPairs);
        Set<Integer> index = new HashSet<Integer>();
        for (Mention mention : mentions)
            for (int i = mention.getStart(); i < mention.getEnd(); ++ i)
                index.add(i);
        Set<String> adjsuffix = new HashSet<String>();
        adjsuffix.add("-");
        adjsuffix.add("associated");
        adjsuffix.add("specific");
        adjsuffix.add("like");
        adjsuffix.add("related");
        adjsuffix.add("causing");
        adjsuffix.add("affected");
        adjsuffix.add("infected");
        adjsuffix.add("suppressor");
//        adjsuffix.add("retarded");
//        adjsuffix.add("patients");

        Set<String> adjprefix = new HashSet<String>();
        adjprefix.add("benign");
        for (Mention mention : mentions)
        {
            int start = mention.getStart();
            int end = mention.getEnd();

//            if (start > 0 && tokens.get(start - 1).getText().equals("and")) {
//                System.out.println("sentence #" + sentence.getSentenceId() + " previous [and] : " + mention.getText() + " | blacklist : " + Arrays.toString(blacklist.toArray()) + " sentence = " + sentence.getText());
//            }
//
//            if (end < tokens.size() && tokens.get(end).getText().equals("and")) {
//                System.out.println("sentence #" + sentence.getSentenceId() + " next [and] : " + mention.getText() + " | blacklist : " + Arrays.toString(blacklist.toArray()) + " sentence = " + sentence.getText());
//            }

            if (start > 1 && adjprefix.contains(tokens.get(start - 1).getText().toLowerCase())) {
                System.out.println("sentence #" + sentence.getSentenceId() + " [missed prefix +] adjusted : " + mention.getText()  + " sentence = " + sentence.getText());
                sentence.removeMention(mention);
                sentence.addMention(new Mention(sentence, start - 1, end, EntityType.getType("Disease"), Mention.MentionType.Found));
                continue;
            }

            if (start > 2 && tokens.get(start - 1).getText().equals("and") && adjsuffix.contains(tokens.get(start-2).getText())) {
                System.out.println("sentence #" + sentence.getSentenceId() + " [xxx-/xxx-adj. and] adjusted : " + mention.getText()  + " sentence = " + sentence.getText());
                sentence.removeMention(mention);
                sentence.addMention(new Mention(sentence, start - 3, end, EntityType.getType("Disease"), Mention.MentionType.Found));
                continue;
            }

            // connect first tokens with '-', exception 'non-'
            if (start > 1 && tokens.get(start - 1).getText().equals("-") && !tokens.get(start-2).getText().toLowerCase().equals("non")) {
                int sindex = start - 1;
                int connective = 1;
                while (sindex >= 0 && connective > 0) {
                    if (tokens.get(sindex).getText().equals("-"))
                        connective = 1;
                    else {
                        -- connective;
                        if (connective < 0)
                            break;
                    }
                    -- sindex;
                }
                ++ sindex;
                // fix a encoding bug in the testing file: caused by missing [u] in St[u]ve-Wiedemann
                if (tokens.get(start).getText().toLowerCase().equals("wiedemann") && tokens.get(sindex).getText().toLowerCase().equals("ve"))
                    -- sindex;
                sentence.removeMention(mention);
                Mention m = new Mention(sentence, sindex, end, EntityType.getType("Disease"), Mention.MentionType.Found);
                sentence.addMention(m);
                System.out.println("sentence #" + sentence.getSentenceId() + " - adjusted : " + mention.getText()  + " to " + m.getText() + " sentence = " + sentence.getText());
            }

            // remove tokens with previous '-'
//            if (start > 0 && tokens.get(start - 1).getText().equals("-") && (start - 1 == 0 || !tokens.get(start - 2).getText().equals("-"))) {
//                System.out.println("sentence #" + sentence.getSentenceId() + " - removed : " + mention.getText()  + " sentence = " + sentence.getText());
//                sentence.removeMention(mention);
//                continue;
//            }

//            if (end < tokens.size() && tokens.get(end).getText().equals("-") && (end + 1 == tokens.size() || !tokens.get(end+1).getText().equals("-"))) {
//                System.out.println("sentence #" + sentence.getSentenceId() + " removed - : " + mention.getText()  + " sentence = " + sentence.getText());
//                sentence.removeMention(mention);
//                continue;
//            }

        }

    }

    public static void main(String[] args) {
        Tokenizer tokenizer = new SimpleTokenizer();
        RemoveAbbrevPostProcessor pp = new RemoveAbbrevPostProcessor();

        Sentence s1 = new Sentence("111-01", "1", "The present study constitutes an extensive molecular characterization of this disease in a small, highly inbred ethnic group with a high incidence of beta-thalassemia--the Jews of Kurdistan.");
        tokenizer.tokenize(s1);
        System.out.println(s1.getTokens());
        s1.addMention(new Mention(s1, 0, 1, EntityType.getType("Disease"), Mention.MentionType.Found));
//        s1.addMention(new Mention(s1, 2, 3, EntityType.getType("Disease"), Mention.MentionType.Found));
        System.out.println("Mentions before PP:");
        for (Mention m : s1.getMentions()) {
            System.out.println("\t" + m.getText());
        }
        pp.postProcess(s1);
        System.out.println("Mentions after PP:");
        for (Mention m : s1.getMentions()) {
            System.out.println("\t" + m.getText());
        }

        Sentence s2 = new Sentence("111-00", "1", "DiGeorge\nsyndrome (DGS) is a a developmental field defect.");
        tokenizer.tokenize(s2);
        System.out.println(s2.getTokens());
        s2.addMention(new Mention(s2, 3, 4, EntityType.getType("Disease"), Mention.MentionType.Found));
        System.out.println("Mentions before PP:");
        for (Mention m : s2.getMentions()) {
            System.out.println("\t" + m.getText());
        }
        pp.postProcess(s2);
        System.out.println("Mentions after PP:");
        for (Mention m : s2.getMentions()) {
            System.out.println("\t" + m.getText());
        }
    }

}