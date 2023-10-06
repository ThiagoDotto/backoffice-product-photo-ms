package br.com.repassa.repository;

import br.com.repassa.entity.PhotosManager;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.socialsignin.spring.data.dynamodb.repository.EnableScanCount;
import org.springframework.data.repository.PagingAndSortingRepository;


@EnableScan
@EnableScanCount
public interface PhotosManagerRepository  extends PagingAndSortingRepository<PhotosManager, String> {
}
