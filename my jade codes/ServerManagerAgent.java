import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.wrapper.*;
import javax.swing.*;
import java.util.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ServerManagerAgent extends Agent
{
	int ID, num_of_vms, total_cpu, total_mem;
	double cpu_load_threshold, mem_load_threshold, cpu_energy_threshold, mem_energy_threshold;
	int cpu_load, mem_load; 
	double cpu_load_percentage, mem_load_percentage, cpu_load_threshold_percentage, mem_load_threshold_percentage;
	double cpu_energy_threshold_percentage, mem_energy_threshold_percentage; 
	VirtualMachine[] vms; 
	ServerMachine serverMachine;
	JTextArea logTextArea;

	public void setup()
	{
		setEnabledO2ACommunication(true,0);
		Object[] args = getArguments();
		ID = (Integer)args[0];
		num_of_vms = (Integer)args[1];
		total_cpu = (Integer)args[2];
		total_mem = (Integer)args[3];
		logTextArea = (JTextArea)args[4];
		serverMachine = (ServerMachine)args[5];
		vms = new VirtualMachine[num_of_vms];
		// System.out.println(getLocalName()+" with ID "+ID+" is started.(No. of vms => "+num_of_vms+")");
		addBehaviour(new RequestGetter());
		addBehaviour(new TriggerThresholdMonitoring());
		addBehaviour(new ServerMachineProvider());
		addBehaviour(new ServerConsolidator());
 	}

 	public void calculateLoad()
 	{
 		cpu_load = 0;
 		mem_load = 0;
 		serverMachine.busy_vm_count = 0;
 		for(int i = 0; i < num_of_vms; i++)
 		{
 			if(vms[i].status == VirtualMachine.BUSY)
 			{
 				cpu_load += (vms[i].core_usage_percentage / 100.0 ) * vms[i].cpu_occupied; //total load of all VMs
 				mem_load += vms[i].mem_occupied;
 				serverMachine.busy_vm_count ++;
 			}
 		}
 		cpu_load_percentage = ((1.0 * cpu_load) / total_cpu) * 100;
 		mem_load_percentage = ((1.0 * mem_load) / total_mem) * 100;

		serverMachine.cpu_load = cpu_load;
		serverMachine.mem_load = mem_load;
		serverMachine.cpu_load_percentage = cpu_load_percentage;
		serverMachine.mem_load_percentage = mem_load_percentage;
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
 				VirtualMachine vm;
 				int[] flag = new int[num_of_vms + 1]; //to check if a particular VMA sent VM instance or not
 				for(int i = 1; i <= num_of_vms; i++)
 					flag[i] = 0; //this means ith VMA has not sent any response yet
 				while(count < num_of_vms)
 				{
 					while((obj = getO2AObject()) == null)
 						;
 					if(obj.getClass().getSimpleName().equals("VirtualMachine"))
 					{
 						vm = (VirtualMachine)obj;
 						if(flag[vm.local_id] == 0) //if this VM instance (vm) is not received previously
 						{
	 						vmarray.add((VirtualMachine)obj);
 							flag[vm.local_id] = 1;
 							vms[count] = vm;
 							count++;
 						}
 						else 
 						{
 							System.out.println("---Prevented duplicate receipt of instance of "+vm.vma_name);
 						}
 					}
 				}

 				//replying to ClusteringAgent
 				try
 				{
					jade.wrapper.AgentContainer container = getContainerController(); 
					AgentController agentcont = container.getAgent("ca"); //ContainerController's method - getAgent()
					agentcont.putO2AObject(vmarray,false);
					System.out.println("Capacity info - sent by "+getLocalName()+" to CA");
					agentcont = container.getAgent("migrationCounter");
					agentcont.putO2AObject(vmarray,false);
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

 	class TriggerThresholdMonitoring extends CyclicBehaviour
 	{
 		public void action()
 		{
			MessageTemplate msgtemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),MessageTemplate.MatchOntology("start-threshold-monitoring"));
			ACLMessage msg = receive(msgtemplate);
			if(msg != null)
			{
				System.out.println("Start-threshold-monitoring message received to sma"+ID);
				addBehaviour(new ThresholdSetUp());
				addBehaviour(new ThresholdMonitoring());
				removeBehaviour(this);
	 		} 			
 		}
 	}

 	class ThresholdSetUp extends OneShotBehaviour
 	{
 		public void action()
 		{
 			//load related thresholds
 			//threshold specific to the capacity of this server
 			serverMachine.cpu_load_threshold = cpu_load_threshold = 0.75 * total_cpu;
 			serverMachine.mem_load_threshold = mem_load_threshold = 0.70 * total_mem;
 			//threshold based on usage percentage => 75%
 			serverMachine.cpu_load_threshold_percentage = cpu_load_threshold_percentage = 75;
 			serverMachine.mem_load_threshold_percentage = mem_load_threshold_percentage = 70;

 			//energy related thresholds
 			serverMachine.cpu_energy_threshold = cpu_energy_threshold = 0.25 * total_cpu;
 			serverMachine.mem_energy_threshold = mem_energy_threshold = 0.25 * total_mem;

 			serverMachine.cpu_energy_threshold_percentage = cpu_energy_threshold_percentage = 25;
 			serverMachine.mem_energy_threshold_percentage = mem_energy_threshold_percentage = 25;
  		}
 	}

 	class ThresholdMonitoring extends CyclicBehaviour 
 	{
 		public void action()
 		{
 			if(serverMachine.migration_triggered == true) //already if the VM to be migrated is found for the current load, skip the below process
 				return;
 			calculateLoad();

 			if(cpu_load_percentage == 0 && mem_load_percentage == 0)
 			{
 				serverMachine.status = ServerMachine.NOT_UTILIZED;
 			}
 			else if(cpu_load_percentage < cpu_energy_threshold_percentage && mem_load_percentage < mem_energy_threshold_percentage)
 			{
 				serverMachine.status = ServerMachine.UNDER_UTILIZED;
 			}
 			else if((cpu_load_percentage > cpu_load_threshold_percentage || mem_load_percentage > mem_load_threshold_percentage) && !serverMachine.vm_selection.equals("mig_time_method"))
 			{
 				serverMachine.status = ServerMachine.OVER_UTILIZED;
 				//trigger migration 
 				logTextArea.append("\n\n"+new Date()+" => MIGRATION TO BE TRIGGERED FOR SERVER "+ID+" (CPU load % = "+cpu_load_percentage+", Mem load % = "+mem_load_percentage+")\n");
 				
 				//choosing VM for migration
 				VirtualMachine[] vm_temp = new VirtualMachine[num_of_vms];
 				int busy_vm_count = 0, i, j;
 				VirtualMachine selected_vm = null;
 				//considering only those VMs that are engaged with a job
 				for(i = 0; i < num_of_vms; i++)
 				{
 					if(vms[i].status == VirtualMachine.BUSY)
 						vm_temp[busy_vm_count++] = vms[i]; 
 				}
 				VirtualMachine temp;
 				for(i = 0; i < busy_vm_count; i++)
 				{
 					//usage with respect to the VM's server's usage
 					vm_temp[i].cpu_usage = (((vm_temp[i].core_usage_percentage / 100.0) * vm_temp[i].cpu_occupied) / (cpu_load * 1.0)) * 100.0;
 					vm_temp[i].mem_usage = (vm_temp[i].mem_occupied / (mem_load * 1.0)) * 100.0;
 				}
 				//sorting acc. to cpu usage
 				for(i = 0; i < busy_vm_count - 1; i++)
 				{
 					for(j = i; j < busy_vm_count; j++)
 					{
 						if(vm_temp[i].cpu_usage > vm_temp[j].cpu_usage)
 						{
 							temp = vm_temp[i];
 							vm_temp[i] = vm_temp[j];
 							vm_temp[j] = temp;
 						}
 					}
 				}
 				if(serverMachine.vm_selection.equals("cpu_method"))
 				{
 					selected_vm = vm_temp[busy_vm_count-1]; //last VM in the sorted list (highest CPU usage value)
 				}
 				else
 				{
	 				//assigning cpu weights
	 				for(i = 0; i < busy_vm_count; i++)
	 				{
	 					vm_temp[i].cpu_weight = i+1;
	 				}
	 				//sorting acc. to memory usage
	 				for(i = 0; i < busy_vm_count - 1; i++)
	 				{
	 					for(j = i; j < busy_vm_count; j++)
	 					{
	 						if(vm_temp[i].mem_usage > vm_temp[j].mem_usage)
	 						{
	 							temp = vm_temp[i];
	 							vm_temp[i] = vm_temp[j];
	 							vm_temp[j] = temp;
	 						}
	 					}
	 				}
	 				if(serverMachine.vm_selection.equals("memory_method"))
	 				{
	 					selected_vm = vm_temp[busy_vm_count - 1]; //last VM in the sorted list (highest mem usage value)
	 				}
	 				else
	 				{
		 				//assigning memory weights
		 				for(i = 0; i < busy_vm_count; i++)
		 				{
		 					vm_temp[i].mem_weight = i+1;
		 					vm_temp[i].total_weight = vm_temp[i].cpu_weight + vm_temp[i].mem_weight;
		 				}
		 				//sorting acc. to total_weight
		 				for(i = 0; i < busy_vm_count - 1; i++)
		 				{
		 					for(j = i; j < busy_vm_count; j++)
		 					{
		 						if(vm_temp[i].total_weight > vm_temp[j].total_weight)
		 						{
		 							temp = vm_temp[i];
		 							vm_temp[i] = vm_temp[j];
		 							vm_temp[j] = temp;
		 						}
		 					}
		 				}
		 				if(serverMachine.vm_selection.equals("combined_cpu_mem_method"))
		 				{
		 					selected_vm = vm_temp[busy_vm_count - 1];
		 				}
		 				else if(serverMachine.vm_selection.equals("middle_vm_method"))
		 				{
		 					//choosing the middle VM in the new order
		 					selected_vm = vm_temp[num_of_vms / 2];
		 				}
		 			}
 				}
 				serverMachine.migration_triggered = true;//to avoid triggering the same computation again and again when the server's threshold is exceeded
 				logTextArea.append("\n\nSelected VM from server "+ID+" for migration => "+selected_vm.vma_name+"\n");
 				selected_vm.startMigration = true;
 				selected_vm.migrationReason = VirtualMachine.SERVER_OVERLOAD;
 				//calculate remaining execution time 
 			}
 			else if((cpu_load_percentage >= cpu_load_threshold_percentage) && serverMachine.vm_selection.equals("mig_time_method") || serverMachine.vm_selection.equals("mig_time_middle_vm_method"))
 			{
 				serverMachine.status = ServerMachine.OVER_UTILIZED;
 				//trigger migration 
 				logTextArea.append("\n\n"+new Date()+" => MIGRATION TO BE TRIGGERED FOR SERVER "+ID+" (CPU load % = "+cpu_load_percentage+", Mem load % = "+mem_load_percentage+")\n");
 				
 				//choosing VM for migration
 				VirtualMachine[] vm_temp = new VirtualMachine[num_of_vms];
 				int busy_vm_count = 0, i, j;
 				VirtualMachine selected_vm = null;
 				//considering only those VMs that are engaged with a job
 				for(i = 0; i < num_of_vms; i++)
 				{
 					if(vms[i].status == VirtualMachine.BUSY)
 						vm_temp[busy_vm_count++] = vms[i]; 
 				}
 				VirtualMachine temp;
 				double mig_time;
 				double[] mig_weight = new double[busy_vm_count];
 				for(i = 0; i < busy_vm_count; i++)
 				{
 					//usage with respect to the VM's server's usage
 					vm_temp[i].cpu_usage = (((vm_temp[i].core_usage_percentage / 100.0) * vm_temp[i].cpu_occupied) / (cpu_load * 1.0)) * 100.0;
 					vm_temp[i].mem_usage = (vm_temp[i].mem_occupied / (mem_load * 1.0)) * 100.0;

 					mig_time = vm_temp[i].mem_usage / vm_temp[i].bandwidth;
 					mig_weight[i] = vm_temp[i].cpu_usage / mig_time;
 				}

 				//sorting acc. to mig_weight
 				for(i = 0; i < busy_vm_count - 1; i++)
 				{
 					for(j = i; j < busy_vm_count; j++)
 					{
 						if(mig_weight[i] > mig_weight[j])
 						{
 							temp = vm_temp[i];
 							vm_temp[i] = vm_temp[j];
 							vm_temp[j] = temp;
 						}
 					}
 				}
 				if(serverMachine.vm_selection.equals("mig_time_method"))
 					selected_vm = vm_temp[busy_vm_count - 1];
 				else if(serverMachine.vm_selection.equals("mig_time_middle_vm_method"))
 					selected_vm = vm_temp[busy_vm_count / 2];
 				serverMachine.migration_triggered = true;//to avoid triggering the same computation again and again when the server's threshold is exceeded
 				logTextArea.append("\n\nSelected VM from server "+ID+" for migration => "+selected_vm.vma_name+"\n");
 				selected_vm.startMigration = true;
 				selected_vm.migrationReason = VirtualMachine.SERVER_OVERLOAD;
 				//calculate remaining execution time 
 			}
 			else
 			{
 				serverMachine.status = ServerMachine.NORMALLY_UTILIZED;
 			}
 		}
 	}

 	public class ServerMachineProvider extends CyclicBehaviour
 	{
 		public void action()
 		{
			MessageTemplate msgtemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),MessageTemplate.MatchOntology("requesting-for-server-machine-instance"));
			ACLMessage msg = receive(msgtemplate);
			if(msg != null)
			{ 		
				try
				{
					jade.wrapper.AgentContainer agentContainer = getContainerController();
					AgentController agentController = agentContainer.getAgent(msg.getSender().getLocalName()); 
					agentController.putO2AObject(serverMachine, false);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
 	}

 	public class ServerConsolidator extends TickerBehaviour
 	{
 		MessageTemplate msgTemplate;
 		ACLMessage msg;
 		int count;
 		public ServerConsolidator()
 		{
 			super(new Agent(), 1000);
 			msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchOntology("start-server-consolidation"));
 		}
 		public void onTick()
 		{
 			msg = receive(msgTemplate);
 			if(msg != null)
 			{
 				count = 0;
 				for(int i=0; i < num_of_vms; i++)
 				{
 					if(vms[i].status == VirtualMachine.BUSY)
 					{
 						count ++;
 						if(vms[i].startMigration == false)
 						{
 							vms[i].startMigration = true;
 							vms[i].migrationReason = VirtualMachine.SERVER_CONSOLIDATION;
 							logTextArea.append("\n\n"+new Date()+" => MIGRATION TO BE TRIGGERED FOR "+vms[i].vma_name+" TO DO SERVER CONSOLIDATION");
 						}
 					}
 				}
 				if(count > 0)
 				{
 					System.out.println(new Date()+" => Server Consolidation started for server "+ID);
 					serverMachine.turnoff_count ++;
 				}
 			}
 		}
 	}
}