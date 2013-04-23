package elbaEC2;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;

public class Utils
{
	public static AWSCredentials getCredentials(String fileName)
	{
		AWSCredentials cred = new ClasspathPropertiesFileCredentialsProvider(fileName).getCredentials();
		
		return cred;
	}
	
	public static void loadXMLConfiguration(String fileName)
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc =builder.parse(fileName);
			doc.normalize();
			
			//Print instances
			NodeList nodeList = doc.getElementsByTagName("instance");
			
			for(int i = 0; i < nodeList.getLength(); i++)
			{
				Node node = nodeList.item(i);
				NamedNodeMap map = node.getAttributes();
				Node name = map.getNamedItem("name");
				System.out.println(name.getNodeValue());
				
				NodeList childNodes = node.getChildNodes();
				
				for(int j = 0; j < childNodes.getLength(); j++)
				{
					Node childNode = childNodes.item(j);
					if(childNode.getNodeName().equals("target"))
					{
						System.out.println("Name: " + childNode.getTextContent());
					}
				}
			}
			
		} catch(ParserConfigurationException | IOException | SAXException e)
		{
			e.printStackTrace();
		}
	}
}
