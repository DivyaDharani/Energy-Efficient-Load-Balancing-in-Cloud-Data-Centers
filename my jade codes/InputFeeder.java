import jade.core.*;
import jade.core.behaviours.*;

public class InputFeeder extends Agent
{
	public void setup()
	{
		addBehaviour(new MyBehaviour());
	}

	public class MyBehaviour extends OneShotBehaviour
	{
		public void action()
		{

		}
	} 	
}