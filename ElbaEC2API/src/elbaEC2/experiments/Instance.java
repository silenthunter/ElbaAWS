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
	
	@XmlElement(name="action")
	public List<Action> actions;
	
	@Override
	public String toString()
	{
		return name + " (" + target + ")";
	}
	
	public static class Action
	{
		@XmlAttribute
		String type;
		
		@XmlAttribute
		String seq;
		
		@XmlAttribute
		String template;
		
		@Override
		public String toString() {
			return "type=" + type + " seq=" + seq + " template=" + template;
		}
	}
}
