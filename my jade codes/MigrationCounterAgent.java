import jade.core.*;
import jade.core.behaviours.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import jade.wrapper.*;

public class MigrationCounterAgent extends Agent
{
	int sma_count = 12;
	int vm_count = 72;
	VirtualMachine[] vms;
	int prev_mig1_count = 0, cur_mig1_count = 0; //mig1_count => No. of migrations due to server overload
	int prev_mig2_count = 0, cur_mig2_count = 0; //mig2_count => No. of migrations due to insufficient capacity of VM
	int prev_mig3_count = 0, cur_mig3_count = 0; //mig3_count => No. of migrations due to server consolidation
	JLabel label1, label2, label3, label4;
	ServerMachine[] serverMachines;
	int prev_turnoff_count = 0, cur_turnoff_count = 0;

	public void setup()
	{
		Object[] args = getArguments();
		serverMachines = (ServerMachine[])args[0];

		setEnabledO2ACommunication(true, 0);
		vms = new VirtualMachine[vm_count];

		JFrame frame = new JFrame("MIGRATION COUNTER");
		frame.setSize(525, 250);
		frame.setLayout(null);
		label1 = new JLabel();
		label1.setFont(new Font("Serif", Font.PLAIN, 20));
		label1.setHorizontalAlignment(JLabel.CENTER);
		label1.setBounds(10, 25, 500, 50);
		label2 = new JLabel();
		label2.setFont(new Font("Serif", Font.PLAIN, 20));
		label2.setHorizontalAlignment(JLabel.CENTER);
		label2.setBounds(10, 55, 500, 50);
		label3 = new JLabel();
		label3.setFont(new Font("Serif", Font.PLAIN, 20));
		label3.setHorizontalAlignment(JLabel.CENTER);
		label3.setBounds(10, 85, 500, 50);
		frame.add(label1);
		frame.add(label2);
		frame.add(label3);
		frame.setVisible(true);
		label1.setText("No. of Migrations due to server overload : 0");
		label2.setText("No. of Migrations due to VM's insufficient capacity : 0");
		label3.setText("No. of Migrations due to server consolidation : 0");

		label4 = new JLabel("No. of servers turned off by server consolidation : 0");
		label4.setFont(new Font("Serif", Font.PLAIN, 20));
		label4.setHorizontalAlignment(JLabel.CENTER);		
		label4.setBounds(10, 115, 500, 50);
		frame.add(label4);

		addBehaviour(new VMInstanceGathering());
		addBehaviour(new MigrationCounter());
	}

	class VMInstanceGathering extends OneShotBehaviour
 	{
 		Object obj = null;
 		int i, j;
 		int vm_counter = 0;
 		ArrayList<ArrayList<VirtualMachine>> arrayofarray = new ArrayList<ArrayList<VirtualMachine>>();
 		public void action()
 		{
 			i = 0;
 			while(i < sma_count)
 			{
 				while((obj = getO2AObject()) == null)
 					; 	
 				if(obj.getClass().getSimpleName().equals("ArrayList"))
 				{			
 					arrayofarray.add((ArrayList<VirtualMachine>)obj); 			
 					i++;
 				}
 			}
 			for(i = 0; i < sma_count; i++)
 			{
 				for(j = 0; j < arrayofarray.get(i).size(); j++)
 				{
 					vms[vm_counter++] = arrayofarray.get(i).get(j);
 				}
 			}

 			try
 			{
 				//sending VM array vms[] to UA (to reset migration count for every set of requests)
 				jade.wrapper.AgentContainer agentContainer = getContainerController();
  				AgentController agentController = agentContainer.getAgent("ua");
      			agentController.putO2AObject(vms, false);
      		}
      		catch(Exception e)
      		{
      			e.printStackTrace();	
      		}

 			selectHostSelectionAlgo();
 		}
 	}

	public void selectHostSelectionAlgo()
	{
		JFrame frame = new JFrame("Host Selection Algo Selector");
		frame.setSize(500, 200);
		frame.setVisible(true);
		JButton firstfit_button = new JButton("First Fit");
		firstfit_button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				for(int i = 0; i < vm_count; i++)
				{
					vms[i].host_selection_algo = "first-fit";
				}
			}
		});
		JButton bestfit_button = new JButton("Best Fit");
		bestfit_button.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e)
			{
				for(int i = 0; i < vm_count; i++)
				{
					vms[i].host_selection_algo = "best-fit";
				}
			}
		});
		frame.setLayout(new FlowLayout());
		firstfit_button.setPreferredSize(new Dimension(150,150));
		bestfit_button.setPreferredSize(new Dimension(150,150));
		frame.add(firstfit_button);
		frame.add(bestfit_button);
	}

	class MigrationCounter extends CyclicBehaviour
	{
		public void action()
		{
			cur_mig1_count = 0;
			cur_mig2_count = 0;
			cur_mig3_count = 0;

			for(int i = 0; i < vm_count; i++)
			{
				cur_mig1_count += vms[i].mig_for_server_overload_count; 
				cur_mig2_count += vms[i].mig_for_insuff_capacity_count;
				cur_mig3_count += vms[i].mig_for_server_consldtn_count;
			}
			if(cur_mig1_count != prev_mig1_count)	
			{
				label1.setText("No. of Migrations due to server overload : "+cur_mig1_count);
				prev_mig1_count = cur_mig1_count;
			}
			if(cur_mig2_count != prev_mig2_count)	
			{
				label2.setText("No. of Migrations due to VM's insufficient capacity : "+cur_mig2_count);
				prev_mig2_count = cur_mig2_count;
			}
			if(cur_mig3_count != prev_mig3_count)	
			{
				label3.setText("No. of Migrations due to server consolidation : "+cur_mig3_count);
				prev_mig3_count = cur_mig3_count;
			}

			//No. of servers turned off due to server consolidation
			cur_turnoff_count = 0;
			for(int i = 0; i < sma_count; i++)
			{
				cur_turnoff_count += serverMachines[i].turnoff_count;
			}
			if(cur_turnoff_count != prev_turnoff_count)
			{
				label4.setText("No. of servers turned off by server consolidation : "+cur_turnoff_count);
				prev_turnoff_count = cur_turnoff_count;
			}
		}
	}
}