import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.*;
import javax.swing.*;
import jade.wrapper.*;
import java.io.*; 

public class ClusteringAgent extends Agent
{
	int sma = 12;
	int vma = 72;
	ArrayList<VirtualMachine> vmarray;
	int k = 3,iteration_count= 200;
	/*int[][] xcluster,ycluster;
	int[] x,y;
	int[] incr;
	double[] xcentroid,ycentroid;*/

	VMCluster[] vmcluster = new VMCluster[k];

	public void setup()
	{
		setEnabledO2ACommunication(true,0);
		addBehaviour(new ClusterBehaviour());
		addBehaviour(new RequestClustering());
		addBehaviour(new ClusterSender());
	}	

	public class ClusterBehaviour extends OneShotBehaviour //must be cyclic behaviour or TickerBehaviour
	{
		
		public void action()
		{
			collectDetails();
			/*incr = new int[k]; //initial values => '0' //increment variable for each Cluster
			xcentroid = new double[k];
			ycentroid = new double[k];*/
			cluster(k,iteration_count);
		}
		public void cluster(int k,int iteration_count)
		{
			//k must be less than or equal to the no. of points
			double[] dis = new double[k];
			int i,j,min;
			
			//initial centroids
			for(i=0;i<k;i++)
			{
				/*xcentroid[i] = x[i];
				ycentroid[i] = y[i];*/
				vmcluster[i] = new VMCluster();
				vmcluster[i].clusterID = i;
				vmcluster[i].xcentroid = (vmarray.get(i)).cpu_capacity;
				vmcluster[i].ycentroid = vmarray.get(i).mem_capacity;
			}
			for(int iteration=0;iteration<iteration_count;iteration++)
			{
				/*xcluster = new int[k][vma];
				ycluster = new int[k][vma];
				min = 0;
				for(i=0;i<k;i++)
					incr[i] = 0;
				for(i=0;i<x.length;i++) //point
				{
					for(j=0;j<k;j++) //centroid		
					{
						dis[j] = findDis(x[i],y[i],xcentroid[j],ycentroid[j]); //dis of point i with j centroid
						if(j == 0)
							min = j;
						else
						{
							if(dis[j] < dis[min])
								min = j;
						}
					}
					//put the point in cluster 'min'
					xcluster[min][incr[min]] = x[i];
					ycluster[min][incr[min]] = y[i];
					incr[min]++;
				}

				findCentroid();
				*/

				for(i=0;i<k;i++)
				{
					vmcluster[i].clearContents(); 
				}

				for(i=0;i<vmarray.size();i++) //vm (a point)
				{
					min = 0;
					for(j=0;j<k;j++) //centroid loop
					{
						dis[j] = findDis((vmarray.get(i)).cpu_capacity,(vmarray.get(i)).mem_capacity,vmcluster[j].xcentroid,vmcluster[j].ycentroid);
						if(j == 0)
							min = j;
						else
						{
							if(dis[j] < dis[min])
								min = j;
						}
					}
					vmcluster[min].add(vmarray.get(i));
				}
				findCentroid();
			}
			showCluster();
		}
		double findDis(int x1,int y1,double x2,double y2)
		{
			//Euclidean distance
			double a = Math.pow(x2-x1,2);
			double b = Math.pow(y2-y1,2);
			return Math.sqrt(a+b);
		}
		void findCentroid()
		{
			/*double xtotal = 0, ytotal = 0;
			xcentroid = new double[k];
			ycentroid = new double[k];
			// assuming x and y will be of same length 
			for(int i=0;i<k;i++) //a cluster
			{
				xtotal = 0;
				ytotal = 0;
				for(int j=0;j<incr[i];j++) //an element in cluster
				{
					xtotal += xcluster[i][j];
					ytotal += ycluster[i][j];
				}
				xcentroid[i] = xtotal / incr[i];
				ycentroid[i] = ytotal / incr[i];
				// System.out.println("Centroid "+(i+1)+" => ("+xcentroid[i]+","+ycentroid[i]+")");
			}*/

			double xtotal,ytotal;
			for(int i=0;i<k;i++) //a cluster
			{
				xtotal = 0;
				ytotal = 0;
				for(int j=0;j<vmcluster[i].getClusterLength();j++)
				{
					xtotal += vmcluster[i].get(j).cpu_capacity;
					ytotal += vmcluster[i].get(j).mem_capacity;
				}
				vmcluster[i].xcentroid = xtotal / vmcluster[i].getClusterLength();
				vmcluster[i].ycentroid = ytotal / vmcluster[i].getClusterLength();
			}
		}
		public void showCluster()
		{
			/*String str="Cluster Information:\n";
			for(int i=0;i<k;i++)
			{
				str+="\nCluster "+(i+1)+":";
				for(int j=0;j<incr[i];j++)
				{
					str+="\t("+xcluster[i][j]+","+ycluster[i][j]+")";
				}
				str += "\tElements: "+incr[i]+"\tCentroid: ("+xcentroid[i]+","+ycentroid[i]+")";
			}
			//JOptionPane.showMessageDialog(null,str);
			System.out.println(str);
			System.out.println();*/
		
			String str = "CLUSTER INFORMATION:";
			for(int i=0;i<k;i++)
			{
				str+="\nCluster "+(i+1)+":";
				for(int j=0;j<vmcluster[i].getClusterLength();j++)
				{
					str+="\t("+vmcluster[i].get(j).cpu_capacity+","+vmcluster[i].get(j).mem_capacity+")";
				}
				str += "\tElements: "+vmcluster[i].getClusterLength()+"\tCentroid: ("+vmcluster[i].xcentroid+","+vmcluster[i].ycentroid+")";
			}
			JOptionPane.showMessageDialog(null,str);
			System.out.println(str+"\n");
			try
			{
				File file = new File("logfile.txt");
				FileWriter fw = new FileWriter(file, true);
				fw.write("\n"+str+"\n");
				fw.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		void collectDetails()
		{
			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
			msg.setOntology("requesting-for-capacity");
			for(int i=1;i<=sma;i++)
			{
				msg.addReceiver(new AID("sma"+i,AID.ISLOCALNAME));
			}
			send(msg);
			System.out.println("requesting-for-capacity message -> from CA to all SMAs");
			/*int count = 0,i,j,k=0;
			Object obj;
			String[] strarr,vmcaparr;
			x = new int[vma];
			y = new int[vma];
			while(count < sma)
			{
				//first enable O2A communication to receive O2A object
				while((obj = getO2AObject())==null)
				{
					;
				}
				strarr = (String[])obj;
				for(i=0;i<strarr.length;i++)
				{
					vmcaparr = strarr[i].split(" ");
					x[k] = Integer.parseInt(vmcaparr[0]); //cpu capacity
					y[k++] = Integer.parseInt(vmcaparr[1]); //mem capacity
				}
				count++;
			}*/
			vmarray = new ArrayList<VirtualMachine>();
			int count = 0;
			Object obj;
			while(count < sma)
			{
				while((obj = getO2AObject()) == null)
					;
				if(obj.getClass().getSimpleName().equals("ArrayList"))
				{
					ArrayList<VirtualMachine> list = (ArrayList<VirtualMachine>)obj;
					for(int i=0;i<list.size();i++)
						vmarray.add(list.get(i));
					count++;
				}
			}

		}
		
	}

	class RequestClustering extends CyclicBehaviour
	{
		Object obj;
		public void action()
		{
			if((obj = getO2AObject()) != null)
			{
				VMRequest vmrequest = (VMRequest) obj;
				int cpu_capacity = vmrequest.cpu_capacity;
				int mem_capacity = vmrequest.mem_capacity;
				VMCluster vmcluster = clusterRequest(cpu_capacity,mem_capacity);
				try
				{
					jade.wrapper.AgentContainer agentContainer = getContainerController();
					AgentController agentController = agentContainer.getAgent(vmrequest.reply_to); //sending the response to the agent mentioned in 'vmrequest.reply_to' string parameter
					agentController.putO2AObject(vmcluster,false);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		
		public VMCluster clusterRequest(int cpu_capacity,int mem_capacity)
		{	
			double[] dis = new double[k];
			int min = 0;
			for(int i=0;i<k;i++) //for each cluster
			{
				dis[i] = findDis(cpu_capacity,mem_capacity,vmcluster[i].xcentroid,vmcluster[i].ycentroid);
				if(dis[i] < dis[min])
				{
					min = i;
				}
			}
			return vmcluster[min];
		}

		double findDis(int x1,int y1,double x2,double y2)
		{
			//Euclidean distance
			double a = Math.pow(x2-x1,2);
			double b = Math.pow(y2-y1,2);
			return Math.sqrt(a+b);
		}
	}

	class ClusterSender extends CyclicBehaviour
	{
		public void action()
		{
			MessageTemplate msgtemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST),MessageTemplate.MatchOntology("requesting-for-cluster"));
     			ACLMessage msg = receive(msgtemplate);
      			if(msg!=null)
      			{
      				JOptionPane.showMessageDialog(null,"'Requesting for cluster' message - received from "+msg.getSender().getLocalName());
      				String str = msg.getContent();
      				int requestedClusterID = Integer.parseInt(str);
      				try
      				{
      					jade.wrapper.AgentContainer agentContainer = getContainerController();
      					AgentController agentController = agentContainer.getAgent(msg.getSender().getLocalName());
      					agentController.putO2AObject(vmcluster[requestedClusterID], false);
      				}
      				catch(Exception e)
      				{
      					e.printStackTrace();
      				}
      			}
		}
	}
}