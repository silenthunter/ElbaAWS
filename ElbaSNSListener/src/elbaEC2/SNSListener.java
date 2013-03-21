package elbaEC2;

import java.io.IOException;

import javax.servlet.http.HttpServlet;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.Jetty;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ConfirmSubscriptionRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.UnsubscribeRequest;

public class SNSListener
{
	AmazonSNSClient client;
	Server jettyServer;
	String identity;
	
	public SNSListener(AWSCredentials cred, String identity)
	{
		this.identity = identity;
		client = new AmazonSNSClient(cred);
		jettyServer = new Server(8080);
		
		//Set the server to use our servlet
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.addServlet(HttpHandler.class, "/");
        jettyServer.setHandler(context);
        
        //Let the HttpHanlder know where to return SNS information
        HttpHandler.callback = this;
	}
	
	/**
	 * Subscribes the SNS listener to the correct 'topic'
	 */
	public void init()
	{	
		//Start the Jetty Server
		try {
			jettyServer.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Subscribe the Jetty Server to receive messages
		SubscribeRequest subscribeRequest = new SubscribeRequest("arn:aws:sns:us-east-1:151036084864:elbaCommands"
				, "http","http://108.84.30.94:8080/");
		client.subscribe(subscribeRequest);
	}
	
	/**
	 * Confirms that this address is to receive SNS updates
	 * @param topicArn
	 * @param token
	 */
	protected void subscribe(String topicArn, String token)
	{
		ConfirmSubscriptionRequest confirmSubscriptionRequest = new ConfirmSubscriptionRequest(topicArn, token);
		client.confirmSubscription(confirmSubscriptionRequest);
	}
	
	/**
	 * Checks to see if this is the destination of the message, and then runs the attached command
	 * @param subject
	 * @param message
	 */
	protected void messageReceived(String subject, String message)
	{
		//This message is for this receiver
		if(subject.equals(identity))
		{
			try {
				Runtime.getRuntime().exec(message, null);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args)
	{
		AWSCredentials cred = Utils.getCredentials("awsAccess.properties");
		
		/*SNSManager sns = new SNSManager(cred);
		sns.createNewTopic("test");
		sns.deleteTopic("test");*/
		SNSListener listener = new SNSListener(cred, args[0]);
		listener.init();
		
		try {
			Thread.sleep(600000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
