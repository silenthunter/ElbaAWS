package com.ggresham.elba;

import org.apache.commons.cli.*;
import java.util.ArrayList;
import java.util.HashMap;

import com.amazonaws.auth.AWSCredentials;

import elbaEC2.EC2Manager;
import elbaEC2.Utils;

public class ElbaCLI {

	/**
	 * @param args
	 * @throws ParseException 
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) throws ParseException 
	{
		Options options = new Options();
		
		options.addOption("h", "help", false, "Shows this screen");
		options.addOption("e", "Experiment", true, "The name of the experiment");
		options.addOption(OptionBuilder.withLongOpt("auth").withDescription("The Amazon access file.").hasArg().create());
		
		CommandLineParser clParser = new BasicParser();
		CommandLine cmd = clParser.parse(options, args);
		
		//Print help
		if(cmd.hasOption("help"))
		{
			HelpFormatter helpFormatter = new HelpFormatter();
			helpFormatter.printHelp("ElbaCLI <Command>", "Available commands:\nlist\nstart\nstop", options, "");
			return;
		}
		
		AWSCredentials cred = Utils.getCredentials(cmd.getOptionValue("auth"));
		EC2Manager ec2 = new EC2Manager(cred);
		
		String[] myArgs = cmd.getArgs();
		
		String command = myArgs[0];
		
		//Prints a list of the running experiments
		if(command.equals("list"))
		{
			HashMap<String, Integer> experiments = ec2.getRunningExperiments();
			System.out.println(experiments.size() + " running experiments:");
			for(String name : experiments.keySet())
			{
				System.out.println(name + "\t" + experiments.get(name));
			}
		}
		else if(command.equals("start"))
		{
			if(myArgs.length < 6)
			{
				System.out.println("Usage: java -jar ElbaCLI start <configurationXML> <project files path> <generatedTar> <rubbos files> <rubbos html>");
				return;
			}
			String xmlFile = myArgs[1];
			String projectPath = myArgs[2];
			String generatedTar = myArgs[3];
			String rubbosFiles = myArgs[4];
			String rubbosHtml = myArgs[5];
			ec2.runExperiment(xmlFile, projectPath, generatedTar, rubbosFiles, rubbosHtml);
		}
		else if(command.equals("stop"))
		{
			String experimentName = cmd.getOptionValue("e");
			ec2.killExperiment(experimentName);
		}

	}

}
