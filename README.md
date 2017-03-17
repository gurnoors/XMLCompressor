# XMLCompressor

Used DOM XPath to compress nodes in input XML having attribute test="1". 
If the XML files are too large or real time performance constraints are needed, 
its better to use <a href="http://vtd-xml.sourceforge.net/">VTD-XML</a> or 
some other streaming parser, which does not load the whole file in memory at once.

<h3>Usage instructions:</h3>
1) Download f5gzip.jar
<br/>
2) java -jar f5gzip.jar path/to/file.xml --gzip
  

<h3>OPTIONS</h3>
     The following options are available:
     
     -z, --gzip        Compress XML tags with attribute test="1"
     -u, --gunzip      De-compress XML tags with attribute test="1"
     
     Optional:
     -o, --output      (Optional) Full path of Output file. By Default output
                       file will be in current directory.
                       
                       
<h3>Sample Output:</h3>
<pre>
gurnoorsinghbhatia$ java -jar f5gzip.jar /&ltsomePath&gt/books.xml --gzip
Compressing...
Output file path: /&ltanotherPath&gt/code/books_compressed.xml

Posting the XML
200 OK
Successfully dumped 0 post variables.
View it at http://www.posttestserver.com/data/2017/03/17/14.41.111023678391
Post body was 952 chars long.
</pre>
