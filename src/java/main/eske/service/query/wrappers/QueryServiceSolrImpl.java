package eske.service.query.wrappers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import esg.search.query.api.SearchInput;
import esg.search.query.api.SearchOutput;
import esg.search.query.api.SearchService;
import esg.search.query.impl.solr.SearchInputImpl;
import esg.search.query.impl.solr.SolrXmlPars;
import eske.model.query.QueryInput;
import eske.model.query.QueryOutput;
import eske.model.query.QueryResult;
import eske.service.query.api.QueryException;
import eske.service.query.api.QueryService;

/**
 * Implementation of ESKE {@link QueryService} based on Apache-Solr back-end.
 */
public class QueryServiceSolrImpl implements QueryService {
	
	/**
	 * The underlying {@link SearchService} to which all functionality is delegated.
	 */
	private final SearchService searchService;
	
	/**
	 * Factory to translate search input instances.
	 */
	private final InputFactory inputFactory;
	
	/**
	 * Factory to translate search output instances.
	 */
	private final OutputFactory outputFactory;
	
	/**
	 * Factory to translate facet instances.
	 */
	private final FacetFactory facetFactory;
	
	/**
	 * Utility class to translate facet and product keys.
	 */
	private final MappingUtil mappingUtil;
	
	private static final Log LOG = LogFactory.getLog(QueryServiceSolrImpl.class);
		
	/**
	 * Constructor
	 * @param searchService
	 */
	public QueryServiceSolrImpl(final SearchService searchService, final MappingUtil mappingUtil) {
		
		this.searchService = searchService;
		this.mappingUtil = mappingUtil;
		
		this.inputFactory = new InputFactory(mappingUtil);
		this.outputFactory = new OutputFactory(mappingUtil);
		this.facetFactory = new FacetFactory(mappingUtil);
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public QueryOutput queryResults(final QueryInput queryInput) throws QueryException {
		
		try {
			
			// translate input
			final SearchInput searchInput = inputFactory.getInstance(queryInput);
			if (LOG.isDebugEnabled()) LOG.debug(searchInput.toString());

			// execute search
			final SearchOutput searchOutput = searchService.getResults(searchInput);
			if (LOG.isDebugEnabled()) LOG.debug(searchOutput.toString());
			
			// translate output
			final QueryOutput queryOutput = outputFactory.getInstance(searchOutput);
			if (LOG.isDebugEnabled()) LOG.debug(queryOutput.toString());
			return queryOutput;
			
		} catch(Exception e) {
			throw new QueryException(e);
		}
		
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Note that this method implementation delegates all business logic to the more general method.
	 */
	public Set<String> queryFacet(final String facetKey, final QueryInput queryInput) throws QueryException {
		
		final Set<String> facetKeys = new HashSet<String>();
		facetKeys.add(facetKey);
		final Map<String, Set<String>> facets = this.queryFacets(facetKeys, queryInput);
		return facets.get(facetKey);

	}

	/**
	 * {@inheritDoc}
	 */
	public Map<String, Set<String>> queryFacets(final Collection<String> facetKeys, final QueryInput queryInput) throws QueryException {
		
		try {
			
			// create search input from constraints, facets
			final SearchInput searchInput = inputFactory.getInstance(queryInput, facetKeys);
			if (LOG.isDebugEnabled()) LOG.debug(searchInput.toString());
			
			// execute search
			final Map<String, esg.search.query.api.Facet> facets = searchService.getFacets(searchInput);
			
			// translate output
			final Map<String, Set<String>> _facets = facetFactory.getFacetMap(facets);
			return _facets;
		
		} catch(Exception e) {
			throw new QueryException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Note that this method implementation delegates all business logic to the more general method.
	 *  
	 */
	public Map<String, Set<String>> queryFacets(final QueryInput queryInput) throws QueryException {
		return this.queryFacets(this.mappingUtil.getFacetKeys(), queryInput);
	}

	/**
	 * {@inheritDoc}
	 */
	public QueryResult queryResult(final String identifier) throws QueryException {
		
		try {
			
			// create search input with ID constraint
			final SearchInput searchInput = new SearchInputImpl();
			searchInput.addConstraint(SolrXmlPars.FIELD_ID, identifier);
			if (LOG.isDebugEnabled()) LOG.debug(searchInput.toString());

			// execute search
			final SearchOutput searchOutput = searchService.getResults(searchInput);
			if (LOG.isDebugEnabled()) LOG.debug(searchOutput.toString());
			
			// translate search output
			if (searchOutput.getCounts()==0) {
				return null; // result not found
			} else if (searchOutput.getCounts()==1) {
				final QueryResult queryResult = outputFactory.getInstance(searchOutput.getResults().get(0));
				if (LOG.isDebugEnabled()) LOG.debug(queryResult.toString());
				return queryResult;
			} else {
				throw new Exception("Too many results associated with unique identifier: "+searchOutput.getCounts());
			}
		
		} catch(Exception e) {
			throw new QueryException(e);
		}
		
	}

}