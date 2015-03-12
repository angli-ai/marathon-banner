
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

## 3. use `newpubmed_e12_13_bioc.xml`
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
* simple-vote k=4, StanfordToken, Union:
```
tp = 22
fp = 20
fn = 12
precision = 0.5238095238095238
recall = 0.6470588235294118
Score  = 578947.3684210527
```

## 4. combine `newpubmed_e12_13_bioc.xml` with `ncbi_train_bioc.xml`
* simple-vote k=15, Union: 687718.81  (current best precision, might be useful later)
```
tp = 16
fp = 7
fn = 18
precision = 0.6956521739130435
recall = 0.47058823529411764
Score  = 561403.5087719297
```
* simple-vote k=16, Union: 796368.39
```
tp = 24
fp = 19
fn = 10
precision = 0.5581395348837209
recall = 0.7058823529411765
Score  = 623376.6233766234
```
* simple-vote k=2, Union
```
tp = 24
fp = 23
fn = 10
precision = 0.5106382978723404
recall = 0.7058823529411765
Score  = 592592.5925925926
```
* simple-vote k=3, Union:
```
tp = 27
fp = 21
fn = 7
precision = 0.5625
recall = 0.7941176470588235
Score  = 658536.5853658537
```
* simple-vote k=4, Union: 817372.59 (this is pretty good)
```
tp = 29
fp = 18
fn = 5
precision = 0.6170212765957447
recall = 0.8529411764705882
Score  = 716049.3827160493
```
* simple-vote k=4, LeftToRight:
```
tp = 26
fp = 19
fn = 8
precision = 0.5777777777777777
recall = 0.7647058823529411
Score  = 658227.8481012657
```
* simple-vote k=5, Union:
```
tp = 26
fp = 20
fn = 8
precision = 0.5652173913043478
recall = 0.7647058823529411
Score  = 650000.0
```
* simple-vote k=6, Union: (same as k=5, need to check)
```
tp = 26
fp = 20
fn = 8
precision = 0.5652173913043478
recall = 0.7647058823529411
Score  = 650000.0
```
