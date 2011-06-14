This is the NIPS0-12 test collection

@author gregor :: arbylon . net
@date 2010-08-23 (release), 2010-11-25 added citation structure
  
To obtain a corpus with various different co-occurrence types is usually difficult. The NIPS corpus
as assembled here has, however, such a wide variety: Full word content, full authorship, vocabulary,
citation graph as well as category labels. By adding the internal citation data, the community
structure can be investigated, starting at the early beginnings, when the researchers did naturally
only cite themselves a few times -- interestingly, there are cross-citations even in the first volume
-- and then more and more forming an internal community that also changes its thematic focus as the
conference develops: from neural networks to support vector machines and statistical methods like
bayesian networks. This re-enactable change of focus has also the advantage that analysis can be 
intuitively verified. Although in this process, the co-citation between articles of the conference 
intensifies, the major portion of academic communication still goes through other conferences and
journals, which is not a surprise in an interdisciplinary field like the one NIPS addresses. In order
to enhance the internal structure of the citation graph, a second dimension of co-citation is added: 
Articles that mention members of the community are linked to those ones written by the people
mentioned. A third dimension, external co-citation, was too difficult to set up cleanly, as already
the internal citation recognition required a good amount of adjustment work in addition to the auto-
matic process.


Contents and line format: 
 - nips.authors        -- document author ids, 1 line per document (line 1 = document 0), 
                          space-separated
 - nips.authors.key    -- author names, 1 line per author (line 1 = author 0)
 - nips.corpus         -- term vectors, 1 line per document, svm-light line format: 
                          nterms (termid:termfreq)+
 - nips.docs           -- document titles, 1 line per document (line 1 = document 0)
 - nips.labels         -- class labels, 1 line per document (line 1 = document 0)
 - nips.labels.extract -- class merging information (original to final labels)
 - nips.label.key      -- class label names, 1 line per label (line 1 = class 0)
 - nips.split          -- permutation of the document set, can be used for random splitting
 - nips.vocab          -- vocabulary index, 1 line per term (line 1 = term 0)
 - nips.vols           -- volume information, 1 line per document (line 1 = volume 0 
                          = year 1987, one volume per year continuously. Note: publication year
                          officially is one year later, as NIPS is held in December)
 - nips.cite          -- citation information, 1 line per document, (docid )*
 - nips.ment           -- mentioning authors that are in the NIPS community don't need direct
                          citations. format like citations, but indexing author ids. Does not
                          contain the authors of the document but may overlap with citations.
 - documents.txt       -- citations of the documents, for aligning references (publication
                          dates starting 1987). Tabbed entries are citations to other NIPS
                          papers and authors. Annotations if vague: =? same as previous, 
                          +? additional candidate, -? inconclusive, =! spelling correction,
                          # comment for the following line  

Sources:
OCR'ed files: http://nips.djvuzone.org/
Original files: http://books.nips.cc/
 --> year/volume information
 --> labels (similar labels have been merged, nips.labels.extract)
Preprocessed MAT files: http://cs.nyu.edu/~roweis/data.html
 --> term vectors
 --> document titles
 --> vocabulary  
Text files: http://cs.nyu.edu/~roweis/data.html, http://ai.stanford.edu/~gal/
 --> citation entries (via searches for nips title and abbreviations and manual
     association with author/title/docnames)
     
Extraction process (leading to citations.txt):

 -- Indexing of all author names with associated paper citation lines
 -- Regex search through text files (nipstxt), dumping of results with citation candidates:
    1: NIPS
    2: Neural [^\n]*?Information Processing[^\n]
    3: Neural\\s+(?:Network)?\\s+Information\\s+Processing
    4: Neur.{1,15}Inf.{1,10}Proc.{1,10}Syst.{1,10}[^\n]*
    5: Neur.{1,15}Inf.{1,10}[^\n]* (location in last half of paper)
    6: Inf.{1,15}Proc.{1,10}[^\n]* (location in last half of paper)
    All expressions are case-insensitiveand range from exact to sloppy to simplify the posterior 
    decision process and manual confirmation.
 -- Each candidate is listed with their full list of papers up to the time the citing article,
    which is necessary because the citations and actual paper titles sometimes differ 
    significantly. Author matching via some normalisation rules: (lower-case, removal of given names,
    Note: Allowing edit distance here lead to high error rates in short names.) Automatic association
    of unequivocal cases (less than one-third because citations differ often quite a bit from the 
    official tiles in the bibtex files and OCR errors still occur frequent). 
 -- Manual alignment for inconclusive cases. Basically from a citation and list of candidates
    the false candidates are deleted. Annotation for unclear cases or detected errors. Example:

1318 nips10/0215.txt: Poggio_T, Riesenhuber_M, "Just One View: Invariances in Inferotemporal Cell Tuning,", vol. NIPS 10, 1997, cat. Neuroscience
	1141 nips09/0041.txt: Bricolo_E, Logothetis_N, Poggio_T, "3D Object Recognition: A Model of View-Tuned Neurons,", vol. NIPS 9, 1996, cat. Neuroscience
	3:neurons

	2: 20334/20644 #1115:
	interactions in macaque area V4. Soc. Neurosc. Abstr. 23,302. 
	[ 16] Riesenhuber, M & Dayan, P (1997). Neural models for part-whole hierarchies. In Advances In 
	Neural Information Processing 9, 17-23. MIT Press. 

	1:interactions
	2:Riesenhuber
	1138 nips09/0017.txt: Dayan_P, Riesenhuber_M, "Neural Models for Pain- Whole Hierarchies,", vol. NIPS 9, 1996, cat. Neuroscience
	1318 nips10/0215.txt: Poggio_T, Riesenhuber_M, "Just One View: Invariances in Inferotemporal Cell Tuning,", vol. NIPS 10, 1997, cat. Neuroscience
	3:Neural
     
    leads to:
    
1318 nips10/0215.txt: Poggio_T, Riesenhuber_M, "Just One View: Invariances in Inferotemporal Cell Tuning,", vol. NIPS 10, 1997, cat. Neuroscience
	1141 nips09/0041.txt: Bricolo_E, Logothetis_N, Poggio_T, "3D Object Recognition: A Model of View-Tuned Neurons,", vol. NIPS 9, 1996, cat. Neuroscience
	=! Pain- Whole = Part-Whole
	1138 nips09/0017.txt: Dayan_P, Riesenhuber_M, "Neural Models for Pain- Whole Hierarchies,", vol. NIPS 9, 1996, cat. Neuroscience

    Note: After brief training, each entry can be handled in a few seconds by a human annotator, allowing manual 
    processing of the whole corpus within a few hours. Although this is only moderately interesting work, training 
    the human brain for the task is much faster than training a machine with a more sophisticated algorithm 
    that may be more error prone. And studying human acceleration in the process makes work actually more rewarding.
    I've tried automatic approaches using Levenshtein distances on the CiteseerX corpus but the results were 
    unsatisfactory due to a high amount of noise in the recognition process that calls for a truly intelligent 
    solution or to program a host of special cases. (Aligning difficult cases also takes the larger half of the 
    time for the human annotator but is more "adaptive"). This truly is a lesson learnt on learning machines and humans.  
 -- Automatic creation of reference list
 -- Finally, the portion of papers with corpus-internal out-going references is about ,,,/1740, that for incoming
    references ,,,/1740 and the corresponding citation graph has ,,,/1740 nodes. Merging in the co-authorship graph,
    the largest connected component is ,,,/1740, with only ,,,/1740 articles and ,,,/,,, authors being disconnected 
    nodes in this actor-media network.
 -- Post-processing annotations: =! : globally substituted, =? : checked, +? : untouched (to keep accuracy)
 
 Typical errors:
 
 1320 nips10/0229.txt: Hamalainen_M, Hari_R, Jousmaki_V, Oja_E, Vigario_R, "Independent Component Analysis for Identification of Artifacts in Magnetoencephalographic Recordings,", vol. NIPS 10, 1997, cat. Neuroscience
 [20] R. Vigfixio, V. Jousm/ki, M. H/m'fil/inen, R. Hari, and E. Oja. Independent component analysis for identification of artifacts in magnetoencephalographic recordings. 
	 
 -- Author mentionings are extracted by creating an inverted index of the corpus and looking up the respective authors
    from the list, writing them into a candidate file with surrounding text and a list of their publications


