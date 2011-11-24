package org.emonocot.checklist.controller;

import java.text.ParseException;
import java.util.List;

import org.emonocot.api.TaxonService;
import org.emonocot.checklist.format.ChecklistIdentifierFormatter;
import org.emonocot.checklist.format.annotation.ChecklistIdentifierFormat;
import org.emonocot.checklist.logging.LoggingConstants;
import org.emonocot.model.pager.Page;
import org.emonocot.model.taxon.Family;
import org.emonocot.model.taxon.Rank;
import org.emonocot.model.taxon.Taxon;
import org.emonocot.model.taxon.TaxonomicStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 *
 * @author ben
 *
 */
@Controller
@RequestMapping("/endpoint")
public class ChecklistWebserviceController {
    /**
     *
     */
    private static final String CHECKLIST_WEBSERVICE_SEARCH_TYPE = "te";

   /**
    * Logger for debugging requests, errors etc.
    */
   private static Logger logger
       = LoggerFactory.getLogger(ChecklistWebserviceController.class);

  /**
   * Querylog for logging requests for reporting etc.
   */
  private static Logger queryLog = LoggerFactory.getLogger("query");

    /**
     *
     */
    private TaxonService taxonService;

    /**
     *
     * @param taxonService Set the taxon dao to use.
     */
    @Autowired
    public final void setTaxonDao(final TaxonService taxonService) {
        this.taxonService = taxonService;
    }

    /**
     * Simple method that allows the client to determine whether the service is
     * up and running.
     *
     * @return An empty ModelAndView with the view name "rdfResponse".
     */
    @RequestMapping(method = RequestMethod.GET, params = {"!function" })
    public final ModelAndView ping() {
        logger.debug("ping");
        return new ModelAndView("rdfResponse");
    }

    /**
     * Method which searches for taxon objects who's names match the search term
     * exactly.
     *
     * @param search A taxon name to search the database for.
     * @return a list of taxon objects (stored under the key 'result')
     */
    @RequestMapping(method = RequestMethod.GET, params = { "function=search",
            "search" })
    public final ModelAndView search(
            @RequestParam(value = "search", required = true)
            final String search) {
        logger.debug("search");
        if (search.endsWith("aceae")) {
            try {
                Family family = Family.valueOf(search);
                Integer numberOfGenera = taxonService.countGenera(family);
                ModelAndView modelAndView
                    = new ModelAndView("rdfFamilyResponse");
                modelAndView.addObject("family", family);
                modelAndView.addObject("numberOfGenera", numberOfGenera);
                try {
                    MDC.put(LoggingConstants.SEARCH_TYPE_KEY,
                            CHECKLIST_WEBSERVICE_SEARCH_TYPE);
                    MDC.put(LoggingConstants.QUERY_KEY, search);
                    MDC.put(LoggingConstants.RESULT_COUNT_KEY,
                            Integer.toString(numberOfGenera));
                    queryLog.info("ChecklistWebserviceController.get");
                } finally {
                    MDC.remove(LoggingConstants.SEARCH_TYPE_KEY);
                    MDC.remove(LoggingConstants.QUERY_KEY);
                    MDC.remove(LoggingConstants.RESULT_COUNT_KEY);
                }
                return modelAndView;
            } catch (IllegalArgumentException iae) {
                // do nothing
            }
        }
        ModelAndView modelAndView = new ModelAndView("rdfResponse");
        Page<Taxon> taxa = taxonService.search(search, null, null, null, null,
                null, null, "taxon-with-related");
        modelAndView.addObject("result", taxa.getRecords());
        try {
            MDC.put(LoggingConstants.SEARCH_TYPE_KEY,
                    CHECKLIST_WEBSERVICE_SEARCH_TYPE);
            MDC.put(LoggingConstants.QUERY_KEY, search);
            MDC.put(LoggingConstants.RESULT_COUNT_KEY,
                    Integer.toString(taxa.getSize()));
            queryLog.info("ChecklistWebserviceController.get");
        } finally {
            MDC.remove(LoggingConstants.SEARCH_TYPE_KEY);
            MDC.remove(LoggingConstants.QUERY_KEY);
            MDC.remove(LoggingConstants.RESULT_COUNT_KEY);
        }
        return modelAndView;
    }

    /**
     * Method which returns a single taxon object with the specified identifier.
     * @param id The identifier of the taxon to be retrieved.
     * @return a taxon objects (stored under the key 'result')
     */
    @RequestMapping(method = RequestMethod.GET, params = {
            "function=details_tcs", "id" })
    public final ModelAndView get(
            @ChecklistIdentifierFormat
            @RequestParam(value = "id")
            final Long id) {
        logger.debug("get");
        ModelAndView modelAndView = new ModelAndView();
        if (id < 0) {
            modelAndView.setViewName("tcsXmlFamilyResponse");
            Family family = Family.fromIdentifier(id);
            List<Taxon> children = taxonService.getGenera(family);
            modelAndView.addObject("children", children);
            modelAndView.addObject("family", family);
            modelAndView.addObject("id", id * -1);
            try {
                MDC.put(LoggingConstants.SEARCH_TYPE_KEY,
                        CHECKLIST_WEBSERVICE_SEARCH_TYPE);
                MDC.put(LoggingConstants.QUERY_KEY, family.name());
                MDC.put(LoggingConstants.RESULT_COUNT_KEY,
                        Integer.toString(children.size()));
                queryLog.info("ChecklistWebserviceController.get");
            } finally {
                MDC.remove(LoggingConstants.SEARCH_TYPE_KEY);
                MDC.remove(LoggingConstants.QUERY_KEY);
                MDC.remove(LoggingConstants.RESULT_COUNT_KEY);
            }
        } else {
            modelAndView.setViewName("tcsXmlResponse");
            modelAndView.addObject("id", id);
            Taxon taxon = taxonService.load(ChecklistIdentifierFormatter.IDENTIFIER_PREFIX + id, "taxon-with-related");
            if(taxon.getStatus() != null && taxon.getStatus().equals(TaxonomicStatus.accepted)) {
                // This taxon is accepted
                // Due to the fact that family records are not present, a dummy reference to the parent taxon must
                // be added for accepted genera
                if(taxon.getParent() == null && (taxon.getRank() != null && taxon.getRank().equals(Rank.GENUS))) {
                    Taxon parent = new Taxon();
                    Family family = Family.valueOf(taxon.getFamily());
                    parent.setName(taxon.getFamily());
                    parent.setRank(Rank.FAMILY);
                    parent.setIdentifier(family.getIdentifier());
                    parent.setId(id);
                    taxon.setParent(parent);
                }
            }
            modelAndView.addObject("result", taxon);
            try {
                MDC.put(LoggingConstants.SEARCH_TYPE_KEY,
                        CHECKLIST_WEBSERVICE_SEARCH_TYPE);
                MDC.put(LoggingConstants.QUERY_KEY, id.toString());
                MDC.put(LoggingConstants.RESULT_COUNT_KEY, "1");
                queryLog.info("ChecklistWebserviceController.get");
            } finally {
                MDC.remove(LoggingConstants.SEARCH_TYPE_KEY);
                MDC.remove(LoggingConstants.QUERY_KEY);
                MDC.remove(LoggingConstants.RESULT_COUNT_KEY);
            }
        }
        return modelAndView;
    }

    /**
     * TaxonDao will throw a (wrapped) UnresolvableObjectException if the client
     * provides an invalid identifier. This method returns a HTTP 400 BAD
     * REQUEST status code and a short message
     *
     * @param exception
     *            The exception just thrown (because of an invalid id)
     * @return A model and view with the name exception and the exception stored
     *         in the model under the key 'exception'
     */
    @ExceptionHandler({ DataRetrievalFailureException.class,
        ParseException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public final ModelAndView handleInvalidTaxonIdentifier(
            final DataRetrievalFailureException exception) {
        logger.debug("exception");
        ModelAndView modelAndView = new ModelAndView("exception");
        modelAndView.addObject("exception", exception);
        return modelAndView;
    }
}