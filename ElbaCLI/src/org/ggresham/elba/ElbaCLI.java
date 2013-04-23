package org.ggresham.elba;

import org.apache.commons.cli.*;

public class ElbaCLI {

	/**
	 * @param args
	 * @throws ParseException 
	 */
	public static void main(String[] args) throws ParseException 
	{
		Options options = new Options();
		
		options.addOption("E", true, "The name of the experiment");
		
		CommandLineParser clParser = new BasicParser();
		CommandLine cmd = clParser.parse(options, args);

	}

}
