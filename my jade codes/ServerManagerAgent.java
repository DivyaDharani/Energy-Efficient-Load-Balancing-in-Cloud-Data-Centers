import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.wrapper.*;
import javax.swing.*;
import java.util.*;
import java.io.*;

public class ServerManagerAgent extends Agent
{
	int ID, num_of_vms, total_cpu, total_mem;
	double cpu_load_threshold, mem_load_threshold;
	int cpu_load, mem_load, cpu_load_activation_threshold, mem_load_activation_threshold, cpu_load_activation_count, mem_load_activation_count;
	double cpu_load_percentage, mem_load_percentage, cpu_load_threshold_percentage, mem_load_threshold_percentagek,; 
	VirtualMachine[] vm; 
	public void setup()
	{
		setEnabledO2ACommunication(true,0);
		Object[] args = getArguments();
		ID = (Integer)args[0];
		num_of_vms = (Integer)args[1];
		total_cpu = (Integer)args[2];
		total_mem = (Integer)args[3];
		vm = new VirtualMachine[num_of_vms];
		// System.out.println(getLocalName()+" with ID "+ID+" is started.(No. of vms => "+num_of_vms+")");
		addBehaviour(new RequestGetter());
		addBehaviour(new VMInstanceGathering());
		addBehaviour(new ThresholdSetUp());
		addBehaviour(new ThresholdMonitoring());
 	}

 	public void calculateLoad()
 	{
 		for(int i = 0; i < num_of_vms; i++)
 		{
 			cpu_load = vm[i].cpu_occupied; //total load of all VMs
 			mem_load = vm[i].mem_occupied;
 			cpu_load_percentage = (cpu_load / total_cpu) * 100;
 			mem_load_percentage = (mem_load / total_mem) * 100;
 		}
 	}

	class RequestGetter extends CyclicBehaviour
 	{
 		MessageTemplate msgtemplate;
 		String[] strarr;
 		public void action()
 		{
 			msgtemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),MessageTemplate.MatchOntology("requesting-for-capacity"));
 			ACLMessage msg = receive(msgtemplate);
 			if(msg!=null)
 			{
 				System.out.println("Requesting for capacity message has been received to sma "+ID);
 				
 				//requesting VMs
 				ACLMessage msg2vma = new ACLMessage(ACLMessage.REQUEST);
 				msg2vma.setOntology(msg.getOntology());
 				for(int i=1;i<=num_of_vms;i++)
 					msg2vma.addReceiver(new AID("vma_"+ID+"_"+i,AID.ISLOCALNAME));
 				send(msg2vma);
 				System.out.println("requesting-for-capacity message -> from SMA"+ID+" to all its VMs");

 				//receiving replies from all the VMAs
 				/*int count = 0;
 				msgtemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchOntology("requesting-for-capacity"));
 				strarr = new String[num_of_vms];
 				while(count < num_of_vms)
 				{
 					//waiting for a message from vma
 						while((msg = receive(msgtemplate)) == null)
 						{
 							;
 						}
 						strarr[count] = msg.getContent();
 						count++;
 				}*/	
 				int count = 0;
 				ArrayList<VirtualMachine> vmarray = new ArrayList<VirtualMachine>();
 				Object obj;
 				while(count < num_of_vms)
 				{
 					while((obj = getO2AObject()) == null)
 						;
 					if(obj.getClass().getSimpleName().equals("VirtualMachine"))
 					{
 						vmarray.add((VirtualMachine)obj);
 						count++;
 					}
 				}

 				//replying to ClusteringAgent
 				try
 				{
					jade.wrapper.AgentContainer container = getContainerController(); 
					AgentController agentcont = container.getAgent("ca"); //ContainerController's method - getAgent()
					//agentcont.putO2AObject(strarr,false);
					agentcont.putO2AObject(vmarray,false);
					System.out.println("Capacity info - sent by "+getLocalName()+" to CA");
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
 				/*ACLMessage reply = msg.createReply();
 				reply.setPerformative(ACLMessage.INFORM);
 				// reply.setOntology("requesting-for-capacity");
 				reply.setContent();*/
 			}
 		}
 	}

 	class VMInstanceGathering extends OneShotBehaviour
 	{
 		Object obj = null;
 		public void action()
 		{
 			for(int i = 0; i < num_of_vms; i++)
 			{
 				while((obj = getO2AObject()) == null)
 					;
 				if(obj.getClass().getSimpleName().equals("VirtualMachine"))
 				{
 					vm[i] = (VirtualMachine)obj;
 					System.out.println("Server "+ID+" => VM instance "+(i+1)+" received");
 				}
 			} 			
 		}
 	}

 	class ThresholdSetUp extends OneShotBehaviour
 	{
 		public void action()
 		{
 			//threshold specific to the capacity of this server
 			cpu_load_threshold = 0.75 * total_cpu;
 			mem_load_threshold = 0.75 * total_mem;
 			//threshold based on usage percentage => 75%
 			cpu_load_threshold_percentage = 75;
 			mem_load_threshold_percentage = 75;

 			cpu_load_activation_threshold = 3;
 			mem_load_activation_threshold = 3;

  		}
 	}

 	class ThresholdMonitoring extends CyclicBehaviour 
 	{
 		public void action()
 		{
 			calculateLoad();
 			if(cpu_load_percentage > cpu_load_threshold_percentage)
 				cpu_load_activation_count ++;
 			if(mem_load_percentage > mem_load_threshold_percentage)
 				mem_load_activation_count ++;

 			if((cpu_load_activation_count > cpu_load_activation_threshold) || (mem_load_activation_count > mem_load_activation_threshold))
 			{
 				//trigger migration ----------------------------------------------------
 				//----------------------------------------------------------------------
 				JOptionPane.showMessageDialog(null, "Migration - to be triggered for Server "+ID+" !!");
 				if(cpu_load_activation_threshold > cpu_load_activation_threshold)
 					cpu_load_activation_count = 0;
 				if(mem_load_activation_count > mem_load_activation_threshold)
 					mem_load_activation_count = 0;
 			}
 		}
 	}
}