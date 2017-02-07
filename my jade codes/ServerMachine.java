public class ServerMachine
{
	int ID, num_of_vms, total_cpu, total_mem;
	double cpu_load_threshold, mem_load_threshold;
	int cpu_load, mem_load, cpu_load_activation_threshold, mem_load_activation_threshold, cpu_load_activation_count, mem_load_activation_count;
	double cpu_load_percentage, mem_load_percentage, cpu_load_threshold_percentage, mem_load_threshold_percentage; 

	public ServerMachine(int ID, int num_of_vms, int total_cpu, int total_mem)
	{
		this.ID = ID;
		this.num_of_vms = num_of_vms;
		this.total_cpu = total_cpu;
		this.total_mem = total_mem;
	}

}