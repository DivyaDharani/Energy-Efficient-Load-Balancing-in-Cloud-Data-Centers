import java.util.*;
import javax.swing.*;
import jade.core.*;
import jade.wrapper.*;

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
	double cpu_usage, mem_usage;
	JTextArea logTextArea;
	boolean startMigration = false;
	int exec_time, extra_cpu_needed, extra_mem_needed;

	public VirtualMachine(int local_id,int server_id,String vma_name,int cpu_capacity,int mem_capacity, JTextArea logTextArea)
	{
		this.local_id = local_id;
		this.server_id = server_id;
		this.vma_name = vma_name; 
		this.cpu_capacity = cpu_capacity;
		this.mem_capacity = mem_capacity;
		this.logTextArea = logTextArea;
	}

	public void runMachine(final VMRequest vmrequest) //execution time in seconds
	{ 
		status = VirtualMachine.BUSY;
		cpu_occupied = vmrequest.cpu_capacity;
		mem_occupied = vmrequest.mem_capacity;
		extra_cpu_needed = vmrequest.extra_cpu;
		extra_mem_needed = vmrequest.extra_mem;
		exec_time = vmrequest.exec_time;

		final int extra_cpu_available = cpu_capacity - cpu_occupied;
		final int extra_mem_available = mem_capacity - mem_occupied;

		//randomizing the time (or just 75% of the execution time)within the execution time - to find 'when' the vm needs extra resources
		new java.util.Timer().schedule(new java.util.TimerTask(){
			public void run()
			{
				logTextArea.append("\n"+new Date()+" -> Extra resource (Virtual Cores = "+extra_cpu_needed+"; Memory = "+extra_mem_needed+") - needed by VM "+vma_name);
				if(extra_cpu_needed <= extra_cpu_available && extra_mem_needed <= extra_mem_available)
				{	
					//no migration required
					cpu_occupied += extra_cpu_needed;
					mem_occupied += extra_mem_needed;
					extra_cpu_needed = 0;
					extra_mem_needed = 0;
					new java.util.Timer().schedule(new java.util.TimerTask(){
						public void run()
						{
							logTextArea.append("\nExecution of VM "+vma_name+" for request ID : "+vmrequest.req_id+" completed");
							cpu_occupied = 0;
							mem_occupied = 0;
							status = VirtualMachine.FREE;
						}
					}, (int)(exec_time * 0.25 * 1000));
				} 
				else
				{
					//migration needed
					//checking if migration is already triggered due to server's load
					if(startMigration == false)
					{
						startMigration = true;
						logTextArea.append("\n"+new Date()+" -> MIGRATION TO BE TRIGGERED FOR "+vma_name);
						//remaining execution time
						exec_time = exec_time - (int)(exec_time * 0.75 * 1000);
					}
				}
			}
		}, (int)(exec_time * 0.75 * 1000)); 
	}
}