package elbaEC2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
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
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.RequestSpotInstancesRequest;
import com.amazonaws.services.ec2.model.RequestSpotInstancesResult;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;

import elbaEC2.experiments.Experiment;
/**
@mainpage
See EC2Manager for the relevant documentation.
*/


/**
 * @brief A class for creating and managing EC2 instances used for Elba experiments 
 * @author Gavin Gresham
 *
 */
public class EC2Manager
{
	AWSCredentials credentials;
	AmazonEC2 ec2;
	AmazonElasticLoadBalancingClient elb;
	
	String PEM_KEY = "C:\\Users\\Gdeter\\Documents\\ggkey.pem";
	String AMI_NAME = "ami-f8128a91";
	
	/**
	 * @brief Instantiates an EC2Manager object
	 * @param credentials The AWS credentials that will be used for all AWS calls this class makes
	 */
	public EC2Manager(AWSCredentials credentials)
	{
		this.credentials = credentials;
		ec2 = new AmazonEC2Client(credentials);
		elb = new AmazonElasticLoadBalancingClient(credentials);
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
	
	/**
	 * @brief Creates a number of spot instances based on the size of the nodeNames list
	 * 
	 * Requests spot instance requests for N instances, where N is <b>nodeNames.size()</b>.
	 * These instances are assigned AWS tags for their node names, and experiment name for 
	 * later filtering. 
	 * 
	 * @param experimentName The name of the experiment
	 * @param maxPrice A string containing the maximum value to pay for an instance hour. Ex: ".015"
	 * @param nodeNames A list of the names for each node.
	 * 
	 * @remark This call will block until all nodes are acquired.
	 */
	public void createSpotInstances(String experimentName, String maxPrice, List<String> nodeNames)
	{
		//Create the request
		RequestSpotInstancesRequest spotReq = new RequestSpotInstancesRequest();
		spotReq.setSpotPrice(maxPrice);
		spotReq.setInstanceCount(nodeNames.size());
		spotReq.setLaunchGroup(experimentName);
		
		//Add the security group
		ArrayList<String> securityGroups = new ArrayList<String>();
		securityGroups.add("elba");
		
		//Describe the type of instance to launch
		LaunchSpecification launchSpecification = new LaunchSpecification();
		launchSpecification.setSecurityGroups(securityGroups);
		launchSpecification.setImageId(AMI_NAME);
		launchSpecification.setInstanceType(InstanceType.M1Small);
		spotReq.setLaunchSpecification(launchSpecification);
		
		//Launch the instances
		RequestSpotInstancesResult reqResult = ec2.requestSpotInstances(spotReq);
		
		List<SpotInstanceRequest> spotInstanceRequest = reqResult.getSpotInstanceRequests();
		
		//Store the ids of the spot instances
		ArrayList<String> spotIds = new ArrayList<String>();
		
		for(SpotInstanceRequest request : spotInstanceRequest)
			spotIds.add(request.getSpotInstanceRequestId());
		
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
					
					//Skip if not one of ours
					if(!spotIds.contains(req.getSpotInstanceRequestId()))
						continue;
					
					//See if this isn't one of our spot instances
					if(req.getLaunchGroup() == null || !req.getLaunchGroup().equals(experimentName))
							continue;
					
					if(req.getState().equals("open") || (req.getState().equals("active") && !req.getStatus().getCode().equals("fulfilled")))
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
		
		//Ask for the results again
		DescribeSpotInstanceRequestsRequest spotRequest = new DescribeSpotInstanceRequestsRequest();
		spotRequest.setSpotInstanceRequestIds(spotIds);
		DescribeSpotInstanceRequestsResult spotInstanceRequests = ec2.describeSpotInstanceRequests(spotRequest);
		List<SpotInstanceRequest> instances = spotInstanceRequests.getSpotInstanceRequests();
		
		for(int i = 0; i < instances.size(); i++)
		{
			//Tag the nodes
			ArrayList<Tag> tags = new ArrayList<Tag>();
			ArrayList<String> resources = new ArrayList<String>();
			resources.add(instances.get(i).getInstanceId());
			
			tags.add(new Tag("ExperimentName", experimentName));
			tags.add(new Tag("NodeName", nodeNames.get(i)));
			tags.add(new Tag("Name", nodeNames.get(i)));
			CreateTagsRequest tagsReq = new CreateTagsRequest();
			tagsReq.setTags(tags);
			tagsReq.setResources(resources);
			
			ec2.createTags(tagsReq);
		}
		
	}
	
	/**
	 * @brief Retrieves a list of instances that exist in a given experiment
	 * @param experimentName The name of the experiment
	 * @return A list of EC2 Instances
	 */
	public ArrayList<Instance> getSpotInstances(String experimentName)
	{
		//Filter for just this experiment
		DescribeInstancesRequest describeReq = new DescribeInstancesRequest();
		ArrayList<String> experiments = new ArrayList<String>();
		experiments.add(experimentName);
		ArrayList<Filter> filters = new ArrayList<Filter>();
		filters.add(new Filter("tag:ExperimentName", experiments));
		
		DescribeInstancesResult res = ec2.describeInstances(describeReq);
		
		ArrayList<Instance> instances = new ArrayList<Instance>();
		for(Reservation rsv : res.getReservations())
		{
			for(Instance inst : rsv.getInstances())
			{
				instances.add(inst);
			}
		}
		
		return instances;
	}
	
	/**
	 * @brief Tags running instances with the given experiment name and node names
	 * 
	 * @param experimentName The name of the experiment
	 * @param nodeNames A list of names that instances will be tagged with
	 * @deprecated \see createSpotInstances will now take care of this
	 * 
	 * @remark Not usable with multiple running experiments
	 */
	public void tagMyInstances(String experimentName, List<String> nodeNames)
	{
		DescribeInstancesResult instRes = ec2.describeInstances();
		
		ArrayList<String> ids = new ArrayList<String>();
		List<Reservation> resv = instRes.getReservations();
		
		for(Reservation rs : resv)
		{
			for(Instance inst : rs.getInstances())
			{
				if(inst.getImageId().equals(AMI_NAME) && inst.getState().getName().equals("running"))
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
			
			tags.add(new Tag("ExperimentName", experimentName));
			tags.add(new Tag("NodeName", nodeNames.get(i - 1)));
			tags.add(new Tag("Name", nodeNames.get(i - 1)));
			CreateTagsRequest tagsReq = new CreateTagsRequest();
			tagsReq.setTags(tags);
			tagsReq.setResources(resources);
			
			ec2.createTags(tagsReq);
		}
		
	}
	
	/**
	 * @brief Sets the /etc/hosts file to map node names to private IPs
	 * @param experimentName The experiment name
	 */
	public void setHosts(String experimentName)
	{
		File keyfile = new File("C:\\Users\\Gdeter\\Documents\\ggkey.pem");
	
		//Find only instances for a given experiment 
		DescribeInstancesRequest instReq = new DescribeInstancesRequest();
		ArrayList<Filter> filters = new ArrayList<Filter>();
		ArrayList<String> filterValues = new ArrayList<String>();
		filterValues.add(experimentName);
		
		Filter experimentNameFilter = new Filter("tag:ExperimentName", filterValues);
		filters.add(experimentNameFilter);
		
		instReq.setFilters(filters);
		
		DescribeInstancesResult instRes = ec2.describeInstances(instReq);
		
		ArrayList<String> addrs = new ArrayList<String>();
		List<Reservation> resv = instRes.getReservations();
		HashMap<String, String> nodeAddrMap = new HashMap<String, String>();
		
		//Map the tags
		for(Reservation rs : resv)
		{
			for(Instance inst : rs.getInstances())
			{
				//TODO: Remove AMI filter?
				if(inst.getImageId().equals(AMI_NAME) && inst.getState().getName().equals("running"))
				{
					String addr = inst.getPublicDnsName();
					List<Tag> tags = inst.getTags();
					
					for(Tag tag : tags)
					{
						if(tag.getKey().equals("NodeName"))
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
				command += "' | sudo tee /etc/hosts;";
				command += "echo -e \"Host *\\n\\tStrictHostKeyChecking no\" > .ssh/config;";
				command += "chmod 700 .ssh/config;";
				command += "echo '' > .ssh/known_hosts";
					
				sess.execCommand(command);
				
				InputStream stdout = new StreamGobbler(sess.getStdout());

				BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

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
	
	/**
	 * @brief Takes a directory, relative to /home/ec2-user/ and copies it to all other
	 * nodes in the experiment
	 *  
	 * @param dir The directory to copy
	 */
	public void distributeDirectory(String dir, String controlNode)
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
				if(inst.getImageId().equals(AMI_NAME) && inst.getState().getName().equals("running"))
				{
					String addr = inst.getPublicDnsName();
					List<Tag> tags = inst.getTags();
					
					for(Tag tag : tags)
					{
						if(tag.getKey().equals("NodeName"))
						{
							//Add this key to the mapping
							nodeAddrMap.put(tag.getValue(), inst.getPrivateIpAddress());
							addrs.add(addr);
							if(tag.getValue().equals(controlNode))
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
	
	/**
	 * @brief Creates a load balancer that redirects to a specific port
	 * 
	 * @param balancerName The name of the new load balancer
	 * @param experimentName The experiment name
	 * @param port The port to forward trafficon
	 */
	public void createLoadBalancer(String balancerName, String experimentName, int port)
	{		
		String fullName = experimentName + "-" + balancerName;
		
		//Configure the balancer
		CreateLoadBalancerRequest balReq = new CreateLoadBalancerRequest();
		balReq.setLoadBalancerName(fullName);
		
		ArrayList<Listener> listeners = new ArrayList<Listener>();
		listeners.add(new Listener("tcp", port, port));
		balReq.setListeners(listeners);
		
		DescribeAvailabilityZonesResult availZones = ec2.describeAvailabilityZones();
		List<AvailabilityZone> zones = availZones.getAvailabilityZones();
		ArrayList<String> zonesStr = new ArrayList<String>();
		for(AvailabilityZone zone : zones)
			zonesStr.add(zone.getZoneName());
		balReq.setAvailabilityZones(zonesStr);
		
		//Configure the health check
		ConfigureHealthCheckRequest configureHealthCheckRequest = new ConfigureHealthCheckRequest();
		configureHealthCheckRequest.setLoadBalancerName(fullName);
		
		HealthCheck healthCheck = new HealthCheck();
		healthCheck.setHealthyThreshold(2);
		healthCheck.setUnhealthyThreshold(2);
		healthCheck.setInterval(30);
		healthCheck.setTarget("TCP:" + port);
		healthCheck.setTimeout(15);
		
		configureHealthCheckRequest.setHealthCheck(healthCheck);
		
		//Create the balancer
		CreateLoadBalancerResult balRes = elb.createLoadBalancer(balReq);
		
		elb.configureHealthCheck(configureHealthCheckRequest);
	}
	
	/**
	 * @brief Adds nodes from the experiment as destinations for the load balancer
	 * 
	 * @param balancerName The load balancer name
	 * @param experimentName The experiment name
	 * @param nodeNames A list of the names of nodes to add
	 */
	public void addNodesToLoadBalancer(String balancerName, String experimentName, List<String> nodeNames)
	{
		String fullName = experimentName + "-" + balancerName;
		
		//Get the instances associated with the name
		DescribeInstancesRequest instReq = new DescribeInstancesRequest();
		Filter nameFilter = new Filter("tag:NodeName", nodeNames);
		
		ArrayList<String> experimentNameList = new ArrayList<String>();
		experimentNameList.add(experimentName);
		Filter experimentFilter = new Filter("tag:ExperimentName", experimentNameList);
		
		ArrayList<Filter> filters = new ArrayList<Filter>();
		filters.add(nameFilter);
		filters.add(experimentFilter);
		instReq.setFilters(filters);
		
		DescribeInstancesResult instResults = ec2.describeInstances(instReq);
		List<Reservation> reservations = instResults.getReservations();
		
		//Add all the retrieved instances to a list
		ArrayList<String> instanceIds = new  ArrayList<String>();
		for(Reservation resv : reservations)
		{
			for(Instance  inst : resv.getInstances())
			{
				instanceIds.add(inst.getInstanceId());
			}
		}
		
		//Creates an ELB instances from the EC2 IDs
		ArrayList<com.amazonaws.services.elasticloadbalancing.model.Instance> instances = 
				new ArrayList<com.amazonaws.services.elasticloadbalancing.model.Instance>();
		for(String instanceId : instanceIds)
			instances.add(new com.amazonaws.services.elasticloadbalancing.model.Instance(instanceId));
		
		RegisterInstancesWithLoadBalancerRequest registerInstancesRequest = new RegisterInstancesWithLoadBalancerRequest();
		registerInstancesRequest.setLoadBalancerName(fullName);
		registerInstancesRequest.setInstances(instances);
		
		elb.registerInstancesWithLoadBalancer(registerInstancesRequest);
	}
	
	/**
	 * @brief Removes all nodes from the load balancer
	 * 
	 * @param balancerName The name of the load balancer
	 * @param experimentName The name of the experiment
	 */
	public void clearLoadBalancer(String balancerName, String experimentName)
	{
		String fullName = experimentName + "-" + balancerName;
		
		//Get the current load balancers
		DescribeLoadBalancersResult loadBalancerRes = elb.describeLoadBalancers();
		List<LoadBalancerDescription> descriptions = loadBalancerRes.getLoadBalancerDescriptions();
		
		for(LoadBalancerDescription description : descriptions)
		{
			//Find the correct balancer
			if(description.getLoadBalancerName().equals(fullName))
			{
				//This load balancer is already empty
				if(description.getInstances() == null || description.getInstances().size() == 0)
					return;
				
				//Clear all instances from the ELB
				DeregisterInstancesFromLoadBalancerRequest deregisterReq =
						new DeregisterInstancesFromLoadBalancerRequest(fullName, description.getInstances());
				
				elb.deregisterInstancesFromLoadBalancer(deregisterReq);
			}
				
		}
		
	}
	
	/**
	 * @brief Gets the public DNS of a node
	 * 
	 * @param nodeName The name of the node
	 * @param experimentName The name of the experiment
	 * @return The DNS of the node
	 */
	private String getNodeDNS(String nodeName, String experimentName)
	{
	/**
	 * @brief Get the public DNS of a specific node
	 * 
	 * @param nodeName The name of the node
	 * @param experimentName
	 * @return
	 */
		//Find the specified node
		DescribeInstancesRequest describeReq = new DescribeInstancesRequest();
		Filter nameFilter = new Filter("tag:Name").withValues(nodeName);
		Filter experFilter = new Filter("tag:ExperimentName").withValues(experimentName);
		Filter stateFilter = new Filter("instance-state-name").withValues("running");
		ArrayList<Filter> filters = new ArrayList<Filter>();
		filters.add(nameFilter);
		filters.add(experFilter);
		filters.add(stateFilter);
		describeReq.setFilters(filters);
		
		DescribeInstancesResult describeRes = ec2.describeInstances(describeReq);
		
		//There should be only one instance if everything went right
		String hostname = null;
		for(Reservation resv : describeRes.getReservations())
		{
			for(Instance inst : resv.getInstances())
			{
				hostname = inst.getPublicDnsName();
			}
		}
		
		return hostname;
	}
	
	/**
	 * @brief Copies a file from the local file system to a node
	 * 
	 * @param file The location of the local file
	 * @param nodeName The name of the node
	 * @param experimentName The name of the experiment
	 */
	public void copyFileToNode(String file, String nodeName, String experimentName)
	{
		String hostname = getNodeDNS(nodeName, experimentName);
		
		//No node found
		if(hostname == null)
			return;
		//Upload the file
		try {
			Connection conn = new Connection(hostname);
			conn.connect();
			conn.authenticateWithPublicKey("ec2-user", new File(PEM_KEY), "");
			SCPClient scpClient = new SCPClient(conn);
			scpClient.put(file, "~/");
			
			conn.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * @brief Executes a shell command on a node
	 * 
	 * @param nodeName The name of the remote node
	 * @param experimentName The name of the experiment
	 * @param command The command to run
	 * @return The output of the command
	 * @remark This method blocks until the command has finished
	 * @remark Commands can be batched together using semicolons
	 */
	public String runRemoteCommand(String nodeName, String experimentName, String command)
	{
		String retn = "";
		String hostname = getNodeDNS(nodeName, experimentName);
		if(hostname == null)
			return retn;
		
		try
		{
			Connection conn = new Connection(hostname);
			conn.connect();
			conn.authenticateWithPublicKey("ec2-user", new File(PEM_KEY), "");

			Session sess = conn.openSession();
			sess.execCommand(command);
			
			//Print the command's output
			InputStream stdout = new StreamGobbler(sess.getStdout());

			BufferedReader br = new BufferedReader(new InputStreamReader(stdout));

			while (true)
			{
				String line = br.readLine();
				if (line == null)
					break;

				retn += line + "\n";
			}
			
			br.close();
			
		} catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return retn;
	}
	
	/**
	 * @brief Find all of the experiments running on nodes
	 * @return An hashmap containing the experiment names as a key, 
	 * and the number of instances as the value. 
	 */
	public HashMap<String, Integer> getRunningExperiments()
	{
		HashMap<String, Integer> count = new HashMap<String, Integer>();
		
		DescribeTagsResult tagResult = ec2.describeTags();
		List<TagDescription> tags = tagResult.getTags();
		
		//Add all unique experiments
		for(TagDescription tag : tags)
		{
			if(tag.getKey().equals("ExperimentName") && !count.containsKey(tag.getValue()))
			{
				count.put(tag.getValue(), 1);
			}
			else if(tag.getKey().equals("ExperimentName"))
			{
				int current = count.get(tag.getValue());
				count.put(tag.getValue(), current + 1);
			}
		}
		
		
		return count;
	}
	
	/**
	 * @brief Gets the public DNS associated with a load balancer
	 * 
	 * @param balancerName The name of the balancer
	 * @param experimentName The name of the experiment
	 * @return The public DNS of the load balancer
	 */
	public String getLoadBalancerDNS(String balancerName, String experimentName)
	{
		
		String fullName = experimentName + "-" + balancerName;
		String dns = null;
		
		DescribeLoadBalancersResult balancers = elb.describeLoadBalancers();
		List<LoadBalancerDescription> descriptions = balancers.getLoadBalancerDescriptions();
		for(LoadBalancerDescription description : descriptions)
		{
			if(description.getLoadBalancerName().equals(fullName))
			{
				dns = description.getDNSName();
				break;
			}
		}
		
		return dns;
	}
	
	/**
	 * @brief Shuts down all instances associated with the specified experiment
	 * 
	 * @param experimentName The name of the experiment
	 */
	public void killExperiment(String experimentName)
	{
		//Filter for this experiment only
		DescribeInstancesRequest descReq = new DescribeInstancesRequest();
		ArrayList<String> experiments = new ArrayList<String>();
		experiments.add(experimentName);
		ArrayList<Filter> filters = new ArrayList<Filter>();
		filters.add(new Filter("tag:ExperimentName", experiments));
		descReq.setFilters(filters);
		
		DescribeInstancesResult result = ec2.describeInstances(descReq);
		
		//Get instance IDs
		ArrayList<String> instanceIds = new ArrayList<String>();
		for(Reservation resv : result.getReservations())
		{
			for(Instance inst : resv.getInstances())
			{
				instanceIds.add(inst.getInstanceId());
			}
		}
		
		//Stop the instances
		TerminateInstancesRequest stopReq = new TerminateInstancesRequest(instanceIds);
		ec2.terminateInstances(stopReq);
	}
	
	/**
	 * @brief Launches and configures a series of nodes based on a XML configuration file.
	 * 
	 * @param xmlFile The experiment XML file
	 * @param projectPath The folder where all these following files are located
	 * @param generatedTar The rubbos experiment created from the XML file 
	 * @param rubbosFiles The rubbosFiles tar
	 * @param rubbosHtml The rubbos html files
	 * 
	 * @remark Files should be located at the projectPath, not sub directories
	 */
	public void runExperiment(String xmlFile, String projectPath, String generatedTar,
			String rubbosFiles, String rubbosHtml)
	{
		//Add a trailing slash. Java likes to remove them...
		if(projectPath.charAt(projectPath.length() - 1) != '\\')
			projectPath += "\\";
		
		//Get experiment info
		Experiment exp = Experiment.loadFromXML(projectPath + xmlFile);
		
		String experimentName = exp.getConfigurationFile().name;
		String maxPrice = ".015";
		
		//Load the environmental variables
		String WORK_HOME = exp.getConfigurationFile().getEnv("WORK_HOME");
		String OUTPUT_HOME = exp.getConfigurationFile().getEnv("OUTPUT_HOME");
		String SOFTWARE_HOME = exp.getConfigurationFile().getEnv("SOFTWARE_HOME");
		
		//Find the control node
		String controlNode = "";
		for(Object inst : exp.getConfigurationFile().instances)
		{
			if(inst instanceof elbaEC2.experiments.Instance)
			{
				elbaEC2.experiments.Instance ourInstance =
						(elbaEC2.experiments.Instance)inst;
				if(ourInstance.type.equals("control_server"))
					controlNode = ourInstance.target;
			}
		}
		
		//Read the names from the config file and load them
		ArrayList<String> names = new ArrayList<String>();
		for(Object instance : exp.getConfigurationFile().instances)
		{
			if(instance instanceof elbaEC2.experiments.Instance)
				names.add(((elbaEC2.experiments.Instance)instance).target);
		}
		
		createSpotInstances(experimentName, maxPrice, names);
		//ec2.tagMyInstances(experimentName, names);
		
		//Wait 2 minutes for the instances to boot
		try {
			Thread.sleep(180000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//Set up the AWS environment
		setHosts(experimentName);
		createLoadBalancer("HTTPDbalancer", experimentName, 8000);
		clearLoadBalancer("HTTPDbalancer", experimentName);
		
		createLoadBalancer("SQLbalancer", experimentName, 3313);
		clearLoadBalancer("SQLbalancer", experimentName);
		
		//Add nodes to the load balancer
		ArrayList<String> apacheNodes = new ArrayList<String>();
		apacheNodes.add("node7");
		//apacheNodes.add("node10");
		addNodesToLoadBalancer("HTTPDbalancer", experimentName, apacheNodes);
		
		ArrayList<String> sqlNodes = new ArrayList<String>();
		sqlNodes.add("node9");
		//sqlNodes.add("node12");
		addNodesToLoadBalancer("SQLbalancer", experimentName, sqlNodes);
		
		//Copy the needed rubbos files to the control server
		copyFileToNode(projectPath + generatedTar, controlNode, experimentName);
		copyFileToNode(projectPath + rubbosFiles, controlNode, experimentName);
		copyFileToNode(projectPath + rubbosHtml, controlNode, experimentName);
		String output = runRemoteCommand(controlNode, experimentName, "tar xvf " + generatedTar + ";mkdir " + OUTPUT_HOME + " -p");
		
		//Get the folder's name
		int idx = output.indexOf('/');
		String folder = output.substring(0, idx);
		
		//Get the load balancers' public DNS
		String mainSQLNode = "node9";
		String loadBalancer = getLoadBalancerDNS("HTTPDbalancer", experimentName);
		String sqlBalancer = getLoadBalancerDNS("SQLbalancer", experimentName);
		
		String command = "mv -f " + folder + "/* " + OUTPUT_HOME + "; rmdir " + folder + ";" +
				"mv " + rubbosFiles + " " + WORK_HOME + "; mkdir " + WORK_HOME + "/apache_files -p; mv " +
				rubbosHtml + " " + WORK_HOME + "/apache_files;" + //Move the rubbos files
				"cd " + WORK_HOME + "; tar xvf " + rubbosFiles + "; " +
				//"rm " + rubbosFile + "; " + //Remove the old rubbos_files tar
				"cp " + OUTPUT_HOME + "*_conf . -R;" + //Copy the config directories to $WORKING_HOME
				" cd apache_files; tar xvf " + rubbosHtml + "; rm " + rubbosHtml + ";" +
				"cd " + OUTPUT_HOME + "; ";
		if(loadBalancer != null)
			command += "sed -i 's/^httpd_hostname.*$/httpd_hostname = " + loadBalancer + "/g' rubbos_conf/*; ";
		if(sqlBalancer != null)
			command += "sed -i 's/" + mainSQLNode + "/" + sqlBalancer + "/g' rubbos_conf/mysql.properties; ";
		command += "cd scripts; rm CONTROL_emu*; sed -i 's/sleep 15$/sleep 150/g' CONTROL_rubbos*;";
				
		runRemoteCommand(controlNode, experimentName, command);
		
		distributeDirectory("test/", controlNode);
		
		//runRemoteCommand(controlNode, experimentName, "nohup bash -c /home/ec2-user/test/rubbosMulini6/output/scripts/run.sh &> logFile 0</dev/null &");

	}
	
	public static void main(String[] args)
	{
		if(args.length < 3)
		{
			System.err.println("Program <XML File> <Rubbos Tar> <rubbos Files> <rubbos html>");
			return;
		}
		
		String rootDir = args[0];
		String xmlFile = args[1];
		String rubbosTar = args[2];
		String rubbosFile = args[3];
		String rubbosHtml = args[4];
		
		AWSCredentials cred = Utils.getCredentials("awsAccess.properties");
		
		EC2Manager ec2 = new EC2Manager(cred);
		ec2.runExperiment(xmlFile, rootDir, rubbosTar, rubbosFile, rubbosHtml);
		
	}

}
