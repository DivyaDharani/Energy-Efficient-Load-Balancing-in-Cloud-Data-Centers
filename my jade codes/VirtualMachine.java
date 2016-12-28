import java.util.*;
import javax.swing.*;
public class VirtualMachine
{
	int local_id,server_id;
	int cpu_capacity,mem_capacity;
	public static int BUSY = 1, FREE = 0;
	int status = VirtualMachine.FREE;
	int cpu_occupied = 0,mem_occupied = 0; 
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

	public void runMachine(VMRequest vmrequest) //time in seconds
	{ 
		status = VirtualMachine.BUSY;
		cpu_occupied = vmrequest.cpu_capacity;
		mem_occupied = vmrequest.mem_capacity;

		new java.util.Timer().schedule(new java.util.TimerTask(){
			public void run()
			{
				JOptionPane.showMessageDialog(null,"Execution of VM "+vma_name+" completed");				
				cpu_occupied = 0;
				mem_occupied = 0;
				status = VirtualMachine.FREE;
			}
		}, vmrequest.exec_time * 1000);
	}
}