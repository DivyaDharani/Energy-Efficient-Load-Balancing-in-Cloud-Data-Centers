import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.wrapper.*;
import javax.swing.*;
public class VirtualMachineAgent extends Agent
{
	// VirtualMachine[] vms = new VirtualMachine[12];

	public int ID,localID,serverID;
	public String vma_name;
	public int cpu_capacity,mem_capacity;
	public int status = 0; //0 for free; 1 for busy
	public VirtualMachine vminstance;
	public JTextArea logTextArea;
	public void setup()
	{
		setEnabledO2ACommunication(true,0);
		vma_name = getLocalName();
		Object[] args = getArguments();
		if(args!=null)
		{
			ID = (Integer)args[0]; 
			localID = (Integer)args[1]; //local to the server machine
			serverID = (Integer)args[2];
			cpu_capacity = (Integer)args[3];
			mem_capacity = (Integer)args[4];
			logTextArea = (JTextArea)args[5];			
		}
		// JOptionPane.showMessageDialog(null,getLocalName()+" at "+getAID()+" started");
		// System.out.println(getLocalName()+" with ID "+ID+" in Server "+serverID+" is started with mem_capacity : "+mem_capacity+" cpu_capacity : "+cpu_capacity);
		
		vminstance = new VirtualMachine(localID,serverID,getLocalName(),cpu_capacity,mem_capacity, logTextArea);

		//sending vm instance to this VMA's host's SMA
		try
		{
			ContainerController container_controller = getContainerController();
			AgentController agent_controller = container_controller.getAgent("sma"+serverID);
			agent_controller.putO2AObject(vminstance, false);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		addBehaviour(new RequestGetter());
		addBehaviour(new Migration());
	}

	class RequestGetter extends CyclicBehaviour
	{
		MessageTemplate msgtemplate;
		public void action()
		{
			msgtemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			msgtemplate = MessageTemplate.and(msgtemplate,MessageTemplate.MatchOntology("requesting-for-capacity"));
			ACLMessage msg = receive(msgtemplate);
			if(msg!=null)
			{
				System.out.println(msg.getOntology()+" received to "+getLocalName());
				/*ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent(cpu_capacity+" "+mem_capacity);
				send(reply);*/
				try
				{
					jade.wrapper.AgentContainer container = getContainerController();
					AgentController agentController = container.getAgent(msg.getSender().getLocalName());
					agentController.putO2AObject(vminstance,false);
					System.out.println("Capacity info - sent by "+getLocalName()+" to "+msg.getSender().getLocalName());
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}	
		}
	}

	class Migration extends CyclicBehaviour
	{
		public void action()
		{
			//monitoring continuously to check if migration has to be started for the corresponding VM
			if(vminstance.startMigration == true)
			{
				//do migration
				try
				{
					int req_id = 0;
					int cpu_capacity = vminstance.cpu_occupied + vminstance.extra_cpu_needed;
					int mem_capacity = vminstance.mem_occupied + vminstance.extra_mem_needed;
					int exec_time = vminstance.exec_time;
					int extra_cpu = 0;
					int extra_mem = 0;
					VMRequest vmrequest = new VMRequest(req_id, cpu_capacity, mem_capacity, exec_time, extra_cpu, extra_mem);
					vmrequest.reply_to = vma_name;
					jade.wrapper.AgentContainer agentContainer = getContainerController();
					AgentController agentController = agentContainer.getAgent("ca");					
					agentController.putO2AObject(vmrequest,false);

					System.out.println("Clustering request sent by "+vma_name+" for selecting server for migration");
					//getting the response from CA
					Object obj;
					while(true)
					{
						while((obj = getO2AObject()) == null)
							;
						if(obj.getClass().getSimpleName().equals("VMCluster"))
						{
							VMCluster vmcluster = (VMCluster)obj;
							System.out.println("Cluster received to "+vma_name+" for migration");
							break; //expected object received
						}
					}
					//process the cluster
					//select server for migration by checking the vms if they are free and by checking if migration would exceed server threshold in which the concerned VM lies  
					
					//do migration

					//after migration
					vminstance.startMigration = false;
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}


 
			} 
		}
	}
} 