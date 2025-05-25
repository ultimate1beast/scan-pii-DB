package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.RoleJpaRepository;
import com.privsense.core.model.Role;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of the RoleRepository interface using Spring Data JPA.
 */
@Repository
@Primary
public class RoleRepositoryImpl implements com.privsense.core.repository.RoleRepository {

    private final RoleJpaRepository jpaRepository;

    @Autowired
    public RoleRepositoryImpl(RoleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRepository.findByName(name);
    }

    @Override
    public Set<Role> findByNameIn(Set<String> names) {
        // Since the JPA repository doesn't have this method, we need to implement it
        return jpaRepository.findAll().stream()
                .filter(role -> names.contains(role.getName()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.findByName(name).isPresent();
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }

    @Override
    public <S extends Role> S saveAndFlush(S entity) {
        return jpaRepository.saveAndFlush(entity);
    }

    @Override
    public <S extends Role> List<S> saveAllAndFlush(Iterable<S> entities) {
        return jpaRepository.saveAllAndFlush(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<Role> entities) {
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
    public Role getOne(UUID uuid) {
        return jpaRepository.getById(uuid);
    }

    @Override
    public Role getById(UUID uuid) {
        return jpaRepository.getById(uuid);
    }

    @Override
    public Role getReferenceById(UUID uuid) {
        return jpaRepository.getReferenceById(uuid);
    }

    @Override
    public <S extends Role> Optional<S> findOne(Example<S> example) {
        return jpaRepository.findOne(example);
    }

    @Override
    public <S extends Role> List<S> findAll(Example<S> example) {
        return jpaRepository.findAll(example);
    }

    @Override
    public <S extends Role> List<S> findAll(Example<S> example, Sort sort) {
        return jpaRepository.findAll(example, sort);
    }

    @Override
    public <S extends Role> Page<S> findAll(Example<S> example, Pageable pageable) {
        return jpaRepository.findAll(example, pageable);
    }

    @Override
    public <S extends Role> long count(Example<S> example) {
        return jpaRepository.count(example);
    }

    @Override
    public <S extends Role> boolean exists(Example<S> example) {
        return jpaRepository.exists(example);
    }

    @Override
    public <S extends Role, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return jpaRepository.findBy(example, queryFunction);
    }

    @Override
    public <S extends Role> S save(S entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public <S extends Role> List<S> saveAll(Iterable<S> entities) {
        return jpaRepository.saveAll(entities);
    }

    @Override
    public Optional<Role> findById(UUID uuid) {
        return jpaRepository.findById(uuid);
    }

    @Override
    public boolean existsById(UUID uuid) {
        return jpaRepository.existsById(uuid);
    }

    @Override
    public List<Role> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Role> findAllById(Iterable<UUID> uuids) {
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
    public void delete(Role entity) {
        jpaRepository.delete(entity);
    }

    @Override
    public void deleteAllById(Iterable<? extends UUID> uuids) {
        jpaRepository.deleteAllById(uuids);
    }

    @Override
    public void deleteAll(Iterable<? extends Role> entities) {
        jpaRepository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public List<Role> findAll(Sort sort) {
        return jpaRepository.findAll(sort);
    }

    @Override
    public Page<Role> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }
}