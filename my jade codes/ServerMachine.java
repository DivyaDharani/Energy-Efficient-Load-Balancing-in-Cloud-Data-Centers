public class ServerMachine
{
	int ID, num_of_vms, total_cpu, total_mem;
	double cpu_load_threshold, mem_load_threshold, cpu_energy_threshold, mem_energy_threshold;
	int cpu_load, mem_load, cpu_load_activation_threshold, mem_load_activation_threshold, cpu_load_activation_count, mem_load_activation_count;
	double cpu_load_percentage, mem_load_percentage, cpu_load_threshold_percentage, mem_load_threshold_percentage; 
	double cpu_energy_threshold_percentage, mem_energy_threshold_percentage; 

	boolean migration_triggered = false;
	public static int NOT_UTILIZED = 0, UNDER_UTILIZED = 1, NORMALLY_UTILIZED = 2, OVER_UTILIZED = 3;
	int status;
	public ServerMachine(int ID, int num_of_vms, int total_cpu, int total_mem)
	{
		this.ID = ID;
		this.num_of_vms = num_of_vms;
		this.total_cpu = total_cpu;
		this.total_mem = total_mem;
		status = ServerMachine.NOT_UTILIZED;
	}
}