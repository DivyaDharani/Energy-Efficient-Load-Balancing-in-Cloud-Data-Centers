public class VMRequest
{
	int cpu_capacity; //(no. of virtual cores)
	int mem_capacity;
	int exec_time;
	int extracpu;
	int extramem;

	public VMRequest(int cpu_capacity, int mem_capacity, int exec_time, int extracpu, int extramem)
	{
		this.cpu_capacity = cpu_capacity;
		this.mem_capacity = mem_capacity;
		this.exec_time = exec_time;
		this.extracpu = extracpu;
		this.extramem = extramem;
	}
}