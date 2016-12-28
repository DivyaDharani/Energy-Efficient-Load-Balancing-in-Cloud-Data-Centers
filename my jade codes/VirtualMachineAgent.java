import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.wrapper.*;
import javax.swing.*;
public class VirtualMachineAgent extends Agent
{
	// VirtualMachine[] vms = new VirtualMachine[12];

	public int ID,localID,serverID=1;
	public int cpu_capacity,mem_capacity;
	public int status = 0; //0 for free; 1 for busy
	public VirtualMachine vminstance;
	public void setup()
	{
		Object[] args = getArguments();
		if(args!=null)
		{
			ID = (Integer)args[0]; 
			localID = (Integer)args[1]; //local to the server machine
			serverID = (Integer)args[2];
			cpu_capacity = (Integer)args[3];
			mem_capacity = (Integer)args[4];			
		}
		// JOptionPane.showMessageDialog(null,getLocalName()+" at "+getAID()+" started");
		// System.out.println(getLocalName()+" with ID "+ID+" in Server "+serverID+" is started with mem_capacity : "+mem_capacity+" cpu_capacity : "+cpu_capacity);
		
		vminstance = new VirtualMachine(localID,serverID,getLocalName(),cpu_capacity,mem_capacity);
		addBehaviour(new RequestGetter());
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
} 