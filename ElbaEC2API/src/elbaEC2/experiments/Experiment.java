package elbaEC2.experiments;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class Experiment
{
	private String experimentName;
	private ConfigurationFile configurationFile;
	private int nodesRunning;
	
	public Experiment(String experimentName)
	{
		this.experimentName = experimentName;
	}
	
	@Override
	public String toString()
	{
		return experimentName;
	}
	
	public ConfigurationFile getConfigurationFile()
	{
		return configurationFile;
	}
	
	public static Experiment loadFromXML(String fileName)
	{
		Experiment  retn = new Experiment("");
		try
		{
			File file = new File(fileName);
			JAXBContext jaxbContext = JAXBContext.newInstance(ConfigurationFile.class);
			
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			ConfigurationFile config = (ConfigurationFile)jaxbUnmarshaller.unmarshal(file);
			
			retn.configurationFile = config;
			retn.experimentName = config.name;
			
			System.out.println(config.name);
		} catch(JAXBException e)
		{
			e.printStackTrace();
		}
		
		return retn;
	}
}
