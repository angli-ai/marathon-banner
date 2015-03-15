package banner.postprocessing;

import java.util.Set;

import banner.postprocessing.ExtractAbbrev.AbbreviationPair;
import banner.tokenization.SimpleTokenizer;
import banner.tokenization.Tokenizer;
import banner.types.Mention;
import banner.types.EntityType;
import banner.types.Sentence;
import banner.types.Mention.MentionType;

public class LocalAbbreviationPostProcessor implements PostProcessor {
	private ExtractAbbrev extractAbbrev;

	public LocalAbbreviationPostProcessor() {
		extractAbbrev = new ExtractAbbrev();
	}

	private void processAbbreviation(Mention formFound, String formNotFound) {
		Sentence sentence = formFound.getSentence();
		EntityType type = formFound.getEntityType();
		int charIndex = sentence.getText().indexOf(formNotFound);
		if (charIndex == -1)
			return;
		int start = sentence.getTokenIndex(charIndex, true);
		int end = sentence.getTokenIndex(charIndex + formNotFound.length(), false);
		if (start == end)
			return;
		Mention newMention = new Mention(sentence, start, end, type, formFound.getMentionType(), formFound.getProbability());
		boolean overlaps = false;
		for (Mention mention : sentence.getMentions())
			overlaps |= mention.overlaps(newMention);
		if (!overlaps)
			sentence.addMention(newMention);
	}

	public void postProcess(Sentence sentence) {
		Set<AbbreviationPair> abbreviationPairs = extractAbbrev.extractAbbrPairs(sentence.getText());
		if (abbreviationPairs.size() > 0) {
			for (AbbreviationPair abbreviation : abbreviationPairs) {
				Mention shortMention = null;
				Mention longMention = null;
				for (Mention mention : sentence.getMentions()) {
					if (abbreviation.getShortForm().equals(mention.getText()))
						shortMention = mention;
					if (abbreviation.getLongForm().equals(mention.getText()))
						longMention = mention;
				}
				if (shortMention == null) {
					if (longMention != null)
						processAbbreviation(longMention, abbreviation.getShortForm());
				} else {
					if (longMention == null)
						processAbbreviation(shortMention, abbreviation.getLongForm());
				}
			}
		}
	}

	public static void main(String[] args) {
		Tokenizer tokenizer = new SimpleTokenizer();
		LocalAbbreviationPostProcessor pp = new LocalAbbreviationPostProcessor();

		Sentence s1 = new Sentence("1", "1", "The von Hippel-Lindau tumor suppressor gene (VHL) has a critical role in the pathogenesis of clear-cell renal cell carcinoma (RCC) , as VHL mutations have been found in both von Hippel-Lindau disease-associated and sporadic RCCs . Recent studies suggest that vascular endothelial growth factor ( VEGF ) mRNA is upregulated in RCC- and von Hippel-Lindau disease-associated tumors . We have therefore assessed the effect of the VHL gene product on VEGF expression . VEGF promoter-luciferase constructs were transiently cotransfected with a wild-type VHL ( wt-VHL ) vector in several cell lines , including 293 embryonic kidney and RCC cell lines . wt-VHL protein inhibited VEGF promoter activity in a dose-dependent manner up to 5- to 10-fold . Deletion analysis defined a 144-bp region of the VEGF promoter necessary for VHL repression . This VHL-responsive element is GC rich and specifically binds the transcription factor Sp1 in crude nuclear extracts . In Drosophila cells , cotransfected VHL represses Sp1-mediated activation but not basal activity of the VEGF promoter . We next demonstrated in coimmunoprecipitates that VHL and Sp1 were part of the same complex and , by using a glutathione-S-transferase-VHL fusion protein and purified Sp1 , that VHL and Sp1 directly interact . Furthermore , endogenous VEGF mRNA levels were suppressed in permanent RCC cell lines expressing wt-VHL , and nuclear run-on studies indicated that VHL regulation of VEGF occurs at least partly at the transcriptional level . These observations support a new mechanism for VHL-mediated transcriptional repression via a direct inhibitory action on Sp1 and suggest that loss of Sp1 inhibition may be important in the pathogenesis of von Hippel-Lindau disease and RCC . . ");
		tokenizer.tokenize(s1);
		s1.addMention(new Mention(s1, 9, 10, EntityType.getType("Disease"), Mention.MentionType.Found));
		System.out.println("Mentions before PP:");
		for (Mention m : s1.getMentions()) {
			System.out.println("\t" + m.getText());
		}
		pp.postProcess(s1);
		System.out.println("Mentions after PP:");
		for (Mention m : s1.getMentions()) {
			System.out.println("\t" + m.getText());
		}

		Sentence s2 = new Sentence("1", "1", "DiGeorge\nsyndrome (DGS) is a a developmental field defect.");
		tokenizer.tokenize(s2);
		s2.addMention(new Mention(s2, 3, 4, EntityType.getType("Disease"), MentionType.Found));
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
