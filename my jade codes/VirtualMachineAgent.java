import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.wrapper.*;
import javax.swing.*;
import java.util.*;

public class VirtualMachineAgent extends Agent
{
	// VirtualMachine[] vms = new VirtualMachine[12];

	public int ID,localID,serverID;
	public String vma_name;
	public int cpu_capacity,mem_capacity;
	public int status = 0; //0 for free; 1 for busy
	public VirtualMachine vminstance;
	public JTextArea logTextArea;
	public ServerMachine serverMachine;
	int first_fit = 1, best_fit = 0;

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
			serverMachine = (ServerMachine)args[6];
		}
		// JOptionPane.showMessageDialog(null,getLocalName()+" at "+getAID()+" started");
		// System.out.println(getLocalName()+" with ID "+ID+" in Server "+serverID+" is started with mem_capacity : "+mem_capacity+" cpu_capacity : "+cpu_capacity);
		
		vminstance = new VirtualMachine(localID,serverID,getLocalName(),cpu_capacity,mem_capacity, logTextArea);

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
				int dummy = -1;
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
					VMCluster vmcluster;
					while(true)
					{
						while((obj = getO2AObject()) == null)
							;
						if(obj.getClass().getSimpleName().equals("VMCluster"))
						{
							vmcluster = (VMCluster)obj;
							System.out.println("Cluster received to "+vma_name+" for migration");
							break; //expected object received
						}
					}
					if(vmcluster.isEmpty() == true)
					{
						System.out.println("Error!! No capable VMs found to migrate job of "+vma_name+" to");
						if(vminstance.migrationReason == VirtualMachine.INSUFFICIENT_CAPACITY)
						{
							//stopping migration and dropping the job
							vminstance.startMigration = false;
							vminstance.migrationReason = VirtualMachine.NO_MIGRATION;
							vminstance.status = VirtualMachine.FREE;
							return;
						}	
					}
					//process the cluster
					//select server for migration by checking the vms if they are free and by checking if migration would exceed server threshold in which the concerned VM lies  
					int n = vmcluster.getClusterLength();;
					VirtualMachine[] vm_array = new VirtualMachine[n];
					ServerMachine[] serverMachines = new ServerMachine[n];
					VirtualMachine vm;
					int count = 0, max_diff_for = 0;
					double total_diff, max_total_diff = 0;

					System.out.println("Iteration count n = "+n+" for "+vma_name);
					for(int i = 0; i < n; i++)
					{
						System.out.println("Loop iteration: "+i+" for "+vma_name);
						vm = vmcluster.get(i);
						//checking status to see if the VM is busy/free and if it's able to allocate the required amount of CPU and memory for the job
						if(vm.status == VirtualMachine.FREE && vm.cpu_capacity >= vmrequest.cpu_capacity && vm.mem_capacity >= vmrequest.mem_capacity)
						{
						 	//requesting for ServerMachine instance of this VM
						 	ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
						 	msg.setOntology("requesting-for-server-machine-instance");
						 	msg.addReceiver(new AID("sma"+vm.server_id,AID.ISLOCALNAME));
						 	send(msg);

						 	dummy = -2;

						 	//getting the ServerMachine object
						 	while((obj = getO2AObject()) == null)
						 		;
						 	if(obj.getClass().getSimpleName().equals("ServerMachine"))
						 	{
						 		dummy = -3;
						 		ServerMachine sm = (ServerMachine)obj;
						 		System.out.println("ServerMachine instance of "+vm.vma_name+" - received to "+vma_name);
						 		//check the load level when this server takes up the job to see if it exceeds the threshold
						 		int cpu_load = sm.cpu_load + vmrequest.cpu_capacity;
						 		int mem_load = sm.mem_load + vmrequest.mem_capacity;
						 		double cpu_load_percentage = ((1.0 * cpu_load) / sm.total_cpu) * 100;
						 		double mem_load_percentage = ((1.0 * mem_load) / sm.total_mem) * 100;
						 		double cpu_load_threshold_percentage = sm.cpu_load_threshold_percentage;
						 		double mem_load_threshold_percentage = sm.mem_load_threshold_percentage;

						 		//(threshold level - load) => for the best server, this diff is maximum 
						 		double cpu_diff = cpu_load_threshold_percentage - cpu_load_percentage;
						 		double mem_diff = mem_load_threshold_percentage - mem_load_percentage;

						 		if(sm.ID != serverID && cpu_diff >= 0 && mem_diff >= 0 && (vminstance.migrationReason != VirtualMachine.SERVER_CONSOLIDATION || sm.status != ServerMachine.NOT_UTILIZED))//checking if it exceeds the threshold or not and checking this condition: if migration reason is server consolidation, host server must not be having "not_utilized" status(logic: just for turning a server off, another server must not be turned on from turned off state)
						 		{
						 			//this server will not be overloaded even when the job is migrated to it; so include the concerned VM and the server 
						 			serverMachines[count] = sm;
						 			vm_array[count] = vm; 

							 		total_diff = cpu_diff + mem_diff;
							 		
							 		//finding the best fit
							 		if(vminstance.host_selection_algo.equals("best-fit"))
							 		{
								 		if(count == 0) //first potential VM's server
								 		{
								 			max_diff_for = count;
								 			max_total_diff = total_diff;
								 		}
								 		else
								 		{
								 			if(total_diff > max_total_diff)
								 			{
								 				max_total_diff = total_diff;
								 				max_diff_for = count;
								 				dummy = max_diff_for;
								 			}
								 		}
								 		count++;
							 		}
							 		//finding the first fit (without changing the code structure of max_diff_for)
							 		else if(vminstance.host_selection_algo.equals("first-fit"))
							 		{
							 			max_diff_for = count;
							 			max_total_diff = total_diff;
							 			count++;
							 			break;
							 		}
							 	}
						 	}
						 	else
						 	{
						 		System.out.println("Some other object is received in place of ServerMachine to VMA "+vma_name);
						 	}
						}  
					}
					if(count == 0) //no capable VM is free; so try next time (some VM may complete the job and it may become free)
					{
						if(vminstance.migrationReason == VirtualMachine.SERVER_CONSOLIDATION)
						{
							vminstance.startMigration = false;
							vminstance.migrationReason = VirtualMachine.NO_MIGRATION;
							serverMachine.turnoff_count--; //since turnoff count would have been incremented before the start of migrations for consolidation, it has to be decremented now as consolidation is cancelled for this server
							System.out.println("Server "+serverID+" is prevented from performing server consolidation as no potential active servers are found");
						}
						else
							System.out.println("No potential server found - for migration of job from "+vma_name);
						return;
					}
					//count > 0 // at least one server and a VM in it are found
						
					//serverMachines[max_diff_for] gives the selected server; vm_array[max_diff_for] gives the VM in the selected server to which the job is to be given
					ServerMachine selected_server = serverMachines[max_diff_for];
					VirtualMachine selected_vm = vm_array[max_diff_for];

					//Migration
					if(selected_vm.status == VirtualMachine.FREE) //checking if the selected VM is still free
					{
						logTextArea.append("\n\n"+new Date()+" -> SELECTED SERVER : "+selected_server.ID+"; SELECTED VM: "+selected_vm.vma_name+" => FOR THE JOB IN VM "+vma_name+"\n");
						System.out.println("\n"+new Date()+" -> SELECTED SERVER : "+selected_server.ID+"; SELECTED VM: "+selected_vm.vma_name+" => FOR THE JOB IN VM "+vma_name+"\n");
						//migration can be done
						selected_vm.runMachine(vmrequest);
						//to notify this VM's server about successful migration 
						if(vminstance.migrationReason == VirtualMachine.SERVER_OVERLOAD)
						{
							ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
						 	msg.setOntology("requesting-for-server-machine-instance");
						 	msg.addReceiver(new AID("sma"+vminstance.server_id,AID.ISLOCALNAME));
						 	send(msg);

						 	//getting the ServerMachine object
						 	while((obj = getO2AObject()) == null)
						 		;
						 	if(obj.getClass().getSimpleName().equals("ServerMachine"))
						 	{
						 		ServerMachine sm = (ServerMachine)obj;
						 		sm.migration_triggered = false; //clearing the variable for the next round of migration
						 	}
						 	else
						 	{
						 		System.out.println("In VMA -> Some other object is received in place of ServerMachine object !!");
						 	}

						 	vminstance.mig_for_server_overload_count++;
						}
						else if(vminstance.migrationReason == VirtualMachine.INSUFFICIENT_CAPACITY)
						{
							vminstance.mig_for_insuff_capacity_count++;
						}
						else if(vminstance.migrationReason == VirtualMachine.SERVER_CONSOLIDATION)
						{
							vminstance.mig_for_server_consldtn_count++;
						}

						//after migration
						vminstance.startMigration = false;
						vminstance.migrationReason = VirtualMachine.NO_MIGRATION;
						vminstance.status = VirtualMachine.FREE; //after migration, this VM will be free since job is ported to some other VM in some other server
						vminstance.cpu_occupied = 0;
						vminstance.mem_occupied = 0;
					}
					else
					{
						logTextArea.append("\n\nSELECTED SERVER : "+selected_server.ID+"; SELECTED VM: "+selected_vm.vma_name+" => for the job in VM "+vma_name+" ----- Oops! VM is found be busy ; MIGRATION FAILED !!\n");
					}
				}
				catch(Exception e)
				{
					System.out.println("Error message : \n max_diff_for = "+dummy);
					e.printStackTrace();
				}
			} 
		}
	}
} 
