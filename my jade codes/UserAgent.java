import jade.core.*; 
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import jade.wrapper.*;
import java.util.*;

public class UserAgent extends Agent
{
	public void setup()
	{	
		addBehaviour(new UserAgentGUI());
		automateRequests();
	}
	
	public void sendRequest(int cpureq,int memreq, int exectime, int extra_cpu, int extra_mem)
	{
		VMRequest vmrequest = new VMRequest(cpureq, memreq, exectime, extra_cpu, extra_mem);
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

					sendRequest(cpureq,memreq,exectime, extra_cpu, extra_mem);
				}

			});

			frame.add(button);
			frame.setLayout(null);
			frame.setVisible(true);

		}
	}

	public void automateRequests()
	{
		JFrame frame = new JFrame("VM instances");
		JTextArea textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		JScrollPane scrollPane = new JScrollPane(textArea);
		frame.add(scrollPane);
		frame.setSize(500,500);
		frame.setVisible(true);
		Random random = new Random();
		int cpureq, memreq, exectime, timelapse, req_no, extracpu, extramem;
		for(int i=1;i<=10;i++)
		{
			req_no = i;
			cpureq = random.nextInt(8) + 1;
			memreq = random.nextInt(20) + 1;
			exectime = random.nextInt(10) + 1;

			extracpu = random.nextInt(4); //0 to 3 (approx.)
			extramem = random.nextInt(5); //0 to 4 (approx.)

			//timelapse = (random.nextInt(10) + 1) * 1000;
			timelapse = i * 1000;
			textArea.append("\n----Req.no : "+req_no+"----Time lapse:"+timelapse+" ms");
			addBehaviour(new AutomateRequestBehaviour(this, timelapse, req_no, cpureq, memreq, exectime, extracpu, extramem, textArea));
		}
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
			sendRequest(cpureq, memreq, exectime, extracpu, extramem);
			textArea.append("\nRequest no: "+req_no+" --> cpu: "+cpureq+" mem: "+memreq+" exectime: "+exectime);
		}
	}
}
