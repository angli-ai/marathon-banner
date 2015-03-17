# marathon-banner
Marathon Match Challenge BANNER

## Package notes
* ``banner_source`` require stanford-corenlp library less than 3.4.1 because I use jdk1.6 which is not supported by stanford-corenlp 3.5.0+

## Improvement & Experiments
* fix problems in Sentence.java: change set to ArrayList to avoid overlaping after processing
* fix the "?-" problem: no need to fix, just skip them
* combine K=4 data with expert data, which gives 81.7 f-measure, tried other K's not getting better results
* tried stanford tokenizer: does not work
* incorporate the trust score of each annotator into the annotation selection process
* postprocessing: rule-based mention combination and rejection
* postprocessing: training-based false positive rejection

## Something planed but not done:
* get a new overlapping solution instead of union
