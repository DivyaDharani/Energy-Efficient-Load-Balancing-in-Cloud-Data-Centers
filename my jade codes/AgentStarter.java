import jade.core.*;
import jade.core.behaviours.*;
import jade.wrapper.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
public class AgentStarter extends Agent
{
	int sma = 12;
	int vma = 72;

	public int num_of_vms(int serverid)
	{
		if(serverid>=1 && serverid<=6)
			return 4;
		else
			return 8;
	}

	public void setup()
	{
		addBehaviour(new StarterBehaviour());
	}

	class StarterBehaviour extends OneShotBehaviour
	{
		int i,j,k,inc;
		ContainerController cc;
		AgentController[] ac = new AgentController[100];
		public void action()
		{
			JFrame logFrame = new JFrame(" Job Execution Logs");
			JTextArea logTextArea = new JTextArea();
			logTextArea.setLineWrap(true);
			logTextArea.setWrapStyleWord(true);
			JScrollPane scrollPane = new JScrollPane(logTextArea);
			logFrame.add(scrollPane);
			logFrame.setSize(500,500);
			logFrame.setVisible(true);

			try
			{
				File file = new File("logfile.txt");
				FileWriter fw = new FileWriter(file); //not in append mode (so, prev content will be erased by now)
				fw.write("LOGS - VM ALLOCATION:\n");
				fw.close();
				
				cc = getContainerController(); //method of Agent class
				inc = 0;
				ac[inc++] = cc.createNewAgent("ua","UserAgent",null);
				// ac.start();
				ac[inc++] = cc.createNewAgent("fa","FrontEndAgent",null);
				// ac.start();

				/*//creating server manager agents
				for(i=1;i<=sma;i++)
				{
					ac[inc++] = cc.createNewAgent("sma"+i,"ServerManagerAgent",new Object[]{i,num_of_vms(i)});//passing server's ID and no. of vms in it
					// ac.start();
				}*/

				//creating SMAs and VMAs

				File memfile = new File("mem capacity.txt");
				File cpufile = new File("cpu capacity.txt");
				FileReader memfilereader = new FileReader(memfile);
				BufferedReader membf = new BufferedReader(memfilereader);
				FileReader cpufilereader = new FileReader(cpufile);
				BufferedReader cpubf = new BufferedReader(cpufilereader);

				int vminc=1;
				String cpustr,memstr;
				String[] cpuarr,memarr;
				for(i=1;i<=sma;i++)
				{
					cpustr = cpubf.readLine(); //each line in file represents one server's info
					cpuarr = cpustr.split(" ");
					memstr = membf.readLine();
					memarr = memstr.split(" ");

					//computing total cpu and mem capacities of server machine 'i'
					int total_cpu = 0, total_mem = 0, t;
					for(t = 0; t < cpuarr.length; t++)
						total_cpu += Integer.parseInt(cpuarr[t]);
					for(t = 0; t < memarr.length; t++)
						total_mem += Integer.parseInt(memarr[t]);

					//creating SMA
					ac[inc++] = cc.createNewAgent("sma"+i,"ServerManagerAgent",new Object[]{i, num_of_vms(i), total_cpu, total_mem, logTextArea});//passing server's ID, no. of vms in it, and total cpu and memory capacities 

					//creating VMAs of server 'i'
					j = num_of_vms(i);
					for(k=0;k<j;k++)
					{
						//vm's local name => vma_<server's id>_<vm's local id in the server>
						ac[inc++] = cc.createNewAgent("vma_"+i+"_"+(k+1),"VirtualMachineAgent", new Object[]{vminc++,k+1,i,Integer.parseInt(cpuarr[k]),Integer.parseInt(memarr[k]), logTextArea});
						//passing vm's ID, cpu capacity and mem capacity
						// ac.start();
					}
				}
				//starting clustering agent
				ac[inc++] = cc.createNewAgent("ca","ClusteringAgent",null);
				// ac.start();

				JFrame frame = new JFrame("");
				frame.setSize(200,200);
				JButton button = new JButton("Start process");
				button.addActionListener(new ActionListener(){
					public void actionPerformed(ActionEvent e)
					{
						try
						{
							for(i=0;i<inc;i++)
								ac[i].start();
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
			catch(Exception e)
			{
				e.printStackTrace();	
			}
		}
	}
}