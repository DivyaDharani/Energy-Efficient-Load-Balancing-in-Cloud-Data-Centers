import jade.core.Agent;
import jade.core.AID.*;
import jade.core.*;
import jade.wrapper.*;
import jade.core.behaviours.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
public class SampleAgent extends Agent
{
	public void setup()
	{
		System.out.println(getLocalName()+" at "+getAID().getName() + "started");
		addBehaviour(new MyOneShotBehaviour());
		// addBehaviour(new MyWakerBehaviour(this,6000));
		try
		{
			ContainerController container = getContainerController();
			AgentController agent_controller = container.createNewAgent("ua","UserAgent",null);
			agent_controller.start();
		}
		catch(Exception e)
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			JOptionPane.showMessageDialog(null,sw.toString());
		}
		/* jade.wrapper.AgentContainer c = getContainerController();
        try {
            AgentController a = c.createNewAgent( "fa", "FrontEndAgent", null );
            a.start();
        }
        catch (Exception e){}
		*/
	}
}

class MyOneShotBehaviour extends OneShotBehaviour
{
	public void action()
	{
		JFrame frame = new JFrame();
		frame.setSize(400,500);
		frame.setVisible(true);
		
	}
}

/*class MyWakerBehaviour extends WakerBehaviour
{
	public MyWakerBehaviour(Agent agent, long millis)
	{
		super(agent,millis);
	}
	public final void onWake()
	{
		JOptionPane.showMessageDialog(null,"I'm being executed after a specified time");
	}
}*/

