package agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import behaviours.CheckProposal;
import behaviours.CheckUnassigned;
//import behaviours.CheckUpdate;
import behaviours.FinishAssignment;
import behaviours.SendCfp;
import behaviours.SendFeedback;
import commons.Aircraft;
import commons.Flight;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.util.Logger;

public class TasAgent extends Agent {

	private static final long serialVersionUID = 2923242081277157616L;
	private final Logger logger = Logger.getMyLogger(getClass().getName());

	public static final String AGENT_NAME = "tas";
	// Triggers
	public static final int CONTAINS_UNASSIGNED = 1;
	public static final int ALL_ASSIGNED = 2;
	public static final int NO_PROPOSAL = 3;
	public static final int PROPOSAL = 4;

	public static int ACFT_QTY = 0;

	// States
	private static final String CHECK_ASSIGNMENT = "Check_for_unassigned";
	private static final String FINISH_ASSIGNMENT = "Finish_Bid";
	private static final String SEND_CFP = "Send_CFP";
	private static final String CHECK_PROPOSAL = "Check_Proposal";
	private static final String SEND_FEEDBACK = "SEND_FEEDBACK";
	private static final String CHECK_UPDATE ="CHECK_UPDATE";
	// Datastore keys
	public static final String KEY_WIN_PROPOSAL = "WinnerProposal";
	public static final String KEY_BID_INCREMENT = "BidIncrement";
	public static final String KEY_MAX_UTILITY = "MaxUtility";
	public static final String KEY_CURRENT_UNASSIGNED = "CurrentUnassigned";
	public static final String KEY_ASSIGNMENT = "Assignment";
	public static final String FLIGHT_UNASSIGNED = "Unassigned";
	public static final String KEY_PROPONENT_LIST = "ProponentList";

	private FSMBehaviour m_fsm;

	private HashMap<Flight, String> m_assignment = new HashMap<Flight, String>();
	private ArrayList<AID> m_recList = new ArrayList<AID>();
	
	@Override
	protected void setup() {
		super.setup();

		// Configura o nome local do agente
		getAID().setLocalName(AGENT_NAME);
		logger.info("Starting up " + getLocalName());

		
		Object[] objetoArgs = new Object[2];
		objetoArgs = this.getArguments();

		List<Flight> listaFlights = new ArrayList<Flight>();
		listaFlights = (List<Flight>) objetoArgs[0];
		for (int i = 0; i < listaFlights.size(); i++) {
			Flight flight = listaFlights.get(i);
			m_assignment.put(flight, TasAgent.FLIGHT_UNASSIGNED);
		}

		
		List<Aircraft> listaAircrafts = new ArrayList<Aircraft>();
		listaAircrafts = (List<Aircraft>) objetoArgs[1];
		ACFT_QTY = listaAircrafts.size();
		for (int i = 0; i < listaAircrafts.size(); i++) {
			Aircraft aircraft = listaAircrafts.get(i);
			m_recList.add(new AID(aircraft.getId().toString(), AID.ISLOCALNAME));
		}

		m_fsm = new FSMBehaviour();

		// REGISTER STATES
		m_fsm.registerFirstState(new CheckUnassigned(m_assignment), CHECK_ASSIGNMENT);

		Behaviour sndCFP = new SendCfp(m_assignment, m_recList);
		sndCFP.setDataStore(m_fsm.getDataStore());
		m_fsm.registerState(sndCFP, SEND_CFP);

		m_fsm.registerLastState(new FinishAssignment(m_assignment), FINISH_ASSIGNMENT);

		Behaviour chkProposal = new CheckProposal(m_assignment);
		chkProposal.setDataStore(m_fsm.getDataStore());
		m_fsm.registerState(chkProposal, CHECK_PROPOSAL);

		Behaviour sndFdbck = new SendFeedback();
		sndFdbck.setDataStore(m_fsm.getDataStore());
		m_fsm.registerState(sndFdbck, SEND_FEEDBACK);
		
		//Behaviour chkUpd = new CheckUpdate();
		//chkUpd.setDataStore(m_fsm.getDataStore());
		//m_fsm.registerState(chkUpd, CHECK_UPDATE);
		
		
		// REGISTER TRANSITIONS
		m_fsm.registerTransition(CHECK_ASSIGNMENT, FINISH_ASSIGNMENT, ALL_ASSIGNED);
		m_fsm.registerTransition(CHECK_ASSIGNMENT, SEND_CFP, CONTAINS_UNASSIGNED);
		m_fsm.registerDefaultTransition(SEND_CFP, CHECK_PROPOSAL);
		m_fsm.registerTransition(CHECK_PROPOSAL, SEND_FEEDBACK, PROPOSAL);
		m_fsm.registerTransition(CHECK_PROPOSAL, CHECK_ASSIGNMENT, NO_PROPOSAL);
		m_fsm.registerDefaultTransition(SEND_FEEDBACK, CHECK_UPDATE);
		m_fsm.registerDefaultTransition(CHECK_UPDATE, CHECK_ASSIGNMENT);
		addBehaviour(m_fsm);
	}

	@Override
	protected void takeDown() {
		super.takeDown();
		logger.info("Taking down " + getLocalName());
	}

}
