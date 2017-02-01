public class VMRequest
{
	int req_id;
	int cpu_capacity; //no. of virtual cores
	int mem_capacity;
	int exec_time;
	int extra_cpu;
	int extra_mem;
	String reply_to = "";
	public VMRequest(int req_id, int cpu_capacity, int mem_capacity, int exec_time, int extra_cpu, int extra_mem)
	{
		this.req_id = req_id;
		this.cpu_capacity = cpu_capacity;
		this.mem_capacity = mem_capacity;
		this.exec_time = exec_time;
		this.extra_cpu = extra_cpu;
		this.extra_mem = extra_mem;
	}
}