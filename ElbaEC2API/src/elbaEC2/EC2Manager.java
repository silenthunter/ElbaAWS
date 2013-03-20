package elbaEC2;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.DimensionFilter;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsRequest;
import com.amazonaws.services.ec2.model.DescribeSpotInstanceRequestsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;

public class EC2Manager
{
	AWSCredentials credentials;
	AmazonEC2 ec2;
	AmazonElasticLoadBalancingClient elb;
	AmazonCloudWatchClient cloudClient;
	
	public EC2Manager(AWSCredentials credentials)
	{
		this.credentials = credentials;
		ec2 = new AmazonEC2Client(credentials);
		elb = new AmazonElasticLoadBalancingClient(credentials);
		cloudClient = new AmazonCloudWatchClient(credentials);
	}
	
	public void createSecurityGroup(String groupName)
	{
		DescribeSecurityGroupsResult secReq = ec2.describeSecurityGroups();
		List<SecurityGroup> securityGroups = secReq.getSecurityGroups();
		
		//Check for exist group by that name
		for(SecurityGroup secGroup : securityGroups)
		{
			//Match found
			if(secGroup.getGroupName().compareTo(groupName) == 0)
			{
				System.err.println("Security group: " + groupName + " already exists!");
				return;
			}
		}
		
		CreateSecurityGroupRequest createReq = new CreateSecurityGroupRequest();
		createReq.setGroupName(groupName);
	}
	
	public void createSpotInstances()
	{
		RequestSpotInstancesRequest spotReq = new RequestSpotInstancesRequest();
		spotReq.setSpotPrice(".015");
		spotReq.setInstanceCount(11);
		spotReq.setLaunchGroup("ggreshamElba");
		
		ArrayList<String> securityGroups = new ArrayList<String>();
		securityGroups.add("elba");
		LaunchSpecification launchSpecification = new LaunchSpecification();
		launchSpecification.setSecurityGroups(securityGroups);
		launchSpecification.setImageId("ami-d624babf");
		launchSpecification.setInstanceType(InstanceType.M1Small);
		
		spotReq.setLaunchSpecification(launchSpecification);
		
		//Kill the request in 3 hours
		/*Date now = new Date();
		Calendar cal = new GregorianCalendar();
		cal.setTime(now);
		cal.add(Calendar.HOUR, 2);
		Date validUntil = cal.getTime();
		spotReq.setValidUntil(validUntil);*/
		
		RequestSpotInstancesResult reqResult = ec2.requestSpotInstances(spotReq);
		
		List<SpotInstanceRequest> spotInstanceRequest = reqResult.getSpotInstanceRequests();
		
		//Store the ids of the spot instances
		ArrayList<String> spotIds = new ArrayList<String>();
		
		for(SpotInstanceRequest request : spotInstanceRequest)
			spotIds.add(request.getInstanceId());
		
		boolean stillOpen = false;
		
		ArrayList<String> launchIds = new ArrayList<String>();
		
		//Wait until the nodes are open
		do
		{
			DescribeSpotInstanceRequestsRequest requestRequest = new DescribeSpotInstanceRequestsRequest();
			requestRequest.setSpotInstanceRequestIds(spotIds);
			
			//Init to false and check for open requests
			stillOpen = false;
			launchIds = new ArrayList<String>();
			
			try
			{
				DescribeSpotInstanceRequestsResult requestResults = ec2.describeSpotInstanceRequests(requestRequest);
				List<SpotInstanceRequest> instanceRequests = requestResults.getSpotInstanceRequests();
				
				for(SpotInstanceRequest req : instanceRequests)
				{
					if(req.getState().equals("open"))
					{
						stillOpen = true;
						break;
					}
					
					launchIds.add(req.getInstanceId());
				}
			}
			catch(AmazonServiceException  e){stillOpen = true;}
			
			//Delay next check
			if(stillOpen)
			{
				try
				{
					Thread.sleep(30000);
				}catch(Exception e){}
			}
			
		}while(stillOpen);
		
		for(int i = 1; i <= launchIds.size(); i++)
		{
			//Tag the nodes
			ArrayList<Tag> tags = new ArrayList<Tag>();
			ArrayList<String> resources = new ArrayList<String>();
			resources.add(launchIds.get(i - 1));
			
			tags.add(new Tag("ExperimentName", "Elba"));
			tags.add(new Tag("NodeNum", "node" + i));
			tags.add(new Tag("Name", "node" + i));
			CreateTagsRequest tagsReq = new CreateTagsRequest();
			tagsReq.setTags(tags);
			tagsReq.setResources(resources);
			
			ec2.createTags(tagsReq);
		}
		
	}
	
	public void getSpotInstances()
	{
		DescribeInstancesResult res = ec2.describeInstances();
		
		for(Reservation rsv : res.getReservations())
		{
			for(Instance inst : rsv.getInstances())
			{
				System.out.println(inst.getImageId());
			}
		}
	}
	
	public void tagMyInstances()
	{
		DescribeInstancesResult instRes = ec2.describeInstances();
		
		ArrayList<String> ids = new ArrayList<String>();
		List<Reservation> resv = instRes.getReservations();
		
		for(Reservation rs : resv)
		{
			for(Instance inst : rs.getInstances())
			{
				if(inst.getImageId().equals("ami-d624babf") && inst.getState().getName().equals("running"))
				{
					ids.add(inst.getInstanceId());
				}
			}
		}
		
		for(int i = 1; i <= ids.size(); i++)
		{
			//Tag the nodes
			ArrayList<Tag> tags = new ArrayList<Tag>();
			ArrayList<String> resources = new ArrayList<String>();
			resources.add(ids.get(i - 1));
			
			tags.add(new Tag("ExperimentName", "Elba"));
			tags.add(new Tag("NodeNum", "node" + i));
			tags.add(new Tag("Name", "node" + i));
			CreateTagsRequest tagsReq = new CreateTagsRequest();
			tagsReq.setTags(tags);
			tagsReq.setResources(resources);
			
			ec2.createTags(tagsReq);
		}
		
	}
	
	public void setHosts()
	{
		File keyfile = new File("C:\\Users\\Gdeter\\Documents\\ggkey.pem");
	
		DescribeInstancesResult instRes = ec2.describeInstances();
		
		ArrayList<String> addrs = new ArrayList<String>();
		List<Reservation> resv = instRes.getReservations();
		HashMap<String, String> nodeAddrMap = new HashMap<String, String>();
		
		//Map the tags
		for(Reservation rs : resv)
		{
			for(Instance inst : rs.getInstances())
			{
				if(inst.getImageId().equals("ami-d624babf") && inst.getState().getName().equals("running"))
				{
					String addr = inst.getPublicDnsName();
					List<Tag> tags = inst.getTags();
					
					for(Tag tag : tags)
					{
						if(tag.getKey().equals("NodeNum"))
						{
							//Add this key to the mapping
							nodeAddrMap.put(tag.getValue(), inst.getPrivateIpAddress());
							addrs.add(addr);
						}
					}
				}
			}
		}
		
		for(int i = 0; i < addrs.size(); i++)
		{
			String addr = addrs.get(i);
			
			try
			{
				Connection conn = new Connection(addr);
				
				conn.connect();
				
				boolean isAuthenticated = conn.authenticateWithPublicKey("ec2-user", keyfile, "");
				
				Session sess = conn.openSession();
				
				String command = "echo -e '";
				command += "127.0.0.1 localhost localhost.localdomain\\n";
				for(String node : nodeAddrMap.keySet())
				{
					command += nodeAddrMap.get(node) + " " + node + "\\n";
				}
				command += "107.22.188.112 balancer\\n";
				command += "' | sudo tee /etc/hosts;";
				command += "echo -e \"Host *\\n\\tStrictHostKeyChecking no\" > .ssh/config;";
				command += "chmod 700 .ssh/config;";
				//command += "rm test/ -rf;";
				//command += "rm exper.tar;";
				//; echo '' > ~/.ssh/known_hosts;";
				/*command += "echo ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAlbwi847yEFNuc8D+zvES5WL" +
						"pfPPOdydQSaDKi1bvUCR09tGjnWeeB0tu0V7wLqSD8/h/qoIdxt7YnXTG8jUV8" +
						"GRO1tAecHdfF7uDv4J0A6HxH2HJnCxVqmVleEel6B1vRqVuDeTsimKmtGXlRQY" +
						"GFeN0dv8ZMw8umAbl9cQP+mZCc3ycSBwhYtY9CebHmlSxAcw5YGsexMFPrAau1" +
						"D12CH3i3OfViEHXcd2q5ZPyTkrDpAjPhFiezr2+wcxrvyJL/9U7Ehojp0sArh0" +
						"jFhMncFtUb4vk5lRN2qvmTWLVgNx46S2beC/rb5jcyXkNPpHHstibAa2Hp0i1" +
						"1+KIfAwZSw== ec2-user >> ~/.ssh/authorized_keys";*/
					
				sess.execCommand(command);
				
				InputStream stdout = new StreamGobbler(sess.getStdout());

				BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

				System.out.println("Here is some information about the remote host:");

				while (true)
				{
					String line = br.readLine();
					if (line == null)
						break;
					System.out.println(line);
				}
				
				br.close();
				
				conn.close();
				
			}catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void distributeDirectory(String dir)
	{
		File keyfile = new File("C:\\Users\\Gdeter\\Documents\\ggkey.pem");
	
		DescribeInstancesResult instRes = ec2.describeInstances();
		
		ArrayList<String> addrs = new ArrayList<String>();
		List<Reservation> resv = instRes.getReservations();
		HashMap<String, String> nodeAddrMap = new HashMap<String, String>();
		String address = "";
		
		//Map the tags
		for(Reservation rs : resv)
		{
			for(Instance inst : rs.getInstances())
			{
				if(inst.getImageId().equals("ami-d624babf") && inst.getState().getName().equals("running"))
				{
					String addr = inst.getPublicDnsName();
					List<Tag> tags = inst.getTags();
					
					for(Tag tag : tags)
					{
						if(tag.getKey().equals("NodeNum"))
						{
							//Add this key to the mapping
							nodeAddrMap.put(tag.getValue(), inst.getPrivateIpAddress());
							addrs.add(addr);
							if(tag.getValue().equals("node1"))
								address = addr;
						}
					}
				}
			}
		}
		
		try
		{
			Connection conn = new Connection(address);
			
			conn.connect();
			
			boolean isAuthenticated = conn.authenticateWithPublicKey("ec2-user", keyfile, "");
			
			Session sess = conn.openSession();
			
			String command = "tar czvf exper.tar " + dir + ";";
			for(String node : nodeAddrMap.keySet())
			{
				command += "scp exper.tar ec2-user@" + node + ":~/;" ;
			}
			
			for(String node : nodeAddrMap.keySet())
			{
				command += "ssh " + node + " \"tar xvf exper.tar\";";
			}
			
			sess.execCommand(command);
			
			InputStream stdout = new StreamGobbler(sess.getStdout());

			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

			System.out.println("Here is some information about the remote host:");

			while (true)
			{
				String line = br.readLine();
				if (line == null)
					break;
				System.out.println(line);
			}
			
			br.close();
			
			conn.close();
			
		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void createLoadBalancers()
	{
		//Get running instances
		ArrayList<com.amazonaws.services.elasticloadbalancing.model.Instance> instances = 
				new ArrayList<com.amazonaws.services.elasticloadbalancing.model.Instance>();
		DescribeInstancesResult instanceRes = ec2.describeInstances();
		for(Reservation reserv : instanceRes.getReservations())
		{
			for(Instance inst : reserv.getInstances())
			{
				for(Tag tag : inst.getTags())
				{
					if(tag.getKey().equals("Name") && 
							(tag.getValue().equals("node7") || tag.getValue().equals("node10")))
					{
						//Convert to an ELB instance
						com.amazonaws.services.elasticloadbalancing.model.Instance elbInst =
								new com.amazonaws.services.elasticloadbalancing.model.Instance(inst.getInstanceId());
						instances.add(elbInst);
					}
				}
			}
		}
		
		//Configure the balancer
		CreateLoadBalancerRequest balReq = new CreateLoadBalancerRequest();
		balReq.setLoadBalancerName("HTTPDbalancer");
		
		ArrayList<Listener> listeners = new ArrayList<Listener>();
		listeners.add(new Listener("tcp", 8000, 8000));
		balReq.setListeners(listeners);
		
		DescribeAvailabilityZonesResult availZones = ec2.describeAvailabilityZones();
		List<AvailabilityZone> zones = availZones.getAvailabilityZones();
		ArrayList<String> zonesStr = new ArrayList<String>();
		for(AvailabilityZone zone : zones)
			zonesStr.add(zone.getZoneName());
		balReq.setAvailabilityZones(zonesStr);
		
		//Create the balancer
		CreateLoadBalancerResult balRes = elb.createLoadBalancer(balReq);
		
		//Register the instances
		RegisterInstancesWithLoadBalancerRequest registerInstancesReq = new RegisterInstancesWithLoadBalancerRequest();
		registerInstancesReq.setLoadBalancerName("HTTPDbalancer");
		
		registerInstancesReq.setInstances(instances);
		elb.registerInstancesWithLoadBalancer(registerInstancesReq);
	}
	
	public void cloudMetrics()
	{
		ListMetricsRequest metricReq = new ListMetricsRequest();
		metricReq.setMetricName("CPUUtilization");
		metricReq.setNamespace("AWS/EC2");
		
		/*ArrayList<DimensionFilter> filters = new ArrayList<DimensionFilter>();
		DimensionFilter dimFil = new DimensionFilter();
		dimFil.setName("CPUUtilization");
		filters.add(dimFil);
		metricReq.setDimensions(filters);*/
		
		ListMetricsResult metricRes = cloudClient.listMetrics(metricReq);
		
		List<Metric> metrics = metricRes.getMetrics();
		for(Metric metric : metrics)
		{
			System.out.println(metric.toString());
		}
		
		GetMetricStatisticsRequest statReq = new GetMetricStatisticsRequest();
		statReq.setMetricName("CPUUtilization");
		try {
			statReq.setStartTime(DateFormat.getDateInstance(DateFormat.SHORT).parse("3/18/2013"));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		statReq.setEndTime(new Date());
		statReq.setPeriod(600);
		
		ArrayList<String> statistics = new ArrayList<String>();
		statistics.add("Average");
		statReq.setStatistics(statistics);
		statReq.setNamespace("AWS/EC2");
		statReq.setUnit(StandardUnit.Percent);
		
		ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
		dimensions.add(new Dimension().withName("InstanceId").withValue("i-fe6c878e"));
		statReq.setDimensions(dimensions);
		
		GetMetricStatisticsResult statRes = cloudClient.getMetricStatistics(statReq); 
		List<Datapoint> datapoints = statRes.getDatapoints();
		for(Datapoint point : datapoints)
		{
			System.out.println(point.getTimestamp().toString());
		}
	}
	
	public static void main(String[] args)
	{
		AWSCredentials cred = Utils.getCredentials("awsAccess.properties");
		EC2Manager ec2 = new EC2Manager(cred);
		//ec2.createSecurityGroup("");
		//ec2.getSpotInstances();
		//ec2.createSpotInstances();
		//ec2.tagMyInstances();
		
		//ec2.setHosts();
		//ec2.distributeDirectory("test/");
		//ec2.createLoadBalancers();
		
		//ec2.cloudMetrics();
		
		Utils.loadXMLConfiguration("I:/RUBBOS-221.xml");
	}

}
