package banner.postprocessing;
import java.io.File;
import java.io.FileNotFoundException;
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
import sun.java2d.pipe.SpanShapeRenderer;

/**
 * Created by ang on 3/14/15.
 */
public class RemoveAbbrevPostProcessor implements  PostProcessor {
    /**
     * Creates a new instance of {@link ParenthesisPostProcessor}
     */
    Set<String> endaccept = new HashSet<String>();
    Set<String> definiteNonDisease = new HashSet<String>();

    public RemoveAbbrevPostProcessor()
    {
        // Empty
        extractAbbrev = new ExtractAbbrev();

        String filename = "data/ncbi-fp.txt";
        Scanner sc = null;
        try {
            sc = new Scanner(new File(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (sc.hasNextLine()) {
            fp.add(sc.nextLine());
        }
        System.out.print("fp = " + Arrays.toString(fp.toArray()));


        definiteNonDisease.add("gene");
        definiteNonDisease.add("virus");
        definiteNonDisease.add("protein");
//        for (String word : fp) {
//            if (word.indexOf(" ") == -1 && word.indexOf("-") == -1) {
//                System.out.println("Non-disease keywords added [" + word + "]");
//                definiteNonDisease.add(word);
//                if (word.charAt(word.length() - 1) != 's') {
//                    System.out.println("Non-disease keywords added [" + word + "s]");
//                    definiteNonDisease.add(word + "s");
//                }
//            }
//        }

        // add plural forms into the false positive word set
        for (String word : new HashSet<String>(fp)) {
            if (word.indexOf(" ") == -1 && word.indexOf("-") == -1 && word.charAt(word.length() - 1) != 's') {
                fp.add(word + "s");
            }
        }

        endaccept.add("syndrome");
        endaccept.add("syndromes");
        endaccept.add("disease");
        endaccept.add("diseases");
        endaccept.add("cancer");
        endaccept.add("cancers");
    }

    private Set<String> fp = new HashSet<String>();

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

    public void final_filtering(Sentence sentence) {
        List<Mention> mentions = new ArrayList<Mention>(sentence.getMentions());
        for (Mention m : mentions) {
            if (endaccept.contains(sentence.getTokens().get(m.getEnd() - 1).getText().toLowerCase()))
                continue;
            if (fp.contains(m.getText())) {
                System.out.println("sentence #" + sentence.getSentenceId() + " false positive removed : " + m.getText()  + " sentence = " + sentence.getText());
                sentence.removeMention(m);
            }
        }
    }

    private static boolean isPunctuation(char ch)
    {
        return ("`~!@#$%^&*()-–=_+[]\\{}|;':\",./<>?".indexOf(ch) != -1);
    }
    private static boolean isPunctuation(String s) {
        for (int i = 0; i < s.length(); ++ i)
            if (!isPunctuation(s.charAt(i)))
                return false;
        return true;
    }

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

        Set<String> adjsuffix = new HashSet<String>();

        Set<String> adjsuffix_remove = new HashSet<String>();
        adjsuffix_remove.add("retarded");
        adjsuffix_remove.add("patients");
        adjsuffix_remove.add("consuming");
        adjsuffix_remove.add("like");
        adjsuffix_remove.add("related");
        adjsuffix_remove.add("infected");
        adjsuffix_remove.add("affected");
        adjsuffix_remove.add("associated");
        adjsuffix_remove.add("specific");
        adjsuffix_remove.add("causing");
        adjsuffix_remove.add("-");
        adjsuffix_remove.add("suppressor");
        adjsuffix_remove.add("linked");


        // dealing with cases where there exists suffix {'-', adj.}
        for (Mention mention : mentions) {
            int start = mention.getStart();
            int end = mention.getEnd();

            // remove two sides punctuations
            boolean changed = false;
            while (start < end && isPunctuation(tokens.get(start).getText())) {
                changed = true;
                ++ start;
            }
            while (end > start && isPunctuation(tokens.get(end-1).getText())) {
                changed = true;
                -- end;
            }
            if (changed) {
                sentence.removeMention(mention);
                mention = new Mention(sentence, start, end, EntityType.getType("Disease"), Mention.MentionType.Found);
                sentence.addMention(mention);
            }
//            if (end - start >= 2 && tokens.get(end - 2).getText().equals("-") && tokens.get(end - 1).getText().charAt(0) <= 'z' && tokens.get(end - 1).getText().charAt(0) >= 'a')
//                System.out.println("sentence #" + sentence.getSentenceId() + " adj. suffix found : " + mention.getText()  + " sentence = " + sentence.getText());

            // remove tokens with suffix '-Adj', this should not be removed. should remove suffix otherwise.
            if (end - start >= 2 && tokens.get(end-2).getText().equals("-") && adjsuffix_remove.contains(tokens.get(end-1).getText())) {
                System.out.println("sentence #" + sentence.getSentenceId() + " adj. suffix removed : " + mention.getText()  + " sentence = " + sentence.getText());
                sentence.removeMention(mention);
                continue;
            }

            // remove the "-adj."s from the original mentions and add them to a new mention
            if (end - start >= 2 && tokens.get(end-2).getText().equals("-") && adjsuffix.contains(tokens.get(end-1).getText())) {
                sentence.removeMention(mention);
                Mention m = new Mention(sentence, start, end-2, EntityType.getType("Disease"), Mention.MentionType.Found);
                sentence.addMention(m);
                System.out.println("sentence #" + sentence.getSentenceId() + " -adj. suffix adjusted : " + mention.getText()  + " to " + m.getText() + " sentence = " + sentence.getText());
                continue;
            }
        }

        mentions = new ArrayList<Mention>(sentence.getMentions());


        for (Mention mention : mentions) {
            int start = mention.getStart();
            int end = mention.getEnd();

            if (definiteNonDisease.contains(tokens.get(end - 1).getText())) {
                System.out.println("sentence #" + sentence.getSentenceId() + " non-disease removed : " + mention.getText() + " sentence = " + sentence.getText());
                sentence.removeMention(mention);
                continue;
            }


//            if (endaccept.contains(tokens.get(end - 1).getText())) {
//                continue;
//            }

        }


        mentions = new ArrayList<Mention>(sentence.getMentions());
        Set<Integer> index = new HashSet<Integer>();
        for (Mention mention : mentions)
            for (int i = mention.getStart(); i < mention.getEnd(); ++ i)
                index.add(i);

//            if (end - start >= 2 && tokens.get(end-2).getText().equals("-")) {
//                System.out.println("sentence #" + sentence.getSentenceId() + " possible mistake found : " + mention.getText()  + " sentence = " + sentence.getText());
//                sentence.removeMention(mention);
//                continue;
//            }

//            if (start > 2 && tokens.get(start - 1).getText().equals("and") && adjsuffix.contains(tokens.get(start-2).getText())) {
//                System.out.println("sentence #" + sentence.getSentenceId() + " [xxx-/xxx-adj. and] adjusted : " + mention.getText()  + " sentence = " + sentence.getText());
//                sentence.removeMention(mention);
//                sentence.addMention(new Mention(sentence, start - 3, end, EntityType.getType("Disease"), Mention.MentionType.Found));
//            }

            // remove tokens with 'A-' immedately before, this is not true, should work on connecting this tokens.
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

//            if (start == 0 || end == tokens.size() || !isStart(tokens.get(start - 1).getText()) || !isEnd(tokens.get(end).getText())) {
//                if (blacklist.contains(mention.getText())) {
//                    System.out.println("sentence #" + sentence.getSentenceId() + " removed : " + mention.getText() + " blacklist : " + Arrays.toString(blacklist.toArray()) + " sentence = " + sentence.getText());
//                    sentence.removeMention(mention);
//                }
//                continue;
//            }

        // dealing with abbreviations
        for (Mention mention : mentions) {
            int start = mention.getStart();
            int end = mention.getEnd();

            // this has to be one word
            int connective = 1;
            int ij = start;
            while (ij < end && connective >= 0) {
                if (tokens.get(ij).getText().equals("-"))
                    connective = 1;
                else {
                    -- connective;
                    if (connective < 0)
                        break;
                }
                ++ ij;
            }

            if (connective < 0) continue;
            if (start < 2) continue;
            if (end == tokens.size()) continue;
            if (!isStart(tokens.get(start - 1).getText())) continue;
            if (!isEnd(tokens.get(end).getText())) continue;

//            System.out.println("sentence #" + sentence.getSentenceId() + " found abbreviations: " + mention.getText() + " | mentions = " + Arrays.toString(mentions.toArray()) + " | sentence = " + sentence.getText());

            // remove abbreviations without an observation immedately before
            if (!index.contains(start - 2)
                    && !endaccept.contains(tokens.get(start - 2).getText())) {
//                blacklist.add(mention.getText());
                System.out.println("sentence #" + sentence.getSentenceId() + " removed : " + mention.getText() + " sentence = " + sentence.getText());
                sentence.removeMention(mention);

//                if (definiteNonDisease.contains(tokens.get(start - 2).getText())) {
//                    // remove all of these abbrevs
//                    blacklist.add(mention.getText());
//                }

                for (int i = start; i < end; ++ i)
                    index.remove(i);
            } else {
                // this is a valid abbreviation
//                blacklist.remove(mention.getText());
//                for (int i = start; i < end; ++ i)
//                    index.remove(i);
            }
        }

        final_filtering(sentence);
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