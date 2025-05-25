package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.UserJpaRepository;
import com.privsense.core.model.User;
import com.privsense.core.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Implementation of the UserRepository interface using Spring Data JPA.
 */
@Repository
@Primary
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public UserRepositoryImpl(UserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        try {
            Long count = entityManager.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
            return count > 0;
        } catch (NoResultException e) {
            return false;
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        try {
            Long count = entityManager.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
            return count > 0;
        } catch (NoResultException e) {
            return false;
        }
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }

    @Override
    public <S extends User> S saveAndFlush(S entity) {
        return jpaRepository.saveAndFlush(entity);
    }

    @Override
    public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) {
        return jpaRepository.saveAllAndFlush(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<User> entities) {
        jpaRepository.deleteAllInBatch(entities);
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<UUID> uuids) {
        jpaRepository.deleteAllByIdInBatch(uuids);
    }

    @Override
    public void deleteAllInBatch() {
        jpaRepository.deleteAllInBatch();
    }

    @Override
    public User getOne(UUID uuid) {
        return jpaRepository.getReferenceById(uuid);
    }

    @Override
    public User getById(UUID uuid) {
        return jpaRepository.getReferenceById(uuid);
    }

    @Override
    public User getReferenceById(UUID uuid) {
        return jpaRepository.getReferenceById(uuid);
    }

    @Override
    public <S extends User> Optional<S> findOne(Example<S> example) {
        return jpaRepository.findOne(example);
    }

    @Override
    public <S extends User> List<S> findAll(Example<S> example) {
        return jpaRepository.findAll(example);
    }

    @Override
    public <S extends User> List<S> findAll(Example<S> example, Sort sort) {
        return jpaRepository.findAll(example, sort);
    }

    @Override
    public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) {
        return jpaRepository.findAll(example, pageable);
    }

    @Override
    public <S extends User> long count(Example<S> example) {
        return jpaRepository.count(example);
    }

    @Override
    public <S extends User> boolean exists(Example<S> example) {
        return jpaRepository.exists(example);
    }

    @Override
    public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return jpaRepository.findBy(example, queryFunction);
    }

    @Override
    public <S extends User> S save(S entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public <S extends User> List<S> saveAll(Iterable<S> entities) {
        return jpaRepository.saveAll(entities);
    }

    @Override
    public Optional<User> findById(UUID uuid) {
        return jpaRepository.findById(uuid);
    }

    @Override
    public boolean existsById(UUID uuid) {
        return jpaRepository.existsById(uuid);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<User> findAllById(Iterable<UUID> uuids) {
        return jpaRepository.findAllById(uuids);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public void deleteById(UUID uuid) {
        jpaRepository.deleteById(uuid);
    }

    @Override
    public void delete(User entity) {
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteAllById(Iterable<? extends UUID> uuids) {
        jpaRepository.deleteAllById(uuids);
    }

    @Override
    public void deleteAll(Iterable<? extends User> entities) {
        jpaRepository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public List<User> findAll(Sort sort) {
        return jpaRepository.findAll(sort);
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }
}