import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import javax.swing.*;
import jade.wrapper.*;
import java.io.*;
import java.util.*;

public class FrontEndAgent extends Agent
{
	File file;
	FileWriter fw;
	ServerMachine[] serverMachines;
	public void setup()
	{
		Object[] args = getArguments();
		serverMachines = (ServerMachine[])args[0];
		try
		{
			file = new File("logfile.txt");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		setEnabledO2ACommunication(true,0);
		addBehaviour(new RequestProcessor());
	}

	class RequestProcessor extends CyclicBehaviour
	{
		Object obj;
		int i,j;
		public void action()
		{

			if((obj = getO2AObject()) != null)
			{
				if(obj.getClass().getSimpleName().equals("VMRequest"))
				{
					VMRequest vmrequest = (VMRequest)obj;
					try
					{
						vmrequest.reply_to = "fa"; //reply should be sent back to FA
						jade.wrapper.AgentContainer agentContainer = getContainerController();
						AgentController agentController = agentContainer.getAgent("ca");
						agentController.putO2AObject(vmrequest,false);

						while(true)
						{
							while((obj = getO2AObject()) == null)
								;
							if(obj.getClass().getSimpleName().equals("VMCluster"))
							{
								VMCluster vmcluster = (VMCluster)obj;
								if(vmcluster.isEmpty() == true)
								{
									System.out.println("Error: VMRequest(CPU="+vmrequest.cpu_capacity+";Mem="+vmrequest.mem_capacity+") could not be allocated in any of the clusters..");
									fw = new FileWriter(file, true);
									fw.write("\n----Error: VMRequest(CPU="+vmrequest.cpu_capacity+";Mem="+vmrequest.mem_capacity+") could not be allocated in any of the clusters----");
									break;
								}
								//assign weights and pick one vm and change the vm's status
								VMCluster vmcluster2 = new VMCluster();
								VirtualMachine vm;

								String free_vms_str, str0, str1, str2, str3, str4, str5;
								str0 = "Request ID : "+vmrequest.req_id+"\n";
								//filtering the vms that are incapable of fulfilling the request and that are busy
								str1 = "Chosen Cluster:\n";
								str2 = "VMs in the cluster after filtering:\n";
								free_vms_str="\nVMs that are free:\n";
								for(i=0;i<vmcluster.getClusterLength();i++)
								{	
									vm = vmcluster.get(i);
									str1 += "("+vm.cpu_capacity+","+vm.mem_capacity+")";
									if(vm.cpu_capacity >= vmrequest.cpu_capacity && vm.mem_capacity >= vmrequest.mem_capacity)
									{
										str2 += "("+vm.cpu_capacity+","+vm.mem_capacity+")";
										if(vm.status == VirtualMachine.FREE)
										{
											vmcluster2.add(vm);
											free_vms_str += "("+vm.cpu_capacity+","+vm.mem_capacity+")";
										}
									}
								}

								//arranging vms in new cluster in the ascending order of cpu capacity
								VirtualMachine tempvm;
								int n = vmcluster2.getClusterLength();
								for(i=0;i<n;i++)
								{
									for(j=i+1;j<n;j++)
									{
										if((vmcluster2.get(i)).cpu_capacity > (vmcluster2.get(j)).cpu_capacity)
										{
											tempvm = vmcluster2.get(i);
											vmcluster2.set(i,vmcluster2.get(j));
											vmcluster2.set(j,tempvm);
										}
									}
								}
								//assigning weights according to the vm order of cpu_capacity
								for(i=0;i<n;i++)
								{
									vmcluster2.get(i).cpu_weight = i;
								}	

								str3 = "\nVMs in the order of CPU capacity:\n";
								for(i=0;i<n;i++)
								{
									str3 += "("+vmcluster2.get(i).cpu_capacity+","+vmcluster2.get(i).mem_capacity+")";
								}
								

								//arranging vms in new cluster in the ascending order of mem capacity
								for(i=0;i<n;i++)
								{
									for(j=i+1;j<n;j++)
									{
										if((vmcluster2.get(i)).mem_capacity > (vmcluster2.get(j)).mem_capacity)
										{
											tempvm = vmcluster2.get(i);
											vmcluster2.set(i,vmcluster2.get(j));
											vmcluster2.set(j,tempvm);
										}
									}
								}

								//assigning weights according to the vm order of mem_capacity
								int min = 0;
								for(i=0;i<n;i++)
								{
									vmcluster2.get(i).mem_weight = i;
									vmcluster2.get(i).total_weight = vmcluster2.get(i).cpu_weight + vmcluster2.get(i).mem_weight;
									//choosing vm with minimum total weight
									if(vmcluster2.get(i).total_weight < vmcluster2.get(min).total_weight)
										min = i;
								}	
								str4 = "\nVMs in the order of Mem capacity:\n";
								for(i=0;i<n;i++)
								{
									str4 += "("+vmcluster2.get(i).cpu_capacity+","+vmcluster2.get(i).mem_capacity+")";
								}
								VirtualMachine selectedvm = null;
								try
								{
									selectedvm = vmcluster2.get(min);

									str5 = "\nRequested Virtual Machine : ("+vmrequest.cpu_capacity+","+vmrequest.mem_capacity+")"+"\nVirtual Machine selected: ("+selectedvm.cpu_capacity+","+selectedvm.mem_capacity+")"+" ["+selectedvm.vma_name+"]";
									//running the selected virtual machine
									selectedvm.runMachine(vmrequest);
								}
								catch(Exception ex)
								{
									System.out.println("\nError for VM request "+vmrequest.req_id);
									ex.printStackTrace();
									str5 = "\nRequested Virtual Machine : ("+vmrequest.cpu_capacity+","+vmrequest.mem_capacity+")"+"\nRequested job could not be allocated";
								}
								String print_string = str0+"\n"+str1+"\n\n"+str2+"\n"+free_vms_str+"\n"+str3+"\n"+str4+"\n"+str5;
								// JOptionPane.showMessageDialog(null,str1+"\n\n"+str2+"\n"+free_vms_str+"\n"+str3+"\n"+str4+"\n"+str5);
								// System.out.println(print_string);
								fw = new FileWriter(file, true);
								fw.write("\n------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
								fw.write("\n"+new Date()+"\n"+print_string);
								fw.close();

								break;
							}
						}

					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}
	}

	//sample class
	/*class RequestGetter extends CyclicBehaviour
	{
		public void action()
		{
			MessageTemplate msgtmplt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = receive(msgtmplt);
			if(msg!=null)
			{
				String content = msg.getContent();
				String[] req = content.split(",");
				int cpureq = Integer.parseInt(req[0]);
				int memreq = Integer.parseInt(req[1]);
				int exectime = Integer.parseInt(req[2]);
				System.out.println("Received msg : "+content);
				// JOptionPane.showMessageDialog(null,content);
				ACLMessage reply = msg.createReply(); //set the receivers, other necessary fields automatically
				reply.setPerformative(ACLMessage.AGREE);
				reply.setContent("I agree with you");
				send(reply);

					
			}
		}
	}*/

}
