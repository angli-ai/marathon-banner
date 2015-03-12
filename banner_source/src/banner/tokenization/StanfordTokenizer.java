package banner.tokenization;

/**
 * Created by ang on 3/11/15.
 */

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import banner.types.Sentence;
import banner.types.Token;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;


public class StanfordTokenizer implements Tokenizer {

    public void tokenize(Sentence sentence) {
        PTBTokenizer ptbt = new PTBTokenizer(new StringReader(sentence.getText()), new CoreLabelTokenFactory(), "");
        for (CoreLabel label; ptbt.hasNext(); ) {
            label = (CoreLabel)ptbt.next();
            sentence.addToken(new Token(sentence, label.beginPosition(), label.endPosition()));
        }
    }

    public List<String> getTokens(String text) {
        List<String> tokens = new ArrayList<String>();
        PTBTokenizer ptbt = new PTBTokenizer(new StringReader(text), new CoreLabelTokenFactory(), "");
        for (CoreLabel label; ptbt.hasNext(); ) {
            label = (CoreLabel)ptbt.next();
            tokens.add(label.toString());
        }
        return tokens;
    }

    public static void main(String[] args) throws IOException {
        String arg = "This is short. I am Dodge. Dr. Peterson is a bad guy ?-hehe something. Testing (A. B. C. E.) also. And another.";
//        for (String arg : args) {
            // option #1: By sentence.
//            DocumentPreprocessor dp = new DocumentPreprocessor(arg);
//            for (List sentence : dp) {
//                System.out.println(sentence);
//            }
            // option #2: By token
            PTBTokenizer ptbt = new PTBTokenizer(new StringReader(arg),
                    new CoreLabelTokenFactory(), "");
            for (CoreLabel label; ptbt.hasNext(); ) {
                label = (CoreLabel)ptbt.next();
                System.out.println(label + " : " + label.beginPosition() + " , " + label.endPosition());
            }
//        }
    }
}

