package org.emonocot.portal.ws.ncbi;

import java.rmi.RemoteException;

import org.emonocot.portal.controller.form.NcbiDto;
import org.emonocot.portal.ws.ncbi.EUtilsServiceStub.EGqueryRequest;
import org.emonocot.portal.ws.ncbi.EUtilsServiceStub.Result;
import org.emonocot.portal.ws.ncbi.EUtilsServiceStub.ResultItemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NcbiService {
	private static Logger logger = LoggerFactory.getLogger(NcbiService.class);
	
	private EUtilsServiceStub service;
	
	private String httpProxyHost;
	
	private Integer httpProxyPort;	
	
	public void setHttpProxyHost(String httpProxyHost) {
		this.httpProxyHost = httpProxyHost;
	}

	public void setHttpProxyPort(Integer httpProxyPort) {
		this.httpProxyPort = httpProxyPort;
	}

	public void afterPropertiesSet() throws Exception {
		this.service = new EUtilsServiceStub(httpProxyHost, httpProxyPort);
	}
	
	public NcbiDto issueRequest(final String term) throws RemoteException {
		if(term == null || term.equals("")) {
			return new NcbiDto();
		}
		EGqueryRequest eGqueryRequest = new EGqueryRequest();
		eGqueryRequest.setTerm(term + "[orgn]");		
	    
	    Result result = service.run_eGquery(eGqueryRequest);
		
		NcbiDto ncbiDto = new NcbiDto();
		for(ResultItemType resultItem : result.getEGQueryResult().getResultItem()) {
			switch(resultItem.getDbName()) {
			case "pubmed":
				ncbiDto.setPubMedEntries(resultItem.getCount());
				break;
			case "nuccore":
				ncbiDto.setNucleotideEntries(resultItem.getCount());
				break;
			case "protein":
				ncbiDto.setProtienEntries(resultItem.getCount());
				break;
			default:
				break;
			}
		}		
		
		return ncbiDto;
	}

}