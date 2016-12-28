import javax.swing.JOptionPane;
public class MyCluster
{
	int x[]={2,4,6,40,70,1000,2000};
	int y[]={6,8,100,50,3000,500,750};
	int k = 3,iteration_count= 2;
	int[][] xcluster,ycluster;
	int n;
	int[] incr;
	double[] xcentroid; 
	double[] ycentroid;
	public MyCluster()
	{
		n = x.length;
		incr = new int[k];
		xcentroid = new double[k];
		ycentroid = new double[k];
		cluster(x,y,k,iteration_count);
	}
	public void cluster(int[] x, int[] y, int k,int iteration_count)
	{
		//k must be less than or equal to the no. of points
		double[] dis = new double[k];
		int i,j,min;
		//initial centroids
		for(i=0;i<k;i++)
		{
			xcentroid[i] = x[i];
			ycentroid[i] = y[i];
		}
		for(int iteration=0;iteration<iteration_count;iteration++)
		{

			xcluster = new int[k][n];
			ycluster = new int[k][n];
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
			showCluster();
		}
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
		double xtotal = 0, ytotal = 0;
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
		}
	}
	public void showCluster()
	{
		String str="Cluster Information:\n";
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
		System.out.println();
	}
	public static void main(String args[])
	{
		MyCluster myCluster = new MyCluster();
	}
}