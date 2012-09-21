package de.dpunkt.myaktion.services;

import java.util.List;
import java.util.logging.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.xml.ws.WebServiceRef;

import de.dpunkt.myaktion.model.Aktion;
import de.dpunkt.myaktion.model.Spende;
import de.dpunkt.myaktion.model.Spende.Status;
import de.dpunkt.myaktion.monitor.ws.SpendeDelegatorService;

@Stateless
public class SpendeServiceBean implements SpendeService {

	@Inject
	EntityManager entityManager;
	
	@Inject
	private Logger logger;
	
	@WebServiceRef(wsdlLocation="http://localhost:8080/my-aktion-monitor-1.0-SNAPSHOT/SpendeDelegatorService?wsdl")
	private SpendeDelegatorService delegatorService;
	
	@RolesAllowed("Organisator")
	public List<Spende> getSpendeList(Long aktionId) {
		Aktion managedAktion = entityManager.find(Aktion.class, aktionId);
		List<Spende> spenden = managedAktion.getSpenden();
		spenden.size(); // Wg. JPA Field-Access müssen wir eine Methode auf dem spenden Objekt aufrufen
		return spenden;
	}

	@PermitAll
	public void addSpende(Long aktionId, Spende spende) {
		// Spende an Glassfish über JAX-WS senden
		try {
			delegatorService.getSpendeDelegatorPort().receiveSpende(spende);
		} catch(RuntimeException e) {
			logger.severe("Spende konnte nicht an Glassfish weitergeleitet werden. Läuft der Glassfish?");
		}
		// Spende in lokaler Datenbank hinzufügen
		Aktion managedAktion = entityManager.find(Aktion.class, aktionId);
		spende.setAktion(managedAktion);
		entityManager.persist(spende);
	}
	
	@PermitAll
    public void transferSpende(){
    	logger.info("Zu bearbeitende Spenden werden überwiesen.");
    	TypedQuery<Spende> query = entityManager.createNamedQuery(Spende.findWorkInProcess, Spende.class);
    	query.setParameter("status", Status.IN_BEARBEITUNG);
		List<Spende> spenden = query.getResultList();
		int count = 0;
    	for (Spende spende : spenden) {
			spende.setStatus(Status.UEBERWIESEN);
			count++;
		}
    	logger.info("Es wurden " + count + " Spenden überwiesen.");
    }

}
