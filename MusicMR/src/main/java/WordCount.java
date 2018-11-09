import java.io.IOException;
import java.util.*;
        
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.htrace.fasterxml.jackson.annotation.JsonCreator;
import org.apache.htrace.fasterxml.jackson.annotation.JsonProperty;
import org.apache.htrace.fasterxml.jackson.databind.JsonNode;
import org.apache.htrace.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.*;

//import org.json.*;
//import org.json.JSONException;

public class WordCount {
	public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
	    private final IntWritable one = new IntWritable(1);
	    private HashMap<String, String> artistsRec = new HashMap<String, String>();
	    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
	    	String line = value.toString();
	    	String[] data = line.split(",");
	    	if(data.length <2) {
	    		return;
	    	}
	    	String artistname = data[0].trim();
	    	String location =data[1].trim();
	    	if(artistsRec.containsKey(artistname)){
	    		context.write(new Text(artistsRec.get(artistname)+","+location), one);
	    	}
	    	else {
	    		String url_artistname = artistname.replaceAll(" ", "+");
		    	String url = "http://itunes.apple.com/search?term="+url_artistname+"&media=music&entity=musicArtist";
		    	//System.out.println(artistname + ","+location+","+url);
		    	URL query = new URL(url);
				URLConnection urlcon = query.openConnection();
				InputStream is = urlcon.getInputStream();
				byte[] rawdata = new byte[4096];
				is.read(rawdata);
				String jsonfmt = new String(rawdata);
				//System.out.println(jsonfmt);
				ObjectMapper mapper = new ObjectMapper();
		        JsonNode root = mapper.readTree(jsonfmt);
		        JsonNode resultc = root.path("resultCount");
		        JsonNode results = root.path("results");
		        if (resultc.asInt() > 0) {  
			        if(results.isArray()) {
			        	for(JsonNode node :results) {
			        		String artist_name = node.path("artistName").asText();
			        		String genre = node.path("primaryGenreName").asText();
			        		if(genre == "") {
			        			genre = "Mistery";
			        		}
			        		artistsRec.put(artistname, artist_name + "," + genre);
			        		//System.out.println(artist_name + ","+genre+","+location);
			        		context.write(new Text(artist_name + "," + genre+","+location), one);
			        	}
			        }
		        }
	    	}
	    }
	}
        
	public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {
	    
	    public void reduce(Text key, Iterable<IntWritable> values, Context context)
	    throws IOException, InterruptedException {
	        int sum = 0;
	        for (IntWritable val : values) {
	            sum += val.get();
	        }
	        context.write(key, new IntWritable(sum));
	    }
	}
        
 public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
        
    Job job = new Job(conf, "wordcount");
  
    job.setJarByClass(WordCount.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
        
    job.setMapperClass(Map.class);
    job.setReducerClass(Reduce.class);
        
    job.setInputFormatClass(TextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);
        
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
        
    job.waitForCompletion(true);
 }
        
}
