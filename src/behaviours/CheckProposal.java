package behaviours;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import agents.TasAgent;
import commons.Flight;
import commons.Proposal;
import jade.core.behaviours.DataStore;
import jade.core.behaviours.SimpleBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;

public class CheckProposal extends SimpleBehaviour {

	/**
	 * Behaviour do agente TasAgent que e responsavel por receber propostas
	 * (Refuse e Propose) e escolher a melhor proposta
	 */
	private static final long serialVersionUID = -2239480822286081170L;

	private boolean m_finished;
	private int m_proposal_counter = 0;
	private MessageTemplate m_op1 = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
	private MessageTemplate m_op2 = MessageTemplate.MatchPerformative(ACLMessage.REFUSE);
	private MessageTemplate m_mt = MessageTemplate.or(m_op1, m_op2);
	private List<ACLMessage> m_proposeList = new ArrayList<ACLMessage>(TasAgent.ACFT_QTY);
	private List<Double> m_bidList = new ArrayList<Double>();
	private List<Flight> m_winRoute = new ArrayList<Flight>();

	private ACLMessage m_winnerProp;
	private HashMap<Flight, String> m_assignment;

	private final Logger m_logger = Logger.getMyLogger(getClass().getName());

	private int m_proposalStatus;

	public CheckProposal(HashMap<Flight, String> p_assignment) {
		m_assignment = p_assignment;
	}

	@Override
	public void action() {
		m_finished = false;
		ACLMessage v_aclMsgProposal = myAgent.receive(m_mt);
		
		
		if (v_aclMsgProposal != null) {
			m_proposal_counter++;

			if (v_aclMsgProposal.getPerformative() == ACLMessage.PROPOSE) {
				m_proposeList.add(v_aclMsgProposal);
			}

		} else {
			block();
		}

		if (m_proposal_counter == TasAgent.ACFT_QTY) {
			if (m_proposeList.isEmpty()) {
				m_finished = true;
				m_proposal_counter = 0;
				
				m_proposalStatus = TasAgent.NO_PROPOSAL;
				m_logger.info("NO PROPOSAL");
				
			} else {
				m_finished = true;
				m_proposal_counter = 0;				
				double v_max = Double.NEGATIVE_INFINITY;
				DataStore v_ds = getDataStore();
				
				for (ACLMessage aclMessage : m_proposeList) {
					try {
						Proposal v_prop = (Proposal) aclMessage.getContentObject();
						//Double v_price = v_prop.getPrice();
						//double v_bid = v_assignmentValue - v_price;
						double v_bid = v_prop.getPrice();
						m_bidList.add(v_bid);

						if (v_bid > v_max) {
							v_max = v_bid;
							m_winnerProp = aclMessage;
							m_winRoute  = v_prop.getRoute();
							
						}
					} catch (UnreadableException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}				

				}

				Collections.sort(m_bidList, Collections.reverseOrder());
				// Max value at current prices
				v_ds.put(TasAgent.KEY_MAX_UTILITY, m_bidList.get(0));
				m_logger.info(TasAgent.KEY_MAX_UTILITY + " => " + m_bidList.get(0));
				
				// Difference between First and Second max value at current
				// prices
				if(m_bidList.size() > 1){
					v_ds.put(TasAgent.KEY_BID_INCREMENT, m_bidList.get(0) - m_bidList.get(1));
				}else{
					v_ds.put(TasAgent.KEY_BID_INCREMENT, m_bidList.get(0) - m_bidList.get(0));
				}				
				m_logger.info(TasAgent.KEY_BID_INCREMENT + " => " + v_ds.get(TasAgent.KEY_BID_INCREMENT));
				
				// WinnerProposal
				v_ds.put(TasAgent.KEY_WIN_PROPOSAL, m_winnerProp);
				m_logger.info(TasAgent.KEY_WIN_PROPOSAL + " => " + m_winnerProp.getSender().getLocalName());
				
				
				// List of proponents
				v_ds.put(TasAgent.KEY_PROPONENT_LIST, m_proposeList);
				
				//Remove another assignment involving winner proponent
				for (Entry<Flight, String> v_winAssign : m_assignment.entrySet()){
					if(v_winAssign.getValue().equals(
							m_winnerProp.getSender().getLocalName())){
						
						m_assignment.put(v_winAssign.getKey(), TasAgent.FLIGHT_UNASSIGNED);
						
					}
				}
				
				//Update assignment set with new assignment involving winner proponent
				for (Flight flt : m_winRoute) {
					m_assignment.put(flt, 
							m_winnerProp.getSender().getLocalName());
				}
				
				
								
				m_proposalStatus = TasAgent.PROPOSAL;				
				m_bidList.clear();
				m_winRoute.clear();
				
				
			}
		}

	}
	
	@Override
	public int onEnd() {
		return m_proposalStatus;
	}
	@Override
	public boolean done() {
		if (m_finished)
			m_logger.info("CHECK PROPOSAL HAS FINISHED");

		return m_finished;
	}

}
