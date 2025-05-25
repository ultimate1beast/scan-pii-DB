package com.privsense.core.repository;

import com.privsense.core.model.User;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Repository interface for managing User entities.
 */
public interface UserRepository {
    
    /**
     * Find a user by username.
     *
     * @param username The username
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find a user by email.
     *
     * @param email The email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if a user with the given username exists.
     *
     * @param username The username
     * @return true if a user with the username exists
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if a user with the given email exists.
     *
     * @param email The email
     * @return true if a user with the email exists
     */
    boolean existsByEmail(String email);
    
    // Standard repository methods
    <S extends User> S save(S entity);
    
    <S extends User> List<S> saveAll(Iterable<S> entities);
    
    Optional<User> findById(UUID id);
    
    boolean existsById(UUID id);
    
    List<User> findAll();
    
    List<User> findAll(Sort sort);
    
    Page<User> findAll(Pageable pageable);
    
    List<User> findAllById(Iterable<UUID> ids);
    
    long count();
    
    void deleteById(UUID id);
    
    void delete(User entity);
    
    void deleteAllById(Iterable<? extends UUID> ids);
    
    void deleteAll(Iterable<? extends User> entities);
    
    void deleteAll();
    
    void flush();
    
    <S extends User> S saveAndFlush(S entity);
    
    <S extends User> List<S> saveAllAndFlush(Iterable<S> entities);
    
    void deleteAllInBatch(Iterable<User> entities);
    
    void deleteAllByIdInBatch(Iterable<UUID> ids);
    
    void deleteAllInBatch();
    
    User getOne(UUID id);
    
    User getById(UUID id);
    
    User getReferenceById(UUID id);
    
    <S extends User> Optional<S> findOne(Example<S> example);
    
    <S extends User> List<S> findAll(Example<S> example);
    
    <S extends User> List<S> findAll(Example<S> example, Sort sort);
    
    <S extends User> Page<S> findAll(Example<S> example, Pageable pageable);
    
    <S extends User> long count(Example<S> example);
    
    <S extends User> boolean exists(Example<S> example);
    
    <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction);
}