package elbaEC2;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

public class HttpHandler extends HttpServlet
{
	static SNSListener callback;
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.doGet(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		//Get the message headers
		String snsTopicARN = req.getHeader("x-amz-sns-topic-arn");
		String snsSubARN = req.getHeader("x-amz-sns-subscription-arn");
		String snsSignature = req.getHeader("x-amz-sns-signature");
		String snsMsgID = req.getHeader("x-amz-sns-message-id");
		String snsMsgType = req.getHeader("x-amz-sns-message-type");
		
		BufferedReader reader = req.getReader();
		String message = "";
		
		String read = "";
		read = reader.readLine();
		
		//Read the POST's contents
		while(read != null)
		{
			message += read;
			read = reader.readLine();
		}
		
		//Process each message type
		if(snsMsgType.equals("SubscriptionConfirmation"))
		{
			subscriptionJSON data = new Gson().fromJson(message, subscriptionJSON.class);
			callback.subscribe(data.TopicArn, data.Token);
		}
		if(snsMsgType.equals("Notification"))
		{
			notificationJSON data = new Gson().fromJson(message, notificationJSON.class);
			callback.messageReceived(data.Subject, data.Message);
		}
		
		super.doPost(req, resp);
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		super.doPut(req, resp);
	}
	
	class subscriptionJSON
	{
		public String Message;
		public String MessageId;
		public String Signature;
		public String SignatureVersion;
		public String SigningCertURL;
		public String SubscribeURL;
		public String Timestamp;
		public String Token;
		public String TopicArn;
		public String Type;
	}
	
	class notificationJSON
	{
		public String Message;
		public String MessageId;
		public String Signature;
		public String SignatureVersion;
		public String SigningCertURL;
		public String Subject;
		public String Timestamp;
		public String TopicArn;
		public String Type;
		public String UnsubscribeURL;
	}
}
