import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import jade.wrapper.*;
import java.io.*;
import java.util.*;

public class FrontEndAgent extends Agent
{
	File file;
	FileWriter fw;
	ServerMachine[] serverMachines;
	JTextArea logTextArea;
	JTextArea textarea;
	long[] response_time = new long[600];
	double avg_response_time;
	int req_count = -1;
	int server_count = 12;
	boolean leader_method = true;

	public void setup()
	{
		Object[] args = getArguments();
		serverMachines = (ServerMachine[])args[0];
		logTextArea = (JTextArea)args[1];
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
		addBehaviour(new TriggerServerMonitor());
		addBehaviour(new AvgResetter());

		JFrame frame = new JFrame("Response time for VM request");
		textarea = new JTextArea();
		textarea.setLineWrap(true);
		textarea.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(textarea);
		frame.add(scrollPane);
		frame.setVisible(true);
		frame.setSize(600, 600);

		consolidationMethodSelector();
	}

	class AvgResetter extends CyclicBehaviour
	{
		public void action()
		{
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), MessageTemplate.MatchOntology("reset-avg"));
			ACLMessage msg = receive(msgTemplate);
			if(msg != null)
			{
				req_count = -1; //when a request comes after this, req_count will be 0. Avg will be reset by RequestProcessor behaviour when the req_count is 0.
			}
		}
	}

	class RequestProcessor extends CyclicBehaviour
	{
		Object obj;
		int i,j;
		long req_arrival_time = 0;
		long vm_selection_time = 0;
		public void action()
		{

			if((obj = getO2AObject()) != null)
			{
				if(obj.getClass().getSimpleName().equals("VMRequest"))
				{
					req_count++;
					VMRequest vmrequest = (VMRequest)obj;
					req_arrival_time = System.currentTimeMillis();
					
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
									vm_selection_time = System.currentTimeMillis();
									response_time[req_count] = vm_selection_time - req_arrival_time;
									if(req_count == 0) //first request
									{
										avg_response_time = (double)response_time[req_count];
										textarea.append("\n\n-------------------------------------------------");
									}
									else
									{
										avg_response_time = ((avg_response_time * req_count) + response_time[req_count]) / (req_count + 1); //calculating for each request
									}
									textarea.append("\n"+vmrequest.req_id+" => "+response_time[req_count]+"ms (Avg: "+avg_response_time+"ms)");

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

	class TriggerServerMonitor extends CyclicBehaviour
	{
		public void action()
		{
			MessageTemplate msgTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), MessageTemplate.MatchOntology("start-monitoring-for-server-consolidation"));
			ACLMessage msg = receive(msgTemplate);
			if(msg != null)
			{
				addBehaviour(new ServerMonitor(new Agent(), 5000));
				removeBehaviour(this);
			}
		}
	}

	public void consolidationMethodSelector()
	{
		JFrame frame = new JFrame("Consolidation Method Selector");
		frame.setSize(500, 200);
		frame.setVisible(true);
		JButton leader_button = new JButton("With Leader Selection");
		leader_button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				leader_method = true;
			}
		});
		JButton no_leader_button = new JButton("Without Leader Selection");
		no_leader_button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				leader_method = false;
			}
		});
		frame.setLayout(new FlowLayout());
		leader_button.setPreferredSize(new Dimension(150,150));
		no_leader_button.setPreferredSize(new Dimension(150,150));
		frame.add(leader_button);
		frame.add(no_leader_button);
		
	}

	class ServerMonitor extends TickerBehaviour 
	{
		ServerMachine[] under_utilized_servers, not_utilized_servers;
		int i, under_count = 0, not_count = 0; //underutilized server count and not utilized server count
		int total_load = 0;
		public ServerMonitor(Agent agent, long period)
		{
			super(agent, period);
		}
		public void onTick()
		{
			under_utilized_servers = new ServerMachine[serverMachines.length];
			not_utilized_servers = new ServerMachine[serverMachines.length];
			under_count = 0;
			not_count = 0;

			if(leader_method == true)
			{
				for(i = 0; i < serverMachines.length; i++)
				{
					if(serverMachines[i].status == ServerMachine.NOT_UTILIZED)
					{
						not_utilized_servers[not_count++] = serverMachines[i];
					}
					else if(serverMachines[i].status == ServerMachine.UNDER_UTILIZED)
					{
						under_utilized_servers[under_count++] = serverMachines[i];
					}
				}
				System.out.println(new Date()+" => Underutilized server list: ");
				for(i = 0; i < under_count; i++)
				{
					System.out.print("\t"+under_utilized_servers[i].ID);
				}
				System.out.println();
				System.out.println(new Date()+" => Not utilized server list: ");
				for(i = 0; i < not_count; i++)
				{
					System.out.print("\t"+not_utilized_servers[i].ID);
				}
				System.out.println();
				int min = 0, min_total_load = 0;
				//Leader Selection
				if(under_count == 1 && not_count == server_count - 1) //only one server is active and that server is under utilized
				{
					//no server consolidation
				}
				else if(under_count > 0)
				{
					for(i = 0; i < under_count; i++)
					{
						total_load = under_utilized_servers[i].cpu_load + under_utilized_servers[i].mem_load;
						if(i == 0)
						{
							min = 0;
							min_total_load = total_load;
						}
						else
						{
							if(total_load < min_total_load)
							{
								min_total_load = total_load;
								min = i;
							}
						}
					}
					ServerMachine leader = under_utilized_servers[min];
					System.out.println(new Date()+"----------- Selected leader for starting server consolidation => Server "+leader.ID+" with CPU load = "+leader.cpu_load_percentage+"%, Mem load = "+leader.mem_load_percentage+"%");
					logTextArea.append("\n"+new Date()+"----------- Selected leader for starting server consolidation => Server "+leader.ID+" with CPU load = "+leader.cpu_load_percentage+"%, Mem load = "+leader.mem_load_percentage+"%");
					//Trigger server consolidation
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setOntology("start-server-consolidation");
					msg.addReceiver(new AID("sma"+leader.ID, AID.ISLOCALNAME));
					send(msg);
					//after server consolidation
					//turn off the server => status = NOT_UTILIZED means server is turned off
					//if the load is 0, the status is set as NOT_UTILIZED in ThresholdMonitoring behaviour of SMA
				}
			}
			else //Without leader selection
			{
				for(i = 0; i < serverMachines.length; i++)
				{
					if(serverMachines[i].status == ServerMachine.NOT_UTILIZED)
					{
						not_utilized_servers[not_count++] = serverMachines[i];
					}
					else if(serverMachines[i].status == ServerMachine.UNDER_UTILIZED)
					{
						System.out.println(new Date()+" => Underutilized server found: Server"+serverMachines[i].ID);
						ServerMachine leader = serverMachines[i];
						System.out.println(new Date()+"----------- Selected leader for starting server consolidation => Server "+leader.ID+" with CPU load = "+leader.cpu_load_percentage+"%, Mem load = "+leader.mem_load_percentage+"%");
						logTextArea.append("\n"+new Date()+"----------- Selected leader for starting server consolidation => Server "+leader.ID+" with CPU load = "+leader.cpu_load_percentage+"%, Mem load = "+leader.mem_load_percentage+"%");
						//Trigger server consolidation
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setOntology("start-server-consolidation");
						msg.addReceiver(new AID("sma"+leader.ID, AID.ISLOCALNAME));
						send(msg);
					}
				}
				System.out.println();
				System.out.println(new Date()+" => Not utilized server list: ");
				for(i = 0; i < not_count; i++)
				{
					System.out.print("\t"+not_utilized_servers[i].ID);
				}
				System.out.println();
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
