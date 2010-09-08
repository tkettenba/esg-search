package esg.search.harvest.thredds;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import thredds.catalog.InvAccess;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDocumentation;
import thredds.catalog.InvProperty;
import thredds.catalog.ThreddsMetadata.Variable;
import thredds.catalog.ThreddsMetadata.Variables;
import esg.search.core.Record;
import esg.search.core.RecordImpl;
import esg.search.query.impl.solr.SolrXmlPars;

/**
 * Implementation of {@link ThreddsParserStrategy} that produces a single {@link Record} for each top-level THREDDS dataset.
 */
public class ThreddsParserStrategyTopLevelDatasetImpl implements ThreddsParserStrategy {
	
	//private final Log LOG = LogFactory.getLog(this.getClass());
	
	private ThreddsDataseUrlBuilder urlBuilder = new ThreddsDatasetUrlBuilderCatalogUrlImpl();
		
	public ThreddsParserStrategyTopLevelDatasetImpl() {}
	
	public List<Record> parseDataset(final InvDataset dataset) {
		
		final List<Record> records = new ArrayList<Record>();
		
		// <dataset name="...." ID="..." restrictAccess="...">
		final String id = dataset.getID();
		Assert.notNull(id,"Dataset ID cannot be null");
		final Record record = new RecordImpl(id);
		final String name = dataset.getName();
		Assert.notNull(name, "Dataset name cannot be null");
		record.addField(SolrXmlPars.FIELD_TITLE, name);
		
		// catalog URL
		record.addField(SolrXmlPars.FIELD_URL, urlBuilder.buildUrl(dataset));
		
		// type
		record.addField(SolrXmlPars.FIELD_TYPE, "Dataset");
		
		// <documentation type="...">.......</documentation>
		for (final InvDocumentation documentation : dataset.getDocumentation()) {
			final String content = documentation.getInlineContent();
			if (StringUtils.hasText(content)) {
				record.addField(SolrXmlPars.FIELD_DESCRIPTION, content);
			}
		}
		
		// <variables vocabulary="CF-1.0">
        //   <variable name="hfss" vocabulary_name="surface_upward_sensible_heat_flux" units="W m-2">Surface Sensible Heat Flux</variable>
        // </variables>
		for (final Variables variables : dataset.getVariables()) {
			final String vocabulary = variables.getVocabulary();
			for (final Variable variable : variables.getVariableList()) {
				record.addField(SolrXmlPars.FIELD_VARIABLE, variable.getName());
				if (vocabulary.equals(ThreddsPars.CF)) record.addField(SolrXmlPars.FIELD_CF_VARIABLE, variable.getDescription());
			}
		}
		
		// <property name="..." value="..." />
		for (final InvProperty property : dataset.getProperties()) {
			// note: record title already set from dataset name
			if (property.getName().equals(SolrXmlPars.FIELD_TITLE)) {
				record.addField(SolrXmlPars.FIELD_DESCRIPTION, property.getValue());
			} else {
				record.addField(property.getName(), property.getValue());
			}
		}
		
		// <access urlPath="/ipcc/sresb1/atm/3h/hfss/miroc3_2_hires/run1/hfss_A3_2050.nc" serviceName="GRIDFTPatPCMDI" dataFormat="NetCDF" />
		for (final InvAccess access : dataset.getAccess()) {
			//
		}

		records.add(record);
		return records;
		
	}

}
