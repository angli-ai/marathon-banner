
# Results
## 1. use `ncbi_train_bioc.xml`
* Union: 801630.83
* LeftToRight: 801630.83
```
tp = 24
fp = 15
fn = 10
precision = 0.6153846153846154
recall = 0.7058823529411765
Score  = 657534.2465753424
```
## 2. use `ncbitrain_e11_bioc.xml`
* simple-vote k=6, Union: bad
* simple-vote k=6, LeftToRight: bad, but better than Union

## 4. use `newpubmed_e12_13_bioc.xml`
* simple-vote k=6, LeftToRight:
```
tp = 18
fp = 17
fn = 16
precision = 0.5142857142857142
recall = 0.5294117647058824
Score  = 521739.1304347826
```
* simple-vote k=4, Union: 778358.61
```
tp = 23
fp = 19
fn = 11
precision = 0.5476190476190477
recall = 0.6764705882352942
Score  = 605263.157894737
```
