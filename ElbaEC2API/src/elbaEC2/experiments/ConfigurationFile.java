package elbaEC2.experiments;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="xtbl")
public class ConfigurationFile
{
	@XmlAttribute
	String name;
	
	@XmlAttribute
	String version;
	
	@XmlElementWrapper(name="instances")
	@XmlElements(
		{
			@XmlElement(name="instance", type = Instance.class),
			@XmlElement(name="params", type = Params.class)
		}
	)
	List<Instance> instances;
	
	@XmlElementWrapper(name="instances")
	@XmlElement
	List<String> strs;
	
	static class Params
	{
		@XmlElementWrapper(name="env")
		@XmlElement(name="param")
		List<Param> env;
		
		@XmlElementWrapper(name="rubbos_conf")
		@XmlElement(name="param")
		List<Param> rubbos_conf;
		
		@XmlElementWrapper(name="apache_conf")
		@XmlElement(name="param")
		List<Param> apache_conf;
		
		@XmlElementWrapper(name="tomcat_conf")
		@XmlElement(name="param")
		List<Param> tomcat_conf;
		
		@XmlElementWrapper(name="logging")
		@XmlElement(name="param")
		List<Param> logging;
	}
	
	static class Param
	{
		@XmlAttribute
		String name;
		
		@XmlAttribute
		String value;
	}
	
}
