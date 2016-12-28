import java.util.*;
import javax.swing.*;
public class VirtualMachine
{
	int local_id,server_id;
	int cpu_capacity,mem_capacity;
	public static int BUSY = 1, FREE = 0;
	int status = VirtualMachine.FREE;
	int cpu_occupied,mem_occupied;
	int service_time;
	int VMID,SMAID;
	String vma_name;
	int cpu_weight, mem_weight, total_weight;

	public VirtualMachine(int local_id,int server_id,String vma_name,int cpu_capacity,int mem_capacity)
	{
		this.local_id = local_id;
		this.server_id = server_id;
		this.vma_name = vma_name; 
		this.cpu_capacity = cpu_capacity;
		this.mem_capacity = mem_capacity;
	}

	public void runMachine(int time) //time in seconds
	{
		status = VirtualMachine.BUSY;
		new java.util.Timer().schedule(new java.util.TimerTask(){
			public void run()
			{
				JOptionPane.showMessageDialog(null,"Execution of VM "+vma_name+" completed");				
				status = VirtualMachine.FREE;
			}
		}, time * 1000);
	}
}