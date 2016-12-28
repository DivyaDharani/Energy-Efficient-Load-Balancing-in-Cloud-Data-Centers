import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.util.Logger;


public class PingAgent extends Agent {

	private Logger myLogger = Logger.getMyLogger(getClass().getName());

	private class WaitPingAndReplyBehaviour extends CyclicBehaviour {

		public WaitPingAndReplyBehaviour(Agent a) {
			super(a);
		}

		public void action() {
			ACLMessage  msg = myAgent.receive();
			if(msg != null){
				ACLMessage reply = msg.createReply();

				if(msg.getPerformative()== ACLMessage.REQUEST){
					String content = msg.getContent();
					if ((content != null) && (content.indexOf("ping") != -1)){
						myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Received PING Request from "+msg.getSender().getLocalName());
						reply.setPerformative(ACLMessage.INFORM);
						reply.setContent("pong");
					}
					else{
						myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected request ["+content+"] received from "+msg.getSender().getLocalName());
						reply.setPerformative(ACLMessage.REFUSE);
						reply.setContent("( UnexpectedContent ("+content+"))");
					}

				}
				else {
					myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected message ["+ACLMessage.getPerformative(msg.getPerformative())+"] received from "+msg.getSender().getLocalName());
					reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
					reply.setContent("( (Unexpected-act "+ACLMessage.getPerformative(msg.getPerformative())+") )");   
				}
				send(reply);
			}
			else {
				block();
			}
		}
	} // END of inner class WaitPingAndReplyBehaviour


	protected void setup() {
		// Registration with the DF 
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();   
		sd.setType("PingAgent"); 
		sd.setName(getName());
		sd.setOwnership("TILAB");
		dfd.setName(getAID());
		dfd.addServices(sd);
		try {
			DFService.register(this,dfd);
			WaitPingAndReplyBehaviour PingBehaviour = new  WaitPingAndReplyBehaviour(this);
			addBehaviour(PingBehaviour);
		} catch (FIPAException e) {
			myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
			doDelete();
		}
	}
}
