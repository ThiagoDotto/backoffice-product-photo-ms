package br.com.repassa.repository;

import br.com.repassa.entity.PhotosManager;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PhotosManagerRepository implements PanacheRepository<PhotosManager> {
}