import jade.core.*; 
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import jade.wrapper.*;
import java.util.*;
import java.io.*;

public class UserAgent extends Agent
{
	int sma_count = 12;
	int vm_count = 72;
	int req_no = 0;
	int req_trigger_count = 0;
	ServerMachine[] serverMachines;
	VirtualMachine[] virtualMachines;

	public void setup()
	{	
		setEnabledO2ACommunication(true, 0);
		Object[] args = getArguments();
		serverMachines = (ServerMachine[])args[0];
		addBehaviour(new UserAgentGUI());
		addBehaviour(new VMInstanceGetter());

		automateRequests();
	}
	
	public void sendRequest(int req_no, int cpureq,int memreq, int exectime, int extra_cpu, int extra_mem)
	{
		VMRequest vmrequest = new VMRequest(req_no, cpureq, memreq, exectime, extra_cpu, extra_mem);
		try
		{
			jade.wrapper.AgentContainer agentContainer = getContainerController();
			AgentController agentController = agentContainer.getAgent("fa");
			agentController.putO2AObject(vmrequest,false);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	class UserAgentGUI extends OneShotBehaviour
	{
		public void action()
		{
			JFrame frame = new JFrame("User Agent");
			frame.setSize(500,300);
			JLabel vmreq = new JLabel("VM Request specifications:");
			vmreq.setBounds(150,10,300,30);
			JLabel cpureq = new JLabel("CPU requirement (no. of virtual cores) : ");
			cpureq.setBounds(20,50,300,30);
			final JTextField cputext = new JTextField(10);
			cputext.setBounds(300,50,100,30);
			JLabel memreq = new JLabel("Memory requirement (in GB) : ");
			memreq.setBounds(20,100,300,30);
			final JTextField memtext = new JTextField(10);
			memtext.setBounds(300,100,100,30);
			JLabel exec = new JLabel("Execution time (in s)");
			exec.setBounds(20,150,300,30);
			final JTextField exectext = new JTextField(10);
			exectext.setBounds(300,150,100,30);

			frame.add(vmreq);
			frame.add(cpureq);
			frame.add(cputext);
			frame.add(memreq);
			frame.add(memtext);
			frame.add(exec);
			frame.add(exectext);

			JButton button = new JButton("Submit Request");
			button.setBounds(150,200,150,30);
			button.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e)
				{
					int cpureq = Integer.parseInt(cputext.getText());
					int memreq = Integer.parseInt(memtext.getText());
					int exectime = Integer.parseInt(exectext.getText());
					
					Random random = new Random();
					int extra_cpu = random.nextInt(4); //0 to 3 (approx.)
					int extra_mem = random.nextInt(5); //0 to 4 (approx.)
					
					/*ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
					message.addReceiver(new AID("fa",AID.ISLOCALNAME));
					message.setContent(cpureq+","+memreq+","+exectime);
					send(message);

					MessageTemplate msgtmplt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
					ACLMessage reply = null;
					while(reply == null)
					{
						reply = receive(msgtmplt);
					}
					String content = reply.getContent();
					JOptionPane.showMessageDialog(null,content);
					*/
					
					//actual processing
					/*VMRequest vmrequest = new VMRequest(cpureq,memreq,exectime);
					try
					{
						jade.wrapper.AgentContainer agentContainer = getContainerController();
						AgentController agentController = agentContainer.getAgent("fa");
						agentController.putO2AObject(vmrequest,false);
					}
					catch(Exception ex)
					{
						ex.printStackTrace();
					}*/

					sendRequest(++req_no,cpureq,memreq,exectime, extra_cpu, extra_mem);
				}

			});

			frame.add(button);
			frame.setLayout(null);
			frame.setVisible(true);

		}
	}

	class VMInstanceGetter extends OneShotBehaviour
	{
		Object obj = null;
		public void action()
		{
			while((obj = getO2AObject()) == null)
				;
			virtualMachines = (VirtualMachine[])obj;
		}
	}

	public void automateRequests()
	{
		JFrame frame = new JFrame("");
		frame.setSize(200,200);
		JButton button = new JButton("Start Request Automation");
		button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					//reset turnoff count for each server (for server consolidation) for each new set of requests
					for(int i = 0; i < sma_count; i++)
					{
						serverMachines[i].turnoff_count = 0;
					}
					//reset migration count for each VM
					for(int i = 0; i < vm_count; i++)
					{
						virtualMachines[i].mig_for_server_overload_count = 0;
						virtualMachines[i].mig_for_insuff_capacity_count = 0;
						virtualMachines[i].mig_for_server_consldtn_count = 0;
					}

					req_trigger_count++;
					if(req_trigger_count == 3)
					{
						ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
						msg.setOntology("process-with-global-cluster");
						msg.addReceiver(new AID("ca", AID.ISLOCALNAME));
						send(msg);

						msg = new ACLMessage(ACLMessage.REQUEST);
						msg.setOntology("reset-avg");
						msg.addReceiver(new AID("fa", AID.ISLOCALNAME));
						send(msg);
					}
			
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					msg.setOntology("start-threshold-monitoring");
					for(int i=1;i<=sma_count;i++)
						msg.addReceiver(new AID("sma"+i,AID.ISLOCALNAME));
					send(msg);

					msg.setOntology("start-monitoring-for-server-consolidation");
					msg.addReceiver(new AID("fa", AID.ISLOCALNAME));
					send(msg);
					
					JFrame frame = new JFrame("VM instances");
					JTextArea textArea = new JTextArea();
					textArea.setLineWrap(true);
					textArea.setWrapStyleWord(true);
					JScrollPane scrollPane = new JScrollPane(textArea);
					frame.add(scrollPane);
					frame.setSize(500,500);
					frame.setVisible(true);
					Random random = new Random();
					int cpureq = 0, memreq = 0, exectime = 0, timelapse = 0, extracpu = 0, extramem = 0;

					boolean compare = false;
					File file = new File("Requests.txt");
					FileReader freader = new FileReader(file);
					BufferedReader bfreader = new BufferedReader(freader);
					if(bfreader.readLine() != null)
						compare = true;
					else
						compare = false;
					if(compare == false)
					{
						FileWriter fwriter = new FileWriter(file, false);
						fwriter.write("timelapse req_no cpureq memreq exectime extracpu extramem\n");
						fwriter.close();
					}
					for(int i=1;i<=100;i++)
					{
						if(compare == true)
						{
							String str = bfreader.readLine();
							String[] strarr = str.split(" ");
							timelapse = Integer.parseInt(strarr[0]);
							req_no = Integer.parseInt(strarr[1]);
							cpureq = Integer.parseInt(strarr[2]);
							memreq = Integer.parseInt(strarr[3]);
							exectime = Integer.parseInt(strarr[4]);
							extracpu = Integer.parseInt(strarr[5]);
							extramem = Integer.parseInt(strarr[6]);
						}
						else
						{
							req_no++;
							cpureq = random.nextInt(8) + 1;
							memreq = random.nextInt(20) + 1;
							exectime = random.nextInt(10) + 1;

							int cpu_bound = cpureq / 4;
							int mem_bound = memreq / 4;
							if(cpu_bound == 0)
								cpu_bound = 1;
							if(mem_bound == 0)
								mem_bound = 1;
							extracpu = random.nextInt(cpu_bound); //0 to half the cpu request
							extramem = random.nextInt(mem_bound); //0 to half the mem request

							int totalcpu  = cpureq + extracpu;
							int totalmem = memreq + extramem;
							if(totalcpu > 8)
							{
								extracpu = extracpu - (totalcpu - 8); //cpu req = 7, extra cpu needed = 4 => total cpu = 11 => this is not possible; extra cpu can only be 1 in this case so that it will make the total 8. => reduce the extra count above 8, from extra cpu needed. extra cpu = 4 - (11-8) = 1.
								
								//or just
								//extracpu = 8 - cpureq;
							}
							if(totalmem > 20)
							{
								extramem = extramem - (totalmem - 20);
							}

							// timelapse = (random.nextInt(10) + 1) * 1000;
							timelapse = i * 500;

							FileWriter fwriter = new FileWriter(file, true);
							fwriter.write(timelapse+" "+req_no+" "+cpureq+" "+memreq+" "+exectime+" "+extracpu+" "+extramem+"\n");
							fwriter.close();
						}
						textArea.append("\n----Req.no : "+req_no+"----Time lapse:"+timelapse+" ms");
						addBehaviour(new AutomateRequestBehaviour(new Agent(), timelapse, req_no, cpureq, memreq, exectime, extracpu, extramem, textArea));
					}
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
				}
			}
		});
		frame.add(button);
		frame.setVisible(true);
	}

	class AutomateRequestBehaviour extends WakerBehaviour 
	{
		int cpureq, memreq, exectime, timelapse, req_no, extracpu, extramem;
		JTextArea textArea;
		public AutomateRequestBehaviour(Agent agent, long millis, int req_no, int cpureq, int memreq, int exectime, int extracpu, int extramem, JTextArea textArea)
		{
			super(agent,millis);
			this.timelapse = timelapse;
			this.req_no = req_no;
			this.cpureq = cpureq;
			this.memreq = memreq;
			this.exectime = exectime;
			this.extracpu = extracpu;
			this.extramem = extramem;
			this.textArea = textArea;
		}
		public void onWake()
		{
			sendRequest(req_no, cpureq, memreq, exectime, extracpu, extramem);
			textArea.append("\n"+new Date()+" Request no: "+req_no+" --> cpu: "+cpureq+" mem: "+memreq+" exectime: "+exectime);
		}
	}
}
