import java.util.*;
public class VMCluster
{
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
}
