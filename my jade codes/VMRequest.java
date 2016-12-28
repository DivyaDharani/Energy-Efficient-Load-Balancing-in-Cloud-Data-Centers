public class VMRequest
{
	int cpu_capacity; //(no. of virtual cores)
	int mem_capacity;
	int exec_time;

	public VMRequest(int cpu_capacity, int mem_capacity, int exec_time)
	{
		this.cpu_capacity = cpu_capacity;
		this.mem_capacity = mem_capacity;
		this.exec_time = exec_time;
	}
}