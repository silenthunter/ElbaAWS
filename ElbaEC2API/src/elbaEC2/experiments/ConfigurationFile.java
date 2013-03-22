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
	public String name;
	
	@XmlAttribute
	public String version;
	
	@XmlElementWrapper(name="instances")
	@XmlElements(
		{
			@XmlElement(name="instance", type = Instance.class),
			@XmlElement(name="params", type = Params.class)
		}
	)
	public List<Object> instances;
	
	public static class Params
	{
		@XmlElementWrapper(name="env")
		@XmlElement(name="param")
		public List<Param> env;
		
		@XmlElementWrapper(name="rubbos_conf")
		@XmlElement(name="param")
		public List<Param> rubbos_conf;
		
		@XmlElementWrapper(name="apache_conf")
		@XmlElement(name="param")
		public List<Param> apache_conf;
		
		@XmlElementWrapper(name="tomcat_conf")
		@XmlElement(name="param")
		public List<Param> tomcat_conf;
		
		@XmlElementWrapper(name="logging")
		@XmlElement(name="param")
		public List<Param> logging;
	}
	
	public static class Param
	{
		@XmlAttribute
		String name;
		
		@XmlAttribute
		String value;
		
		@Override
		public String toString()
		{
			return name + " = " + value;
		}
	}
	
}
