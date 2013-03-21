package elbaEC2.experiments;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class Experiment
{
	private String experimentName;
	private String configurationFile;
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
	
	public static Experiment loadFromXML(String fileName)
	{
		try
		{
			File file = new File(fileName);
			JAXBContext jaxbContext = JAXBContext.newInstance(ConfigurationFile.class);
			
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			ConfigurationFile config = (ConfigurationFile)jaxbUnmarshaller.unmarshal(file);
			
			System.out.println(config.name);
		} catch(JAXBException e)
		{
			e.printStackTrace();
		}
		
		//TODO: Write real code
		Experiment  retn = new Experiment("");
		return retn;
	}
}
