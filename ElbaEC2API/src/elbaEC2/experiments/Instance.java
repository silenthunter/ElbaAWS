package elbaEC2.experiments;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class Instance
{
	@XmlAttribute
	String name;
	
	@XmlAttribute
	String type;
	
	@XmlElement
	String target;
	
	//@XmlElementWrapper(name="instance")
	@XmlElement(name="action")
	List<Action> actions;
	
	static class Action
	{
		@XmlAttribute
		String type;
		
		@XmlAttribute
		String seq;
		
		@XmlAttribute
		String template;
	}
}
