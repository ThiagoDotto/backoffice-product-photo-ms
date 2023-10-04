package br.com.repassa.service.repository;

import br.com.repassa.service.entity.PhotosManager;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.socialsignin.spring.data.dynamodb.repository.EnableScanCount;
import org.springframework.data.repository.PagingAndSortingRepository;


@EnableScan
@EnableScanCount
public interface PhotosManagerRepository  extends PagingAndSortingRepository<PhotosManager, String> {
}
