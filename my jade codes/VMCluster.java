import java.util.*;
public class VMCluster
{
	int clusterID;
	ArrayList<VirtualMachine> cluster = new ArrayList<VirtualMachine>();
	
	public double xcentroid,ycentroid;

	public void add(VirtualMachine vm)
	{
		cluster.add(vm);
	}

	public void set(int index, VirtualMachine vm)
	{
		cluster.set(index,vm);
	}

	public VirtualMachine get(int i)
	{
		return cluster.get(i);
	}

	public void clearContents()
	{
		cluster.clear();
	}
	public int getClusterLength()
	{
		return cluster.size();
	}
	public boolean isEmpty()
	{
		return cluster.isEmpty();
	}

	public boolean checkAllocationPossibility(int cpu_capacity, int mem_capacity)
	{
		VirtualMachine vm;
		for(int i = 0; i < cluster.size(); i++)
		{
			vm = cluster.get(i);
			if(vm.cpu_capacity >= cpu_capacity && vm.mem_capacity >= mem_capacity)
			{
				return true;
			}
		}
		return false;
	} 
}
