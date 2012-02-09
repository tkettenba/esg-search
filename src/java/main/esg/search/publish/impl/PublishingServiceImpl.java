package esg.search.publish.impl;

import java.rmi.RemoteException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import esg.search.publish.api.MetadataDeletionService;
import esg.search.publish.api.MetadataRepositoryCrawlerManager;
import esg.search.publish.api.MetadataRepositoryType;
import esg.search.publish.api.PublishingService;

/**
 * Implementation of {@link PublishingService} that delegates all functionality
 * to collaborating beans for crawling remote metadata repositories, producing
 * search records, and consuming search records for ingestion or removal.
 * 
 * @author luca.cinquini
 * 
 */
@Service("publishingService")
public class PublishingServiceImpl implements PublishingService {

    /**
     * Collaborator that crawls remote metadata repositories for the purpose of
     * publishing records into the system.
     */
    private final MetadataRepositoryCrawlerManager publisherCrawler;

    /**
     * Collaborator that crawls remote metadata repositories for the purpose of
     * unpublishing records from the system.
     */
    private final MetadataRepositoryCrawlerManager unpublisherCrawler;
    
    private final Log LOG = LogFactory.getLog(this.getClass());

    /**
     * Collaborator that deletes records with known identifiers.
     */
    private final MetadataDeletionService recordRemover;

    @Autowired
    public PublishingServiceImpl(
            final PublisherCrawlerManagerImpl publisherCrawler,
            final UnpublisherCrawlerManagerImpl unpublisherCrawler,
            final MetadataDeletionService recordRemover) {

        this.publisherCrawler = publisherCrawler;
        this.unpublisherCrawler = unpublisherCrawler;
        this.recordRemover = recordRemover;
    }

    @Override
	public void publish(String uri, boolean recursive, MetadataRepositoryType metadataRepositoryType) throws RemoteException {
             
        try {
            publisherCrawler.crawl(uri, recursive, metadataRepositoryType, true); // publish=true
        } catch(Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            throw new RemoteException(e.getMessage());
        }
		
	}

    @Override
    public void unpublish(String uri, boolean recursive, MetadataRepositoryType metadataRepositoryType) throws RemoteException {

        try {
            unpublisherCrawler.crawl(uri, recursive, metadataRepositoryType, false); // publish=false
        } catch(Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

    @Override
    public void unpublish(List<String> ids) throws RemoteException {

        try {
            recordRemover.delete(ids);
        } catch(Exception e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

    }

}
