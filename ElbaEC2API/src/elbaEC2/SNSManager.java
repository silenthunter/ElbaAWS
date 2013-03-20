package elbaEC2;

import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.amazonaws.services.sns.model.GetTopicAttributesRequest;
import com.amazonaws.services.sns.model.GetTopicAttributesResult;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.SetTopicAttributesRequest;
import com.amazonaws.services.sns.model.Topic;

public class SNSManager
{
	
	AmazonSNSClient client;
	
	public SNSManager(AWSCredentials cred)
	{
		client = new AmazonSNSClient(cred);
	}
	
	/**
	 * Creates a new SNS topic
	 * @param topicName The name of the new topic
	 */
	public void createNewTopic(String topicName)
	{
		CreateTopicRequest createTopicRequest = new CreateTopicRequest(topicName);
		CreateTopicResult createTopicResult = client.createTopic(createTopicRequest);
		
		//Set the name of the new topic
		SetTopicAttributesRequest setAttributes = 
				new SetTopicAttributesRequest(createTopicResult.getTopicArn(), "DisplayName", topicName);
		client.setTopicAttributes(setAttributes);
	}
	
	/**
	 * Deletes a SNS topic
	 * @param topicName The name of the topic to delete
	 * @note Makes an API request for each topic on SNS. Use sparingly if you have many topics. 
	 */
	public void deleteTopic(String topicName)
	{
		ListTopicsResult topicResults = client.listTopics();
		List<Topic> topicList = topicResults.getTopics();
		
		//Find the attributes of each topic
		for(Topic topic : topicList)
		{
			//Read the attributes of the topic
			GetTopicAttributesRequest attributeRequest = new GetTopicAttributesRequest(topic.getTopicArn());
			GetTopicAttributesResult attributeResults = client.getTopicAttributes(attributeRequest);
			
			//Check the display name against the provided topic name
			if(attributeResults.getAttributes().get("DisplayName").equals(topicName))
			{
				
				DeleteTopicRequest deleteTopicRequest = new DeleteTopicRequest(topic.getTopicArn());
				client.deleteTopic(deleteTopicRequest);
				break;
				
			}
		}
	}
}
