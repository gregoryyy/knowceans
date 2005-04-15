knowceans-tools (version 20050328)

@date 2005-03-28
@author Gregor Heinrich questions / remarks email gregor :: arbylon . net

This package provides a couple of helper classes for diverse programming problems:

 - org.knowceans.util.* 
   miscellaneous classes, such as 
   
   o ExternalProcess : an interface to external processes that captures output in a 
     buffered streamreader that runs in its own thread
     
   o Conf : a properties file reader that extends the properties file format by a simple 
     way to define variables and simplify complex configurations that would require tedious
     editing.
   
   o WebLogAnalyzer : a "hack" to analyse Tomcat logs whose target log file format can be 
     easily be adjusted via regular expressions.
     
   o ...
   
   
 - org.knowceans.map.*
   Java 1.5 implementation of 1:1, 1:n, and m:n relations in top of type-safe HashMaps, 
   allowing reverse lookup and partly access to keys via wildcards and regular expressions
   
   o BijectiveHashMap : 1:1 mapping with fast search.
   
   o BijectiveTreeMap : 1:1 mapping with sorting in both domain (keys) and co-domain (values).
   
   o HashMultiMap : m:n mapping, i.e., a HashMap that allows a key map to different values,
     however without reverse lookup (from values to keys). Allows wildcard and regex lookup.
   
   o InvertibleHashMap : 1:n mapping with reverse lookup (cf. HashMap, which is a 1:n mapping
     without reverse lookup.)
     
   o InvertibleHashMultiMap : m:n mapping with reverse lookup. Allows wildcard and regex lookup.
   
   + The implementation fully uses Java-1.5's generics features. (But beware: testing has not 
     been extensive after the generics have been introduced to the *.map package)

   + Different interfaces allow to develop other implementations, e.g., TreeMap-based or such that
     leave the implementation of Map generic
   
   
 - org.knowceans.sandbox.* 
   some classes for quick playing around
   
The classes have been developed with the Eclipse platform, for which the .project and .classpath 
files are provided. Further, an ant build.xml exists.
